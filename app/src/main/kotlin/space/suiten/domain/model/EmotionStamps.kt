package space.suiten.domain.model

data class EmotionStamp(val emoji: String, val label: String)

/**
 * Emotion stamps a reader can put on a capture.
 * A capture can have several; they are stored comma-separated (e.g. "🤔,❤️").
 * Doubt (🤔) and empathy (❤️) both matter: doubts show what the reader worries about,
 * empathy shows what they believe, and the book summary surfaces both.
 */
object EmotionStamps {
    val ALL = listOf(
        EmotionStamp("💡", "ひらめき"),
        EmotionStamp("🤔", "疑問"),
        EmotionStamp("⚠️", "注意"),
        EmotionStamp("🔥", "重要"),
        EmotionStamp("❤️", "共感")
    )

    private const val SEPARATOR = ","

    /** Joins selected emojis for storage, or null when none are selected. */
    fun encode(emojis: List<String>): String? =
        emojis.filter { it.isNotBlank() }.distinct().joinToString(SEPARATOR).ifEmpty { null }

    /** Splits a stored value back into emojis. */
    fun decode(stored: String?): List<String> =
        stored?.split(SEPARATOR)?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

    /** Emojis separated by spaces, for display. */
    fun display(stored: String?): String = decode(stored).joinToString(" ")

    /** Labels such as "疑問・共感", for prompts and accessibility. */
    fun labels(stored: String?): String =
        decode(stored).map { emoji -> ALL.find { it.emoji == emoji }?.label ?: emoji }.joinToString("・")
}
