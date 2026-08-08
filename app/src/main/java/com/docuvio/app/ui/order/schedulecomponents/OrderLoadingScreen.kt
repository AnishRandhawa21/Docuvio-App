package com.docuvio.app.ui.order.schedulecomponents

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.theme.Cream


// Brand colors pulled from the home screen — adjust hex values if you have
// them defined centrally in your theme already.
private val DeepGreen = Color(0xFF1F5E3D)   // dark card green
private val LeafGreen = Color(0xFF7CB342)   // banner accent green
private val PaleGreen = Color(0xFFB9D6A5)   // soft highlight for shimmer

@Composable
fun OrderLoadingScreen(text: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    // Slow, single shimmer sweep across the "page" — much calmer than a
    // multi-line staggered animation, reads as "working", not "busy".
    val shimmer by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // Gentle breathing scale on the document card itself.
    val breathe by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1800, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val dot1 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot1"
    )
    val dot2 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, delayMillis = 200, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot2"
    )
    val dot3 by infiniteTransition.animateFloat(
        initialValue = 0.25f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, delayMillis = 400, easing = LinearEasing), RepeatMode.Reverse),
        label = "dot3"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Document card
            Box(
                modifier = Modifier
                    .size(width = 72.dp, height = 90.dp)
                    .scale(breathe)
                    .shadow(10.dp, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 10.dp, vertical = 14.dp)
            ) {
                // Folded corner, tinted with brand green instead of plain cream
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd)
                        .background(PaleGreen.copy(alpha = 0.5f))
                        .clip(RoundedCornerShape(bottomStart = 8.dp))
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val widths = listOf(0.85f, 1f, 0.6f, 0.9f)

                    widths.forEach { w ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(w)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(DeepGreen.copy(alpha = 0.12f))
                        ) {
                            // one soft shimmer highlight sweeping across all lines together
                            val alpha = when {
                                shimmer < -0.4f -> 0f
                                shimmer < 0f -> (shimmer + 0.4f) / 0.4f
                                shimmer > 1.4f -> 0f
                                shimmer > 1f -> 1f - (shimmer - 1f) / 0.4f
                                else -> 1f
                            }.coerceIn(0f, 1f)

                            val normalized = ((shimmer + 1f) / 3f).coerceIn(0f, 1f)

                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.45f)
                                    .offset { IntOffset(x = (normalized * 110).toInt(), y = 0) }
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                DeepGreen.copy(alpha = 0f),
                                                LeafGreen.copy(alpha = alpha * 0.9f),
                                                DeepGreen.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = text.uppercase(),
                    fontSize = 10.sp,
                    letterSpacing = 3.5.sp,
                    color = DeepGreen.copy(alpha = 0.55f)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf(dot1, dot2, dot3).forEach { alpha ->
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(LeafGreen.copy(alpha = alpha))
                        )
                    }
                }
            }
        }
    }
}