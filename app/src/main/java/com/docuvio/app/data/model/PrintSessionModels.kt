package com.docuvio.app.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PrintSessionStatus {
    @SerializedName("created") @SerialName("created") CREATED,
    @SerializedName("connected") @SerialName("connected") CONNECTED,
    @SerializedName("customer_details") @SerialName("customer_details") CUSTOMER_DETAILS,
    @SerializedName("files_uploading") @SerialName("files_uploading") FILES_UPLOADING,
    @SerializedName("files_uploaded") @SerialName("files_uploaded") FILES_UPLOADED,
    @SerializedName("reviewing") @SerialName("reviewing") REVIEWING,
    @SerializedName("quote_ready") @SerialName("quote_ready") QUOTE_READY,
    @SerializedName("payment_pending") @SerialName("payment_pending") PAYMENT_PENDING,
    @SerializedName("paid") @SerialName("paid") PAID,
    @SerializedName("printing") @SerialName("printing") PRINTING,
    @SerializedName("ready_for_pickup") @SerialName("ready_for_pickup") READY_FOR_PICKUP,
    @SerializedName("completed") @SerialName("completed") COMPLETED,
    @SerializedName("expired") @SerialName("expired") EXPIRED
}

@Serializable
data class PrintSession(
    @SerializedName("id") val id: String? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("sessionToken") val sessionToken: String? = null, 
    @SerializedName("shop_id") val shopId: String? = null,
    @SerializedName("status") val status: PrintSessionStatus? = null,
    @SerializedName("quoted_amount") val quotedAmount: Int? = null,
    @SerializedName("total_amount") val totalAmount: Int? = null,
    @SerializedName("customer_name") val customerName: String? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("shop") val shop: Shop? = null
) {
    val activeToken: String get() = token ?: sessionToken ?: ""
}

@Serializable
data class PrintSessionFile(
    @SerializedName("id") val id: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("file_name") val fileName: String,
    @SerializedName("file_url") val fileUrl: String? = null,
    @SerializedName("status") val status: String
)

@Serializable
data class PrintSessionQuote(
    @SerializedName("total_amount") val totalAmount: Int,
    @SerializedName("currency") val currency: String = "INR"
)

data class StartSessionResponse(
    @SerializedName("session") val session: PrintSession? = null,
    @SerializedName("sessionToken") val sessionToken: String? = null
)

data class CustomerDetailsRequest(
    @SerializedName("customerName") val name: String,
    @SerializedName("customerPhone") val phone: String
)

data class SessionPaymentResponse(
    @SerializedName("payment") val payment: SessionPaymentData
)

data class SessionPaymentData(
    @SerializedName("razorpay_order_id") val orderId: String,
    @SerializedName("amount") val amount: Int
)
