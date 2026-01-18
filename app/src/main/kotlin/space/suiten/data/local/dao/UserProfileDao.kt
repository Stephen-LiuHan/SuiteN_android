package space.suiten.data.local.dao

import androidx.room.*
import space.suiten.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun getUserProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfileEntity)

    @Update
    suspend fun updateProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profiles WHERE updatedAt > :sinceTimestamp LIMIT 1")
    suspend fun getProfileUpdatedSince(sinceTimestamp: Long): UserProfileEntity?

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getProfileSync(): UserProfileEntity?
}
