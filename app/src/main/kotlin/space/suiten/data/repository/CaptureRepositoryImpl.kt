package space.suiten.data.repository

import space.suiten.data.local.dao.CaptureDao
import space.suiten.data.local.dao.ReadingSessionDao
import space.suiten.data.local.entity.CaptureEntity
import space.suiten.data.local.entity.ReadingSessionEntity
import space.suiten.domain.model.Capture
import space.suiten.domain.model.ReadingSession
import space.suiten.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CaptureRepositoryImpl(
    private val captureDao: CaptureDao,
    private val sessionDao: ReadingSessionDao
) : CaptureRepository {

    override fun getCapturesBySessionId(sessionId: String): Flow<List<Capture>> {
        return captureDao.getCapturesBySessionId(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getCapturesByBookId(bookId: String): Flow<List<Capture>> {
        return captureDao.getCapturesByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPendingCaptures(): Flow<List<Capture>> {
        return captureDao.getPendingCaptures().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecentCaptures(limit: Int): Flow<List<Capture>> {
        return captureDao.getRecentCaptures(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getCaptureById(id: String): Capture? {
        return captureDao.getCaptureById(id)?.toDomain()
    }

    override suspend fun createCapture(capture: Capture) {
        captureDao.insertCapture(CaptureEntity.fromDomain(capture))
    }

    override suspend fun updateCapture(capture: Capture) {
        captureDao.updateCapture(CaptureEntity.fromDomain(capture))
    }

    override suspend fun deleteCapture(id: String) {
        val capture = captureDao.getCaptureById(id)
        if (capture != null) {
            captureDao.deleteCapture(capture)
        }
    }

    override fun getSessionsByBookId(bookId: String): Flow<List<ReadingSession>> {
        return sessionDao.getSessionsByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSessionById(id: String): ReadingSession? {
        return sessionDao.getSessionById(id)?.toDomain()
    }

    override suspend fun getActiveSession(): ReadingSession? {
        return sessionDao.getActiveSession()?.toDomain()
    }

    override suspend fun createSession(session: ReadingSession) {
        sessionDao.insertSession(ReadingSessionEntity.fromDomain(session))
    }

    override suspend fun updateSession(session: ReadingSession) {
        sessionDao.updateSession(ReadingSessionEntity.fromDomain(session))
    }
}
