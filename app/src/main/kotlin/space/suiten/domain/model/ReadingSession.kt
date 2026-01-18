package space.suiten.domain.model

import java.util.Date

data class ReadingSession(
    val id: String,
    val bookId: String,
    val startedAt: Date,
    val endedAt: Date? = null,
    val note: String? = null,
    val createdAt: Date,
    val updatedAt: Date
)
