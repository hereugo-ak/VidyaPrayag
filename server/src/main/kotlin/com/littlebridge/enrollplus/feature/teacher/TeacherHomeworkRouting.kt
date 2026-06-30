/*
 * File: TeacherHomeworkRouting.kt
 * Module: feature.teacher
 *
 * T-405 (Doc 08 Part B §6/§7/§8) — the canonical, TYPED HOMEWORK lifecycle plane.
 * It replaces the crippled legacy `/homework` GET+POST handler in
 * TeacherRoutingTasks.kt (which could list + create only by free-text class,
 * surfaced NO submissions board, had NO extension, NO attachments, and whose
 * UI Assign button was dead — F-HW-1). This plane:
 *   • GET  …/homework?assignmentId=           → active homework for an owned
 *                                                assignment, per-status counts +
 *                                                attachments + is_past_due.
 *   • POST …/homework                          → ASSIGN (title/desc/typed due-date
 *                                                (+time)/allow-late + attachments);
 *                                                ≥ today guard (H1); notifies parents.
 *                                                The real fix for B-HW-1/F-HW-1.
 *   • GET  …/homework/{id}/submissions         → the BOARD, ROSTER-JOINED so even
 *                                                NOT-SUBMITTED students appear
 *                                                (B-HW-3/H7); status columns + counts.
 *   • POST …/homework/{id}/extend              → teacher EXTENSION — whole class
 *                                                (student_id NULL, H5) or one student
 *                                                (the "she was sick" case, H4); logged.
 *   • PATCH …/homework/{id}/submissions/{studentId} → mark reviewed/graded.
 *   • POST …/homework/{id}/close               → deactivate; board read-only (H9).
 *
 * The no-submit-past-due rule (D-HW-4/H2/H3) is enforced on the STUDENT submission
 * path (parent/student side), not here; this teacher plane sets the policy
 * (`allow_late`) and grants the extensions that relax the cutoff.
 *
 * Every read and write is scope-bound to the authorizing teacher_subject_assignment
 * via requireOwnedAssignment (X-1) — never a free-text class/section/subject. A
 * homework row is reachable ONLY through an owned assignment (requireOwnedHomework
 * checks the homework's typed assignment_id == the owned assignment, with a
 * legacy author/privileged fallback for rows written before the binding existed).
 * Scoping is enforced at THREE levels (the constitution): the SQL only ever
 * touches owned homework + this assignment's enrollment roster (query), the
 * response only carries that scope (API), and the screen reaches this pre-scoped
 * (UI, T-406).
 *
 * PATH NOTE (converged in T-406):
 *   This plane was staged under a temporary `/api/v1/teacher/homework-v2` prefix
 *   in T-405 because the legacy `teacherTaskRoutes()` still bound `GET/POST
 *   /api/v1/teacher/homework` (Ktor forbids two handlers on the same
 *   method+path) — the same staging precedent as T-203's `/attendance-typed`,
 *   T-303's `/gradebook`, and T-402's `/syllabus-typed`. T-406 DELETED the
 *   legacy handler and CONVERGED this plane onto the canonical
 *   `/api/v1/teacher/homework` paths from Doc 08.
 *
 * DTOs are defined server-side (the :server module does NOT depend on :shared)
 * and mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field.
 */
package com.littlebridge.enrollplus.feature.teacher

import com.littlebridge.enrollplus.core.OwnedAssignment
import com.littlebridge.enrollplus.core.TeacherContext
import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.enrollmentsFor
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireOwnedAssignment
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.HomeworkAttachmentsTable
import com.littlebridge.enrollplus.db.HomeworkExtensionsTable
import com.littlebridge.enrollplus.db.HomeworkSubmissionsTable
import com.littlebridge.enrollplus.db.HomeworkTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.notifications.NotifyRecipients
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Submission status constants (Doc 08 §5.3) — mirror shared HomeworkSubmissionStatus.
// ─────────────────────────────────────────────────────────────────────────────
private object HwStatus {
    const val SUBMITTED = "submitted"
    const val LATE = "late"
    const val GRADED = "graded"
    const val NOT_SUBMITTED = "not_submitted"
    val VALID = setOf(SUBMITTED, LATE, GRADED, NOT_SUBMITTED)
}

// ─────────────────────────────────────────────────────────────────────────────
// Server-side DTOs — mirror shared TeacherModels.kt field-for-field.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class HwAttachmentDto(
    val id: String,
    val url: String,
    val filename: String = "",
    val mime: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
)

@Serializable
data class HwItemDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String? = null,
    val title: String,
    val description: String = "",
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    @SerialName("due_date") val dueDate: String,
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("allow_late") val allowLate: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_past_due") val isPastDue: Boolean = false,
    @SerialName("submitted_count") val submittedCount: Int = 0,
    @SerialName("late_count") val lateCount: Int = 0,
    @SerialName("graded_count") val gradedCount: Int = 0,
    @SerialName("not_submitted_count") val notSubmittedCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
    val attachments: List<HwAttachmentDto> = emptyList(),
)

@Serializable
data class HwListData(val items: List<HwItemDto> = emptyList())

@Serializable
data class HwAssignAttachmentDto(
    val url: String,
    val filename: String = "",
    val mime: String = "",
    @SerialName("size_bytes") val sizeBytes: Long = 0,
)

@Serializable
data class HwAssignRequest(
    @SerialName("assignment_id") val assignmentId: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("due_date") val dueDate: String = "",
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("allow_late") val allowLate: Boolean = false,
    val attachments: List<HwAssignAttachmentDto> = emptyList(),
)

// (HwAssignData removed — assign now returns the HwItemDto directly via call.created,
//  letting the canonical envelope provide the single { success, message, data } layer.)

@Serializable
data class HwSubmissionRowDto(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_code") val studentCode: String = "",
    val name: String,
    @SerialName("roll_no") val rollNo: Int? = null,
    val status: String = HwStatus.NOT_SUBMITTED,
    @SerialName("submitted_at") val submittedAt: String? = null,
    val grade: String? = null,
    @SerialName("has_extension") val hasExtension: Boolean = false,
    @SerialName("extended_to") val extendedTo: String? = null,
)

@Serializable
data class HwBoardData(
    @SerialName("homework_id") val homeworkId: String = "",
    val title: String = "",
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    @SerialName("due_date") val dueDate: String = "",
    @SerialName("due_time") val dueTime: String? = null,
    @SerialName("allow_late") val allowLate: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("is_past_due") val isPastDue: Boolean = false,
    val rows: List<HwSubmissionRowDto> = emptyList(),
    @SerialName("submitted_count") val submittedCount: Int = 0,
    @SerialName("late_count") val lateCount: Int = 0,
    @SerialName("graded_count") val gradedCount: Int = 0,
    @SerialName("not_submitted_count") val notSubmittedCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
data class HwExtendRequest(
    @SerialName("assignment_id") val assignmentId: String = "",
    @SerialName("student_id") val studentId: String? = null,
    @SerialName("new_due_date") val newDueDate: String = "",
    @SerialName("new_due_time") val newDueTime: String? = null,
    val reason: String? = null,
)

@Serializable
data class HwReviewRequest(
    @SerialName("assignment_id") val assignmentId: String = "",
    val status: String = HwStatus.GRADED,
    val grade: String? = null,
)

@Serializable
data class HwMutationData(val success: Boolean = true, val message: String = "")

// ─────────────────────────────────────────────────────────────────────────────
// Helpers.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolve a homework id to its row, asserting it belongs to the caller's school
 * AND to the SAME owned assignment scope. This is the homework scope gate — a
 * homework is reachable ONLY through an owned assignment whose id matches the
 * homework's typed `assignment_id` (X-1). For legacy rows written before the
 * binding existed (assignment_id NULL), fall back to authorship (teacher_id ==
 * caller) or a privileged role. Responds + returns null on 400/404/403.
 */
private suspend fun ApplicationCall.requireOwnedHomework(
    ctx: TeacherContext,
    asg: OwnedAssignment,
    homeworkId: String?,
): ResultRow? {
    val id = homeworkId?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
        fail("A valid homework id is required", HttpStatusCode.BadRequest, "BAD_HOMEWORK_ID")
        return null
    }
    val row = dbQuery {
        HomeworkTable.selectAll().where {
            (HomeworkTable.id eq id) and (HomeworkTable.schoolId eq ctx.schoolId)
        }.singleOrNull()
    } ?: run {
        fail("Homework not found in your school", HttpStatusCode.NotFound, "HOMEWORK_NOT_FOUND")
        return null
    }
    val boundAssignment = row[HomeworkTable.assignmentId]
    val privileged = ctx.role == "school_admin" || ctx.role == "admin"
    val scopeMatches = when {
        boundAssignment != null -> boundAssignment == asg.assignmentId
        // Legacy/unbound: author owns it, or a privileged role stands in.
        else -> row[HomeworkTable.teacherId] == ctx.userId || privileged
    }
    if (!scopeMatches) {
        fail("This homework is not in your assigned class", HttpStatusCode.Forbidden, "NOT_IN_SCOPE")
        return null
    }
    return row
}

private fun ResultRow.isHomeworkPastDue(): Boolean {
    val dueDate = this[HomeworkTable.dueDate]
    val dueTime = this[HomeworkTable.dueTime]
    val today = LocalDate.now()
    return when {
        dueDate.isBefore(today) -> true
        dueDate.isAfter(today) -> false
        // Same day: past only if a cutoff time exists and has elapsed.
        else -> dueTime != null && LocalTime.now().isAfter(dueTime)
    }
}

private suspend fun attachmentsFor(homeworkId: UUID): List<HwAttachmentDto> = dbQuery {
    HomeworkAttachmentsTable.selectAll().where {
        HomeworkAttachmentsTable.homeworkId eq homeworkId
    }.orderBy(HomeworkAttachmentsTable.createdAt, SortOrder.ASC).map {
        HwAttachmentDto(
            id = it[HomeworkAttachmentsTable.id].value.toString(),
            url = it[HomeworkAttachmentsTable.url],
            filename = it[HomeworkAttachmentsTable.filename],
            mime = it[HomeworkAttachmentsTable.mime],
            sizeBytes = it[HomeworkAttachmentsTable.sizeBytes],
        )
    }
}

/**
 * Build the roster-joined board for a homework: every active enrolled student
 * (B-HW-3/H7 — not-submitted students appear), each carrying their submission
 * status (or not_submitted), submitted-at, grade, and any extension. Returns the
 * rows + per-status counts.
 */
private suspend fun buildBoard(
    homeworkRow: ResultRow,
    asg: OwnedAssignment,
): HwBoardData {
    val homeworkId = homeworkRow[HomeworkTable.id].value
    val roster = enrollmentsFor(asg)

    // Submissions for this homework, keyed by both typed studentUuid and legacy
    // student_code so a row written either way is found.
    val raw = dbQuery {
        val subs = HomeworkSubmissionsTable.selectAll().where {
            HomeworkSubmissionsTable.homeworkId eq homeworkId
        }.toList()
        val byUuid = subs.mapNotNull { s ->
            s[HomeworkSubmissionsTable.studentUuid]?.let { it to s }
        }.toMap()
        val byCode = subs.associateBy { it[HomeworkSubmissionsTable.studentId] }

        val exts = HomeworkExtensionsTable.selectAll().where {
            HomeworkExtensionsTable.homeworkId eq homeworkId
        }.toList()
        val perStudent = exts.mapNotNull { e ->
            e[HomeworkExtensionsTable.studentId]?.let { it to e[HomeworkExtensionsTable.newDueDate] }
        }.toMap()
        // The latest whole-class extension date (student_id NULL), if any.
        val classExt = exts.filter { it[HomeworkExtensionsTable.studentId] == null }
            .maxByOrNull { it[HomeworkExtensionsTable.newDueDate] }
            ?.get(HomeworkExtensionsTable.newDueDate)
        BoardRaw(byUuid, byCode, perStudent, classExt)
    }
    val subsByUuid = raw.byUuid
    val subsByCode = raw.byCode
    val extByStudent = raw.perStudent
    val classExtension = raw.classExt

    var submitted = 0; var late = 0; var graded = 0; var notSubmitted = 0
    val rows = roster.map { st ->
        val sub = subsByUuid[st.studentId] ?: subsByCode[st.studentCode]
        val status = sub?.get(HomeworkSubmissionsTable.status)?.takeIf { it.isNotBlank() }
            ?: HwStatus.NOT_SUBMITTED
        when (status) {
            HwStatus.SUBMITTED -> submitted++
            HwStatus.LATE -> late++
            HwStatus.GRADED -> graded++
            else -> notSubmitted++
        }
        val perStudentExt = extByStudent[st.studentId]
        val extendedTo = perStudentExt ?: classExtension
        HwSubmissionRowDto(
            studentId = st.studentId.toString(),
            studentCode = st.studentCode,
            name = st.fullName,
            rollNo = st.rollNumber,
            status = status,
            submittedAt = sub?.get(HomeworkSubmissionsTable.submittedAt)?.toString(),
            grade = sub?.get(HomeworkSubmissionsTable.grade),
            hasExtension = perStudentExt != null || classExtension != null,
            extendedTo = extendedTo?.toString(),
        )
    }

    return HwBoardData(
        homeworkId = homeworkId.toString(),
        title = homeworkRow[HomeworkTable.title],
        className = asg.className,
        section = asg.section,
        subject = asg.subject,
        dueDate = homeworkRow[HomeworkTable.dueDate].toString(),
        dueTime = homeworkRow[HomeworkTable.dueTime]?.toString(),
        allowLate = homeworkRow[HomeworkTable.allowLate],
        isActive = homeworkRow[HomeworkTable.isActive],
        isPastDue = homeworkRow.isHomeworkPastDue(),
        rows = rows,
        submittedCount = submitted,
        lateCount = late,
        gradedCount = graded,
        notSubmittedCount = notSubmitted,
        totalCount = roster.size,
    )
}

// Small carrier so the board's single-transaction read returns cleanly.
private data class BoardRaw(
    val byUuid: Map<UUID, ResultRow>,
    val byCode: Map<String, ResultRow>,
    val perStudent: Map<UUID, LocalDate>,
    val classExt: LocalDate?,
)

// ─────────────────────────────────────────────────────────────────────────────
// Route registration.
// ─────────────────────────────────────────────────────────────────────────────
fun Route.teacherHomeworkRouting() {
    authenticate("jwt") {
        // CANONICAL (T-406): converged from the T-405 staging prefix `/homework-v2`
        // to `/api/v1/teacher/homework` now that the legacy GET+POST handler in
        // teacherTaskRoutes() is DELETED — completing the T-203/T-303/T-402 staging
        // precedent for this plane.
        route("/api/v1/teacher/homework") {
            homeworkListAndAssign()
            homeworkBoard()
            homeworkExtend()
            homeworkReview()
            homeworkClose()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET  …/homework?assignmentId=   (active homework, per-status counts)
// POST …/homework                 (ASSIGN — fixes F-HW-1)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.homeworkListAndAssign() {
    get {
        val ctx = call.requireTeacherContext() ?: return@get
        val assignmentParam = call.request.queryParameters["assignmentId"]
            ?: call.request.queryParameters["assignment_id"]
        val asg = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@get

        val rows = dbQuery {
            // Scope by typed assignment_id first; legacy unbound rows by this
            // teacher's class+section display tuple as a fallback (so pre-binding
            // homework still appears) — never widens past the owned scope.
            HomeworkTable.selectAll().where {
                (HomeworkTable.schoolId eq ctx.schoolId) and
                    (HomeworkTable.isActive eq true) and
                    (
                        (HomeworkTable.assignmentId eq asg.assignmentId) or
                            (
                                HomeworkTable.assignmentId.isNull() and
                                    (HomeworkTable.className eq asg.className) and
                                    (HomeworkTable.section eq asg.section) and
                                    (HomeworkTable.subject eq asg.subject)
                            )
                    )
            }.orderBy(HomeworkTable.dueDate, SortOrder.DESC).toList()
        }

        val rosterCount = enrollmentsFor(asg).size
        val items = rows.map { hw ->
            val hwId = hw[HomeworkTable.id].value
            val counts = dbQuery {
                HomeworkSubmissionsTable.selectAll().where {
                    HomeworkSubmissionsTable.homeworkId eq hwId
                }.toList()
            }
            var submitted = 0; var late = 0; var graded = 0
            counts.forEach {
                when (it[HomeworkSubmissionsTable.status]) {
                    HwStatus.SUBMITTED -> submitted++
                    HwStatus.LATE -> late++
                    HwStatus.GRADED -> graded++
                }
            }
            val turnedIn = submitted + late + graded
            HwItemDto(
                id = hwId.toString(),
                assignmentId = hw[HomeworkTable.assignmentId]?.toString(),
                title = hw[HomeworkTable.title],
                description = hw[HomeworkTable.description],
                className = asg.className,
                section = asg.section,
                subject = hw[HomeworkTable.subject],
                dueDate = hw[HomeworkTable.dueDate].toString(),
                dueTime = hw[HomeworkTable.dueTime]?.toString(),
                allowLate = hw[HomeworkTable.allowLate],
                isActive = hw[HomeworkTable.isActive],
                isPastDue = hw.isHomeworkPastDue(),
                submittedCount = submitted,
                lateCount = late,
                gradedCount = graded,
                notSubmittedCount = (rosterCount - turnedIn).coerceAtLeast(0),
                totalCount = rosterCount,
                attachments = attachmentsFor(hwId),
            )
        }
        call.ok(HwListData(items), message = if (items.isEmpty()) "No homework yet" else "Homework loaded")
    }

    // POST → ASSIGN. The real F-HW-1/B-HW-1 fix.
    post {
        val ctx = call.requireTeacherContext() ?: return@post
        val req = runCatching { call.receive<HwAssignRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@post
        }
        val asg = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@post

        val title = req.title.trim()
        if (title.isBlank()) {
            call.fail("Homework title is required", HttpStatusCode.BadRequest, "BAD_TITLE"); return@post
        }
        val dueDate = req.dueDate.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull() ?: run {
                call.fail("due_date must be YYYY-MM-DD", HttpStatusCode.BadRequest, "BAD_DATE"); return@post
            }
        } ?: run {
            call.fail("due_date is required", HttpStatusCode.BadRequest, "BAD_DATE"); return@post
        }
        // H1: due date in the past is blocked at create.
        if (dueDate.isBefore(LocalDate.now())) {
            call.fail("Due date cannot be in the past", HttpStatusCode.BadRequest, "DUE_IN_PAST"); return@post
        }
        val dueTime: LocalTime? = req.dueTime?.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalTime.parse(it) }.getOrNull() ?: run {
                call.fail("due_time must be HH:mm", HttpStatusCode.BadRequest, "BAD_TIME"); return@post
            }
        }

        val now = Instant.now()
        val newId = UUID.randomUUID()
        dbQuery {
            HomeworkTable.insert {
                it[id] = newId
                it[schoolId] = ctx.schoolId
                it[teacherId] = ctx.userId
                it[assignmentId] = asg.assignmentId
                it[classId] = asg.classId
                it[subjectId] = asg.subjectId
                it[className] = asg.className
                it[section] = asg.section
                it[subject] = asg.subject
                it[HomeworkTable.title] = title
                it[description] = req.description.trim()
                it[HomeworkTable.dueDate] = dueDate
                it[HomeworkTable.dueTime] = dueTime
                it[allowLate] = req.allowLate
                it[isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }
            // Attachments by reference (H6: attach succeeds even on retried upload).
            req.attachments.filter { it.url.isNotBlank() }.forEach { att ->
                HomeworkAttachmentsTable.insert {
                    it[id] = UUID.randomUUID()
                    it[homeworkId] = newId
                    it[url] = att.url
                    it[filename] = att.filename
                    it[mime] = att.mime
                    it[sizeBytes] = att.sizeBytes
                    it[uploadedBy] = ctx.userId
                    it[createdAt] = now
                }
            }
        }

        // Notify the class parents that new homework was assigned (RA-41 parity).
        val parents = NotifyRecipients.parentsOfClass(ctx.schoolId, asg.className)
        if (parents.isNotEmpty()) {
            Notify.toUsers(
                userIds = parents,
                category = "homework",
                title = "New homework",
                body = "${asg.subject}: $title — due $dueDate.",
                schoolId = ctx.schoolId,
                actorId = ctx.userId,
                deepLink = "parent/academics",
                refType = "homework",
                refId = newId.toString(),
            )
        }

        val rosterSize = enrollmentsFor(asg).size
        val dto = HwItemDto(
            id = newId.toString(),
            assignmentId = asg.assignmentId.toString(),
            title = title,
            description = req.description.trim(),
            className = asg.className,
            section = asg.section,
            subject = asg.subject,
            dueDate = dueDate.toString(),
            dueTime = dueTime?.toString(),
            allowLate = req.allowLate,
            isActive = true,
            isPastDue = false,
            submittedCount = 0,
            lateCount = 0,
            gradedCount = 0,
            notSubmittedCount = rosterSize,
            totalCount = rosterSize,
            attachments = attachmentsFor(newId),
        )
        // Pass the item DIRECTLY — call.created already wraps it in the canonical
        // { success, message, data } envelope, so AssignHomeworkResponse.data resolves to
        // the item. (Previously this wrapped dto a SECOND time in HwAssignData, producing
        // { data: { success, data: item } }; the client then crashed deserializing
        // HomeworkItemDto: "Fields [id, title, due_date] missing at path $.data".)
        call.created(dto, message = "Homework assigned")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET …/homework/{id}/submissions   (the roster-joined board — B-HW-3)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.homeworkBoard() {
    get("/{id}/submissions") {
        val ctx = call.requireTeacherContext() ?: return@get
        val assignmentParam = call.request.queryParameters["assignmentId"]
            ?: call.request.queryParameters["assignment_id"]
        val asg = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@get
        val hwRow = call.requireOwnedHomework(ctx, asg, call.parameters["id"]) ?: return@get

        val board = buildBoard(hwRow, asg)
        call.ok(
            board,
            message = if (board.totalCount == 0) "No students in this class" else "Submissions board loaded",
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POST …/homework/{id}/extend   (teacher extension — whole class or one student)
// Doc 08 §7 / H4 / H5.
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.homeworkExtend() {
    post("/{id}/extend") {
        val ctx = call.requireTeacherContext() ?: return@post
        val req = runCatching { call.receive<HwExtendRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@post
        }
        val asg = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@post
        val hwRow = call.requireOwnedHomework(ctx, asg, call.parameters["id"]) ?: return@post
        val homeworkId = hwRow[HomeworkTable.id].value

        val newDueDate = req.newDueDate.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull() ?: run {
                call.fail("new_due_date must be YYYY-MM-DD", HttpStatusCode.BadRequest, "BAD_DATE"); return@post
            }
        } ?: run {
            call.fail("new_due_date is required", HttpStatusCode.BadRequest, "BAD_DATE"); return@post
        }
        if (newDueDate.isBefore(LocalDate.now())) {
            call.fail("Extension date cannot be in the past", HttpStatusCode.BadRequest, "EXT_IN_PAST"); return@post
        }
        val newDueTime: LocalTime? = req.newDueTime?.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalTime.parse(it) }.getOrNull() ?: run {
                call.fail("new_due_time must be HH:mm", HttpStatusCode.BadRequest, "BAD_TIME"); return@post
            }
        }

        // student_id NULL = whole class (H5). Otherwise it must be a student on the
        // roster (so a teacher can't extend for a student outside their class).
        val studentUuid: UUID? = if (!req.studentId.isNullOrBlank()) {
            val sid = runCatching { UUID.fromString(req.studentId) }.getOrNull() ?: run {
                call.fail("student_id must be a valid id", HttpStatusCode.BadRequest, "BAD_STUDENT"); return@post
            }
            val onRoster = enrollmentsFor(asg).any { it.studentId == sid }
            if (!onRoster) {
                call.fail("That student is not in this class", HttpStatusCode.Forbidden, "NOT_ON_ROSTER"); return@post
            }
            sid
        } else null

        val now = Instant.now()
        dbQuery {
            HomeworkExtensionsTable.insert {
                it[id] = UUID.randomUUID()
                it[HomeworkExtensionsTable.homeworkId] = homeworkId
                it[studentId] = studentUuid
                it[HomeworkExtensionsTable.newDueDate] = newDueDate
                it[HomeworkExtensionsTable.newDueTime] = newDueTime
                it[grantedBy] = ctx.userId
                it[reason] = req.reason?.takeIf { r -> r.isNotBlank() }
                it[createdAt] = now
            }
            // A whole-class extension also moves the homework's own cutoff (H5);
            // a single-student extension leaves the class cutoff intact (H4).
            if (studentUuid == null) {
                HomeworkTable.update({ HomeworkTable.id eq homeworkId }) {
                    it[dueDate] = newDueDate
                    if (newDueTime != null) it[dueTime] = newDueTime
                    it[updatedAt] = now
                }
            }
        }
        val msg = if (studentUuid == null) "Extension granted to the whole class" else "Extension granted to the student"
        call.ok(HwMutationData(success = true, message = msg), message = msg)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PATCH …/homework/{id}/submissions/{studentId}   (mark reviewed/graded)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.homeworkReview() {
    patch("/{id}/submissions/{studentId}") {
        val ctx = call.requireTeacherContext() ?: return@patch
        val req = runCatching { call.receive<HwReviewRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@patch
        }
        val asg = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@patch
        val hwRow = call.requireOwnedHomework(ctx, asg, call.parameters["id"]) ?: return@patch
        if (!hwRow[HomeworkTable.isActive]) {
            call.fail("This homework is closed", HttpStatusCode.Conflict, "HOMEWORK_CLOSED"); return@patch
        }
        val homeworkId = hwRow[HomeworkTable.id].value

        val status = req.status.trim().lowercase()
        if (status !in HwStatus.VALID) {
            call.fail("Invalid status", HttpStatusCode.BadRequest, "BAD_STATUS"); return@patch
        }
        val targetStudentId = call.parameters["studentId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
            call.fail("A valid student id is required", HttpStatusCode.BadRequest, "BAD_STUDENT"); return@patch
        }
        val enrolled = enrollmentsFor(asg).firstOrNull { it.studentId == targetStudentId } ?: run {
            call.fail("That student is not in this class", HttpStatusCode.Forbidden, "NOT_ON_ROSTER"); return@patch
        }

        val now = Instant.now()
        dbQuery {
            val existing = HomeworkSubmissionsTable.selectAll().where {
                (HomeworkSubmissionsTable.homeworkId eq homeworkId) and
                    (
                        (HomeworkSubmissionsTable.studentUuid eq targetStudentId) or
                            (HomeworkSubmissionsTable.studentId eq enrolled.studentCode)
                    )
            }.singleOrNull()
            if (existing != null) {
                HomeworkSubmissionsTable.update({ HomeworkSubmissionsTable.id eq existing[HomeworkSubmissionsTable.id] }) {
                    it[HomeworkSubmissionsTable.status] = status
                    if (req.grade != null) it[grade] = req.grade.takeIf { g -> g.isNotBlank() }
                    it[reviewedBy] = ctx.userId
                    it[reviewedAt] = now
                    if (existing[HomeworkSubmissionsTable.studentUuid] == null) it[studentUuid] = enrolled.studentId
                }
            } else {
                HomeworkSubmissionsTable.insert {
                    it[id] = UUID.randomUUID()
                    it[HomeworkSubmissionsTable.homeworkId] = homeworkId
                    it[studentId] = enrolled.studentCode
                    it[studentUuid] = enrolled.studentId
                    it[HomeworkSubmissionsTable.status] = status
                    it[grade] = req.grade?.takeIf { g -> g.isNotBlank() }
                    it[reviewedBy] = ctx.userId
                    it[reviewedAt] = now
                }
            }
        }
        call.ok(HwMutationData(success = true, message = "Submission updated"), message = "Submission updated")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POST …/homework/{id}/close   (deactivate — board read-only, H9)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.homeworkClose() {
    post("/{id}/close") {
        val ctx = call.requireTeacherContext() ?: return@post
        val assignmentParam = call.request.queryParameters["assignmentId"]
            ?: call.request.queryParameters["assignment_id"]
        val asg = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@post
        val hwRow = call.requireOwnedHomework(ctx, asg, call.parameters["id"]) ?: return@post
        val homeworkId = hwRow[HomeworkTable.id].value

        dbQuery {
            HomeworkTable.update({ HomeworkTable.id eq homeworkId }) {
                it[isActive] = false
                it[updatedAt] = Instant.now()
            }
        }
        call.ok(HwMutationData(success = true, message = "Homework closed"), message = "Homework closed")
    }
}
