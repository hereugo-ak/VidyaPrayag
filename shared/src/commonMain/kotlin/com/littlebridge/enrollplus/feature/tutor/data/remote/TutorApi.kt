package com.littlebridge.enrollplus.feature.tutor.data.remote

import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.tutor.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TutorApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Doubt (POST /tutor/doubt) ─────────────────────────────────────

    suspend fun askDoubt(token: String, request: DoubtRequest): NetworkResult<DoubtResponse> {
        return safeApiCall {
            client.post(getUrl("api/v1/tutor/doubt")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
        }
    }

    // ── Learner Bundle (GET /tutor/learner-bundle/{childId}/{subjectId}) ──

    suspend fun getLearnerBundle(
        token: String,
        childId: String,
        subjectId: String,
    ): NetworkResult<LearnerBundleResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/tutor/learner-bundle/$childId/$subjectId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    // ── Practice Grade (POST /tutor/practice/grade) ───────────────────

    suspend fun gradePractice(
        token: String,
        request: PracticeGradeRequest,
    ): NetworkResult<PracticeGradeResponse> {
        return safeApiCall {
            client.post(getUrl("api/v1/tutor/practice/grade")) {
                header(HttpHeaders.Authorization, "Bearer $token")
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(request))
            }
        }
    }

    // ── Adaptive Plan (GET /tutor/plan/{childId}) ─────────────────────

    suspend fun getPlan(
        token: String,
        childId: String,
        subjectId: String? = null,
    ): NetworkResult<PlanResponse> {
        val url = if (subjectId != null) {
            getUrl("api/v1/tutor/plan/$childId?subjectId=$subjectId")
        } else {
            getUrl("api/v1/tutor/plan/$childId")
        }
        return safeApiCall {
            client.get(url) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    // ── Teacher Heatmap ───────────────────────────────────────────────

    suspend fun getTeacherScope(token: String): NetworkResult<TeacherScopeResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/tutor/heatmap/scope")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    suspend fun getHeatmap(
        token: String,
        classId: String,
        subjectId: String,
    ): NetworkResult<HeatmapResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/tutor/heatmap/$classId/$subjectId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    // ── Parent Progress ───────────────────────────────────────────────

    suspend fun getProgressCard(
        token: String,
        childId: String,
        subjectId: String,
    ): NetworkResult<ProgressCardResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/tutor/progress/$childId/$subjectId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    // ── Efficacy ──────────────────────────────────────────────────────

    suspend fun getEfficacy(
        token: String,
        childId: String,
        subjectId: String,
    ): NetworkResult<EfficacyResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/tutor/efficacy/$childId/$subjectId")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    // ── Module Status ─────────────────────────────────────────────────

    suspend fun getModuleStatus(token: String): NetworkResult<ModuleStatusResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/tutor/modules")) {
                header(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }
}
