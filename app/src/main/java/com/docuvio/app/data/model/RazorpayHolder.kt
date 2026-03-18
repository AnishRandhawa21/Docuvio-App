package com.docuvio.app.data.model

data class RazorpayResult(
    val orderId: String,
    val paymentId: String,
    val signature: String,
    val cancelled: Boolean = false,
    val errorMessage: String? = null
)

object RazorpayHolder {
    var result: RazorpayResult? = null
}
