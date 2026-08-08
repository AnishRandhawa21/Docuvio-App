package com.docuvio.app.ui.order.schedulecomponents

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuvio.app.data.model.*
import com.docuvio.app.viewmodel.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import com.docuvio.app.theme.*

private val PROCESSING_STEPS = setOf(
    OrderStep.CREATING_ORDER,
    OrderStep.UPLOADING,
    OrderStep.ATTACHING_DOCUMENT,
    OrderStep.PROCESSING_PAYMENT,
)

@Composable
fun CreateOrderScreen(
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

    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            val rawMime = context.contentResolver.getType(uri) ?: "application/octet-stream"

            val mimeType = when (rawMime.lowercase().trim()) {
                "image/jpg", "image/JPEG" -> "image/jpeg"
                else -> rawMime
            }

            val extension = when {
                mimeType.contains("pdf")  -> "pdf"
                mimeType.contains("png")  -> "png"
                mimeType.contains("jpeg") -> "jpg"
                else -> "bin"
            }

            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.$extension")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                } ?: run {
                    viewModel.setError("Could not read the selected file. Please try another.")
                    return@rememberLauncherForActivityResult
                }
            } catch (e: IOException) {
                file.delete()
                Log.e("FilePicker", "Copy failed for $uri", e)
                viewModel.setError("Failed to copy file. Please try again.")
                return@rememberLauncherForActivityResult
            }

            if (file.length() > 500 * 1024 * 1024) {
                file.delete()
                viewModel.setError("File is too large (max 500MB).")
                return@rememberLauncherForActivityResult
            }

            viewModel.setFileAndReadPages(file, mimeType)
        }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onOrderSuccess()
    }

    LaunchedEffect(Unit) {
        RazorpayHolder.resultFlow.collect { result ->
            if (result.cancelled || !result.errorMessage.isNullOrBlank()) {
                viewModel.clearError()
                viewModel.setPaymentCancelled()
                if (!result.errorMessage.isNullOrBlank()) {
                    viewModel.setError(result.errorMessage)
                }
            } else {
                viewModel.verifyPayment(result.orderId, result.paymentId, result.signature)
            }
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
                    documentPrice  = uiState.documentPrice,
                    platformFee    = uiState.platformFee,
                    handlingFee    = uiState.handlingFee,
                    total          = uiState.totalAmount,
                    onRetry  = { viewModel.clearError(); },
                    onCancel = { viewModel.setPaymentCancelled(); viewModel.clearError(); }
                )
            } else {
                SelectOptionsContent(
                    uiState = uiState,
                    onFileSelect = {
                        filePickerLauncher.launch(arrayOf(
                            "*/*"
                        ))
                    },
                    onPaperTypeSelect = viewModel::setPaperType,
                    onColorModeSelect = viewModel::setColorMode,
                    onFinishTypeSelect = viewModel::setFinishType,
                    onCopiesChange = viewModel::setCopies,
                    onOrientationChange = viewModel::setOrientation,
                    onPrintSideChange = viewModel::setPrintSide,
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