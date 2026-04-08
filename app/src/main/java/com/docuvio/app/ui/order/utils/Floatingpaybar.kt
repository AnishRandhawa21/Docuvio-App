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
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.ui.order.utils.PricingUtils.calculateTotal
import com.docuvio.app.viewmodel.CreateOrderUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── Dimensions ─────────────────────────────────────────────────────────────
private val ThumbSize    = 40.dp
private val TrackPadding = 4.dp
private val TrackHeight  = 48.dp

// ── Green thumb / fill (unchanged) ─────────────────────────────────────────
private val ThumbTop  = Color(0xFF8FD16B)
private val ThumbBot  = Color(0xFF5AB348)
private val FillColor = Color(0xFF6BBF4E)

// ── Dark glass palette ──────────────────────────────────────────────────────
//private val CardBg           = Color(0xCC111111)   // ~80 % opaque near-black
private val CardBg = Color(0xF7111111)
private val CardTopShimmer   = Color(0x22FFFFFF)   // faint inner glow at top
private val CardBorderBright = Color(0x33FFFFFF)   // lit top rim
private val CardBorderSide   = Color(0x14FFFFFF)   // subtle sides/bottom

private val TrackBg          = Color(0x1AFFFFFF)   // dim white pill on dark card
private val TrackBorderColor = Color(0x22FFFFFF)

// ── Text — white on dark ────────────────────────────────────────────────────
private val TextStrong  = Color(0xFFFFFFFF)
private val TextMedium  = Color(0xBFFFFFFF)   // 75 % white
private val TextSoft    = Color(0x66FFFFFF)   // 40 % white



private fun Modifier.glassRim(cornerPx: Float): Modifier = drawBehind {
    val stroke = 1.dp.toPx()
    val cr = CornerRadius(cornerPx)
    drawRoundRect(color = CardBorderSide, cornerRadius = cr, style = Stroke(width = stroke))
    drawRoundRect(
        brush = Brush.horizontalGradient(
            colors = listOf(Color.Transparent, CardBorderBright, CardBorderBright, Color.Transparent),
            startX = size.width * 0.10f,
            endX   = size.width * 0.90f
        ),
        cornerRadius = cr,
        style = Stroke(width = stroke)
    )
}

@Composable
fun FloatingPayBar(
    uiState: CreateOrderUiState,
    onSubmit: () -> Unit
) {
    val isEnabled = uiState.selectedFile != null &&
            uiState.selectedPaperType != null &&
            uiState.selectedFinishType != null &&
            uiState.pickupAt != null

    val shakeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun triggerShake() {
        scope.launch {
            for (target in listOf(18f, -14f, 10f, -6f, 3f, 0f)) {
                shakeOffset.animateTo(target, tween(60, easing = LinearEasing))
            }
        }
    }

    val total = calculateTotal(uiState)
    val corner   = 22.dp
    val cornerPx = with(LocalDensity.current) { corner.toPx() }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
//                   .graphicsLayer {
//                       renderEffect = BlurEffect(20f, 20f, TileMode.Clamp)
//                   }
                .clip(RoundedCornerShape(corner))
                .background(CardBg)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
                .glassRim(cornerPx)
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
                    text = "Total ${uiState.pageCount ?: 0} pages",
                    color = TextMedium,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.1.sp
                )
                Text(
                    text = "Fees Incl.",
                    color = TextSoft,
                    fontSize = 10.sp
                )
            }

            // Centre — amount
            Text(
                text = "₹$total",
                color = TextStrong,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.wrapContentWidth()
            )

            // Right — swipe track
            SwipeToPayButton(
                enabled = isEnabled,
                onSwiped = onSubmit,
                onAttemptedWhenDisabled = ::triggerShake,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun SwipeToPayButton(
    enabled: Boolean,
    onSwiped: () -> Unit,
    modifier: Modifier = Modifier,
    onAttemptedWhenDisabled: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val scope   = rememberCoroutineScope()

    var trackWidthPx by remember { mutableFloatStateOf(0f) }
    val thumbSizePx  = with(density) { ThumbSize.toPx() }
    val paddingPx    = with(density) { TrackPadding.toPx() }
    val maxOffsetPx  = (trackWidthPx - thumbSizePx - paddingPx * 2).coerceAtLeast(0f)

    var completed      by remember { mutableStateOf(false) }
    val animatedOffset  = remember { Animatable(0f) }

    val progress = if (maxOffsetPx > 0f)
        (animatedOffset.value / maxOffsetPx).coerceIn(0f, 1f) else 0f

    val thumbBrush = if (!enabled)
        Brush.verticalGradient(listOf(ThumbTop.copy(alpha = 0.4f), ThumbBot.copy(alpha = 0.4f)))
    else
        Brush.verticalGradient(listOf(ThumbTop, ThumbBot))

    Box(
        modifier = modifier
            .height(TrackHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(TrackBg)
            .drawBehind {
                drawRoundRect(
                    color = TrackBorderColor,
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
                        ThumbSize + TrackPadding + TrackPadding +
                                with(density) { animatedOffset.value.toDp() }
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                FillColor.copy(alpha = 0.6f),
                                FillColor.copy(alpha = 0.15f)
                            )
                        )
                    )
            )
        }

        // Label — starts after the thumb, single line, never wraps
        Text(
            text = if (completed) "Processing…" else "Swipe to pay",
            color = Color.White.copy(
                alpha = when {
                    completed -> 0.75f
                    !enabled  -> 0.25f
                    else      -> (0.65f * (1f - progress * 2f)).coerceIn(0f, 0.65f)
                }
            ),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = ThumbSize + TrackPadding + 4.dp, end = 8.dp)
        )

        // Draggable thumb
        Box(
            modifier = Modifier
                .padding(start = TrackPadding)
                .offset { IntOffset(animatedOffset.value.roundToInt(), 0) }
                .size(ThumbSize)
                .clip(CircleShape)
                .background(thumbBrush)
                .pointerInput(enabled, completed, maxOffsetPx) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (!enabled) {
                                scope.launch {
                                    animatedOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                                onAttemptedWhenDisabled?.invoke()
                                return@detectHorizontalDragGestures
                            }
                            if (animatedOffset.value < maxOffsetPx - 10f) {
                                scope.launch {
                                    animatedOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animatedOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
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
                                    animatedOffset.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
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
                modifier = Modifier.size(20.dp)
            )
        }
    }
}