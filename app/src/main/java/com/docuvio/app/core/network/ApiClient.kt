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
 * Industry Level networking with robust token management and automated recovery.
 */
class ApiClient(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi?,
    private val onUnauthorized: () -> Unit
) {

    /**
     * 🔑 Auth Interceptor
     * Injects the latest Bearer token from local storage.
     */
    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()

        // 🛡️ Skip specific paths that don't need auth or handle it internally
        val path = request.url.encodedPath
        if (path.contains("/auth/refresh") || path.contains("/auth/login") || path.contains("/auth/register")) {
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
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    /**
     * 🚀 OkHttpClient
     */
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            
            // 🚨 Detecting unauthorized state
            if (response.code == 401) {
                val path = request.url.encodedPath
                
                // If it's not a base auth path
                if (!path.contains("/auth/login") && !path.contains("/auth/register")) {
                    val isRetry = response.priorResponse != null
                    
                    // 🔥 If we still have a 401 after an Authenticator retry, or if it was 
                    // a path where we don't even have an authenticator (like refresh itself)
                    if (isRetry || authApi == null || path.contains("/auth/refresh")) {
                        android.util.Log.e("API", "🛑 Unauthorized. Session expired at $path")
                        onUnauthorized()
                    }
                }
            }
            response
        }
        .apply {
            // 🔄 Attach Authenticator only if AuthApi is available (main client)
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
