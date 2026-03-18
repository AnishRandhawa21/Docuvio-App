package com.docuvio.app.data.model

import com.google.gson.annotations.SerializedName

data class WalkInOrderRequest(

    @SerializedName("shop_id")
    val shopId: String,

    @SerializedName("order_type")
    val orderType: String = "walk_in",

    val notes: String? = null
)