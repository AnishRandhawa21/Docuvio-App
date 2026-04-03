package com.docuvio.app.utils

import java.time.LocalTime
import java.time.format.DateTimeFormatter

object ShopTimeUtils {

    private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")

    private val systemStart = LocalTime.of(0, 0)   // 12:00 AM
    private val systemEnd = LocalTime.of(6, 0)     // 6:00 AM

    fun isSystemUnavailable(): Boolean {
        val now = LocalTime.now()
        return !now.isBefore(systemStart) && now.isBefore(systemEnd)
    }

    fun isShopOpen(openTime: String, closeTime: String): Boolean {

        // 🚫 Global rule (12 AM → 6 AM)
        if (isSystemUnavailable()) return false

        return try {

            val now = LocalTime.now()

            val open = LocalTime.parse(openTime, formatter)
            val close = LocalTime.parse(closeTime, formatter)

            if (close.isAfter(open)) {
                // Normal case (09:00 → 17:00)
                !now.isBefore(open) && now.isBefore(close)
            } else {
                // Overnight case (18:00 → 02:00)
                now.isAfter(open) || now.isBefore(close)
            }

        } catch (e: Exception) {
            false
        }
    }
}

//new commit
