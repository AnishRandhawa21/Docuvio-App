package com.docuvio.app.ui.order

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.docuvio.app.theme.AlmostBlack
import com.docuvio.app.theme.Blue
import com.docuvio.app.theme.CoralRed
import com.docuvio.app.theme.Cream
import com.docuvio.app.theme.GoldenYellow
import com.docuvio.app.theme.LimeGreen
import com.docuvio.app.theme.MediumGray
import com.docuvio.app.theme.OffWhite
import com.docuvio.app.theme.SoftBlue
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material.icons.filled.CalendarToday
import com.docuvio.app.theme.DarkBlue
import java.text.SimpleDateFormat
import java.util.*

import com.docuvio.app.ui.order.utils.PricingUtils.calculateTotal
import com.docuvio.app.ui.order.utils.DateUtils.isTomorrowPickup
import com.docuvio.app.ui.order.utils.formatPickupDateTime
import com.docuvio.app.ui.order.utils.formatTime
import com.docuvio.app.ui.order.utils.toMinutes

/* -------------------------------------------------- */
/* ---------------- MAIN SCREEN ----------------------*/
/* -------------------------------------------------- */


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

    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@rememberLauncherForActivityResult

            val fileName = getFileName(context, uri)
            val input = context.contentResolver.openInputStream(uri)
            val file = File(context.cacheDir, fileName)

            input?.use { inp ->
                FileOutputStream(file).use { out ->
                    inp.copyTo(out)
                }
            }

            viewModel.setFileAndReadPages(context, file)
        }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onOrderSuccess()
    }

    when (uiState.currentStep) {

        OrderStep.LOADING_OPTIONS ->
            LoadingScreen("Loading print options...")

        OrderStep.SELECT_OPTIONS ->
            SelectOptionsContent(
                uiState = uiState,
                onFileSelect = { filePickerLauncher.launch("application/pdf") },
                onPaperTypeSelect = viewModel::setPaperType,
                onColorModeSelect = viewModel::setColorMode,
                onFinishTypeSelect = viewModel::setFinishType,
                onCopiesChange = viewModel::setCopies,
                onOrientationChange = viewModel::setOrientation,
                onDescriptionChange = viewModel::setDescription,
                onPickupAtChange = viewModel::setPickupAt,
                onSubmit = {

                    if (activity == null) return@SelectOptionsContent

                    viewModel.submitOrder { razorpayOrderId, amount ->
                        startRazorpayPayment(
                            activity = activity,
                            razorpayOrderId = razorpayOrderId,
                            amount = amount,
                            onSuccess = { paymentId, signature ->
                                viewModel.verifyPayment(
                                    razorpayOrderId,
                                    paymentId,
                                    signature
                                )
                            },
                            onError = { errorMsg ->
                                Log.e("RAZORPAY", "Payment Error: $errorMsg")
                            }
                        )
                    }
                }
            )

        OrderStep.UPLOADING ->
            UploadingFileScreen()

        OrderStep.CREATING_ORDER ->
            LoadingScreen("Creating order...")

        OrderStep.PROCESSING_PAYMENT ->
            LoadingScreen("Processing payment...")

        OrderStep.SUCCESS ->
            LoadingScreen("Redirecting…")
    }

    uiState.error?.let { errorMessage ->

        AlertDialog(
            onDismissRequest = { viewModel.clearError() },

            containerColor = Cream,
            titleContentColor = AlmostBlack,
            textContentColor = MediumGray,

            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = CoralRed
                )
            },

            title = {
                Text(
                    text = "Something went wrong",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },

            text = {
                Text(
                    text = errorMessage,
                    fontSize = 15.sp
                )
            },

            confirmButton = {
                TextButton(
                    onClick = { viewModel.clearError() },
                    colors = ButtonDefaults.textButtonColors(contentColor = CoralRed)
                ) {
                    Text(text = "OK", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
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

                viewModel.verifyPayment(
                    razorpayOrderId = it.orderId,
                    razorpayPaymentId = it.paymentId,
                    razorpaySignature = it.signature
                )
            }
            kotlinx.coroutines.delay(500)
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- SELECT OPTIONS -------------------*/
/* -------------------------------------------------- */

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
    onSubmit: () -> Unit
) {

    var showInstructions by remember { mutableStateOf(false) }

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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Print Order",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = AlmostBlack
                )

                InfoIconButton { showInstructions = true }
            }


            // File Upload Section
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
                                width = if (uiState.selectedFile != null) 2.dp else 1.5.dp,
                                color = if (uiState.selectedFile != null) DarkBlue.copy(alpha = 0.7f)
                                else AlmostBlack.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(16.dp)
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
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = DarkBlue,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = "Tap to upload PDF",
                                    color = AlmostBlack,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Max 500MB",
                                    color = MediumGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = "Accepted format: PDF Max 500MB per file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Options Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column {
                    // Number of Copies
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Number of Copies: ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AlmostBlack,
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable {
                                        if (uiState.copies > 1) onCopiesChange(uiState.copies - 1)
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = DarkBlue
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "−",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = uiState.copies.toString(),
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
                                    Text(
                                        text = "+",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Color Mode
                    Text(
                        text = "Color Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AlmostBlack
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        uiState.printOptions?.colorModes?.forEach { colorMode ->
                            val isSelected = uiState.selectedColorMode == colorMode

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(140.dp)
                                    .background(
                                        color = if (isSelected) SoftBlue.copy(alpha = 0.1f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) DarkBlue else AlmostBlack.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onColorModeSelect(colorMode) }
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
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = Blue,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(22.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Column(
                                        modifier = Modifier.height(44.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = colorMode.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) DarkBlue else AlmostBlack,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "₹${colorMode.extraPrice}/page",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (isSelected) Color(0xFF2E7D32) else MediumGray
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        when {
                                            colorMode.name.contains("color", ignoreCase = true) -> GradientDot()
                                            colorMode.name.contains("cv", ignoreCase = true) -> SolidDot(Color(0xFFF3ECDC))
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

                    // Paper Type Dropdown
                    DropdownSection(
                        label = "Paper Type",
                        placeholder = "Select paper size",
                        selected = uiState.selectedPaperType?.name ?: "",
                        items = uiState.printOptions?.paperTypes ?: emptyList(),
                        itemText = { "${it.name} - ₹${it.basePrice}" },
                        onSelect = onPaperTypeSelect
                    )
                    Spacer(Modifier.height(24.dp))

                    // Finish Type Dropdown
                    DropdownSection(
                        label = "Finish Type",
                        placeholder = "Select finish type",
                        selected = uiState.selectedFinishType?.name ?: "",
                        items = uiState.printOptions?.finishTypes ?: emptyList(),
                        itemText = { "${it.name} - ₹${it.extraPrice}" },
                        onSelect = onFinishTypeSelect
                    )

                    Spacer(Modifier.height(24.dp))

                    // Orientation
                    Text(
                        text = "Orientation",
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
                                        color = if (isSelected) OffWhite.copy(alpha = 0.3f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else 1.5.dp,
                                        color = if (isSelected) DarkBlue else AlmostBlack.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onOrientationChange(orientation) }
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = orientation.displayName,
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
                                    .background(
                                        color = OffWhite.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = CoralRed,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = CoralRed
                                    )
                                    Column {
                                        Text(
                                            text = "Handling Fee Applied",
                                            fontWeight = FontWeight.Bold,
                                            color = AlmostBlack,
                                            fontSize = 16.sp
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = "₹10 added for next-day pickup",
                                            color = CoralRed,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    DescriptionSection(
                        value = uiState.description,
                        onValueChange = onDescriptionChange
                    )
                }
            }

            Spacer(
                modifier = Modifier
                    .height(100.dp)
                    .navigationBarsPadding()
            )
        }

        // Bottom Bar
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.Bottom
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AlmostBlack,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total ${uiState.pageCount} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = LimeGreen
                        )
                        Text(
                            text = "₹${calculateTotal(uiState)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Button(
                        onClick = onSubmit,
                        enabled = uiState.selectedFile != null &&
                                uiState.selectedPaperType != null &&
                                uiState.selectedFinishType != null &&
                                uiState.pickupAt != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LimeGreen,
                            disabledContainerColor = MediumGray.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .width(180.dp)
                    ) {
                        Text(
                            text = "Proceed To Pay",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        if (showInstructions) {
            AlertDialog(
                onDismissRequest = { showInstructions = false },
                containerColor = Cream,
                titleContentColor = AlmostBlack,
                textContentColor = MediumGray,
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = CoralRed
                    )
                },
                title = {
                    Text(text = "Printing Instructions", fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InstructionItem("For CV printing, select BOND in Paper Type and CV in Finish Type.")
                        InstructionItem("Upload PDF files only.")
                        InstructionItem("Urgent printing adds an extra ₹10 to the total price.")
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showInstructions = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = LimeGreen)
                    ) {
                        Text("Got it")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- DROPDOWN ------------------------- */
/* -------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownSection(
    label: String,
    placeholder: String,
    selected: String,
    items: List<T>,
    itemText: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isSelected = selected.isNotEmpty()

    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AlmostBlack
        )
        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            Box(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        color = if (isSelected) OffWhite.copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.5.dp,
                        color = if (isSelected) DarkBlue else AlmostBlack.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { expanded = true }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (selected.isEmpty()) placeholder else selected,
                        color = if (isSelected) AlmostBlack else MediumGray,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (isSelected) AlmostBlack else MediumGray
                    )
                }
            }

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(OffWhite)
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(text = itemText(item), color = AlmostBlack) },
                        onClick = {
                            onSelect(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- HELPERS -------------------------- */
/* -------------------------------------------------- */

@Composable
private fun LoadingScreen(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = GoldenYellow, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(text = text, color = MediumGray, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun UploadingFileScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Cream),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                modifier = Modifier.width(220.dp).height(6.dp),
                color = GoldenYellow,
                trackColor = MediumGray.copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Uploading document…",
                color = MediumGray,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/* -------------------------------------------------- */
/* ---------------- RAZORPAY ------------------------- */
/* -------------------------------------------------- */

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

/* -------------------------------------------------- */
/* ---------------- FILE NAME HELPER ----------------- */
/* -------------------------------------------------- */

private fun getFileName(context: Context, uri: Uri): String {
    val cursor = context.contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (it.moveToFirst()) return it.getString(nameIndex)
    }
    return "document.pdf"
}

@Composable
fun DescriptionSection(value: String, onValueChange: (String) -> Unit) {
    Column {
        Text(
            text = "Instruction",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AlmostBlack
        )
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent, RoundedCornerShape(12.dp))
                .border(
                    width = 1.5.dp,
                    color = AlmostBlack.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
                )
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
                        text = "Any special instructions? (optional)",
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
            imageVector = Icons.Outlined.Info,
            contentDescription = "Instructions",
            tint = CoralRed,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun InstructionItem(text: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(text = "•", color = AlmostBlack, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
        Text(text = text, color = AlmostBlack, fontSize = 14.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SolidDot(color: Color) {
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(color)
            .border(width = 1.dp, color = AlmostBlack.copy(alpha = 0.3f), shape = RoundedCornerShape(9.dp))
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
            text = "Pickup Date & Time",
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
                        width = if (selectedDateMillis == today && value != null) 0.dp else 1.5.dp,
                        color = if (!todayEnabled) MediumGray.copy(alpha = 0.3f) else AlmostBlack.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = todayEnabled) {
                        selectedDateMillis = today
                        showTimePicker = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Today",
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
                            text = "Closed",
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
                            width = if (selectedDateMillis == tomorrow && value != null) 0.dp else 1.5.dp,
                            color = AlmostBlack.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            selectedDateMillis = tomorrow
                            showTimePicker = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tomorrow",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (selectedDateMillis == tomorrow && value != null) Color.White else AlmostBlack
                        )
                        if (selectedDateMillis != tomorrow || value == null) {
                            Text(
                                text = "+₹10 handling",
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
                    color = if (value != null) OffWhite.copy(alpha = 0.3f) else Color.Transparent,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (value != null) 2.dp else 1.5.dp,
                    color = if (value != null) DarkBlue else AlmostBlack.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp)
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
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select time",
                    tint = if (value != null) SoftBlue else MediumGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Pickup available between ${formatTime(openTime)} and ${formatTime(closeTime)}",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MediumGray,
            maxLines = 1
        )
    }

    if (showTimePicker && selectedDateMillis != null) {

        val timePickerState = rememberTimePickerState(is24Hour = false)

        val now = Calendar.getInstance()
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

        // ✅ 30-minute buffer from current time
        val earliestAllowedMinutes = currentMinutes + 30

        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val isToday = selectedDateMillis == todayStart

        val isValidTime = run {
            val selected = timePickerState.hour * 60 + timePickerState.minute
            when {
                // Today: must be within shop hours AND at least 30 min from now
                isToday -> selected in openMinutes until lastOrderMinutes &&
                        selected >= earliestAllowedMinutes
                // Tomorrow: just within shop hours
                else -> selected in openMinutes until closeMinutes
            }
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

                        val isoFormat = SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
                            Locale.getDefault()
                        )
                        onValueChange(isoFormat.format(calendar.time))
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
                Text(
                    text = "Select Pickup Time",
                    fontWeight = FontWeight.Bold,
                    color = AlmostBlack
                )
            },
            text = {
                Column {
                    // ✅ Updated hint text to mention the 30-min buffer for today
                    Text(
                        text = if (isToday)
                            "Min. 30 min from now · Available until ${formatTime(closeTime)}"
                        else
                            "Available between ${formatTime(openTime)} and ${formatTime(closeTime)}",
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