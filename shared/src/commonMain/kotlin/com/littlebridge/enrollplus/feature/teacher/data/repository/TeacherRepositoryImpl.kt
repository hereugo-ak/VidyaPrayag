package com.littlebridge.enrollplus.feature.teacher.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.admin.domain.model.ChangeRequestListResponse
import com.littlebridge.enrollplus.feature.admin.domain.model.CreateChangeRequestRequest
import com.littlebridge.enrollplus.feature.admin.domain.model.TimetableChangeRequestDto
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

    // Read Receipts: teacher 1:1 messaging.
    override suspend fun getMessageThreads(token: String): NetworkResult<TeacherMessageThreadsResponse> =
        api.getMessageThreads(token)

    override suspend fun getThreadMessages(token: String, threadId: String): NetworkResult<TeacherThreadMessagesResponse> =
        api.getThreadMessages(token, threadId)

    override suspend fun markThreadRead(token: String, threadId: String): NetworkResult<Unit> =
        when (val r = api.markThreadRead(token, threadId)) {
            is NetworkResult.Success -> {
                val envelope = r.data
                if (!envelope.success) NetworkResult.Error(envelope.message.ifBlank { "Failed to mark thread as read" })
                else NetworkResult.Success(Unit)
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }

    override suspend fun getUnreadCount(token: String): NetworkResult<Int> =
        when (val r = api.getUnreadCount(token)) {
            is NetworkResult.Success -> {
                val dto = r.data
                if (!dto.success) NetworkResult.Error("Failed to fetch unread count")
                else NetworkResult.Success(dto.data?.unreadCount ?: 0)
            }
            is NetworkResult.Error -> NetworkResult.Error(r.message, r.code)
            is NetworkResult.ConnectionError -> NetworkResult.ConnectionError
        }

    override suspend fun sendMessage(token: String, request: TeacherSendMessageRequest): NetworkResult<TeacherSendMessageResponse> =
        api.sendMessage(token, request)

    // Lesson Planning (LESSON_PLANNING_SPEC.md — P1-20)
    override suspend fun listLessonPlans(
        token: String, assignmentId: String, status: String?,
        from: String?, to: String?, unitId: String?,
    ): NetworkResult<LessonPlanListResponse> =
        api.listLessonPlans(token, assignmentId, status, from, to, unitId)

    override suspend fun getLessonPlan(token: String, planId: String): NetworkResult<LessonPlanSingleResponse> =
        api.getLessonPlan(token, planId)

    override suspend fun createLessonPlan(token: String, request: CreateLessonPlanRequest): NetworkResult<LessonPlanSingleResponse> =
        api.createLessonPlan(token, request)

    override suspend fun updateLessonPlan(token: String, planId: String, request: UpdateLessonPlanRequest): NetworkResult<LessonPlanSingleResponse> =
        api.updateLessonPlan(token, planId, request)

    override suspend fun deleteLessonPlan(token: String, planId: String): NetworkResult<ApiResponse<Unit>> =
        api.deleteLessonPlan(token, planId)

    override suspend fun completeLessonPlan(token: String, planId: String): NetworkResult<LessonPlanSingleResponse> =
        api.completeLessonPlan(token, planId)

    override suspend fun skipLessonPlan(token: String, planId: String): NetworkResult<LessonPlanSingleResponse> =
        api.skipLessonPlan(token, planId)

    override suspend fun getLessonCalendar(token: String, assignmentId: String, month: String): NetworkResult<LessonCalendarResponse> =
        api.getLessonCalendar(token, assignmentId, month)

    override suspend fun listLessonTemplates(token: String, assignmentId: String): NetworkResult<LessonTemplateListResponse> =
        api.listLessonTemplates(token, assignmentId)

    override suspend fun saveLessonTemplate(token: String, request: SaveLessonTemplateRequest): NetworkResult<LessonTemplateDto> =
        api.saveLessonTemplate(token, request)

    override suspend fun deleteLessonTemplate(token: String, templateId: String): NetworkResult<ApiResponse<Unit>> =
        api.deleteLessonTemplate(token, templateId)

    override suspend fun instantiateLessonFromTemplate(token: String, templateId: String, request: InstantiateFromTemplateRequest): NetworkResult<LessonPlanSingleResponse> =
        api.instantiateLessonFromTemplate(token, templateId, request)

    override suspend fun getTimetableChangeRequests(token: String): NetworkResult<ChangeRequestListResponse> =
        api.getTimetableChangeRequests(token)

    override suspend fun submitTimetableChangeRequest(token: String, request: CreateChangeRequestRequest): NetworkResult<ApiResponse<TimetableChangeRequestDto>> =
        api.submitTimetableChangeRequest(token, request)
}
