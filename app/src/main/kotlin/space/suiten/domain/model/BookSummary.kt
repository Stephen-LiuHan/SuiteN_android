package space.suiten.domain.model

import java.util.Date

/**
 * State changes for the book-level summary ("自分のまとめ").
 *
 * The North Star Metric is the monthly count of completed summaries. A summary is
 * complete when it was built from at least [MIN_CAPTURES] captures, the reader saved
 * or edited it, and rated it at least [MIN_HELPFULNESS]. [Book.summaryCompletedAt]
 * keeps the first completion, so each book counts once.
 */
object BookSummaryActions {
    /** One capture is a single reaction; the reader's way of thinking shows across several. */
    const val MIN_CAPTURES = 3
    const val MIN_HELPFULNESS = 2

    fun isComplete(book: Book): Boolean =
        book.summary != null &&
            book.summaryGeneratedAt != null &&
            (book.summaryCaptureCount ?: 0) >= MIN_CAPTURES &&
            (book.summarySavedAt != null || book.summaryEditedAt != null) &&
            (book.summaryHelpfulness ?: 0) >= MIN_HELPFULNESS

    /** A new summary replaces the old one, so the reader's save, edit and rating start over. */
    fun generated(book: Book, summary: String, captureCount: Int, now: Date = Date()): Book =
        book.copy(
            summary = summary,
            summaryGeneratedAt = now,
            summarySavedAt = null,
            summaryEditedAt = null,
            summaryHelpfulness = null,
            summaryCaptureCount = captureCount,
            updatedAt = now
        ).withCompletion(now)

    fun saved(book: Book, now: Date = Date()): Book =
        book.copy(summarySavedAt = now, updatedAt = now).withCompletion(now)

    fun edited(book: Book, summary: String, now: Date = Date()): Book =
        book.copy(summary = summary, summaryEditedAt = now, updatedAt = now).withCompletion(now)

    fun rated(book: Book, helpfulness: Int, now: Date = Date()): Book {
        require(helpfulness in 1..3) { "helpfulness must be 1-3, was $helpfulness" }
        return book.copy(summaryHelpfulness = helpfulness, updatedAt = now).withCompletion(now)
    }

    private fun Book.withCompletion(now: Date): Book =
        if (summaryCompletedAt == null && isComplete(this)) copy(summaryCompletedAt = now) else this
}
