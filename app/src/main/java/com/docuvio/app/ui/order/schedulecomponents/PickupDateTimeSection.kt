package com.docuvio.app.ui.order.schedulecomponents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
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
import com.docuvio.app.ui.order.utils.formatPickupDateTime
import com.docuvio.app.ui.order.utils.formatTime
import com.docuvio.app.ui.order.utils.toMinutes
import java.text.SimpleDateFormat
import java.util.*

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
            val todaySelected = selectedDateMillis == today && value != null

            // ── Today ─────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            todaySelected -> PrimaryGreen
                            !todayEnabled -> AlmostBlack.copy(alpha = 0.05f)
                            else -> SurfaceCream
                        }
                    )
                    .border(
                        width = if (todaySelected) 0.dp else 1.5.dp,
                        color = when {
                            todaySelected -> Color.Transparent
                            !todayEnabled -> AlmostBlack.copy(alpha = 0.08f)
                            else -> PrimaryGreen.copy(alpha = 0.4f)
                        },
                        shape = RoundedCornerShape(12.dp)
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
                            todaySelected -> Color.White
                            !todayEnabled -> MediumGray.copy(alpha = 0.6f)
                            else -> AlmostBlack
                        }
                    )
                    if (!todayEnabled) {
                        Text(
                            "Closed",
                            fontSize = 10.sp,
                            color = MediumGray.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // ── Tomorrow ──────────────────────────
            if (showTomorrowButton) {
                val tomorrowSelected = selectedDateMillis == tomorrow && value != null
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (tomorrowSelected) PrimaryGreen else SurfaceCream)
                        .border(
                            width = if (tomorrowSelected) 0.dp else 1.5.dp,
                            color = if (tomorrowSelected) Color.Transparent else PrimaryGreen.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { selectedDateMillis = tomorrow; showTimePicker = true },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Tomorrow",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (tomorrowSelected) Color.White else AlmostBlack
                        )
                        if (!tomorrowSelected) {
                            Text(
                                "+₹10 handling",
                                fontSize = 10.sp,
                                color = CoralRed.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

// ── Select date, then time ───────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (value != null) LightGreen.copy(alpha = 0.15f) else SurfaceCream,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    width = if (value != null) 2.dp else 1.5.dp,
                    color = if (value != null) PrimaryGreen else PrimaryGreen.copy(alpha = 0.3f),
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
                    text = value?.let { formatPickupDateTime(it) } ?: "Select date, then time",
                    color = if (value != null) AlmostBlack else MediumGray.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (value != null) FontWeight.SemiBold else FontWeight.Normal
                )
                Icon(
                    Icons.Default.CalendarToday, "Select time",
                    tint = if (value != null) PrimaryGreen else MediumGray.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Pickup available between ${formatTime(openTime)} and ${formatTime(closeTime)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MediumGray,
                maxLines = 1
            )
        }
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
                        contentColor = if (isValidTime) PrimaryGreen else MediumGray
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
                            containerColor = Cream,

                            clockDialColor = LightGreen.copy(alpha = 0.15f),
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = AlmostBlack,
                            selectorColor = PrimaryGreen,

                            periodSelectorBorderColor = PrimaryGreen.copy(alpha = 0.5f),
                            periodSelectorSelectedContainerColor = PrimaryGreen,
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = Color.White,
                            periodSelectorUnselectedContentColor = AlmostBlack,

                            timeSelectorSelectedContainerColor = PrimaryGreen,
                            timeSelectorUnselectedContainerColor = LightGreen.copy(alpha = 0.15f),
                            timeSelectorSelectedContentColor = Color.White,
                            timeSelectorUnselectedContentColor = AlmostBlack
                        )
                    )
                }
            },
            containerColor = Cream,
            shape = RoundedCornerShape(16.dp)
        )
    }
}