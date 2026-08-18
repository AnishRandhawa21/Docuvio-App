package com.docuvio.app.data.model

import com.google.gson.annotations.SerializedName

/* ================= CREATE ORDER ================= */


data class CreateOrderRequest(
    @SerializedName("shop_id")
    val shopId: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("orientation")
    val orientation: PrintOrientation,

    @SerializedName("pickup_at")
    val pickupAt: String?,

    @SerializedName("is_handled")
    val isHandled: Boolean
)

data class CreateOrderResponse(
    @SerializedName("id")
    val id: String,

    @SerializedName("shop_id")
    val shopId: String,

    @SerializedName("status")
    val status: String
)

/* ================= UPLOAD ================= */


data class UploadResponse(
    @SerializedName("data")
    val data: UploadData
)

data class UploadData(
    @SerializedName("fileKey")
    val fileKey: String
)


/* ================= ORDERS ================= */

data class OrdersResponse(
    @SerializedName("data")
    val data: List<Order>
)

data class Order(
    @SerializedName("id")
    val id: String,

    @SerializedName("order_no")
    val orderNo: String?,

    @SerializedName("status")
    val status: String,

    @SerializedName("orientation")
    val orientation: PrintOrientation?,

    @SerializedName("total_price")
    val totalPrice: Int,

    @SerializedName("notes")
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
    val isExpired: Boolean,

    @SerializedName("is_handled")
    val isHandled: Boolean?,

    @SerializedName("handling_fee")
    val handlingFee: Int?,

    @SerializedName("documents")
    val documents: List<OrderDocument>?
)

/* ================= SHOP (RENAMED) ================= */

data class OrderShop(
    @SerializedName("shop_name")
    val shopName: String,

    @SerializedName("block")
    val block: String?
)

/* ================= DOCUMENT ================= */

data class OrderDocument(
    @SerializedName("file_name")
    val fileName: String?,

    @SerializedName("page_count")
    val pageCount: Int?,

    @SerializedName("copies")
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

fun Order.getTotalPages(): Int {
    return documents?.sumOf { it.pageCount ?: 0 } ?: 0
}

fun Order.getTotalPrints(): Int {
    return documents?.sumOf {
        (it.pageCount ?: 0) * (it.copies ?: 1)
    } ?: 0
}
