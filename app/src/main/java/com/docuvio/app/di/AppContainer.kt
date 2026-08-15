package com.docuvio.app.di

import android.content.Context
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.core.network.ApiClient
import com.docuvio.app.data.api.*
import com.docuvio.app.data.repository.*
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

class AppContainer(context: Context, onUnauthorized: () -> Unit) {

    val tokenManager = TokenManager(context)

    // 🔹 Supabase Client
    val supabase = createSupabaseClient(
        supabaseUrl = com.docuvio.app.BuildConfig.SUPABASE_URL,
        supabaseKey = com.docuvio.app.BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Realtime)
        
        // 🔥 CRITICAL: Handle extra fields from DB without crashing
        defaultSerializer = KotlinXSerializer(Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            encodeDefaults = true
        })
    }

    // 🔹 1. Base client (NO authenticator)
    private val baseApiClient = ApiClient(
        tokenManager,
        authApi = null,
        onUnauthorized = onUnauthorized
    )

    // 🔹 2. Auth API (login, signup)
    private val authApi: AuthApi = baseApiClient.createService()

    // 🔥 3. REFRESH CLIENT (IMPORTANT - CLEAN CLIENT)
    private val refreshClient = ApiClient(
        tokenManager = tokenManager,
        authApi = null, // ❗ no authenticator
        onUnauthorized = onUnauthorized
    )

    // 🔥 4. Refresh API (used ONLY for token refresh)
    private val refreshAuthApi: AuthApi = refreshClient.createService()

    // 🔹 5. Main client (WITH authenticator)
    private val apiClient = ApiClient(
        tokenManager,
        refreshAuthApi, // ✅ IMPORTANT FIX
        onUnauthorized
    )

    // APIs
    private val shopApi: ShopApi = apiClient.createService()
    private val orderApi: OrderApi = apiClient.createService()
    private val printSessionApi: PrintSessionApi = apiClient.createService()
    val notificationApi: NotificationApi = apiClient.createService()

    // Repositories
    val authRepository = AuthRepository(authApi, tokenManager, notificationApi)
    val shopRepository = ShopRepository(shopApi, supabase)
    val orderRepository = OrderRepository(orderApi)
    val printSessionRepository = PrintSessionRepository(printSessionApi, supabase)
}

/**
 * ⚠️ Temporary dummy (only used for base client init)
 */
fun dummyAuthApi(): AuthApi {
    throw IllegalStateException("AuthApi not initialized yet")
}