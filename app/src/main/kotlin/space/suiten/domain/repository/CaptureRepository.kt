package space.suiten.domain.repository

import space.suiten.domain.model.Capture
import space.suiten.domain.model.ReadingSession
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun getCapturesBySessionId(sessionId: String): Flow<List<Capture>>
    fun getCapturesByBookId(bookId: String): Flow<List<Capture>>
    fun getPendingCaptures(): Flow<List<Capture>>
    fun getRecentCaptures(limit: Int): Flow<List<Capture>>
    suspend fun getCaptureById(id: String): Capture?
    suspend fun createCapture(capture: Capture)
    suspend fun updateCapture(capture: Capture)
    suspend fun deleteCapture(id: String)

    // Reading Session operations
    fun getSessionsByBookId(bookId: String): Flow<List<ReadingSession>>
    suspend fun getSessionById(id: String): ReadingSession?
    suspend fun getActiveSession(): ReadingSession?
    suspend fun createSession(session: ReadingSession)
    suspend fun updateSession(session: ReadingSession)
}
