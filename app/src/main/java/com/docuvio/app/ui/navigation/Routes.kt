package com.docuvio.app.ui.navigation

sealed class Routes(val route: String) {
    object Splash : Routes("splash")
    object Login : Routes("login")
    object Signup : Routes("signup")
    object Main : Routes("main")
    object Home : Routes("home")
    object Orders : Routes("orders")
    object Profile : Routes("profile")

    object DeleteAccount : Routes("delete_account")

    // NEW
    object Feedback : Routes("feedback")

    object CreateOrder : Routes("createOrder/{shopId}") {
        fun createRoute(shopId: String) = "createOrder/$shopId"
    }

    object QRScanner : Routes("qrScanner")

    object PrintSession : Routes("printSession/{sessionToken}") {
        fun createRoute(sessionToken: String) = "printSession/$sessionToken"
    }

    object DeepLinkHandler : Routes("deeplink/{shopCode}") {
        fun createRoute(shopCode: String) = "deeplink/$shopCode"
    }
}