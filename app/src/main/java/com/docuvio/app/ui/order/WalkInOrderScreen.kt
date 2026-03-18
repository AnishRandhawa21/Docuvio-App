    package com.docuvio.app.ui.order

    import android.app.Activity
    import android.content.Context
    import android.content.ContextWrapper
    import android.util.Log
    import androidx.activity.compose.rememberLauncherForActivityResult
    import androidx.activity.result.contract.ActivityResultContracts
    import androidx.compose.foundation.background
    import androidx.compose.foundation.border
    import androidx.compose.foundation.clickable
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.rememberScrollState
    import androidx.compose.foundation.verticalScroll
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.foundation.text.BasicTextField
    import androidx.compose.foundation.text.KeyboardOptions
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.Add
    import androidx.compose.material3.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.unit.sp
    import androidx.lifecycle.viewmodel.compose.viewModel
    import com.docuvio.app.data.model.RazorpayHolder
    import com.docuvio.app.theme.*
    import com.docuvio.app.utils.FileUtils
    import com.docuvio.app.viewmodel.WalkInOrderViewModel
    import com.docuvio.app.viewmodel.WalkInOrderViewModelFactory
    import java.io.File
    import java.io.FileOutputStream
    import androidx.compose.ui.text.input.KeyboardType

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

        val filePicker =
            rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri ?: return@rememberLauncherForActivityResult
                val fileName = FileUtils.getFileName(context, uri)
                val input = context.contentResolver.openInputStream(uri)
                val file = File(context.cacheDir, fileName)
                input?.use { inp -> FileOutputStream(file).use { out -> inp.copyTo(out) } }
                viewModel.setFile(file)
            }

        LaunchedEffect(uiState.isSuccess) {
            if (uiState.isSuccess) onSuccess()
        }

        LaunchedEffect(Unit) {
            while (!uiState.isSuccess) {
                RazorpayHolder.result?.let {
                    RazorpayHolder.result = null
                    if (it.paymentId.isBlank() || it.signature.isBlank()) {
                        viewModel.clearError()
                        return@let
                    }
                    viewModel.verifyPayment(
                        razorpayOrderId = it.orderId,
                        razorpayPaymentId = it.paymentId,
                        razorpaySignature = it.signature
                    )
                }
                kotlinx.coroutines.delay(200)
            }
        }

        Surface(modifier = Modifier.fillMaxSize(), color = Cream) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {

                // ── HEADER ──────────────────────────────────────────
                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Walk-In Order",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = AlmostBlack
                )

                Text(
                    text = "Upload a PDF and set price manually",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MediumGray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(28.dp))

                // ── FILE UPLOAD ──────────────────────────────────────
                SectionLabel("Document")

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (uiState.selectedFile != null)
                                Color.Transparent
                            else
                                AlmostBlack.copy(alpha = 0.04f)
                        )
                        .border(
                            width = if (uiState.selectedFile != null) 2.dp else 1.5.dp,
                            color = if (uiState.selectedFile != null)
                                DarkBlue.copy(alpha = 0.7f)
                            else
                                AlmostBlack.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = !uiState.uploadingFile) {
                            filePicker.launch("application/pdf")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        uiState.uploadingFile -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(
                                    color = DarkBlue,
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Uploading ${uiState.uploadProgress}%",
                                    color = MediumGray,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        uiState.selectedFile != null -> {
                            FilePreview(
                                file = uiState.selectedFile!!,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                            // Change file hint
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
                        else -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(DarkBlue.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = DarkBlue,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    "Tap to upload PDF",
                                    color = AlmostBlack,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Max 500MB",
                                    color = MediumGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── PRICE ────────────────────────────────────────────
                SectionLabel("Manual Price")

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (uiState.amount.isNotBlank()) DarkBlue.copy(alpha = 0.05f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (uiState.amount.isNotBlank()) 2.dp else 1.5.dp,
                            color = if (uiState.amount.isNotBlank()) DarkBlue.copy(alpha = 0.7f)
                            else AlmostBlack.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "₹",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.amount.isNotBlank()) DarkBlue else MediumGray,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        BasicTextField(
                            value = uiState.amount,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() }) viewModel.setAmount(input)
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            textStyle = LocalTextStyle.current.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AlmostBlack
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) { innerTextField ->
                            if (uiState.amount.isEmpty()) {
                                Text(
                                    "Enter amount",
                                    color = MediumGray,
                                    fontSize = 16.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ── NOTES ────────────────────────────────────────────
                SectionLabel("Notes")

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (uiState.notes.isNotBlank()) DarkBlue.copy(alpha = 0.05f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (uiState.notes.isNotBlank()) 2.dp else 1.5.dp,
                            color = if (uiState.notes.isNotBlank()) DarkBlue.copy(alpha = 0.7f)
                            else AlmostBlack.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = uiState.notes,
                        onValueChange = viewModel::setNotes,
                        minLines = 3,
                        textStyle = LocalTextStyle.current.copy(
                            color = AlmostBlack,
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.notes.isEmpty()) {
                            Text(
                                "Any special instructions? (optional)",
                                color = MediumGray,
                                fontSize = 15.sp
                            )
                        }
                        it()
                    }
                }

                Spacer(Modifier.height(32.dp))

                // ── SUBMIT BUTTON ────────────────────────────────────
                val canSubmit = uiState.selectedFile != null &&
                        uiState.amount.isNotBlank() &&
                        !uiState.uploadingFile

                Button(
                    onClick = {
                        if (activity == null) return@Button
                        viewModel.submitOrder { razorpayOrderId, amount ->
                            startRazorpayPayment(
                                activity = activity,
                                razorpayOrderId = razorpayOrderId,
                                amount = amount,
                                onSuccess = { paymentId, signature ->
                                    viewModel.verifyPayment(
                                        razorpayOrderId = razorpayOrderId,
                                        razorpayPaymentId = paymentId,
                                        razorpaySignature = signature
                                    )
                                },
                                onError = { error -> Log.e("RAZORPAY", error) }
                            )
                        }
                    },
                    enabled = canSubmit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkGreen,
                        contentColor = Color.White,
                        disabledContainerColor = MediumGray.copy(alpha = 0.2f),
                        disabledContentColor = MediumGray.copy(alpha = 0.5f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp
                    )
                ) {
                    Text(
                        "Create Walk-In Order",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(40.dp))
            }

            // ── PROCESSING OVERLAY ───────────────────────────────
            if (uiState.uploadingFile || uiState.isLoading) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Cream
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = DarkBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = if (uiState.uploadingFile)
                                "Uploading file ${uiState.uploadProgress}%"
                            else
                                "Preparing your order",
                            fontWeight = FontWeight.Bold,
                            color = AlmostBlack,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = if (uiState.uploadingFile) "This might take a moment"
                            else "Opening payment securely...",
                            color = MediumGray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // ── ERROR DIALOG ─────────────────────────────────────
            uiState.error?.let {
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    containerColor = Cream,
                    titleContentColor = AlmostBlack,
                    textContentColor = MediumGray,
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.clearError() },
                            colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                        ) {
                            Text("OK", fontWeight = FontWeight.Bold)
                        }
                    },
                    title = {
                        Text("Something went wrong", fontWeight = FontWeight.Bold)
                    },
                    text = { Text(it) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }

    // ── REUSABLE SECTION LABEL ───────────────────────────────
    @Composable
    private fun SectionLabel(text: String) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AlmostBlack
        )
    }