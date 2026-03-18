package com.docuvio.app.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuvio.app.viewmodel.SplashViewModelFactory
import com.docuvio.app.theme.Cream // ADDED: Import theme colors
import com.docuvio.app.theme.GoldenYellow // ADDED: Import theme colors
import com.docuvio.app.R
@Composable
fun SplashScreen(
    viewModelFactory: SplashViewModelFactory,
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit
) {
    val viewModel: SplashViewModel = viewModel(factory = viewModelFactory)
    val navigationDestination by viewModel.navigationDestination.collectAsState()

    var startAnimation by remember { mutableStateOf(false) }

    // Fade in animation
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "alpha"
    )

    // Ring rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotate"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    LaunchedEffect(navigationDestination) {
        when (navigationDestination) {
            "login" -> onNavigateToLogin()
            "main" -> onNavigateToMain()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream), // CHANGED: from Color(0xFF151419) to Cream - matches app background
        contentAlignment = Alignment.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.docuvio_logo_png),
            contentDescription = "Docuvio Logo",
            modifier = Modifier
                .size(100.dp) // CHANGED: from 80.dp to 100.dp - slightly larger for better visibility on light background
                .alpha(alphaAnim.value)
        )

        // Loading ring around logo (much closer)
        Canvas(
            modifier = Modifier
                .size(120.dp) // CHANGED: from 82.dp to 120.dp - adjusted to match new logo size
                .alpha(alphaAnim.value)
        ) {
            drawArc(
                color = GoldenYellow, // CHANGED: from Color(0xFFFF8C00) to GoldenYellow - matches your theme
                startAngle = rotation,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(
                    width = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}