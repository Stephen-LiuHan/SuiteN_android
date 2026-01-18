package space.suiten.data.local.dao

import androidx.room.*
import space.suiten.data.local.entity.CaptureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CaptureDao {
    @Query("SELECT * FROM captures WHERE sessionId = :sessionId ORDER BY createdAt DESC")
    fun getCapturesBySessionId(sessionId: String): Flow<List<CaptureEntity>>

    @Query("""
        SELECT c.* FROM captures c
        INNER JOIN reading_sessions s ON c.sessionId = s.id
        WHERE s.bookId = :bookId
        ORDER BY c.createdAt DESC
    """)
    fun getCapturesByBookId(bookId: String): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures WHERE id = :id")
    suspend fun getCaptureById(id: String): CaptureEntity?

    @Query("SELECT * FROM captures WHERE aiStatus = 'pending' ORDER BY createdAt ASC")
    fun getPendingCaptures(): Flow<List<CaptureEntity>>

    @Query("SELECT * FROM captures ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentCaptures(limit: Int): Flow<List<CaptureEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCapture(capture: CaptureEntity)

    @Update
    suspend fun updateCapture(capture: CaptureEntity)

    @Delete
    suspend fun deleteCapture(capture: CaptureEntity)

    @Query("DELETE FROM captures WHERE id = :id")
    suspend fun deleteCaptureById(id: String)

    @Query("SELECT * FROM captures WHERE updatedAt > :sinceTimestamp")
    suspend fun getCapturesUpdatedSince(sinceTimestamp: Long): List<CaptureEntity>

    @Query("SELECT * FROM captures")
    suspend fun getAllCapturesSync(): List<CaptureEntity>
}
