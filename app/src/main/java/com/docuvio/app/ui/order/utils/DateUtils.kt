package com.docuvio.app.ui.order.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    fun isTomorrowPickup(pickupAt: String?): Boolean {

        if (pickupAt == null) return false

        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss"
        )

        val date = formats.firstNotNullOfOrNull { pattern ->
            try {
                SimpleDateFormat(pattern, Locale.getDefault()).parse(pickupAt)
            } catch (_: Exception) {
                null
            }
        } ?: return false

        val pickupCal = Calendar.getInstance().apply { time = date }
        val tomorrowCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }

        return pickupCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                pickupCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
    }
}