package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.SchoolProfileApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.SchoolProfileDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.UpdateSchoolProfileRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.SchoolProfileRepository

class SchoolProfileRepositoryImpl(
    private val api: SchoolProfileApi
) : SchoolProfileRepository {

    override suspend fun getProfile(token: String): NetworkResult<ApiResponse<SchoolProfileDto>> =
        api.getProfile(token)

    override suspend fun updateProfile(
        token: String,
        request: UpdateSchoolProfileRequest
    ): NetworkResult<ApiResponse<SchoolProfileDto>> =
        api.updateProfile(token, request)
}
