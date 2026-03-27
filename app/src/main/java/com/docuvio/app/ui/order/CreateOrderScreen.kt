package com.docuvio.app.ui.order

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuvio.app.BuildConfig
import com.docuvio.app.data.model.*
import com.docuvio.app.viewmodel.*
import com.razorpay.Checkout
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.docuvio.app.theme.*
import java.text.SimpleDateFormat
import java.util.*
import com.docuvio.app.ui.order.utils.DateUtils.isTomorrowPickup
import com.docuvio.app.ui.order.utils.FloatingPayBar
import com.docuvio.app.ui.order.utils.PricingUtils.calculateTotal
import com.docuvio.app.ui.order.utils.formatPickupDateTime
import com.docuvio.app.ui.order.utils.formatTime
import com.docuvio.app.ui.order.utils.toMinutes

private val PROCESSING_STEPS = setOf(
    OrderStep.CREATING_ORDER,
    OrderStep.UPLOADING,
    OrderStep.ATTACHING_DOCUMENT,
    OrderStep.PROCESSING_PAYMENT,
)

/* ─────────────────────────────────────────────────────────────────────────── */
/*  MAIN SCREEN                                                                */
/* ─────────────────────────────────────────────────────────────────────────── */

@Composable
fun CreateOrderScreen(
    shopId: String,
    viewModelFactory: CreateOrderViewModelFactory,
    onOrderSuccess: () -> Unit
) {
    val viewModel: CreateOrderViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val activity = remember(context) {
        var ctx: Context = context
        while (ctx is ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    // ── FIXED FILE PICKER ─────────────────────────────────────────────────────
    // Previously: mimeType was resolved but then thrown away before calling
    //             setFileAndReadPages — so the ViewModel and repository always
    //             uploaded with application/octet-stream, corrupting JPEGs on
    //             the server side.
    // Now:        canonical mimeType flows all the way into the ViewModel and
    //             from there into the multipart upload request.
    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            // Step 1: resolve true MIME from ContentResolver
            val rawMime = context.contentResolver.getType(uri) ?: "application/octet-stream"

            // Step 2: normalise — "image/jpg" is not a real MIME type
            val mimeType = when (rawMime.lowercase().trim()) {
                "image/jpg", "image/pjpeg" -> "image/jpeg"
                else -> rawMime
            }

            // Step 3: derive extension from the canonical MIME (not from filename)
            val extension = when {
                mimeType.contains("pdf")  -> "pdf"
                mimeType.contains("png")  -> "png"
                mimeType.contains("jpeg") -> "jpg"
                else -> "bin"
            }

            // Step 4: copy bytes — both streams in try-with-resources, flush before use
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                        output.flush()  // force OS buffer → disk before ViewModel reads file
                    }
                } ?: run {
                    viewModel.setError("Could not read the selected file. Please try another.")
                    return@rememberLauncherForActivityResult
                }
            } catch (e: IOException) {
                file.delete()  // don't leave a partial file in cache
                Log.e("FilePicker", "Copy failed for $uri", e)
                viewModel.setError("Failed to copy file. Please try again.")
                return@rememberLauncherForActivityResult
            }

            // Step 5: pass file AND its canonical mimeType to ViewModel
            viewModel.setFileAndReadPages(context, file, mimeType)
        }
    // ─────────────────────────────────────────────────────────────────────────

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onOrderSuccess()
    }

    LaunchedEffect(Unit) {
        while (true) {
            RazorpayHolder.result?.let {
                RazorpayHolder.result = null
                if (it.paymentId.isBlank() || it.signature.isBlank()) {
                    viewModel.clearError()
                    viewModel.setPaymentCancelled()
                    return@let
                }
                viewModel.verifyPayment(it.orderId, it.paymentId, it.signature)
            }
            kotlinx.coroutines.delay(500)
        }
    }

    var enteredFlow by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.currentStep) {
        if (uiState.currentStep in PROCESSING_STEPS) enteredFlow = true
        if (uiState.currentStep == OrderStep.SELECT_OPTIONS && uiState.error == null) enteredFlow = false
    }

    val showProcessing = uiState.currentStep in PROCESSING_STEPS ||
            (enteredFlow && uiState.error != null)

    if (uiState.currentStep == OrderStep.LOADING_OPTIONS) {
        OrderLoadingScreen("Loading print options…")
        return
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        AnimatedContent(
            targetState = showProcessing,
            transitionSpec = {
                if (targetState) {
                    (slideInVertically(tween(350, easing = FastOutSlowInEasing)) { it } +
                            fadeIn(tween(250))) togetherWith
                            (slideOutVertically(tween(250)) { -it / 6 } + fadeOut(tween(200)))
                } else {
                    (slideInVertically(tween(300)) { -it / 6 } + fadeIn(tween(250))) togetherWith
                            (slideOutVertically(tween(350, easing = FastOutSlowInEasing)) { it } +
                                    fadeOut(tween(200)))
                }
            },
            label = "order_screen_swap"
        ) { processing ->

            if (processing) {
                OrderProcessingView(
                    step           = uiState.currentStep,
                    uploadProgress = uiState.uploadProgress,
                    error          = uiState.error,
                    documentPrice  = uiState.documentPrice,   // ✅ exists
                    platformFee    = uiState.platformFee,     // ✅ exists
                    handlingFee    = uiState.handlingFee,     // ✅ already existed
                    total          = uiState.totalAmount,     // ✅ exists
                    onRetry  = { viewModel.clearError(); enteredFlow = false },
                    onCancel = { viewModel.setPaymentCancelled(); viewModel.clearError(); enteredFlow = false }
                )
            } else {
                SelectOptionsContent(
                    uiState = uiState,
                    onFileSelect = {
                        filePickerLauncher.launch(arrayOf(
                            "application/pdf", "image/png", "image/jpeg", "image/jpg"
                        ))
                    },
                    onPaperTypeSelect = viewModel::setPaperType,
                    onColorModeSelect = viewModel::setColorMode,
                    onFinishTypeSelect = viewModel::setFinishType,
                    onCopiesChange = viewModel::setCopies,
                    onOrientationChange = viewModel::setOrientation,
                    onDescriptionChange = viewModel::setDescription,
                    onPickupAtChange = viewModel::setPickupAt,
                    onCvModeToggle = viewModel::toggleCvMode,
                    onSubmit = {
                        if (activity == null) return@SelectOptionsContent
                        viewModel.submitOrder { razorpayOrderId, amount ->
                            startRazorpayPayment(
                                activity = activity,
                                razorpayOrderId = razorpayOrderId,
                                amount = amount,
                                onSuccess = { paymentId, signature ->
                                    viewModel.verifyPayment(razorpayOrderId, paymentId, signature)
                                },
                                onError = { errorMsg -> Log.e("RAZORPAY", "Payment Error: $errorMsg") }
                            )
                        }
                    }
                )
            }
        }
    }

    if (!showProcessing) {
        uiState.error?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                containerColor = Cream,
                titleContentColor = AlmostBlack,
                textContentColor = MediumGray,
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = CoralRed) },
                title = {
                    Text(
                        "Something went wrong",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                text = { Text(errorMessage, fontSize = 15.sp) },
                confirmButton = {
                    TextButton(
                        onClick = { viewModel.clearError() },
                        colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                    ) {
                        Text("OK", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────── */
/*  PROCESSING VIEW                                                            */
/* ─────────────────────────────────────────────────────────────────────────── */

private data class OrderFlowStep(val step: OrderStep, val label: String, val sub: String)

private val ORDER_FLOW_STEPS = listOf(
    OrderFlowStep(OrderStep.CREATING_ORDER,     "Creating order",     "Setting up your order"),
    OrderFlowStep(OrderStep.UPLOADING,          "Uploading document", "Sending file to server"),
    OrderFlowStep(OrderStep.ATTACHING_DOCUMENT, "Attaching document", "Linking file to order"),
    OrderFlowStep(OrderStep.PROCESSING_PAYMENT, "Preparing payment",  "Initialising Razorpay"),
)

@Composable
private fun OrderProcessingView(
    step: OrderStep,
    uploadProgress: Int,
    error: String?,
    documentPrice: Int,
    platformFee: Int,
    handlingFee: Int = 0,
    total: Int,
    // ─────────────────────────────────
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
                isFailed -> error ?: "Please try again"
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
                                if (i < currentIndex) DarkBlue.copy(alpha = 0.4f)
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
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
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
                        OrderStepState.Done    -> DarkBlue
                        OrderStepState.Active  -> DarkBlue.copy(alpha = 0.15f)
                        OrderStepState.Failed  -> Color(0xFFFFEDED)
                        OrderStepState.Pending -> AlmostBlack.copy(alpha = 0.08f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                OrderStepState.Done    -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                OrderStepState.Active  -> CircularProgressIndicator(color = DarkBlue, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
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
                    color = DarkBlue,
                    trackColor = DarkBlue.copy(alpha = 0.15f)
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
                color = DarkBlue,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkBlue.copy(alpha = 0.1f))
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
        // Header
        Text(
            text = "Bill Summary",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MediumGray,
            letterSpacing = 0.5.sp
        )

        Spacer(Modifier.height(2.dp))

        // Documents price
        BillRow(label = "Documents", amount = documentPrice)

        // Platform fee
        BillRow(label = "Platform Fee", amount = platformFee)

        // Handling fee — only rendered when applicable
        if (handlingFee > 0) {
            BillRow(
                label = "Handling Fee",
                amount = handlingFee,
                labelColor = CoralRed.copy(alpha = 0.85f),
                amountColor = CoralRed
            )
        }

        // Divider
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 2.dp),
            thickness = 1.dp,
            color = AlmostBlack.copy(alpha = 0.10f)
        )

        // Total
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
                color = DarkBlue
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
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            color = labelColor
        )
        Text(
            text = "₹$amount",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = amountColor
        )
    }
}

/* ─────────────────────────────────────────────────────────────────────────── */
/*  SELECT OPTIONS                                                             */
/* ─────────────────────────────────────────────────────────────────────────── */

@Composable
fun SelectOptionsContent(
    uiState: CreateOrderUiState,
    onFileSelect: () -> Unit,
    onPaperTypeSelect: (PaperType) -> Unit,
    onColorModeSelect: (ColorMode) -> Unit,
    onFinishTypeSelect: (FinishType) -> Unit,
    onCopiesChange: (Int) -> Unit,
    onOrientationChange: (PrintOrientation) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPickupAtChange: (String) -> Unit,
    onCvModeToggle: () -> Unit,
    onSubmit: () -> Unit
) {
    var showInstructions by remember { mutableStateOf(false) }

    val filteredColorModes = remember(uiState.printOptions) {
        (uiState.printOptions?.colorModes ?: emptyList())
            .filter { it.name.lowercase() != "bond" }
    }

    val filteredPaperTypes = remember(uiState.printOptions, uiState.isCvMode) {
        val all = uiState.printOptions?.paperTypes ?: emptyList()
        if (uiState.isCvMode) all.filter { it.name.lowercase() == "bond" } else all
    }

    val filteredFinishTypes = remember(uiState.printOptions) {
        (uiState.printOptions?.finishTypes ?: emptyList())
            .filter { it.name.lowercase() != "bond" }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(),
        color = Cream
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Create Print Order",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = AlmostBlack
                )
                InfoIconButton { showInstructions = true }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (uiState.selectedFile != null) Color.Transparent
                                else AlmostBlack.copy(alpha = 0.04f)
                            )
                            .border(
                                if (uiState.selectedFile != null) 2.dp else 1.5.dp,
                                if (uiState.selectedFile != null) DarkBlue.copy(alpha = 0.7f)
                                else AlmostBlack.copy(alpha = 0.3f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { onFileSelect() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.selectedFile != null) {
                            FilePreview(
                                file = uiState.selectedFile,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AlmostBlack.copy(alpha = 0.6f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${uiState.pageCount} pages",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(10.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AlmostBlack.copy(alpha = 0.6f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Tap to change",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DarkBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add, null,
                                        tint = DarkBlue,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Tap to upload document",
                                    color = AlmostBlack,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Max 500MB", color = MediumGray, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Accepted: PDF, PNG, JPG (Max 500MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)) {
                Column {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Number of Copies: ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AlmostBlack
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { if (uiState.copies > 1) onCopiesChange(uiState.copies - 1) },
                                shape = RoundedCornerShape(8.dp),
                                color = DarkBlue
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("−", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Text(
                                uiState.copies.toString(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = AlmostBlack,
                                modifier = Modifier.width(40.dp),
                                textAlign = TextAlign.Center
                            )
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { onCopiesChange(uiState.copies + 1) },
                                shape = RoundedCornerShape(8.dp),
                                color = DarkBlue
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    CvModeToggle(
                        isEnabled = uiState.isCvMode,
                        onToggle = onCvModeToggle
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Color Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isCvMode) AlmostBlack.copy(alpha = 0.4f) else AlmostBlack
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        filteredColorModes.forEach { colorMode ->
                            val isSelected = !uiState.isCvMode && uiState.selectedColorMode == colorMode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(140.dp)
                                    .alpha(if (uiState.isCvMode) 0.4f else 1f)
                                    .background(
                                        if (isSelected) SoftBlue.copy(alpha = 0.1f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        if (isSelected) 2.dp else 1.5.dp,
                                        if (isSelected) DarkBlue else AlmostBlack.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable(enabled = !uiState.isCvMode) { onColorModeSelect(colorMode) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        if (isSelected) Icon(
                                            Icons.Default.Check, "Selected",
                                            tint = Blue,
                                            modifier = Modifier.size(22.dp)
                                        ) else Spacer(Modifier.size(22.dp))
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Column(
                                        modifier = Modifier.height(44.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            colorMode.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) DarkBlue else AlmostBlack,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "₹${colorMode.extraPrice}/page",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) Color(0xFF2E7D32) else MediumGray
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        when {
                                            colorMode.name.lowercase().contains("color") -> GradientDot()
                                            colorMode.name.lowercase().contains("cv") -> SolidDot(Color(0xFFF3ECDC))
                                            else -> {
                                                SolidDot(AlmostBlack)
                                                Spacer(Modifier.width(4.dp))
                                                SolidDot(Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    DropdownSection(
                        label = "Paper Type",
                        placeholder = "Select paper size",
                        selected = uiState.selectedPaperType?.name ?: "",
                        items = filteredPaperTypes,
                        itemText = { "${it.name} - ₹${it.basePrice}" },
                        enabled = !uiState.isCvMode,
                        onSelect = onPaperTypeSelect
                    )

                    Spacer(Modifier.height(24.dp))

                    DropdownSection(
                        label = "Finish Type",
                        placeholder = "Select finish type",
                        selected = uiState.selectedFinishType?.name ?: "",
                        items = filteredFinishTypes,
                        itemText = { "${it.name} - ₹${it.extraPrice}" },
                        enabled = !uiState.isCvMode,
                        onSelect = onFinishTypeSelect
                    )

                    Spacer(Modifier.height(24.dp))

                    Text(
                        "Orientation",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AlmostBlack
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PrintOrientation.values().forEach { orientation ->
                            val isSelected = uiState.orientation == orientation
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 80.dp)
                                    .background(
                                        if (isSelected) OffWhite.copy(alpha = 0.3f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        if (isSelected) 2.dp else 1.5.dp,
                                        if (isSelected) DarkBlue else AlmostBlack.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onOrientationChange(orientation) }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    orientation.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) DarkBlue else AlmostBlack
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    uiState.shop?.let { shop ->
                        PickupDateTimeSection(
                            value = uiState.pickupAt,
                            openTime = shop.openTime,
                            closeTime = shop.closeTime,
                            onValueChange = onPickupAtChange
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    if (isTomorrowPickup(uiState.pickupAt)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(OffWhite.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .border(2.dp, CoralRed, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Default.Warning, null, tint = CoralRed)
                                    Column {
                                        Text(
                                            "Handling Fee Applied",
                                            fontWeight = FontWeight.Bold,
                                            color = AlmostBlack,
                                            fontSize = 16.sp
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            "₹10 added for next-day pickup",
                                            color = CoralRed,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    DescriptionSection(value = uiState.description, onValueChange = onDescriptionChange)
                }
            }

            Spacer(modifier = Modifier.height(130.dp).navigationBarsPadding())
        }

        FloatingPayBar(uiState = uiState, onSubmit = onSubmit)

        if (showInstructions) {
            AlertDialog(
                onDismissRequest = { showInstructions = false },
                containerColor = Cream,
                titleContentColor = AlmostBlack,
                textContentColor = MediumGray,
                icon = { Icon(Icons.Outlined.Lock, null, tint = DarkBlue) },
                title = { Text("Secure Payment", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        InstructionItem("Payments are processed securely via Razorpay.")

                        InstructionItem("Final amount includes convenience fee.")

                        InstructionItem("You will receive instant confirmation after payment.")

                        InstructionItem("If payment fails, amount will be refunded automatically.")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showInstructions = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = LimeGreen)
                    ) {
                        Text("Continue")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────── */
/*  CV MODE TOGGLE                                                             */
/* ─────────────────────────────────────────────────────────────────────────── */

@Composable
private fun CvModeToggle(
    isEnabled: Boolean,
    onToggle: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isEnabled) DarkBlue else Color.Transparent,
        animationSpec = tween(250),
        label = "cv_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isEnabled) DarkBlue else AlmostBlack.copy(alpha = 0.6f),
        animationSpec = tween(250),
        label = "cv_border"
    )
    val labelColor by animateColorAsState(
        targetValue = if (isEnabled) Color.White else AlmostBlack,
        animationSpec = tween(250),
        label = "cv_label"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isEnabled) 0.dp else 1.5.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "CV Mode",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = labelColor
            )
            AnimatedVisibility(visible = isEnabled) {
                Text(
                    text = "Bond paper auto-selected",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        AnimatedContent(
            targetState = isEnabled,
            transitionSpec = {
                (scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()) togetherWith
                        (scaleOut() + fadeOut())
            },
            label = "cv_icon"
        ) { enabled ->
            if (enabled) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "CV Mode On",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkBlue.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 1.dp)
                ) {
                    Text(
                        "CV",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkBlue
                    )
                }
            }
        }
    }
}

/* ─────────────────────────────────────────────────────────────────────────── */
/*  HELPERS                                                                    */
/* ─────────────────────────────────────────────────────────────────────────── */

@Composable
private fun OrderLoadingScreen(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = GoldenYellow, strokeWidth = 3.dp)
            Spacer(Modifier.height(14.dp))
            Text(text, color = MediumGray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSection(
    label: String,
    placeholder: String,
    selected: String,
    items: List<T>,
    itemText: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val isSelected = selected.isNotEmpty()

    LaunchedEffect(enabled) { if (!enabled) expanded = false }

    Column {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (enabled) AlmostBlack else AlmostBlack.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(12.dp))
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            Box(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        if (isSelected) OffWhite.copy(alpha = 0.3f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        if (isSelected) 2.dp else 1.5.dp,
                        if (isSelected) DarkBlue else AlmostBlack.copy(alpha = if (enabled) 0.6f else 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = enabled) { expanded = true }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (selected.isEmpty()) placeholder else selected,
                        color = when {
                            !enabled && isSelected -> AlmostBlack.copy(alpha = 0.4f)
                            isSelected             -> AlmostBlack
                            else                   -> MediumGray
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(
                        if (enabled) Icons.Default.ArrowDropDown else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (enabled) {
                            if (isSelected) AlmostBlack else MediumGray
                        } else {
                            DarkBlue.copy(alpha = 0.4f)
                        }
                    )
                }
            }
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(OffWhite)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemText(item), color = AlmostBlack) },
                        onClick = { onSelect(item); expanded = false }
                    )
                }
            }
        }
    }
}

fun startRazorpayPayment(
    activity: Activity,
    razorpayOrderId: String,
    amount: Int,
    onSuccess: (paymentId: String, orderId: String) -> Unit,
    onError: (String) -> Unit
) {
    try {
        val checkout = Checkout()
        checkout.setKeyID(BuildConfig.RAZORPAY_KEY_ID)
        val options = JSONObject().apply {
            put("name", "Docuvio")
            put("description", "Print Order Payment")
            put("order_id", razorpayOrderId)
            put("currency", "INR")
            put("amount", amount)
            put("theme.color", "#FFBF5E7")
        }
        checkout.open(activity, options)
    } catch (e: Exception) {
        Log.e("RAZORPAY", "Error opening Razorpay", e)
        onError(e.message ?: "Payment initialization failed")
    }
}

private fun getFileName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst()) return it.getString(nameIndex)
    }
    return "file_${System.currentTimeMillis()}"
}

@Composable
fun DescriptionSection(value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(
            "Instruction",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AlmostBlack
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent, RoundedCornerShape(12.dp))
                .border(1.5.dp, AlmostBlack.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                minLines = 3,
                maxLines = 5,
                textStyle = LocalTextStyle.current.copy(color = AlmostBlack, fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth()
            ) { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        "Any special instructions? (optional)",
                        color = MediumGray.copy(alpha = 0.5f)
                    )
                }
                innerTextField()
            }
        }
    }
}

@Composable
fun InfoIconButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(32.dp)) {
        Icon(
            Icons.Outlined.Info, "Instructions",
            tint = CoralRed,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun InstructionItem(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text("•", color = AlmostBlack, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        Text(text, color = AlmostBlack, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SolidDot(color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(color)
            .border(1.dp, AlmostBlack.copy(alpha = 0.3f), RoundedCornerShape(9.dp))
    )
}

@Composable
fun GradientDot() {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
                        Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA)
                    )
                )
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupDateTimeSection(
    value: String?,
    openTime: String,
    closeTime: String,
    onValueChange: (String) -> Unit
) {
    var selectedDateMillis by remember { mutableStateOf<Long?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    val openMinutes = openTime.toMinutes()
    val closeMinutes = closeTime.toMinutes()
    val lastOrderMinutes = closeMinutes - 30
    val nowMinutes = remember {
        val now = Calendar.getInstance()
        now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    }
    val showTomorrowButton = nowMinutes >= lastOrderMinutes

    Column {
        Text(
            "Pickup Date & Time",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AlmostBlack
        )
        Spacer(Modifier.height(12.dp))
        val today = remember {
            Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        val tomorrow = remember {
            Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val todayEnabled = nowMinutes < lastOrderMinutes
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            selectedDateMillis == today && value != null -> DarkBlue
                            !todayEnabled -> MediumGray.copy(alpha = 0.15f)
                            else -> OffWhite
                        }
                    )
                    .border(
                        if (selectedDateMillis == today && value != null) 0.dp else 1.5.dp,
                        if (!todayEnabled) MediumGray.copy(alpha = 0.3f) else AlmostBlack.copy(alpha = 0.5f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = todayEnabled) {
                        selectedDateMillis = today; showTimePicker = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Today",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = when {
                            selectedDateMillis == today && value != null -> Color.White
                            !todayEnabled -> MediumGray.copy(alpha = 0.4f)
                            else -> AlmostBlack
                        }
                    )
                    if (!todayEnabled) {
                        Text(
                            "Closed",
                            fontSize = 10.sp,
                            color = MediumGray.copy(alpha = 0.4f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            if (showTomorrowButton) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedDateMillis == tomorrow && value != null) DarkBlue else OffWhite
                        )
                        .border(
                            if (selectedDateMillis == tomorrow && value != null) 0.dp else 1.5.dp,
                            AlmostBlack.copy(alpha = 0.5f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedDateMillis = tomorrow; showTimePicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Tomorrow",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (selectedDateMillis == tomorrow && value != null) Color.White else AlmostBlack
                        )
                        if (selectedDateMillis != tomorrow || value == null) {
                            Text(
                                "+₹10 handling",
                                fontSize = 10.sp,
                                color = CoralRed.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (value != null) OffWhite.copy(alpha = 0.3f) else Color.Transparent,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    if (value != null) 2.dp else 1.5.dp,
                    if (value != null) DarkBlue else AlmostBlack.copy(alpha = 0.6f),
                    RoundedCornerShape(12.dp)
                )
                .clickable(enabled = selectedDateMillis != null) { showTimePicker = true }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value?.let { formatPickupDateTime(it) }
                        ?: "Select date above, then choose time",
                    color = if (value != null) AlmostBlack else MediumGray.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (value != null) FontWeight.SemiBold else FontWeight.Normal
                )
                Icon(
                    Icons.Default.CalendarToday, "Select time",
                    tint = if (value != null) SoftBlue else MediumGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Pickup available between ${formatTime(openTime)} and ${formatTime(closeTime)}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MediumGray,
            maxLines = 1
        )
    }

    if (showTimePicker && selectedDateMillis != null) {
        val timePickerState = rememberTimePickerState(is24Hour = false)
        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val earliestAllowedMinutes = currentMinutes + 30
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val isToday = selectedDateMillis == todayStart
        val isValidTime = run {
            val selected = timePickerState.hour * 60 + timePickerState.minute
            if (isToday) selected in openMinutes until lastOrderMinutes && selected >= earliestAllowedMinutes
            else selected in openMinutes until closeMinutes
        }
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isValidTime) return@TextButton
                        val calendar = Calendar.getInstance().apply {
                            timeInMillis = selectedDateMillis!!
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onValueChange(
                            SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                                Locale.getDefault()
                            ).format(calendar.time)
                        )
                        showTimePicker = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (isValidTime) DarkBlue else SoftBlue
                    )
                ) {
                    Text("Confirm", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = MediumGray)
                }
            },
            title = {
                Text("Select Pickup Time", fontWeight = FontWeight.Bold, color = AlmostBlack)
            },
            text = {
                Column {
                    Text(
                        if (isToday) "Min. 30 min from now · Available until ${formatTime(closeTime)}"
                        else "Available between ${formatTime(openTime)} and ${formatTime(closeTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumGray
                    )
                    Spacer(Modifier.height(16.dp))
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = OffWhite,
                            selectorColor = SoftBlue,
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = AlmostBlack
                        )
                    )
                }
            },
            containerColor = Cream,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

private fun String.toMinutes(): Int = try {
    val p = split(":"); p[0].toInt() * 60 + p[1].toInt()
} catch (e: Exception) { 0 }