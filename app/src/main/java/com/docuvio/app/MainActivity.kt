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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.docuvio.app.theme.GoldenYellow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

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

    // Three rings at different speeds and directions
    val rotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "outer"
    )
    val rotationMid by infiniteTransition.animateFloat(
        initialValue = 360f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "mid"
    )
    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "inner"
    )

    // Pulsing glow behind logo
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    // Label blink
    val labelAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "label"
    )

    // Floating particles (6 dots at fixed offsets)
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "particles"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            // Floating particles layer
            Canvas(modifier = Modifier.fillMaxSize()) {
                val particleData = listOf(
                    Offset(0.10f, 0.20f) to 4.dp.toPx(),
                    Offset(0.05f, 0.70f) to 3.dp.toPx(),
                    Offset(0.85f, 0.15f) to 5.dp.toPx(),
                    Offset(0.88f, 0.75f) to 3.dp.toPx(),
                    Offset(0.02f, 0.50f) to 4.dp.toPx(),
                    Offset(0.93f, 0.45f) to 3.dp.toPx(),
                )
                particleData.forEachIndexed { i, (pos, radius) ->
                    val delay = i / 6f
                    val t = ((particleProgress + delay) % 1f)
                    val alpha = when {
                        t < 0.3f -> t / 0.3f
                        t < 0.7f -> 1f
                        else -> 1f - (t - 0.7f) / 0.3f
                    } * 0.75f
                    val floatY = -22.dp.toPx() * t
                    drawCircle(
                        color = GoldenYellow.copy(alpha = alpha),
                        radius = radius,
                        center = Offset(
                            size.width * pos.x,
                            size.height * pos.y + floatY
                        )
                    )
                }
            }

            // Outer ring (clockwise, with orbiting dot)
            Canvas(modifier = Modifier.size(160.dp)) {
                val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = GoldenYellow,
                    startAngle = rotationOuter,
                    sweepAngle = 220f,
                    useCenter = false,
                    style = stroke
                )
                drawArc(
                    color = GoldenYellow.copy(alpha = 0.25f),
                    startAngle = rotationOuter + 220f,
                    sweepAngle = 140f,
                    useCenter = false,
                    style = stroke
                )
                // Orbiting dot at tip of outer arc
                val dotAngleRad = Math.toRadians(rotationOuter.toDouble())
                val cx = center.x + (size.minDimension / 2f) * cos(dotAngleRad).toFloat()
                val cy = center.y + (size.minDimension / 2f) * sin(dotAngleRad).toFloat()
                drawCircle(color = GoldenYellow, radius = 5.dp.toPx(), center = Offset(cx, cy))
                drawCircle(color = GoldenYellow.copy(alpha = 0.3f), radius = 9.dp.toPx(), center = Offset(cx, cy))
            }

            //
            Canvas(modifier = Modifier.size(128.dp)) {
                val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                drawArc(
                    color = GoldenYellow.copy(alpha = 0.85f),
                    startAngle = rotationMid,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = stroke
                )
                drawArc(
                    color = GoldenYellow.copy(alpha = 0.15f),
                    startAngle = rotationMid + 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    style = stroke
                )
            }

            // Inner ring (fast clockwise)
            Canvas(modifier = Modifier.size(100.dp)) {
                drawArc(
                    color = GoldenYellow.copy(alpha = 0.9f),
                    startAngle = rotationInner,
                    sweepAngle = 120f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Pulsing glow behind logo
//            Canvas(modifier = Modifier.size(80.dp)) {
//                drawCircle(
//                    color = GoldenYellow.copy(alpha = pulseAlpha * 0.35f),
//                    radius = (size.minDimension / 2f) * pulseScale
//                )
//            }

            // Logo
            Image(
                painter = painterResource(id = R.drawable.docuvio_logo_png),
                contentDescription = "Docuvio",
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "LOADING",
            fontSize = 11.sp,
            letterSpacing = 5.sp,
            color = Color(0xFF8B7355).copy(alpha = labelAlpha),
            fontWeight = FontWeight.Medium
        )
    }
}
