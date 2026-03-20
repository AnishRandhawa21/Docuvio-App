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

            // Optional splash delay (for animation)
            delay(2000)

            val isValidSession = tokenManager.isSessionValid()

            if (isValidSession) {
                _navigationDestination.value = "main"
            } else {
                tokenManager.clearAll() // clear expired session
                _navigationDestination.value = "login"
            }
        }
    }
}