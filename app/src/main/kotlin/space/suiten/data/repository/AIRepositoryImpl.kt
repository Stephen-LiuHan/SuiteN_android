package space.suiten.data.repository

import space.suiten.data.local.dao.AIResultDao
import space.suiten.data.local.dao.FeedbackLogDao
import space.suiten.data.local.dao.NextActionDao
import space.suiten.data.local.dao.UserProfileDao
import space.suiten.data.local.entity.AIResultEntity
import space.suiten.data.local.entity.FeedbackLogEntity
import space.suiten.data.local.entity.NextActionEntity
import space.suiten.data.local.entity.UserProfileEntity
import space.suiten.domain.model.AIResult
import space.suiten.domain.model.NextAction
import space.suiten.domain.model.UserProfile
import space.suiten.domain.repository.AIRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class AIRepositoryImpl(
    private val aiResultDao: AIResultDao,
    private val nextActionDao: NextActionDao,
    private val userProfileDao: UserProfileDao,
    private val feedbackLogDao: FeedbackLogDao
) : AIRepository {

    override suspend fun getResultByCaptureId(captureId: String): AIResult? {
        return aiResultDao.getResultByCaptureId(captureId)?.toDomain()
    }

    override suspend fun getResultById(id: String): AIResult? {
        return aiResultDao.getResultById(id)?.toDomain()
    }

    override fun getRecentResults(limit: Int): Flow<List<AIResult>> {
        return aiResultDao.getRecentResults(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun createAIResult(result: AIResult, nextActions: List<NextAction>) {
        aiResultDao.insertResult(AIResultEntity.fromDomain(result))
        if (nextActions.isNotEmpty()) {
            nextActionDao.insertActions(nextActions.map { NextActionEntity.fromDomain(it) })
        }
    }

    override suspend fun updateAIResult(result: AIResult) {
        aiResultDao.updateResult(AIResultEntity.fromDomain(result))
    }

    override suspend fun deleteResultByCaptureId(captureId: String) {
        val result = aiResultDao.getResultByCaptureId(captureId)
        if (result != null) {
            // Delete related next actions first
            nextActionDao.deleteActionsByResultId(result.id)
            // Delete the AI result
            aiResultDao.deleteResult(result)
        }
    }

    override fun getActionsByResultId(resultId: String): Flow<List<NextAction>> {
        return nextActionDao.getActionsByResultId(resultId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getUserProfile(): Flow<UserProfile?> {
        return userProfileDao.getUserProfile().map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun updateUserProfile(profile: UserProfile) {
        userProfileDao.insertProfile(UserProfileEntity.fromDomain(profile))
    }

    override suspend fun saveFeedback(captureId: String, helpfulness: Int) {
        val feedback = FeedbackLogEntity(
            id = UUID.randomUUID().toString(),
            captureId = captureId,
            helpfulness = helpfulness,
            createdAt = System.currentTimeMillis()
        )
        feedbackLogDao.insertFeedback(feedback)
    }

    override suspend fun getFeedback(captureId: String): Int? {
        return feedbackLogDao.getFeedbackByCaptureId(captureId)?.helpfulness
    }
}
