package space.suiten.domain.model

import java.util.Date

data class AIResult(
    val id: String,
    val captureId: String,
    val draftSummary: String,
    val deepeningQuestion: String,
    val axisSelected: String? = null,
    val topicTags: List<String>,
    val createdAt: Date,
    val updatedAt: Date
)
