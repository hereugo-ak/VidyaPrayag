/*
 * File: AdminDashboardApi.kt
 * Module: feature.admin.data.remote
 *
 * Network client for the redesigned admin home dashboard.
 *
 * Server routes (all JWT — the bearer token is attached automatically by the
 * Ktor Auth plugin; the `token` arg only gates the call in the ViewModel):
 *   GET /api/admin/dashboard/summary
 *   GET /api/admin/dashboard/analytics
 *   GET /api/admin/dashboard/activity
 */
package com.littlebridge.enrollplus.feature.admin.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardActivity
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardAnalytics
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardOverview
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminDashboardSummary
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminHomeAnalytics
import com.littlebridge.enrollplus.feature.admin.domain.model.AdminSetupProgress
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateAdminSetupStepRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.DailyDigest
import com.littlebridge.enrollplus.feature.teacher.domain.model.PaceSnapshotsResponse
import com.littlebridge.enrollplus.feature.teacher.domain.model.PaceAlertsResponse
import com.littlebridge.enrollplus.feature.teacher.domain.model.PaceAlertResolveResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AdminDashboardApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun getSummary(
        token: String
    ): NetworkResult<ApiResponse<AdminDashboardSummary>> = safeApiCall {
        client.get(getUrl("api/admin/dashboard/summary"))
    }

    suspend fun getAnalytics(
        token: String
    ): NetworkResult<ApiResponse<AdminDashboardAnalytics>> = safeApiCall {
        client.get(getUrl("api/admin/dashboard/analytics"))
    }

    suspend fun getActivity(
        token: String
    ): NetworkResult<ApiResponse<AdminDashboardActivity>> = safeApiCall {
        client.get(getUrl("api/admin/dashboard/activity"))
    }

    suspend fun getHomeAnalytics(
        token: String,
        dashboard: String,
        filter: String = "all",
    ): NetworkResult<ApiResponse<AdminHomeAnalytics>> = safeApiCall {
        client.get(getUrl("api/admin/dashboard/home-analytics")) {
            parameter("dashboard", dashboard)
            parameter("filter", filter)
        }
    }

    /**
     * Consolidated command-center payload powering the redesigned
     * SchoolHomeScreenV2 in ONE network call (School Pulse, KPIs, insights,
     * parent engagement, communication, events, teacher spotlight,
     * achievements, fee analytics, birthdays).
     */
    suspend fun getOverview(
        token: String
    ): NetworkResult<ApiResponse<AdminDashboardOverview>> = safeApiCall {
        client.get(getUrl("api/admin/dashboard/overview"))
    }

    /** Daily focus digest for the redesigned home hero. */
    suspend fun getDigest(
        token: String
    ): NetworkResult<ApiResponse<DailyDigest>> = safeApiCall {
        client.get(getUrl("api/admin/dashboard/digest"))
    }

    suspend fun getSetupProgress(
        token: String,
    ): NetworkResult<ApiResponse<AdminSetupProgress>> = safeApiCall {
        client.get(getUrl("api/admin/setup"))
    }

    suspend fun updateSetupStep(
        token: String,
        stepKey: String,
        status: String,
    ): NetworkResult<ApiResponse<AdminSetupProgress>> = safeApiCall {
        client.put(getUrl("api/admin/setup/$stepKey")) {
            contentType(ContentType.Application.Json)
            setBody(UpdateAdminSetupStepRequest(status))
        }
    }

    // ── Agentic Syllabus — pace monitoring (admin) ───────────────────────────

    /** All pace snapshots for the school (optionally filtered by class/section). */
    suspend fun getPaceSnapshots(
        token: String,
        classId: String? = null,
        section: String? = null,
    ): NetworkResult<PaceSnapshotsResponse> = safeApiCall {
        client.get(getUrl("api/v1/school/syllabus-pace/snapshots")) {
            classId?.takeIf { it.isNotBlank() }?.let { parameter("classId", it) }
            section?.takeIf { it.isNotBlank() }?.let { parameter("section", it) }
        }
    }

    /** Active pace alerts (unresolved). */
    suspend fun getPaceAlerts(
        token: String,
    ): NetworkResult<PaceAlertsResponse> = safeApiCall {
        client.get(getUrl("api/v1/school/syllabus-pace/alerts"))
    }

    /** Resolve (dismiss) a pace alert. */
    suspend fun resolvePaceAlert(
        token: String,
        alertId: String,
    ): NetworkResult<PaceAlertResolveResponse> = safeApiCall {
        client.post(getUrl("api/v1/school/syllabus-pace/alerts/$alertId/resolve"))
    }

    /** Coverage overview (per class/section). */
    suspend fun getPaceCoverage(
        token: String,
        classId: String? = null,
        section: String? = null,
    ): NetworkResult<PaceSnapshotsResponse> = safeApiCall {
        client.get(getUrl("api/v1/school/syllabus-pace/coverage")) {
            classId?.takeIf { it.isNotBlank() }?.let { parameter("classId", it) }
            section?.takeIf { it.isNotBlank() }?.let { parameter("section", it) }
        }
    }

    /** Manually recalculate pace for all assignments in the school. */
    suspend fun recalculatePace(
        token: String,
    ): NetworkResult<PaceSnapshotsResponse> = safeApiCall {
        client.post(getUrl("api/v1/school/syllabus-pace/recalculate"))
    }
}
