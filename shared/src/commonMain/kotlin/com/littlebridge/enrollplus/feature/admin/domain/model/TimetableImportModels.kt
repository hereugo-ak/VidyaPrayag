package com.littlebridge.enrollplus.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TimetableImportResponse(
    val slots: List<SchoolDaySlotDto>,
    val name: String = "",
    @SerialName("ai_used") val aiUsed: Boolean = false,
    @SerialName("raw_text") val rawText: String? = null,
)

@Serializable
data class TimetableImportOcrRequest(
    val image: String,
    @SerialName("mime_type") val mimeType: String = "image/jpeg",
)

@Serializable
data class TimetableImportTextRequest(
    val text: String,
)
