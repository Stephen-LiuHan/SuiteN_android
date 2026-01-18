package space.suiten.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "feedback_logs",
    foreignKeys = [
        ForeignKey(
            entity = CaptureEntity::class,
            parentColumns = ["id"],
            childColumns = ["captureId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("captureId")]
)
data class FeedbackLogEntity(
    @PrimaryKey
    val id: String,
    val captureId: String,
    val helpfulness: Int, // 1-3
    val createdAt: Long
)
