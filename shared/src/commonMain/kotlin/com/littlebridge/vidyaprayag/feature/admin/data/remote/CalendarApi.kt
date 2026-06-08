/*
 * File: CalendarApi.kt
 * Module: feature.admin.data.remote
 *
 * Network client for academic calendar endpoint.
 * Server route: GET /api/v1/school/calendar
 */
package com.littlebridge.vidyaprayag.feature.admin.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.admin.domain.model.CalendarResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get

class CalendarApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun getCalendar(
        token: String,
        date: String? = null,
        viewType: String = "month"
    ): NetworkResult<ApiResponse<CalendarResponse>> = safeApiCall {
        val params = buildList {
            date?.let { add("date=$it") }
            add("view_type=$viewType")
        }.joinToString("&")
        val url = if (params.isBlank()) getUrl("api/v1/school/calendar")
                  else getUrl("api/v1/school/calendar?$params")
        client.get(url)
    }
}
