package com.docuvio.app.ui.home

import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.docuvio.app.R
import com.docuvio.app.data.model.Shop
import com.docuvio.app.viewmodel.HomeViewModel
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.docuvio.app.firebase.registerFcmToken
import com.docuvio.app.theme.*
import com.docuvio.app.utils.ShopStatusResolver
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// --------------------------------------------------
// FILTER TYPES
// --------------------------------------------------
enum class ShopFilter(val label: String) {
    ALL("All"),
    OPEN("Open"),
    CLOSED("Closed"),
    ONLINE("Online")
}

// --------------------------------------------------
// --------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    tokenManager: com.docuvio.app.core.auth.TokenManager,
    notificationApi: com.docuvio.app.data.api.NotificationApi,
    onScheduleClick: (String) -> Unit,
    onOrderNowClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadShops()
    }

    val pullToRefreshState = rememberPullToRefreshState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ShopFilter.ALL) }

    val filteredShops by remember(searchQuery, selectedFilter, uiState.shops) {
        derivedStateOf {
            uiState.shops
                .filter {
                    searchQuery.isBlank() ||
                            it.shopName.contains(searchQuery, true) ||
                            it.block.contains(searchQuery, true)
                }
                .filter { shop ->
                    val capabilities = ShopStatusResolver.resolve(shop)
                    when (selectedFilter) {
                        ShopFilter.ALL -> true
                        ShopFilter.OPEN -> capabilities.walkInEnabled || capabilities.onlineEnabled
                        ShopFilter.CLOSED -> !capabilities.walkInEnabled && !capabilities.onlineEnabled
                        ShopFilter.ONLINE -> capabilities.onlineEnabled
                    }
                }
                .sortedBy { !it.isActive }
        }
    }

    val isSearching = searchQuery.isNotBlank()
    val noSearchResults = (isSearching || selectedFilter != ShopFilter.ALL) && filteredShops.isEmpty() && uiState.shops.isNotEmpty()

    // ── Multiple Click Prevention ────────────────────
    var isNavigating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()

    val headerAlpha by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) 0f
            else (1f - (scrollState.firstVisibleItemScrollOffset.toFloat() / 500f)).coerceIn(0f, 1f)
        }
    }

    val headerTranslationY by remember {
        derivedStateOf {
            if (scrollState.firstVisibleItemIndex > 0) -40f
            else (-(scrollState.firstVisibleItemScrollOffset.toFloat() / 12f)).coerceIn(-40f, 0f)
        }
    }

    // ────────────────────────────────────────────────

    val handleNavigate = { action: (String) -> Unit, shopId: String ->
        if (!isNavigating) {
            isNavigating = true
            action(shopId)
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
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        LazyColumn(
            state = scrollState,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // --- SCROLLABLE HEADER (Hides on scroll) ---
            item {
                AnimatedVisibility(
                    visible = searchQuery.isBlank() && selectedFilter == ShopFilter.ALL,
                    enter = expandVertically(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500)),
                    exit = shrinkVertically(animationSpec = tween(500)) + fadeOut(animationSpec = tween(500))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .graphicsLayer {
                                alpha = headerAlpha
                                translationY = headerTranslationY
                            }
                    ) {
                        Spacer(Modifier.height(8.dp))
                        HomeBrandBanner()
                    }
                }
            }

            // --- STICKY SEARCH & FILTER (Stays at top) ---
            stickyHeader {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            FancySearchBar(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                onClearClick = { searchQuery = "" }
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        FilterChipRow(
                            selected = selectedFilter,
                            onSelect = { selectedFilter = it }
                        )
                    }
                }
            }

            // --- MAIN CONTENT ---
            item {
                Spacer(Modifier.height(4.dp))
            }

            when {
                uiState.shops.isEmpty() && uiState.error == null -> {
                    items(6) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            SkeletonShopCard()
                        }
                    }
                }

                noSearchResults -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxHeight(0.7f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(R.drawable.noshop),
                                    contentDescription = "No shops found"
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    text = if (isSearching) "No shops match \"$searchQuery\""
                                    else "No ${selectedFilter.label.lowercase()} shops right now",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = Manrope,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                uiState.shops.isEmpty() && uiState.error != null -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillParentMaxSize()
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.connectionlost),
                                contentDescription = "No internet"
                            )
                        }
                    }
                }

                else -> {
                    items(filteredShops, key = { it.id }) { shop ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .animateItem()
                        ) {
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
// 🎨 HOME BRAND BANNER — now a single pre-made image,
// with its own entrance fade/slide-in animation,
// independent of the scroll-linked header fade above.
// --------------------------------------------------
@Composable
fun HomeBrandBanner() {
    Image(
        painter = painterResource(R.drawable.banner1),
        contentDescription = "Docuvio — find nearby print shops",
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2.57f) // match your exported banner's width:height ratio — adjust if yours differs
            .clip(RoundedCornerShape(24.dp)),
        contentScale = ContentScale.FillWidth
    )
}

// --------------------------------------------------
// 🧭 FILTER CHIP ROW
// --------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipRow(
    selected: ShopFilter,
    onSelect: (ShopFilter) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ShopFilter.entries.toTypedArray()) { filter ->
            val isSelected = filter == selected

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) SuccessGreen else MaterialTheme.colorScheme.surfaceVariant,
                label = "chipBackground"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                label = "chipContent"
            )
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                label = "chipScale"
            )

            Surface(
                modifier = Modifier
                    .height(38.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelect(filter) },
                shape = RoundedCornerShape(24.dp),
                color = backgroundColor,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Manrope,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// --------------------------------------------------
// 🔍 SEARCH BAR — tonal surface instead of stark white
// --------------------------------------------------
@Composable
fun FancySearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClearClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 20.dp)
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = "Search",
                tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        "Search shop by name or block",
                        color = MediumGray.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MediumGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// --------------------------------------------------
// 💀 SKELETON
// --------------------------------------------------
@Composable
fun SkeletonShopCard() {
    val shimmerBrush = rememberShimmerBrush()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceCream)
                .background(shimmerBrush)
                .padding(20.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkGray.copy(alpha = 0.08f))
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .fillMaxWidth(0.65f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkGray.copy(alpha = 0.10f))
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .height(16.dp)
                            .fillMaxWidth(0.35f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkGray.copy(alpha = 0.07f))
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(40.dp)
                        .width(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkGray.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkGray.copy(alpha = 0.06f))
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkGray.copy(alpha = 0.08f))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkGray.copy(alpha = 0.08f))
                )
            }
        }
    }
}

// --------------------------------------------------
// ✨ SHIMMER BRUSH — barely-there sweep
// --------------------------------------------------
@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing)
        ),
        label = "x"
    )
    return Brush.linearGradient(
        colors = listOf(
            Color.Transparent,
            White.copy(alpha = 0.15f),
            Color.Transparent
        ),
        start = Offset(x - 300f, 0f),
        end = Offset(x, 600f)
    )
}