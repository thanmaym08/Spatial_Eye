package com.facialai.models

import com.google.gson.annotations.SerializedName

data class Place(
    val id: String,
    @SerializedName("place_name") val placeName: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("object_count") val objectCount: Int
)
