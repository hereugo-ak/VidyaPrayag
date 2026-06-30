package com.littlebridge.enrollplus.feature.teacher.data.repository

import com.littlebridge.enrollplus.core.model.ApiResponse
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.core.offline.outbox.OutboxRepository
import com.littlebridge.enrollplus.core.offline.sync.SyncEngine
import com.littlebridge.enrollplus.feature.teacher.data.local.TeacherDayLocalDataSource
import com.littlebridge.enrollplus.feature.teacher.data.offline.AttendanceOutboxOps
import com.littlebridge.enrollplus.feature.teacher.data.offline.OutboxOps
import com.littlebridge.enrollplus.feature.teacher.data.remote.TeacherApi
import com.littlebridge.enrollplus.feature.teacher.domain.model.*
import com.littlebridge.enrollplus.feature.teacher.domain.repository.TeacherRepository
import com.littlebridge.enrollplus.util.todayIso

class TeacherRepositoryImpl(
    private val api: TeacherApi,
    private val dayLocalDataSource: TeacherDayLocalDataSource,
    private val outboxRepository: OutboxRepository? = null,
    private val syncEngine: SyncEngine? = null,
) : TeacherRepository {
    // T-601 (DELETE-don't-patch): getHome override removed — Today tab (getDay/getWeek)
    // replaces the legacy Home tab (Doc 04 §4).

    override suspend fun listClassesV2(token: String): NetworkResult<TeacherClassesV2Response> =
        api.listClassesV2(token)

    override suspend fun getClassDetailV2(token: String, assignmentId: String): NetworkResult<ClassDetailResponse> =
        api.getClassDetailV2(token, assignmentId)

    override suspend fun getStudentProfileV2(token: String, studentId: String): NetworkResult<StudentProfileResponse> =
        api.getStudentProfileV2(token, studentId)

    override suspend fun getDay(token: String, date: String?): NetworkResult<ResolvedDayResponse> {
        val result = api.getDay(token, date)
        return when (result) {
            is NetworkResult.Success -> {
                dayLocalDataSource.save(result.data.data)
                result
            }
            is NetworkResult.ConnectionError -> {
                val cached = dayLocalDataSource.getByDate(date ?: todayIso())
                if (cached != null) {
                    NetworkResult.Success(ResolvedDayResponse(success = true, data = cached))
                } else {
                    result
                }
            }
            is NetworkResult.Error -> result
        }
    }

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

    override suspend fun saveAttendance(token: String, request: AttendanceSaveRequest): NetworkResult<AttendanceSaveResponse> {
        val result = api.saveAttendance(token, request)
        return when (result) {
            is NetworkResult.Success -> result
            is NetworkResult.ConnectionError -> {
                if (outboxRepository != null) {
                    val now = com.littlebridge.enrollplus.core.offline.sync.currentTimeMillis()
                    val idempotencyKey = "att-${request.assignmentId}-${request.date}"
                    val op = AttendanceOutboxOps.create(request, idempotencyKey, now)
                    outboxRepository.enqueue(op)
                    syncEngine?.syncNow()
                    NetworkResult.Success(AttendanceSaveResponse(
                        success = true,
                        data = AttendanceSaveResultDto(
                            saved = request.marks.size,
                            date = request.date,
                        ),
                    ))
                } else {
                    result
                }
            }
            is NetworkResult.Error -> result
        }
    }

    // T-406: legacy createHomework override removed (assignHomework replaces it).

    // T-405/T-406: typed homework lifecycle.
    override suspend fun listHomework(token: String, assignmentId: String): NetworkResult<HomeworkListResponse> =
        api.listHomework(token, assignmentId)

    override suspend fun assignHomework(token: String, request: AssignHomeworkRequest): NetworkResult<AssignHomeworkResponse> {
        val result = api.assignHomework(token, request)
        return when (result) {
            is NetworkResult.Success -> result
            is NetworkResult.ConnectionError -> {
                if (outboxRepository != null) {
                    val now = com.littlebridge.enrollplus.core.offline.sync.currentTimeMillis()
                    outboxRepository.enqueue(OutboxOps.homeworkAssign(request, now))
                    syncEngine?.syncNow()
                    NetworkResult.Success(AssignHomeworkResponse(success = true))
                } else result
            }
            is NetworkResult.Error -> result
        }
    }

    override suspend fun getHomeworkBoard(token: String, homeworkId: String, assignmentId: String): NetworkResult<HomeworkBoardResponse> =
        api.getHomeworkBoard(token, homeworkId, assignmentId)

    override suspend fun grantHomeworkExtension(token: String, homeworkId: String, request: GrantExtensionRequest): NetworkResult<HomeworkMutationResponse> {
        val result = api.grantHomeworkExtension(token, homeworkId, request)
        return when (result) {
            is NetworkResult.Success -> result
            is NetworkResult.ConnectionError -> {
                if (outboxRepository != null) {
                    val now = com.littlebridge.enrollplus.core.offline.sync.currentTimeMillis()
                    outboxRepository.enqueue(OutboxOps.homeworkExtend(homeworkId, request, now))
                    syncEngine?.syncNow()
                    NetworkResult.Success(HomeworkMutationResponse(success = true))
                } else result
            }
            is NetworkResult.Error -> result
        }
    }

    override suspend fun reviewHomeworkSubmission(token: String, homeworkId: String, studentId: String, request: ReviewSubmissionRequest): NetworkResult<HomeworkMutationResponse> {
        val result = api.reviewHomeworkSubmission(token, homeworkId, studentId, request)
        return when (result) {
            is NetworkResult.Success -> result
            is NetworkResult.ConnectionError -> {
                if (outboxRepository != null) {
                    val now = com.littlebridge.enrollplus.core.offline.sync.currentTimeMillis()
                    outboxRepository.enqueue(OutboxOps.homeworkReview(homeworkId, studentId, request, now))
                    syncEngine?.syncNow()
                    NetworkResult.Success(HomeworkMutationResponse(success = true))
                } else result
            }
            is NetworkResult.Error -> result
        }
    }

    override suspend fun closeHomework(token: String, homeworkId: String, assignmentId: String): NetworkResult<HomeworkMutationResponse> {
        val result = api.closeHomework(token, homeworkId, assignmentId)
        return when (result) {
            is NetworkResult.Success -> result
            is NetworkResult.ConnectionError -> {
                if (outboxRepository != null) {
                    val now = com.littlebridge.enrollplus.core.offline.sync.currentTimeMillis()
                    outboxRepository.enqueue(OutboxOps.homeworkClose(homeworkId, assignmentId, now))
                    syncEngine?.syncNow()
                    NetworkResult.Success(HomeworkMutationResponse(success = true))
                } else result
            }
            is NetworkResult.Error -> result
        }
    }

    override suspend fun getLeaveRequests(token: String, status: String?): NetworkResult<TeacherLeaveListResponse> =
        api.getLeaveRequests(token, status)

    override suspend fun decideLeaveRequest(token: String, id: String, request: TeacherLeaveDecisionRequest): NetworkResult<ApiResponse<Unit>> {
        val result = api.decideLeaveRequest(token, id, request)
        return when (result) {
            is NetworkResult.Success -> result
            is NetworkResult.ConnectionError -> {
                if (outboxRepository != null) {
                    val now = com.littlebridge.enrollplus.core.offline.sync.currentTimeMillis()
                    outboxRepository.enqueue(OutboxOps.leaveDecide(id, request, now))
                    syncEngine?.syncNow()
                    NetworkResult.Success(ApiResponse(success = true))
                } else result
            }
            is NetworkResult.Error -> result
        }
    }

    // T-602a: the teacher's OWN leave (apply + status list).
    override suspend fun getMyLeave(token: String, status: String?): NetworkResult<TeacherSelfLeaveListResponse> =
        api.getMyLeave(token, status)

    override suspend fun applyMyLeave(token: String, request: CreateTeacherLeaveRequest): NetworkResult<TeacherSelfLeaveResponse> {
        val result = api.applyMyLeave(token, request)
        return when (result) {
            is NetworkResult.Success -> result
            is NetworkResult.ConnectionError -> {
                if (outboxRepository != null) {
                    val now = com.littlebridge.enrollplus.core.offline.sync.currentTimeMillis()
                    outboxRepository.enqueue(OutboxOps.leaveApply(request, now))
                    syncEngine?.syncNow()
                    NetworkResult.Success(TeacherSelfLeaveResponse(success = true))
                } else result
            }
            is NetworkResult.Error -> result
        }
    }

    override suspend fun broadcastToClass(token: String, request: TeacherClassBroadcastRequest): NetworkResult<TeacherClassBroadcastResponse> {
        val result = api.broadcastToClass(token, request)
        return when (result) {
            is NetworkResult.Success -> result
            is NetworkResult.ConnectionError -> {
                if (outboxRepository != null) {
                    val now = com.littlebridge.enrollplus.core.offline.sync.currentTimeMillis()
                    outboxRepository.enqueue(OutboxOps.broadcastClass(request, now))
                    syncEngine?.syncNow()
                    NetworkResult.Success(TeacherClassBroadcastResponse(success = true))
                } else result
            }
            is NetworkResult.Error -> result
        }
    }

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
}
