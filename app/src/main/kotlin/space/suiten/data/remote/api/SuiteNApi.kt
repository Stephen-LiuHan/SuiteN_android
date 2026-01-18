package space.suiten.data.remote.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*
import space.suiten.data.remote.dto.*

interface SuiteNApi {

    // === Auth ===
    @POST("auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<RegisterResponse>>

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<LoginResponse>>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body request: RefreshTokenRequest
    ): Response<ApiResponse<RefreshTokenResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @DELETE("auth/account")
    suspend fun deleteAccount(): Response<Unit>

    // === Sync ===
    @POST("sync")
    suspend fun sync(
        @Body request: SyncRequest
    ): Response<ApiResponse<SyncResponse>>

    @GET("sync/status")
    suspend fun getSyncStatus(): Response<ApiResponse<SyncStatusResponse>>

    // === Backup ===
    @POST("backup/export")
    suspend fun exportBackup(): Response<ApiResponse<ExportResponse>>

    @Multipart
    @POST("backup/import")
    suspend fun importBackup(
        @Part file: MultipartBody.Part,
        @Part("mode") mode: RequestBody
    ): Response<ApiResponse<ImportResponse>>

    // === Individual Resources (Optional) ===
    @GET("books")
    suspend fun getBooks(
        @Query("limit") limit: Int = 50,
        @Query("cursor") cursor: String? = null,
        @Query("updated_since") updatedSince: Long? = null
    ): Response<ApiResponse<BooksListResponse>>

    @GET("books/{id}")
    suspend fun getBook(
        @Path("id") bookId: String
    ): Response<ApiResponse<BookSyncData>>

    @GET("books/{book_id}/sessions")
    suspend fun getBookSessions(
        @Path("book_id") bookId: String
    ): Response<ApiResponse<List<ReadingSessionSyncData>>>

    @GET("sessions/{session_id}/captures")
    suspend fun getSessionCaptures(
        @Path("session_id") sessionId: String
    ): Response<ApiResponse<List<CaptureSyncData>>>

    @GET("captures/{capture_id}/ai-result")
    suspend fun getCaptureAIResult(
        @Path("capture_id") captureId: String
    ): Response<ApiResponse<AIResultSyncData>>

    @GET("profile")
    suspend fun getProfile(): Response<ApiResponse<UserProfileSyncData>>

    // === Analytics ===
    @GET("analytics/personal")
    suspend fun getPersonalAnalytics(
        @Query("period") period: String = "30d"
    ): Response<ApiResponse<PersonalAnalyticsResponse>>

    @GET("analytics/global")
    suspend fun getGlobalAnalytics(): Response<ApiResponse<GlobalAnalyticsResponse>>

    @GET("analytics/trends")
    suspend fun getTrends(
        @Query("metric") metric: String,
        @Query("period") period: String = "30d",
        @Query("scope") scope: String = "personal"
    ): Response<ApiResponse<TrendsResponse>>
}

// Additional response types
data class BooksListResponse(
    val books: List<BookSyncData>,
    val next_cursor: String?,
    val has_more: Boolean
)
