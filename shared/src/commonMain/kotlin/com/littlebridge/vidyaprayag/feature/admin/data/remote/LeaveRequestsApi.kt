/*
 * File: LeaveRequestsApi.kt
 * Module: feature.admin.data.remote
 *
 * Network client for leave-requests endpoints.
 *
 * Server routes:
 *   GET   /api/v1/school/leave-requests?type=student|teacher&status=...
 *   POST  /api/v1/school/leave-requests
 *   PATCH /api/v1/school/leave-requests/{id}/status
 */
package com.littlebridge.vidyaprayag.feature.admin.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CreateLeaveRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.LeaveRequestDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.LeaveRequestsResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.UpdateLeaveStatusRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class LeaveRequestsApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun getLeaveRequests(
        token: String,
        type: String = "student",
        status: String? = null
    ): NetworkResult<ApiResponse<LeaveRequestsResponse>> = safeApiCall {
        val params = buildList {
            add("type=$type")
            status?.let { add("status=$it") }
        }.joinToString("&")
        client.get(getUrl("api/v1/school/leave-requests?$params")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun createLeaveRequest(
        token: String,
        request: CreateLeaveRequest
    ): NetworkResult<ApiResponse<LeaveRequestDto>> = safeApiCall {
        client.post(getUrl("api/v1/school/leave-requests")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun updateLeaveStatus(
        token: String,
        id: String,
        status: String
    ): NetworkResult<ApiResponse<LeaveRequestDto>> = safeApiCall {
        client.patch(getUrl("api/v1/school/leave-requests/$id/status")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(UpdateLeaveStatusRequest(status))
        }
    }
}
