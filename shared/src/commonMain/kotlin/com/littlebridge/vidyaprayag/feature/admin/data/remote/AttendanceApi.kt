/*
 * File: AttendanceApi.kt
 * Module: feature.admin.data.remote
 *
 * Network client for daily attendance endpoint.
 * Server route: GET /api/v1/school/attendance/daily
 */
package com.littlebridge.vidyaprayag.feature.admin.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.admin.domain.model.AttendanceResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders

class AttendanceApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    /**
     * @param type  "student" or "faculty"
     * @param grade Required when type == "student" (e.g. "Grade 10-A")
     * @param date  YYYY-MM-DD; defaults to today on the server
     */
    suspend fun getDailyAttendance(
        token: String,
        type: String = "student",
        grade: String? = null,
        date: String? = null
    ): NetworkResult<ApiResponse<AttendanceResponse>> = safeApiCall {
        val params = buildList {
            add("type=$type")
            grade?.let { add("grade=$it") }
            date?.let { add("date=$it") }
        }.joinToString("&")
        client.get(getUrl("api/v1/school/attendance/daily?$params")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}
