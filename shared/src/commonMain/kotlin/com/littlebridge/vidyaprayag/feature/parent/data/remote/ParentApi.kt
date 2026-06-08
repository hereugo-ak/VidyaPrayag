package com.littlebridge.vidyaprayag.feature.parent.data.remote

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.parent.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType

class ParentApi(
    private val client: HttpClient,
    private val baseUrl: String
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    suspend fun getDashboard(token: String): NetworkResult<ParentDashboardResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/dashboard"))
        }
    }

    suspend fun getTrackProgress(token: String): NetworkResult<TrackProgressResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/track-progress"))
        }
    }

    suspend fun getFees(token: String): NetworkResult<FeeResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/fees"))
        }
    }

    suspend fun getScholarships(token: String): NetworkResult<ScholarshipsResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/scholarships"))
        }
    }

    suspend fun getAnnouncements(token: String): NetworkResult<ParentAnnouncementsResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/announcements"))
        }
    }

    // RA-42/46: point at the role-aware spine endpoint so admin/teacher tokens
    // get their OWN notifications (the old /parent/notifications returned empty
    // for non-parents). Response shape is identical (notifications + unread_count).
    suspend fun getNotifications(token: String): NetworkResult<ParentNotificationsResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/notifications"))
        }
    }

    /** RA-46: persist a single mark-read on the server. */
    suspend fun markNotificationRead(token: String, id: String): NetworkResult<Unit> {
        return safeApiCall {
            client.patch(getUrl("api/v1/notifications/$id/read"))
        }
    }

    /** RA-46: persist mark-all-read on the server. */
    suspend fun markAllNotificationsRead(token: String): NetworkResult<Unit> {
        return safeApiCall {
            client.post(getUrl("api/v1/notifications/read-all"))
        }
    }

    suspend fun searchSchools(token: String, query: String): NetworkResult<SchoolSearchResponse> {
        return safeApiCall {
            client.get(getUrl("api/v1/parent/schools/search")) {
                url { parameters.append("q", query) }
            }
        }
    }

    suspend fun linkChild(token: String, request: LinkChildRequest): NetworkResult<LinkChildResponse> {
        return safeApiCall {
            client.post(getUrl("api/v1/parent/link-child")) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }
}
