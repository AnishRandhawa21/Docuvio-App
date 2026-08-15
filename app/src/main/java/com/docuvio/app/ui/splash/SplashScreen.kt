package com.docuvio.app.ui.splash

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuvio.app.viewmodel.SplashViewModelFactory
import com.docuvio.app.ui.main.DocuvioLoadingAnimation

@Composable
fun SplashScreen(
    viewModelFactory: SplashViewModelFactory,
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToOrderSuccess: (String) -> Unit,
    onNavigateToPrintSession: (String) -> Unit
) {
    val viewModel: SplashViewModel = viewModel(factory = viewModelFactory)
    val navigationDestination by viewModel.navigationDestination.collectAsState()

    LaunchedEffect(navigationDestination) {
        val destination = navigationDestination ?: return@LaunchedEffect
        
        when {
            destination == "login" -> onNavigateToLogin()
            destination == "main" -> onNavigateToMain()
            destination.startsWith("order_success_recovery/") -> {
                val orderId = destination.removePrefix("order_success_recovery/")
                onNavigateToOrderSuccess(orderId)
            }
            destination.startsWith("print_session_recovery/") -> {
                val token = destination.removePrefix("print_session_recovery/")
                onNavigateToPrintSession(token)
            }
        }
    }

    // Reuse the modern, unified loading animation
    DocuvioLoadingAnimation()
}
