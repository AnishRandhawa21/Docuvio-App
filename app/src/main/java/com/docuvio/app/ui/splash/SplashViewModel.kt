package com.docuvio.app.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.docuvio.app.core.auth.TokenManager
import com.docuvio.app.data.repository.OrderRepository
import com.docuvio.app.data.repository.Result
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashViewModel(
    private val tokenManager: TokenManager,
    private val orderRepository: OrderRepository
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
            if (!isValidSession) {
                tokenManager.clearAll()
                _navigationDestination.value = "login"
                return@launch
            }

            // 🔐 VERIFICATION: If we have a user session, verify it's still active on the server
            // This prevents "optimistic" navigation to main with an expired/revoked session.
            val refreshToken = tokenManager.getRefreshTokenBlocking()
            if (!refreshToken.isNullOrBlank()) {
                // Try to fetch orders. This is a strictly authenticated endpoint.
                // If it fails with 401/403, we know the session is truly dead.
                val verificationResult = orderRepository.getOrders()
                if (verificationResult is Result.Error) {
                    if (verificationResult.message.contains("Session expired", ignoreCase = true)) {
                        android.util.Log.e("SPLASH", "🛑 Session verification failed: ${verificationResult.message}")
                        tokenManager.clearAll()
                        _navigationDestination.value = "login"
                        return@launch
                    }
                    // For other errors (network, 500), we proceed as the app handles offline/errors internally.
                }
            }

            // 🔍 RECONCILIATION: Check if we have an unverified paid order
            val pendingOrderId = tokenManager.getPendingOrderIdBlocking()
            if (pendingOrderId != null) {
                android.util.Log.d("SPLASH", "🔍 Found pending order: $pendingOrderId. Checking status...")
                
                // 🔥 Retry logic: Give the webhook up to 5 seconds to finish
                var orderFoundAndPaid = false
                repeat(3) { attempt ->
                    if (attempt > 0) delay(1500) // Wait between retries
                    
                    when (val result = orderRepository.getOrderStatus(pendingOrderId)) {
                        is Result.Success -> {
                            val order = result.data
                            if (order != null && order.isPaid) {
                                android.util.Log.d("SPLASH", "✅ Order $pendingOrderId is PAID. Redirecting to Success.")
                                tokenManager.savePendingOrderId(null)
                                orderFoundAndPaid = true
                                return@repeat
                            }
                        }
                        else -> {}
                    }
                }

                if (orderFoundAndPaid) {
                    val orderType = tokenManager.getPendingOrderTypeBlocking()
                    val guestToken = tokenManager.getGuestSessionTokenBlocking()

                    if (orderType == "print_session" && guestToken != null) {
                        // It was a guest print session! Take them back to the session view
                        _navigationDestination.value = "print_session_recovery/$guestToken"
                    } else {
                        // It was a standard scheduled order
                        _navigationDestination.value = "order_success_recovery/$pendingOrderId"
                    }
                    return@launch
                }
            }

            _navigationDestination.value = "main"
        }
    }
}
