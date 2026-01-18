package space.suiten.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import space.suiten.domain.model.AIStatus
import space.suiten.domain.model.Capture
import java.util.Date

@Entity(
    tableName = "captures",
    foreignKeys = [
        ForeignKey(
            entity = ReadingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class CaptureEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val imagePath: String,
    val emotion: String?,
    val userMemo: String?,
    val aiStatus: String, // pending/running/done/error
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): Capture = Capture(
        id = id,
        sessionId = sessionId,
        imagePath = imagePath,
        emotion = emotion,
        userMemo = userMemo,
        aiStatus = AIStatus.fromString(aiStatus),
        createdAt = Date(createdAt),
        updatedAt = Date(updatedAt)
    )

    companion object {
        fun fromDomain(capture: Capture): CaptureEntity = CaptureEntity(
            id = capture.id,
            sessionId = capture.sessionId,
            imagePath = capture.imagePath,
            emotion = capture.emotion,
            userMemo = capture.userMemo,
            aiStatus = capture.aiStatus.value,
            createdAt = capture.createdAt.time,
            updatedAt = capture.updatedAt.time
        )
    }
}
