package space.suiten.data.remote.dto

import com.google.gson.annotations.SerializedName

// === Sync Request ===
data class SyncRequest(
    @SerializedName("last_sync_at") val lastSyncAt: Long?,
    @SerializedName("device_id") val deviceId: String,
    @SerializedName("changes") val changes: SyncChanges
)

data class SyncChanges(
    @SerializedName("books") val books: List<SyncItem<BookSyncData>>? = null,
    @SerializedName("reading_sessions") val readingSessions: List<SyncItem<ReadingSessionSyncData>>? = null,
    @SerializedName("captures") val captures: List<SyncItem<CaptureSyncData>>? = null,
    @SerializedName("ai_results") val aiResults: List<SyncItem<AIResultSyncData>>? = null,
    @SerializedName("next_actions") val nextActions: List<SyncItem<NextActionSyncData>>? = null,
    @SerializedName("feedback_logs") val feedbackLogs: List<SyncItem<FeedbackLogSyncData>>? = null,
    @SerializedName("action_logs") val actionLogs: List<SyncItem<ActionLogSyncData>>? = null,
    @SerializedName("user_profile") val userProfile: SyncItem<UserProfileSyncData>? = null
)

data class SyncItem<T>(
    @SerializedName("id") val id: String? = null,
    @SerializedName("action") val action: String, // "upsert" or "delete"
    @SerializedName("data") val data: T? = null
)

// === Sync Data Models ===
data class BookSyncData(
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long
)

data class ReadingSessionSyncData(
    @SerializedName("book_id") val bookId: String,
    @SerializedName("started_at") val startedAt: Long,
    @SerializedName("ended_at") val endedAt: Long?,
    @SerializedName("note") val note: String?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long
)

data class CaptureSyncData(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("emotion") val emotion: String?,
    @SerializedName("user_memo") val userMemo: String?,
    @SerializedName("ai_status") val aiStatus: String,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long
)

data class AIResultSyncData(
    @SerializedName("capture_id") val captureId: String,
    @SerializedName("draft_summary") val draftSummary: String,
    @SerializedName("deepening_question") val deepeningQuestion: String,
    @SerializedName("axis_selected") val axisSelected: String?,
    @SerializedName("topic_tags") val topicTags: List<String>,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long
)

data class NextActionSyncData(
    @SerializedName("ai_result_id") val aiResultId: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("query") val query: String?,
    @SerializedName("rank") val rank: Int
)

data class FeedbackLogSyncData(
    @SerializedName("capture_id") val captureId: String,
    @SerializedName("helpfulness") val helpfulness: Int,
    @SerializedName("created_at") val createdAt: Long
)

data class ActionLogSyncData(
    @SerializedName("ai_result_id") val aiResultId: String,
    @SerializedName("action_type") val actionType: String,
    @SerializedName("opened") val opened: Boolean,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("created_at") val createdAt: Long
)

data class UserProfileSyncData(
    @SerializedName("axis_preferences") val axisPreferences: Map<String, Int>,
    @SerializedName("action_preferences") val actionPreferences: Map<String, Int>
)

// === Sync Response ===
data class SyncResponse(
    @SerializedName("changes") val serverChanges: SyncChangesResponse,
    @SerializedName("server_timestamp") val syncAt: Long,
    @SerializedName("conflicts") val conflicts: List<SyncConflict>?
)

// Response structures from server (different from request structures)
data class SyncChangesResponse(
    @SerializedName("books") val books: List<BookResponse>? = null,
    @SerializedName("sessions") val readingSessions: List<SessionResponse>? = null,
    @SerializedName("captures") val captures: List<CaptureResponse>? = null,
    @SerializedName("ai_results") val aiResults: List<AIResultResponse>? = null,
    @SerializedName("next_actions") val nextActions: List<NextActionResponse>? = null,
    @SerializedName("feedback_logs") val feedbackLogs: List<FeedbackLogResponse>? = null,
    @SerializedName("action_logs") val actionLogs: List<ActionLogResponse>? = null,
    @SerializedName("user_profile") val userProfile: UserProfileSyncData? = null
)

data class SyncConflict(
    @SerializedName("table") val table: String,
    @SerializedName("id") val id: String,
    @SerializedName("client_version") val clientVersion: Map<String, Any>,
    @SerializedName("server_version") val serverVersion: Map<String, Any>
)

// === Sync Status ===
data class SyncStatusResponse(
    @SerializedName("last_sync_at") val lastSyncAt: Long?,
    @SerializedName("pending_changes") val pendingChanges: Int,
    @SerializedName("storage_used_bytes") val storageUsedBytes: Long,
    @SerializedName("storage_limit_bytes") val storageLimitBytes: Long
)

// === Sync Response Models (from server) ===
data class BookResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String?,
    @SerializedName("summary") val summary: String?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("deleted_at") val deletedAt: Long? = null
)

data class SessionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("book_id") val bookId: String,
    @SerializedName("started_at") val startedAt: Long,
    @SerializedName("ended_at") val endedAt: Long? = null,
    @SerializedName("note") val note: String?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("deleted_at") val deletedAt: Long? = null
)

data class CaptureResponse(
    @SerializedName("id") val id: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("emotion") val emotion: String?,
    @SerializedName("user_memo") val userMemo: String?,
    @SerializedName("ai_status") val aiStatus: String,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("deleted_at") val deletedAt: Long? = null
)

data class AIResultResponse(
    @SerializedName("id") val id: String,
    @SerializedName("capture_id") val captureId: String,
    @SerializedName("draft_summary") val draftSummary: String,
    @SerializedName("deepening_question") val deepeningQuestion: String,
    @SerializedName("axis_selected") val axisSelected: String?,
    @SerializedName("topic_tags") val topicTags: List<String>,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long,
    @SerializedName("deleted_at") val deletedAt: Long? = null
)

data class NextActionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("ai_result_id") val aiResultId: String,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("query") val query: String?,
    @SerializedName("link") val link: String?,
    @SerializedName("rank") val rank: Int,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("deleted_at") val deletedAt: Long? = null
)

data class FeedbackLogResponse(
    @SerializedName("id") val id: String,
    @SerializedName("capture_id") val captureId: String,
    @SerializedName("helpfulness") val helpfulness: Int,
    @SerializedName("created_at") val createdAt: Long
)

data class ActionLogResponse(
    @SerializedName("id") val id: String,
    @SerializedName("ai_result_id") val aiResultId: String,
    @SerializedName("action_type") val actionType: String,
    @SerializedName("opened") val opened: Boolean,
    @SerializedName("completed") val completed: Boolean,
    @SerializedName("created_at") val createdAt: Long
)
