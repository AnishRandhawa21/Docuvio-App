package com.docuvio.app.ui.printsession

import android.app.Activity
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.docuvio.app.data.model.*
import com.docuvio.app.theme.*
import com.docuvio.app.ui.order.schedulecomponents.startRazorpayPayment
import com.docuvio.app.viewmodel.PrintSessionViewModel
import com.docuvio.app.viewmodel.PrintSessionViewModelFactory
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintSessionScreen(
    viewModelFactory: PrintSessionViewModelFactory,
    onBack: () -> Unit
) {
    val viewModel: PrintSessionViewModel = viewModel(factory = viewModelFactory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    // 🔥 Root container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── PREMIUM HEADER ────────────────────────────────
            PrintSessionHeader(
                shopName = uiState.session?.shop?.shopName ?: "Print Session",
                onBack = onBack
            )

            if (uiState.isLoading && uiState.session == null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryGreen)
                }
            } else {
                val session = uiState.session
                if (session == null) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                            val isClosed = uiState.error?.contains("closed", true) == true
                            Icon(
                                imageVector = if (isClosed) Icons.Default.LockClock else Icons.Default.ErrorOutline,
                                contentDescription = null, 
                                tint = if (isClosed) BrandOrange else DarkGray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(20.dp))
                            Text(
                                text = if (isClosed) "Shop is Currently Closed" else (uiState.error ?: "Session not found"), 
                                color = AlmostBlack,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (isClosed) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Please try again during the shop's operating hours.",
                                    color = MediumGray,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            
                            Spacer(Modifier.height(32.dp))

                            // 🔥 RECOVERY BUTTON for "Locked" state
                            if (uiState.error?.contains("active session", true) == true) {
                                Button(
                                    onClick = { viewModel.rejoinExistingSession() },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Resume My Active Session", fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(12.dp))
                            }

                            OutlinedButton(
                                onClick = onBack, 
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Go Back")
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        PrintSessionContent(
                            uiState = uiState,
                            viewModel = viewModel,
                            onDetailsSubmit = { name, phone -> viewModel.submitDetails(name, phone) },
                            onFileSelect = { uri -> handleFileSelection(context, uri) { file, mime -> viewModel.uploadFile(file, mime) } },
                            onPayClick = {
                                viewModel.createPayment { razorpayOrderId, amount ->
                                    if (activity != null) {
                                        startRazorpayPayment(activity, razorpayOrderId, amount, { _ -> })
                                        viewModel.setStatusLocally(PrintSessionStatus.PAYMENT_PENDING)
                                    }
                                }
                            },
                            onHomeClick = {
                                viewModel.dismissActiveSession()
                                onBack()
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
        )
    }
}

@Composable
fun PrintSessionHeader(shopName: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .background(AlmostBlack.copy(alpha = 0.05f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AlmostBlack, modifier = Modifier.size(20.dp))
        }
        
        Spacer(Modifier.width(16.dp))
        
        Column {
            Text(
                text = shopName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                color = AlmostBlack,
                maxLines = 1
            )
            Text(
                text = "Live Print Session",
                style = MaterialTheme.typography.labelSmall,
                color = MediumGray
            )
        }
    }
}

@Composable
fun PrintSessionContent(
    uiState: com.docuvio.app.viewmodel.PrintSessionUiState,
    viewModel: PrintSessionViewModel,
    onDetailsSubmit: (String, String) -> Unit,
    onFileSelect: (Uri) -> Unit,
    onPayClick: () -> Unit,
    onHomeClick: () -> Unit
) {
    val status = uiState.session?.status ?: PrintSessionStatus.CREATED

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp, start = 20.dp, end = 20.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            StatusProgressIndicator(status)
            Spacer(Modifier.height(28.dp))
        }

        item {
            AnimatedContent(
                targetState = status,
                transitionSpec = {
                    fadeIn(tween(400)) + slideInVertically { it / 2 } togetherWith
                    fadeOut(tween(400)) + slideOutVertically { -it / 2 }
                },
                label = "statusTransition"
            ) { targetStatus ->
                when (targetStatus) {
                    PrintSessionStatus.CREATED, PrintSessionStatus.CONNECTED -> {
                        CustomerDetailsForm(onDetailsSubmit, uiState.isLoading, viewModel)
                    }
                    PrintSessionStatus.CUSTOMER_DETAILS, PrintSessionStatus.FILES_UPLOADING, PrintSessionStatus.FILES_UPLOADED -> {
                        FileUploadSection(uiState, onFileSelect)
                    }
                    PrintSessionStatus.REVIEWING -> {
                        WaitingSection("Shop is reviewing files...", "Please wait while the shopkeeper checks your documents and provides a quote.")
                    }
                    PrintSessionStatus.QUOTE_READY -> {
                        QuotationSection(
                            amount = uiState.session?.quotedAmount ?: uiState.session?.totalAmount ?: 0, 
                            onPayClick = onPayClick,
                            isLoading = uiState.isLoading
                        )
                    }
                    PrintSessionStatus.PAYMENT_PENDING -> {
                        WaitingSection("Verifying Payment...", "We are waiting for payment confirmation from the bank. This screen will update automatically.")
                    }
                    PrintSessionStatus.PAID -> {
                        SuccessSection("Payment Successful", "Payment received! The shop is now preparing your documents for printing.", Icons.Default.CheckCircle, onHomeClick)
                    }
                    PrintSessionStatus.PRINTING -> {
                        SuccessSection("Printing in Progress", "The shop has started printing your documents.", Icons.Default.Print, onHomeClick)
                    }
                    PrintSessionStatus.READY_FOR_PICKUP -> {
                        SuccessSection("Ready for Pickup", "Great news! Your prints are ready. You can head to the counter now.", Icons.Default.CheckCircle, onHomeClick)
                    }
                    PrintSessionStatus.COMPLETED -> {
                        SuccessSection("Order Completed", "Thank you for using Docuvio! Hope you have a great day.", Icons.Default.DoneAll, onHomeClick)
                    }
                    PrintSessionStatus.EXPIRED -> {
                        ErrorSection("Session Expired", "This print session is no longer active. Please scan a new QR code to start again.")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusProgressIndicator(status: PrintSessionStatus) {
    val step = when(status) {
        PrintSessionStatus.CREATED, PrintSessionStatus.CONNECTED -> 1
        PrintSessionStatus.CUSTOMER_DETAILS, PrintSessionStatus.FILES_UPLOADING, PrintSessionStatus.FILES_UPLOADED -> 2
        PrintSessionStatus.REVIEWING, PrintSessionStatus.QUOTE_READY -> 3
        else -> 4
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StepCircle(1, "Contact", step >= 1)
        ProgressLine(step > 1)
        StepCircle(2, "Upload", step >= 2)
        ProgressLine(step > 2)
        StepCircle(3, "Quote", step >= 3)
        ProgressLine(step > 3)
        StepCircle(4, "Print", step >= 4)
    }
}

@Composable
fun StepCircle(number: Int, label: String, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isActive) SuccessGreen else AlmostBlack.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            if (isActive && number < 4) {
                Icon(Icons.Default.Check, null, tint = White, modifier = Modifier.size(16.dp))
            } else {
                Text(number.toString(), color = if (isActive) White else MediumGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (isActive) AlmostBlack else MediumGray)
    }
}

@Composable
fun RowScope.ProgressLine(isFilled: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(2.dp)
            .padding(horizontal = 4.dp)
            .offset(y = (-10).dp)
            .background(if (isFilled) SuccessGreen else AlmostBlack.copy(alpha = 0.1f))
    )
}

@Composable
fun CustomerDetailsForm(onSubmit: (String, String) -> Unit, isLoading: Boolean = false, viewModel: PrintSessionViewModel? = null) {
    var name by remember { mutableStateOf(viewModel?.getPrefilledName() ?: "") }
    var phone by remember { mutableStateOf(viewModel?.getPrefilledPhone() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = AlmostBlack.copy(alpha = 0.1f))
            .clip(RoundedCornerShape(24.dp))
            .background(White)
            .padding(24.dp)
    ) {
        if (isLoading && name.isNotBlank() && phone.isNotBlank()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                CircularProgressIndicator(color = SuccessGreen)
                Spacer(Modifier.height(16.dp))
                Text("Personalizing your session...", fontWeight = FontWeight.Medium)
                Text("Using your saved contact info", fontSize = 12.sp, color = MediumGray)
            }
        } else {
            Text("Get Started", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold))
            Text("Enter your contact info to link your files.", style = MaterialTheme.typography.bodySmall, color = MediumGray)
            
            Spacer(Modifier.height(24.dp))
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = AlmostBlack.copy(alpha = 0.1f)
                )
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone Number") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = AlmostBlack.copy(alpha = 0.1f)
                )
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = { if (name.isNotBlank() && phone.length == 10) onSubmit(name, phone) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                enabled = !isLoading && name.length >= 3 && phone.length == 10
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Proceed to Upload", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FileUploadSection(uiState: com.docuvio.app.viewmodel.PrintSessionUiState, onFileSelect: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { onFileSelect(it) }
    }

    val status = uiState.session?.status ?: PrintSessionStatus.CREATED
    
    Column(modifier = Modifier.fillMaxWidth()) {
        if (status == PrintSessionStatus.FILES_UPLOADED) {
            Surface(
                color = SuccessGreen.copy(alpha = 0.08f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("Files sent! Add more or wait for the shop to review.", fontSize = 13.sp, color = SuccessGreen, fontWeight = FontWeight.Medium)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(if (uiState.isUploading) White else AlmostBlack.copy(alpha = 0.03f))
                .border(2.dp, if (uiState.isUploading) PrimaryGreen.copy(alpha = 0.3f) else Color.Transparent, RoundedCornerShape(24.dp))
                .clickable(enabled = !uiState.isUploading) { launcher.launch("*/*") },
            contentAlignment = Alignment.Center
        ) {
            if (uiState.isUploading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        progress = { (uiState.uploadProgress ?: 0) / 100f },
                        color = SuccessGreen,
                        strokeWidth = 6.dp,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Uploading... ${uiState.uploadProgress ?: 0}%", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CloudUpload, null, tint = PrimaryGreen, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to Select Files", fontWeight = FontWeight.Bold, color = AlmostBlack)
                    Text("PDF, JPG, PNG up to 10MB", style = MaterialTheme.typography.labelSmall, color = MediumGray)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Text("Uploaded Documents", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
            Text("${uiState.files.size} Total", style = MaterialTheme.typography.labelSmall, color = MediumGray)
        }
        Spacer(Modifier.height(16.dp))
        
        if (uiState.files.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                Text("No files uploaded yet", color = MediumGray, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            uiState.files.forEach { file ->
                FileItem(file)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun FileItem(file: PrintSessionFile) {
    Surface(
        color = White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val extension = file.fileName.substringAfterLast(".", "FILE").uppercase()
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)).background(PrimaryGreen.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(extension, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
            }
            Spacer(Modifier.width(14.dp))
            Text(file.fileName, modifier = Modifier.weight(1f), maxLines = 1, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
            if (file.status == "uploaded") {
                Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun WaitingSection(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            val infiniteTransition = rememberInfiniteTransition(label = "waiting")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f, targetValue = 1.3f,
                animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "pulse"
            )
            Box(modifier = Modifier.size(80.dp).graphicsLayer { scaleX = scale; scaleY = scale }.background(PrimaryGreen.copy(alpha = 0.1f), CircleShape))
            CircularProgressIndicator(color = SuccessGreen, strokeWidth = 3.dp, modifier = Modifier.size(56.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MediumGray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
fun QuotationSection(amount: Int, onPayClick: () -> Unit, isLoading: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(20.dp, RoundedCornerShape(32.dp), spotColor = AlmostBlack.copy(alpha = 0.15f))
            .clip(RoundedCornerShape(32.dp))
            .background(White)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = SuccessGreen.copy(alpha = 0.08f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "READY TO PRINT",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                fontWeight = FontWeight.Bold,
                color = SuccessGreen
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Final Quotation",
            style = MaterialTheme.typography.titleMedium,
            color = AlmostBlack.copy(alpha = 0.6f)
        )

        Spacer(Modifier.height(12.dp))

        // ── Amount Area ───────────────────────────────────
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                "₹",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AlmostBlack,
                modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
            )
            Text(
                "$amount",
                fontSize = 72.sp,
                fontWeight = FontWeight.Black,
                color = AlmostBlack,
                letterSpacing = (-2).sp
            )
        }

        Spacer(Modifier.height(32.dp))
        HorizontalDivider(color = AlmostBlack.copy(alpha = 0.06f), thickness = 1.dp)
        Spacer(Modifier.height(24.dp))

        // ── Info Rows ─────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            InfoRow("Delivery", "Instant Printing", SuccessGreen)
            InfoRow("Shop Status", "Ready for Pickup", AlmostBlack)
        }

        Spacer(Modifier.height(32.dp))

        // ── Pay Button ────────────────────────────────────
        Button(
            onClick = onPayClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
            enabled = !isLoading,
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 2.dp
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(
                    "Pay",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        Text(
            "Securely processed by Razorpay",
            style = MaterialTheme.typography.labelSmall,
            color = MediumGray
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MediumGray)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = valueColor
        )
    }
}

@Composable
fun SuccessSection(title: String, description: String, icon: ImageVector, onHomeClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(100.dp).background(SuccessGreen.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, modifier = Modifier.size(48.dp), tint = SuccessGreen)
        }
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MediumGray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
        
        Spacer(Modifier.height(40.dp))
        
        Button(
            onClick = onHomeClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
        ) {
            Text("Return Home", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ErrorSection(title: String, description: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Error, null, modifier = Modifier.size(80.dp), tint = CoralRed)
        Spacer(Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = CoralRed)
        Spacer(Modifier.height(12.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MediumGray, textAlign = TextAlign.Center)
    }
}

private fun handleFileSelection(context: android.content.Context, uri: Uri, onFileReady: (File, String) -> Unit) {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"
    val fileName = getFileName(context, uri) ?: "upload_file"
    
    val file = File(context.cacheDir, fileName)
    try {
        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        onFileReady(file, mimeType)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) result = it.getString(index)
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) result = result?.substring(cut + 1)
    }
    return result
}
