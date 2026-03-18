package com.docuvio.app.data.model

import com.google.gson.annotations.SerializedName

data class AttachDocumentRequest(
    @SerializedName("fileKey")
    val fileKey: String,

    @SerializedName("fileName")
    val fileName: String,

    @SerializedName("page_count")
    val pageCount: Int,

    val copies: Int,

    @SerializedName("paper_type_id")
    val paperTypeId: String,

    @SerializedName("color_mode_id")
    val colorModeId: String,

    @SerializedName("finish_type_id")
    val finishTypeId: String,

    @SerializedName("pickup_at")
    val pickupAt: String? = null,
)
data class AttachWalkInDocument(

    @SerializedName("fileKey")
    val fileKey: String,

    @SerializedName("fileName")
    val fileName: String,

    @SerializedName("page_count")
    val pageCount: Int = 1,   // always send 1

    @SerializedName("manual_price")
    val manualPrice: Int
)