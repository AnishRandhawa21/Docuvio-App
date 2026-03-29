package com.docuvio.app.data.model

import com.google.gson.annotations.SerializedName

/* -----------------------------
   SHOP
----------------------------- */
data class Shop(
    @SerializedName("id")
    val id: String,

    @SerializedName("shop_name")
    val shopName: String,

    @SerializedName("block")
    val block: String,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("open_time")
    val openTime: String,

    @SerializedName("close_time")
    val closeTime: String,

    @SerializedName("is_accepting_orders")
    val isAcceptingOrder: Boolean
)

/* -----------------------------
   PRINT OPTIONS RESPONSE
----------------------------- */

/* -----------------------------
   PAPER TYPE
----------------------------- */
data class PaperType(
    val id: String,
    val name: String,
    @SerializedName("base_price")
    val basePrice: Int,
    @SerializedName("double_side_price")
    val doubleSidePrice: Int
)

data class ColorMode(
    val id: String,
    val name: String,
    @SerializedName("extra_price")
    val extraPrice: Int
)

data class FinishType(
    val id: String,
    val name: String,
    @SerializedName("extra_price")
    val extraPrice: Int
)

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T
)

