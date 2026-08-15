package com.docuvio.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.ui.splash.SplashViewModel

class SplashViewModelFactory(
    private val tokenManager: TokenManager,
    private val orderRepository: com.docuvio.app.data.repository.OrderRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(tokenManager, orderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}