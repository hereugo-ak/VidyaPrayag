package com.littlebridge.enrollplus.feature.scheduling.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.scheduling.data.remote.ScheduledMessageApi
import com.littlebridge.enrollplus.feature.scheduling.domain.model.CreateScheduledMessageRequest
import com.littlebridge.enrollplus.feature.scheduling.domain.model.ScheduledMessageDto
import com.littlebridge.enrollplus.feature.scheduling.domain.model.ScheduledMessageListResponse
import com.littlebridge.enrollplus.feature.scheduling.domain.model.UpdateScheduledMessageRequest
import com.littlebridge.enrollplus.feature.scheduling.domain.repository.ScheduledMessageRepository

class ScheduledMessageRepositoryImpl(
    private val api: ScheduledMessageApi
) : ScheduledMessageRepository {

    override suspend fun createScheduledMessage(
        token: String,
        request: CreateScheduledMessageRequest
    ): NetworkResult<ApiResponse<ScheduledMessageDto>> =
        api.createScheduledMessage(token, request)

    override suspend fun getScheduledMessages(
        token: String,
        status: String?
    ): NetworkResult<ApiResponse<ScheduledMessageListResponse>> =
        api.getScheduledMessages(token, status)

    override suspend fun getScheduledMessage(
        token: String,
        id: String
    ): NetworkResult<ApiResponse<ScheduledMessageDto>> =
        api.getScheduledMessage(token, id)

    override suspend fun updateScheduledMessage(
        token: String,
        id: String,
        request: UpdateScheduledMessageRequest
    ): NetworkResult<ApiResponse<ScheduledMessageDto>> =
        api.updateScheduledMessage(token, id, request)

    override suspend fun cancelScheduledMessage(
        token: String,
        id: String
    ): NetworkResult<ApiResponse<Unit>> =
        api.cancelScheduledMessage(token, id)

    override suspend fun dispatchNow(
        token: String,
        id: String
    ): NetworkResult<ApiResponse<Unit>> =
        api.dispatchNow(token, id)
}
