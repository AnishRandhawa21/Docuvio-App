package com.docuvio.app.ui.orders

import com.docuvio.app.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.docuvio.app.theme.*
import com.docuvio.app.viewmodel.OrdersViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState


/* -------------------------------------------------------------------------- */
/*                                   SCREEN                                   */
/* -------------------------------------------------------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadOrders(force = true)
    }

    val pullToRefreshState = rememberPullToRefreshState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var expandedOrderId by remember { mutableStateOf<String?>(null) }
    var isHeaderVisible by remember { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // When user scrolls down (y is negative), hide header
                if (available.y < -15f && isHeaderVisible) {
                    isHeaderVisible = false
                }
                // When user scrolls up (y is positive), show header
                if (available.y > 15f && !isHeaderVisible) {
                    isHeaderVisible = true
                }
                return Offset.Zero
            }
        }
    }

    val tabs = listOf("Current Orders", "History")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream
    ) {

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadOrders(force = true) },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isLoading,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Color.White,
                    color = SoftPink
                )
            }
        ) {

            Column(
                modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 0.dp)
            ) {

                AnimatedVisibility(
                    visible = isHeaderVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 0.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            text = "Orders",
                            style = MaterialTheme.typography.headlineLarge,
                            color = AlmostBlack
                        )
                    }
                }

                Surface(
                    shadowElevation = 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, AlmostBlack.copy(alpha = 0.6f)),
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Box {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = AlmostBlack,
                            divider = {},
                            indicator = { tabPositions ->
                                Box(
                                    modifier = Modifier
                                        .tabIndicatorOffset(tabPositions[selectedTab])
                                        .fillMaxHeight()
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(AlmostBlack)
                                        .zIndex(-1f)
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index

                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedTab = index },
                                    modifier = Modifier.zIndex(1f),
                                    text = {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isSelected) Color.White else AlmostBlack.copy(alpha = 0.8f),
                                            // bumped: Bold/SemiBold instead of Bold/Medium — keeps
                                            // unselected tab readable at the same weight family
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                val orders =
                    if (selectedTab == 0)
                        uiState.currentOrders
                    else
                        uiState.orderHistory

                when {

                    uiState.isLoading &&
                            uiState.currentOrders.isEmpty() &&
                            uiState.orderHistory.isEmpty() -> {

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(6) {
                                SkeletonOrderCard()
                            }
                        }
                    }

                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = CoralRed
                            )
                        }
                    }

                    orders.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {

                            Image(
                                painter = painterResource(id = R.drawable.no_order),
                                contentDescription = "NO_Order",
                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = orders,
                                key = { it.id }
                            ) { order ->
                                ExpandableOrderCard(
                                    order = order,
                                    expanded = expandedOrderId == order.id,
                                    isHistory = selectedTab == 1,
                                    onClick = {
                                        expandedOrderId =
                                            if (expandedOrderId == order.id) null
                                            else order.id
                                    }
                                )
                            }
                        }
                    }
                }

            }
        }
    }
}