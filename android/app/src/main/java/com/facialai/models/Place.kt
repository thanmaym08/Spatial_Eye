package com.facialai.models

import com.google.gson.annotations.SerializedName

data class Place(
    @SerializedName("_id") val id: String = "",
    @SerializedName("name") val placeName: String = "",
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("scene_description") val sceneDescription: String = "",
    @SerializedName("image_path") val imagePath: String = "",
    val objects: List<DetectedObject> = emptyList()
) {
    val objectCount: Int
        get() = objects.size
}

