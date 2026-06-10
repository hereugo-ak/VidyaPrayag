/*
 * File: TeacherAssignmentRouting.kt
 * Module: feature.school
 *
 * Structured teacher ⇄ class+section ⇄ subject assignment management.
 *
 * Replaces the free-text `school_subjects.teacher_assigned` column with a real
 * assignment graph (teacher_subject_assignments) so the backend can:
 *   - render per-class subject pools with their actual teacher,
 *   - scope teacher broadcasts to the classes/subjects they teach
 *     (consumed by AnnouncementRouting.resolveRecipientPhones),
 *   - audit who teaches what.
 *
 * Endpoints (all JWT-guarded + school-scoped via requireSchoolContext):
 *   GET    /api/v1/school/teacher-assignments
 *   GET    /api/v1/school/teacher-assignments?class_name=Grade%205
 *   POST   /api/v1/school/teacher-assignments          (create/upsert)
 *   DELETE /api/v1/school/teacher-assignments/{id}      (soft delete)
 *
 * Spec ref: SCHOOL_SIDE_STATUS_REPORT.md §5.5 (class/subject/teacher model).
 */
package com.littlebridge.vidyaprayag.feature.school

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.requireSchoolContext
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.TeacherSubjectAssignmentsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

@Serializable
data class TeacherAssignmentDto(
    val id: String,
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String,
    val section: String = "A",
    @SerialName("subject_id") val subjectId: String? = null,
    val subject: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null
)

@Serializable
data class TeacherAssignmentsListResponse(
    val assignments: List<TeacherAssignmentDto>
)

@Serializable
data class CreateTeacherAssignmentDto(
    @SerialName("class_id") val classId: String? = null,
    @SerialName("class_name") val className: String,
    val section: String? = null,
    @SerialName("subject_id") val subjectId: String? = null,
    val subject: String,
    @SerialName("teacher_id") val teacherId: String? = null,
    @SerialName("teacher_name") val teacherName: String? = null
)

private fun org.jetbrains.exposed.sql.ResultRow.toAssignmentDto() = TeacherAssignmentDto(
    id = this[TeacherSubjectAssignmentsTable.id].value.toString(),
    classId = this[TeacherSubjectAssignmentsTable.classId]?.toString(),
    className = this[TeacherSubjectAssignmentsTable.className],
    section = this[TeacherSubjectAssignmentsTable.section],
    subjectId = this[TeacherSubjectAssignmentsTable.subjectId]?.toString(),
    subject = this[TeacherSubjectAssignmentsTable.subject],
    teacherId = this[TeacherSubjectAssignmentsTable.teacherId]?.toString(),
    teacherName = this[TeacherSubjectAssignmentsTable.teacherName]
)

private fun parseUuid(s: String?): UUID? = s?.let { runCatching { UUID.fromString(it) }.getOrNull() }

fun Route.teacherAssignmentRouting() {
    authenticate("jwt") {
        route("/api/v1/school/teacher-assignments") {

            // ---- list (optionally filtered by class) ----
            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val classFilter = call.request.queryParameters["class_name"]?.takeIf { it.isNotBlank() }
                val list = dbQuery {
                    val base = TeacherSubjectAssignmentsTable.selectAll()
                        .where {
                            (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                                (TeacherSubjectAssignmentsTable.isActive eq true)
                        }
                    val rows = if (classFilter != null) {
                        base.filter { it[TeacherSubjectAssignmentsTable.className].equals(classFilter, ignoreCase = true) }
                    } else {
                        base.toList()
                    }
                    rows.sortedWith(
                        compareBy(
                            { it[TeacherSubjectAssignmentsTable.className] },
                            { it[TeacherSubjectAssignmentsTable.section] },
                            { it[TeacherSubjectAssignmentsTable.subject] }
                        )
                    ).map { it.toAssignmentDto() }
                }
                call.ok(
                    TeacherAssignmentsListResponse(list),
                    message = "Teacher assignments fetched successfully"
                )
            }

            // ---- create / upsert ----
            post {
                val ctx = call.requireSchoolContext() ?: return@post
                val req = call.receive<CreateTeacherAssignmentDto>()

                if (req.className.isBlank() || req.subject.isBlank()) {
                    call.fail("class_name and subject are required", HttpStatusCode.BadRequest)
                    return@post
                }
                if (req.teacherId == null && req.teacherName.isNullOrBlank()) {
                    call.fail("Either teacher_id or teacher_name is required", HttpStatusCode.BadRequest)
                    return@post
                }

                val section = req.section?.takeIf { it.isNotBlank() } ?: "A"
                val now = Instant.now()

                val dto = dbQuery {
                    // Upsert on (school, class, section, subject, teacher_name).
                    val existing = TeacherSubjectAssignmentsTable.selectAll()
                        .where {
                            (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                                (TeacherSubjectAssignmentsTable.className eq req.className) and
                                (TeacherSubjectAssignmentsTable.section eq section) and
                                (TeacherSubjectAssignmentsTable.subject eq req.subject)
                        }
                        .firstOrNull()

                    val rowId: UUID = if (existing != null) {
                        val rid = existing[TeacherSubjectAssignmentsTable.id].value
                        TeacherSubjectAssignmentsTable.update({ TeacherSubjectAssignmentsTable.id eq rid }) {
                            it[classId] = parseUuid(req.classId)
                            it[subjectId] = parseUuid(req.subjectId)
                            it[teacherId] = parseUuid(req.teacherId)
                            it[teacherName] = req.teacherName
                            it[isActive] = true
                            it[updatedAt] = now
                        }
                        rid
                    } else {
                        val newId = UUID.randomUUID()
                        TeacherSubjectAssignmentsTable.insert {
                            it[id] = newId
                            it[schoolId] = ctx.schoolId
                            it[classId] = parseUuid(req.classId)
                            it[className] = req.className
                            it[TeacherSubjectAssignmentsTable.section] = section
                            it[subjectId] = parseUuid(req.subjectId)
                            it[subject] = req.subject
                            it[teacherId] = parseUuid(req.teacherId)
                            it[teacherName] = req.teacherName
                            it[isActive] = true
                            it[createdAt] = now
                            it[updatedAt] = now
                        }
                        newId
                    }

                    TeacherSubjectAssignmentsTable.selectAll()
                        .where { TeacherSubjectAssignmentsTable.id eq rowId }
                        .first()
                        .toAssignmentDto()
                }

                call.created(dto, message = "Teacher assignment saved")
            }

            // ---- soft delete ----
            delete("/{id}") {
                val ctx = call.requireSchoolContext() ?: return@delete
                val id = parseUuid(call.parameters["id"])
                if (id == null) {
                    call.fail("Invalid assignment id", HttpStatusCode.BadRequest)
                    return@delete
                }
                val updated = dbQuery {
                    TeacherSubjectAssignmentsTable.update({
                        (TeacherSubjectAssignmentsTable.id eq id) and
                            (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId)
                    }) {
                        it[isActive] = false
                        it[updatedAt] = Instant.now()
                    }
                }
                if (updated == 0) {
                    call.fail("Assignment not found in your school", HttpStatusCode.NotFound)
                    return@delete
                }
                call.ok(mapOf("id" to id.toString()), message = "Teacher assignment removed")
            }
        }
    }
}
