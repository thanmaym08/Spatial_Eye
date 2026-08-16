package com.facialai.models

import com.google.gson.annotations.SerializedName

data class DetectedObject(
    @SerializedName("name") val name: String,
    @SerializedName("confidence") val confidence: Float,
    @SerializedName("x_center") val xCenter: Float,
    @SerializedName("y_center") val yCenter: Float,
    @SerializedName("width") val width: Float,
    @SerializedName("height") val height: Float,
    @SerializedName("spatial_zone") val spatialZone: String
)
