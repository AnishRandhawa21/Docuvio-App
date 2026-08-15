package com.docuvio.app.ui.main

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.docuvio.app.R

@Composable
fun FixSystemBars(route: String?) {
    val view = LocalView.current

    DisposableEffect(route) {
        val window = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowCompat.getInsetsController(window, view)

        // 🔥Top bar Icon Changer - Always Light Mode (Dark Icons)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        // 🔥 EXTRA FORCE (handles dialog override)
        window.decorView.post {
            controller.isAppearanceLightStatusBars = true
        }

        onDispose { }
    }

    // Theme-aware status bar background
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .background(MaterialTheme.colorScheme.background)
    )
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

    val red   = com.docuvio.app.theme.BrandRed
    val amber = com.docuvio.app.theme.BrandOrange
    val lime  = com.docuvio.app.theme.BrandLime

    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
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
                        .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
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
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}
