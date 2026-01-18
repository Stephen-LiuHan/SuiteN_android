package space.suiten.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Common API response wrapper
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("data") val data: T?,
    @SerializedName("error") val error: ApiError?
)

data class ApiError(
    @SerializedName("code") val code: String,
    @SerializedName("message") val message: String,
    @SerializedName("details") val details: Map<String, Any>? = null
)
