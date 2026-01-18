package space.suiten.domain.model

import java.util.Date

data class Capture(
    val id: String,
    val sessionId: String,
    val imagePath: String,
    val emotion: String? = null,
    val userMemo: String? = null,
    val aiStatus: AIStatus,
    val createdAt: Date,
    val updatedAt: Date
)
