package space.suiten.data.repository

import android.os.Build
import kotlinx.coroutines.flow.Flow
import space.suiten.data.remote.NetworkModule
import space.suiten.data.remote.api.SuiteNApi
import space.suiten.data.remote.auth.TokenManager
import space.suiten.data.remote.dto.LoginRequest
import space.suiten.data.remote.dto.RegisterRequest
import space.suiten.domain.repository.AuthRepository

class AuthRepositoryImpl(
    private val api: SuiteNApi = NetworkModule.getApi(),
    private val tokenManager: TokenManager = NetworkModule.getTokenManager()
) : AuthRepository {

    override val isLoggedIn: Flow<Boolean> = tokenManager.isLoggedInFlow

    override suspend fun register(email: String, password: String): Result<Unit> {
        return try {
            val response = api.register(RegisterRequest(email, password))

            if (response.isSuccessful && response.body()?.success == true) {
                // Auto-login after registration
                login(email, password)
            } else {
                val error = response.body()?.error
                Result.failure(Exception(error?.message ?: "Registration failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
            val deviceId = tokenManager.getDeviceId()
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

            val response = api.login(
                LoginRequest(
                    email = email,
                    password = password,
                    deviceId = deviceId,
                    deviceName = deviceName
                )
            )

            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data!!
                tokenManager.saveTokens(
                    accessToken = data.accessToken,
                    refreshToken = data.refreshToken,
                    expiresIn = data.expiresIn,
                    userId = data.user.userId,
                    userEmail = data.user.email
                )
                Result.success(Unit)
            } else {
                val error = response.body()?.error
                Result.failure(Exception(error?.message ?: "Login failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        return try {
            api.logout()
            tokenManager.clearTokens()
            NetworkModule.reset()
            Result.success(Unit)
        } catch (e: Exception) {
            // Clear tokens even if API call fails
            tokenManager.clearTokens()
            NetworkModule.reset()
            Result.success(Unit)
        }
    }

    override suspend fun deleteAccount(): Result<Unit> {
        return try {
            val response = api.deleteAccount()

            if (response.isSuccessful) {
                tokenManager.clearTokens()
                NetworkModule.reset()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete account"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
