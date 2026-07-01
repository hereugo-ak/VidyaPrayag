package com.littlebridge.enrollplus.feature.admin.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.data.remote.SchoolDayConfigApi
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolDayConfigRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDayConfigDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDayConfigListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolDayConfigRequest
import com.littlebridge.enrollplus.feature.admin.domain.repository.SchoolDayConfigRepository

class SchoolDayConfigRepositoryImpl(
    private val api: SchoolDayConfigApi,
) : SchoolDayConfigRepository {

    override suspend fun list(token: String): NetworkResult<ApiResponse<SchoolDayConfigListResponse>> =
        api.list(token)

    override suspend fun getById(token: String, id: String): NetworkResult<ApiResponse<SchoolDayConfigDto>> =
        api.getById(token, id)

    override suspend fun create(token: String, request: CreateSchoolDayConfigRequest): NetworkResult<ApiResponse<SchoolDayConfigDto>> =
        api.create(token, request)

    override suspend fun update(token: String, id: String, request: UpdateSchoolDayConfigRequest): NetworkResult<ApiResponse<SchoolDayConfigDto>> =
        api.update(token, id, request)

    override suspend fun deactivate(token: String, id: String): NetworkResult<ApiResponse<Unit>> =
        api.deactivate(token, id)

    override suspend fun forClass(token: String, classLevel: String, weekday: Int): NetworkResult<ApiResponse<SchoolDayConfigDto>> =
        api.forClass(token, classLevel, weekday)
}
