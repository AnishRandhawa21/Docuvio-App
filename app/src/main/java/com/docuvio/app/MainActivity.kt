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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

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

                // Small loading state to bridge the gap between Terms and Home
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
                                showTerms = false 
                            }
                        )
                    }

                    // Loading overlay for transition
                    // Replace the entire AnimatedVisibility loading overlay block with this:

                    AnimatedVisibility(
                        visible = isTransitioning,
                        enter = fadeIn(tween(300)),
                        exit = fadeOut(tween(500))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Cream),
                            contentAlignment = Alignment.Center
                        ) {
                            DocuvioLoadingAnimation()
                        }
                    }

                    // Hide loading overlay after Home screen likely has started loading
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

@Composable
fun DocuvioLoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val logoAlpha by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    val barProgress by infiniteTransition.animateFloat(
        initialValue = -1f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(3500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "bar"
    )

    val red   = Color(0xFFE8453C)
    val amber = Color(0xFFF5A623)
    val lime  = Color(0xFFC8D837)

    Box(
        Modifier.fillMaxSize().background(Cream),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Logo — breathing
            Image(
                painter = painterResource(R.drawable.docuvio_logo_png),
                contentDescription = "Docuvio",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Sliding gradient bar
                Box(
                    modifier = Modifier
                        .width(80.dp)
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.08f))
                ) {
                    val fillWidth = 0.4f // 40% of track
                    val offset = barProgress * (1f + fillWidth) - fillWidth
                    val alpha = when {
                        barProgress < 0.15f -> barProgress / 0.15f
                        barProgress > 0.85f -> 1f - (barProgress - 0.85f) / 0.15f
                        else -> 1f
                    }.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fillWidth)
                            .offset(x = 80.dp * offset)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                Brush.horizontalGradient(listOf(red, amber, lime))
                            )
                            .graphicsLayer { this.alpha = alpha }
                    )
                }

                // Wordmark
                Text(
                    text = "DOCUVIO",
                    fontSize = 11.sp,
                    letterSpacing = 5.sp,
                    color = Color.Black.copy(alpha = 0.25f),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
