package com.littlebridge.enrollplus.feature.alumni.data.remote

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.network.safeApiCall
import com.littlebridge.enrollplus.feature.alumni.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*

class AlumniApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Admin: Alumni CRUD ──────────────────────────────────────

    suspend fun listAlumni(
        token: String,
        year: Int? = null,
        profession: String? = null,
        city: String? = null,
        company: String? = null,
        industry: String? = null,
        q: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): NetworkResult<ApiResponse<AlumniListResponse>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni")) {
            bearerAuth(token)
            year?.let { parameter("year", it) }
            profession?.let { parameter("profession", it) }
            city?.let { parameter("city", it) }
            company?.let { parameter("company", it) }
            industry?.let { parameter("industry", it) }
            q?.let { parameter("q", it) }
            parameter("page", page)
            parameter("limit", limit)
        }
    }

    suspend fun createAlumni(token: String, request: CreateAlumniRequest): NetworkResult<ApiResponse<Alumni>> = safeApiCall {
        client.post(getUrl("api/v1/school/alumni")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getAlumni(token: String, alumniId: String): NetworkResult<ApiResponse<Alumni>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/$alumniId")) {
            bearerAuth(token)
        }
    }

    suspend fun updateAlumni(token: String, alumniId: String, request: UpdateAlumniRequest): NetworkResult<ApiResponse<Alumni>> = safeApiCall {
        client.patch(getUrl("api/v1/school/alumni/$alumniId")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun deactivateAlumni(token: String, alumniId: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.patch(getUrl("api/v1/school/alumni/$alumniId/deactivate")) {
            bearerAuth(token)
        }
    }

    suspend fun graduateStudents(token: String, request: GraduateStudentsRequest): NetworkResult<ApiResponse<List<Alumni>>> = safeApiCall {
        client.post(getUrl("api/v1/school/alumni/graduate")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun bulkImport(token: String, rows: List<CreateAlumniRequest>): NetworkResult<ApiResponse<BulkImportResult>> = safeApiCall {
        client.post(getUrl("api/v1/school/alumni/import")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(rows)
        }
    }

    // ── Admin: Verification ─────────────────────────────────────

    suspend fun listPendingVerifications(token: String): NetworkResult<ApiResponse<List<Alumni>>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/pending")) {
            bearerAuth(token)
        }
    }

    suspend fun verifyAlumni(token: String, alumniId: String, action: String): NetworkResult<ApiResponse<Alumni>> = safeApiCall {
        client.patch(getUrl("api/v1/school/alumni/$alumniId/verify")) {
            bearerAuth(token)
            parameter("action", action)
        }
    }

    // ── Admin: Featured ─────────────────────────────────────────

    suspend fun toggleFeatured(token: String, alumniId: String): NetworkResult<ApiResponse<Alumni>> = safeApiCall {
        client.patch(getUrl("api/v1/school/alumni/$alumniId/feature")) {
            bearerAuth(token)
        }
    }

    // ── Admin: Campaigns ────────────────────────────────────────

    suspend fun listCampaigns(token: String): NetworkResult<ApiResponse<List<AlumniDonationCampaign>>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/campaigns")) {
            bearerAuth(token)
        }
    }

    suspend fun createCampaign(token: String, request: CreateCampaignRequest): NetworkResult<ApiResponse<AlumniDonationCampaign>> = safeApiCall {
        client.post(getUrl("api/v1/school/alumni/campaigns")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getCampaign(token: String, campaignId: String): NetworkResult<ApiResponse<AlumniDonationCampaign>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/campaigns/$campaignId")) {
            bearerAuth(token)
        }
    }

    suspend fun updateCampaign(token: String, campaignId: String, status: String): NetworkResult<ApiResponse<AlumniDonationCampaign>> = safeApiCall {
        client.patch(getUrl("api/v1/school/alumni/campaigns/$campaignId")) {
            bearerAuth(token)
            parameter("status", status)
        }
    }

    // ── Admin: Donations ────────────────────────────────────────

    suspend fun listDonations(token: String, campaignId: String? = null, alumniId: String? = null): NetworkResult<ApiResponse<List<AlumniDonation>>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/donations")) {
            bearerAuth(token)
            campaignId?.let { parameter("campaign_id", it) }
            alumniId?.let { parameter("alumni_id", it) }
        }
    }

    suspend fun getAlumniDonations(token: String, alumniId: String): NetworkResult<ApiResponse<List<AlumniDonation>>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/$alumniId/donations")) {
            bearerAuth(token)
        }
    }

    suspend fun createDonation(token: String, request: CreateDonationRequest): NetworkResult<ApiResponse<AlumniDonation>> = safeApiCall {
        client.post(getUrl("api/v1/school/alumni/donations")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Admin: Mentorship ───────────────────────────────────────

    suspend fun listMentorships(token: String): NetworkResult<ApiResponse<List<AlumniMentorship>>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/mentorships")) {
            bearerAuth(token)
        }
    }

    suspend fun createMentorship(token: String, request: CreateMentorshipRequest): NetworkResult<ApiResponse<AlumniMentorship>> = safeApiCall {
        client.post(getUrl("api/v1/school/alumni/mentorships")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun endMentorship(token: String, mentorshipId: String): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.patch(getUrl("api/v1/school/alumni/mentorships/$mentorshipId")) {
            bearerAuth(token)
        }
    }

    suspend fun listMentorshipRequests(token: String): NetworkResult<ApiResponse<List<AlumniMentorshipRequest>>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/mentorship-requests")) {
            bearerAuth(token)
        }
    }

    suspend fun adminRespondToMentorshipRequest(token: String, requestId: String, action: String): NetworkResult<ApiResponse<AlumniMentorshipRequest>> = safeApiCall {
        client.submitForm(
            url = getUrl("api/v1/school/alumni/mentorship-requests/$requestId"),
            formParameters = parameters { append("action", action) }
        ) {
            bearerAuth(token)
            method = HttpMethod.Patch
        }
    }

    // ── Admin: Mentorship Settings ──────────────────────────────

    suspend fun getMentorshipSettings(token: String): NetworkResult<ApiResponse<MentorshipSettings>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/mentorship/settings")) {
            bearerAuth(token)
        }
    }

    suspend fun updateMentorshipSettings(token: String, request: MentorshipSettings): NetworkResult<ApiResponse<MentorshipSettings>> = safeApiCall {
        client.put(getUrl("api/v1/school/alumni/mentorship/settings")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // ── Admin: Analytics ────────────────────────────────────────

    suspend fun getAnalyticsOverview(token: String): NetworkResult<ApiResponse<AlumniAnalytics>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/analytics/overview")) {
            bearerAuth(token)
        }
    }

    suspend fun getEngagementMetrics(token: String): NetworkResult<ApiResponse<EngagementMetrics>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/analytics/engagement")) {
            bearerAuth(token)
        }
    }

    suspend fun getDonationAnalytics(token: String): NetworkResult<ApiResponse<DonationAnalytics>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/analytics/donations")) {
            bearerAuth(token)
        }
    }

    suspend fun getCareerAnalytics(token: String): NetworkResult<ApiResponse<CareerAnalytics>> = safeApiCall {
        client.get(getUrl("api/v1/school/alumni/analytics/career")) {
            bearerAuth(token)
        }
    }

    // ── Alumni Self-Service ─────────────────────────────────────

    suspend fun getAlumniProfile(token: String): NetworkResult<ApiResponse<Alumni>> = safeApiCall {
        client.get(getUrl("api/v1/alumni/profile")) {
            bearerAuth(token)
        }
    }

    suspend fun updateAlumniProfile(token: String, request: UpdateAlumniRequest): NetworkResult<ApiResponse<Alumni>> = safeApiCall {
        client.patch(getUrl("api/v1/alumni/profile")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun updatePrivacy(token: String, request: AlumniPrivacy): NetworkResult<ApiResponse<Alumni>> = safeApiCall {
        client.patch(getUrl("api/v1/alumni/privacy")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun volunteerAsMentor(token: String, expertise: String): NetworkResult<ApiResponse<Alumni>> = safeApiCall {
        client.submitForm(
            url = getUrl("api/v1/alumni/mentor-volunteer"),
            formParameters = parametersOf("expertise" to listOf(expertise))
        ) {
            bearerAuth(token)
        }
    }

    suspend fun getMentorshipRequestsForAlumni(token: String): NetworkResult<ApiResponse<List<AlumniMentorshipRequest>>> = safeApiCall {
        client.get(getUrl("api/v1/alumni/mentorship-requests")) {
            bearerAuth(token)
        }
    }

    suspend fun respondToMentorshipRequest(token: String, requestId: String, action: String): NetworkResult<ApiResponse<AlumniMentorshipRequest>> = safeApiCall {
        client.submitForm(
            url = getUrl("api/v1/alumni/mentorship-requests/$requestId"),
            formParameters = parametersOf("action" to listOf(action))
        ) {
            bearerAuth(token)
            method = HttpMethod.Patch
        }
    }

    suspend fun getOwnMentorships(token: String): NetworkResult<ApiResponse<List<AlumniMentorship>>> = safeApiCall {
        client.get(getUrl("api/v1/alumni/mentorships")) {
            bearerAuth(token)
        }
    }

    suspend fun getCareerHistory(token: String): NetworkResult<ApiResponse<List<CareerHistory>>> = safeApiCall {
        client.get(getUrl("api/v1/alumni/career-history")) {
            bearerAuth(token)
        }
    }

    suspend fun addCareerHistory(token: String, request: CreateCareerHistoryRequest): NetworkResult<ApiResponse<CareerHistory>> = safeApiCall {
        client.post(getUrl("api/v1/alumni/career-history")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun updateCareerHistory(token: String, entryId: String, request: CreateCareerHistoryRequest): NetworkResult<ApiResponse<CareerHistory>> = safeApiCall {
        client.patch(getUrl("api/v1/alumni/career-history/$entryId")) {
            bearerAuth(token)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun getOwnDonations(token: String): NetworkResult<ApiResponse<List<AlumniDonation>>> = safeApiCall {
        client.get(getUrl("api/v1/alumni/donations")) {
            bearerAuth(token)
        }
    }

    suspend fun getActiveCampaigns(token: String): NetworkResult<ApiResponse<List<AlumniDonationCampaign>>> = safeApiCall {
        client.get(getUrl("api/v1/alumni/campaigns")) {
            bearerAuth(token)
        }
    }

    suspend fun searchDirectory(
        token: String,
        year: Int? = null,
        profession: String? = null,
        city: String? = null,
        q: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): NetworkResult<ApiResponse<AlumniListResponse>> = safeApiCall {
        client.get(getUrl("api/v1/alumni/directory")) {
            bearerAuth(token)
            year?.let { parameter("year", it) }
            profession?.let { parameter("profession", it) }
            city?.let { parameter("city", it) }
            q?.let { parameter("q", it) }
            parameter("page", page)
            parameter("limit", limit)
        }
    }

    // ── Public: Registration + School Search ────────────────────

    suspend fun registerAlumni(request: AlumniRegisterRequest): NetworkResult<ApiResponse<RegistrationResult>> = safeApiCall {
        client.post(getUrl("api/v1/alumni/register")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun searchSchools(query: String): NetworkResult<ApiResponse<List<SchoolSearchResult>>> = safeApiCall {
        client.get(getUrl("api/v1/alumni/schools/search")) {
            parameter("q", query)
        }
    }
}
