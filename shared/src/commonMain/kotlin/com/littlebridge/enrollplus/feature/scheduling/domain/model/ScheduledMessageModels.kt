package com.littlebridge.enrollplus.feature.scheduling.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class CreateScheduledMessageRequest(
    val messageType: String,
    val scheduledAt: String,
    val payload: JsonElement,
    val addToCalendar: Boolean = false,
    val audienceType: String = "ALL_SCHOOL",
    val audienceLabel: String? = null,
    val title: String? = null,
    val bodyPreview: String? = null,
    @SerialName("client_msg_id") val clientMsgId: String? = null,
)

@Serializable
data class UpdateScheduledMessageRequest(
    val scheduledAt: String? = null,
    val payload: JsonElement? = null,
    val title: String? = null,
    val bodyPreview: String? = null,
    val audienceType: String? = null,
    val audienceLabel: String? = null,
    val addToCalendar: Boolean? = null,
)

@Serializable
data class ScheduledMessageDto(
    val id: String,
    val messageType: String,
    val status: String,
    val scheduledAt: String,
    val dispatchedAt: String? = null,
    val payload: JsonElement,
    val createdBy: String,
    val authorRole: String,
    val authorName: String? = null,
    val audienceType: String,
    val audienceLabel: String? = null,
    val title: String? = null,
    val bodyPreview: String? = null,
    val addToCalendar: Boolean = false,
    val calendarEventCode: String? = null,
    val retryCount: Int = 0,
    val lastError: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ScheduledMessageListResponse(
    val messages: List<ScheduledMessageDto>,
    val total: Int,
)
