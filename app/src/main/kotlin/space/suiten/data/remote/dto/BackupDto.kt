package space.suiten.data.remote.dto

import com.google.gson.annotations.SerializedName

// === Export ===
data class ExportResponse(
    @SerializedName("export_url") val exportUrl: String,
    @SerializedName("expires_at") val expiresAt: Long
)

// === Import ===
data class ImportResponse(
    @SerializedName("imported_counts") val importedCounts: ImportCounts
)

data class ImportCounts(
    @SerializedName("books") val books: Int,
    @SerializedName("reading_sessions") val readingSessions: Int,
    @SerializedName("captures") val captures: Int,
    @SerializedName("ai_results") val aiResults: Int
)
