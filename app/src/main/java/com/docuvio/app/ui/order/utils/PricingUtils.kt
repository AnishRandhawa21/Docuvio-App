package com.docuvio.app.ui.order.utils

import com.docuvio.app.viewmodel.CreateOrderUiState
import com.docuvio.app.ui.order.utils.DateUtils.isTomorrowPickup

object PricingUtils {

    fun calculatePlatformFee(documentPrice: Int): Int {
        return when {
            documentPrice < 20  -> 2
            documentPrice <= 50  -> 4
            documentPrice <= 80  -> 6
            documentPrice <= 100 -> 9
            documentPrice <= 200 -> 13
            else                 -> 16
        }
    }

    fun calculateDocumentPrice(uiState: CreateOrderUiState): Int {

        val basePrice = uiState.selectedPaperType?.basePrice ?: 0

        val colorPrice  = uiState.selectedColorMode?.extraPrice ?: 0
        val finishPrice = uiState.selectedFinishType?.extraPrice ?: 0

        val pages  = uiState.pageCount
        val copies = uiState.copies

        if (pages == 0 || copies == 0) return 0

        val sheets = if (uiState.printSide == "double") {
            kotlin.math.ceil(pages / 2.0).toInt()
        } else {
            pages
        }

        // 🔥 ALWAYS USE BASE PRICE
        val pricePerSheet = basePrice

        return sheets * copies * (pricePerSheet + colorPrice + finishPrice)
    }

    fun calculateHandlingFee(uiState: CreateOrderUiState): Int {
        return if (isTomorrowPickup(uiState.pickupAt)) 10 else 0
    }

    fun calculateTotal(uiState: CreateOrderUiState): Int {
        val documentPrice = calculateDocumentPrice(uiState)
        val platformFee   = calculatePlatformFee(documentPrice)
        val handlingFee   = calculateHandlingFee(uiState)
        return documentPrice + platformFee + handlingFee
    }
}