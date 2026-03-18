package com.docuvio.app.data.model

import com.google.gson.annotations.SerializedName

data class PrintOptions(
    @SerializedName("paper_types")
    val paperTypes: List<PaperType> = emptyList(),

    @SerializedName("color_modes")
    val colorModes: List<ColorMode> = emptyList(),

    @SerializedName("finish_types")
    val finishTypes: List<FinishType> = emptyList()
)
