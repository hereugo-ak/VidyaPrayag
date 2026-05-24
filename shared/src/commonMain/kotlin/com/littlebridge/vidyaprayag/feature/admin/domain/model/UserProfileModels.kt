/*
 * File: UserProfileModels.kt
 * Module: feature.admin.domain.model
 *
 * DTOs for the school / institutional-profile endpoints.
 * Matches server: feature.user.UserProfileRouting.kt
 *
 *   GET  /api/v1/user/profile                → UserProfileResponse
 *   PUT  /api/v1/user/profile/philosophy     ← PhilosophyDetails
 *   PUT  /api/v1/user/profile/tour-videos    ← TourVideosRequest
 *   PUT  /api/v1/user/profile/gallery        ← GalleryRequest  → GalleryUpdateResponse
 *
 * NOTE: keep field names in lock-step with the Ktor side (snake_case via
 * @SerialName) — this is the source of truth for the wire format.
 */
package com.littlebridge.vidyaprayag.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhilosophyDetailsDto(
    @SerialName("core_mission") val coreMission: String? = null,
    @SerialName("learning_model") val learningModel: String? = null,
    @SerialName("primary_language") val primaryLanguage: String? = null
)

@Serializable
data class GalleryBlockDto(
    val images: List<String> = emptyList(),
    @SerialName("total_storage") val totalStorage: String = "10 GB",
    @SerialName("storage_used") val storageUsed: String = "0 B"
)

@Serializable
data class UserProfileResponse(
    @SerialName("public_profile") val publicProfile: Boolean = true,
    @SerialName("philosophy_details") val philosophyDetails: PhilosophyDetailsDto = PhilosophyDetailsDto(),
    @SerialName("video_tour_data") val videoTourData: List<String> = emptyList(),
    val gallery: GalleryBlockDto = GalleryBlockDto()
)

@Serializable
data class TourVideosRequest(
    @SerialName("video_tour_data") val videoTourData: List<String>
)

@Serializable
data class GalleryRequest(
    val images: List<String>
)

@Serializable
data class GalleryUpdateResponse(
    @SerialName("storage_used") val storageUsed: String,
    @SerialName("total_storage") val totalStorage: String
)
