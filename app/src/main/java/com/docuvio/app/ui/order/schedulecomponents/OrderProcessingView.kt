package com.docuvio.app.ui.order.schedulecomponents

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.theme.*
import com.docuvio.app.viewmodel.OrderStep

private data class OrderFlowStep(val step: OrderStep, val label: String, val sub: String)

private val ORDER_FLOW_STEPS = listOf(
    OrderFlowStep(OrderStep.CREATING_ORDER,     "Creating order",     "Setting up your order"),
    OrderFlowStep(OrderStep.UPLOADING,          "Uploading document", "Sending file to server"),
    OrderFlowStep(OrderStep.ATTACHING_DOCUMENT, "Attaching document", "Linking file to order"),
    OrderFlowStep(OrderStep.PROCESSING_PAYMENT, "Preparing payment",  "Initialising Razorpay"),
)

@Composable
fun OrderProcessingView(
    step: OrderStep,
    uploadProgress: Int,
    error: String?,
    documentPrice: Int,
    platformFee: Int,
    handlingFee: Int = 0,
    total: Int,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
) {
    val isFailed = error != null
    val currentIndex = ORDER_FLOW_STEPS.indexOfFirst { it.step == step }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(40.dp))

        AnimatedContent(
            targetState = if (isFailed) "Something went wrong"
            else ORDER_FLOW_STEPS.getOrNull(currentIndex)?.label ?: "Processing…",
            transitionSpec = {
                slideInVertically { it / 2 } + fadeIn() togetherWith
                        slideOutVertically { -it / 2 } + fadeOut()
            },
            label = "order_proc_title"
        ) { title ->
            Text(
                title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = if (isFailed) Color(0xFFD32F2F) else AlmostBlack,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(6.dp))

        AnimatedContent(
            targetState = when {
                isFailed -> error
                step == OrderStep.UPLOADING -> "$uploadProgress% uploaded"
                else -> ORDER_FLOW_STEPS.getOrNull(currentIndex)?.sub ?: ""
            },
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
            label = "order_proc_sub"
        ) { sub ->
            Text(sub, fontSize = 14.sp, color = MediumGray, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(48.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AlmostBlack.copy(alpha = 0.04f))
                .padding(vertical = 8.dp)
        ) {
            ORDER_FLOW_STEPS.forEachIndexed { i, flowStep ->
                val s = when {
                    i < currentIndex               -> OrderStepState.Done
                    i == currentIndex && !isFailed -> OrderStepState.Active
                    i == currentIndex && isFailed  -> OrderStepState.Failed
                    else                           -> OrderStepState.Pending
                }
                OrderStepRow(
                    label = flowStep.label,
                    state = s,
                    uploadProgress = if (flowStep.step == OrderStep.UPLOADING && s == OrderStepState.Active)
                        uploadProgress / 100f else null
                )
                if (i < ORDER_FLOW_STEPS.lastIndex) {
                    Box(
                        Modifier
                            .padding(start = 34.dp)
                            .width(2.dp)
                            .height(14.dp)
                            .background(
                                if (i < currentIndex) PrimaryGreen.copy(alpha = 0.4f)
                                else AlmostBlack.copy(alpha = 0.1f)
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))

        BillBreakdown(
            documentPrice = documentPrice,
            platformFee = platformFee,
            handlingFee = handlingFee,
            total = total
        )

        Spacer(Modifier.height(24.dp))

        if (isFailed) {
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
            ) {
                Text("Try Again", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text(
                "Cancel",
                color = if (isFailed) MediumGray else Color(0xFFD32F2F),
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }

        Spacer(Modifier.height(32.dp))
    }
}

private enum class OrderStepState { Pending, Active, Done, Failed }

@Composable
private fun OrderStepRow(
    label: String,
    state: OrderStepState,
    uploadProgress: Float?
) {
    val inf = rememberInfiniteTransition(label = "pulse")
    val pulse by inf.animateFloat(
        0.85f, 1f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "p"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .scale(if (state == OrderStepState.Active) pulse else 1f)
                .clip(CircleShape)
                .background(
                    when (state) {
                        OrderStepState.Done    -> PrimaryGreen
                        OrderStepState.Active  -> PrimaryGreen.copy(alpha = 0.15f)
                        OrderStepState.Failed  -> Color(0xFFFFEDED)
                        OrderStepState.Pending -> AlmostBlack.copy(alpha = 0.08f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                OrderStepState.Done    -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                OrderStepState.Active  -> CircularProgressIndicator(color = PrimaryGreen, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                OrderStepState.Failed  -> Icon(Icons.Default.Close, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                OrderStepState.Pending -> Unit
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 15.sp,
                fontWeight = if (state == OrderStepState.Active) FontWeight.SemiBold else FontWeight.Normal,
                color = when (state) {
                    OrderStepState.Done, OrderStepState.Active -> AlmostBlack
                    OrderStepState.Failed  -> Color(0xFFD32F2F)
                    OrderStepState.Pending -> MediumGray
                }
            )
            if (uploadProgress != null) {
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { uploadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = PrimaryGreen,
                    trackColor = PrimaryGreen.copy(alpha = 0.15f)
                )
            }
        }

        AnimatedVisibility(
            visible = state == OrderStepState.Done,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)),
            exit = scaleOut()
        ) {
            Text(
                "Done",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = PrimaryGreen,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(PrimaryGreen.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
fun BillBreakdown(
    documentPrice: Int,
    platformFee: Int,
    handlingFee: Int = 0,
    total: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AlmostBlack.copy(alpha = 0.04f))
            .border(1.dp, AlmostBlack.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Bill Summary",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MediumGray,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(2.dp))

        BillRow(label = "Documents", amount = documentPrice)
        BillRow(label = "Platform Fee", amount = platformFee)

        if (handlingFee > 0) {
            BillRow(
                label = "Handling Fee",
                amount = handlingFee,
                labelColor = CoralRed.copy(alpha = 0.85f),
                amountColor = CoralRed
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 2.dp),
            thickness = 1.dp,
            color = AlmostBlack.copy(alpha = 0.10f)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = AlmostBlack
            )
            Text(
                text = "₹$total",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PrimaryGreen
            )
        }
    }
}

@Composable
private fun BillRow(
    label: String,
    amount: Int,
    labelColor: Color = MediumGray,
    amountColor: Color = AlmostBlack
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Normal, color = labelColor)
        Text(text = "₹$amount", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = amountColor)
    }
}