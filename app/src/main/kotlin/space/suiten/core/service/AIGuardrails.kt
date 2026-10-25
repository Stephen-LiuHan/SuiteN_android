package space.suiten.core.service

/**
 * Checks AI output against the rules in SuiteN_doc/spec/04_ai_spec.md.
 *
 * The answer to the reader's question is in the reader's head, not in the book,
 * so AI output must stay on the reader's reaction: the reader is the subject,
 * phrasing is tentative, and nothing reproduces or summarizes the book text.
 * Output that breaks a rule is regenerated or replaced; it is never shown as is.
 */
object AIGuardrails {
    const val MAX_REACTION_LENGTH = 250
    const val MAX_QUESTION_LENGTH = 60
    const val MAX_BOOK_SUMMARY_LENGTH = 400
    const val MAX_READING_STYLE_LENGTH = 60
    const val MAX_POINT_LENGTH = 40
    const val MAX_CHAT_LENGTH = 200

    /** Shared runs of this many characters with the OCR text count as reproduction. */
    const val VERBATIM_MIN_LENGTH = 15

    /** Phrases that report what the book says instead of how the reader reacted. */
    val FORBIDDEN_PHRASES = listOf(
        "述べている", "述べています", "述べられて",
        "書かれている", "書かれています", "書いてある",
        "記載されている", "記載されています",
        "説明している", "説明しています",
        "主張している", "主張しています",
        "紹介されている", "紹介されています"
    )

    /** Openings that make the book or author the subject. */
    val BOOK_SUBJECTS = listOf("この本では", "この本は", "本書では", "本書は", "著者は", "筆者は", "作者は", "この章では")

    /** At least one of these marks the phrasing as tentative (感じた, 捉えた, ...). */
    val TENTATIVE_MARKERS = listOf("感じ", "捉え", "ようです", "ようだ", "かもしれ", "のでは", "でしょうか", "ように見え", "と思われ")

    /** The AI reflects and asks; it doesn't tell the reader what to do. */
    val ADVICE_PHRASES = listOf("ましょう", "すべきです", "すべきだ", "するべきです")

    enum class Kind { FORBIDDEN_PHRASE, BOOK_SUBJECT, TOO_LONG, NOT_TENTATIVE, ADVICE, VERBATIM, EMPTY }

    data class Violation(val field: String, val kind: Kind, val detail: String)

    /** Checks one piece of generated text. */
    fun checkText(
        field: String,
        text: String,
        maxLength: Int,
        requireTentative: Boolean,
        sourceText: String? = null
    ): List<Violation> {
        val violations = mutableListOf<Violation>()
        if (text.isBlank()) {
            return listOf(Violation(field, Kind.EMPTY, "空です"))
        }
        FORBIDDEN_PHRASES.filter { text.contains(it) }.forEach {
            violations += Violation(field, Kind.FORBIDDEN_PHRASE, "「$it」は本の内容の説明になります")
        }
        BOOK_SUBJECTS.filter { text.contains(it) }.forEach {
            violations += Violation(field, Kind.BOOK_SUBJECT, "「$it」で本や著者が主語になっています")
        }
        ADVICE_PHRASES.filter { text.contains(it) }.forEach {
            violations += Violation(field, Kind.ADVICE, "「$it」は助言になっています")
        }
        if (text.length > maxLength) {
            violations += Violation(field, Kind.TOO_LONG, "${text.length}文字で、上限の${maxLength}文字を超えています")
        }
        if (requireTentative && TENTATIVE_MARKERS.none { text.contains(it) }) {
            violations += Violation(field, Kind.NOT_TENTATIVE, "断定的です（〜と感じた、〜と捉えた、などにする）")
        }
        if (!sourceText.isNullOrBlank()) {
            val shared = longestSharedRun(text, sourceText)
            if (shared >= VERBATIM_MIN_LENGTH) {
                violations += Violation(field, Kind.VERBATIM, "画面の文章と${shared}文字続けて一致しています")
            }
        }
        return violations
    }

    /** Checks a capture's reaction and question. [ocrText] is used only for this comparison. */
    fun checkReaction(reaction: String, question: String, ocrText: String?): List<Violation> =
        checkText("draft_summary", reaction, MAX_REACTION_LENGTH, requireTentative = true, sourceText = ocrText) +
            checkText("deepening_question", question, MAX_QUESTION_LENGTH, requireTentative = false, sourceText = ocrText)

    fun checkBookSummary(output: BookSummaryOutput): List<Violation> {
        val violations = checkText("summary", output.summary, MAX_BOOK_SUMMARY_LENGTH, requireTentative = true) +
            checkText("reading_style", output.readingStyle, MAX_READING_STYLE_LENGTH, requireTentative = false) +
            checkText("next_question", output.nextQuestion, MAX_QUESTION_LENGTH, requireTentative = false)
        val pointViolations = output.points.flatMap {
            checkText("points", it, MAX_POINT_LENGTH, requireTentative = false)
        }
        return violations + pointViolations
    }

    fun checkChat(reply: String): List<Violation> =
        checkText("reply", reply, MAX_CHAT_LENGTH, requireTentative = false)

    /** Instructions appended to the prompt when regenerating. */
    fun feedback(violations: List<Violation>): String = buildString {
        appendLine("【前回の出力の問題点（必ず直してください）】")
        violations.forEach { appendLine("- ${it.field}: ${it.detail}") }
    }

    /**
     * Length of the longest run of characters that appears in both texts,
     * ignoring whitespace (OCR inserts line breaks in the middle of sentences).
     */
    fun longestSharedRun(a: String, b: String): Int {
        val x = a.filterNot { it.isWhitespace() }
        val y = b.filterNot { it.isWhitespace() }
        if (x.isEmpty() || y.isEmpty()) return 0
        var previous = IntArray(y.length + 1)
        var current = IntArray(y.length + 1)
        var best = 0
        for (i in 1..x.length) {
            for (j in 1..y.length) {
                current[j] = if (x[i - 1] == y[j - 1]) previous[j - 1] + 1 else 0
                if (current[j] > best) best = current[j]
            }
            val swap = previous
            previous = current
            current = swap
            current.fill(0)
        }
        return best
    }
}
