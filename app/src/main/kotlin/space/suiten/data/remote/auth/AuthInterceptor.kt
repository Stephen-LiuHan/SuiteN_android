package space.suiten.data.remote.auth

import android.os.Build
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val tokenManager: TokenManager,
    private val clientVersion: String = "1.0.0"
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Skip auth header for auth endpoints (except logout)
        val path = originalRequest.url.encodedPath
        if (path.contains("/auth/") && !path.contains("/logout")) {
            return chain.proceed(
                originalRequest.newBuilder()
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .addHeader("X-Client-Version", clientVersion)
                    .build()
            )
        }

        val accessToken = tokenManager.getAccessTokenSync()

        val requestBuilder = originalRequest.newBuilder()
            .addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .addHeader("X-Client-Version", clientVersion)
            .addHeader("X-Device-ID", getDeviceIdSync())

        if (accessToken != null) {
            requestBuilder.addHeader("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }

    private fun getDeviceIdSync(): String {
        return "${Build.MANUFACTURER}_${Build.MODEL}_${Build.ID}".replace(" ", "_")
    }
}
