package space.suiten.data.remote.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import space.suiten.data.remote.api.SuiteNApi
import space.suiten.data.remote.dto.RefreshTokenRequest

/**
 * Handles automatic token refresh when receiving 401 responses
 */
class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val apiProvider: () -> SuiteNApi
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        // Don't retry if we've already tried refreshing
        if (response.request.header("X-Token-Refreshed") != null) {
            return null
        }

        val refreshToken = tokenManager.getRefreshTokenSync() ?: return null

        return runBlocking {
            try {
                val refreshResponse = apiProvider().refreshToken(
                    RefreshTokenRequest(refreshToken)
                )

                if (refreshResponse.isSuccessful && refreshResponse.body()?.success == true) {
                    val data = refreshResponse.body()?.data!!
                    tokenManager.saveTokens(
                        accessToken = data.accessToken,
                        refreshToken = data.refreshToken,
                        expiresIn = data.expiresIn
                    )

                    response.request.newBuilder()
                        .header("Authorization", "Bearer ${data.accessToken}")
                        .header("X-Token-Refreshed", "true")
                        .build()
                } else {
                    // Refresh failed, clear tokens (user needs to login again)
                    tokenManager.clearTokens()
                    null
                }
            } catch (e: Exception) {
                tokenManager.clearTokens()
                null
            }
        }
    }
}
