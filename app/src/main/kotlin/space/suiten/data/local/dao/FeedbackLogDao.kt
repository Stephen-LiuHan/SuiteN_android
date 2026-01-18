package space.suiten.data.local.dao

import androidx.room.*
import space.suiten.data.local.entity.FeedbackLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackLogDao {
    @Query("SELECT * FROM feedback_logs WHERE captureId = :captureId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getFeedbackByCaptureId(captureId: String): FeedbackLogEntity?

    @Query("SELECT * FROM feedback_logs ORDER BY createdAt DESC")
    fun getAllFeedback(): Flow<List<FeedbackLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedback(feedback: FeedbackLogEntity)

    @Delete
    suspend fun deleteFeedback(feedback: FeedbackLogEntity)

    @Query("DELETE FROM feedback_logs")
    suspend fun deleteAllFeedback()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedbackLog(feedback: FeedbackLogEntity)

    @Query("DELETE FROM feedback_logs WHERE id = :id")
    suspend fun deleteFeedbackLogById(id: String)

    @Query("SELECT * FROM feedback_logs WHERE createdAt > :sinceTimestamp")
    suspend fun getFeedbackCreatedSince(sinceTimestamp: Long): List<FeedbackLogEntity>

    @Query("SELECT * FROM feedback_logs")
    suspend fun getAllFeedbackSync(): List<FeedbackLogEntity>
}
