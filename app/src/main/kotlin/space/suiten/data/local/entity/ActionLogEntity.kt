package space.suiten.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "action_logs",
    foreignKeys = [
        ForeignKey(
            entity = AIResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["aiResultId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("aiResultId")]
)
data class ActionLogEntity(
    @PrimaryKey
    val id: String,
    val aiResultId: String,
    val actionType: String,
    val opened: Boolean,
    val completed: Boolean,
    val createdAt: Long
)
