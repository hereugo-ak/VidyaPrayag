/*
 * File: UserProfileApi.kt
 * Module: feature.admin.data.remote
 *
 * Network client for the institutional-profile / school-profile endpoints.
 * Server routes:
 *   GET  /api/v1/user/profile
 *   PUT  /api/v1/user/profile/philosophy
 *   PUT  /api/v1/user/profile/tour-videos
 *   PUT  /api/v1/user/profile/gallery
 *   PUT  /api/v1/user/profile/visibility
 */
package com.littlebridge.vidyaprayag.feature.admin.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.admin.domain.model.GalleryRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.GalleryUpdateResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.PhilosophyDetailsDto
import com.littlebridge.vidyaprayag.feature.admin.domain.model.TourVideosRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.UserProfileResponse
import com.littlebridge.vidyaprayag.feature.admin.domain.model.VisibilityRequest
import com.littlebridge.vidyaprayag.feature.admin.domain.model.VisibilityResponse
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType

class UserProfileApi(
    private val client: HttpClient,
    private val baseUrl: String
) {

    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun getProfile(
        token: String
    ): NetworkResult<ApiResponse<UserProfileResponse>> = safeApiCall {
        client.get(getUrl("api/v1/user/profile")) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
    }

    suspend fun updatePhilosophy(
        token: String,
        body: PhilosophyDetailsDto
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.put(getUrl("api/v1/user/profile/philosophy")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun updateTourVideos(
        token: String,
        body: TourVideosRequest
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.put(getUrl("api/v1/user/profile/tour-videos")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun updateGallery(
        token: String,
        body: GalleryRequest
    ): NetworkResult<ApiResponse<GalleryUpdateResponse>> = safeApiCall {
        client.put(getUrl("api/v1/user/profile/gallery")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }

    suspend fun updateVisibility(
        token: String,
        body: VisibilityRequest
    ): NetworkResult<ApiResponse<VisibilityResponse>> = safeApiCall {
        client.put(getUrl("api/v1/user/profile/visibility")) {
            header(HttpHeaders.Authorization, "Bearer $token")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
    }
}
