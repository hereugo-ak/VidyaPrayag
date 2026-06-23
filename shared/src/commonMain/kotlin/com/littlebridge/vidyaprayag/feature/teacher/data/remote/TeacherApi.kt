package com.littlebridge.vidyaprayag.feature.teacher.data.remote

import com.littlebridge.vidyaprayag.core.model.ApiResponse
import com.littlebridge.vidyaprayag.core.network.NetworkResult
import com.littlebridge.vidyaprayag.core.network.safeApiCall
import com.littlebridge.vidyaprayag.feature.teacher.domain.model.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*

/**
 * TeacherApi — Ktor client for the (new) `api/v1/teacher/…` routes.
 *
 * Mirrors [com.littlebridge.vidyaprayag.feature.parent.data.remote.ParentApi]: every call is wrapped
 * in [safeApiCall], authenticated with a bearer token, and hits the school base URL. Read endpoints
 * return typed response envelopes; write endpoints return `ApiResponse<Unit>` (server replies
 * `{ success, message }`).
 */
class TeacherApi(
    private val client: HttpClient,
    private val baseUrl: String,
) {
    private fun getUrl(path: String): String {
        val base = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$base$cleanPath"
    }

    // ── Reads ───────────────────────────────────────────────────────────────

    suspend fun getHome(token: String): NetworkResult<TeacherHomeResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/home"))
    }

    suspend fun getClasses(token: String): NetworkResult<TeacherClassesResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/classes"))
    }

    // T-104/T-105: the server-resolved schedule. `/day` merges periods +
    // exceptions + holidays + calendar + per-period attendanceMarked and carries
    // authoritative now/next indices; `/week` returns Mon–Sat resolved.
    suspend fun getDay(
        token: String,
        date: String? = null,
    ): NetworkResult<ResolvedDayResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/day")) {
            if (date != null) parameter("date", date)
        }
    }

    suspend fun getWeek(
        token: String,
        date: String? = null,
    ): NetworkResult<ResolvedWeekResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/week")) {
            if (date != null) parameter("date", date)
        }
    }

    // T-205 (Doc 06 §3.8): the TYPED, SCOPED attendance load. Keyed by the
    // authorizing assignmentId (Doc 05 binding) — never a free-text class/grade.
    // Returns the enrollment roster with approved-leave pre-defaults, alreadyMarked
    // + last-marked audit (load-for-EDIT), holiday/cancelled flags and the
    // back-date window. Replaces the legacy class_id+grade getAttendance.
    suspend fun loadAttendance(
        token: String,
        assignmentId: String,
        date: String? = null,
    ): NetworkResult<AttendanceLoadResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/attendance")) {
            parameter("assignmentId", assignmentId)
            if (date != null) parameter("date", date)
        }
    }

    suspend fun getHomework(token: String): NetworkResult<TeacherHomeworkResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/homework"))
    }

    // ── T-402/T-403: Typed, scoped syllabus (Doc 08 §1.2/§3) ─────────────────
    // The template/progress-split contract. Reached PRE-SCOPED by assignmentId
    // (X-1) — never a free-text class/subject (contrast the deleted getSyllabus).
    // T-403 CONVERGED these onto the canonical `/api/v1/teacher/syllabus[/...]`
    // paths after deleting the legacy `/syllabus` GET+PATCH handler (the T-203
    // `/attendance-typed`→`/attendance` and T-303 `/gradebook`→`/assessments`
    // convergence precedent).

    /** Units + per-section progress, hierarchical (chapter ▸ topic). Scoped by assignmentId. */
    suspend fun loadSyllabus(
        token: String,
        assignmentId: String,
    ): NetworkResult<SyllabusLoadResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/syllabus")) {
            parameter("assignmentId", assignmentId)
        }
    }

    /** Create a unit (chapter or topic). parentId null → chapter. Fixes B-SYL-1. */
    suspend fun createSyllabusUnit(
        token: String,
        request: CreateSyllabusUnitRequest,
    ): NetworkResult<SyllabusUnitMutationResponse> = safeApiCall {
        client.post(getUrl("api/v1/teacher/syllabus/units")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /** Rename / reorder a unit (edit-mode, deliberate). assignmentId scopes the owned unit. */
    suspend fun updateSyllabusUnit(
        token: String,
        assignmentId: String,
        unitId: String,
        request: UpdateSyllabusUnitRequest,
    ): NetworkResult<SyllabusUnitMutationResponse> = safeApiCall {
        client.patch(getUrl("api/v1/teacher/syllabus/units/$unitId")) {
            parameter("assignmentId", assignmentId)
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /** The one-tap toggle — idempotent, optimistic; stamps typed covered_on. */
    suspend fun toggleSyllabusProgress(
        token: String,
        request: ToggleSyllabusProgressRequest,
    ): NetworkResult<SyllabusUnitMutationResponse> = safeApiCall {
        client.patch(getUrl("api/v1/teacher/syllabus/progress")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // T-305: legacy getMarks / getAssessments (the RA-40 exam-picker GET) removed —
    // the canonical scoped gradebook lifecycle below replaces them at /assessments/*.

    // ── T-302/T-303/T-304/T-305: Gradebook lifecycle (Doc 07 §2/§5/§6) ───────
    // The canonical assessment + marks contract: scoped list, roster-with-marks
    // load, SAVE (no publish — the B-MK-1 fix), explicit publish/unpublish, and
    // server-aggregated history. All scoped to the authorizing assignmentId.

    /** List assessments for an owned assignment, optionally filtered by lifecycle status. */
    suspend fun listAssessments(
        token: String,
        assignmentId: String,
        status: String? = null,
    ): NetworkResult<AssessmentListResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/assessments")) {
            parameter("assignmentId", assignmentId)
            if (status != null) parameter("status", status)
        }
    }

    /** Create a scoped assessment (returns a draft). Doc 07 §3. */
    suspend fun createAssessmentV2(
        token: String,
        request: CreateAssessmentRequestV2,
    ): NetworkResult<AssessmentCreateResponse> = safeApiCall {
        client.post(getUrl("api/v1/teacher/assessments")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /** Roster (enrollment-ordered) + any already-entered marks for an assessment. Doc 07 §5. */
    suspend fun getAssessmentMarks(
        token: String,
        assessmentId: String,
    ): NetworkResult<MarksLoadResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/assessments/$assessmentId/marks"))
    }

    /** SAVE marks — writes `assessment_marks`, status stays marks_pending. NO publish (B-MK-1 fix). */
    suspend fun saveAssessmentMarks(
        token: String,
        assessmentId: String,
        request: MarksSaveRequest,
    ): NetworkResult<MarksSaveResponse> = safeApiCall {
        client.put(getUrl("api/v1/teacher/assessments/$assessmentId/marks")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /** Explicit publish — the ONLY path that notifies parents. Doc 07 §2. */
    suspend fun publishAssessment(
        token: String,
        assessmentId: String,
    ): NetworkResult<PublishResponse> = safeApiCall {
        client.post(getUrl("api/v1/teacher/assessments/$assessmentId/publish")) {
            contentType(ContentType.Application.Json)
        }
    }

    /** Unpublish (audited) — retracts a published assessment; no re-notify. Doc 07 §2. */
    suspend fun unpublishAssessment(
        token: String,
        assessmentId: String,
    ): NetworkResult<PublishResponse> = safeApiCall {
        client.post(getUrl("api/v1/teacher/assessments/$assessmentId/unpublish")) {
            contentType(ContentType.Application.Json)
        }
    }

    /** Server-aggregated trends (timeline + distribution) for an assignment. Doc 07 §6. */
    suspend fun getAssessmentHistory(
        token: String,
        assignmentId: String,
    ): NetworkResult<AssessmentHistoryResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/assessments/history")) {
            parameter("assignmentId", assignmentId)
        }
    }

    suspend fun getProfile(token: String): NetworkResult<TeacherProfileResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/profile"))
    }

    // T-106c: teacher self check-in status for a date (default today). Powers the
    // Today greeting band's amber→green pill (Doc 06 §2.3, §2.4 server-stamped).
    suspend fun getCheckInStatus(
        token: String,
        date: String? = null,
    ): NetworkResult<CheckInStatusResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/checkin")) {
            if (date != null) parameter("date", date)
        }
    }

    // T-107: real "what needs me" obligations for the Today strip (Doc 04 §5.5).
    // Counts are live + scoped to the teacher's allocation server-side; the strip
    // shows "all caught up" only when every count is zero (TeacherObligationsDto
    // .isAllCaughtUp). Replaces the fabricated Today tasks (B-HOME-4).
    suspend fun getObligations(
        token: String,
    ): NetworkResult<TeacherObligationsResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/obligations"))
    }

    // ── Writes ──────────────────────────────────────────────────────────────

    // T-205 (Doc 06 §3.7): the TYPED attendance save. Upserts on (school, date,
    // type, student, assignment); server stamps marked_by/marked_at/source and
    // enforces the future/back-date window (E9/E10). No publish side effects —
    // attendance just saves (contrast the marks B-MK-1 bug). Replaces the legacy
    // submitAttendance (classId + entries).
    suspend fun saveAttendance(
        token: String,
        request: AttendanceSaveRequest,
    ): NetworkResult<AttendanceSaveResponse> = safeApiCall {
        client.post(getUrl("api/v1/teacher/attendance")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // T-106c: record the teacher's self check-in. Idempotent per (teacher, date);
    // `method` is the biometric-ladder rung that succeeded (biometric|pin|manual,
    // Doc 06 §2.1). The server stamps the authoritative timestamp and echoes the
    // (possibly pre-existing) status back in `data`.
    suspend fun checkIn(
        token: String,
        request: TeacherCheckInRequest,
    ): NetworkResult<CheckInStatusResponse> = safeApiCall {
        client.post(getUrl("api/v1/teacher/checkin")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    suspend fun createHomework(
        token: String,
        request: CreateHomeworkRequest,
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.post(getUrl("api/v1/teacher/homework")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    // T-305: legacy submitMarks (POST /marks, force-publish) + createAssessment
    // (POST /assessments, free-text) removed. The gradebook lifecycle above owns
    // PUT /assessments/{id}/marks (SAVE, no publish) + POST /assessments (typed create).

    // ── RA-44: teacher leave workflow ─────────────────────────────────────────

    /** Leave requests routed to this teacher's classes (optionally status-filtered). */
    suspend fun getLeaveRequests(
        token: String,
        status: String? = null,
    ): NetworkResult<TeacherLeaveListResponse> = safeApiCall {
        client.get(getUrl("api/v1/teacher/leave-requests")) {
            if (status != null) parameter("status", status)
        }
    }

    /** Approve / reject a leave request the teacher owns. */
    suspend fun decideLeaveRequest(
        token: String,
        id: String,
        request: TeacherLeaveDecisionRequest,
    ): NetworkResult<ApiResponse<Unit>> = safeApiCall {
        client.patch(getUrl("api/v1/teacher/leave-requests/$id")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }

    /** RA-51: broadcast a message to every parent of an owned class. */
    suspend fun broadcastToClass(
        token: String,
        request: TeacherClassBroadcastRequest,
    ): NetworkResult<TeacherClassBroadcastResponse> = safeApiCall {
        client.post(getUrl("api/v1/teacher/messages/class")) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
    }
}
