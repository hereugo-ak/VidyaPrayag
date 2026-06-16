package com.littlebridge.vidyaprayag.feature.admin.data.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.data.remote.AdminDashboardApi
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AdminDashboardActivity
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AdminDashboardAnalytics
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AdminDashboardSummary
import com.littlebridge.vidyaprayag.feature.admin.domain.repository.AdminDashboardRepository

class AdminDashboardRepositoryImpl(
    private val api: AdminDashboardApi
) : AdminDashboardRepository {

    override suspend fun getSummary(token: String): NetworkResult<ApiResponse<AdminDashboardSummary>> =
        api.getSummary(token)

    override suspend fun getAnalytics(token: String): NetworkResult<ApiResponse<AdminDashboardAnalytics>> =
        api.getAnalytics(token)

    override suspend fun getActivity(token: String): NetworkResult<ApiResponse<AdminDashboardActivity>> =
        api.getActivity(token)
}
