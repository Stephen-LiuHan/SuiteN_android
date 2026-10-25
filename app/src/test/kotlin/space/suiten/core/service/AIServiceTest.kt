package space.suiten.core.service

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AIServiceTest {
    private val goodReaction = """{"draft_summary": "自社の承認の流れでは、その速さで回すのは難しいのでは、と引っかかりを感じたようです。",
        "deepening_question": "いちばん気になっているのはどこですか？",
        "next_actions": [{"type": "dialog", "title": "AIと対話して深める", "description": "前提を整理する"},
                         {"type": "books", "title": "関連書籍を探す", "description": "社内で小さく始める方法", "query": "社内 新規事業 小さく始める"},
                         {"type": "videos", "title": "関連動画を探す", "description": "検証の進め方", "query": "新規事業 検証"},
                         {"type": "books", "title": "4件目", "description": "上限を超える"}],
        "topic_tags": ["意思決定の速さ", "承認プロセス"]}"""

    private val bookSummaryReaction = """{"draft_summary": "この本では小さな実験が紹介されています。", "deepening_question": "どう思いますか？",
        "next_actions": [], "topic_tags": []}"""

    private class FakeModel(private vararg val responses: String) {
        val prompts = mutableListOf<String>()
        suspend fun generate(prompt: String): String {
            prompts += prompt
            return responses[minOf(prompts.size - 1, responses.size - 1)]
        }
    }

    @Test
    fun `a compliant reaction is used as is, with at most three next actions`() = runBlocking {
        val model = FakeModel(goodReaction)
        val (result, actions) = AIService(model::generate).processCapture("本文", "うちで2週間は無理？", "疑問")
        assertTrue(result.draftSummary.startsWith("自社の承認の流れ"))
        assertEquals(3, actions.size)
        assertEquals(1, model.prompts.size)
    }

    @Test
    fun `a rule-breaking reaction is regenerated with feedback`() = runBlocking {
        val model = FakeModel(bookSummaryReaction, goodReaction)
        val (result, _) = AIService(model::generate).processCapture("本文", "メモ", "疑問")
        assertEquals(2, model.prompts.size)
        assertTrue(model.prompts[1].contains("前回の出力の問題点"))
        assertTrue(result.draftSummary.startsWith("自社の承認の流れ"))
    }

    @Test
    fun `output that keeps breaking rules is never shown`() = runBlocking {
        val model = FakeModel(bookSummaryReaction)
        val (result, _) = AIService(model::generate).processCapture("本文", "前の企画が通らなかったの、これだ", "共感")
        assertFalse(result.draftSummary.contains("この本では"))
        assertTrue(result.draftSummary.contains("前の企画が通らなかったの、これだ"))
        assertEquals("この共感は、どんな経験と重なっていますか？", result.deepeningQuestion)
        assertTrue(AIGuardrails.checkReaction(result.draftSummary, result.deepeningQuestion, null).isEmpty())
    }

    @Test
    fun `the prompt carries SuiteN's belief, stamps and earlier reactions`() = runBlocking {
        val model = FakeModel(goodReaction)
        AIService(model::generate).processCapture("本文", null, "疑問・共感", listOf("前回は意思決定の速さに引っかかったようです。"))
        val prompt = model.prompts.single()
        assertTrue(prompt.contains("答えは読者の頭の中にあります"))
        assertTrue(prompt.contains("感情: 疑問・共感"))
        assertTrue(prompt.contains("前回は意思決定の速さに引っかかったようです。"))
    }

    @Test
    fun `book summary uses reader data and is formatted with its sections`() = runBlocking {
        val model = FakeModel("""{"summary": "疑問は意思決定の仕組みに集まり、共感は手応えに向いていたようです。",
            "reading_style": "組織で実行できるかを確かめながら読んでいる",
            "points": ["失敗してよい範囲を先に決める"],
            "next_question": "最初の小さな手応えは何にしますか？"}""")
        val inputs = listOf(
            BookSummaryCaptureInput("疑問", "うちで2週間は無理？", "引っかかりを感じたようです。", "リスク", listOf("承認")),
            BookSummaryCaptureInput("共感", "前の企画が通らなかったの、これだ", null, null, emptyList())
        )
        val text = AIService(model::generate).generateBookSummary("小さく始める新規事業", "本", null, inputs)

        val prompt = model.prompts.single()
        assertTrue(prompt.contains("1. 感情: 疑問 / メモ: うちで2週間は無理？"))
        assertTrue(prompt.contains("4択で選んだ観点: リスク"))
        assertTrue(text.contains("【浮かび上がった読み方】\n組織で実行できるかを確かめながら読んでいる"))
        assertTrue(text.contains("【あなたの論点】\n・失敗してよい範囲を先に決める"))
    }

    @Test(expected = IllegalStateException::class)
    fun `a book summary that keeps breaking rules fails instead of being saved`(): Unit = runBlocking {
        val model = FakeModel("""{"summary": "この本では環境設計が紹介されています。", "reading_style": "", "points": [], "next_question": "？"}""")
        AIService(model::generate).generateBookSummary("t", "本", null, listOf(BookSummaryCaptureInput("疑問", null, null, null, emptyList())))
        Unit
    }

    @Test
    fun `chat asks rather than answers and falls back when it breaks rules`() = runBlocking {
        val context = space.suiten.domain.model.AIResult(
            id = "r", captureId = "c", draftSummary = "反応", deepeningQuestion = "問い",
            axisSelected = "risk", topicTags = emptyList(), createdAt = java.util.Date(), updatedAt = java.util.Date()
        )
        val model = FakeModel("著者は小さく始めるべきだと述べている。小さく始めましょう。")
        val reply = AIService(model::generate).chat(context, "失敗したら誰の責任？")
        assertEquals(2, model.prompts.size)
        assertTrue(model.prompts[0].contains("【読者が選んだ観点】リスク"))
        assertTrue(AIGuardrails.checkChat(reply).isEmpty())
    }

    @Test
    fun `offline responses follow the rules`() {
        val (reaction, question) = AIPrompts.fallbackReaction("疑問", "うちで2週間は無理？")
        assertTrue(AIGuardrails.checkReaction(reaction, question, null).isEmpty())
        val summary = AIPrompts.offlineBookSummary(listOf(
            BookSummaryCaptureInput("疑問", "うちで2週間は無理？", null, null, emptyList()),
            BookSummaryCaptureInput("共感", "これだ", null, null, emptyList())
        ))
        assertTrue(AIGuardrails.checkBookSummary(summary).isEmpty())
    }
}
