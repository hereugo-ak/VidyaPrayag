package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.AnalyticsApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AnalyticsOverviewResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.StudentAnalyticsResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.AnalyticsRepository

class AnalyticsRepositoryImpl(private val api: AnalyticsApi) : AnalyticsRepository {
    override suspend fun getOverview(token: String): NetworkResult<ApiResponse<AnalyticsOverviewResponse>> = api.getOverview(token)
    override suspend fun getStudentAnalytics(token: String, studentId: String): NetworkResult<ApiResponse<StudentAnalyticsResponse>> = api.getStudentAnalytics(token, studentId)
}
