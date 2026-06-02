/*
 * File: PtmModels.kt
 * Module: feature.admin.domain.model
 *
 * DTOs for school PTM endpoints.
 * Matches server: feature.school.PtmRouting.kt
 *
 * GET /api/v1/school/ptm  → PtmResponse
 * POST /api/v1/school/ptm → PtmActiveEventDto
 */
package com.littlebridge.vidyaprayag.feature.admin.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PtmActiveEventDto(
    val id: String,
    val title: String,
    val date: String,
    val slot: String,
    @SerialName("expected_parents") val expectedParents: Int,
    @SerialName("checked_in_parents") val checkedInParents: Int,
    @SerialName("invites_delivered") val invitesDelivered: Int,
    @SerialName("read_receipts") val readReceipts: Int
)

@Serializable
data class PtmHistoryDto(
    val id: String,
    val date: String,
    val title: String,
    val turnout: Int,
    @SerialName("total_met") val totalMet: Int
)

@Serializable
data class PtmClassProgressDto(
    val id: String,
    @SerialName("class_name") val className: String,
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("met_count") val metCount: Int,
    @SerialName("total_count") val totalCount: Int,
    val progress: Float
)

@Serializable
data class PtmResponse(
    @SerialName("active_event") val activeEvent: PtmActiveEventDto? = null,
    val history: List<PtmHistoryDto> = emptyList(),
    @SerialName("class_progress") val classProgress: List<PtmClassProgressDto> = emptyList()
)

@Serializable
data class CreatePtmRequest(
    val title: String,
    val date: String,  // YYYY-MM-DD
    val slot: String
)
