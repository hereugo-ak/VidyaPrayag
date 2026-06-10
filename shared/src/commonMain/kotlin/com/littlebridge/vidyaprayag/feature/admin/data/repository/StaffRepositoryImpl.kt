package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.StaffApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateStaffRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.StaffDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.StaffListResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.UpdateStaffRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.StaffRepository

class StaffRepositoryImpl(
    private val api: StaffApi
) : StaffRepository {

    override suspend fun getStaff(token: String, query: String?, department: String?): NetworkResult<ApiResponse<StaffListResponse>> =
        api.getStaff(token, query, department)

    override suspend fun createStaff(token: String, request: CreateStaffRequest): NetworkResult<ApiResponse<StaffDto>> =
        api.createStaff(token, request)

    override suspend fun getStaffProfile(token: String, staffId: String): NetworkResult<ApiResponse<StaffDto>> =
        api.getStaffProfile(token, staffId)

    override suspend fun updateStaff(token: String, staffId: String, request: UpdateStaffRequest): NetworkResult<ApiResponse<StaffDto>> =
        api.updateStaff(token, staffId, request)

    override suspend fun deleteStaff(token: String, staffId: String): NetworkResult<ApiResponse<Unit>> =
        api.deleteStaff(token, staffId)
}
