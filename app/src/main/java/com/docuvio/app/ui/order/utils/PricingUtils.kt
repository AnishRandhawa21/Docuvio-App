package com.docuvio.app.ui.order.utils

import com.docuvio.app.viewmodel.CreateOrderUiState
import com.docuvio.app.ui.order.utils.DateUtils.isTomorrowPickup

object PricingUtils {

    fun calculateTotal(uiState: CreateOrderUiState): Int {

        val paperPrice = uiState.selectedPaperType?.basePrice ?: 0
        val colorPrice = uiState.selectedColorMode?.extraPrice ?: 0
        val finishPrice = uiState.selectedFinishType?.extraPrice ?: 0

        val baseTotal =
            (paperPrice + colorPrice + finishPrice) *
                    uiState.pageCount *
                    uiState.copies

        val handlingFee = if (isTomorrowPickup(uiState.pickupAt)) 10 else 0

        return baseTotal + handlingFee
    }
}