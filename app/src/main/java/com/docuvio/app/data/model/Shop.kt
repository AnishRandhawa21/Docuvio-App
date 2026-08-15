package com.docuvio.app.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Shop(
    @SerializedName("id") @SerialName("id") val id: String,
    @SerializedName("shop_name") @SerialName("shop_name") val shopName: String,
    @SerializedName("block") @SerialName("block") val block: String,
    @SerializedName("is_active") @SerialName("is_active") val isActive: Boolean,
    @SerializedName("open_time") @SerialName("open_time") val openTime: String,
    @SerializedName("close_time") @SerialName("close_time") val closeTime: String,
    @SerializedName("is_accepting_orders") @SerialName("is_accepting_orders") val isAcceptingOrder: Boolean
)

@Serializable
data class PaperType(
    @SerializedName("id") @SerialName("id") val id: String,
    @SerializedName("name") @SerialName("name") val name: String,
    @SerializedName("base_price") @SerialName("base_price") val basePrice: Int,
    @SerializedName("double_side_price") @SerialName("double_side_price") val doubleSidePrice: Int
)

@Serializable
data class ColorMode(
    @SerializedName("id") @SerialName("id") val id: String,
    @SerializedName("name") @SerialName("name") val name: String,
    @SerializedName("extra_price") @SerialName("extra_price") val extraPrice: Int
)

@Serializable
data class FinishType(
    @SerializedName("id") @SerialName("id") val id: String,
    @SerializedName("name") @SerialName("name") val name: String,
    @SerializedName("extra_price") @SerialName("extra_price") val extraPrice: Int
)

data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T? = null
)
