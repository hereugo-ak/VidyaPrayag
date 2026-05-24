package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AnalyticsOverviewResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.StudentAnalyticsResponse
import kotlinx.serialization.json.JsonElement

interface AnalyticsRepository {
    suspend fun getOverview(token: String): NetworkResult<ApiResponse<AnalyticsOverviewResponse>>
    suspend fun getStudentAnalytics(token: String, studentId: String): NetworkResult<ApiResponse<StudentAnalyticsResponse>>
    suspend fun getClassPerformance(token: String, className: String? = null): NetworkResult<ApiResponse<JsonElement>>
    suspend fun getTeacherPerformance(token: String): NetworkResult<ApiResponse<JsonElement>>
    suspend fun getSyllabusCoverage(token: String): NetworkResult<ApiResponse<JsonElement>>
    suspend fun getStudentCohort(token: String): NetworkResult<ApiResponse<JsonElement>>
}
