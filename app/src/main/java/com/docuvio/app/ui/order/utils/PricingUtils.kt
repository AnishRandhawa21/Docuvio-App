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
        val paperPrice  = uiState.selectedPaperType?.basePrice  ?: 0
        val colorPrice  = uiState.selectedColorMode?.extraPrice ?: 0
        val finishPrice = uiState.selectedFinishType?.extraPrice ?: 0
        return (paperPrice + colorPrice + finishPrice) * uiState.pageCount * uiState.copies
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