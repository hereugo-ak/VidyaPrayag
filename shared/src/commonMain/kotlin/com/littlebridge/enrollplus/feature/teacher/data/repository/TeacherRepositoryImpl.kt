package com.littlebridge.enrollplus.feature.teacher.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.teacher.data.remote.TeacherApi
import com.littlebridge.enrollplus.feature.teacher.domain.model.*
import com.littlebridge.enrollplus.feature.teacher.domain.repository.TeacherRepository

class TeacherRepositoryImpl(
    private val api: TeacherApi,
) : TeacherRepository {
    // T-601 (DELETE-don't-patch): getHome override removed — Today tab (getDay/getWeek)
    // replaces the legacy Home tab (Doc 04 §4).

    override suspend fun listClassesV2(token: String): NetworkResult<TeacherClassesV2Response> =
        api.listClassesV2(token)

    override suspend fun getClassDetailV2(token: String, assignmentId: String): NetworkResult<ClassDetailResponse> =
        api.getClassDetailV2(token, assignmentId)

    override suspend fun getStudentProfileV2(token: String, studentId: String): NetworkResult<StudentProfileResponse> =
        api.getStudentProfileV2(token, studentId)

    override suspend fun getDay(token: String, date: String?): NetworkResult<ResolvedDayResponse> =
        api.getDay(token, date)

    override suspend fun getWeek(token: String, date: String?): NetworkResult<ResolvedWeekResponse> =
        api.getWeek(token, date)

    override suspend fun loadAttendance(token: String, assignmentId: String, date: String?): NetworkResult<AttendanceLoadResponse> =
        api.loadAttendance(token, assignmentId, date)

    // T-406: legacy getHomework override removed (listHomework replaces it).

    // T-402: typed, assignment-scoped syllabus (Doc 08 §1.2/§3).
    override suspend fun loadSyllabus(token: String, assignmentId: String): NetworkResult<SyllabusLoadResponse> =
        api.loadSyllabus(token, assignmentId)

    override suspend fun createSyllabusUnit(token: String, request: CreateSyllabusUnitRequest): NetworkResult<SyllabusUnitMutationResponse> =
        api.createSyllabusUnit(token, request)

    override suspend fun updateSyllabusUnit(token: String, assignmentId: String, unitId: String, request: UpdateSyllabusUnitRequest): NetworkResult<SyllabusUnitMutationResponse> =
        api.updateSyllabusUnit(token, assignmentId, unitId, request)

    override suspend fun toggleSyllabusProgress(token: String, request: ToggleSyllabusProgressRequest): NetworkResult<SyllabusUnitMutationResponse> =
        api.toggleSyllabusProgress(token, request)

    override suspend fun getProfile(token: String): NetworkResult<TeacherProfileResponse> =
        api.getProfile(token)

    // T-302/T-303/T-304/T-305: Gradebook lifecycle (Doc 07 §2/§5/§6).
    override suspend fun listAssessments(token: String, assignmentId: String, status: String?): NetworkResult<AssessmentListResponse> =
        api.listAssessments(token, assignmentId, status)

    override suspend fun createAssessmentV2(token: String, request: CreateAssessmentRequestV2): NetworkResult<AssessmentCreateResponse> =
        api.createAssessmentV2(token, request)

    override suspend fun getAssessmentMarks(token: String, assessmentId: String): NetworkResult<MarksLoadResponse> =
        api.getAssessmentMarks(token, assessmentId)

    override suspend fun saveAssessmentMarks(token: String, assessmentId: String, request: MarksSaveRequest): NetworkResult<MarksSaveResponse> =
        api.saveAssessmentMarks(token, assessmentId, request)

    override suspend fun publishAssessment(token: String, assessmentId: String): NetworkResult<PublishResponse> =
        api.publishAssessment(token, assessmentId)

    override suspend fun unpublishAssessment(token: String, assessmentId: String): NetworkResult<PublishResponse> =
        api.unpublishAssessment(token, assessmentId)

    override suspend fun getAssessmentHistory(token: String, assignmentId: String): NetworkResult<AssessmentHistoryResponse> =
        api.getAssessmentHistory(token, assignmentId)

    override suspend fun getCheckInStatus(token: String, date: String?): NetworkResult<CheckInStatusResponse> =
        api.getCheckInStatus(token, date)

    override suspend fun checkIn(token: String, request: TeacherCheckInRequest): NetworkResult<CheckInStatusResponse> =
        api.checkIn(token, request)

    override suspend fun getObligations(token: String): NetworkResult<TeacherObligationsResponse> =
        api.getObligations(token)

    override suspend fun saveAttendance(token: String, request: AttendanceSaveRequest): NetworkResult<AttendanceSaveResponse> =
        api.saveAttendance(token, request)

    // T-406: legacy createHomework override removed (assignHomework replaces it).

    // T-405/T-406: typed homework lifecycle.
    override suspend fun listHomework(token: String, assignmentId: String): NetworkResult<HomeworkListResponse> =
        api.listHomework(token, assignmentId)

    override suspend fun assignHomework(token: String, request: AssignHomeworkRequest): NetworkResult<AssignHomeworkResponse> =
        api.assignHomework(token, request)

    override suspend fun getHomeworkBoard(token: String, homeworkId: String, assignmentId: String): NetworkResult<HomeworkBoardResponse> =
        api.getHomeworkBoard(token, homeworkId, assignmentId)

    override suspend fun grantHomeworkExtension(token: String, homeworkId: String, request: GrantExtensionRequest): NetworkResult<HomeworkMutationResponse> =
        api.grantHomeworkExtension(token, homeworkId, request)

    override suspend fun reviewHomeworkSubmission(token: String, homeworkId: String, studentId: String, request: ReviewSubmissionRequest): NetworkResult<HomeworkMutationResponse> =
        api.reviewHomeworkSubmission(token, homeworkId, studentId, request)

    override suspend fun closeHomework(token: String, homeworkId: String, assignmentId: String): NetworkResult<HomeworkMutationResponse> =
        api.closeHomework(token, homeworkId, assignmentId)

    override suspend fun getLeaveRequests(token: String, status: String?): NetworkResult<TeacherLeaveListResponse> =
        api.getLeaveRequests(token, status)

    override suspend fun decideLeaveRequest(token: String, id: String, request: TeacherLeaveDecisionRequest): NetworkResult<ApiResponse<Unit>> =
        api.decideLeaveRequest(token, id, request)

    // T-602a: the teacher's OWN leave (apply + status list).
    override suspend fun getMyLeave(token: String, status: String?): NetworkResult<TeacherSelfLeaveListResponse> =
        api.getMyLeave(token, status)

    override suspend fun applyMyLeave(token: String, request: CreateTeacherLeaveRequest): NetworkResult<TeacherSelfLeaveResponse> =
        api.applyMyLeave(token, request)

    override suspend fun broadcastToClass(token: String, request: TeacherClassBroadcastRequest): NetworkResult<TeacherClassBroadcastResponse> =
        api.broadcastToClass(token, request)
}
