package space.suiten.data.remote.auth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

class TokenManager(private val context: Context) {

    companion object {
        private val ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val TOKEN_EXPIRES_AT = longPreferencesKey("token_expires_at")
        private val USER_ID = stringPreferencesKey("user_id")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val DEVICE_ID = stringPreferencesKey("device_id")
        private val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshTokenFlow: Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val isLoggedInFlow: Flow<Boolean> = context.dataStore.data.map { it[ACCESS_TOKEN] != null }

    fun getAccessTokenSync(): String? = runBlocking {
        context.dataStore.data.first()[ACCESS_TOKEN]
    }

    fun getRefreshTokenSync(): String? = runBlocking {
        context.dataStore.data.first()[REFRESH_TOKEN]
    }

    suspend fun getAccessToken(): String? {
        return context.dataStore.data.first()[ACCESS_TOKEN]
    }

    suspend fun getRefreshToken(): String? {
        return context.dataStore.data.first()[REFRESH_TOKEN]
    }

    suspend fun getDeviceId(): String {
        val stored = context.dataStore.data.first()[DEVICE_ID]
        if (stored != null) return stored

        val newDeviceId = java.util.UUID.randomUUID().toString()
        context.dataStore.edit { it[DEVICE_ID] = newDeviceId }
        return newDeviceId
    }

    suspend fun getLastSyncAt(): Long? {
        return context.dataStore.data.first()[LAST_SYNC_AT]
    }

    suspend fun saveTokens(
        accessToken: String,
        refreshToken: String,
        expiresIn: Long,
        userId: String? = null,
        userEmail: String? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
            prefs[TOKEN_EXPIRES_AT] = System.currentTimeMillis() + (expiresIn * 1000)
            userId?.let { prefs[USER_ID] = it }
            userEmail?.let { prefs[USER_EMAIL] = it }
        }
    }

    suspend fun updateLastSyncAt(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_SYNC_AT] = timestamp
        }
    }

    suspend fun clearTokens() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
            prefs.remove(REFRESH_TOKEN)
            prefs.remove(TOKEN_EXPIRES_AT)
            prefs.remove(USER_ID)
            prefs.remove(USER_EMAIL)
            prefs.remove(LAST_SYNC_AT)
        }
    }

    suspend fun isTokenExpired(): Boolean {
        val expiresAt = context.dataStore.data.first()[TOKEN_EXPIRES_AT] ?: return true
        return System.currentTimeMillis() >= expiresAt - 60000 // 1 minute buffer
    }
}
