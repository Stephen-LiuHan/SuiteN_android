package space.suiten.data.local.dao

import androidx.room.*
import space.suiten.data.local.entity.AIResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIResultDao {
    @Query("SELECT * FROM ai_results WHERE captureId = :captureId")
    suspend fun getResultByCaptureId(captureId: String): AIResultEntity?

    @Query("SELECT * FROM ai_results WHERE id = :id")
    suspend fun getResultById(id: String): AIResultEntity?

    @Query("SELECT * FROM ai_results ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentResults(limit: Int): Flow<List<AIResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: AIResultEntity)

    @Update
    suspend fun updateResult(result: AIResultEntity)

    @Delete
    suspend fun deleteResult(result: AIResultEntity)

    @Query("DELETE FROM ai_results WHERE id = :id")
    suspend fun deleteAIResultById(id: String)

    @Query("SELECT * FROM ai_results WHERE updatedAt > :sinceTimestamp")
    suspend fun getResultsUpdatedSince(sinceTimestamp: Long): List<AIResultEntity>

    @Query("SELECT * FROM ai_results")
    suspend fun getAllResultsSync(): List<AIResultEntity>
}
