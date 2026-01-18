package space.suiten.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import space.suiten.domain.model.UserProfile
import java.util.Date

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey
    val id: String,
    val axisPreferencesJson: String, // JSON map
    val actionPreferencesJson: String, // JSON map
    val updatedAt: Long
) {
    fun toDomain(): UserProfile {
        val gson = Gson()
        val mapType = object : TypeToken<Map<String, Int>>() {}.type
        val axisPreferences = gson.fromJson<Map<String, Int>>(axisPreferencesJson, mapType)
        val actionPreferences = gson.fromJson<Map<String, Int>>(actionPreferencesJson, mapType)

        return UserProfile(
            id = id,
            axisPreferences = axisPreferences,
            actionPreferences = actionPreferences,
            updatedAt = Date(updatedAt)
        )
    }

    companion object {
        fun fromDomain(userProfile: UserProfile): UserProfileEntity {
            val gson = Gson()
            return UserProfileEntity(
                id = userProfile.id,
                axisPreferencesJson = gson.toJson(userProfile.axisPreferences),
                actionPreferencesJson = gson.toJson(userProfile.actionPreferences),
                updatedAt = userProfile.updatedAt.time
            )
        }
    }
}
