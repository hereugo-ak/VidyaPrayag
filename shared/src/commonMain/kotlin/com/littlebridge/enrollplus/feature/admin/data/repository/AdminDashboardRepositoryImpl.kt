package com.littlebridge.enrollplus.feature.admin.data.repository

import com.littlebridge.enrollplus.core.cache.CacheManager
import com.littlebridge.enrollplus.core.cache.cacheFirstNetworkResult
import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.data.remote.AdminDashboardApi
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardActivity
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardAnalytics
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardOverview
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardSummary
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminHomeAnalytics
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminSetupProgress
import com.littlebridge.enrollplus.feature.admin.domain.model.DailyDigest
import com.littlebridge.enrollplus.feature.admin.domain.repository.AdminDashboardRepository
import com.littlebridge.enrollplus.feature.teacher.domain.model.PaceSnapshotsResponse
import com.littlebridge.enrollplus.feature.teacher.domain.model.PaceAlertsResponse
import com.littlebridge.enrollplus.feature.teacher.domain.model.PaceAlertResolveResponse

class AdminDashboardRepositoryImpl(
    private val api: AdminDashboardApi,
    private val cache: CacheManager,
) : AdminDashboardRepository {

    override suspend fun getSummary(token: String): NetworkResult<ApiResponse<AdminDashboardSummary>> =
        cacheFirstNetworkResult(cache, "admin_dashboard_summary", ApiResponse.serializer(AdminDashboardSummary.serializer())) { api.getSummary(token) }

    override suspend fun getAnalytics(token: String): NetworkResult<ApiResponse<AdminDashboardAnalytics>> =
        cacheFirstNetworkResult(cache, "admin_dashboard_analytics", ApiResponse.serializer(AdminDashboardAnalytics.serializer())) { api.getAnalytics(token) }

    override suspend fun getActivity(token: String): NetworkResult<ApiResponse<AdminDashboardActivity>> =
        cacheFirstNetworkResult(cache, "admin_dashboard_activity", ApiResponse.serializer(AdminDashboardActivity.serializer())) { api.getActivity(token) }

    override suspend fun getOverview(token: String): NetworkResult<ApiResponse<AdminDashboardOverview>> =
        cacheFirstNetworkResult(cache, "admin_dashboard_overview", ApiResponse.serializer(AdminDashboardOverview.serializer())) { api.getOverview(token) }

    override suspend fun getHomeAnalytics(token: String, dashboard: String, filter: String): NetworkResult<ApiResponse<AdminHomeAnalytics>> =
        api.getHomeAnalytics(token, dashboard, filter)

    override suspend fun getDigest(token: String): NetworkResult<ApiResponse<DailyDigest>> =
        api.getDigest(token)

    override suspend fun getSetupProgress(token: String): NetworkResult<ApiResponse<AdminSetupProgress>> =
        api.getSetupProgress(token)

    override suspend fun updateSetupStep(token: String, stepKey: String, status: String): NetworkResult<ApiResponse<AdminSetupProgress>> =
        api.updateSetupStep(token, stepKey, status)

    // ── Agentic Syllabus — pace monitoring (admin) ───────────────────────────
    override suspend fun getPaceSnapshots(token: String, classId: String?, section: String?): NetworkResult<PaceSnapshotsResponse> =
        cacheFirstNetworkResult(cache, "admin_pace_snapshots_v2_${classId ?: "all"}_${section ?: "all"}", PaceSnapshotsResponse.serializer()) { api.getPaceSnapshots(token, classId, section) }

    override suspend fun getPaceAlerts(token: String): NetworkResult<PaceAlertsResponse> =
        cacheFirstNetworkResult(cache, "admin_pace_alerts_v2", PaceAlertsResponse.serializer()) { api.getPaceAlerts(token) }

    override suspend fun resolvePaceAlert(token: String, alertId: String): NetworkResult<PaceAlertResolveResponse> =
        api.resolvePaceAlert(token, alertId)

    override suspend fun getPaceCoverage(token: String, classId: String?, section: String?): NetworkResult<PaceSnapshotsResponse> =
        cacheFirstNetworkResult(cache, "admin_pace_coverage_v2_${classId ?: "all"}_${section ?: "all"}", PaceSnapshotsResponse.serializer()) { api.getPaceCoverage(token, classId, section) }

    override suspend fun recalculatePace(token: String): NetworkResult<PaceSnapshotsResponse> =
        api.recalculatePace(token)
}
