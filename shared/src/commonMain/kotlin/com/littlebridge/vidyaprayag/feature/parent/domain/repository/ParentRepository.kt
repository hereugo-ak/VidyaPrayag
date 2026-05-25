package com.littlebridge.vidyaprayag.feature.parent.domain.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.parent.domain.model.*

interface ParentRepository {
    suspend fun getTrackProgress(token: String): NetworkResult<TrackProgressResponse>
    suspend fun getFees(token: String): NetworkResult<FeeResponse>
    suspend fun getScholarships(token: String): NetworkResult<ScholarshipsResponse>
    suspend fun getAnnouncements(token: String): NetworkResult<ParentAnnouncementsResponse>
    suspend fun getParentDashboard(token: String): NetworkResult<ParentDashboardResponse>
    suspend fun getCareerPath(token: String): NetworkResult<CareerPathResponse>
    suspend fun getDailyStatus(token: String): NetworkResult<DailyStatusResponse>
    suspend fun getReports(token: String): NetworkResult<ParentReportsResponse>
    suspend fun getPtmScheduling(token: String): NetworkResult<PtmSchedulingResponse>
}
