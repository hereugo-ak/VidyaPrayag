/*
 * File: TeacherGradebookRouting.kt
 * Module: feature.teacher
 *
 * T-303 (Doc 07 §2/§3/§5) — the canonical, typed, lifecycle-aware GRADEBOOK
 * plane that replaces the legacy force-publishing `/marks` + free-text
 * `/assessments` handlers in TeacherRoutingTasks.kt.
 *
 * THE B-MK-1 FIX LIVES HERE. The legacy POST `/marks` set `isPublished=true` and
 * notified parents on EVERY save — a half-entered draft or a typo went straight
 * to parents. This plane splits that apart:
 *   • PUT  …/{id}/marks      → SAVE ONLY. Writes assessment_marks, advances status
 *                              to `marks_pending`, and NEVER notifies. (B-MK-1)
 *   • POST …/{id}/publish    → the ONLY path that sets status=published,
 *                              published_at, and notifies parents (confirmed UI).
 *   • POST …/{id}/unpublish  → retract (audited); status→marks_pending; no notify.
 *
 * Every read and write is scope-bound to the authorizing teacher_subject_assignment
 * via requireOwnedAssignment (X-1/D-ASMT-6) — never a free-text class/section/
 * subject. Roster comes from enrollments (typed, roll-ordered). Entry validation
 * (≤max, ≥0, AB≠0) is enforced SERVER-side (§5.2), not just in the grid.
 *
 * PATH NOTE (deviation, flagged in 11_REBUILD_SEQUENCE):
 *   The legacy `teacherTaskRoutes()` still binds GET/POST `/api/v1/teacher/marks`
 *   and GET/POST `/api/v1/teacher/assessments`; Ktor forbids two handlers on the
 *   same method+path. So this new plane is mounted under `/api/v1/teacher/gradebook`
 *   for now (same convergence pattern as T-203's `/attendance-typed`). T-305
 *   deletes the legacy handlers and converges these to the canonical
 *   `/assessments` paths from Doc 07 §2.
 *
 * Scoping is enforced at THREE levels (the constitution): the SQL only ever
 * touches owned assignments + their enrollment roster (query), the response only
 * carries that scope (API), and the screen reaches this pre-scoped (UI, T-305).
 *
 * DTOs are defined server-side (the :server module does NOT depend on :shared)
 * and mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field.
 */
package com.littlebridge.vidyaprayag.feature.teacher

import com.littlebridge.vidyaprayag.core.OwnedAssignment
import com.littlebridge.vidyaprayag.core.TeacherContext
import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.enrollmentsFor
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.requireOwnedAssignment
import com.littlebridge.vidyaprayag.core.requireTeacherContext
import com.littlebridge.vidyaprayag.db.AssessmentMarksTable
import com.littlebridge.vidyaprayag.db.AssessmentsTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.feature.notifications.Notify
import com.littlebridge.vidyaprayag.feature.notifications.NotifyRecipients
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Lifecycle + type vocab (mirror shared AssessmentStatus / AssessmentType).
// ─────────────────────────────────────────────────────────────────────────────
private object GbStatus {
    const val DRAFT = "draft"
    const val SCHEDULED = "scheduled"
    const val MARKS_PENDING = "marks_pending"
    const val PUBLISHED = "published"
    const val ARCHIVED = "archived"
    val ALL = setOf(DRAFT, SCHEDULED, MARKS_PENDING, PUBLISHED, ARCHIVED)
}
private val GB_TYPES = setOf("scheduled", "surprise", "assignment", "project", "exam")

// ─────────────────────────────────────────────────────────────────────────────
// Server-side DTOs — mirror shared/.../teacher/domain/model/TeacherModels.kt
// (AssessmentDto / AssessmentListData / CreateAssessmentRequestV2 / MarksLoadDto /
//  MarkEntryDto / MarksSaveRequest / MarkSaveEntryDto / MarksSaveResultDto /
//  PublishResultDto) field-for-field.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class GbAssessmentDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("subject_id") val subjectId: String? = null,
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    val name: String,
    val type: String = "scheduled",
    @SerialName("max_marks") val maxMarks: Int = 100,
    @SerialName("pass_marks") val passMarks: Int? = null,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("calendar_event_id") val calendarEventId: String? = null,
    val status: String = GbStatus.DRAFT,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("entered_count") val enteredCount: Int = 0,
    @SerialName("roster_count") val rosterCount: Int = 0,
)

@Serializable
data class GbAssessmentListData(
    val assessments: List<GbAssessmentDto> = emptyList(),
)

@Serializable
data class GbCreateAssessmentRequest(
    @SerialName("assignment_id") val assignmentId: String = "",
    val name: String = "",
    val type: String = "scheduled",
    @SerialName("max_marks") val maxMarks: Int = 0,
    @SerialName("pass_marks") val passMarks: Int? = null,
    @SerialName("exam_date") val examDate: String? = null,
    @SerialName("calendar_event_id") val calendarEventId: String? = null,
    @SerialName("link_to_calendar") val linkToCalendar: Boolean = false,
)

@Serializable
data class GbMarkEntryDto(
    @SerialName("student_id") val studentId: String,
    val name: String,
    @SerialName("roll_no") val rollNo: String = "",
    val marks: Float? = null,
    @SerialName("is_absent") val isAbsent: Boolean = false,
    val remark: String? = null,
)

@Serializable
data class GbMarksLoadDto(
    val assessment: GbAssessmentDto,
    val students: List<GbMarkEntryDto> = emptyList(),
    @SerialName("entered_count") val enteredCount: Int = 0,
    @SerialName("roster_count") val rosterCount: Int = 0,
)

@Serializable
data class GbMarkSaveEntryDto(
    @SerialName("student_id") val studentId: String,
    val marks: Float? = null,
    @SerialName("is_absent") val isAbsent: Boolean = false,
    val remark: String? = null,
)

@Serializable
data class GbMarksSaveRequest(
    val entries: List<GbMarkSaveEntryDto> = emptyList(),
)

@Serializable
data class GbMarksSaveResultDto(
    val saved: Int = 0,
    val status: String = GbStatus.MARKS_PENDING,
    @SerialName("entered_count") val enteredCount: Int = 0,
    @SerialName("roster_count") val rosterCount: Int = 0,
)

@Serializable
data class GbPublishResultDto(
    val status: String = GbStatus.PUBLISHED,
    @SerialName("published_at") val publishedAt: String? = null,
    @SerialName("parents_notified") val parentsNotified: Int = 0,
)

// ─────────────────────────────────────────────────────────────────────────────
// Shared helpers.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolve an assessment id to its row, asserting it belongs to the caller's
 * school AND to an assignment the caller owns. Returns (row, ownedAssignment) or
 * null after responding with the appropriate error. This is the gradebook's
 * scope gate — an assessment is reachable ONLY through an owned assignment
 * (X-1/D-ASMT-6). Legacy rows with a null assignment_id fall back to a
 * school+teacher_id ownership check so the teacher's own historical assessments
 * (and the mirrored exam_results, T-301) remain reachable without fabricating a
 * binding.
 */
private suspend fun ApplicationCall.requireOwnedAssessment(
    ctx: TeacherContext,
    assessmentId: String?,
): Pair<org.jetbrains.exposed.sql.ResultRow, OwnedAssignment?>? {
    val id = assessmentId?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
        fail("A valid assessment id is required", HttpStatusCode.BadRequest, "BAD_ASSESSMENT_ID")
        return null
    }
    val row = dbQuery {
        AssessmentsTable.selectAll().where {
            (AssessmentsTable.id eq id) and (AssessmentsTable.schoolId eq ctx.schoolId)
        }.singleOrNull()
    } ?: run {
        fail("Assessment not found in your school", HttpStatusCode.NotFound, "ASSESSMENT_NOT_FOUND")
        return null
    }

    val assignmentId = row[AssessmentsTable.assignmentId]
    if (assignmentId != null) {
        // Scope-bound (the normal path): the assignment must be owned by caller.
        val asg = requireOwnedAssignment(ctx, assignmentId.toString()) ?: return null
        return row to asg
    }

    // Legacy/unbound row: own it via author teacher_id (or privileged role).
    val authorId = row[AssessmentsTable.teacherId] ?: row[AssessmentsTable.createdBy]
    val privileged = ctx.role == "school_admin" || ctx.role == "admin"
    if (!privileged && authorId != ctx.userId) {
        fail("You are not assigned to this assessment", HttpStatusCode.Forbidden, "NOT_ASSIGNED")
        return null
    }
    return row to null
}

/** Map an assessment row → the wire DTO, filling entered/roster counts. */
private fun assessmentRowToDto(
    row: org.jetbrains.exposed.sql.ResultRow,
    enteredCount: Int = 0,
    rosterCount: Int = 0,
): GbAssessmentDto = GbAssessmentDto(
    id = row[AssessmentsTable.id].value.toString(),
    assignmentId = row[AssessmentsTable.assignmentId]?.toString(),
    classId = row[AssessmentsTable.classId]?.toString(),
    subjectId = row[AssessmentsTable.subjectId]?.toString(),
    className = row[AssessmentsTable.className],
    section = row[AssessmentsTable.section],
    subject = row[AssessmentsTable.subject],
    name = row[AssessmentsTable.name],
    type = row[AssessmentsTable.type],
    maxMarks = row[AssessmentsTable.maxMarks],
    passMarks = row[AssessmentsTable.passMarks],
    examDate = row[AssessmentsTable.examDate]?.toString(),
    calendarEventId = row[AssessmentsTable.calendarEventId]?.toString(),
    status = row[AssessmentsTable.status],
    publishedAt = row[AssessmentsTable.publishedAt]?.toString(),
    enteredCount = enteredCount,
    rosterCount = rosterCount,
)

/**
 * Count entered marks for an assessment — a mark is "entered" when it is either
 * absent (AB is a deliberate entry) or has a non-null numeric value. Used for the
 * "entered k/n" progress on list rows + the save/load result.
 */
private fun countEntered(assessmentId: UUID): Int =
    AssessmentMarksTable.selectAll().where {
        AssessmentMarksTable.assessmentId eq assessmentId
    }.count { row ->
        row[AssessmentMarksTable.isAbsent] || row[AssessmentMarksTable.marks] != null
    }

fun Route.teacherGradebookRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher/gradebook") {
            assessmentListAndCreate()
            assessmentMarksLoadAndSave()
            assessmentPublishUnpublish()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET  /gradebook/assessments?assignmentId=&status=   (list, scoped)
// POST /gradebook/assessments                          (create, returns draft)
// Doc 07 §2/§3.
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.assessmentListAndCreate() {
    route("/assessments") {

        // ── LIST ─────────────────────────────────────────────────────────────
        get {
            val ctx = call.requireTeacherContext() ?: return@get
            val assignmentParam = call.request.queryParameters["assignmentId"]
                ?: call.request.queryParameters["assignment_id"]
            val asg = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@get
            val statusFilter = call.request.queryParameters["status"]
                ?.takeIf { it.isNotBlank() }?.lowercase()
                ?.takeIf { it in GbStatus.ALL }

            val rosterCount = enrollmentsFor(asg).size

            val list = dbQuery {
                AssessmentsTable.selectAll().where {
                    (AssessmentsTable.schoolId eq ctx.schoolId) and
                        (AssessmentsTable.assignmentId eq asg.assignmentId) and
                        (AssessmentsTable.isActive eq true)
                }.orderBy(AssessmentsTable.createdAt, SortOrder.DESC)
                    .filter { statusFilter == null || it[AssessmentsTable.status] == statusFilter }
                    .map { row ->
                        val aId = row[AssessmentsTable.id].value
                        assessmentRowToDto(
                            row,
                            enteredCount = countEntered(aId),
                            rosterCount = rosterCount,
                        )
                    }
                    // Newest exam date first, undated rows last; createdAt as the
                    // stable tiebreaker (already DESC from the query).
                    .sortedWith(
                        compareByDescending<GbAssessmentDto> { it.examDate ?: "" },
                    )
            }
            call.ok(GbAssessmentListData(assessments = list), message = "Assessments loaded")
        }

        // ── CREATE ───────────────────────────────────────────────────────────
        post {
            val ctx = call.requireTeacherContext() ?: return@post
            val req = runCatching { call.receive<GbCreateAssessmentRequest>() }.getOrNull()
            if (req == null) {
                call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST")
                return@post
            }
            val asg = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@post

            // Validation (Doc 07 §3) — name non-empty; max>0; 0≤pass≤max; valid type.
            val name = req.name.trim()
            if (name.isBlank()) {
                call.fail("Assessment name is required", HttpStatusCode.BadRequest, "BAD_NAME"); return@post
            }
            if (req.maxMarks <= 0) {
                call.fail("Max marks must be greater than 0", HttpStatusCode.BadRequest, "BAD_MAX"); return@post
            }
            val type = req.type.trim().lowercase().takeIf { it in GB_TYPES } ?: "scheduled"
            val pass = req.passMarks
            if (pass != null && (pass < 0 || pass > req.maxMarks)) {
                call.fail("Pass marks must be between 0 and max marks", HttpStatusCode.BadRequest, "BAD_PASS"); return@post
            }
            val examDate = req.examDate?.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it) }.getOrNull() ?: run {
                    call.fail("exam_date must be YYYY-MM-DD", HttpStatusCode.BadRequest, "BAD_DATE")
                    return@post
                }
            }
            val calEventId = req.calendarEventId?.takeIf { it.isNotBlank() }
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }

            // Initial status (Doc 07 §4): a future-dated scheduled test waits in
            // `scheduled`; a surprise test (or any test for today/past) opens
            // straight into `marks_pending`. Everything else starts `draft`.
            val today = LocalDate.now()
            val initialStatus = when {
                type == "scheduled" && examDate != null && examDate.isAfter(today) -> GbStatus.SCHEDULED
                type == "surprise" -> GbStatus.MARKS_PENDING
                examDate != null && !examDate.isAfter(today) -> GbStatus.MARKS_PENDING
                else -> GbStatus.DRAFT
            }

            // M8: warn (not block) on a duplicate name within the same assignment.
            val dupExists = dbQuery {
                AssessmentsTable.selectAll().where {
                    (AssessmentsTable.schoolId eq ctx.schoolId) and
                        (AssessmentsTable.assignmentId eq asg.assignmentId) and
                        (AssessmentsTable.isActive eq true)
                }.any { it[AssessmentsTable.name].equals(name, ignoreCase = true) }
            }

            val now = Instant.now()
            val newId = UUID.randomUUID()
            dbQuery {
                AssessmentsTable.insert {
                    it[id] = newId
                    it[schoolId] = ctx.schoolId
                    it[teacherId] = ctx.userId
                    // Legacy display columns (kept in sync for legacy readers).
                    it[className] = asg.className
                    it[section] = asg.section
                    it[subject] = asg.subject
                    it[AssessmentsTable.name] = name
                    it[maxMarks] = req.maxMarks
                    it[examDate] = examDate
                    it[isActive] = true
                    // The lifecycle is now the source of truth; is_published mirrors it.
                    it[isPublished] = false
                    // T-301 typed scope binding.
                    it[assignmentId] = asg.assignmentId
                    it[classId] = asg.classId
                    it[subjectId] = asg.subjectId
                    it[AssessmentsTable.type] = type
                    it[passMarks] = pass
                    it[calendarEventId] = calEventId
                    it[status] = initialStatus
                    it[createdBy] = ctx.userId
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
            val rosterCount = enrollmentsFor(asg).size
            val dto = GbAssessmentDto(
                id = newId.toString(),
                assignmentId = asg.assignmentId.toString(),
                classId = asg.classId?.toString(),
                subjectId = asg.subjectId?.toString(),
                className = asg.className,
                section = asg.section,
                subject = asg.subject,
                name = name,
                type = type,
                maxMarks = req.maxMarks,
                passMarks = pass,
                examDate = examDate?.toString(),
                calendarEventId = calEventId?.toString(),
                status = initialStatus,
                publishedAt = null,
                enteredCount = 0,
                rosterCount = rosterCount,
            )
            val msg = if (dupExists) {
                "Assessment created (note: another \"$name\" already exists for this class)"
            } else {
                "Assessment created"
            }
            call.created(dto, message = msg)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /gradebook/assessments/{id}/marks   (roster + existing marks)
// PUT /gradebook/assessments/{id}/marks   (SAVE ONLY — the B-MK-1 fix)
// Doc 07 §2/§5.
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.assessmentMarksLoadAndSave() {
    route("/assessments/{id}/marks") {

        // ── LOAD roster + restore entered values (§5.3, M4/M5) ─────────────────
        get {
            val ctx = call.requireTeacherContext() ?: return@get
            val (row, asg) = call.requireOwnedAssessment(ctx, call.parameters["id"]) ?: return@get
            val assessmentId = row[AssessmentsTable.id].value

            // Roster from enrollments (typed, roll-ordered). When the assessment is
            // bound to an assignment we use that scope; legacy unbound rows fall back
            // to whatever marks already exist (no fabricated roster).
            val roster = asg?.let { enrollmentsFor(it) } ?: emptyList()

            val data = dbQuery {
                val existing = AssessmentMarksTable.selectAll().where {
                    AssessmentMarksTable.assessmentId eq assessmentId
                }.toList()
                // Key existing marks by the typed studentRef first (D-ASMT-3),
                // falling back to the legacy student_code text for mirrored/legacy rows.
                val byRef = existing.mapNotNull { r ->
                    r[AssessmentMarksTable.studentRef]?.let { it to r }
                }.toMap()
                val byCode = existing.associateBy { it[AssessmentMarksTable.studentId] }

                val students = if (roster.isNotEmpty()) {
                    roster.map { s ->
                        val mark = byRef[s.studentId] ?: byCode[s.studentCode]
                        GbMarkEntryDto(
                            studentId = s.studentId.toString(),
                            name = s.fullName,
                            rollNo = s.rollNumber?.toString() ?: "",
                            marks = mark?.get(AssessmentMarksTable.marks)?.toFloat(),
                            isAbsent = mark?.get(AssessmentMarksTable.isAbsent) ?: false,
                            remark = mark?.get(AssessmentMarksTable.remark),
                        )
                    }
                } else {
                    // Legacy/unbound assessment: surface whatever marks exist by their
                    // denormalised name, so the teacher's history isn't empty.
                    existing.map { r ->
                        GbMarkEntryDto(
                            studentId = r[AssessmentMarksTable.studentRef]?.toString()
                                ?: r[AssessmentMarksTable.studentId],
                            name = r[AssessmentMarksTable.studentName],
                            rollNo = "",
                            marks = r[AssessmentMarksTable.marks]?.toFloat(),
                            isAbsent = r[AssessmentMarksTable.isAbsent],
                            remark = r[AssessmentMarksTable.remark],
                        )
                    }
                }
                val rosterCount = if (roster.isNotEmpty()) roster.size else students.size
                val enteredCount = students.count { it.isAbsent || it.marks != null }
                GbMarksLoadDto(
                    assessment = assessmentRowToDto(row, enteredCount, rosterCount),
                    students = students,
                    enteredCount = enteredCount,
                    rosterCount = rosterCount,
                )
            }
            call.ok(data, message = "Marks roster loaded")
        }

        // ── SAVE marks — NO publish, NO notify (THE B-MK-1 FIX) ────────────────
        put {
            val ctx = call.requireTeacherContext() ?: return@put
            val pair = call.requireOwnedAssessment(ctx, call.parameters["id"]) ?: return@put
            val (row, asg) = pair
            val assessmentId = row[AssessmentsTable.id].value
            val maxMarks = row[AssessmentsTable.maxMarks].toDouble()

            val req = runCatching { call.receive<GbMarksSaveRequest>() }.getOrNull()
            if (req == null) {
                call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST")
                return@put
            }

            // Server-side entry validation (§5.2): >max rejected; <0 rejected. A
            // single out-of-range entry fails the whole save so the teacher fixes
            // it (the grid already blocks inline; this is defense-in-depth).
            for (e in req.entries) {
                if (e.isAbsent) continue
                val m = e.marks ?: continue
                if (m < 0f) {
                    call.fail("Marks cannot be negative", HttpStatusCode.BadRequest, "MARK_NEGATIVE"); return@put
                }
                if (m.toDouble() > maxMarks) {
                    val maxLabel = row[AssessmentsTable.maxMarks]
                    call.fail(
                        "A mark exceeds the maximum of $maxLabel",
                        HttpStatusCode.BadRequest,
                        "MARK_OVER_MAX",
                    )
                    return@put
                }
            }

            // Build a student_code lookup for the typed roster so we can write both
            // the typed studentRef (D-ASMT-3) AND the legacy student_code/name (kept
            // in sync until the legacy readers retire).
            val roster = asg?.let { enrollmentsFor(it) } ?: emptyList()
            val rosterById = roster.associateBy { it.studentId }
            val now = Instant.now()

            val saved = dbQuery {
                var count = 0
                for (e in req.entries) {
                    val sid = runCatching { UUID.fromString(e.studentId) }.getOrNull() ?: continue
                    // Only enrolled students can be marked (M5: transferred-out excluded).
                    // For legacy unbound assessments (empty roster) we still allow the
                    // write keyed by studentRef so historical edits work.
                    val enrolled = rosterById[sid]
                    if (roster.isNotEmpty() && enrolled == null) continue
                    val code = enrolled?.studentCode ?: row[AssessmentsTable.className] // fallback noop
                    val name = enrolled?.fullName ?: ""
                    val clamped = if (e.isAbsent) null else e.marks?.toDouble()?.coerceIn(0.0, maxMarks)

                    val existing = AssessmentMarksTable.selectAll().where {
                        (AssessmentMarksTable.assessmentId eq assessmentId) and
                            (AssessmentMarksTable.studentRef eq sid)
                    }.firstOrNull()
                        ?: enrolled?.let { en ->
                            AssessmentMarksTable.selectAll().where {
                                (AssessmentMarksTable.assessmentId eq assessmentId) and
                                    (AssessmentMarksTable.studentId eq en.studentCode)
                            }.firstOrNull()
                        }

                    if (existing != null) {
                        AssessmentMarksTable.update({
                            AssessmentMarksTable.id eq existing[AssessmentMarksTable.id]
                        }) {
                            it[marks] = clamped
                            it[isAbsent] = e.isAbsent
                            it[remark] = e.remark?.takeIf { r -> r.isNotBlank() }
                            it[studentRef] = sid
                            it[enteredBy] = ctx.userId
                            it[enteredAt] = now
                            it[updatedAt] = now
                        }
                    } else {
                        AssessmentMarksTable.insert {
                            it[id] = UUID.randomUUID()
                            it[AssessmentMarksTable.assessmentId] = assessmentId
                            it[studentId] = enrolled?.studentCode ?: e.studentId
                            it[studentName] = name
                            it[marks] = clamped
                            it[isAbsent] = e.isAbsent
                            it[remark] = e.remark?.takeIf { r -> r.isNotBlank() }
                            it[studentRef] = sid
                            it[enteredBy] = ctx.userId
                            it[enteredAt] = now
                            it[createdAt] = now
                            it[updatedAt] = now
                        }
                    }
                    count++
                }

                // Lifecycle: SAVE advances draft/scheduled → marks_pending. It NEVER
                // touches published_at / is_published / sends a notification. THE FIX.
                val current = row[AssessmentsTable.status]
                if (current == GbStatus.DRAFT || current == GbStatus.SCHEDULED) {
                    AssessmentsTable.update({ AssessmentsTable.id eq assessmentId }) {
                        it[status] = GbStatus.MARKS_PENDING
                        it[updatedAt] = now
                    }
                }
                count
            }

            val rosterCount = if (roster.isNotEmpty()) roster.size else dbQuery {
                AssessmentMarksTable.selectAll()
                    .where { AssessmentMarksTable.assessmentId eq assessmentId }.count().toInt()
            }
            val entered = dbQuery { countEntered(assessmentId) }
            val newStatus = dbQuery {
                AssessmentsTable.selectAll().where { AssessmentsTable.id eq assessmentId }
                    .first()[AssessmentsTable.status]
            }
            call.ok(
                GbMarksSaveResultDto(
                    saved = saved,
                    status = newStatus,
                    enteredCount = entered,
                    rosterCount = rosterCount,
                ),
                message = "Saved (not published)",
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POST /gradebook/assessments/{id}/publish     (the ONLY notify path)
// POST /gradebook/assessments/{id}/unpublish   (retract, audited, no notify)
// Doc 07 §2.
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.assessmentPublishUnpublish() {

    post("/assessments/{id}/publish") {
        val ctx = call.requireTeacherContext() ?: return@post
        val (row, asg) = call.requireOwnedAssessment(ctx, call.parameters["id"]) ?: return@post
        val assessmentId = row[AssessmentsTable.id].value

        if (row[AssessmentsTable.status] == GbStatus.PUBLISHED) {
            call.fail("This assessment is already published", HttpStatusCode.Conflict, "ALREADY_PUBLISHED")
            return@post
        }

        val now = Instant.now()
        dbQuery {
            AssessmentsTable.update({ AssessmentsTable.id eq assessmentId }) {
                it[status] = GbStatus.PUBLISHED
                it[publishedAt] = now
                // Keep the legacy parent-visibility gate in sync (parent reads still
                // filter is_published=true until they move onto status='published').
                it[isPublished] = true
                it[updatedAt] = now
            }
        }

        // Notify parents — the ONLY place marks publish fans out (contrast B-MK-1).
        val examName = row[AssessmentsTable.name]
        val subject = row[AssessmentsTable.subject]
        val className = asg?.className ?: row[AssessmentsTable.className]
        val parents = NotifyRecipients.parentsOfClass(ctx.schoolId, className)
        if (parents.isNotEmpty()) {
            Notify.toUsers(
                userIds = parents,
                category = "marks",
                title = "Results published",
                body = "Marks for \"$examName\" ($subject) have been published.",
                schoolId = ctx.schoolId,
                actorId = ctx.userId,
                deepLink = "parent/academics/marks",
                refType = "assessment",
                refId = assessmentId.toString(),
            )
        }
        call.ok(
            GbPublishResultDto(
                status = GbStatus.PUBLISHED,
                publishedAt = now.toString(),
                parentsNotified = parents.size,
            ),
            message = "Published to ${parents.size} parent(s)",
        )
    }

    post("/assessments/{id}/unpublish") {
        val ctx = call.requireTeacherContext() ?: return@post
        val (row, _) = call.requireOwnedAssessment(ctx, call.parameters["id"]) ?: return@post
        val assessmentId = row[AssessmentsTable.id].value

        if (row[AssessmentsTable.status] != GbStatus.PUBLISHED) {
            call.fail("This assessment is not published", HttpStatusCode.Conflict, "NOT_PUBLISHED")
            return@post
        }

        val now = Instant.now()
        dbQuery {
            AssessmentsTable.update({ AssessmentsTable.id eq assessmentId }) {
                // Retract to marks_pending (audited via updated_at). No re-notify.
                it[status] = GbStatus.MARKS_PENDING
                it[publishedAt] = null
                it[isPublished] = false
                it[updatedAt] = now
            }
        }
        call.ok(
            GbPublishResultDto(
                status = GbStatus.MARKS_PENDING,
                publishedAt = null,
                parentsNotified = 0,
            ),
            message = "Unpublished",
        )
    }
}
