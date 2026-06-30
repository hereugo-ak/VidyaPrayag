package com.littlebridge.enrollplus.feature.alumni.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.alumni.data.remote.AlumniApi
import com.littlebridge.enrollplus.feature.alumni.domain.model.*
import com.littlebridge.enrollplus.feature.alumni.domain.repository.AlumniRepository

class AlumniRepositoryImpl(
    private val api: AlumniApi,
) : AlumniRepository {

    override suspend fun listAlumni(token: String, year: Int?, profession: String?, city: String?, company: String?, industry: String?, q: String?, page: Int, limit: Int) =
        api.listAlumni(token, year, profession, city, company, industry, q, page, limit)

    override suspend fun createAlumni(token: String, request: CreateAlumniRequest) =
        api.createAlumni(token, request)

    override suspend fun getAlumni(token: String, alumniId: String) =
        api.getAlumni(token, alumniId)

    override suspend fun updateAlumni(token: String, alumniId: String, request: UpdateAlumniRequest) =
        api.updateAlumni(token, alumniId, request)

    override suspend fun deactivateAlumni(token: String, alumniId: String) =
        api.deactivateAlumni(token, alumniId)

    override suspend fun graduateStudents(token: String, request: GraduateStudentsRequest) =
        api.graduateStudents(token, request)

    override suspend fun bulkImport(token: String, rows: List<CreateAlumniRequest>) =
        api.bulkImport(token, rows)

    override suspend fun listPendingVerifications(token: String) =
        api.listPendingVerifications(token)

    override suspend fun verifyAlumni(token: String, alumniId: String, action: String) =
        api.verifyAlumni(token, alumniId, action)

    override suspend fun toggleFeatured(token: String, alumniId: String) =
        api.toggleFeatured(token, alumniId)

    override suspend fun listCampaigns(token: String) =
        api.listCampaigns(token)

    override suspend fun createCampaign(token: String, request: CreateCampaignRequest) =
        api.createCampaign(token, request)

    override suspend fun getCampaign(token: String, campaignId: String) =
        api.getCampaign(token, campaignId)

    override suspend fun updateCampaign(token: String, campaignId: String, status: String) =
        api.updateCampaign(token, campaignId, status)

    override suspend fun listDonations(token: String, campaignId: String?, alumniId: String?) =
        api.listDonations(token, campaignId, alumniId)

    override suspend fun getAlumniDonations(token: String, alumniId: String) =
        api.getAlumniDonations(token, alumniId)

    override suspend fun createDonation(token: String, request: CreateDonationRequest) =
        api.createDonation(token, request)

    override suspend fun listMentorships(token: String) =
        api.listMentorships(token)

    override suspend fun createMentorship(token: String, request: CreateMentorshipRequest) =
        api.createMentorship(token, request)

    override suspend fun endMentorship(token: String, mentorshipId: String) =
        api.endMentorship(token, mentorshipId)

    override suspend fun listMentorshipRequests(token: String) =
        api.listMentorshipRequests(token)

    override suspend fun adminRespondToMentorshipRequest(token: String, requestId: String, action: String) =
        api.adminRespondToMentorshipRequest(token, requestId, action)

    override suspend fun getMentorshipSettings(token: String) =
        api.getMentorshipSettings(token)

    override suspend fun updateMentorshipSettings(token: String, request: MentorshipSettings) =
        api.updateMentorshipSettings(token, request)

    override suspend fun getAnalyticsOverview(token: String) =
        api.getAnalyticsOverview(token)

    override suspend fun getEngagementMetrics(token: String) =
        api.getEngagementMetrics(token)

    override suspend fun getDonationAnalytics(token: String) =
        api.getDonationAnalytics(token)

    override suspend fun getCareerAnalytics(token: String) =
        api.getCareerAnalytics(token)

    override suspend fun getAlumniProfile(token: String) =
        api.getAlumniProfile(token)

    override suspend fun updateAlumniProfile(token: String, request: UpdateAlumniRequest) =
        api.updateAlumniProfile(token, request)

    override suspend fun updatePrivacy(token: String, request: AlumniPrivacy) =
        api.updatePrivacy(token, request)

    override suspend fun volunteerAsMentor(token: String, expertise: String) =
        api.volunteerAsMentor(token, expertise)

    override suspend fun getMentorshipRequestsForAlumni(token: String) =
        api.getMentorshipRequestsForAlumni(token)

    override suspend fun respondToMentorshipRequest(token: String, requestId: String, action: String) =
        api.respondToMentorshipRequest(token, requestId, action)

    override suspend fun getOwnMentorships(token: String) =
        api.getOwnMentorships(token)

    override suspend fun getCareerHistory(token: String) =
        api.getCareerHistory(token)

    override suspend fun addCareerHistory(token: String, request: CreateCareerHistoryRequest) =
        api.addCareerHistory(token, request)

    override suspend fun updateCareerHistory(token: String, entryId: String, request: CreateCareerHistoryRequest) =
        api.updateCareerHistory(token, entryId, request)

    override suspend fun getOwnDonations(token: String) =
        api.getOwnDonations(token)

    override suspend fun getActiveCampaigns(token: String) =
        api.getActiveCampaigns(token)

    override suspend fun searchDirectory(token: String, year: Int?, profession: String?, city: String?, q: String?, page: Int, limit: Int) =
        api.searchDirectory(token, year, profession, city, q, page, limit)

    override suspend fun registerAlumni(request: AlumniRegisterRequest) =
        api.registerAlumni(request)

    override suspend fun searchSchools(query: String) =
        api.searchSchools(query)
}
