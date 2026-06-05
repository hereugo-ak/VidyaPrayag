package com.littlebridge.vidyaprayag.feature.teacher.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * TeacherApi — Ktor client for the (new) `api/v1/teacher/*` routes.
 *
 * Mirrors [com.littlebridge.vidyaprayag.feature.parent.data.remote.ParentApi]: every call is wrapped
 * in [safeApiCall], authenticated with a bearer token, and hits the school base URL. Read endpoints
 * return typed response envelopes; write endpoints return `ApiResponse<Unit>` (server replies
 * `{ success, message }`).
 */
class TeacherApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Reads ───────────────────────────────────────────────────────────────

    suspend fun getHome(token: String): NetworkResult<TeacherHomeResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/home")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getClasses(token: String): NetworkResult<TeacherClassesResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/classes")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getAttendance(
        token: String,
        classId: String,
        date: String,
    ): NetworkResult<TeacherAttendanceResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/attendance")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("class_id", classId)
            parameter("date", date)
        }
    }

    suspend fun getMarks(
        token: String,
        classId: String,
        examId: String,
    ): NetworkResult<TeacherMarksResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/marks")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("class_id", classId)
            parameter("exam_id", examId)
        }
    }

    suspend fun getSyllabus(
        token: String,
        classId: String,
        subject: String,
    ): NetworkResult<TeacherSyllabusResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/syllabus")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            parameter("class_id", classId)
            parameter("subject", subject)
        }
    }

    suspend fun getHomework(token: String): NetworkResult<TeacherHomeworkResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/homework")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getProfile(token: String): NetworkResult<TeacherProfileResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/profile")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    // ── Writes ──────────────────────────────────────────────────────────────

    suspend fun submitAttendance(
        token: String,
        request: SubmitAttendanceRequest,
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.post(getUrl("api/v1/teacher/attendance")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun submitMarks(
        token: String,
        request: SubmitMarksRequest,
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.post(getUrl("api/v1/teacher/marks")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun updateSyllabus(
        token: String,
        request: UpdateSyllabusRequest,
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.patch(getUrl("api/v1/teacher/syllabus")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun createHomework(
        token: String,
        request: CreateHomeworkRequest,
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.post(getUrl("api/v1/teacher/homework")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
