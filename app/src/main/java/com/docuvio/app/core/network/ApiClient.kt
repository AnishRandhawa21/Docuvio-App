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

class ApiClient(
    private val tokenManager: TokenManager,
    private val authApi: AuthApi?,
    private val onUnauthorized: () -> Unit
) {

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request()

        // 🔥 Skip refresh endpoint
        if (request.url.encodedPath.endsWith("/auth/refresh")) {
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

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .apply {
            authApi?.let {
                authenticator(AuthAuthenticator(tokenManager, it))
            }
        }
        .addInterceptor(loggingInterceptor)
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
