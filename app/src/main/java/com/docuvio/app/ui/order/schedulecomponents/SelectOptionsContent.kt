package com.docuvio.app.ui.order.schedulecomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.StayCurrentLandscape
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.data.model.*
import com.docuvio.app.theme.*
import com.docuvio.app.ui.order.FilePreview
import com.docuvio.app.ui.order.utils.DateUtils.isTomorrowPickup
import com.docuvio.app.ui.order.utils.FloatingPayBar
import com.docuvio.app.viewmodel.CreateOrderUiState

@Composable
fun SelectOptionsContent(
    uiState: CreateOrderUiState,
    onFileSelect: () -> Unit,
    onPaperTypeSelect: (PaperType) -> Unit,
    onColorModeSelect: (ColorMode) -> Unit,
    onFinishTypeSelect: (FinishType) -> Unit,
    onPrintSideChange: (String) -> Unit,
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
            // ── Screen title ─────────────────────────────
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

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // ── Step 1: Upload ───────────────────────
                OrderStepCard(stepNumber = 1, title = "Upload your document") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                when {
                                    uiState.isConverting -> PrimaryGreen.copy(alpha = 0.06f)
                                    uiState.selectedFile != null -> Color.Transparent
                                    else -> AlmostBlack.copy(alpha = 0.04f)
                                }
                            )
                            .border(
                                width = if (uiState.selectedFile != null) 2.dp else 1.5.dp,
                                color = when {
                                    uiState.isConverting -> PrimaryGreen.copy(alpha = 0.4f)
                                    uiState.selectedFile != null -> PrimaryGreen
                                    uiState.conversionError != null -> CoralRed.copy(alpha = 0.7f)
                                    else -> AlmostBlack.copy(alpha = 0.3f)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = !uiState.isConverting) { onFileSelect() },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            uiState.isConverting -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = PrimaryGreen,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Converting DOCX → PDF…",
                                        color = PrimaryGreen,
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
                                            .background(PrimaryGreen.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Add, null,
                                            tint = PrimaryGreen,
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
                                    Text("PDF, PNG, JPG, DOCX · Max 100MB", color = MediumGray, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Accepted: PDF, PNG, JPG, DOCX (Max 100MB)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MediumGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // ── Step 2: Copies ───────────────────────
                OrderStepCard(stepNumber = 2, title = "Number of copies") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { if (uiState.copies > 1) onCopiesChange(uiState.copies - 1) },
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryGreen
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
                            modifier = Modifier.width(64.dp),
                            textAlign = TextAlign.Center
                        )
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { onCopiesChange(uiState.copies + 1) },
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryGreen
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // ── Step 3: CV mode ──────────────────────
                OrderStepCard(
                    stepNumber = 3,
                    title = "CV Mode",
                    subtitle = "Auto-selects bond paper for resumes"
                ) {
                    CvModeToggle(isEnabled = uiState.isCvMode, onToggle = onCvModeToggle)
                }

                // ── Step 4: Color mode ───────────────────
                OrderStepCard(
                    stepNumber = 4,
                    title = "Color mode",
                    enabled = !uiState.isCvMode
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(if (uiState.isCvMode) 0.4f else 1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        filteredColorModes.forEach { colorMode ->
                            val isSelected = !uiState.isCvMode && uiState.selectedColorMode == colorMode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 80.dp)
                                    .background(
                                        if (isSelected) LightGreen.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        if (isSelected) 2.dp else 1.5.dp,
                                        if (isSelected) PrimaryGreen else AlmostBlack.copy(alpha = 0.3f),
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
                                            tint = ForestGreen,
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
                                            color = if (isSelected) ForestGreen else AlmostBlack,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
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
                }

                // ── Step 5: Paper & finish type ──────────
                OrderStepCard(stepNumber = 5, title = "Paper & finish") {
                    DropdownSection(
                        label = "Paper Type",
                        placeholder = "Select paper size",
                        selected = uiState.selectedPaperType?.name ?: "",
                        items = filteredPaperTypes,
                        itemText = { it.name },
                        enabled = !uiState.isCvMode,
                        onSelect = onPaperTypeSelect
                    )
                    Spacer(Modifier.height(16.dp))
                    DropdownSection(
                        label = "Finish Type",
                        placeholder = "Select finish type",
                        selected = uiState.selectedFinishType?.name ?: "",
                        items = filteredFinishTypes,
                        itemText = { it.name },
                        enabled = !uiState.isCvMode,
                        onSelect = onFinishTypeSelect
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Note: For spiral binding, select Finish Type",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = ForestGreen,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                // ── Step 6: Orientation ──────────────────
                OrderStepCard(stepNumber = 6, title = "Orientation") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PrintOrientation.values().forEach { orientation ->
                            val isSelected = uiState.orientation == orientation
                            val icon = when (orientation) {
                                PrintOrientation.PORTRAIT -> Icons.Outlined.StayCurrentPortrait
                                PrintOrientation.LANDSCAPE -> Icons.Outlined.StayCurrentLandscape
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 80.dp)
                                    .background(
                                        if (isSelected) LightGreen.copy(alpha = 0.2f) else Color.Transparent,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        if (isSelected) 2.dp else 1.5.dp,
                                        if (isSelected) PrimaryGreen else AlmostBlack.copy(alpha = 0.3f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onOrientationChange(orientation) }
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = orientation.displayName,
                                        tint = if (isSelected) ForestGreen else AlmostBlack.copy(alpha = 0.5f),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        orientation.displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) ForestGreen else AlmostBlack
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Step 7: Print side ───────────────────
                OrderStepCard(stepNumber = 7, title = "Print side") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .background(
                                    if (uiState.printSide == "single") LightGreen.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    if (uiState.printSide == "single") 2.dp else 1.5.dp,
                                    if (uiState.printSide == "single") PrimaryGreen else AlmostBlack.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onPrintSideChange("single") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Article,
                                    contentDescription = "Single Side",
                                    tint = if (uiState.printSide == "single") ForestGreen else AlmostBlack.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Single Side",
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.printSide == "single") ForestGreen else AlmostBlack
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .background(
                                    if (uiState.printSide == "double") LightGreen.copy(alpha = 0.2f) else Color.Transparent,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    if (uiState.printSide == "double") 2.dp else 1.5.dp,
                                    if (uiState.printSide == "double") PrimaryGreen else AlmostBlack.copy(alpha = 0.3f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onPrintSideChange("double") }
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoStories,
                                    contentDescription = "Double Side",
                                    tint = if (uiState.printSide == "double") ForestGreen else AlmostBlack.copy(alpha = 0.5f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    "Double Side",
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.printSide == "double") ForestGreen else AlmostBlack
                                )
                            }
                        }
                    }
                }

                // ── Step 8: Pickup ───────────────────────
                OrderStepCard(stepNumber = 8, title = "Pickup date & time") {
                    uiState.shop?.let { shop ->
                        PickupDateTimeSection(
                            value = uiState.pickupAt,
                            openTime = shop.openTime,
                            closeTime = shop.closeTime,
                            onValueChange = onPickupAtChange
                        )
                    }
                    if (isTomorrowPickup(uiState.pickupAt)) {
                        Spacer(Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CoralRed.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .border(1.5.dp, CoralRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
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

                // ── Step 9: Instructions ─────────────────
                OrderStepCard(
                    stepNumber = 9,
                    title = "Instructions",
                    subtitle = "Optional"
                ) {
                    DescriptionSection(value = uiState.description, onValueChange = onDescriptionChange)
                }
            }

            Spacer(modifier = Modifier.height(90.dp).navigationBarsPadding())
        }

        FloatingPayBar(uiState = uiState, onSubmit = onSubmit)

        if (showInstructions) {
            AlertDialog(
                onDismissRequest = { showInstructions = false },
                containerColor = Cream,
                titleContentColor = AlmostBlack,
                textContentColor = MediumGray,
                icon = { Icon(Icons.Outlined.Lock, null, tint = PrimaryGreen) },
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
                        colors = ButtonDefaults.textButtonColors(contentColor = ForestGreen)
                    ) {
                        Text("Continue")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}