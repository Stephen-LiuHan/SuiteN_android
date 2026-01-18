package space.suiten.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import space.suiten.domain.model.ReadingSession
import java.util.Date

@Entity(
    tableName = "reading_sessions",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class ReadingSessionEntity(
    @PrimaryKey
    val id: String,
    val bookId: String,
    val startedAt: Long,
    val endedAt: Long?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): ReadingSession = ReadingSession(
        id = id,
        bookId = bookId,
        startedAt = Date(startedAt),
        endedAt = endedAt?.let { Date(it) },
        note = note,
        createdAt = Date(createdAt),
        updatedAt = Date(updatedAt)
    )

    companion object {
        fun fromDomain(session: ReadingSession): ReadingSessionEntity = ReadingSessionEntity(
            id = session.id,
            bookId = session.bookId,
            startedAt = session.startedAt.time,
            endedAt = session.endedAt?.time,
            note = session.note,
            createdAt = session.createdAt.time,
            updatedAt = session.updatedAt.time
        )
    }
}
