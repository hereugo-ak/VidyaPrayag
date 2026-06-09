package com.littlebridge.vidyaprayag.feature.parent.domain.repository

import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.parent.domain.model.*

interface ParentRepository {
    suspend fun getDashboard(token: String): NetworkResult<ParentDashboardResponse>
    suspend fun getTrackProgress(token: String): NetworkResult<TrackProgressResponse>
    suspend fun getFees(token: String): NetworkResult<FeeResponse>
    suspend fun getScholarships(token: String): NetworkResult<ScholarshipsResponse>
    suspend fun getAnnouncements(token: String): NetworkResult<ParentAnnouncementsResponse>
    suspend fun getNotifications(token: String): NetworkResult<ParentNotificationsResponse>
    /** RA-46: persist read state on the server. */
    suspend fun markNotificationRead(token: String, id: String): NetworkResult<Unit>
    suspend fun markAllNotificationsRead(token: String): NetworkResult<Unit>
    // RA-43/RA-56: child-scoped academic reads.
    suspend fun getChildAttendance(token: String, childId: String): NetworkResult<ParentAttendanceResponse>
    suspend fun getChildMarks(token: String, childId: String): NetworkResult<ParentMarksResponse>
    suspend fun getChildSyllabus(token: String, childId: String): NetworkResult<ParentSyllabusResponse>
    suspend fun searchSchools(token: String, query: String): NetworkResult<SchoolSearchResponse>
    suspend fun linkChild(token: String, request: LinkChildRequest): NetworkResult<LinkChildResponse>
    // RA-44: parent leave workflow.
    suspend fun getLeaveRequests(token: String): NetworkResult<ParentLeaveListResponse>
    suspend fun applyLeave(token: String, request: CreateParentLeaveRequest): NetworkResult<ParentLeaveCreateResponse>
    // RA-51: parent ↔ teacher/admin messaging.
    suspend fun getMessageThreads(token: String): NetworkResult<ParentMessageThreadsResponse>
    suspend fun getThreadMessages(token: String, threadId: String): NetworkResult<ParentThreadMessagesResponse>
    suspend fun sendMessage(token: String, request: ParentSendMessageRequest): NetworkResult<ParentSendMessageResponse>
    // RA-S07: compose-new — who the parent can start a conversation with.
    suspend fun getMessageRecipients(token: String): NetworkResult<ParentRecipientsResponse>
}
