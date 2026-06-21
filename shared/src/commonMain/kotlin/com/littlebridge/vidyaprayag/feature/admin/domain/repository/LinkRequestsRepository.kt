package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.LinkDecisionResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.LinkRequestsResponse

/** RA-48: school-admin link-request queue. */
interface LinkRequestsRepository {
    suspend fun getLinkRequests(token: String, status: String = "pending"): NetworkResult<ApiResponse<LinkRequestsResponse>>
    suspend fun approve(token: String, id: String): NetworkResult<ApiResponse<LinkDecisionResult>>
    suspend fun reject(token: String, id: String): NetworkResult<ApiResponse<LinkDecisionResult>>
}
