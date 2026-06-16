package com.littlebridge.vidyaprayag.feature.admin.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AdminDashboardActivity
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AdminDashboardAnalytics
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AdminDashboardSummary

/**
 * Reads for the redesigned admin home (SchoolHomeScreenV2). Backed by
 * GET /api/admin/dashboard/{summary,analytics,activity}.
 */
interface AdminDashboardRepository {
    suspend fun getSummary(token: String): NetworkResult<ApiResponse<AdminDashboardSummary>>
    suspend fun getAnalytics(token: String): NetworkResult<ApiResponse<AdminDashboardAnalytics>>
    suspend fun getActivity(token: String): NetworkResult<ApiResponse<AdminDashboardActivity>>
}
