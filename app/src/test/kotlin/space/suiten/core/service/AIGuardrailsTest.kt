package space.suiten.core.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import space.suiten.core.service.AIGuardrails.Kind

class AIGuardrailsTest {
    private fun kinds(v: List<AIGuardrails.Violation>) = v.map { it.kind }.toSet()

    // The ideal outputs shown in the product deck must pass.
    @Test
    fun `deck examples pass`() {
        val doubt = AIGuardrails.checkReaction(
            "短い周期で小さく試す進め方に惹かれつつも、自社の承認の流れではその速さで回すのは難しいのでは、と引っかかりを感じたようです。",
            "このずれについて、いちばん気になっているのはどこですか？",
            ocrText = PAGE_48
        )
        assertEquals(emptyList<AIGuardrails.Violation>(), doubt)

        val empathy = AIGuardrails.checkReaction(
            "去年の企画が通らなかった経験と重なり、深く共感したようです。計画の出来より、目に見える手応えが人を動かす、という捉え方が腑に落ちたのかもしれません。",
            "この共感は、どこにいちばん響いていますか？",
            ocrText = PAGE_73
        )
        assertEquals(emptyList<AIGuardrails.Violation>(), empathy)
    }

    @Test
    fun `book summary from the deck passes`() {
        val output = BookSummaryOutput(
            summary = "この本を通して、疑問は手法そのものより「自社の意思決定の仕組みで回せるか」に集まり、共感は「計画より目に見える手応えが人を動かす」ことに向いていました。去年企画が通らなかった経験と重なっているようです。",
            readingStyle = "疑問では組織で実行できるかを確かめ、共感では人を動かす方法を探している",
            points = listOf("失敗してよい範囲と責任の所在を先に決める"),
            nextQuestion = "部長に見せる最初の小さな手応えは、何にしますか？"
        )
        assertEquals(emptyList<AIGuardrails.Violation>(), AIGuardrails.checkBookSummary(output))
    }

    @Test
    fun `summarizing the book is rejected`() {
        val v = AIGuardrails.checkReaction(
            "この本では『環境設計』という概念が紹介されています。著者は意志力より環境が大切だと述べている。",
            "どう思いますか？",
            ocrText = null
        )
        assertTrue(kinds(v).containsAll(setOf(Kind.BOOK_SUBJECT, Kind.FORBIDDEN_PHRASE, Kind.NOT_TENTATIVE)))
    }

    @Test
    fun `copying the page text is rejected`() {
        val v = AIGuardrails.checkReaction(
            "完璧な計画を立てるより、2週間単位で小さな実験を回すほうが学びは早い、と感じたようです。",
            "どこが気になりましたか？",
            ocrText = PAGE_48
        )
        assertTrue(Kind.VERBATIM in kinds(v))
    }

    @Test
    fun `line breaks in OCR text do not hide copying`() {
        val ocrWithBreaks = "完璧な計画を立てるより、\n2週間単位で小さな\n実験を回すほうが学びは早い。"
        assertTrue(AIGuardrails.longestSharedRun("2週間単位で小さな実験を回すほうが学びは早い", ocrWithBreaks) >= AIGuardrails.VERBATIM_MIN_LENGTH)
    }

    @Test
    fun `length limits are 250 for the reaction and 60 for the question`() {
        val longReaction = "と感じたようです。".repeat(30)
        val longQuestion = "どこ".repeat(31) + "？"
        val v = AIGuardrails.checkReaction(longReaction, longQuestion, null)
        assertEquals(setOf("draft_summary", "deepening_question"), v.filter { it.kind == Kind.TOO_LONG }.map { it.field }.toSet())
    }

    @Test
    fun `advice is rejected`() {
        val v = AIGuardrails.checkReaction("小さく始めてみましょう。そう感じたようです。", "どう思いますか？", null)
        assertTrue(Kind.ADVICE in kinds(v))
    }

    @Test
    fun `feedback lists every problem`() {
        val v = AIGuardrails.checkReaction("この本では大事だと述べている。", "？", null)
        val feedback = AIGuardrails.feedback(v)
        v.forEach { assertTrue(feedback.contains(it.detail)) }
    }

    companion object {
        const val PAGE_48 = "新規事業の初期には、完璧な計画を立てるより、2週間単位で小さな実験を回すほうが学びは早い。仮説を一つに絞り、最小限の試作品を顧客に見せ、反応を見て次の2週間の打ち手を決める。大切なのは、失敗を前提に予算と期間をあらかじめ小さく区切っておくことだ。"
        const val PAGE_73 = "どれほど練り上げた計画書でも、小さな事実ひとつにはかなわない。人が動くのは、論理の正しさより「もう動いている」という手応えを見たときだ。"
    }
}
