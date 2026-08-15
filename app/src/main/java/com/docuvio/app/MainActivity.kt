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
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.docuvio.app.data.model.RazorpayHolder
import com.docuvio.app.data.model.RazorpayResult
import com.docuvio.app.theme.LovelyPrintsTheme
import com.docuvio.app.ui.main.MainScreen
import com.docuvio.app.ui.navigation.AppNavHost
import com.docuvio.app.ui.navigation.Routes
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.google.firebase.messaging.FirebaseMessaging
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.navigation.compose.currentBackStackEntryAsState
import com.docuvio.app.ui.main.DocuvioLoadingAnimation
import com.docuvio.app.ui.main.FixSystemBars
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {
    private lateinit var appUpdateManager: AppUpdateManager
    
    fun enableEdgeToEdgeSafe() {
        enableEdgeToEdge() // official API
    }


    fun prepareWindowForRazorpay(onReady: () -> Unit) {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = Color.White.toArgb()

        // Wait for window to re-layout BEFORE opening Razorpay
        window.decorView.post {
            window.decorView.post {  // double post = next-next frame, fully laid out
                onReady()
            }
        }
    }
    
    private fun checkForAppUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->

            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    AppUpdateType.FLEXIBLE,
                    this,
                    101
                )
            }
        }
    }
    
    override fun onResume() {
        super.onResume()

        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                appUpdateManager.completeUpdate()
            }
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Log.d("NOTIFICATION", "Permission granted = $isGranted")
        }

    @OptIn(ExperimentalGetImage::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔍 Log Deep Link Intent
        val action: String? = intent?.action
        val data: android.net.Uri? = intent?.data
        Log.d("DEEP_LINK", "Action: $action, Data: $data")

        requestNotificationPermissionFirstLaunch()

        Checkout.preload(applicationContext)

        // 🔥 KEEP THIS (your normal UI)
        enableEdgeToEdgeSafe()

        appUpdateManager = AppUpdateManagerFactory.create(this)
        checkForAppUpdate()

        appUpdateManager.registerListener { state ->
            if (state.installStatus() == InstallStatus.DOWNLOADED) {
                runOnUiThread {
                    android.widget.Toast.makeText(
                        this,
                        "Update downloaded. Restarting...",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    appUpdateManager.completeUpdate()
                }
            }
        }

        FirebaseMessaging.getInstance().token

        val tokenManager = (application as DocuvioApp).appContainer.tokenManager

        setContent {
            LovelyPrintsTheme {
                val scope = rememberCoroutineScope()
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                FixSystemBars(route = currentRoute)

                var showTerms by rememberSaveable { 
                    mutableStateOf(!tokenManager.hasAcceptedTermsBlocking()) 
                }
                var isTransitioning by remember { mutableStateOf(false) }

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
                            onAccept = {
                                isTransitioning = true
                                scope.launch {
                                    tokenManager.saveTermsAccepted(true)
                                    showTerms = false
                                }
                            }
                        )
                    }

                    AnimatedVisibility(
                        visible = isTransitioning,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            DocuvioLoadingAnimation()
                        }
                    }

                    if (isTransitioning) {
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(1000)
                            isTransitioning = false
                        }
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

        prefs.edit().putBoolean("notification_permission_asked", true).apply()

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
        // 🔥 RESTORE EDGE TO EDGE
        enableEdgeToEdgeSafe()

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
        // 🔥 RESTORE EDGE TO EDGE
        enableEdgeToEdgeSafe()

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
