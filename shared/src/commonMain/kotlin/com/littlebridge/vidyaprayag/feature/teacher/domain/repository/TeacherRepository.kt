package com.littlebridge.vidyaprayag.feature.teacher.domain.repository

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.*

interface TeacherRepository {
    // Reads
    // T-601 (DELETE-don't-patch): getHome removed — the legacy Home tab is replaced
    // by the Today tab (getDay/getWeek below), Doc 04 §4.

    // T-501 (Doc 09 §2): aggregated class list (single query set; real flags).
    suspend fun listClassesV2(token: String): NetworkResult<TeacherClassesV2Response>

    // T-502 (Doc 09 §3): composite class detail (roster + signals + summaries).
    suspend fun getClassDetailV2(token: String, assignmentId: String): NetworkResult<ClassDetailResponse>

    // T-503 (Doc 09 §4): scoped student profile (403 if not taught).
    suspend fun getStudentProfileV2(token: String, studentId: String): NetworkResult<StudentProfileResponse>

    // T-104/T-105: server-resolved schedule for the Today tab.
    suspend fun getDay(token: String, date: String? = null): NetworkResult<ResolvedDayResponse>
    suspend fun getWeek(token: String, date: String? = null): NetworkResult<ResolvedWeekResponse>
    // T-205: typed, assignment-scoped attendance (Doc 06 §3.8). Replaces the legacy
    // getAttendance(classId, date) / submitAttendance(SubmitAttendanceRequest).
    suspend fun loadAttendance(token: String, assignmentId: String, date: String? = null): NetworkResult<AttendanceLoadResponse>
    // T-406: legacy getHomework removed — listHomework(assignmentId) below replaces it.

    // T-402: typed, assignment-scoped syllabus (Doc 08 §1.2/§3). The template/
    // progress-split contract — hierarchical load, create unit (B-SYL-1 fix),
    // rename/reorder, one-tap covered toggle. Reached pre-scoped by assignmentId.
    suspend fun loadSyllabus(token: String, assignmentId: String): NetworkResult<SyllabusLoadResponse>
    suspend fun createSyllabusUnit(token: String, request: CreateSyllabusUnitRequest): NetworkResult<SyllabusUnitMutationResponse>
    suspend fun updateSyllabusUnit(token: String, assignmentId: String, unitId: String, request: UpdateSyllabusUnitRequest): NetworkResult<SyllabusUnitMutationResponse>
    suspend fun toggleSyllabusProgress(token: String, request: ToggleSyllabusProgressRequest): NetworkResult<SyllabusUnitMutationResponse>
    suspend fun getProfile(token: String): NetworkResult<TeacherProfileResponse>

    // T-302/T-303/T-304/T-305: Gradebook lifecycle (Doc 07 §2/§5/§6). The canonical
    // assessment + marks contract — scoped list, roster-with-marks, SAVE (no
    // publish), explicit publish/unpublish, server-aggregated history.
    suspend fun listAssessments(token: String, assignmentId: String, status: String? = null): NetworkResult<AssessmentListResponse>
    suspend fun createAssessmentV2(token: String, request: CreateAssessmentRequestV2): NetworkResult<AssessmentCreateResponse>
    suspend fun getAssessmentMarks(token: String, assessmentId: String): NetworkResult<MarksLoadResponse>
    suspend fun saveAssessmentMarks(token: String, assessmentId: String, request: MarksSaveRequest): NetworkResult<MarksSaveResponse>
    suspend fun publishAssessment(token: String, assessmentId: String): NetworkResult<PublishResponse>
    suspend fun unpublishAssessment(token: String, assessmentId: String): NetworkResult<PublishResponse>
    suspend fun getAssessmentHistory(token: String, assignmentId: String): NetworkResult<AssessmentHistoryResponse>

    // T-106c: teacher self check-in (Doc 06 §2).
    suspend fun getCheckInStatus(token: String, date: String? = null): NetworkResult<CheckInStatusResponse>
    suspend fun checkIn(token: String, request: TeacherCheckInRequest): NetworkResult<CheckInStatusResponse>
    suspend fun getObligations(token: String): NetworkResult<TeacherObligationsResponse>

    // Writes
    // T-305: legacy getMarks/submitMarks/getAssessments/createAssessment removed — the
    // canonical scoped gradebook lifecycle above (listAssessments/createAssessmentV2/
    // getAssessmentMarks/saveAssessmentMarks/publish/unpublish/history) replaces them.
    suspend fun saveAttendance(token: String, request: AttendanceSaveRequest): NetworkResult<AttendanceSaveResponse>

    // T-405/T-406: typed homework lifecycle (assign, board, extend, review, close).
    // T-406: legacy createHomework removed — assignHomework below replaces it.
    suspend fun listHomework(token: String, assignmentId: String): NetworkResult<HomeworkListResponse>
    suspend fun assignHomework(token: String, request: AssignHomeworkRequest): NetworkResult<AssignHomeworkResponse>
    suspend fun getHomeworkBoard(token: String, homeworkId: String, assignmentId: String): NetworkResult<HomeworkBoardResponse>
    suspend fun grantHomeworkExtension(token: String, homeworkId: String, request: GrantExtensionRequest): NetworkResult<HomeworkMutationResponse>
    suspend fun reviewHomeworkSubmission(token: String, homeworkId: String, studentId: String, request: ReviewSubmissionRequest): NetworkResult<HomeworkMutationResponse>
    suspend fun closeHomework(token: String, homeworkId: String, assignmentId: String): NetworkResult<HomeworkMutationResponse>

    // RA-44: teacher leave workflow (STUDENT leave routed to my classes).
    suspend fun getLeaveRequests(token: String, status: String? = null): NetworkResult<TeacherLeaveListResponse>
    suspend fun decideLeaveRequest(token: String, id: String, request: TeacherLeaveDecisionRequest): NetworkResult<ApiResponse<Unit>>

    // T-602a (Doc 04 §5.14): the teacher's OWN leave (apply + status list).
    suspend fun getMyLeave(token: String, status: String? = null): NetworkResult<TeacherSelfLeaveListResponse>
    suspend fun applyMyLeave(token: String, request: CreateTeacherLeaveRequest): NetworkResult<TeacherSelfLeaveResponse>

    // RA-51: message all parents of an owned class.
    suspend fun broadcastToClass(token: String, request: TeacherClassBroadcastRequest): NetworkResult<TeacherClassBroadcastResponse>
}
