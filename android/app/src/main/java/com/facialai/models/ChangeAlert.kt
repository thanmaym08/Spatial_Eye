package com.facialai.models

import com.google.gson.annotations.SerializedName

data class ChangeAlertResponse(
    @SerializedName("changes_detected") val changesDetected: Boolean,
    @SerializedName("risk_level") val riskLevel: String, // "HIGH", "MEDIUM", "LOW", "NONE"
    @SerializedName("tts_message") val ttsMessage: String,
    @SerializedName("changes_list") val changesList: List<String>
)
