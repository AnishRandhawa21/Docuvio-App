package com.docuvio.app.core.network

import com.docuvio.app.BuildConfig
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.data.api.AuthApi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 🌐 ApiClient
 * Configures OkHttp and Retrofit for the application.
 * Handles automatic token injection and 401 Unauthorized handling.
 */
class ApiClient(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi?,
    private val onUnauthorized: () -> Unit
) {

    /**
     * 🔑 Auth Interceptor
     * Injects the Bearer token into the headers of every request.
     */
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()

        // 🛡️ Skip refresh and base auth endpoints to avoid loops
        val path = request.url.encodedPath
        if (path.endsWith("/auth/refresh") || path.endsWith("/auth/login") || path.endsWith("/auth/register")) {
            return@Interceptor chain.proceed(request)
        }

        val token = tokenManager.getTokenBlocking()

        val newRequest = request.newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addHeader("Authorization", "Bearer $token")
            }
        }.build()

        chain.proceed(newRequest)
    }

    /**
     * 📊 Logging Interceptor
     * Logs network activity in Debug builds.
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    /**
     * 🚀 OkHttpClient Configuration
     */
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val response = chain.proceed(chain.request())
            
            // 🚨 Detect a 401 response that has already been retried
            // If the Authenticator ran and failed, the response will still be 401.
            // We check if this is a "prior response" to see if we've already tried to fix it.
            if (response.code == 401) {
                val isRetry = response.priorResponse != null
                val isAuthPath = response.request.url.encodedPath.contains("/auth/")
                
                // If it's a 401 on a non-auth path and it's either the 2nd attempt 
                // OR the Authenticator wasn't even able to return a retry request.
                if (!isAuthPath && (isRetry || authApi == null)) {
                    android.util.Log.e("API", "🛑 Persistent 401 detected. Triggering logout.")
                    onUnauthorized()
                }
            }
            response
        }
        .apply {
            // 🔄 Attach the Authenticator if an AuthApi is provided
            authApi?.let {
                authenticator(AuthAuthenticator(tokenManager, it))
            }
        }
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    fun <T> createService(serviceClass: Class<T>): T {
        return retrofit.create(serviceClass)
    }

    inline fun <reified T> createService(): T {
        return createService(T::class.java)
    }
}
