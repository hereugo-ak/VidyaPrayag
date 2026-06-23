package com.littlebridge.vidyaprayag.feature.teacher.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.*

interface TeacherRepository {
    // Reads
    suspend fun getHome(token: String): NetworkResult<TeacherHomeResponse>
    suspend fun getClasses(token: String): NetworkResult<TeacherClassesResponse>

    // T-104/T-105: server-resolved schedule for the Today tab.
    suspend fun getDay(token: String, date: String? = null): NetworkResult<ResolvedDayResponse>
    suspend fun getWeek(token: String, date: String? = null): NetworkResult<ResolvedWeekResponse>
    // T-205: typed, assignment-scoped attendance (Doc 06 §3.8). Replaces the legacy
    // getAttendance(classId, date) / submitAttendance(SubmitAttendanceRequest).
    suspend fun loadAttendance(token: String, assignmentId: String, date: String? = null): NetworkResult<AttendanceLoadResponse>
    suspend fun getMarks(token: String, classId: String, examId: String): NetworkResult<TeacherMarksResponse>
    suspend fun getSyllabus(token: String, classId: String, subject: String): NetworkResult<TeacherSyllabusResponse>
    suspend fun getHomework(token: String): NetworkResult<TeacherHomeworkResponse>
    suspend fun getProfile(token: String): NetworkResult<TeacherProfileResponse>
    suspend fun getAssessments(token: String, classId: String): NetworkResult<TeacherAssessmentsResponse>

    // T-106c: teacher self check-in (Doc 06 §2).
    suspend fun getCheckInStatus(token: String, date: String? = null): NetworkResult<CheckInStatusResponse>
    suspend fun checkIn(token: String, request: TeacherCheckInRequest): NetworkResult<CheckInStatusResponse>
    suspend fun getObligations(token: String): NetworkResult<TeacherObligationsResponse>

    // Writes
    suspend fun saveAttendance(token: String, request: AttendanceSaveRequest): NetworkResult<AttendanceSaveResponse>
    suspend fun submitMarks(token: String, request: SubmitMarksRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun updateSyllabus(token: String, request: UpdateSyllabusRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun createHomework(token: String, request: CreateHomeworkRequest): NetworkResult<ApiResponse<Unit>>
    suspend fun createAssessment(token: String, request: CreateAssessmentRequest): NetworkResult<ApiResponse<TeacherAssessmentDto>>

    // RA-44: teacher leave workflow.
    suspend fun getLeaveRequests(token: String, status: String? = null): NetworkResult<TeacherLeaveListResponse>
    suspend fun decideLeaveRequest(token: String, id: String, request: TeacherLeaveDecisionRequest): NetworkResult<ApiResponse<Unit>>

    // RA-51: message all parents of an owned class.
    suspend fun broadcastToClass(token: String, request: TeacherClassBroadcastRequest): NetworkResult<TeacherClassBroadcastResponse>
}
