package com.docuvio.app.ui.orders

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.data.model.Order
import com.docuvio.app.data.model.lastSix
import com.docuvio.app.theme.*
import com.docuvio.app.ui.order.utils.formatPickupDateTime
import com.docuvio.app.utils.formatOrderDate

/* -------------------------------------------------------------------------- */
/*                              EXPANDABLE CARD                                */
/* -------------------------------------------------------------------------- */

@Composable
fun ExpandableOrderCard(
    order: Order,
    expanded: Boolean,
    isHistory: Boolean,
    onClick: () -> Unit
) {
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
            .then(
                if (isHistory) Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) { onClick() }
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {

            /* ---------------- HEADER ROW ---------------- */

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Order #${order.orderNo}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = AlmostBlack
                    )
                    Text(
                        text = "ID: #${order.id.lastSix()}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = AlmostBlack.copy(alpha = 0.6f)
                    )
                    order.shop?.shopName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = AlmostBlack
                        )
                    }
                    formatOrderDate(order.createdAt)?.let {
                        Text(
                            text = "Placed on $it",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = AlmostBlack.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    OrderStatusChip(if (order.isExpired) "expired" else order.status)
                    if (isHistory) {
                        Spacer(Modifier.height(10.dp))
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = AlmostBlack.copy(alpha = 0.6f),
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
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(color = MediumGray.copy(alpha = 0.25f))
                    Spacer(Modifier.height(14.dp))

                    // ---------- PICKUP TIME ----------
                    order.pickupAt?.let { pickupAt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pickup at:",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AlmostBlack.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = formatPickupDateTime(pickupAt),
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = AlmostBlack
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = MediumGray.copy(alpha = 0.25f))
                        Spacer(Modifier.height(14.dp))
                    }

                    // ---------- BILL (history only) ----------
                    if (isHistory) {
                        OrderBillSection(order)
                        Spacer(Modifier.height(4.dp))
                    }

                    // ---------- AMOUNT + PAYMENT STATUS (current orders) ----------
                    if (!isHistory) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Amount",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AlmostBlack.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "₹${order.totalPrice}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = AlmostBlack
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Payment Status",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AlmostBlack.copy(alpha = 0.7f)
                            )
                            PaymentStatusChip(if (order.isPaid) "paid" else "pending")
                        }

                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = MediumGray.copy(alpha = 0.25f))
                        Spacer(Modifier.height(14.dp))

                        // ---------- DOCUMENT SUMMARY ----------
                        order.documents?.firstOrNull()?.let { doc ->
                            Text(
                                text = "${doc.fileName ?: "Document"} • " +
                                        "${doc.pageCount ?: 0} pages • " +
                                        "${doc.copies ?: 1} copies",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = AlmostBlack.copy(alpha = 0.7f)
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
                            Spacer(Modifier.height(14.dp))
                            HorizontalDivider(color = MediumGray.copy(alpha = 0.25f))
                            Spacer(Modifier.height(14.dp))
                            PickupOtpBox(otp = order.deliveryOtp!!)
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
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1F5A3D)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Pickup Code",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = White.copy(alpha = 0.9f)
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = otp,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = White,
                letterSpacing = 4.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Show this code at the shop counter to collect your prints",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/*                                   CHIPS                                    */
/* -------------------------------------------------------------------------- */

@Composable
fun OrderStatusChip(status: String) {

    val color = when (status.lowercase()) {
        "expired" -> Color(0xFFD32F2F)
        "pending" -> Color(0xFFF9A825)
        "confirmed" -> Blue
        "processing" -> DarkBlue
        "ready" -> Color(0xFF388E3C)
        "completed" -> SuccessGreen
        "cancelled" -> Color(0xFFD32F2F)
        else -> MediumGray
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = White,
            maxLines = 1
        )
    }
}

@Composable
fun PaymentStatusChip(status: String) {

    val color = when (status.lowercase()) {
        "paid" -> Color(0xFF689F38)
        "pending" -> DeepAmber
        "failed" -> Color(0xFF6E2B2B)
        else -> MediumGray
    }

    Surface(
        color = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = status.uppercase(),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = White,
            maxLines = 1
        )
    }
}

@Composable
fun SkeletonOrderCard() {
    val shimmer = rememberShimmerBrush()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = OffWhite),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.height(18.dp).fillMaxWidth(0.45f).clip(RoundedCornerShape(6.dp)).background(shimmer))
                Box(modifier = Modifier.height(13.dp).fillMaxWidth(0.3f).clip(RoundedCornerShape(6.dp)).background(shimmer))
                Box(modifier = Modifier.height(13.dp).fillMaxWidth(0.4f).clip(RoundedCornerShape(6.dp)).background(shimmer))
            }

            Box(
                modifier = Modifier
                    .height(22.dp)
                    .width(72.dp)
                    .clip(RoundedCornerShape(8.dp))
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
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "x"
    )

    return Brush.linearGradient(
        colors = listOf(
            MediumGray.copy(alpha = 0.5f),
            OffWhite.copy(alpha = 0.2f),
            MediumGray.copy(alpha = 0.5f)
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
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
            else
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = AlmostBlack
        )

        Text(
            text = value,
            style = if (bold)
                MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
            else
                MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = AlmostBlack
        )
    }
}

//Bill breakdown composable
@Composable
fun OrderBillSection(order: Order) {

    val documents = order.documents ?: return

    val totalPages =
        documents.sumOf { doc ->
            (doc.pageCount ?: 0) * (doc.copies ?: 1)
        }

    val pricePerPage =
        if (totalPages == 0) 0.0
        else order.totalPrice.toDouble() / totalPages

    Column {

        Text(
            text = "Bill Summary",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
            color = AlmostBlack
        )

        Spacer(Modifier.height(10.dp))

        documents.forEach { doc ->

            val pages = doc.pageCount ?: 0
            val copies = doc.copies ?: 1
            val subtotal = pages * copies * pricePerPage

            Text(
                text = doc.fileName ?: "Document",
                color = Blue,
                fontSize = 13.sp,
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(4.dp))

            BillRow(
                title = "$pages pages × ₹${"%.2f".format(pricePerPage)} × $copies copies",
                value = "₹${"%.2f".format(subtotal)}"
            )

            Spacer(Modifier.height(10.dp))
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = MediumGray.copy(alpha = 0.25f)
        )

        Spacer(Modifier.height(10.dp))

        BillRow(
            title = "Total",
            value = "₹${order.totalPrice}",
            bold = true
        )
    }
}