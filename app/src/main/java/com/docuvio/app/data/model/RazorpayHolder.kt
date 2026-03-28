package com.docuvio.app.data.model

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

data class RazorpayResult(
    val orderId: String,
    val paymentId: String,
    val signature: String,
    val cancelled: Boolean = false,
    val errorMessage: String? = null
)

object RazorpayHolder {
    private val _resultFlow = MutableSharedFlow<RazorpayResult>(extraBufferCapacity = 1)
    val resultFlow = _resultFlow.asSharedFlow()

    var result: RazorpayResult? = null
        set(value) {
            field = value
            value?.let { _resultFlow.tryEmit(it) }
        }
}
