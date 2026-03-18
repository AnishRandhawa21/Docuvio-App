package com.docuvio.app.utils

import androidx.compose.ui.graphics.Color
import com.docuvio.app.data.model.Shop

object ShopStatusResolver {

    data class Capabilities(
        val walkInEnabled: Boolean,
        val onlineEnabled: Boolean,
        val bannerText: String,
        val bannerBg: Color,
        val bannerTextColor: Color
    )

    fun resolve(shop: Shop): Capabilities {

        // 🚫 Global system downtime (12 AM → 6 AM)
        if (ShopTimeUtils.isSystemUnavailable()) {
            return Capabilities(
                walkInEnabled = false,
                onlineEnabled = false,
                bannerText = "Services resume at 6:00 AM",
                bannerBg = Color(0xFFE3F2FD),      // soft blue background
                bannerTextColor = Color(0xFF1565C0) // deep blue text
            )
        }

        val bannerText: String
        val bannerBg: Color
        val bannerTextColor: Color

        val isOpen = shop.isActive
        val isAcceptingOrder = shop.isAcceptingOrder

        when {

            isOpen && isAcceptingOrder -> {
                bannerText = "OPEN"
                bannerBg = Color(0xFF2E7D32)
                bannerTextColor = Color.White
            }

            isOpen && !isAcceptingOrder -> {
                bannerText = "Open for Walk-in Orders"
                bannerBg = Color(0xFFE8F5E9)
                bannerTextColor = Color(0xFF2E7D32)
            }

            !isOpen && isAcceptingOrder -> {
                bannerText = "Online"
                bannerBg = Color(0xFFFFF3CD)
                bannerTextColor = Color(0xFF856404)
            }

            else -> {
                bannerText = "Currently Closed"
                bannerBg = Color(0xFFE0E0E0)
                bannerTextColor = Color(0xFF616161)
            }
        }

        val walkInEnabled = isOpen
        val onlineEnabled = isAcceptingOrder

        return Capabilities(
            walkInEnabled = walkInEnabled,
            onlineEnabled = onlineEnabled,
            bannerText = bannerText,
            bannerBg = bannerBg,
            bannerTextColor = bannerTextColor
        )
    }
}
