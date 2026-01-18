package space.suiten.domain.model

import java.util.Date

data class UserProfile(
    val id: String,
    val axisPreferences: Map<String, Int>, // {"リスク": 10, ...}
    val actionPreferences: Map<String, Int>, // {"dialog": 5, ...}
    val updatedAt: Date
)
