package com.docuvio.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.core.auth.TokenManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _navigationDestination = MutableStateFlow<String?>(null)
    val navigationDestination: StateFlow<String?> = _navigationDestination

    init {
        checkAuthenticationStatus()
    }

    private fun checkAuthenticationStatus() {
        viewModelScope.launch {
            // Show splash screen for at least 2 seconds
            delay(2000)

            // Check if user is logged in
            val token = tokenManager.getTokenBlocking()

            _navigationDestination.value = if (token.isNullOrEmpty()) {
                "login"
            } else {
                "main"
            }
        }
    }
}