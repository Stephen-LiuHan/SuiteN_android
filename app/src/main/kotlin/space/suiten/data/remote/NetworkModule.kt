package space.suiten.data.remote

import android.content.Context
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import space.suiten.BuildConfig
import space.suiten.data.remote.api.SuiteNApi
import space.suiten.data.remote.auth.AuthInterceptor
import space.suiten.data.remote.auth.TokenAuthenticator
import space.suiten.data.remote.auth.TokenManager
import java.util.concurrent.TimeUnit

object NetworkModule {

    private const val BASE_URL_PROD = "https://suiten.space/v1/"
    private const val BASE_URL_DEV = "https://suiten.space/v1/"  // Use production for now

    private var tokenManager: TokenManager? = null
    private var api: SuiteNApi? = null

    fun init(context: Context) {
        if (tokenManager == null) {
            tokenManager = TokenManager(context.applicationContext)
        }
    }

    fun getTokenManager(): TokenManager {
        return tokenManager ?: throw IllegalStateException("NetworkModule not initialized. Call init() first.")
    }

    fun getApi(): SuiteNApi {
        if (api == null) {
            api = createApi()
        }
        return api!!
    }

    private fun createApi(): SuiteNApi {
        val tm = getTokenManager()

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tm))
            .addInterceptor(loggingInterceptor)
            .authenticator(TokenAuthenticator(tm) { getApi() })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val baseUrl = if (BuildConfig.DEBUG) BASE_URL_DEV else BASE_URL_PROD

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SuiteNApi::class.java)
    }

    // Reset for testing or logout
    fun reset() {
        api = null
    }
}
