package com.littlebridge.vidyaprayag.feature.teacher.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.*
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class TeacherApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun getHome(token: String): NetworkResult<ApiResponse<TeacherHomeResponse>> = safeApiCall {
        client.get(getUrl("api/v1/teacher/home")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getTimetable(token: String, day: String): NetworkResult<ApiResponse<TeacherTimetableResponse>> = safeApiCall {
        client.get(getUrl("api/v1/teacher/timetable?day=$day")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getClasses(token: String): NetworkResult<ApiResponse<TeacherClassesResponse>> = safeApiCall {
        client.get(getUrl("api/v1/teacher/classes")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getTests(token: String, className: String? = null, subject: String? = null): NetworkResult<ApiResponse<TeacherTestsResponse>> = safeApiCall {
        val params = buildList {
            className?.let { add("class=$it") }
            subject?.let { add("subject=$it") }
        }.joinToString("&")
        val url = if (params.isBlank()) getUrl("api/v1/teacher/tests")
                  else getUrl("api/v1/teacher/tests?$params")
        client.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun createTest(token: String, request: CreateTestRequest): NetworkResult<ApiResponse<CreateTestResponse>> = safeApiCall {
        client.post(getUrl("api/v1/teacher/tests")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getHomework(token: String, className: String? = null): NetworkResult<ApiResponse<TeacherHomeworkResponse>> = safeApiCall {
        val url = if (className.isNullOrBlank()) getUrl("api/v1/teacher/homework")
                  else getUrl("api/v1/teacher/homework?class=$className")
        client.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun createHomework(token: String, request: CreateHomeworkRequest): NetworkResult<ApiResponse<CreateHomeworkResponse>> = safeApiCall {
        client.post(getUrl("api/v1/teacher/homework")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getCurriculum(token: String, subject: String? = null): NetworkResult<ApiResponse<TeacherCurriculumResponse>> = safeApiCall {
        val url = if (subject.isNullOrBlank()) getUrl("api/v1/teacher/curriculum")
                  else getUrl("api/v1/teacher/curriculum?subject=$subject")
        client.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun getProfile(token: String): NetworkResult<ApiResponse<TeacherProfileResponse>> = safeApiCall {
        client.get(getUrl("api/v1/teacher/profile")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
