package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreatePtmRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PtmActiveEventDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PtmResponse

interface PtmRepository {
    suspend fun getPtm(token: String): NetworkResult<ApiResponse<PtmResponse>>
    suspend fun createPtm(token: String, request: CreatePtmRequest): NetworkResult<ApiResponse<PtmActiveEventDto>>
}
