@file:OptIn(ExperimentalAnimationApi::class)

package com.docuvio.app.ui.navigation

import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.docuvio.app.di.AppContainer
import com.docuvio.app.theme.*
import com.docuvio.app.ui.auth.LoginScreen
import com.docuvio.app.ui.auth.SignupScreen
import com.docuvio.app.ui.home.HomeScreen
import com.docuvio.app.ui.order.schedulecomponents.CreateOrderScreen
import com.docuvio.app.ui.orders.OrdersScreen
import com.docuvio.app.ui.printsession.PrintSessionScreen
import com.docuvio.app.ui.profile.DeleteAccountScreen
import com.docuvio.app.ui.profile.FeedbackScreen
import com.docuvio.app.ui.profile.ProfileScreen
import com.docuvio.app.ui.qr.QRScannerScreen
import com.docuvio.app.ui.splash.SplashScreen
import com.docuvio.app.viewmodel.*
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import kotlinx.coroutines.launch

// ── Which routes are stack screens (pushed on top of tabs) ───
private val STACK_ROUTES = setOf(
    "createOrder",
    "qrScanner",
    "printSession",
    "delete_account",
    "feedback",
    "login",
    "signup",
    "deeplink"
)

private fun isStackRoute(route: String?) =
    STACK_ROUTES.any { route?.startsWith(it) == true }

// ── Tab order for direction detection ────────────────────────
private val TAB_ORDER = listOf("home", "orders", "profile")

private fun tabIndex(route: String?) =
    TAB_ORDER.indexOfFirst { route?.startsWith(it) == true }

// ── Stack animations ──────────────────────────────────────────
private fun stackEnter() = slideInHorizontally(
    initialOffsetX = { it }, animationSpec = tween(300)
) + fadeIn(animationSpec = tween(300))

private fun stackExit() = slideOutHorizontally(
    targetOffsetX = { -it / 3 }, animationSpec = tween(300)
) + fadeOut(animationSpec = tween(200))

private fun stackPopEnter() = slideInHorizontally(
    initialOffsetX = { -it / 3 }, animationSpec = tween(300)
) + fadeIn(animationSpec = tween(300))

private fun stackPopExit() = slideOutHorizontally(
    targetOffsetX = { it }, animationSpec = tween(300)
) + fadeOut(animationSpec = tween(200))

// ── Tab exit ──────────────────────────────────────────────────
private fun tabExit(fromRoute: String?, toRoute: String?) =
    if (isStackRoute(toRoute)) {
        stackExit()
    } else {
        val from = tabIndex(fromRoute)
        val to = tabIndex(toRoute)
        val direction = if (to > from) -1 else 1
        slideOutHorizontally(
            targetOffsetX = { (it * 0.18f * direction).toInt() },
            animationSpec = tween(260, easing = EaseInCubic)
        ) + fadeOut(animationSpec = tween(180, easing = EaseIn))
    }

// ── Tab enter ─────────────────────────────────────────────────
private fun tabEnter(fromRoute: String?, toRoute: String?) =
    if (isStackRoute(fromRoute)) {
        stackPopEnter()
    } else {
        val from = tabIndex(fromRoute)
        val to = tabIndex(toRoute)
        val direction = if (to > from) 1 else -1
        slideInHorizontally(
            initialOffsetX = { (it * 0.18f * direction).toInt() },
            animationSpec = tween(260, easing = EaseOutCubic)
        ) + fadeIn(animationSpec = tween(260, easing = EaseOut))
    }

@SuppressLint("UnrememberedGetBackStackEntry")
@OptIn(ExperimentalAnimationApi::class)
@androidx.camera.core.ExperimentalGetImage
@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {

    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier.background(Cream)
    ) {

        // ------------------------------------------------
        // Splash Screen
        // ------------------------------------------------

        composable(
            route = Routes.Splash.route,
            exitTransition = { fadeOut(animationSpec = tween(300)) }
        ) {
            SplashScreen(
                viewModelFactory = SplashViewModelFactory(
                    appContainer.tokenManager,
                    appContainer.orderRepository
                ),
                onNavigateToLogin = {
                    // Always go to Login screen, let the user choose to Resume or Login there
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Routes.Home.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                },
                onNavigateToOrderSuccess = { orderId ->
                    // Recovered a paid order! Take user to their orders list
                    navController.navigate(Routes.Orders.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToPrintSession = { token ->
                    // Recovered a guest session! Take them back to the session screen
                    navController.navigate(Routes.PrintSession.createRoute(token)) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // ------------------------------------------------
        // Auth Screens — stack slide
        // ------------------------------------------------

        composable(
            route = Routes.Login.route,
            enterTransition = { stackEnter() },
            exitTransition = { stackExit() },
            popEnterTransition = { stackPopEnter() },
            popExitTransition = { stackPopExit() }
        ) {
            LoginScreen(
                viewModelFactory = AuthViewModelFactory(appContainer),
                onLoginSuccess = {
                    navController.navigate(Routes.Home.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(Routes.Signup.route)
                },
                onNavigateToQRScanner = {
                    navController.navigate(Routes.QRScanner.route)
                },
                onResumeSession = { token ->
                    navController.navigate(Routes.PrintSession.createRoute(token))
                }
            )
        }

        composable(
            route = Routes.Signup.route,
            enterTransition = { stackEnter() },
            exitTransition = { stackExit() },
            popEnterTransition = { stackPopEnter() },
            popExitTransition = { stackPopExit() }
        ) {
            SignupScreen(
                viewModelFactory = AuthViewModelFactory(appContainer),
                onSignupSuccess = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Signup.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // ------------------------------------------------
        // Deep Link Handler
        // ------------------------------------------------

        composable(
            route = Routes.DeepLinkHandler.route,
            deepLinks = listOf(
                navDeepLink { uriPattern = "https://www.docuvio.co.in/print/shop/{shopCode}" },
                navDeepLink { uriPattern = "https://docuvio.co.in/print/shop/{shopCode}" },
                navDeepLink { uriPattern = "https://docuvio.in/print/shop/{shopCode}" }
            )
        ) { backStackEntry ->
            val shopCode = backStackEntry.arguments?.getString("shopCode")
            var error by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(shopCode) {
                if (shopCode != null) {
                    when (val result = appContainer.printSessionRepository.startSession(shopCode)) {
                        is com.docuvio.app.data.repository.Result.Success -> {
                            val token = result.data.activeToken
                            navController.navigate(Routes.PrintSession.createRoute(token)) {
                                popUpTo(Routes.DeepLinkHandler.route) { inclusive = true }
                            }
                        }
                        is com.docuvio.app.data.repository.Result.Error -> {
                            if (result.message.contains("active session", true)) {
                                val existingToken = appContainer.tokenManager.getGuestSessionTokenBlocking()
                                if (existingToken != null) {
                                    navController.navigate(Routes.PrintSession.createRoute(existingToken)) {
                                        popUpTo(Routes.DeepLinkHandler.route) { inclusive = true }
                                    }
                                } else {
                                    error = result.message
                                }
                            } else {
                                error = result.message
                            }
                        }
                        else -> {
                            error = "Failed to start session"
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (error != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text("Session Error", style = MaterialTheme.typography.titleMedium, color = AlmostBlack)
                        Spacer(Modifier.height(8.dp))
                        Text(error!!, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate(Routes.Login.route) { popUpTo(0) } },
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Go to Login")
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SuccessGreen)
                        Spacer(Modifier.height(16.dp))
                        Text("Connecting to Shop...", fontWeight = FontWeight.Medium, color = AlmostBlack)
                    }
                }
            }
        }

        // ------------------------------------------------
        // Bottom Nav Tabs — direction-aware
        // ------------------------------------------------

        composable(
            route = Routes.Home.route,
            enterTransition = {
                tabEnter(initialState.destination.route, targetState.destination.route)
            },
            exitTransition = {
                tabExit(initialState.destination.route, targetState.destination.route)
            },
            popEnterTransition = {
                tabEnter(initialState.destination.route, targetState.destination.route)
            },
            popExitTransition = {
                tabExit(initialState.destination.route, targetState.destination.route)
            }
        ) { backStackEntry ->

            val parentEntry = remember(navController) {
                navController.getBackStackEntry(Routes.Home.route)
            }

            val viewModel: HomeViewModel = viewModel(
                parentEntry,
                factory = HomeViewModelFactory(appContainer.shopRepository)
            )

            HomeScreen(
                viewModel = viewModel,
                tokenManager = appContainer.tokenManager,
                notificationApi = appContainer.notificationApi,
                onScheduleClick = { shopId ->
                    navController.navigate(Routes.CreateOrder.createRoute(shopId))
                },
                onQRScanClick = {
                    navController.navigate(Routes.QRScanner.route)
                },
                onResumeSession = { token ->
                    navController.navigate(Routes.PrintSession.createRoute(token))
                }
            )
        }

        composable(
            route = Routes.Orders.route,
            enterTransition = {
                tabEnter(initialState.destination.route, targetState.destination.route)
            },
            exitTransition = {
                tabExit(initialState.destination.route, targetState.destination.route)
            },
            popEnterTransition = {
                tabEnter(initialState.destination.route, targetState.destination.route)
            },
            popExitTransition = {
                tabExit(initialState.destination.route, targetState.destination.route)
            }
        ) { backStackEntry ->

            val parentEntry = remember(navController) {
                navController.getBackStackEntry(Routes.Orders.route)
            }

            val viewModel: OrdersViewModel = viewModel(
                parentEntry,
                factory = OrdersViewModelFactory(appContainer.orderRepository)
            )

            OrdersScreen(
                viewModel = viewModel
            )
        }

        composable(
            route = Routes.Profile.route,
            enterTransition = {
                tabEnter(initialState.destination.route, targetState.destination.route)
            },
            exitTransition = {
                tabExit(initialState.destination.route, targetState.destination.route)
            },
            popEnterTransition = {
                tabEnter(initialState.destination.route, targetState.destination.route)
            },
            popExitTransition = {
                tabExit(initialState.destination.route, targetState.destination.route)
            }
        ) { backStackEntry ->

            val viewModel: ProfileViewModel = viewModel(
                backStackEntry,
                factory = ProfileViewModelFactory(appContainer.authRepository)
            )

            ProfileScreen(
                viewModel = viewModel,
                tokenManager = appContainer.tokenManager,
                onLogout = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onDeleteClick = {
                    navController.navigate(Routes.DeleteAccount.route)
                },
                onFeedbackClick = {
                    navController.navigate(Routes.Feedback.route)
                }
            )
        }

        // ------------------------------------------------
        // Stack screens pushed from tabs — slide
        // ------------------------------------------------

        composable(
            route = Routes.QRScanner.route,
            enterTransition = { stackEnter() },
            exitTransition = { stackExit() },
            popEnterTransition = { stackPopEnter() },
            popExitTransition = { stackPopExit() }
        ) {
            val scope = rememberCoroutineScope()
            var navStatus by remember { mutableStateOf<String?>(null) }
            
            QRScannerScreen(
                onCodeScanned = { rawCode ->
                    scope.launch {
                        try {
                            // Extract shop code from URL (e.g. .../print/shop/code) or raw string
                            val publicCode = when {
                                rawCode.contains("/print/shop/") -> {
                                    rawCode.substringAfter("/print/shop/").split("?")[0].split("/")[0]
                                }
                                rawCode.startsWith("shop-") -> rawCode
                                else -> rawCode
                            }
                            
                            android.util.Log.d("NAV", "Extracted code: $publicCode from raw: $rawCode")
                            
                            if (publicCode.isBlank()) {
                                navStatus = "Invalid QR code"
                                return@launch
                            }

                            when (val result = appContainer.printSessionRepository.startSession(publicCode)) {
                                is com.docuvio.app.data.repository.Result.Success -> {
                                    val token = result.data.activeToken
                                    if (token.isBlank()) {
                                        navStatus = "Error: Session token missing from server response"
                                        return@launch
                                    }
                                    
                                    android.util.Log.d("NAV", "Navigating to session with token: $token")
                                    navController.navigate(Routes.PrintSession.createRoute(token)) {
                                        popUpTo(Routes.QRScanner.route) {
                                            inclusive = true
                                        }
                                    }
                                }
                                is com.docuvio.app.data.repository.Result.Error -> {
                                    if (result.message.contains("active session", true)) {
                                        val existingToken = appContainer.tokenManager.getGuestSessionTokenBlocking()
                                        if (existingToken != null) {
                                            navController.navigate(Routes.PrintSession.createRoute(existingToken)) {
                                                popUpTo(Routes.QRScanner.route) { inclusive = true }
                                            }
                                        } else {
                                            navStatus = result.message
                                        }
                                    } else {
                                        navStatus = result.message
                                    }
                                }
                                else -> {
                                    navStatus = "Failed to start session"
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NAV", "Scanner navigation crash", e)
                            val detail = e.message ?: e.javaClass.simpleName
                            navStatus = "App Error: $detail"
                        }
                    }
                },
                onBack = { navController.popBackStack() },
                statusMessage = navStatus
            )
        }

        composable(
            route = Routes.PrintSession.route,
            arguments = listOf(navArgument("sessionToken") { type = NavType.StringType }),
            enterTransition = { stackEnter() },
            exitTransition = { stackExit() },
            popEnterTransition = { stackPopEnter() },
            popExitTransition = { stackPopExit() }
        ) { backStackEntry ->
            val token = backStackEntry.arguments?.getString("sessionToken")
            PrintSessionScreen(
                viewModelFactory = PrintSessionViewModelFactory(
                    appContainer.printSessionRepository,
                    appContainer.tokenManager,
                    token
                ),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DeleteAccount.route,
            enterTransition = { stackEnter() },
            exitTransition = { stackExit() },
            popEnterTransition = { stackPopEnter() },
            popExitTransition = { stackPopExit() }
        ) {
            val activity = LocalContext.current as Activity
            DeleteAccountScreen(
                tokenManager = appContainer.tokenManager,
                activity = activity,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.Feedback.route,
            enterTransition = { stackEnter() },
            exitTransition = { stackExit() },
            popEnterTransition = { stackPopEnter() },
            popExitTransition = { stackPopExit() }
        ) {
            FeedbackScreen(
                tokenManager = appContainer.tokenManager,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CreateOrder.route,
            arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
            enterTransition = { stackEnter() },
            exitTransition = { stackExit() },
            popEnterTransition = { stackPopEnter() },
            popExitTransition = { stackPopExit() }
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId")
                ?: return@composable

            CreateOrderScreen(
                viewModelFactory = CreateOrderViewModelFactory(
                    appContainer.shopRepository,
                    appContainer.orderRepository,
                    appContainer.tokenManager,
                    shopId
                ),
                onOrderSuccess = {
                    // Navigate to Orders and CLEAR the stack Order screens (no saveState)
                    navController.navigate(Routes.Orders.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = false
                            saveState = false
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
