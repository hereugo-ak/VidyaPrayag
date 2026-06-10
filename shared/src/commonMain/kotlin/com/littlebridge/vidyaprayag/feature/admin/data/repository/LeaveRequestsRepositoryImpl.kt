package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.LeaveRequestsApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateLeaveRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.LeaveRequestDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.LeaveRequestsResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.LeaveRequestsRepository

class LeaveRequestsRepositoryImpl(
    private val api: LeaveRequestsApi
) : LeaveRequestsRepository {

    override suspend fun getLeaveRequests(
        token: String,
        type: String,
        status: String?
    ): NetworkResult<ApiResponse<LeaveRequestsResponse>> = api.getLeaveRequests(token, type, status)

    override suspend fun createLeaveRequest(
        token: String,
        request: CreateLeaveRequest
    ): NetworkResult<ApiResponse<LeaveRequestDto>> = api.createLeaveRequest(token, request)

    override suspend fun updateLeaveStatus(
        token: String,
        id: String,
        status: String
    ): NetworkResult<ApiResponse<LeaveRequestDto>> = api.updateLeaveStatus(token, id, status)
}
