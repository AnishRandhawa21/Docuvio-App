package com.docuvio.app.ui.order.schedulecomponents


import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.docuvio.app.theme.*
import androidx.compose.foundation.BorderStroke

@Composable
fun CvModeToggle(isEnabled: Boolean, onToggle: () -> Unit) {
    val bgColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isEnabled) PrimaryGreen else Color.Transparent,
        animationSpec = androidx.compose.animation.core.tween(250),
        label = "cv_bg"
    )
    val borderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isEnabled) PrimaryGreen else AlmostBlack.copy(alpha = 0.3f),
        animationSpec = androidx.compose.animation.core.tween(250),
        label = "cv_border"
    )
    val labelColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isEnabled) Color.White else AlmostBlack,
        animationSpec = androidx.compose.animation.core.tween(250),
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
                text = if (isEnabled) "CV Mode: On" else "CV Mode: Off",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = labelColor
            )
            androidx.compose.animation.AnimatedVisibility(visible = isEnabled) {
                Text(
                    text = "Bond paper auto-selected",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f)
                )
            }
        }

        androidx.compose.animation.AnimatedContent(
            targetState = isEnabled,
            transitionSpec = {
                (androidx.compose.animation.scaleIn(androidx.compose.animation.core.spring(androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)) + androidx.compose.animation.fadeIn()) togetherWith
                        (androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut())
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
                        .background(PrimaryGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 1.dp)
                ) {
                    Text("CV", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryGreen)
                }
            }
        }
    }
}

// NOTE: add this import at the top of the file alongside your existing ones:
// import androidx.compose.foundation.BorderStroke

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownSection(
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
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) AlmostBlack.copy(alpha = 0.7f) else AlmostBlack.copy(alpha = 0.35f)
        )
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded && enabled,
            onExpandedChange = { if (enabled) expanded = it }
        ) {
            Box(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(52.dp)
                    .background(
                        if (isSelected) LightGreen.copy(alpha = 0.15f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        if (isSelected) 2.dp else 1.5.dp,
                        if (isSelected) PrimaryGreen else AlmostBlack.copy(alpha = if (enabled) 0.3f else 0.15f),
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
                        selected.ifEmpty { placeholder },
                        color = when {
                            !enabled && isSelected -> AlmostBlack.copy(alpha = 0.4f)
                            isSelected -> AlmostBlack
                            else -> MediumGray
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(
                        if (enabled) Icons.Default.ArrowDropDown else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (enabled) {
                            if (isSelected) AlmostBlack else MediumGray
                        } else {
                            PrimaryGreen.copy(alpha = 0.4f)
                        }
                    )
                }
            }
            val menuShape = RoundedCornerShape(12.dp)
            ExposedDropdownMenu(
                expanded = expanded && enabled,
                onDismissRequest = { expanded = false },
                shape = menuShape,
                containerColor = Cream,
                border = BorderStroke(1.5.dp, PrimaryGreen.copy(alpha = 0.25f)),
                shadowElevation = 4.dp
            ) {
                items.forEach { item ->
                    val itemLabel = itemText(item)
                    val itemIsSelected = itemLabel == selected

                    DropdownMenuItem(
                        text = {
                            Text(
                                itemLabel,
                                color = if (itemIsSelected) ForestGreen else AlmostBlack,
                                fontWeight = if (itemIsSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        trailingIcon = {
                            if (itemIsSelected) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = ForestGreen,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        modifier = Modifier.background(
                            if (itemIsSelected) LightGreen.copy(alpha = 0.18f) else Color.Transparent
                        ),
                        onClick = { onSelect(item); expanded = false }
                    )
                }
            }
        }
    }
}


@Composable
fun DescriptionSection(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent, RoundedCornerShape(12.dp))
            .border(1.5.dp, AlmostBlack.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
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
                    color = MediumGray.copy(alpha = 0.6f)
                )
            }
            innerTextField()
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
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
                        Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA)
                    )
                )
            )
    )
}