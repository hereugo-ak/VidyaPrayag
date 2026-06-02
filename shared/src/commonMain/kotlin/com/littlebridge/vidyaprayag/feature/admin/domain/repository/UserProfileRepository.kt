package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.GalleryRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.GalleryUpdateResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PhilosophyDetailsDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TourVideosRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.UserProfileResponse

interface UserProfileRepository {
    suspend fun getProfile(token: String): NetworkResult<ApiResponse<UserProfileResponse>>
    suspend fun updatePhilosophy(token: String, body: PhilosophyDetailsDto): NetworkResult<ApiResponse<Unit>>
    suspend fun updateTourVideos(token: String, body: TourVideosRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun updateGallery(token: String, body: GalleryRequest): NetworkResult<ApiResponse<GalleryUpdateResponse>>
}
