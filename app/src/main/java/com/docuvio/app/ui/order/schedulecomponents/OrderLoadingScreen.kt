package com.docuvio.app.ui.order.schedulecomponents

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.theme.*

@Composable
fun OrderLoadingScreen(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "aesthetic_loading")

    // Gentle vertical float for the paper stack
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Pulse for the shadow to match the float
    val shadowScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shadow"
    )

    // Shimmer/Scan line progress
    val scanProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.BottomCenter) {
                // ── Shadow ──
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(6.dp)
                        .scale(shadowScale)
                        .background(AlmostBlack.copy(alpha = 0.05f), RoundedCornerShape(100))
                )

                // ── Paper Stack ──
                Box(
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .offset(y = floatAnim.dp)
                        .size(width = 64.dp, height = 82.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Back Paper
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .offset(x = 6.dp, y = (-6).dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AlmostBlack.copy(alpha = 0.04f))
                    )

                    // Front Paper
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        // Scan line effect
                        val scanAlpha = when {
                            scanProgress < 0.2f -> scanProgress / 0.2f
                            scanProgress > 0.8f -> 1f - (scanProgress - 0.8f) / 0.2f
                            else -> 1f
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.15f)
                                .offset(y = (scanProgress * 82).dp)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            SuccessGreen.copy(alpha = 0f),
                                            SuccessGreen.copy(alpha = 0.12f * scanAlpha),
                                            SuccessGreen.copy(alpha = 0f)
                                        )
                                    )
                                )
                        )

                        // Placeholder lines (minimal lines on paper)
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(Modifier.fillMaxWidth(0.6f).height(2.dp).background(AlmostBlack.copy(alpha = 0.05f)))
                            Box(Modifier.fillMaxWidth(0.9f).height(2.dp).background(AlmostBlack.copy(alpha = 0.05f)))
                            Box(Modifier.fillMaxWidth(0.4f).height(2.dp).background(AlmostBlack.copy(alpha = 0.05f)))
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))

            // ── Text ──
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = Manrope,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    fontSize = 11.sp
                ),
                color = AlmostBlack.copy(alpha = 0.35f)
            )
        }
    }
}
