package com.littlebridge.vidyaprayag.feature.parent.data.remote

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.parent.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*

class ParentApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun getTrackProgress(token: String): NetworkResult<TrackProgressResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/track-progress")) {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun getFees(token: String): NetworkResult<FeeResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/fees")) {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun getScholarships(token: String): NetworkResult<ScholarshipsResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/scholarships")) {
                header("Authorization", "Bearer $token")
            }
        }
    }

    suspend fun getAnnouncements(token: String): NetworkResult<ParentAnnouncementsResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/announcements")) {
                header("Authorization", "Bearer $token")
            }
        }
    }
}
