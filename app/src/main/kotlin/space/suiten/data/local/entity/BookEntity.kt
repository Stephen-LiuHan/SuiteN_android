package space.suiten.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import space.suiten.domain.model.Book
import space.suiten.domain.model.ContentType
import java.util.Date

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val author: String?,
    val summary: String?,
    val contentType: String = "BOOK",
    val customTypeName: String? = null,
    val url: String? = null,
    val memo: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val summaryGeneratedAt: Long? = null,
    val summarySavedAt: Long? = null,
    val summaryEditedAt: Long? = null,
    val summaryHelpfulness: Int? = null,
    val summaryCaptureCount: Int? = null,
    val summaryCompletedAt: Long? = null
) {
    fun toDomain(): Book = Book(
        id = id,
        title = title,
        author = author,
        summary = summary,
        contentType = try { ContentType.valueOf(contentType) } catch (e: Exception) { ContentType.BOOK },
        customTypeName = customTypeName,
        url = url,
        memo = memo,
        createdAt = Date(createdAt),
        updatedAt = Date(updatedAt),
        summaryGeneratedAt = summaryGeneratedAt?.let { Date(it) },
        summarySavedAt = summarySavedAt?.let { Date(it) },
        summaryEditedAt = summaryEditedAt?.let { Date(it) },
        summaryHelpfulness = summaryHelpfulness,
        summaryCaptureCount = summaryCaptureCount,
        summaryCompletedAt = summaryCompletedAt?.let { Date(it) }
    )

    companion object {
        fun fromDomain(book: Book): BookEntity = BookEntity(
            id = book.id,
            title = book.title,
            author = book.author,
            summary = book.summary,
            contentType = book.contentType.name,
            customTypeName = book.customTypeName,
            url = book.url,
            memo = book.memo,
            createdAt = book.createdAt.time,
            updatedAt = book.updatedAt.time,
            summaryGeneratedAt = book.summaryGeneratedAt?.time,
            summarySavedAt = book.summarySavedAt?.time,
            summaryEditedAt = book.summaryEditedAt?.time,
            summaryHelpfulness = book.summaryHelpfulness,
            summaryCaptureCount = book.summaryCaptureCount,
            summaryCompletedAt = book.summaryCompletedAt?.time
        )
    }
}
