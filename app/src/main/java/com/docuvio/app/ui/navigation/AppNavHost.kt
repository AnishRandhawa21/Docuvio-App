@file:OptIn(ExperimentalAnimationApi::class)

package com.docuvio.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.navArgument

import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseInCubic
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

import androidx.compose.foundation.background
import com.docuvio.app.di.AppContainer
import com.docuvio.app.ui.auth.LoginScreen
import com.docuvio.app.ui.auth.SignupScreen
import com.docuvio.app.ui.home.HomeScreen
import com.docuvio.app.ui.order.CreateOrderScreen
import com.docuvio.app.ui.order.WalkInOrderScreen
import com.docuvio.app.ui.orders.OrdersScreen
import com.docuvio.app.ui.profile.DeleteAccountScreen
import com.docuvio.app.ui.profile.ProfileScreen
import com.docuvio.app.ui.splash.SplashScreen
import com.docuvio.app.viewmodel.*
import com.docuvio.app.ui.profile.FeedbackScreen
import com.docuvio.app.theme.Cream

import android.app.Activity
import androidx.compose.ui.platform.LocalContext

// ── Which routes are stack screens (pushed on top of tabs) ───
private val STACK_ROUTES = setOf(
    "create_order",
    "walkin_order",
    "delete_account",
    "feedback",
    "login",
    "signup"
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
// - Going TO a stack screen → behave like stackExit (slide left)
// - Switching tabs → gentle directional slide in correct direction
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
// - Coming FROM a stack screen → behave like stackPopEnter (slide from left)
// - Switching tabs → gentle directional slide from correct direction
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

@OptIn(ExperimentalAnimationApi::class)
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
                viewModelFactory = SplashViewModelFactory(appContainer.tokenManager),
                onNavigateToLogin = {
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
        ) {
            HomeScreen(
                viewModelFactory = HomeViewModelFactory(appContainer.shopRepository),
                tokenManager = appContainer.tokenManager,
                notificationApi = appContainer.notificationApi,
                onShopClick = { shopId ->
                    navController.navigate(Routes.CreateOrder.createRoute(shopId))
                },
                onScheduleClick = { shopId ->
                    navController.navigate(Routes.CreateOrder.createRoute(shopId))
                },
                onOrderNowClick = { shopId ->
                    navController.navigate(Routes.WalkInOrder.createRoute(shopId))
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
        ) {
            OrdersScreen(
                viewModelFactory = OrdersViewModelFactory(appContainer.orderRepository)
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
        ) {
            ProfileScreen(
                viewModelFactory = ProfileViewModelFactory(appContainer.authRepository),
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
            route = Routes.WalkInOrder.route,
            arguments = listOf(navArgument("shopId") { type = NavType.StringType }),
            enterTransition = { stackEnter() },
            exitTransition = { stackExit() },
            popEnterTransition = { stackPopEnter() },
            popExitTransition = { stackPopExit() }
        ) { backStackEntry ->
            val shopId = backStackEntry.arguments?.getString("shopId")
                ?: return@composable

            WalkInOrderScreen(
                viewModelFactory = WalkInOrderViewModelFactory(
                    orderRepository = appContainer.orderRepository,
                    shopId = shopId
                ),
                onSuccess = {
                    navController.popBackStack(Routes.Home.route, inclusive = false)
                    navController.navigate(Routes.Orders.route) {
                        launchSingleTop = true
                    }
                }
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
                shopId = shopId,
                viewModelFactory = CreateOrderViewModelFactory(
                    appContainer.shopRepository,
                    appContainer.orderRepository,
                    shopId
                ),
                onOrderSuccess = {
                    navController.popBackStack(Routes.Home.route, inclusive = false)
                    navController.navigate(Routes.Orders.route) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}