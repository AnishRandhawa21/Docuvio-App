package com.docuvio.app.ui.orders

import com.docuvio.app.R
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.data.model.Order
import com.docuvio.app.theme.Inter
import com.docuvio.app.utils.formatOrderDate
import com.docuvio.app.viewmodel.OrdersViewModel
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.docuvio.app.data.model.lastSix
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.res.painterResource
import com.docuvio.app.theme.AlmostBlack // ADDED: Import theme colors
import com.docuvio.app.theme.Cream // ADDED: Import theme colors
import com.docuvio.app.theme.DeepAmber // ADDED: Import theme colors
import com.docuvio.app.theme.MediumGray // ADDED: Import theme colors
import com.docuvio.app.theme.OffWhite // ADDED: Import theme colors
import com.docuvio.app.theme.SoftPink
import androidx.compose.ui.zIndex
import com.docuvio.app.theme.CoralRed
import com.docuvio.app.ui.order.utils.formatPickupDateTime


/* -------------------------------------------------------------------------- */
/*                                   SCREEN                                   */
/* -------------------------------------------------------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    viewModel: OrdersViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    val pullToRefreshState = rememberPullToRefreshState()

    var selectedTab by remember { mutableStateOf(0) }
    var expandedOrderId by remember { mutableStateOf<String?>(null) }

    val tabs = listOf("Current Orders", "History")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Cream // CHANGED: from Color(0xFF151419) to Cream
    ) {

        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = { viewModel.loadOrders() },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize(),
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isLoading,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Color.White, // CHANGED: from Color(0xFF363636) to Color.White
                    color = SoftPink // CHANGED: from Color(0xFFFF9500) to GoldenYellow
                )
            }
        ) {

            Column(
                modifier = Modifier.padding(top = 0.dp, start = 16.dp, end = 16.dp, bottom = 0.dp) // CHANGED: Added padding control
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "Orders",
                        style = MaterialTheme.typography.headlineLarge.copy( // CHANGED: Added font weight
                            fontWeight = FontWeight.Bold
                        ),
                        fontFamily = Inter,
                        color = AlmostBlack // CHANGED: from Color(0xFF878787) to AlmostBlack
                    )
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
                        // Animated sliding indicator - BEHIND the tabs
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
                                        .zIndex(-1f) // Put it BEHIND the text
                                )
                            }
                        ) {
                            tabs.forEachIndexed { index, title ->
                                val isSelected = selectedTab == index

                                Tab(
                                    selected = isSelected,
                                    onClick = { selectedTab = index },
                                    modifier = Modifier.zIndex(1f), // Put text in FRONT
                                    text = {
                                        Text(
                                            title,
                                            color = if (isSelected) Color.White else AlmostBlack.copy(alpha = 0.6f),
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
                            contentPadding = PaddingValues(vertical = 16.dp), // CHANGED: only vertical padding
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
                                color = CoralRed // CHANGED: using error red color
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
//                            Text(
//                                text =
//                                    if (selectedTab == 0)
//                                        "No current orders"
//                                    else
//                                        "No order history",
//                                color = MediumGray // CHANGED: from Color.Gray to MediumGray
//                            )
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp), // CHANGED: only vertical padding
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


/* -------------------------------------------------------------------------- */
/*                              OTP                                           */
/* -------------------------------------------------------------------------- */

@Composable
fun PickupOtpBox(otp: String) {

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1F5A3D) // KEPT: Green color for OTP box (good for contrast)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Pickup Code",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFB6EACB) // KEPT: Light green for text
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = otp,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Show this code at the shop counter to collect your prints",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFB6EACB), // KEPT: Light green for text
                textAlign = TextAlign.Center
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                              EXPANDABLE CARD                                */
/* -------------------------------------------------------------------------- */

@Composable
fun StrokedText(
    text: String,
    textColor: Color,
    strokeColor: Color,
    strokeWidth: Float,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val strokeStyle = remember(strokeColor, strokeWidth, fontSize, fontWeight) {
        TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = strokeColor,
            drawStyle = Stroke(
                width = strokeWidth,
                join = StrokeJoin.Round
            )
        )
    }

    val fillStyle = remember(textColor, fontSize, fontWeight) {
        TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = textColor
        )
    }

    Box {
        Text(text = text, style = strokeStyle)
        Text(text = text, style = fillStyle)
    }
}

@Composable
fun ExpandableOrderCard(
    order: Order,
    expanded: Boolean,
    isHistory: Boolean,
    onClick: () -> Unit
) {
    // Current orders are always expanded — arrow only shows in history
    val alwaysExpanded = !isHistory
    val isExpanded = alwaysExpanded || expanded

    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "arrow_rotation"
    )
    val interactionSource = remember { MutableInteractionSource() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            // Only clickable in history mode
            .then(
                if (isHistory) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            /* ---------------- HEADER ROW ---------------- */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Order #${order.orderNo}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = AlmostBlack
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "ID: #${order.id.lastSix()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumGray
                    )
                    order.shop?.shopName?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = AlmostBlack
                        )
                    }
                    formatOrderDate(order.createdAt)?.let {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Placed on $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MediumGray
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    OrderStatusChip(if (order.isExpired) "expired" else order.status)
                    // Arrow only shown in history (current orders are always open)
                    if (isHistory) {
                        Spacer(Modifier.height(8.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = AlmostBlack,
                            modifier = Modifier
                                .size(22.dp)
                                .rotate(rotation)
                        )
                    }
                }
            }

            /* ---------------- EXPANDED CONTENT ---------------- */

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(animationSpec = tween(200, easing = LinearOutSlowInEasing)) +
                        expandVertically(animationSpec = tween(360, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing)) +
                        shrinkVertically(animationSpec = tween(380, easing = FastOutLinearInEasing))
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = MediumGray.copy(alpha = 0.3f))
                    Spacer(Modifier.height(12.dp))

                    // ---------- PICKUP TIME ----------
                    order.pickupAt?.let { pickupAt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pickup at:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MediumGray
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = formatPickupDateTime(pickupAt),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = AlmostBlack
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MediumGray.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                    }

                    // ---------- BILL (history only) ----------
                    if (isHistory) {
                        OrderBillSection(order)
                        Spacer(Modifier.height(10.dp))
                    }

                    // ---------- AMOUNT + PAYMENT STATUS (current orders) ----------
                    if (!isHistory) {

                        // Amount row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Amount",
                                style = MaterialTheme.typography.bodySmall,
                                color = MediumGray
                            )
                            Text(
                                text = "₹${order.totalPrice}",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = AlmostBlack
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        // Payment status row — right below amount, tight and aligned
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MediumGray
                            )
                            PaymentStatusChip(if (order.isPaid) "paid" else "pending")
                        }

                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = MediumGray.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))

                        // ---------- DOCUMENT SUMMARY ----------
                        order.documents?.firstOrNull()?.let { doc ->
                            Text(
                                text = "${doc.fileName ?: "Document"} • " +
                                        "${doc.pageCount ?: 0} pages • " +
                                        "${doc.copies ?: 1} copies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MediumGray
                            )
                        }
                    }

                    // ---------- OTP BOX (ready state) ----------
                    AnimatedVisibility(
                        visible = order.status == "ready" &&
                                !order.isExpired &&
                                !order.otpVerified &&
                                !order.deliveryOtp.isNullOrBlank(),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(Modifier.height(12.dp))
                            HorizontalDivider(color = MediumGray.copy(alpha = 0.3f))
                            Spacer(Modifier.height(12.dp))
                            PickupOtpBox(otp = order.deliveryOtp!!)
                        }
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                   CHIPS                                    */
/* -------------------------------------------------------------------------- */

@Composable
fun OrderStatusChip(status: String) {

    val color = when (status.lowercase()) {

        "expired" ->
            Color(0xFFD32F2F) // strong red

        "pending" ->
            Color(0xFFF9A825) // amber / yellow

        "confirmed" ->
            Color(0xFF1976D2) // BLUE (confirmation / success)

        "processing" ->
            Color(0xFF0288D1) // lighter blue (in progress)

        "ready" ->
            Color(0xFF388E3C) // GREEN (ready to go)

        "completed" ->
            Color(0xFF2E7D32) // darker green (final state)

        "cancelled" ->
            Color(0xFFD32F2F) // deep red

        else ->
            Color(0xFF757575) // neutral gray
    }


    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp) // CHANGED: from MaterialTheme.shapes.small to specific value
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}


@Composable
fun PaymentStatusChip(status: String) {

    val color = when (status.lowercase()) {
        "paid" -> Color(0xFF689F38) // KEPT: Green for paid
        "pending" -> DeepAmber // CHANGED: from Color(0xFF9C5A10) to DeepAmber
        "failed" -> Color(0xFF6E2B2B) // KEPT: Red for failed
        else -> MediumGray // CHANGED: from Color(0xFF4F4F4F) to MediumGray
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp) // CHANGED: from MaterialTheme.shapes.small to specific value
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}

@Composable
fun SkeletonOrderCard() {

    val shimmer = rememberShimmerBrush()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = OffWhite // CHANGED: from Color(0xFF1E1E1E) to Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp) // CHANGED: Added elevation
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Column {
                Box(
                    modifier = Modifier
                        .height(18.dp)
                        .fillMaxWidth(0.5f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .height(14.dp)
                        .fillMaxWidth(0.35f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(shimmer)
                )
            }

            Box(
                modifier = Modifier
                    .height(24.dp)
                    .width(80.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(12.dp))
                    .background(shimmer)
            )
        }
    }
}
@Composable
fun rememberShimmerBrush(): Brush {

    val transition = rememberInfiniteTransition(label = "shimmer")

    val x by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = LinearEasing
            )
        ),
        label = "x"
    )

    return Brush.linearGradient(
        colors = listOf(
            MediumGray.copy(alpha = 0.5f), // CHANGED: from Color(0xFF2A2A2A) to OffWhite
            OffWhite.copy(alpha = 0.2f), // CHANGED: from Color(0xFF3A3A3A) to MediumGray
            MediumGray.copy(alpha = 0.5f) // CHANGED: from Color(0xFF2A2A2A) to OffWhite
        ),
        start = Offset(x - 300f, 0f),
        end = Offset(x, 600f)
    )
}

//Billing system

@Composable
fun BillRow(
    title: String,
    value: String,
    bold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = if (bold)
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            else
                MaterialTheme.typography.bodySmall,
            color = AlmostBlack // CHANGED: from Color.White to AlmostBlack
        )

        Text(
            text = value,
            style = if (bold)
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            else
                MaterialTheme.typography.bodySmall,
            color = AlmostBlack // CHANGED: from Color.White to AlmostBlack
        )
    }
}

//Bill breakdown composable
@Composable
fun OrderBillSection(order: Order) {

    val documents = order.documents ?: return

    // Total printed pages across all documents
    val totalPages =
        documents.sumOf { doc ->
            (doc.pageCount ?: 0) * (doc.copies ?: 1)
        }

    // Derive per-page price
    val pricePerPage =
        if (totalPages == 0) 0.0
        else order.totalPrice.toDouble() / totalPages

    Column {

        Text(
            text = "Bill Summary",
            style = MaterialTheme.typography.labelMedium.copy( // CHANGED: Added bold
                fontWeight = FontWeight.Bold
            ),
            color = AlmostBlack // CHANGED: from Color.White to AlmostBlack
        )

        Spacer(Modifier.height(8.dp))

        documents.forEach { doc ->

            val pages = doc.pageCount ?: 0
            val copies = doc.copies ?: 1

            val subtotal =
                pages * copies * pricePerPage

            Text(
                text = doc.fileName ?: "Document",
                color = Color(0xFF1976D2), // CHANGED: from Color(0xFFFF9500) to GoldenYellow
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium // CHANGED: Added medium weight
            )

            Spacer(Modifier.height(4.dp))

            BillRow(
                title = "$pages pages × ₹${"%.2f".format(pricePerPage)} × $copies copies",
                value = "₹${"%.2f".format(subtotal)}"
            )

            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MediumGray.copy(alpha = 0.3f) // CHANGED: from Color(0xFF555555) to MediumGray with transparency
        )

        Spacer(Modifier.height(8.dp))

        BillRow(
            title = "Total",
            value = "₹${order.totalPrice}",
            bold = true
        )
    }
}