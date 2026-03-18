package com.docuvio.app.utils

import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DISPLAY_FORMATTER =
    DateTimeFormatter
        .ofPattern("dd MMM • hh:mm a")
        .withZone(ZoneId.systemDefault())

fun formatOrderDate(date: String?): String? {
    if (date.isNullOrBlank()) return null

    return try {

        // ✅ Parses: 2026-01-19T11:25:02.943732+00:00
        val offsetDateTime = OffsetDateTime.parse(date)

        DISPLAY_FORMATTER.format(offsetDateTime.toInstant())

    } catch (e: Exception) {
        null
    }
}
