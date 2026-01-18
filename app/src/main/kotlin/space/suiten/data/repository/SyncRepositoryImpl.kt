package space.suiten.data.repository

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import space.suiten.data.local.SuiteNDatabase
import space.suiten.data.local.entity.*
import space.suiten.data.remote.NetworkModule
import space.suiten.data.remote.api.SuiteNApi
import space.suiten.data.remote.auth.TokenManager
import space.suiten.data.remote.dto.*
import space.suiten.domain.repository.SyncRepository
import space.suiten.domain.repository.SyncResult

class SyncRepositoryImpl(
    private val database: SuiteNDatabase,
    private val api: SuiteNApi = NetworkModule.getApi(),
    private val tokenManager: TokenManager = NetworkModule.getTokenManager()
) : SyncRepository {

    private val _isSyncing = MutableStateFlow(false)
    override val isSyncing: Flow<Boolean> = _isSyncing

    override val lastSyncAt: Flow<Long?> = tokenManager.accessTokenFlow.map {
        tokenManager.getLastSyncAt()
    }

    override suspend fun sync(): SyncResult {
        if (_isSyncing.value) {
            return SyncResult(false, null, error = "Sync already in progress")
        }

        _isSyncing.value = true

        return try {
            val lastSync = tokenManager.getLastSyncAt()
            val deviceId = tokenManager.getDeviceId()

            // Collect local changes since last sync
            val changes = collectLocalChanges(lastSync)

            val response = api.sync(
                SyncRequest(
                    lastSyncAt = lastSync,
                    deviceId = deviceId,
                    changes = changes
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data!!
                Log.d("SyncRepo", "Received serverChanges: ${data.serverChanges}")

                // Apply server changes to local database
                applyServerChanges(data.serverChanges)

                // Update last sync timestamp
                tokenManager.updateLastSyncAt(data.syncAt)

                SyncResult(
                    success = true,
                    syncedAt = data.syncAt,
                    conflicts = data.conflicts ?: emptyList()
                )
            } else {
                val error = response.body()?.error
                SyncResult(false, null, error = error?.message ?: "Sync failed")
            }
        } catch (e: Exception) {
            Log.e("SyncRepo", "Sync error: ${e.message}", e)
            SyncResult(false, null, error = e.message ?: "Sync error")
        } finally {
            _isSyncing.value = false
        }
    }

    override suspend fun getSyncStatus(): Result<SyncStatusResponse> {
        return try {
            val response = api.getSyncStatus()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(response.body()?.data!!)
            } else {
                Result.failure(Exception(response.body()?.error?.message ?: "Failed to get sync status"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun forceFullSync(): SyncResult {
        // Clear last sync timestamp to force full sync
        tokenManager.updateLastSyncAt(0)
        return sync()
    }

    private suspend fun collectLocalChanges(lastSyncAt: Long?): SyncChanges {
        val sinceTimestamp = lastSyncAt ?: 0L

        return SyncChanges(
            books = collectBookChanges(sinceTimestamp),
            readingSessions = collectReadingSessionChanges(sinceTimestamp),
            captures = collectCaptureChanges(sinceTimestamp),
            aiResults = collectAIResultChanges(sinceTimestamp),
            nextActions = null, // Server-generated, not synced from client
            feedbackLogs = collectFeedbackLogChanges(sinceTimestamp),
            actionLogs = null, // TODO: implement if needed
            userProfile = collectUserProfileChanges(sinceTimestamp)
        )
    }

    private suspend fun collectBookChanges(sinceTimestamp: Long): List<SyncItem<BookSyncData>> {
        val books = if (sinceTimestamp == 0L) {
            database.bookDao().getAllBooksSync()
        } else {
            database.bookDao().getBooksUpdatedSince(sinceTimestamp)
        }

        return books.map { book ->
            SyncItem(
                id = book.id,
                action = "upsert",
                data = BookSyncData(
                    title = book.title,
                    author = book.author,
                    summary = book.summary,
                    createdAt = book.createdAt,
                    updatedAt = book.updatedAt
                )
            )
        }
    }

    private suspend fun collectReadingSessionChanges(sinceTimestamp: Long): List<SyncItem<ReadingSessionSyncData>> {
        val sessions = if (sinceTimestamp == 0L) {
            database.readingSessionDao().getAllSessionsSync()
        } else {
            database.readingSessionDao().getSessionsUpdatedSince(sinceTimestamp)
        }

        return sessions.map { session ->
            SyncItem(
                id = session.id,
                action = "upsert",
                data = ReadingSessionSyncData(
                    bookId = session.bookId,
                    startedAt = session.startedAt,
                    endedAt = session.endedAt,
                    note = session.note,
                    createdAt = session.createdAt,
                    updatedAt = session.updatedAt
                )
            )
        }
    }

    private suspend fun collectCaptureChanges(sinceTimestamp: Long): List<SyncItem<CaptureSyncData>> {
        val captures = if (sinceTimestamp == 0L) {
            database.captureDao().getAllCapturesSync()
        } else {
            database.captureDao().getCapturesUpdatedSince(sinceTimestamp)
        }

        return captures.map { capture ->
            SyncItem(
                id = capture.id,
                action = "upsert",
                data = CaptureSyncData(
                    sessionId = capture.sessionId,
                    emotion = capture.emotion,
                    userMemo = capture.userMemo,
                    aiStatus = capture.aiStatus,
                    createdAt = capture.createdAt,
                    updatedAt = capture.updatedAt
                )
            )
        }
    }

    private suspend fun collectAIResultChanges(sinceTimestamp: Long): List<SyncItem<AIResultSyncData>> {
        val results = if (sinceTimestamp == 0L) {
            database.aiResultDao().getAllResultsSync()
        } else {
            database.aiResultDao().getResultsUpdatedSince(sinceTimestamp)
        }

        val gson = com.google.gson.Gson()
        val listType = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type

        return results.map { result ->
            val topicTags: List<String> = try {
                gson.fromJson(result.topicTagsJson, listType) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            SyncItem(
                id = result.id,
                action = "upsert",
                data = AIResultSyncData(
                    captureId = result.captureId,
                    draftSummary = result.draftSummary,
                    deepeningQuestion = result.deepeningQuestion,
                    axisSelected = result.axisSelected,
                    topicTags = topicTags,
                    createdAt = result.createdAt,
                    updatedAt = result.updatedAt
                )
            )
        }
    }

    private suspend fun collectFeedbackLogChanges(sinceTimestamp: Long): List<SyncItem<FeedbackLogSyncData>> {
        val feedbacks = if (sinceTimestamp == 0L) {
            database.feedbackLogDao().getAllFeedbackSync()
        } else {
            database.feedbackLogDao().getFeedbackCreatedSince(sinceTimestamp)
        }

        return feedbacks.map { feedback ->
            SyncItem(
                id = feedback.id,
                action = "upsert",
                data = FeedbackLogSyncData(
                    captureId = feedback.captureId,
                    helpfulness = feedback.helpfulness,
                    createdAt = feedback.createdAt
                )
            )
        }
    }

    private suspend fun collectUserProfileChanges(sinceTimestamp: Long): SyncItem<UserProfileSyncData>? {
        val profile = if (sinceTimestamp == 0L) {
            database.userProfileDao().getProfileSync()
        } else {
            database.userProfileDao().getProfileUpdatedSince(sinceTimestamp)
        }

        if (profile == null) return null

        val gson = com.google.gson.Gson()
        val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Int>>() {}.type

        val axisPreferences: Map<String, Int> = try {
            gson.fromJson(profile.axisPreferencesJson, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        val actionPreferences: Map<String, Int> = try {
            gson.fromJson(profile.actionPreferencesJson, mapType) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

        return SyncItem(
            id = profile.id,
            action = "upsert",
            data = UserProfileSyncData(
                axisPreferences = axisPreferences,
                actionPreferences = actionPreferences
            )
        )
    }

    private suspend fun applyServerChanges(changes: SyncChangesResponse) {
        // Apply books
        changes.books?.forEach { book ->
            database.bookDao().insertBook(
                BookEntity(
                    id = book.id,
                    title = book.title,
                    author = book.author,
                    summary = book.summary,
                    createdAt = book.createdAt,
                    updatedAt = book.updatedAt
                )
            )
        }

        // Apply reading sessions
        changes.readingSessions?.forEach { session ->
            database.readingSessionDao().insertSession(
                ReadingSessionEntity(
                    id = session.id,
                    bookId = session.bookId,
                    startedAt = session.startedAt,
                    endedAt = session.endedAt,
                    note = session.note,
                    createdAt = session.createdAt,
                    updatedAt = session.updatedAt
                )
            )
        }

        // Apply captures (without image - images are local only)
        changes.captures?.forEach { capture ->
            val existingCapture = database.captureDao().getCaptureById(capture.id)
            if (existingCapture != null) {
                // Preserve existing imagePath
                val updatedCapture = existingCapture.copy(
                    sessionId = capture.sessionId,
                    emotion = capture.emotion,
                    userMemo = capture.userMemo,
                    aiStatus = capture.aiStatus,
                    createdAt = capture.createdAt,
                    updatedAt = capture.updatedAt
                )
                database.captureDao().updateCapture(updatedCapture)
            } else {
                // Insert new capture without imagePath
                database.captureDao().insertCapture(
                    CaptureEntity(
                        id = capture.id,
                        sessionId = capture.sessionId,
                        imagePath = "", // Images are not synced
                        emotion = capture.emotion,
                        userMemo = capture.userMemo,
                        aiStatus = capture.aiStatus,
                        createdAt = capture.createdAt,
                        updatedAt = capture.updatedAt
                    )
                )
            }
        }

        // Apply AI results
        changes.aiResults?.forEach { aiResult ->
            database.aiResultDao().insertResult(
                AIResultEntity(
                    id = aiResult.id,
                    captureId = aiResult.captureId,
                    draftSummary = aiResult.draftSummary,
                    deepeningQuestion = aiResult.deepeningQuestion,
                    axisSelected = aiResult.axisSelected,
                    topicTagsJson = com.google.gson.Gson().toJson(aiResult.topicTags),
                    createdAt = aiResult.createdAt,
                    updatedAt = aiResult.updatedAt
                )
            )
        }

        // Apply next actions
        changes.nextActions?.forEach { nextAction ->
            database.nextActionDao().insertNextAction(
                NextActionEntity(
                    id = nextAction.id,
                    aiResultId = nextAction.aiResultId,
                    type = nextAction.type,
                    title = nextAction.title,
                    description = nextAction.description ?: "",
                    query = nextAction.query,
                    link = nextAction.link,
                    rank = nextAction.rank
                )
            )
        }

        // Apply feedback logs
        changes.feedbackLogs?.forEach { feedbackLog ->
            database.feedbackLogDao().insertFeedbackLog(
                FeedbackLogEntity(
                    id = feedbackLog.id,
                    captureId = feedbackLog.captureId,
                    helpfulness = feedbackLog.helpfulness,
                    createdAt = feedbackLog.createdAt
                )
            )
        }

        // Apply user profile
        changes.userProfile?.let { profile ->
            val gson = com.google.gson.Gson()
            val entity = UserProfileEntity(
                id = "default",
                axisPreferencesJson = gson.toJson(profile.axisPreferences),
                actionPreferencesJson = gson.toJson(profile.actionPreferences),
                updatedAt = System.currentTimeMillis()
            )
            database.userProfileDao().insertProfile(entity)
        }
    }
}