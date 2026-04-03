package com.docuvio.app.di

import android.content.Context
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.core.network.ApiClient
import com.docuvio.app.data.api.AuthApi
import com.docuvio.app.data.api.NotificationApi
import com.docuvio.app.data.api.OrderApi
import com.docuvio.app.data.api.ShopApi
import com.docuvio.app.data.repository.AuthRepository
import com.docuvio.app.data.repository.OrderRepository
import com.docuvio.app.data.repository.ShopRepository

class AppContainer(context: Context, onUnauthorized: () -> Unit) {

    val tokenManager = TokenManager(context)

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
    val notificationApi: NotificationApi = apiClient.createService()

    // Repositories
    val authRepository = AuthRepository(authApi, tokenManager, notificationApi)
    val shopRepository = ShopRepository(shopApi)
    val orderRepository = OrderRepository(orderApi)
}

/**
 * ⚠️ Temporary dummy (only used for base client init)
 */
fun dummyAuthApi(): AuthApi {
    throw IllegalStateException("AuthApi not initialized yet")
}