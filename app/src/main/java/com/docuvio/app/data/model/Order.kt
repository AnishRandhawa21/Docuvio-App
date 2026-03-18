package com.docuvio.app.data.model

import com.google.gson.annotations.SerializedName

/* ================= CREATE ORDER ================= */


data class CreateOrderRequest(

    @SerializedName("shop_id")
    val shopId: String,

    val description: String,
    val orientation: PrintOrientation,

    @SerializedName("pickup_at")
    val pickupAt: String?,

    @SerializedName("is_handled")
    val isHandled: Boolean
)

data class CreateOrderResponse(
    val id: String,

    @SerializedName("shop_id")
    val shopId: String,

    val status: String
)

/* ================= UPLOAD ================= */


data class UploadResponse(
    val data: UploadData
)

data class UploadData(
    val fileKey: String
)


/* ================= ORDERS ================= */

data class OrdersResponse(
    val data: List<Order>
)

data class Order(
    val id: String,

    @SerializedName("order_no")
    val orderNo: String?,

    val status: String,

    val orientation: PrintOrientation?,

//    @SerializedName("is_urgent")
//    val isUrgent: Boolean?,

    @SerializedName("total_price")
    val totalPrice: Int,

    val notes: String?,

    @SerializedName("is_paid")
    val isPaid: Boolean,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("pickup_at")
    val pickupAt: String?,

    @SerializedName("shops")
    val shop: OrderShop?,

    @SerializedName("delivery_otp")
    val deliveryOtp: String?,

    @SerializedName("otp_verified")
    val otpVerified: Boolean,

    @SerializedName("is_expired")
    val isExpired: Boolean, // ✅ NEW

    @SerializedName("is_handled")
    val isHandled: Boolean?,

    @SerializedName("handling_fee")
    val handlingFee: Int?,

    val documents: List<OrderDocument>?
)

/* ================= SHOP (RENAMED) ================= */

data class OrderShop(
    @SerializedName("shop_name")
    val shopName: String,

    val block: String?
)

/* ================= DOCUMENT ================= */

data class OrderDocument(
    @SerializedName("file_name")
    val fileName: String?,

    @SerializedName("page_count")
    val pageCount: Int?,

    val copies: Int?
)

fun ColorMode.displayName(): String {
    return when (name.trim()) {
        "Black & White" -> "B&W"
        else -> name
    }
}

enum class PrintOrientation(
    @SerializedName("portrait")
    val apiValue: String,
    val displayName: String
) {
    @SerializedName("portrait")
    PORTRAIT("portrait", "Portrait"),

    @SerializedName("landscape")
    LANDSCAPE("landscape", "Landscape")
}


fun String.lastSix(): String {
    return if (length <= 6) this else takeLast(6)
}

