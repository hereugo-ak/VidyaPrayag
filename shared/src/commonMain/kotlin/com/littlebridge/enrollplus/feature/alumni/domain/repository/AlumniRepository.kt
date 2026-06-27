package com.littlebridge.enrollplus.feature.alumni.domain.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.alumni.domain.model.*

interface AlumniRepository {

    // Admin: CRUD
    suspend fun listAlumni(token: String, year: Int? = null, profession: String? = null, city: String? = null, company: String? = null, industry: String? = null, q: String? = null, page: Int = 1, limit: Int = 20): NetworkResult<ApiResponse<AlumniListResponse>>
    suspend fun createAlumni(token: String, request: CreateAlumniRequest): NetworkResult<ApiResponse<Alumni>>
    suspend fun getAlumni(token: String, alumniId: String): NetworkResult<ApiResponse<Alumni>>
    suspend fun updateAlumni(token: String, alumniId: String, request: UpdateAlumniRequest): NetworkResult<ApiResponse<Alumni>>
    suspend fun deactivateAlumni(token: String, alumniId: String): NetworkResult<ApiResponse<Unit>>
    suspend fun graduateStudents(token: String, request: GraduateStudentsRequest): NetworkResult<ApiResponse<List<Alumni>>>
    suspend fun bulkImport(token: String, rows: List<CreateAlumniRequest>): NetworkResult<ApiResponse<BulkImportResult>>

    // Admin: Verification
    suspend fun listPendingVerifications(token: String): NetworkResult<ApiResponse<List<Alumni>>>
    suspend fun verifyAlumni(token: String, alumniId: String, action: String): NetworkResult<ApiResponse<Alumni>>

    // Admin: Featured
    suspend fun toggleFeatured(token: String, alumniId: String): NetworkResult<ApiResponse<Alumni>>

    // Admin: Campaigns
    suspend fun listCampaigns(token: String): NetworkResult<ApiResponse<List<AlumniDonationCampaign>>>
    suspend fun createCampaign(token: String, request: CreateCampaignRequest): NetworkResult<ApiResponse<AlumniDonationCampaign>>
    suspend fun getCampaign(token: String, campaignId: String): NetworkResult<ApiResponse<AlumniDonationCampaign>>
    suspend fun updateCampaign(token: String, campaignId: String, status: String): NetworkResult<ApiResponse<AlumniDonationCampaign>>

    // Admin: Donations
    suspend fun listDonations(token: String, campaignId: String? = null, alumniId: String? = null): NetworkResult<ApiResponse<List<AlumniDonation>>>
    suspend fun getAlumniDonations(token: String, alumniId: String): NetworkResult<ApiResponse<List<AlumniDonation>>>
    suspend fun createDonation(token: String, request: CreateDonationRequest): NetworkResult<ApiResponse<AlumniDonation>>

    // Admin: Mentorship
    suspend fun listMentorships(token: String): NetworkResult<ApiResponse<List<AlumniMentorship>>>
    suspend fun createMentorship(token: String, request: CreateMentorshipRequest): NetworkResult<ApiResponse<AlumniMentorship>>
    suspend fun endMentorship(token: String, mentorshipId: String): NetworkResult<ApiResponse<Unit>>
    suspend fun listMentorshipRequests(token: String): NetworkResult<ApiResponse<List<AlumniMentorshipRequest>>>
    suspend fun adminRespondToMentorshipRequest(token: String, requestId: String, action: String): NetworkResult<ApiResponse<AlumniMentorshipRequest>>

    // Admin: Mentorship Settings
    suspend fun getMentorshipSettings(token: String): NetworkResult<ApiResponse<MentorshipSettings>>
    suspend fun updateMentorshipSettings(token: String, request: MentorshipSettings): NetworkResult<ApiResponse<MentorshipSettings>>

    // Admin: Analytics
    suspend fun getAnalyticsOverview(token: String): NetworkResult<ApiResponse<AlumniAnalytics>>
    suspend fun getEngagementMetrics(token: String): NetworkResult<ApiResponse<EngagementMetrics>>
    suspend fun getDonationAnalytics(token: String): NetworkResult<ApiResponse<DonationAnalytics>>
    suspend fun getCareerAnalytics(token: String): NetworkResult<ApiResponse<CareerAnalytics>>

    // Alumni self-service
    suspend fun getAlumniProfile(token: String): NetworkResult<ApiResponse<Alumni>>
    suspend fun updateAlumniProfile(token: String, request: UpdateAlumniRequest): NetworkResult<ApiResponse<Alumni>>
    suspend fun updatePrivacy(token: String, request: AlumniPrivacy): NetworkResult<ApiResponse<Alumni>>
    suspend fun volunteerAsMentor(token: String, expertise: String): NetworkResult<ApiResponse<Alumni>>
    suspend fun getMentorshipRequestsForAlumni(token: String): NetworkResult<ApiResponse<List<AlumniMentorshipRequest>>>
    suspend fun respondToMentorshipRequest(token: String, requestId: String, action: String): NetworkResult<ApiResponse<AlumniMentorshipRequest>>
    suspend fun getOwnMentorships(token: String): NetworkResult<ApiResponse<List<AlumniMentorship>>>
    suspend fun getCareerHistory(token: String): NetworkResult<ApiResponse<List<CareerHistory>>>
    suspend fun addCareerHistory(token: String, request: CreateCareerHistoryRequest): NetworkResult<ApiResponse<CareerHistory>>
    suspend fun updateCareerHistory(token: String, entryId: String, request: CreateCareerHistoryRequest): NetworkResult<ApiResponse<CareerHistory>>
    suspend fun getOwnDonations(token: String): NetworkResult<ApiResponse<List<AlumniDonation>>>
    suspend fun getActiveCampaigns(token: String): NetworkResult<ApiResponse<List<AlumniDonationCampaign>>>
    suspend fun searchDirectory(token: String, year: Int? = null, profession: String? = null, city: String? = null, q: String? = null, page: Int = 1, limit: Int = 20): NetworkResult<ApiResponse<AlumniListResponse>>

    // Public
    suspend fun registerAlumni(request: AlumniRegisterRequest): NetworkResult<ApiResponse<RegistrationResult>>
    suspend fun searchSchools(query: String): NetworkResult<ApiResponse<List<SchoolSearchResult>>>
}
