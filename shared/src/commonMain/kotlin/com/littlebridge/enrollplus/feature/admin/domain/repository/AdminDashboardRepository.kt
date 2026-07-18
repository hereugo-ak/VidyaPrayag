package com.littlebridge.enrollplus.feature.admin.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardActivity
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardAnalytics
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardOverview
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardSummary
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminHomeAnalytics
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminSetupProgress
import com.littlebridge.enrollplus.feature.admin.domain.model.DailyDigest
import com.littlebridge.enrollplus.feature.teacher.domain.model.PaceSnapshotsResponse
import com.littlebridge.enrollplus.feature.teacher.domain.model.PaceAlertsResponse
import com.littlebridge.enrollplus.feature.teacher.domain.model.PaceAlertResolveResponse

/**
 * Reads for the redesigned admin home (SchoolHomeScreenV2). Backed by
 * GET /api/admin/dashboard/{summary,analytics,activity,overview}.
 *
 * `getOverview` is the consolidated command-center payload (preferred for the
 * redesigned home); the others remain for backwards compatibility.
 */
interface AdminDashboardRepository {
    suspend fun getSummary(token: String): NetworkResult<ApiResponse<AdminDashboardSummary>>
    suspend fun getAnalytics(token: String): NetworkResult<ApiResponse<AdminDashboardAnalytics>>
    suspend fun getActivity(token: String): NetworkResult<ApiResponse<AdminDashboardActivity>>
    suspend fun getOverview(token: String): NetworkResult<ApiResponse<AdminDashboardOverview>>
    suspend fun getHomeAnalytics(token: String, dashboard: String, filter: String = "all"): NetworkResult<ApiResponse<AdminHomeAnalytics>>
    suspend fun getDigest(token: String): NetworkResult<ApiResponse<DailyDigest>>
    suspend fun getSetupProgress(token: String): NetworkResult<ApiResponse<AdminSetupProgress>>
    suspend fun updateSetupStep(token: String, stepKey: String, status: String): NetworkResult<ApiResponse<AdminSetupProgress>>

    // ── Agentic Syllabus — pace monitoring (admin) ───────────────────────────
    suspend fun getPaceSnapshots(token: String, classId: String? = null, section: String? = null): NetworkResult<PaceSnapshotsResponse>
    suspend fun getPaceAlerts(token: String): NetworkResult<PaceAlertsResponse>
    suspend fun resolvePaceAlert(token: String, alertId: String): NetworkResult<PaceAlertResolveResponse>
    suspend fun getPaceCoverage(token: String, classId: String? = null, section: String? = null): NetworkResult<PaceSnapshotsResponse>
    suspend fun recalculatePace(token: String): NetworkResult<PaceSnapshotsResponse>
}
