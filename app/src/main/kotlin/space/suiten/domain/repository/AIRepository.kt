package space.suiten.domain.repository

import space.suiten.domain.model.AIResult
import space.suiten.domain.model.NextAction
import space.suiten.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AIRepository {
    suspend fun getResultByCaptureId(captureId: String): AIResult?
    suspend fun getResultById(id: String): AIResult?
    fun getRecentResults(limit: Int): Flow<List<AIResult>>
    suspend fun createAIResult(result: AIResult, nextActions: List<NextAction>)
    suspend fun updateAIResult(result: AIResult)
    suspend fun deleteResultByCaptureId(captureId: String)

    // Next Actions
    fun getActionsByResultId(resultId: String): Flow<List<NextAction>>

    // User Profile
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun updateUserProfile(profile: UserProfile)

    // Feedback
    suspend fun saveFeedback(captureId: String, helpfulness: Int)
    suspend fun getFeedback(captureId: String): Int?
}
