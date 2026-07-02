package com.littlebridge.enrollplus.feature.admin.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableImportOcrRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableImportResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableImportTextRequest
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class TimetableImportApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun importOcr(
        token: String,
        request: TimetableImportOcrRequest,
    ): NetworkResult<ApiResponse<TimetableImportResponse>> = safeApiCall {
        client.post(getUrl("api/v1/school/timetable/import-ocr")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun importText(
        token: String,
        request: TimetableImportTextRequest,
    ): NetworkResult<ApiResponse<TimetableImportResponse>> = safeApiCall {
        client.post(getUrl("api/v1/school/timetable/import-text")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
