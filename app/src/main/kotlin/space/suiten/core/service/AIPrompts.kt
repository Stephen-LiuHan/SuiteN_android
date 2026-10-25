package space.suiten.core.service

import com.google.gson.annotations.SerializedName

/** One capture's reader-side data used for the book summary. Never contains book text. */
data class BookSummaryCaptureInput(
    val emotionLabels: String,
    val memo: String?,
    val reaction: String?,
    val axisLabel: String?,
    val topicTags: List<String>
)

data class BookSummaryOutput(
    @SerializedName("summary") val summary: String = "",
    @SerializedName("reading_style") val readingStyle: String = "",
    @SerializedName("points") val points: List<String> = emptyList(),
    @SerializedName("next_question") val nextQuestion: String = ""
)

/**
 * Prompts for SuiteN's AI (see SuiteN_doc/spec/04_ai_spec.md).
 *
 * Every prompt starts from SuiteN's belief: the book does not hold the answer to the
 * reader's question; the answer is in the reader's head. The AI draws it out.
 */
object AIPrompts {
    private const val BELIEF = """SuiteN の考え方：本に答えはありません。読者が本に求めているのは自分の悩みへの答えですが、その問いは読者に特有の状況から生まれるので、本に直接の答えは書かれていません。答えは読者の頭の中にあります。あなたの役割は答えを出すことではなく、読者の反応を言葉にして、考えを引き出すことです。"""

    private const val COMMON_RULES = """- 主語は読者。「〜と感じたようです」「〜と捉えたのかもしれません」のように推量で書く
- 本の内容をまとめない。「この本では」「著者は」「本書は」を主語にしない
- 「述べている」「書かれている」「紹介されている」「説明している」を使わない
- 助言しない（「〜しましょう」「〜すべきです」を使わない）"""

    val REACTION_SYSTEM_PROMPT = """あなたは読書アプリ SuiteN のAIです。
$BELIEF

【入力】
- 画面の文章：読者が反応した箇所。どこに反応したかを理解するためだけに使い、要約・引用・言い換えはしない
- 感情スタンプ：疑問、共感、ひらめき、注意、重要（複数のこともある）
- メモ：読者の短いメモ（ないこともある）
- 同じ本でのこれまでの反応：あれば、つながりに触れてよい

【出力】次の形式のJSONだけを返す
{
  "draft_summary": "読者のその場の反応を1〜2文で（120文字程度、最大${AIGuardrails.MAX_REACTION_LENGTH}文字）",
  "deepening_question": "考えを一段深める問いを1つ（${AIGuardrails.MAX_QUESTION_LENGTH}文字以内）",
  "next_actions": [
    {"type": "dialog", "title": "AIと対話して深める", "description": "何を対話で整理するか", "query": null, "link": null},
    {"type": "books", "title": "関連書籍を探す", "description": "どんな方向の本か", "query": "検索語", "link": null},
    {"type": "videos", "title": "関連動画を探す", "description": "どんな方向の動画か", "query": "検索語", "link": null}
  ],
  "topic_tags": ["読者側の論点を3〜5個"]
}

【draft_summary の書き方】
$COMMON_RULES
- 疑問のときは、何が引っかかったのか。共感のときは、何が響き、どんな経験と重なっていそうか
- メモがなくても、スタンプと画面の文章から反応の向きを推し量る
- 画面の文章を${AIGuardrails.VERBATIM_MIN_LENGTH}文字以上続けて使わない

【良い例】感情: 疑問 / メモ: うちで2週間は無理？
{"draft_summary": "短い周期で小さく試す進め方に惹かれつつも、自社の承認の流れではその速さで回すのは難しいのでは、と引っかかりを感じたようです。", "deepening_question": "このずれについて、いちばん気になっているのはどこですか？"}

【良い例】感情: 共感 / メモ: 前の企画が通らなかったの、これだ
{"draft_summary": "去年の企画が通らなかった経験と重なり、深く共感したようです。計画の出来より、目に見える手応えが人を動かす、という捉え方が腑に落ちたのかもしれません。", "deepening_question": "この共感は、どこにいちばん響いていますか？"}"""

    val BOOK_SUMMARY_SYSTEM_PROMPT = """あなたは読書アプリ SuiteN のAIです。
$BELIEF

一冊の中で読者が残した反応（感情スタンプ、メモ、その場の反応、4択で選んだ観点）を横断して、読者自身の読み方と考えの変化を浮かび上がらせてください。1件ずつでは愚痴や相づちに見える反応も、並べると読者の考え方が見えてきます。疑問は読者が気にしていること、共感は読者が大切にしていることを表します。

【出力】次の形式のJSONだけを返す
{
  "summary": "一冊を通した読者の読み方（最大${AIGuardrails.MAX_BOOK_SUMMARY_LENGTH}文字）",
  "reading_style": "浮かび上がった読み方を1文で（${AIGuardrails.MAX_READING_STYLE_LENGTH}文字以内）",
  "points": ["読者の論点（各${AIGuardrails.MAX_POINT_LENGTH}文字以内、最大3件）"],
  "next_question": "次に考えたい問い（${AIGuardrails.MAX_QUESTION_LENGTH}文字以内）"
}

【summary の書き方】
$COMMON_RULES
- 疑問がどこに集まり、共感がどこに向いていたかを書く（例：5回のうち4回が承認や責任に関するもの）
- 読み進めるうちに、問いや考えがどう変わったように見えるかを書く

【良い例】
{"summary": "この本を通して、疑問は手法そのものより「自社の意思決定の仕組みで回せるか」に集まり、共感は「計画より目に見える手応えが人を動かす」ことに向いていました。去年企画が通らなかった経験と重なっているようです。読み進めるうちに、問いは「うちでは無理では」から「小さな手応えで部長を味方にできるのでは」へ変わっていったように見えます。", "reading_style": "疑問では組織で実行できるかを確かめ、共感では人を動かす方法を探している", "points": ["失敗してよい範囲と責任の所在を先に決める"], "next_question": "部長に見せる最初の小さな手応えは、何にしますか？"}"""

    val CHAT_SYSTEM_PROMPT = """あなたは読書アプリ SuiteN のAIです。
$BELIEF

読者との対話では答えを出しません。読者の言葉を短く受け止め、考えを一段深める問いを1つ返してください。
$COMMON_RULES
- 本の文章を引用しない
- ${AIGuardrails.MAX_CHAT_LENGTH}文字以内"""

    private val AXIS_LABELS = mapOf(
        "risk" to "リスク",
        "success_rate" to "成功率",
        "rationale" to "あるべき論の根拠",
        "concept" to "コンセプトと意義"
    )

    fun axisLabel(axis: String?): String? = axis?.let { AXIS_LABELS[it] ?: it }

    /** [ocrText] is used in memory for this request only and must not be stored or logged. */
    fun buildReactionPrompt(
        ocrText: String?,
        emotionLabels: String,
        memo: String?,
        pastReactions: List<String>,
        feedback: String? = null
    ): String = buildString {
        appendLine(REACTION_SYSTEM_PROMPT)
        appendLine()
        if (!ocrText.isNullOrBlank()) {
            appendLine("【画面の文章（参照のみ・引用禁止）】")
            appendLine(ocrText)
            appendLine()
        }
        appendLine("【読者の反応】")
        appendLine("感情: ${emotionLabels.ifBlank { "なし" }}")
        appendLine("メモ: ${memo?.takeIf { it.isNotBlank() } ?: "なし"}")
        if (pastReactions.isNotEmpty()) {
            appendLine()
            appendLine("【同じ本でのこれまでの反応】")
            pastReactions.takeLast(5).forEach { appendLine("- $it") }
        }
        if (feedback != null) {
            appendLine()
            append(feedback)
        }
    }

    fun buildBookSummaryPrompt(
        bookTitle: String,
        contentTypeName: String,
        bookMemo: String?,
        captures: List<BookSummaryCaptureInput>,
        feedback: String? = null
    ): String = buildString {
        appendLine(BOOK_SUMMARY_SYSTEM_PROMPT)
        appendLine()
        appendLine("【${contentTypeName}】$bookTitle")
        if (!bookMemo.isNullOrBlank()) {
            appendLine("読者のメモ: $bookMemo")
        }
        appendLine()
        appendLine("【読者が残した反応（${captures.size}件、読んだ順）】")
        captures.forEachIndexed { index, c ->
            appendLine("${index + 1}. 感情: ${c.emotionLabels.ifBlank { "なし" }} / メモ: ${c.memo?.takeIf { it.isNotBlank() } ?: "なし"}")
            c.reaction?.let { appendLine("   その場の反応: $it") }
            c.axisLabel?.let { appendLine("   4択で選んだ観点: $it") }
            if (c.topicTags.isNotEmpty()) appendLine("   論点: ${c.topicTags.joinToString("、")}")
        }
        if (feedback != null) {
            appendLine()
            append(feedback)
        }
    }

    fun buildChatPrompt(reaction: String, question: String, axisLabel: String?, userMessage: String, feedback: String? = null): String =
        buildString {
            appendLine(CHAT_SYSTEM_PROMPT)
            appendLine()
            appendLine("【このキャプチャでの読者の反応】$reaction")
            appendLine("【深掘りの問い】$question")
            axisLabel?.let { appendLine("【読者が選んだ観点】$it") }
            appendLine()
            appendLine("【読者のメッセージ】")
            appendLine(userMessage)
            if (feedback != null) {
                appendLine()
                append(feedback)
            }
        }

    /** The text stored in books.summary and shown (and edited) as the reader's summary. */
    fun formatBookSummary(output: BookSummaryOutput): String = buildString {
        append(output.summary.trim())
        if (output.readingStyle.isNotBlank()) {
            append("\n\n【浮かび上がった読み方】\n").append(output.readingStyle.trim())
        }
        val points = output.points.map { it.trim() }.filter { it.isNotEmpty() }.take(3)
        if (points.isNotEmpty()) {
            append("\n\n【あなたの論点】\n").append(points.joinToString("\n") { "・$it" })
        }
        if (output.nextQuestion.isNotBlank()) {
            append("\n\n【次に考えたい問い】\n").append(output.nextQuestion.trim())
        }
    }

    /** Extracts the JSON object from a model response that may include extra text. */
    fun extractJson(text: String): String {
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        require(start >= 0 && end > start) { "No JSON object in AI response" }
        return text.substring(start, end + 1)
    }

    // --- Offline responses (no API key, or AI output that kept breaking the rules) ---

    /** A reaction built only from the reader's own input. It never uses the OCR text. */
    fun fallbackReaction(emotionLabels: String, memo: String?): Pair<String, String> {
        val feeling = emotionLabels.ifBlank { "心が動いた" }
        val reaction = if (!memo.isNullOrBlank()) {
            "「${memo.take(60)}」と書き留めたところに、${feeling}の気持ちが動いたようです。"
        } else {
            "この箇所で、${feeling}の気持ちが動いたようです。"
        }
        val question = when {
            emotionLabels.contains("共感") -> "この共感は、どんな経験と重なっていますか？"
            emotionLabels.contains("疑問") -> "いちばん引っかかっているのは、どこですか？"
            else -> "この箇所の、どこが気になりましたか？"
        }
        return reaction to question
    }

    fun offlineBookSummary(captures: List<BookSummaryCaptureInput>): BookSummaryOutput {
        val doubts = captures.count { it.emotionLabels.contains("疑問") }
        val empathy = captures.count { it.emotionLabels.contains("共感") }
        return BookSummaryOutput(
            summary = "${captures.size}件の反応のうち、疑問が${doubts}件、共感が${empathy}件ありました。" +
                "疑問は気にしていること、共感は大切にしていることを表しているのかもしれません。",
            readingStyle = "自分の状況に引きつけながら読んでいるようです",
            points = captures.mapNotNull { it.memo?.takeIf { m -> m.isNotBlank() }?.take(AIGuardrails.MAX_POINT_LENGTH) }.take(3),
            nextQuestion = "この本で、いちばん自分に引きつけて考えたのはどこですか？"
        )
    }
}
