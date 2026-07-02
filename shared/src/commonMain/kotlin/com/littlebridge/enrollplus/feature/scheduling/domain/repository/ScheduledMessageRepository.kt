package com.littlebridge.enrollplus.feature.scheduling.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.scheduling.domain.model.CreateScheduledMessageRequest
import com.littlebridge.enrollplus.feature.scheduling.domain.model.ScheduledMessageDto
import com.littlebridge.enrollplus.feature.scheduling.domain.model.ScheduledMessageListResponse
import com.littlebridge.enrollplus.feature.scheduling.domain.model.UpdateScheduledMessageRequest

interface ScheduledMessageRepository {
    suspend fun createScheduledMessage(token: String, request: CreateScheduledMessageRequest): NetworkResult<ApiResponse<ScheduledMessageDto>>
    suspend fun getScheduledMessages(token: String, status: String? = null): NetworkResult<ApiResponse<ScheduledMessageListResponse>>
    suspend fun getScheduledMessage(token: String, id: String): NetworkResult<ApiResponse<ScheduledMessageDto>>
    suspend fun updateScheduledMessage(token: String, id: String, request: UpdateScheduledMessageRequest): NetworkResult<ApiResponse<ScheduledMessageDto>>
    suspend fun cancelScheduledMessage(token: String, id: String): NetworkResult<ApiResponse<Unit>>
    suspend fun dispatchNow(token: String, id: String): NetworkResult<ApiResponse<Unit>>
}
