package space.suiten.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import space.suiten.domain.model.AIResult
import java.util.Date

@Entity(
    tableName = "ai_results",
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
data class AIResultEntity(
    @PrimaryKey
    val id: String,
    val captureId: String,
    val draftSummary: String,
    val deepeningQuestion: String,
    val axisSelected: String?,
    val topicTagsJson: String, // JSON array
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomain(): AIResult {
        val gson = Gson()
        val listType = object : TypeToken<List<String>>() {}.type
        val topicTags = gson.fromJson<List<String>>(topicTagsJson, listType)

        return AIResult(
            id = id,
            captureId = captureId,
            draftSummary = draftSummary,
            deepeningQuestion = deepeningQuestion,
            axisSelected = axisSelected,
            topicTags = topicTags,
            createdAt = Date(createdAt),
            updatedAt = Date(updatedAt)
        )
    }

    companion object {
        fun fromDomain(aiResult: AIResult): AIResultEntity {
            val gson = Gson()
            return AIResultEntity(
                id = aiResult.id,
                captureId = aiResult.captureId,
                draftSummary = aiResult.draftSummary,
                deepeningQuestion = aiResult.deepeningQuestion,
                axisSelected = aiResult.axisSelected,
                topicTagsJson = gson.toJson(aiResult.topicTags),
                createdAt = aiResult.createdAt.time,
                updatedAt = aiResult.updatedAt.time
            )
        }
    }
}
