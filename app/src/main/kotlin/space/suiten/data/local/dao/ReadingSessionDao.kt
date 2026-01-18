package space.suiten.data.local.dao

import androidx.room.*
import space.suiten.data.local.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingSessionDao {
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId ORDER BY startedAt DESC")
    fun getSessionsByBookId(bookId: String): Flow<List<ReadingSessionEntity>>

    @Query("SELECT * FROM reading_sessions WHERE id = :id")
    suspend fun getSessionById(id: String): ReadingSessionEntity?

    @Query("SELECT * FROM reading_sessions WHERE endedAt IS NULL LIMIT 1")
    suspend fun getActiveSession(): ReadingSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity)

    @Update
    suspend fun updateSession(session: ReadingSessionEntity)

    @Delete
    suspend fun deleteSession(session: ReadingSessionEntity)

    @Query("DELETE FROM reading_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: String)

    @Query("SELECT * FROM reading_sessions WHERE updatedAt > :sinceTimestamp")
    suspend fun getSessionsUpdatedSince(sinceTimestamp: Long): List<ReadingSessionEntity>

    @Query("SELECT * FROM reading_sessions")
    suspend fun getAllSessionsSync(): List<ReadingSessionEntity>
}
