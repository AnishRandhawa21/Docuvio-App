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

    private val apiClient = ApiClient(tokenManager, onUnauthorized)

    // APIs
    private val authApi: AuthApi = apiClient.createService()
    private val shopApi: ShopApi = apiClient.createService()
    private val orderApi: OrderApi = apiClient.createService()
    val notificationApi: NotificationApi = apiClient.createService()  // ✅ FIXED

    // Repositories
    val authRepository = AuthRepository(authApi, tokenManager)
    val shopRepository = ShopRepository(shopApi)
    val orderRepository = OrderRepository(orderApi)
}