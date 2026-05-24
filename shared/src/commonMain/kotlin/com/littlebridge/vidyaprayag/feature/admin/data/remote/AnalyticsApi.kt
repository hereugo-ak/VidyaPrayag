/*
 * File: AnalyticsApi.kt
 * Module: feature.admin.data.remote
 *
 * Network client for school analytics endpoints.
 *
 * Server routes:
 *   GET /api/v1/school/analytics/overview
 *   GET /api/v1/school/analytics/class-performance?class=<optional>
 *   GET /api/v1/school/analytics/teacher-performance
 *   GET /api/v1/school/analytics/student/{studentId}
 *   GET /api/v1/school/analytics/syllabus-coverage
 */
package com.littlebridge.vidyaprayag.feature.admin.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AnalyticsOverviewResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.StudentAnalyticsResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

class AnalyticsApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun getOverview(
        token: String
    ): NetworkResult<ApiResponse<AnalyticsOverviewResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/analytics/overview")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getStudentAnalytics(
        token: String,
        studentId: String
    ): NetworkResult<ApiResponse<StudentAnalyticsResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/analytics/student/$studentId")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
