package com.docuvio.app.ui.order.utils

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

fun String.toMinutes(): Int {
    val parts = split(":")
    val hour = parts[0].toInt()
    val minute = parts[1].toInt()
    return hour * 60 + minute
}

fun formatTime(time: String): String {
    return try {
        val parts = time.split(":")
        val hour24 = parts[0].toInt()
        val minute = parts[1].toInt()

        val amPm = if (hour24 >= 12) "PM" else "AM"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }

        "$hour12:${minute.toString().padStart(2, '0')} $amPm"
    } catch (e: Exception) {
        time // fallback, never crash UI
    }
}

fun formatPickupDateTime(isoString: String): String {
    val possibleFormats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss"
    )

    val date = possibleFormats.firstNotNullOfOrNull { pattern ->
        try {
            SimpleDateFormat(pattern, Locale.getDefault()).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(isoString)
        } catch (_: Exception) {
            null
        }
    } ?: return "Invalid date"

    return SimpleDateFormat(
        "MMM dd, yyyy 'at' hh:mm a",
        Locale.getDefault()
    ).format(date)
}