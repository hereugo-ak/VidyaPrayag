package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.UserProfileApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.GalleryRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.GalleryUpdateResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PhilosophyDetailsDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TourVideosRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.UserProfileResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.VisibilityRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.VisibilityResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.UserProfileRepository

class UserProfileRepositoryImpl(private val api: UserProfileApi) : UserProfileRepository {
    override suspend fun getProfile(token: String): NetworkResult<ApiResponse<UserProfileResponse>> =
        api.getProfile(token)

    override suspend fun updatePhilosophy(token: String, body: PhilosophyDetailsDto): NetworkResult<ApiResponse<Unit>> =
        api.updatePhilosophy(token, body)

    override suspend fun updateTourVideos(token: String, body: TourVideosRequest): NetworkResult<ApiResponse<Unit>> =
        api.updateTourVideos(token, body)

    override suspend fun updateGallery(token: String, body: GalleryRequest): NetworkResult<ApiResponse<GalleryUpdateResponse>> =
        api.updateGallery(token, body)

    override suspend fun updateVisibility(token: String, body: VisibilityRequest): NetworkResult<ApiResponse<VisibilityResponse>> =
        api.updateVisibility(token, body)
}
