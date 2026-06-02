package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AnnouncementDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AnnouncementListResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateAnnouncementRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.SyncWhatsAppResponse

interface AnnouncementsRepository {
    suspend fun getAnnouncements(token: String): NetworkResult<ApiResponse<AnnouncementListResponse>>
    suspend fun searchAnnouncements(token: String, query: String): NetworkResult<ApiResponse<AnnouncementListResponse>>
    suspend fun createAnnouncement(token: String, request: CreateAnnouncementRequest): NetworkResult<ApiResponse<AnnouncementDto>>
    suspend fun syncWhatsApp(token: String): NetworkResult<ApiResponse<SyncWhatsAppResponse>>
}
