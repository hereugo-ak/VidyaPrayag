/*
 * File: AnnouncementModels.kt
 * Module: feature.admin.domain.model
 *
 * DTOs for the school announcements API.
 * Matches server: feature.announcements.AnnouncementRouting.kt
 */
package com.littlebridge.vidyaprayag.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AnnouncementDto(
    val type: String,
    @SerialName("event_id") val eventId: String,
    val title: String,
    @SerialName("sub_title") val subTitle: String? = null,
    val description: String,
    @SerialName("event_image") val eventImage: String? = null,
    val date: String
)

@Serializable
data class AnnouncementListResponse(
    val announcements: List<AnnouncementDto>
)

@Serializable
data class CreateAnnouncementRequest(
    val type: String,
    val title: String,
    @SerialName("sub_title") val subTitle: String? = null,
    val description: String,
    @SerialName("event_image") val eventImage: String? = null,
    val date: String
)

@Serializable
data class SyncWhatsAppResponse(
    @SerialName("job_id") val jobId: String,
    @SerialName("total_queued") val totalQueued: Int,
    @SerialName("estimated_time_minutes") val estimatedTimeMinutes: Int
)
