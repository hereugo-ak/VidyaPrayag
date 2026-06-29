/*
 * File: ReportCardApi.kt
 * Module: feature.reportcard.data.remote
 *
 * Network client for AI Report Card 2.0 endpoints.
 * Bearer token is attached by the shared HttpClient Auth plugin.
 *
 * Mirrors server feature.reportcard.assemble.AssembleRouting.kt +
 * feature.reportcard.learn.LearnRouting.kt +
 * feature.reportcard.ecosystem.EcosystemRouting.kt.
 */
package com.littlebridge.enrollplus.feature.reportcard.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.reportcard.domain.model.ReportCardModels
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ReportCardApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Teacher: batch generation ────────────────────────────────────────

    suspend fun generateBatch(
        token: String,
        request: ReportCardModels.GenerateRequest,
    ): NetworkResult<ApiResponse<ReportCardModels.BatchResult>> = safeApiCall {
        client.post(getUrl("api/v1/report-card/generate")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Job status ───────────────────────────────────────────────────────

    suspend fun getJobStatus(
        token: String,
        jobId: String,
    ): NetworkResult<ApiResponse<ReportCardModels.JobStatus>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/jobs/$jobId"))
    }

    // ── Teacher: review queue ────────────────────────────────────────────

    suspend fun getReviewQueue(
        token: String,
        className: String,
        section: String,
        term: String,
        academicYearId: String? = null,
    ): NetworkResult<ApiResponse<List<ReportCardModels.DraftDto>>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/review-queue")) {
            parameter("className", className)
            parameter("section", section)
            parameter("term", term)
            if (!academicYearId.isNullOrBlank()) parameter("academicYearId", academicYearId)
        }
    }

    // ── Teacher: get single draft ────────────────────────────────────────

    suspend fun getDraft(
        token: String,
        draftId: String,
    ): NetworkResult<ApiResponse<ReportCardModels.DraftDto>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/drafts/$draftId"))
    }

    // ── Teacher: edit draft ──────────────────────────────────────────────

    suspend fun editDraft(
        token: String,
        draftId: String,
        request: ReportCardModels.EditDraftRequest,
    ): NetworkResult<ApiResponse<Map<String, Boolean>>> = safeApiCall {
        client.put(getUrl("api/v1/report-card/drafts/$draftId/edit")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Teacher: approve draft ───────────────────────────────────────────

    suspend fun approveDraft(
        token: String,
        draftId: String,
    ): NetworkResult<ApiResponse<Map<String, Boolean>>> = safeApiCall {
        client.post(getUrl("api/v1/report-card/drafts/$draftId/approve"))
    }

    // ── Teacher: regenerate draft ────────────────────────────────────────

    suspend fun regenerateDraft(
        token: String,
        draftId: String,
    ): NetworkResult<ApiResponse<ReportCardModels.RegenerateResult>> = safeApiCall {
        client.post(getUrl("api/v1/report-card/drafts/$draftId/regenerate"))
    }

    // ── Teacher: bulk approve ────────────────────────────────────────────

    suspend fun bulkApprove(
        token: String,
        draftIds: List<String>,
    ): NetworkResult<ApiResponse<ReportCardModels.BulkApproveResult>> = safeApiCall {
        client.post(getUrl("api/v1/report-card/bulk-approve")) {
            contentType(ContentType.Application.Json)
            setBody(ReportCardModels.BulkApproveRequest(draftIds))
        }
    }

    // ── Admin: publish ───────────────────────────────────────────────────

    suspend fun publishClass(
        token: String,
        request: ReportCardModels.PublishRequest,
    ): NetworkResult<ApiResponse<ReportCardModels.PublishResult>> = safeApiCall {
        client.post(getUrl("api/v1/report-card/publish")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Admin: oversight ─────────────────────────────────────────────────

    suspend fun getOversight(
        token: String,
        term: String,
        academicYearId: String? = null,
    ): NetworkResult<ApiResponse<ReportCardModels.OversightSummary>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/oversight")) {
            parameter("term", term)
            if (!academicYearId.isNullOrBlank()) parameter("academicYearId", academicYearId)
        }
    }

    // ── Admin: term config ───────────────────────────────────────────────

    suspend fun getTermConfig(
        token: String,
    ): NetworkResult<ApiResponse<ReportCardModels.TermConfig>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/term-config"))
    }

    // ── Admin: learn flywheel ────────────────────────────────────────────

    suspend fun runFlywheel(
        token: String,
        currentTerm: String,
        previousTerm: String,
        academicYearId: String? = null,
    ): NetworkResult<ApiResponse<List<ReportCardModels.EffectivenessReport>>> = safeApiCall {
        client.post(getUrl("api/v1/report-card/learn/flywheel")) {
            parameter("currentTerm", currentTerm)
            parameter("previousTerm", previousTerm)
            if (!academicYearId.isNullOrBlank()) parameter("academicYearId", academicYearId)
        }
    }

    suspend fun getEffectiveness(
        token: String,
    ): NetworkResult<ApiResponse<List<ReportCardModels.EffectivenessReport>>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/learn/effectiveness"))
    }

    suspend fun getProjectionAccuracy(
        token: String,
        currentTerm: String,
        previousTerm: String,
        academicYearId: String? = null,
    ): NetworkResult<ApiResponse<ReportCardModels.ProjectionAccuracy>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/learn/projection-accuracy")) {
            parameter("currentTerm", currentTerm)
            parameter("previousTerm", previousTerm)
            if (!academicYearId.isNullOrBlank()) parameter("academicYearId", academicYearId)
        }
    }

    // ── Admin: cohort patterns ───────────────────────────────────────────

    suspend fun getPatterns(
        token: String,
        term: String,
        academicYearId: String? = null,
    ): NetworkResult<ApiResponse<ReportCardModels.CohortPatternReport>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/patterns")) {
            parameter("term", term)
            if (!academicYearId.isNullOrBlank()) parameter("academicYearId", academicYearId)
        }
    }

    // ── Parent: published reports ────────────────────────────────────────

    suspend fun getPublishedReports(
        token: String,
        childId: String,
        academicYearId: String? = null,
    ): NetworkResult<ApiResponse<List<ReportCardModels.ParentReport>>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/published")) {
            parameter("childId", childId)
            if (!academicYearId.isNullOrBlank()) parameter("academicYearId", academicYearId)
        }
    }

    // ── Parent: conference pack ──────────────────────────────────────────

    suspend fun getConferencePack(
        token: String,
        childId: String,
    ): NetworkResult<ApiResponse<ReportCardModels.ConferencePack>> = safeApiCall {
        client.get(getUrl("api/v1/report-card/conference-pack")) {
            parameter("childId", childId)
        }
    }
}
