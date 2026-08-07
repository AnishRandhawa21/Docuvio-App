package com.docuvio.app.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.data.model.Shop
import com.docuvio.app.theme.*
import com.docuvio.app.ui.order.utils.formatTime
import com.docuvio.app.utils.ShopStatusResolver
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ShoppingBag

@Composable
fun ShopCard(
    shop: Shop,
    onScheduleClick: (String) -> Unit,
    onOrderNowClick: (String) -> Unit
) {
    val capabilities = ShopStatusResolver.resolve(shop)
    val walkInEnabled = capabilities.walkInEnabled
    val onlineEnabled = capabilities.onlineEnabled
    val isVisuallyActive = walkInEnabled || onlineEnabled

    val cardBrush = if (isVisuallyActive) {
        Brush.linearGradient(listOf(ActiveCardStart, ActiveCardEnd))
    } else {
        Brush.linearGradient(listOf(InactiveCardStart, InactiveCardEnd))
    }

    // Distinct pill color per status, using colors already in your theme
    val statusColor = when {
        walkInEnabled -> SuccessGreen
        onlineEnabled -> Blue     // 0xFF423C38 — dark warm neutral, calm against green
        else -> DarkGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBrush)
                .padding(20.dp)
        ) {
            // --- SECTION 1: HEADER ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Storefront,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.shopName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = Manrope,
                        color = if (isVisuallyActive) White else TextDisabled,
                        lineHeight = 28.sp,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Level: ${shop.block}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = Manrope,
                        color = if (isVisuallyActive) White.copy(alpha = 0.85f) else TextDisabledSecondary
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // --- SECTION 2: STATUS + TIME ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status chip — now color-coded per status
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = capabilities.bannerText.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = Manrope,
                        color = White,
                        letterSpacing = 1.sp,
                        maxLines = 1
                    )
                }

                val timeText = if (isVisuallyActive) "Closes at ${formatTime(shop.closeTime)}"
                else "Opens at ${formatTime(shop.openTime)}"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(White.copy(alpha = 0.95f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Manrope,
                        color = if (isVisuallyActive) SuccessGreen else TextDisabled,
                        maxLines = 1
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // --- SECTION 3: ACTIONS ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ImageCompatible3DButton(
                    text = "Schedule",
                    icon = Icons.Outlined.CalendarMonth,
                    enabled = onlineEnabled,
                    onClick = { onScheduleClick(shop.id) },
                    modifier = Modifier.weight(1f)
                )
                ImageCompatible3DButton(
                    text = "Order Now",
                    icon = Icons.Outlined.ShoppingBag,
                    enabled = walkInEnabled,
                    onClick = { onOrderNowClick(shop.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ImageCompatible3DButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val depth = 6.dp
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .height(60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) {
                scope.launch {
                    offsetY.animateTo(depth.value, animationSpec = tween(50))
                    offsetY.animateTo(0f, animationSpec = tween(100))
                    onClick()
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Shadow layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) ButtonShadowEnabled.copy(alpha = 0.5f) else ButtonShadowDisabled.copy(alpha = 0.3f))
        )

        // Top surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .offset(y = offsetY.value.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        if (enabled) listOf(White, Color(0xFFF5F5F5))
                        else listOf(LightGray, Color(0xFFD7D7D7))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = Manrope,
                color = if (enabled) SuccessGreen else ButtonTextDisabled
            )
        }
    }
}

@Composable
fun ImageCompatible3DButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val depth = 6.dp
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .height(60.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled
            ) {
                scope.launch {
                    offsetY.animateTo(depth.value, animationSpec = tween(50))
                    offsetY.animateTo(0f, animationSpec = tween(100))
                    onClick()
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        // Shadow layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) ButtonShadowEnabled.copy(alpha = 0.5f) else ButtonShadowDisabled.copy(alpha = 0.3f))
        )

        // Top surface
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .offset(y = offsetY.value.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    brush = Brush.verticalGradient(
                        if (enabled) listOf(White, Color(0xFFF5F5F5))
                        else listOf(LightGray, Color(0xFFD7D7D7))
                    )
                ),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) SuccessGreen else ButtonTextDisabled,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = Manrope,
                color = if (enabled) SuccessGreen else ButtonTextDisabled
            )
        }
    }
}