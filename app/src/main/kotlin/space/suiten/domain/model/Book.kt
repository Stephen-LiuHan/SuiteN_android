package space.suiten.domain.model

import java.util.Date

/**
 * Content type for items (books, videos, etc.)
 */
enum class ContentType {
    BOOK,    // 本
    VIDEO,   // 動画
    SEMINAR, // セミナー
    OTHER    // その他（自由入力）
}

data class Book(
    val id: String,
    val title: String,
    val author: String? = null,
    val summary: String? = null,
    val contentType: ContentType = ContentType.BOOK,
    val customTypeName: String? = null, // For OTHER type
    val url: String? = null,
    val memo: String? = null,
    val createdAt: Date,
    val updatedAt: Date,
    // Book-level summary metrics (see BookSummaryActions)
    val summaryGeneratedAt: Date? = null,
    val summarySavedAt: Date? = null,
    val summaryEditedAt: Date? = null,
    val summaryHelpfulness: Int? = null,
    val summaryCaptureCount: Int? = null,
    val summaryCompletedAt: Date? = null
) {
    /**
     * Get display name for content type
     */
    fun getTypeDisplayName(): String = when (contentType) {
        ContentType.BOOK -> "本"
        ContentType.VIDEO -> "動画"
        ContentType.SEMINAR -> "セミナー"
        ContentType.OTHER -> customTypeName ?: "その他"
    }
}
