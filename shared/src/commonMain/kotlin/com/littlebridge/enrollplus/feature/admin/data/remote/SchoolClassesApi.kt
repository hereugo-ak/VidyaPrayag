package com.littlebridge.enrollplus.feature.admin.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateSchoolSubjectRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolClassListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectDto
import com.littlebridge.enrollplus.feature.admin.domain.model.SchoolSubjectListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableDto
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolClassRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.UpdateSchoolSubjectRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SchoolClassesApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Classes ────────────────────────────────────────────────────────────────

    suspend fun listClasses(token: String): NetworkResult<ApiResponse<SchoolClassListResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/classes")) { bearerAuth(token) }
    }

    suspend fun createClass(token: String, req: CreateSchoolClassRequest): NetworkResult<ApiResponse<SchoolClassDto>> = safeApiCall {
        client.post(getUrl("api/v1/school/classes")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
    }

    suspend fun updateClass(token: String, id: String, req: UpdateSchoolClassRequest): NetworkResult<ApiResponse<SchoolClassDto>> = safeApiCall {
        client.put(getUrl("api/v1/school/classes/$id")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
    }

    suspend fun deleteClass(token: String, id: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/classes/$id")) { bearerAuth(token) }
    }

    // ── Subjects ───────────────────────────────────────────────────────────────

    suspend fun listSubjects(token: String, classId: String): NetworkResult<ApiResponse<SchoolSubjectListResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/classes/$classId/subjects")) { bearerAuth(token) }
    }

    suspend fun createSubject(token: String, classId: String, req: CreateSchoolSubjectRequest): NetworkResult<ApiResponse<SchoolSubjectDto>> = safeApiCall {
        client.post(getUrl("api/v1/school/classes/$classId/subjects")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
    }

    suspend fun updateSubject(token: String, id: String, req: UpdateSchoolSubjectRequest): NetworkResult<ApiResponse<SchoolSubjectDto>> = safeApiCall {
        client.put(getUrl("api/v1/school/subjects/$id")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(req)
        }
    }

    suspend fun deleteSubject(token: String, id: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.delete(getUrl("api/v1/school/subjects/$id")) { bearerAuth(token) }
    }

    // ── Timetable (read-only) ──────────────────────────────────────────────────

    suspend fun getTimetable(token: String, classFilter: String? = null): NetworkResult<ApiResponse<TimetableDto>> = safeApiCall {
        client.get(getUrl("api/v1/school/timetable")) {
            bearerAuth(token)
            if (!classFilter.isNullOrBlank()) parameter("class", classFilter)
        }
    }
}
