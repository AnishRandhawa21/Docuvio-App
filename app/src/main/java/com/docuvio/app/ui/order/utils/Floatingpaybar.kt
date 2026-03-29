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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.theme.MediumGray
import com.docuvio.app.ui.order.utils.PricingUtils.calculateTotal
import com.docuvio.app.viewmodel.CreateOrderUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ThumbSize      = 48.dp
private val TrackPadding   = 6.dp
private val TrackHeight    = 58.dp
private val ThumbColorIdle = Color(0xFF8FD16B)
private val ThumbColorDone = Color(0xFF5AB348)
private val FillColor      = Color(0xFF6BBF4E)
private val CardColor      = Color(0xFF1C1C1E)
private val TrackBgColor   = Color(0xFF2C2C2E)

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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                .shadow(
                    elevation = 32.dp,
                    shape = RoundedCornerShape(24.dp),
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.4f),
                    spotColor = Color.Black.copy(alpha = 0.4f)
                )
                .clip(RoundedCornerShape(24.dp))
                .background(CardColor)
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 10.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column {
                        Text(
                            text = "Total ${uiState.pageCount ?: 0} pages",
                            color = Color(0xFFB0B0B0), // or MediumGray
                            fontSize = 13.sp
                        )

                        Text(
                            text = "Incl. Platform Fee",
                            color = Color(0xFFB0B0B0),
                            fontSize = 11.sp
                        )
                    }

                    Text(
                        text = "₹ $total",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))

                SwipeToPayButton(
                    enabled = isEnabled,
                    onSwiped = onSubmit,
                    onAttemptedWhenDisabled = ::triggerShake,
                    modifier = Modifier.fillMaxWidth()
                )
            }
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

    var completed       by remember { mutableStateOf(false) }
    val animatedOffset   = remember { Animatable(0f) }


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
        (animatedOffset.value / maxOffsetPx).coerceIn(0f, 1f)
    else 0f

    val thumbColor = when {
        completed -> ThumbColorDone
        !enabled  -> ThumbColorIdle.copy(alpha = 0.45f)
        else      -> ThumbColorIdle
    }

    Box(
        modifier = modifier
            .height(TrackHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(TrackBgColor)
            .onGloballyPositioned { trackWidthPx = it.size.width.toFloat() },
        contentAlignment = Alignment.CenterStart
    ) {

        // Green fill track
        if (maxOffsetPx > 0f && enabled) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(
                        ThumbSize + TrackPadding + TrackPadding +
                                with(density) { animatedOffset.value.toDp() }
                    )
                    .clip(RoundedCornerShape(999.dp))
                    .background(FillColor)
            )
        }

        // Pulsing arrows — anchored to right edge, not left
        if (!completed) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)  // ← right side
                    .padding(end = 16.dp),       // ← right padding
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf(arrowAlpha1, arrowAlpha2, arrowAlpha3).forEach { alpha ->
                    val effectiveAlpha = if (!enabled) {
                        alpha * 0.3f
                    } else {
                        (alpha * (1f - progress * 2.5f)).coerceIn(0f, 1f)
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = effectiveAlpha),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
            fontSize = 16.sp,
            modifier = Modifier.align(Alignment.Center)
        )

        // Draggable thumb
        Box(
            modifier = Modifier
                .padding(start = TrackPadding)
                .offset { IntOffset(animatedOffset.value.roundToInt(), 0) }
                .size(ThumbSize)
                .clip(CircleShape)
                .background(thumbColor)
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
                                            dampingRatio = Spring.DampingRatioMediumBouncy
                                        )
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