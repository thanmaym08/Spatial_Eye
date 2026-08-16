package com.facialai.models

import com.google.gson.annotations.SerializedName

data class CheckChangesResponse(
    @SerializedName("change_result") val changeResult: ChangeResult,
    @SerializedName("message") val message: String
)

data class ChangeResult(
    @SerializedName("new_objects") val newObjects: List<DetectedObject>,
    @SerializedName("removed_objects") val removedObjects: List<DetectedObject>,
    @SerializedName("moved_objects") val movedObjects: List<DetectedObject>,
    @SerializedName("changes_summary") val changesSummary: String,
    @SerializedName("alert_messages") val alertMessages: List<String>,
    @SerializedName("has_important_changes") val hasImportantChanges: Boolean,
    @SerializedName("tts_message") val ttsMessage: String
)
