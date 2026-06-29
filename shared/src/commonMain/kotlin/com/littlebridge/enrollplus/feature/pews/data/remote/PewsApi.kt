/*
 * File: PewsApi.kt
 * Module: feature.pews.data.remote
 *
 * Network client for the Predictive Early Warning System. Bearer token is
 * attached by the shared HttpClient Auth plugin (the `token` arg is kept for
 * call-site symmetry with the rest of the data layer).
 *
 * Mirrors server feature.pews.PewsRouting.kt.
 */
package com.littlebridge.enrollplus.feature.pews.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsCohortDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsConfigDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsEffectivenessDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsEffectivenessTrendDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsInterventionDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsJobStatusDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsParentNudgeDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsRunResultDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDetailDto
import com.littlebridge.enrollplus.feature.pews.domain.model.PewsStudentDto
import com.littlebridge.enrollplus.feature.pews.domain.model.ParentDraftDto
import com.littlebridge.enrollplus.feature.pews.domain.model.SendParentMessageDto
import com.littlebridge.enrollplus.feature.pews.domain.model.UpdateInterventionRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class PewsApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ───────────────────────────── SCHOOL ADMIN ─────────────────────────────

    suspend fun getCohort(
        token: String,
        minLevel: String? = null,
    ): NetworkResult<ApiResponse<PewsCohortDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/pews/cohort")) {
            if (!minLevel.isNullOrBlank()) parameter("min_level", minLevel)
        }
    }

    suspend fun getStudent(
        token: String,
        studentCode: String,
    ): NetworkResult<ApiResponse<PewsStudentDetailDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/pews/student/$studentCode"))
    }

    suspend fun getInterventions(
        token: String,
        status: String? = null,
    ): NetworkResult<ApiResponse<List<PewsInterventionDto>>> = safeApiCall {
        client.get(getUrl("api/v1/school/pews/interventions")) {
            if (!status.isNullOrBlank()) parameter("status", status)
        }
    }

    suspend fun updateIntervention(
        token: String,
        interventionId: String,
        request: UpdateInterventionRequest,
    ): NetworkResult<ApiResponse<PewsInterventionDto>> = safeApiCall {
        client.patch(getUrl("api/v1/school/pews/interventions/$interventionId")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getEffectiveness(
        token: String,
    ): NetworkResult<ApiResponse<PewsEffectivenessDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/pews/effectiveness"))
    }

    suspend fun getConfig(
        token: String,
    ): NetworkResult<ApiResponse<PewsConfigDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/pews/config"))
    }

    suspend fun updateConfig(
        token: String,
        config: PewsConfigDto,
    ): NetworkResult<ApiResponse<PewsConfigDto>> = safeApiCall {
        client.put(getUrl("api/v1/school/pews/config")) {
            contentType(ContentType.Application.Json)
            setBody(config)
        }
    }

    suspend fun runNow(
        token: String,
    ): NetworkResult<ApiResponse<PewsRunResultDto>> = safeApiCall {
        client.post(getUrl("api/v1/school/pews/run"))
    }

    suspend fun getJobStatus(
        token: String,
        jobId: String,
    ): NetworkResult<ApiResponse<PewsJobStatusDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/pews/run/$jobId"))
    }

    suspend fun getTrend(
        token: String,
        days: Int = 30,
    ): NetworkResult<ApiResponse<PewsEffectivenessTrendDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/pews/trend")) {
            parameter("days", days)
        }
    }

    // ─────────────────────────────── TEACHER ────────────────────────────────

    suspend fun getTeacherStudents(
        token: String,
    ): NetworkResult<ApiResponse<List<PewsStudentDto>>> = safeApiCall {
        client.get(getUrl("api/v1/teacher/pews/students"))
    }

    suspend fun getTeacherInterventions(
        token: String,
        status: String? = null,
    ): NetworkResult<ApiResponse<List<PewsInterventionDto>>> = safeApiCall {
        client.get(getUrl("api/v1/teacher/pews/interventions")) {
            if (!status.isNullOrBlank()) parameter("status", status)
        }
    }

    suspend fun updateTeacherIntervention(
        token: String,
        interventionId: String,
        request: UpdateInterventionRequest,
    ): NetworkResult<ApiResponse<Map<String, Boolean>>> = safeApiCall {
        client.patch(getUrl("api/v1/teacher/pews/interventions/$interventionId")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun generateParentDraft(
        token: String,
        interventionId: String,
        lang: String = "en",
    ): NetworkResult<ApiResponse<ParentDraftDto>> = safeApiCall {
        client.post(getUrl("api/v1/teacher/pews/interventions/$interventionId/draft-message")) {
            parameter("lang", lang)
        }
    }

    suspend fun sendParentMessage(
        token: String,
        interventionId: String,
    ): NetworkResult<ApiResponse<SendParentMessageDto>> = safeApiCall {
        client.post(getUrl("api/v1/teacher/pews/interventions/$interventionId/send-parent-message"))
    }

    // ─────────────────────────────── PARENT ─────────────────────────────────

    suspend fun getParentNudge(
        token: String,
        childId: String,
    ): NetworkResult<ApiResponse<PewsParentNudgeDto>> = safeApiCall {
        client.get(getUrl("api/v1/parent/pews/$childId"))
    }
}
