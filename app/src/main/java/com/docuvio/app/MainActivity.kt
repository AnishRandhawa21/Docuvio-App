package com.docuvio.app

import com.docuvio.app.ui.terms.TermsScreen
import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.docuvio.app.data.model.RazorpayHolder
import com.docuvio.app.data.model.RazorpayResult
import com.docuvio.app.theme.LovelyPrintsTheme
import com.docuvio.app.ui.main.MainScreen
import com.docuvio.app.ui.navigation.AppNavHost
import com.docuvio.app.ui.navigation.Routes
import com.docuvio.app.theme.Cream
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d("NOTIFICATION", "Permission granted = $isGranted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermissionFirstLaunch()

        Checkout.preload(applicationContext)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    return@addOnCompleteListener
                }
                val token = task.result
                // FCM token logging removed for security
            }

        setContent {
            LovelyPrintsTheme {

                FixSystemBars(enabled = true)

                val navController = rememberNavController()

                var showTerms by rememberSaveable {
                    mutableStateOf(true)
                }

                Box {

                    MainScreen(navController) { padding ->
                        AppNavHost(
                            navController = navController,
                            startDestination = Routes.Splash.route,
                            appContainer = (application as DocuvioApp).appContainer,
                            modifier = padding
                        )
                    }

                    if (showTerms) {
                        TermsScreen(
                            onAccept = { showTerms = false }
                        )
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionFirstLaunch() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val alreadyAsked = prefs.getBoolean("notification_permission_asked", false)
        if (alreadyAsked) return

        prefs.edit()
            .putBoolean("notification_permission_asked", true)
            .apply()

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // --------------------------------------------------
    // Razorpay callbacks
    // --------------------------------------------------

    override fun onPaymentSuccess(
        razorpayPaymentId: String?,
        paymentData: PaymentData?
    ) {
        val orderId = paymentData?.orderId ?: return
        val paymentId = paymentData.paymentId ?: return
        val signature = paymentData.signature ?: return

        RazorpayHolder.result = RazorpayResult(orderId, paymentId, signature)
        Log.d("RAZORPAY", "PAYMENT SUCCESS")
    }

    override fun onPaymentError(
        code: Int,
        description: String?,
        paymentData: PaymentData?
    ) {
        if (code == 0) {
            RazorpayHolder.result = RazorpayResult(
                orderId = paymentData?.orderId ?: "",
                paymentId = "",
                signature = "",
                cancelled = true
            )
            Log.d("RAZORPAY", "PAYMENT CANCELLED BY USER")
            return
        }

        RazorpayHolder.result = RazorpayResult(
            orderId = paymentData?.orderId ?: "",
            paymentId = "",
            signature = "",
            errorMessage = description
        )
        Log.e("RAZORPAY", "PAYMENT FAILED → $description")
    }
}

/* -------------------------------------------------- */
/* ---------------- SYSTEM BAR FIX ------------------- */
/* -------------------------------------------------- */

@Composable
fun FixSystemBars(enabled: Boolean) {

    val view = LocalView.current

    SideEffect {
        if (!enabled) return@SideEffect

        val window = (view.context as Activity).window

        // Status bar → Cream with dark icons
        window.statusBarColor = Cream.toArgb()

        // Navigation bar → transparent so the floating bottom nav
        // draws correctly over the edge-to-edge content.
        // The Scaffold's navigationBarsPadding() handles the spacing.
        window.navigationBarColor = Color.Transparent.toArgb()

        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = true   // dark icons on cream
            isAppearanceLightNavigationBars = false // light icons on dark/transparent nav bar
        }
    }
}
