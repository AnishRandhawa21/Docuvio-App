package com.docuvio.app.ui.order.utils

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── Dimensions ─────────────────────────────────────────────────────────────
private val WalkInThumbSize    = 40.dp
private val WalkInTrackPadding = 4.dp
private val WalkInTrackHeight  = 48.dp

// ── Green thumb / fill (unchanged) ─────────────────────────────────────────
private val WalkInThumbTop  = Color(0xFF8FD16B)
private val WalkInThumbBot  = Color(0xFF5AB348)
private val WalkInFillColor = Color(0xFF6BBF4E)

// ── Dark glass palette (same as FloatingPayBar) ─────────────────────────────
private val WalkInCardBg           = Color(0xF7111111)
private val WalkInCardTopShimmer   = Color(0x22FFFFFF)
private val WalkInCardBorderBright = Color(0x33FFFFFF)
private val WalkInCardBorderSide   = Color(0x14FFFFFF)

private val WalkInTrackBg          = Color(0x1AFFFFFF)
private val WalkInTrackBorderColor = Color(0x22FFFFFF)

// ── Text ────────────────────────────────────────────────────────────────────
private val WalkInTextStrong = Color(0xFFFFFFFF)
private val WalkInTextMedium = Color(0xBFFFFFFF)
private val WalkInTextSoft   = Color(0x66FFFFFF)

private fun Modifier.walkInGlassRim(cornerPx: Float): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val cr = CornerRadius(cornerPx)
    drawRoundRect(color = WalkInCardBorderSide, cornerRadius = cr, style = Stroke(width = stroke))
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, WalkInCardBorderBright, WalkInCardBorderBright, Color.Transparent),
            startX = size.width * 0.10f,
            endX   = size.width * 0.90f
        ),
        cornerRadius = cr,
        style = Stroke(width = stroke)
    )
}

/* ─────────────────────────────────────────────────────────
   WalkInFloatingPayBar
   ─────────────────────────────────────────────────────────
   No price row — user already sees the amount in the input.
   Just a clean swipe bar floating above the bottom.

   Usage — place OUTSIDE the Column, as a sibling inside
   the Surface's implicit Box. Replace the Button block:

       WalkInFloatingPayBar(
           isEnabled = canSubmit,
           onSubmit  = { ... }
       )

   Change bottom spacer from 40.dp → 100.dp:
       Spacer(Modifier.height(100.dp))
   ───────────────────────────────────────────────────────── */

@Composable
fun WalkInFloatingPayBar(
    total: Int,
    pageCount: Int,
    isEnabled: Boolean,
    onSubmit: () -> Unit
) {
    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerShake() {
        scope.launch {
            for (target in listOf(18f, -14f, 10f, -6f, 3f, 0f)) {
                shakeOffset.animateTo(target, tween(60, easing = LinearEasing))
            }
        }
    }

    val corner   = 22.dp
    val cornerPx = with(LocalDensity.current) { corner.toPx() }

    // Overlay anchored to bottom of the parent Surface Box
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
//            .navigationBarsPadding()
            .padding(bottom = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                // ── Dark frosted glass ──────────────────────────────────
                // Add on API 31+ for real blur-behind:
                //   .graphicsLayer {
                //       renderEffect = BlurEffect(20f, 20f, TileMode.Clamp)
                //   }
                .clip(RoundedCornerShape(corner))
                .background(WalkInCardBg)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
                .walkInGlassRim(cornerPx)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Left — page count + fee label
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.wrapContentWidth()
            ) {
                Text(
                    text = "$pageCount pages",
                    color = WalkInTextMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.1.sp
                )
                Text(
                    text = "incl. conv. fee",
                    color = WalkInTextSoft,
                    fontSize = 10.sp
                )
            }

            // Centre — amount
            Text(
                text = "₹ $total",
                color = WalkInTextStrong,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.wrapContentWidth()
            )

            // Right — swipe track
            WalkInSwipeToPayButton(
                enabled = isEnabled,
                onSwiped = onSubmit,
                onAttemptedWhenDisabled = ::triggerShake,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun WalkInSwipeToPayButton(
    enabled: Boolean,
    onSwiped: () -> Unit,
    modifier: Modifier = Modifier,
    onAttemptedWhenDisabled: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val scope   = rememberCoroutineScope()

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbSizePx  = with(density) { WalkInThumbSize.toPx() }
    val paddingPx    = with(density) { WalkInTrackPadding.toPx() }
    val maxOffsetPx  = (trackWidthPx - thumbSizePx - paddingPx * 2).coerceAtLeast(0f)

    var completed      by remember { mutableStateOf(false) }
    val animatedOffset  = remember { Animatable(0f) }

    val arrowAlpha1 by rememberInfiniteTransition(label = "a1").animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "a1"
    )
    val arrowAlpha2 by rememberInfiniteTransition(label = "a2").animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 180, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "a2"
    )
    val arrowAlpha3 by rememberInfiniteTransition(label = "a3").animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, delayMillis = 360, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "a3"
    )

    val progress = if (maxOffsetPx > 0f)
        (animatedOffset.value / maxOffsetPx).coerceIn(0f, 1f) else 0f

    val thumbBrush = if (!enabled)
        Brush.verticalGradient(listOf(WalkInThumbTop.copy(alpha = 0.4f), WalkInThumbBot.copy(alpha = 0.4f)))
    else
        Brush.verticalGradient(listOf(WalkInThumbTop, WalkInThumbBot))

    Box(
        modifier = modifier
            .height(WalkInTrackHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(WalkInTrackBg)
            .drawBehind {
                drawRoundRect(
                    color = WalkInTrackBorderColor,
                    cornerRadius = CornerRadius(size.height / 2f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {
        // Green fill sweep
        if (maxOffsetPx > 0f && enabled) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(
                        WalkInThumbSize + WalkInTrackPadding + WalkInTrackPadding +
                                with(density) { animatedOffset.value.toDp() }
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                WalkInFillColor.copy(alpha = 0.6f),
                                WalkInFillColor.copy(alpha = 0.15f)
                            )
                        )
                    )
            )
        }

        // Centre label
        Text(
            text = if (completed) "Processing…" else "Swipe to Pay",
            color = Color.White.copy(
                alpha = when {
                    completed -> 1f
                    !enabled  -> 0.35f
                    else      -> (1f - progress * 2f).coerceIn(0f, 1f)
                }
            ),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = WalkInThumbSize + WalkInTrackPadding + 4.dp, end = 8.dp)
        )

        // Pulsing arrows — anchored to right edge, not left
//        if (!completed) {
//            Row(
//                modifier = Modifier
//                    .align(Alignment.CenterEnd)
//                    .padding(end = 16.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(2.dp)
//            ) {
//                listOf(arrowAlpha1, arrowAlpha2, arrowAlpha3).forEach { alpha ->
//                    val effectiveAlpha = if (!enabled) {
//                        alpha * 0.3f
//                    } else {
//                        (alpha * (1f - progress * 2.5f)).coerceIn(0f, 1f)
//                    }
//                    Icon(
//                        imageVector = Icons.Default.ArrowForward,
//                        contentDescription = null,
//                        tint = Color.White.copy(alpha = effectiveAlpha),
//                        modifier = Modifier.size(16.dp)
//                    )
//                }
//            }
//        }

        // Draggable thumb
        Box(
            modifier = Modifier
                .padding(start = WalkInTrackPadding)
                .offset { IntOffset(animatedOffset.value.roundToInt(), 0) }
                .size(WalkInThumbSize)
                .clip(CircleShape)
                .background(thumbBrush)
                .pointerInput(enabled, completed, maxOffsetPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (!enabled) {
                                scope.launch {
                                    animatedOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        )
                                    )
                                }
                                onAttemptedWhenDisabled?.invoke()
                                return@detectHorizontalDragGestures
                            }
                            if (animatedOffset.value < maxOffsetPx - 10f) {
                                scope.launch {
                                    animatedOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animatedOffset.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    )
                                )
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            if (completed) return@detectHorizontalDragGestures
                            val limit = if (enabled) maxOffsetPx else maxOffsetPx * 0.25f
                            val newVal = (animatedOffset.value + dragAmount).coerceIn(0f, limit)
                            scope.launch { animatedOffset.snapTo(newVal) }
                            if (enabled && newVal >= maxOffsetPx - 5f) {
                                completed = true
                                scope.launch {
                                    animatedOffset.snapTo(maxOffsetPx)
                                    delay(150)
                                    onSwiped()
                                    delay(2500)
                                    completed = false
                                    animatedOffset.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        )
                                    )
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (completed) Icons.Default.Check else Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}