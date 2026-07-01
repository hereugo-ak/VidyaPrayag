package com.littlebridge.enrollplus.feature.admin.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolDayConfigRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDayConfigDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolDayConfigListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolDayConfigRequest

interface SchoolDayConfigRepository {
    suspend fun list(token: String): NetworkResult<ApiResponse<SchoolDayConfigListResponse>>
    suspend fun getById(token: String, id: String): NetworkResult<ApiResponse<SchoolDayConfigDto>>
    suspend fun create(token: String, request: CreateSchoolDayConfigRequest): NetworkResult<ApiResponse<SchoolDayConfigDto>>
    suspend fun update(token: String, id: String, request: UpdateSchoolDayConfigRequest): NetworkResult<ApiResponse<SchoolDayConfigDto>>
    suspend fun deactivate(token: String, id: String): NetworkResult<ApiResponse<Unit>>
    suspend fun forClass(token: String, classLevel: String, weekday: Int): NetworkResult<ApiResponse<SchoolDayConfigDto>>
}
