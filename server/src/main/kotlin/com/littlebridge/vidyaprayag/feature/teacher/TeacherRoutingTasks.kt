/*
 * File: TeacherRoutingTasks.kt
 * Module: feature.teacher
 *
 * The teacher TASK surface — attendance, marks, syllabus and homework — split
 * out of TeacherRouting.kt for readability. Mounted as child routes of
 * `/api/v1/teacher` by TeacherRouting.teacherRouting() via teacherTaskRoutes().
 *
 * Every handler:
 *   - resolves a trusted TeacherContext (401/403/404 on failure)
 *   - for class-scoped calls, resolves the client class_id to an OwnedAssignment
 *     (400/403/404) so a teacher can only touch their own classes (gap G1)
 *   - reads/writes via dbQuery and replies with the canonical envelope.
 *
 * Storage:
 *   attendance → attendance_records (type=student, grade="Class-Section")
 *   marks      → assessments + assessment_marks (numeric max_marks + exam id, G4)
 *   syllabus   → syllabus_units
 *   homework   → homework + homework_submissions
 *
 * No fabricated data: empty rosters / unit lists are returned honestly.
 */
package com.littlebridge.vidyaprayag.feature.teacher

import com.littlebridge.vidyaprayag.core.ClassNaming
import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.feature.notifications.Notify
import com.littlebridge.vidyaprayag.feature.notifications.NotifyRecipients
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.okMessage
import com.littlebridge.vidyaprayag.core.requireOwnedAssignment
import com.littlebridge.vidyaprayag.core.requireTeacherContext
import com.littlebridge.vidyaprayag.core.teacherAssignmentsFor
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.HomeworkSubmissionsTable
import com.littlebridge.vidyaprayag.db.HomeworkTable
import com.littlebridge.vidyaprayag.db.SyllabusUnitsTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
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
// DTOs — mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field.
//
// T-205 (Doc 06 §3, Doc 11): the legacy packed-`grade` attendance DTOs
// (AttendanceEntryDto / TeacherAttendanceData / AttendanceMarkDto /
// SubmitAttendanceRequest) and the legacy `route("/attendance") { get/post }`
// handler that lived here are DELETED — not patched. The typed, assignment-scoped
// attendance plane (TeacherAttendanceRouting.kt, T-203) now OWNS `GET/POST
// /api/v1/teacher/attendance`. This file keeps only the marks/syllabus/homework
// child routes. (DELETE-don't-patch law.)
// ─────────────────────────────────────────────────────────────────────────────

// T-305 (DELETE-don't-patch): the legacy marks DTOs (MarksEntryDto / TeacherMarksData /
// MarkScoreDto / SubmitMarksRequest) and assessment DTOs (TeacherAssessmentDto /
// TeacherAssessmentsData / CreateAssessmentRequest) that mirrored the deleted `/marks`
// and `/assessments` handlers are GONE. The typed gradebook plane in
// TeacherGradebookRouting.kt defines its own Gb* DTOs (the :server module mirrors shared
// field-for-field).

@Serializable
data class SyllabusUnitDto(
    val id: String,
    val title: String,
    @SerialName("is_covered") val isCovered: Boolean = false,
    @SerialName("covered_on") val coveredOn: String? = null,
)

@Serializable
data class TeacherSyllabusData(
    @SerialName("class_name") val className: String,
    val subject: String,
    @SerialName("overall_progress") val overallProgress: Float = 0f,
    val units: List<SyllabusUnitDto> = emptyList(),
)

@Serializable
data class UpdateSyllabusRequest(
    @SerialName("unit_id") val unitId: String,
    @SerialName("is_covered") val isCovered: Boolean,
)

@Serializable
data class HomeworkDto(
    val id: String,
    val title: String,
    val description: String = "",
    @SerialName("class_name") val className: String,
    val subject: String,
    @SerialName("due_date") val dueDate: String,
    @SerialName("submitted_count") val submittedCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
data class TeacherHomeworkData(
    val items: List<HomeworkDto> = emptyList(),
)

@Serializable
data class CreateHomeworkRequest(
    @SerialName("class_id") val classId: String,
    val title: String,
    val description: String = "",
    @SerialName("due_date") val dueDate: String,
)

/** Mounts the syllabus/homework child routes under /api/v1/teacher.
 *  (Attendance moved to TeacherAttendanceRouting.kt — typed & scoped, T-203/T-205.) */
fun Route.teacherTaskRoutes() {

    // ── Attendance ──────────────────────────────────────────────────────────
    // REMOVED (T-205): the legacy packed-`grade` GET/POST /attendance handler that
    // lived here is deleted. The typed, assignment-scoped plane in
    // TeacherAttendanceRouting.kt now owns GET/POST /api/v1/teacher/attendance.
    // ── Marks & Assessments ───────────────────────────────────────────────────
    // REMOVED (T-305): the legacy force-publishing `route("/marks")` (POST set
    // isPublished=true and notified parents on EVERY save — the B-MK-1 bug) and the
    // free-text `route("/assessments")` (list + create) handlers that lived here are
    // DELETED. The canonical, typed, lifecycle-aware GRADEBOOK plane in
    // TeacherGradebookRouting.kt now owns GET/POST /api/v1/teacher/assessments,
    // GET/PUT …/{id}/marks (SAVE never publishes), POST …/{id}/publish (the ONLY
    // notify path), POST …/{id}/unpublish, and GET …/assessments/history.

    // ── Syllabus ────────────────────────────────────────────────────────────
    route("/syllabus") {
        // GET ?class_id=&subject= → unit list + overall progress.
        get {
            val ctx = call.requireTeacherContext() ?: return@get
            val classId = call.request.queryParameters["class_id"]
            val asg = call.requireOwnedAssignment(ctx, classId) ?: return@get
            // subject param defaults to the assignment's own subject.
            val subject = call.request.queryParameters["subject"]?.takeIf { it.isNotBlank() } ?: asg.subject
            val grade = "${asg.className}-${asg.section}"

            val units = dbQuery {
                // ROOT FIX (ISSUE 1): subject exact, (class, section) normalised.
                SyllabusUnitsTable.selectAll().where {
                    (SyllabusUnitsTable.schoolId eq ctx.schoolId) and
                        (SyllabusUnitsTable.subject eq subject)
                }.orderBy(SyllabusUnitsTable.position, SortOrder.ASC)
                    .filter {
                        ClassNaming.sameClassSection(
                            it[SyllabusUnitsTable.className], it[SyllabusUnitsTable.section],
                            asg.className, asg.section
                        )
                    }
            }
            val dtos = units.map { u ->
                SyllabusUnitDto(
                    id = u[SyllabusUnitsTable.id].value.toString(),
                    title = u[SyllabusUnitsTable.title],
                    isCovered = u[SyllabusUnitsTable.isCovered],
                    coveredOn = u[SyllabusUnitsTable.coveredOn]?.toString(),
                )
            }
            val progress = if (dtos.isEmpty()) 0f else dtos.count { it.isCovered }.toFloat() / dtos.size.toFloat()
            call.ok(
                TeacherSyllabusData(className = grade, subject = subject, overallProgress = progress, units = dtos),
                message = "Syllabus loaded",
            )
        }

        // PATCH → toggle one unit's covered flag (scoped via its assignment).
        patch {
            val ctx = call.requireTeacherContext() ?: return@patch
            val req = call.receive<UpdateSyllabusRequest>()
            val unitId = runCatching { UUID.fromString(req.unitId) }.getOrNull()
            if (unitId == null) {
                call.fail("A valid unit_id is required", HttpStatusCode.BadRequest, "BAD_UNIT_ID")
                return@patch
            }

            val unit = dbQuery {
                SyllabusUnitsTable.selectAll().where {
                    (SyllabusUnitsTable.id eq unitId) and
                        (SyllabusUnitsTable.schoolId eq ctx.schoolId)
                }.singleOrNull()
            }
            if (unit == null) {
                call.fail("Syllabus unit not found in your school", HttpStatusCode.NotFound, "UNIT_NOT_FOUND")
                return@patch
            }

            // Authz: the teacher must be assigned to this unit's class+subject.
            val owns = teacherAssignmentsFor(ctx).any {
                it.className == unit[SyllabusUnitsTable.className] &&
                    it.section == unit[SyllabusUnitsTable.section] &&
                    it.subject == unit[SyllabusUnitsTable.subject]
            } || ctx.role == "school_admin" || ctx.role == "admin"
            if (!owns) {
                call.fail("You are not assigned to this class/subject", HttpStatusCode.Forbidden, "NOT_ASSIGNED")
                return@patch
            }

            dbQuery {
                SyllabusUnitsTable.update({ SyllabusUnitsTable.id eq unitId }) {
                    it[isCovered] = req.isCovered
                    // T-004: syllabus_units.covered_on is now a typed `date` column (nullable).
                    it[coveredOn] = if (req.isCovered) LocalDate.now() else null
                    it[coveredBy] = if (req.isCovered) ctx.userId else null
                    it[updatedAt] = Instant.now()
                }
            }
            call.okMessage(if (req.isCovered) "Unit marked covered" else "Unit marked not covered")
        }
    }

    // ── Homework ──────────────────────────────────────────────────────────────
    route("/homework") {
        // GET → all active homework authored by this teacher, with submit ratios.
        get {
            val ctx = call.requireTeacherContext() ?: return@get
            val privileged = ctx.role == "school_admin" || ctx.role == "admin"

            val rows = dbQuery {
                if (privileged) {
                    HomeworkTable.selectAll().where {
                        (HomeworkTable.schoolId eq ctx.schoolId) and (HomeworkTable.isActive eq true)
                    }.orderBy(HomeworkTable.dueDate, SortOrder.DESC).toList()
                } else {
                    HomeworkTable.selectAll().where {
                        (HomeworkTable.schoolId eq ctx.schoolId) and
                            (HomeworkTable.teacherId eq ctx.userId) and
                            (HomeworkTable.isActive eq true)
                    }.orderBy(HomeworkTable.dueDate, SortOrder.DESC).toList()
                }
            }

            val items = rows.map { hw ->
                val hwId = hw[HomeworkTable.id].value
                val className = hw[HomeworkTable.className]
                val section = hw[HomeworkTable.section]
                val submitted = dbQuery {
                    HomeworkSubmissionsTable.selectAll().where {
                        HomeworkSubmissionsTable.homeworkId eq hwId
                    }.count().toInt()
                }
                val total = studentCountFor(ctx.schoolId, className, section)
                HomeworkDto(
                    id = hwId.toString(),
                    title = hw[HomeworkTable.title],
                    description = hw[HomeworkTable.description],
                    className = "$className-$section",
                    subject = hw[HomeworkTable.subject],
                    dueDate = hw[HomeworkTable.dueDate].toString(),
                    submittedCount = submitted,
                    totalCount = total,
                )
            }
            call.ok(TeacherHomeworkData(items), message = "Homework loaded")
        }

        // POST → create a homework row for one of the teacher's classes.
        post {
            val ctx = call.requireTeacherContext() ?: return@post
            val req = call.receive<CreateHomeworkRequest>()
            val asg = call.requireOwnedAssignment(ctx, req.classId) ?: return@post
            if (req.title.isBlank()) {
                call.fail("title is required", HttpStatusCode.BadRequest)
                return@post
            }
            if (req.dueDate.isBlank()) {
                call.fail("due_date is required", HttpStatusCode.BadRequest)
                return@post
            }

            val now = Instant.now()
            val newId = UUID.randomUUID()
            dbQuery {
                HomeworkTable.insert {
                    it[id] = newId
                    it[schoolId] = ctx.schoolId
                    it[teacherId] = ctx.userId
                    it[className] = asg.className
                    it[section] = asg.section
                    it[subject] = asg.subject
                    it[title] = req.title
                    it[description] = req.description
                    // T-004: homework.due_date is now a typed `date` column.
                    it[dueDate] = LocalDate.parse(req.dueDate)
                    it[isActive] = true
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }

            // RA-41: notify the class parents that new homework was assigned.
            val parents = NotifyRecipients.parentsOfClass(ctx.schoolId, asg.className)
            if (parents.isNotEmpty()) {
                Notify.toUsers(
                    userIds = parents,
                    category = "homework",
                    title = "New homework",
                    body = "${asg.subject}: ${req.title} — due ${req.dueDate}.",
                    schoolId = ctx.schoolId,
                    actorId = ctx.userId,
                    deepLink = "parent/academics",
                    refType = "homework",
                    refId = newId.toString(),
                )
            }
            call.created(mapOf("id" to newId.toString()), message = "Homework created")
        }
    }
}
