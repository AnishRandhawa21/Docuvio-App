package com.docuvio.app.ui.home

import android.util.Log
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.docuvio.app.R
import com.docuvio.app.data.model.Shop
import com.docuvio.app.theme.Inter
import com.docuvio.app.viewmodel.HomeViewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.docuvio.app.firebase.registerFcmToken
import com.docuvio.app.theme.AlmostBlack
import com.docuvio.app.theme.Cream
import com.docuvio.app.theme.LimeGreen
import com.docuvio.app.theme.MediumGray
import com.docuvio.app.theme.OffWhite
import com.docuvio.app.utils.ShopStatusResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --------------------------------------------------
// 🏠 HOME SCREEN
// --------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    tokenManager: com.docuvio.app.core.auth.TokenManager,
    notificationApi: com.docuvio.app.data.api.NotificationApi,
    onScheduleClick: (String) -> Unit,
    onOrderNowClick: (String) -> Unit
){
    val uiState by viewModel.uiState.collectAsState()
    // ✅ ADD THIS HERE
    LaunchedEffect(Unit) {
        viewModel.loadShops()
    }

    val pullToRefreshState = rememberPullToRefreshState()

    var searchQuery by remember { mutableStateOf("") }

    val filteredShops by remember(searchQuery, uiState.shops) {
        derivedStateOf {
            if (searchQuery.isBlank()) {
                uiState.shops
            } else {
                uiState.shops.filter {
                    it.shopName.contains(searchQuery, true) ||
                            it.block.contains(searchQuery, true)
                }
            }.sortedBy { !it.isActive }
        }
    }

    val isSearching = searchQuery.isNotBlank()
    val noSearchResults = isSearching && filteredShops.isEmpty()

    // ── Multiple Click Prevention ────────────────────
    var isNavigating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val handleNavigate = { action: (String) -> Unit, shopId: String ->
        if (!isNavigating) {
            isNavigating = true
            action(shopId)
            // Re-enable after a short delay or navigation cycle
            scope.launch {
                delay(1000)
                isNavigating = false
            }
        }
    }
    // ────────────────────────────────────────────────

    PullToRefreshBox(
        state = pullToRefreshState,
        isRefreshing = uiState.isRefreshing,
        onRefresh = { viewModel.refreshShops() },
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = uiState.isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = Color.White,
                color = LimeGreen
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Cream)
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.docuvio_logo_512_png),
                    contentDescription = "App Logo",
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Available Shops",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    fontFamily = Inter,
                    color = AlmostBlack
                )
            }

            Spacer(Modifier.height(12.dp))

            FancySearchBar(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                onClearClick = { searchQuery = "" }
            )

            Spacer(Modifier.height(16.dp))

            when {
                // Handle initial loading and gap: Show skeletons if we have no shops and no error yet
                uiState.shops.isEmpty() && uiState.error == null -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(6) { SkeletonShopCard() }
                    }
                }

                noSearchResults -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(painter = painterResource(R.drawable.noshop), contentDescription = "No shops found")
                    }
                }

                uiState.shops.isEmpty() && uiState.error != null -> {
                    // Show connection lost image
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        item {
                            Image(painter = painterResource(R.drawable.connectionlost), contentDescription = "No internet")
                        }
                    }
                }

                else -> {
                    // Show the actual shops
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 90.dp)
                    ) {
                        items(filteredShops) { shop ->
                            ShopCard(
                                shop = shop,
                                onScheduleClick = { handleNavigate(onScheduleClick, it) },
                                onOrderNowClick = { handleNavigate(onOrderNowClick, it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------------------------------
// 🔍 SEARCH BAR
// --------------------------------------------------
@Composable
fun FancySearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClearClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, AlmostBlack.copy(alpha = 0.6f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp)
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = AlmostBlack.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text("Search shop by name or block", color = MediumGray.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(color = AlmostBlack, fontSize = 16.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = MediumGray, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

// --------------------------------------------------
// ✨ SHOP CARD
// --------------------------------------------------
@Composable
fun ShopCard(
    shop: Shop,
    onScheduleClick: (String) -> Unit,
    onOrderNowClick: (String) -> Unit
) {
    val capabilities = ShopStatusResolver.resolve(shop)

    val walkInEnabled = capabilities.walkInEnabled
    val onlineEnabled = capabilities.onlineEnabled
    val bannerText = capabilities.bannerText
    val bannerBg = capabilities.bannerBg
    val bannerTextColor = capabilities.bannerTextColor

    Log.d("ShopDebug", "Shop=${shop.shopName} walkIn=$walkInEnabled online=$onlineEnabled banner=$bannerText")

    val shouldShowClosingTime = walkInEnabled || onlineEnabled
    val timeText = if (shouldShowClosingTime) "Closes at ${formatTime(shop.closeTime)}"
    else "Opens at ${formatTime(shop.openTime)}"

    val isVisuallyActive = walkInEnabled || onlineEnabled
    val cardBrush = if (isVisuallyActive) {
        Brush.linearGradient(listOf(Color(0xFF9CCC65), Color(0xFF7CB342)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFE0E0E0), Color(0xFFBDBDBD)))
    }

    Card(
        modifier = Modifier.fillMaxWidth().height(290.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBrush)
                .padding(12.dp)
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shop.shopName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isVisuallyActive) Color.White else Color(0xFF616161)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Level: ${shop.block}",
                        fontSize = 16.sp,
                        color = if (isVisuallyActive) Color.White.copy(alpha = 0.95f) else Color(0xFF9E9E9E)
                    )
                }
                Box(
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.Storefront, contentDescription = null, tint = Color.White, modifier = Modifier.size(46.dp))
                }
            }

            Spacer(Modifier.height(10.dp))
            ShopStatusBanner(text = bannerText, background = bannerBg, textColor = bannerTextColor)
            Spacer(Modifier.height(8.dp))
            ShopTimeBanner(text = timeText, background = Color.White.copy(alpha = 0.9f), textColor = Color(0xFF2E7D32))
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThreeDButton(text = "Schedule", enabled = onlineEnabled, onClick = { onScheduleClick(shop.id) }, modifier = Modifier.weight(1f))
                ThreeDButton(text = "Order Now", enabled = walkInEnabled, onClick = { onOrderNowClick(shop.id) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ShopTimeBanner(text: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(background).padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = textColor, maxLines = 1)
    }
}

private fun formatTime(time: String): String {
    return try {
        val parts = time.split(":")
        val hour24 = parts[0].toInt()
        val minute = parts[1]
        val hour12 = when { hour24 == 0 -> 12; hour24 > 12 -> hour24 - 12; else -> hour24 }
        val amPm = if (hour24 < 12) "AM" else "PM"
        "$hour12:$minute $amPm"
    } catch (_: Exception) { time }
}

// --------------------------------------------------
// 💀 SKELETON
// --------------------------------------------------
@Composable
fun SkeletonShopCard() {
    val shimmer = rememberShimmerBrush()
    Card(
        modifier = Modifier.fillMaxWidth().height(290.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)).background(OffWhite).padding(12.dp)
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.height(24.dp).fillMaxWidth(0.7f).clip(RoundedCornerShape(6.dp)).background(shimmer))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.height(15.dp).fillMaxWidth(0.45f).clip(RoundedCornerShape(6.dp)).background(shimmer))
                }
                Box(modifier = Modifier.size(88.dp).clip(RoundedCornerShape(16.dp)).background(shimmer))
            }
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(12.dp)).background(shimmer))
            Spacer(Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(22.dp).clip(RoundedCornerShape(10.dp)).background(shimmer))
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(shimmer))
                Box(modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(10.dp)).background(shimmer))
            }
        }
    }
}

// --------------------------------------------------
// ✨ SHIMMER BRUSH
// --------------------------------------------------
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "x"
    )
    return Brush.linearGradient(
        colors = listOf(
            MediumGray.copy(alpha = 0.6f),   // base — darker
            OffWhite.copy(alpha = 0.95f),     // peak — near opaque white
            MediumGray.copy(alpha = 0.6f),   // base — darker
        ),
        start = Offset(x - 300f, 0f),
        end = Offset(x, 600f)
    )
}

// --------------------------------------------------
// 🏪 STATUS BANNER
// --------------------------------------------------
@Composable
fun ShopStatusBanner(text: String, background: Color, textColor: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(background).padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textColor, maxLines = 1)
    }
}

// --------------------------------------------------
// 🔘 3D BUTTON — animates on tap, not on hold
// --------------------------------------------------
@Composable
fun ThreeDButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonHeight = 64.dp
    val depth = 6.dp

    // Animatable gives full control: snap down instantly on tap, spring back, then fire callback
    val offsetY = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.height(buttonHeight), contentAlignment = Alignment.TopCenter) {

        // Shadow / depth layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight - depth)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(12.dp))
                .background(if (enabled) Color(0xFF4A7C20) else Color(0xFF9E9E9E))
        )

        // Top surface
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(buttonHeight - depth - 2.dp)
                .offset(y = offsetY.value.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    brush = Brush.verticalGradient(
                        if (enabled) listOf(Color(0xFFFFFFFF), Color(0xFFE8E8E8))
                        else listOf(Color(0xFFDDDDDD), Color(0xFFCCCCCC))
                    )
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled
                ) {
                    scope.launch {
                        offsetY.animateTo(depth.value, animationSpec = tween(55, easing = EaseIn))
                        offsetY.animateTo(0f, animationSpec = tween(110, easing = EaseOut))
                        onClick()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (enabled) Color(0xFF1B5E20) else Color(0xFF8E8E8E)
            )
        }
    }
}
