package com.docuvio.app.ui.order

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuvio.app.data.model.RazorpayHolder
import com.docuvio.app.theme.*
import com.docuvio.app.viewmodel.WalkInOrderStage
import com.docuvio.app.viewmodel.WalkInOrderUiState
import com.docuvio.app.viewmodel.WalkInOrderViewModel
import com.docuvio.app.viewmodel.WalkInOrderViewModelFactory
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.text.input.KeyboardType
import com.docuvio.app.ui.order.utils.WalkInFloatingPayBar
import androidx.compose.material.icons.filled.Warning
import kotlinx.coroutines.launch

// ── Stage metadata ────────────────────────────────────────────────────────────

private data class StepMeta(val stage: WalkInOrderStage, val label: String, val sub: String)

private val STEPS = listOf(
    StepMeta(WalkInOrderStage.CreatingOrder,     "Creating order",     "Setting up your order"),
    StepMeta(WalkInOrderStage.UploadingFile,     "Uploading document", "Sending file to server"),
    StepMeta(WalkInOrderStage.AttachingDocument, "Attaching document", "Linking file to order"),
    StepMeta(WalkInOrderStage.CreatingPayment,   "Preparing payment",  "Initialising Razorpay"),
)

private fun stageIndex(stage: WalkInOrderStage) = STEPS.indexOfFirst { it.stage == stage }

// ── Root ──────────────────────────────────────────────────────────────────────

@Composable
fun WalkInOrderScreen(
    viewModelFactory: WalkInOrderViewModelFactory,
    onSuccess: () -> Unit
) {
    val viewModel: WalkInOrderViewModel = viewModel(factory = viewModelFactory)
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

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: run {
            Log.d("FilePicker", "No URI returned — user cancelled")
            return@rememberLauncherForActivityResult
        }

        val rawMime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        Log.d("FilePicker", "URI: $uri")
        Log.d("FilePicker", "Raw MIME from ContentResolver: $rawMime")

        val mimeType = when (rawMime.lowercase().trim()) {
            "image/jpg", "image/jpeg" -> "image/jpeg"
            else -> rawMime
        }
        Log.d("FilePicker", "Normalised MIME: $mimeType")

        val extension = when {
            mimeType.contains("pdf")            -> "pdf"
            mimeType.contains("png")            -> "png"
            mimeType.contains("jpeg")           -> "jpg"
            mimeType.contains("word") ||
                    mimeType.contains("officedocument") -> "docx"
            else                                -> "bin"
        }
        Log.d("FilePicker", "Resolved extension: $extension")

        val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
        Log.d("FilePicker", "Cache file path: ${file.absolutePath}")

        try {
            context.contentResolver.openInputStream(uri)?.use { inp ->
                FileOutputStream(file).use { out ->
                    val bytesCopied = inp.copyTo(out)
                    out.flush()
                    Log.d("FilePicker", "Bytes copied to cache: $bytesCopied")
                }
            } ?: run {
                Log.e("FilePicker", "openInputStream returned null for URI: $uri")
                viewModel.clearError()
                return@rememberLauncherForActivityResult
            }
        } catch (e: Exception) {
            Log.e("FilePicker", "Failed to copy file to cache", e)
            file.delete()
            return@rememberLauncherForActivityResult
        }

        // Pre-upload size check
        if (file.length() > 500 * 1024 * 1024) {
            file.delete()
            viewModel.setPaymentCancelled() // Reuse or set specific error
            // Better to have a generic setError but WalkInOrderViewModel is simpler
            return@rememberLauncherForActivityResult
        }

        Log.d("FilePicker", "File exists after copy: ${file.exists()}, size: ${file.length()} bytes")
        Log.d("FilePicker", "Calling viewModel.setFile with mimeType: $mimeType")
        viewModel.setFile(file, mimeType)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) { viewModel.resetState(); onSuccess() }
    }

    // Fixed: Using SharedFlow from RazorpayHolder instead of polling loop
    LaunchedEffect(Unit) {
        RazorpayHolder.resultFlow.collect { result ->
            if (result.cancelled || !result.errorMessage.isNullOrBlank()) {
                viewModel.setPaymentCancelled()
            } else {
                viewModel.verifyPayment(result.orderId, result.paymentId, result.signature)
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        AnimatedContent(
            targetState = uiState.stage != WalkInOrderStage.Idle,
            transitionSpec = {
                if (targetState)
                    slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it / 3 } + fadeOut()
                else
                    slideInVertically { -it / 3 } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
            },
            label = "screen_swap"
        ) { showProcessing ->
            if (showProcessing) {
                ProcessingView(
                    stage          = uiState.stage,
                    uploadProgress = uiState.uploadProgress,
                    error          = uiState.error,
                    onRetry        = { viewModel.clearError() },
                    onCancel       = { viewModel.cancelOrder() }
                )
            } else {
                FormView(
                    uiState    = uiState,
                    viewModel  = viewModel,
                    activity   = activity,
                    onPickFile = { filePicker.launch(arrayOf(
                        "*/*"
                    )) }
                )
            }
        }
    }
}

// ── Form ──────────────────────────────────────────────────────────────────────

@Composable
private fun FormView(
    uiState: WalkInOrderUiState,
    viewModel: WalkInOrderViewModel,
    activity: Activity?,
    onPickFile: () -> Unit
) {
    val scrollState = rememberScrollState()
    val scope       = rememberCoroutineScope()
    val canSubmit   = uiState.selectedFile != null && uiState.amount.isNotBlank()
    val baseAmount = uiState.amount.toIntOrNull() ?: 0

    val convenienceFee = if (baseAmount < 100) 1 else 2

    val finalTotal = baseAmount + convenienceFee

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxWidth()      // ← was fillMaxSize()
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(20.dp))

            Text(
                "Walk-In Order",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = AlmostBlack
            )
            Text(
                "Upload document (PDF, PNG, JPG)",
                style = MaterialTheme.typography.bodyMedium,
                color = MediumGray,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(Modifier.height(28.dp))
            SectionLabel("Document")
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            uiState.isConverting -> DarkBlue.copy(alpha = 0.04f)
                            uiState.selectedFile != null -> Color.Transparent
                            else -> AlmostBlack.copy(alpha = 0.04f)
                        }
                    )
                    .border(
                        width = if (uiState.selectedFile != null) 2.dp else 1.5.dp,
                        color = when {
                            uiState.isConverting -> DarkBlue.copy(alpha = 0.4f)
                            uiState.selectedFile != null -> DarkBlue.copy(alpha = 0.7f)
                            uiState.conversionError != null -> CoralRed.copy(alpha = 0.7f)
                            else -> AlmostBlack.copy(alpha = 0.3f)
                        },
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = !uiState.isConverting) { onPickFile() },
                contentAlignment = Alignment.Center
            ) {
                when {
                    uiState.isConverting -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = DarkBlue,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Converting DOCX → PDF…",
                                color = DarkBlue,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "This may take a few seconds",
                                color = MediumGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    uiState.selectedFile != null -> {
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
                                "${uiState.pageCount} pages",
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
                    }

                    uiState.conversionError != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Conversion failed",
                                color = CoralRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Tap to try another file",
                                color = MediumGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    else -> {
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
                            Text("PDF, PNG, JPG, DOCX · Max 500MB", color = MediumGray, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            AnimatedVisibility(
                visible = uiState.amount.isNotBlank() && baseAmount > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBlue.copy(alpha = 0.04f))
                        .border(1.dp, DarkBlue.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Print cost", fontSize = 14.sp, color = MediumGray)
                        Text("₹ $baseAmount", fontSize = 14.sp, color = AlmostBlack, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Convenience fee", fontSize = 14.sp, color = MediumGray)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (baseAmount < 100) "under ₹100" else "₹100+",
                                fontSize = 10.sp,
                                color = DarkBlue.copy(alpha = 0.6f),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(DarkBlue.copy(alpha = 0.08f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("+ ₹ $convenienceFee", fontSize = 14.sp, color = AlmostBlack, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = DarkBlue.copy(alpha = 0.1f), thickness = 1.dp)
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AlmostBlack)
                        Text("₹ $finalTotal", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DarkBlue)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            SectionLabel("Enter Price")
            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (uiState.amount.isNotBlank()) DarkBlue.copy(alpha = 0.05f) else Color.Transparent)
                    .border(
                        width = if (uiState.amount.isNotBlank()) 2.dp else 1.5.dp,
                        color = if (uiState.amount.isNotBlank()) DarkBlue.copy(alpha = 0.7f) else AlmostBlack.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "₹", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                        color = if (uiState.amount.isNotBlank()) DarkBlue else MediumGray,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    BasicTextField(
                        value = uiState.amount,
                        onValueChange = { if (it.all { c -> c.isDigit() }) viewModel.setAmount(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AlmostBlack),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { f ->
                            if (f.isFocused) scope.launch {
                                kotlinx.coroutines.delay(300)
                                scrollState.animateScrollTo(scrollState.maxValue) }
                        }
                    ) { inner ->
                        if (uiState.amount.isEmpty()) Text("Enter amount", color = MediumGray, fontSize = 16.sp)
                        inner()
                    }
                }
            }
            Spacer(Modifier.height(200.dp))
        }
        WalkInFloatingPayBar(
            total = finalTotal,
            pageCount = uiState.pageCount,
            isEnabled = canSubmit,
            onSubmit = {
                if (activity == null) return@WalkInFloatingPayBar
                viewModel.submitOrder { razorpayOrderId, amount ->
                    startRazorpayPayment(
                        activity,
                        razorpayOrderId,
                        amount
                    ) { Log.e("RAZORPAY", it) }
                }
            }
        )
    }
}

// ── Processing ────────────────────────────────────────────────────────────────

@Composable
private fun ProcessingView(
    stage: WalkInOrderStage,
    uploadProgress: Int,
    error: String?,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    val currentIndex = stageIndex(stage)
    val isFailed     = error != null

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(72.dp))

        // Icon
        AnimatedContent(
            targetState = isFailed,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "top_icon"
        ) { failed ->
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape)
                    .background(if (failed) Color(0xFFFFEDED) else DarkBlue.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (failed) Icon(Icons.Default.Close, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(36.dp))
                else CircularProgressIndicator(color = DarkBlue, strokeWidth = 3.dp, modifier = Modifier.size(36.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        // Headline
        AnimatedContent(
            targetState = if (isFailed) "Something went wrong" else STEPS.getOrNull(currentIndex)?.label ?: "Processing…",
            transitionSpec = { slideInVertically { it / 2 } + fadeIn() togetherWith slideOutVertically { -it / 2 } + fadeOut() },
            label = "headline"
        ) { text ->
            Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = if (isFailed) Color(0xFFD32F2F) else AlmostBlack, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(6.dp))

        AnimatedContent(
            targetState = when {
                isFailed -> error
                stage == WalkInOrderStage.UploadingFile -> "Uploading… $uploadProgress%"
                else -> STEPS.getOrNull(currentIndex)?.sub ?: ""
            },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "sublabel"
        ) { sub ->
            Text(sub, fontSize = 14.sp, color = MediumGray, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(52.dp))

        // Steps
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(AlmostBlack.copy(alpha = 0.04f))
                .padding(vertical = 8.dp)
        ) {
            STEPS.forEachIndexed { index, step ->
                val state = when {
                    index < currentIndex               -> PStepState.Done
                    index == currentIndex && !isFailed -> PStepState.Active
                    index == currentIndex && isFailed  -> PStepState.Failed
                    else                               -> PStepState.Pending
                }
                PStepRow(
                    label    = step.label,
                    state    = state,
                    progress = if (step.stage == WalkInOrderStage.UploadingFile && state == PStepState.Active)
                        uploadProgress / 100f else null
                )
                if (index < STEPS.lastIndex) {
                    Box(
                        modifier = Modifier.padding(start = 34.dp).width(2.dp).height(16.dp)
                            .background(if (index < currentIndex) DarkBlue.copy(alpha = 0.4f) else AlmostBlack.copy(alpha = 0.12f))
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        if (isFailed) {
            Button(
                onClick = onRetry,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBlue)
            ) {
                Text("Try Again", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(12.dp))
        }

        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel", color = if (isFailed) MediumGray else Color(0xFFD32F2F),
                fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Step row ──────────────────────────────────────────────────────────────────

private enum class PStepState { Pending, Active, Done, Failed }

@Composable
private fun PStepRow(label: String, state: PStepState, progress: Float?) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.85f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_scale"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .scale(if (state == PStepState.Active) pulse else 1f)
                .clip(CircleShape)
                .background(when (state) {
                    PStepState.Done    -> DarkBlue
                    PStepState.Active  -> DarkBlue.copy(alpha = 0.15f)
                    PStepState.Failed  -> Color(0xFFFFEDED)
                    PStepState.Pending -> AlmostBlack.copy(alpha = 0.08f)
                }),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                PStepState.Done    -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                PStepState.Active  -> CircularProgressIndicator(color = DarkBlue, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                PStepState.Failed  -> Icon(Icons.Default.Close, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(14.dp))
                PStepState.Pending -> Unit
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                label, fontSize = 15.sp,
                fontWeight = if (state == PStepState.Active) FontWeight.SemiBold else FontWeight.Normal,
                color = when (state) {
                    PStepState.Done, PStepState.Active -> AlmostBlack
                    PStepState.Failed  -> Color(0xFFD32F2F)
                    PStepState.Pending -> MediumGray
                }
            )
            if (progress != null) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                    color = DarkBlue, trackColor = DarkBlue.copy(alpha = 0.15f)
                )
            }
        }

        AnimatedVisibility(
            visible = state == PStepState.Done,
            enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)),
            exit  = scaleOut()
        ) {
            Text(
                "Done", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = DarkBlue,
                modifier = Modifier.clip(RoundedCornerShape(6.dp))
                    .background(DarkBlue.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AlmostBlack)
}
