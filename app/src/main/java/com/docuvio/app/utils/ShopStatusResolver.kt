package com.docuvio.app.utils

import androidx.compose.ui.graphics.Color
import com.docuvio.app.data.model.Shop
import com.docuvio.app.theme.DarkBlue
import com.docuvio.app.theme.SoftBlue
import com.docuvio.app.theme.SuccessGreen
import com.docuvio.app.theme.DeepAmber
import com.docuvio.app.theme.SoftGreen
import com.docuvio.app.theme.SoftBeige
import com.docuvio.app.theme.DarkGray
import com.docuvio.app.theme.DarkGreen
import com.docuvio.app.theme.LightGray
import com.docuvio.app.theme.White

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
                bannerText = "RESUMES 6 AM",   // shortened from "Services resume at 6:00 AM"
                bannerBg = SoftBlue,
                bannerTextColor = DarkBlue
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
                bannerBg = SuccessGreen
                bannerTextColor = White
            }

            isOpen && !isAcceptingOrder -> {
                bannerText = "WALK-IN ONLY"   // shortened from "Open for Walk-in Orders"
                bannerBg = SoftGreen
                bannerTextColor = DarkGreen
            }

            !isOpen && isAcceptingOrder -> {
                bannerText = "ONLINE"          // shortened from "Online" + made consistent caps
                bannerBg = SoftBeige
                bannerTextColor = DeepAmber
            }

            else -> {
                bannerText = "CLOSED"
                bannerBg = LightGray
                bannerTextColor = DarkGray
            }
        }

        return Capabilities(
            walkInEnabled = isOpen,
            onlineEnabled = isAcceptingOrder,
            bannerText = bannerText,
            bannerBg = bannerBg,
            bannerTextColor = bannerTextColor
        )
    }
}