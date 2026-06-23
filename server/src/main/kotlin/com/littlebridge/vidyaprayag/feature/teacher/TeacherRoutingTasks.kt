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

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.feature.notifications.Notify
import com.littlebridge.vidyaprayag.feature.notifications.NotifyRecipients
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.requireOwnedAssignment
import com.littlebridge.vidyaprayag.core.requireTeacherContext
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.HomeworkSubmissionsTable
import com.littlebridge.vidyaprayag.db.HomeworkTable
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

// T-403 (DELETE-don't-patch): the legacy flat syllabus DTOs (SyllabusUnitDto /
// TeacherSyllabusData / UpdateSyllabusRequest) and the `route("/syllabus")`
// GET/PATCH handler that mirrored the old class+subject contract are GONE. The
// typed, hierarchical, assignment-scoped syllabus plane in TeacherSyllabusRouting.kt
// (T-402/T-403) now OWNS GET/POST/PATCH /api/v1/teacher/syllabus(/units|/progress)
// and defines its own Syl* DTOs (the :server module mirrors shared field-for-field).

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
    // REMOVED (T-403): the legacy class+subject `route("/syllabus") { get/patch }`
    // handler that lived here is DELETED — not patched. It read the flat
    // syllabus_units table, matched class/section by string normalisation, and
    // toggled coverage by a free unit_id. The typed, hierarchical, assignment-scoped
    // plane in TeacherSyllabusRouting.kt now OWNS GET /api/v1/teacher/syllabus
    // (hierarchical load), POST …/syllabus/units (B-SYL-1), PATCH …/syllabus/units/{id},
    // and PATCH …/syllabus/progress (one-tap covered toggle). (DELETE-don't-patch law.)

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
