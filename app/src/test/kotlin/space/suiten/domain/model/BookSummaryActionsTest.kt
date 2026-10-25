package space.suiten.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class BookSummaryActionsTest {
    private val created = Date(1_760_000_000_000)
    private val book = Book(id = "b1", title = "小さく始める新規事業", createdAt = created, updatedAt = created)

    private fun at(minutes: Long) = Date(created.time + minutes * 60_000)

    @Test
    fun `summary from three captures, saved and rated 2, is complete`() {
        var b = BookSummaryActions.generated(book, "まとめ", captureCount = 3, now = at(1))
        assertNull(b.summaryCompletedAt)

        b = BookSummaryActions.saved(b, now = at(2))
        assertNull("not rated yet", b.summaryCompletedAt)

        b = BookSummaryActions.rated(b, helpfulness = 2, now = at(3))
        assertTrue(BookSummaryActions.isComplete(b))
        assertEquals(at(3), b.summaryCompletedAt)
    }

    @Test
    fun `editing counts the same as saving`() {
        var b = BookSummaryActions.generated(book, "まとめ", captureCount = 4, now = at(1))
        b = BookSummaryActions.rated(b, helpfulness = 3, now = at(2))
        b = BookSummaryActions.edited(b, "自分で直したまとめ", now = at(3))
        assertEquals("自分で直したまとめ", b.summary)
        assertEquals(at(3), b.summaryCompletedAt)
    }

    @Test
    fun `two captures or a low rating is not complete`() {
        val twoCaptures = BookSummaryActions.generated(book, "まとめ", captureCount = 2, now = at(1))
            .let { BookSummaryActions.saved(it, at(2)) }
            .let { BookSummaryActions.rated(it, 3, at(3)) }
        assertFalse(BookSummaryActions.isComplete(twoCaptures))

        val lowRating = BookSummaryActions.generated(book, "まとめ", captureCount = 5, now = at(1))
            .let { BookSummaryActions.saved(it, at(2)) }
            .let { BookSummaryActions.rated(it, 1, at(3)) }
        assertFalse(BookSummaryActions.isComplete(lowRating))
        assertNull(lowRating.summaryCompletedAt)
    }

    @Test
    fun `regenerating resets the reader's actions but keeps the first completion`() {
        val complete = BookSummaryActions.generated(book, "まとめ", 3, at(1))
            .let { BookSummaryActions.saved(it, at(2)) }
            .let { BookSummaryActions.rated(it, 3, at(3)) }

        val regenerated = BookSummaryActions.generated(complete, "新しいまとめ", 6, at(10))

        assertNull(regenerated.summarySavedAt)
        assertNull(regenerated.summaryHelpfulness)
        assertEquals(6, regenerated.summaryCaptureCount)
        assertEquals("the book still counts once, in its first month", at(3), regenerated.summaryCompletedAt)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rating outside 1 to 3 is rejected`() {
        BookSummaryActions.rated(book, 4)
    }

    @Test
    fun `generated sets capture count and generation time`() {
        val b = BookSummaryActions.generated(book, "まとめ", 3, at(5))
        assertNotNull(b.summaryGeneratedAt)
        assertEquals(at(5), b.updatedAt)
    }
}
