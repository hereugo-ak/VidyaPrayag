/*
 * File: TeacherLessonPlanRouting.kt
 * Module: feature.teacher
 *
 * LESSON_PLANNING_SPEC.md (P1-20) — the canonical, typed, assignment-scoped
 * LESSON PLAN plane. Follows the same X-1 ownership pattern as every other
 * teacher surface (attendance, gradebook, syllabus, homework).
 *
 *   • GET    …/lesson-plans?assignmentId=&status=&from=&to=&unitId=  → list
 *   • POST   …/lesson-plans                                         → create
 *   • GET    …/lesson-plans/{id}                                    → single
 *   • PATCH  …/lesson-plans/{id}                                    → update
 *   • DELETE …/lesson-plans/{id}                                    → soft delete
 *   • POST   …/lesson-plans/{id}/complete                           → complete + syllabus progress
 *   • POST   …/lesson-plans/{id}/skip                               → skip (no syllabus update)
 *   • GET    …/lesson-plans/calendar?assignmentId=&month=YYYY-MM    → calendar
 *   • GET    …/lesson-plan-templates?assignmentId=                  → list templates
 *   • POST   …/lesson-plan-templates                                → save template
 *   • DELETE …/lesson-plan-templates/{id}                           → soft delete template
 *   • POST   …/lesson-plans/from-template/{templateId}              → instantiate
 *
 * Every read and write is scope-bound to the authorizing
 * teacher_subject_assignment via requireOwnedAssignment (X-1). A lesson plan
 * is reachable ONLY through an owned assignment.
 *
 * DTOs are defined server-side (the :server module does NOT depend on :shared)
 * and mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field.
 */
package com.littlebridge.enrollplus.feature.teacher

import com.littlebridge.enrollplus.core.OwnedAssignment
import com.littlebridge.enrollplus.core.TeacherContext
import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.requireOwnedAssignment
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.db.CurriculumUnitsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.LessonPlanTemplatesTable
import com.littlebridge.enrollplus.db.LessonPlansTable
import com.littlebridge.enrollplus.db.SyllabusProgressTable
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
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Server-side DTOs — mirror shared/.../teacher/domain/model/TeacherModels.kt
// field-for-field (Lp* prefix, following the Syl*/Hw*/Gb* convention).
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class LpActivityDto(
    val activity: String,
    @SerialName("duration_min") val durationMin: Int = 15,
)

@Serializable
data class LpCreateRequest(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LpActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,
)

@Serializable
data class LpUpdateRequest(
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    val title: String? = null,
    val objectives: List<String>? = null,
    val activities: List<LpActivityDto>? = null,
    val resources: List<String>? = null,
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,
)

@Serializable
data class LpPlanDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("class_id") val classId: String,
    val section: String = "",
    @SerialName("subject_name") val subjectName: String,
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    @SerialName("curriculum_unit_title") val curriculumUnitTitle: String? = null,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LpActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val status: String = "planned",
    @SerialName("template_source_id") val templateSourceId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class LpListResponse(
    val success: Boolean = true,
    val data: List<LpPlanDto> = emptyList(),
)

@Serializable
data class LpSingleResponse(
    val success: Boolean = true,
    val data: LpPlanDto,
)

@Serializable
data class LpCalendarDto(
    val month: String,
    val days: List<LpCalendarDayDto> = emptyList(),
)

@Serializable
data class LpCalendarDayDto(
    val date: String,
    val plans: List<LpPlanDto> = emptyList(),
)

@Serializable
data class LpCalendarResponse(
    val success: Boolean = true,
    val data: LpCalendarDto,
)

// ── Template DTOs ────────────────────────────────────────────────────────────

@Serializable
data class LpTemplateDto(
    val id: String,
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("subject_name") val subjectName: String,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LpActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("is_shared") val isShared: Boolean = false,
)

@Serializable
data class LpTemplateListResponse(
    val success: Boolean = true,
    val data: List<LpTemplateDto> = emptyList(),
)

@Serializable
data class LpSaveTemplateRequest(
    @SerialName("assignment_id") val assignmentId: String,
    val title: String,
    val objectives: List<String> = emptyList(),
    val activities: List<LpActivityDto> = emptyList(),
    val resources: List<String> = emptyList(),
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("is_shared") val isShared: Boolean = false,
)

@Serializable
data class LpInstantiateRequest(
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("planned_date") val plannedDate: String,
)

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private object LpStatus {
    const val PLANNED = "planned"
    const val COMPLETED = "completed"
    const val SKIPPED = "skipped"
    val VALID = setOf(PLANNED, COMPLETED, SKIPPED)
}

/** JSON (de)serialisation for objectives/activities/resources stored as TEXT. */
private fun toJsonArray(items: List<String>): String =
    items.joinToString(prefix = "[", postfix = "]") { item ->
        "\"${item.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    }

private fun fromJsonArray(json: String?): List<String> {
    if (json.isNullOrBlank()) return emptyList()
    // Simple parse: strip [ ], split by "," respecting quotes.
    val content = json.trim().removeSurrounding("[", "]").trim()
    if (content.isEmpty()) return emptyList()
    return content.split("\",\"")
        .map { it.trim().removeSurrounding("\"") }
        .map { it.replace("\\\"", "\"").replace("\\\\", "\\") }
}

private fun activitiesToJson(activities: List<LpActivityDto>): String {
    if (activities.isEmpty()) return "[]"
    return activities.joinToString(prefix = "[", postfix = "]", separator = ",") { a ->
        "{\"activity\":\"${a.activity.replace("\\", "\\\\").replace("\"", "\\\"")}\",\"duration_min\":${a.durationMin}}"
    }
}

private fun activitiesFromJson(json: String?): List<LpActivityDto> {
    if (json.isNullOrBlank()) return emptyList()
    // Naive parse — the server stores what the client sends; the client parses it back.
    // For robustness, return empty on parse failure (the client re-enters).
    return try {
        val content = json.trim().removeSurrounding("[", "]").trim()
        if (content.isEmpty()) return emptyList()
        // Split by "},{" to get individual objects
        content.split("},{").map { obj ->
            val clean = obj.removeSurrounding("{", "}")
            val activityMatch = Regex("\"activity\"\\s*:\\s*\"(.*?)\"").find(clean)
            val durationMatch = Regex("\"duration_min\"\\s*:\\s*(\\d+)").find(clean)
            LpActivityDto(
                activity = activityMatch?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\\\", "\\") ?: "",
                durationMin = durationMatch?.groupValues?.get(1)?.toIntOrNull() ?: 15,
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Map a lesson_plans row to the DTO, optionally joining the curriculum unit title. */
private suspend fun rowToDto(
    row: ResultRow,
    asg: OwnedAssignment,
): LpPlanDto {
    val unitId = row[LessonPlansTable.curriculumUnitId]
    val unitTitle = if (unitId != null) {
        dbQuery {
            CurriculumUnitsTable.selectAll().where {
                CurriculumUnitsTable.id eq unitId
            }.singleOrNull()?.get(CurriculumUnitsTable.title)
        }
    } else null

    return LpPlanDto(
        id = row[LessonPlansTable.id].value.toString(),
        assignmentId = asg.assignmentId.toString(),
        classId = asg.classId?.toString() ?: "",
        section = asg.section,
        subjectName = asg.subject,
        curriculumUnitId = unitId?.toString(),
        curriculumUnitTitle = unitTitle,
        title = row[LessonPlansTable.title],
        objectives = fromJsonArray(row[LessonPlansTable.objectives]),
        activities = activitiesFromJson(row[LessonPlansTable.activities]),
        resources = fromJsonArray(row[LessonPlansTable.resources]),
        assessmentMethod = row[LessonPlansTable.assessmentMethod],
        durationMinutes = row[LessonPlansTable.durationMinutes],
        homeworkId = row[LessonPlansTable.homeworkId]?.toString(),
        plannedDate = row[LessonPlansTable.plannedDate]?.toString(),
        completedAt = row[LessonPlansTable.completedAt]?.toString(),
        status = row[LessonPlansTable.status],
        templateSourceId = row[LessonPlansTable.templateSourceId]?.toString(),
        createdAt = row[LessonPlansTable.createdAt].toString(),
        updatedAt = row[LessonPlansTable.updatedAt].toString(),
    )
}

/**
 * Resolve a lesson plan by id, asserting it belongs to the caller's school AND
 * to a TSA owned by the caller. Responds + returns null on 400/404/403.
 */
private suspend fun ApplicationCall.requireOwnedLessonPlan(
    ctx: TeacherContext,
    planId: String?,
): Pair<ResultRow, OwnedAssignment>? {
    val id = planId?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
        fail("A valid lesson plan id is required", HttpStatusCode.BadRequest, "BAD_PLAN_ID")
        return null
    }
    val row = dbQuery {
        LessonPlansTable.selectAll().where {
            (LessonPlansTable.id eq id) and
                (LessonPlansTable.schoolId eq ctx.schoolId) and
                (LessonPlansTable.deletedAt.isNull())
        }.singleOrNull()
    } ?: run {
        fail("Lesson plan not found", HttpStatusCode.NotFound, "PLAN_NOT_FOUND")
        return null
    }
    val asg = requireOwnedAssignment(ctx, row[LessonPlansTable.assignmentId].toString())
        ?: return null
    return row to asg
}

/** Map a lesson_plan_templates row to the DTO. */
private fun templateRowToDto(row: ResultRow): LpTemplateDto = LpTemplateDto(
    id = row[LessonPlanTemplatesTable.id].value.toString(),
    assignmentId = row[LessonPlanTemplatesTable.assignmentId].toString(),
    subjectName = row[LessonPlanTemplatesTable.subjectName],
    title = row[LessonPlanTemplatesTable.title],
    objectives = fromJsonArray(row[LessonPlanTemplatesTable.objectives]),
    activities = activitiesFromJson(row[LessonPlanTemplatesTable.activities]),
    resources = fromJsonArray(row[LessonPlanTemplatesTable.resources]),
    assessmentMethod = row[LessonPlanTemplatesTable.assessmentMethod],
    durationMinutes = row[LessonPlanTemplatesTable.durationMinutes],
    isShared = row[LessonPlanTemplatesTable.isShared],
)

// ─────────────────────────────────────────────────────────────────────────────
// Route registration
// ─────────────────────────────────────────────────────────────────────────────

fun Route.teacherLessonPlanRouting() {
    authenticate("jwt") {
        route("/api/v1/teacher/lesson-plans") {
            lessonPlanListAndCreate()
            lessonPlanSingle()
            lessonPlanComplete()
            lessonPlanSkip()
            lessonPlanCalendar()
            lessonPlanFromTemplate()
        }
        route("/api/v1/teacher/lesson-plan-templates") {
            templateListAndSave()
            templateDelete()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET  …/lesson-plans?assignmentId=&status=&from=&to=&unitId=   (list)
// POST …/lesson-plans                                          (create)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.lessonPlanListAndCreate() {
    get {
        val ctx = call.requireTeacherContext() ?: return@get
        val assignmentParam = call.request.queryParameters["assignmentId"]
            ?: call.request.queryParameters["assignment_id"]
        val asg = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@get

        val statusFilter = call.request.queryParameters["status"]?.takeIf { it in LpStatus.VALID }
        val fromStr = call.request.queryParameters["from"]
        val toStr = call.request.queryParameters["to"]
        val unitIdStr = call.request.queryParameters["unitId"]
            ?: call.request.queryParameters["unit_id"]

        val fromDate = fromStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val toDate = toStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val unitId = unitIdStr?.let { runCatching { UUID.fromString(it) }.getOrNull() }

        val rows = dbQuery {
            LessonPlansTable.selectAll().where {
                val base = (LessonPlansTable.assignmentId eq asg.assignmentId) and
                    (LessonPlansTable.deletedAt.isNull())
                var pred: org.jetbrains.exposed.sql.Op<Boolean> = base
                if (statusFilter != null) pred = pred and (LessonPlansTable.status eq statusFilter)
                if (fromDate != null) pred = pred and (LessonPlansTable.plannedDate greaterEq fromDate)
                if (toDate != null) pred = pred and (LessonPlansTable.plannedDate lessEq toDate)
                if (unitId != null) pred = pred and (LessonPlansTable.curriculumUnitId eq unitId)
                pred
            }.orderBy(LessonPlansTable.plannedDate, SortOrder.DESC).toList()
        }

        val plans = rows.map { rowToDto(it, asg) }
        call.ok(plans, message = if (plans.isEmpty()) "No lesson plans yet" else "Lesson plans loaded")
    }

    post {
        val ctx = call.requireTeacherContext() ?: return@post
        val req = runCatching { call.receive<LpCreateRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@post
        }
        val asg = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@post

        val title = req.title.trim()
        if (title.isBlank()) {
            call.fail("Title is required", HttpStatusCode.BadRequest, "BAD_TITLE"); return@post
        }

        val plannedDate = req.plannedDate?.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull() ?: run {
                call.fail("planned_date must be YYYY-MM-DD", HttpStatusCode.BadRequest, "BAD_DATE"); return@post
            }
        }

        val unitId = req.curriculumUnitId?.takeIf { it.isNotBlank() }?.let {
            runCatching { UUID.fromString(it) }.getOrNull() ?: run {
                call.fail("Invalid curriculum_unit_id", HttpStatusCode.BadRequest, "BAD_UNIT_ID"); return@post
            }
        }

        val hwId = req.homeworkId?.takeIf { it.isNotBlank() }?.let {
            runCatching { UUID.fromString(it) }.getOrNull() ?: run {
                call.fail("Invalid homework_id", HttpStatusCode.BadRequest, "BAD_HW_ID"); return@post
            }
        }

        val now = Instant.now()
        val planId = UUID.randomUUID()

        dbQuery {
            LessonPlansTable.insert {
                it[LessonPlansTable.id] = planId
                it[LessonPlansTable.schoolId] = ctx.schoolId
                it[LessonPlansTable.teacherId] = ctx.userId
                it[LessonPlansTable.assignmentId] = asg.assignmentId
                it[LessonPlansTable.classId] = asg.classId ?: UUID.randomUUID()
                it[LessonPlansTable.section] = asg.section
                it[LessonPlansTable.subjectId] = asg.subjectId
                it[LessonPlansTable.subjectName] = asg.subject
                it[LessonPlansTable.curriculumUnitId] = unitId
                it[LessonPlansTable.title] = title
                it[LessonPlansTable.objectives] = toJsonArray(req.objectives)
                it[LessonPlansTable.activities] = activitiesToJson(req.activities)
                it[LessonPlansTable.resources] = toJsonArray(req.resources)
                it[LessonPlansTable.assessmentMethod] = req.assessmentMethod
                it[LessonPlansTable.durationMinutes] = req.durationMinutes
                it[LessonPlansTable.homeworkId] = hwId
                it[LessonPlansTable.plannedDate] = plannedDate
                it[LessonPlansTable.status] = LpStatus.PLANNED
                it[LessonPlansTable.createdAt] = now
                it[LessonPlansTable.updatedAt] = now
            }
        }

        val row = dbQuery {
            LessonPlansTable.selectAll().where { LessonPlansTable.id eq planId }.single()
        }
        call.created(rowToDto(row, asg), message = "Lesson plan created")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET    …/lesson-plans/{id}   (single)
// PATCH  …/lesson-plans/{id}   (update)
// DELETE …/lesson-plans/{id}   (soft delete)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.lessonPlanSingle() {
    get("/{id}") {
        val ctx = call.requireTeacherContext() ?: return@get
        val (row, asg) = call.requireOwnedLessonPlan(ctx, call.parameters["id"]) ?: return@get
        call.ok(rowToDto(row, asg), message = "Lesson plan loaded")
    }

    patch("/{id}") {
        val ctx = call.requireTeacherContext() ?: return@patch
        val (row, asg) = call.requireOwnedLessonPlan(ctx, call.parameters["id"]) ?: return@patch
        val req = runCatching { call.receive<LpUpdateRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@patch
        }

        val planId = row[LessonPlansTable.id].value
        val now = Instant.now()

        val plannedDate = req.plannedDate?.takeIf { it.isNotBlank() }?.let {
            runCatching { LocalDate.parse(it) }.getOrNull() ?: run {
                call.fail("planned_date must be YYYY-MM-DD", HttpStatusCode.BadRequest, "BAD_DATE"); return@patch
            }
        }

        val unitId = req.curriculumUnitId?.takeIf { it.isNotBlank() }?.let {
            runCatching { UUID.fromString(it) }.getOrNull() ?: run {
                call.fail("Invalid curriculum_unit_id", HttpStatusCode.BadRequest, "BAD_UNIT_ID"); return@patch
            }
        }

        val hwId = req.homeworkId?.takeIf { it.isNotBlank() }?.let {
            runCatching { UUID.fromString(it) }.getOrNull() ?: run {
                call.fail("Invalid homework_id", HttpStatusCode.BadRequest, "BAD_HW_ID"); return@patch
            }
        }

        dbQuery {
            LessonPlansTable.update({ LessonPlansTable.id eq planId }) {
                if (unitId != null || req.curriculumUnitId == "") it[LessonPlansTable.curriculumUnitId] = unitId
                if (req.title != null) it[LessonPlansTable.title] = req.title.trim()
                if (req.objectives != null) it[LessonPlansTable.objectives] = toJsonArray(req.objectives)
                if (req.activities != null) it[LessonPlansTable.activities] = activitiesToJson(req.activities)
                if (req.resources != null) it[LessonPlansTable.resources] = toJsonArray(req.resources)
                if (req.assessmentMethod != null) it[LessonPlansTable.assessmentMethod] = req.assessmentMethod
                if (req.durationMinutes != null) it[LessonPlansTable.durationMinutes] = req.durationMinutes
                if (hwId != null || req.homeworkId == "") it[LessonPlansTable.homeworkId] = hwId
                if (plannedDate != null) it[LessonPlansTable.plannedDate] = plannedDate
                it[LessonPlansTable.updatedAt] = now
            }
        }

        val updatedRow = dbQuery {
            LessonPlansTable.selectAll().where { LessonPlansTable.id eq planId }.single()
        }
        call.ok(rowToDto(updatedRow, asg), message = "Lesson plan updated")
    }

    delete("/{id}") {
        val ctx = call.requireTeacherContext() ?: return@delete
        val (row, _) = call.requireOwnedLessonPlan(ctx, call.parameters["id"]) ?: return@delete
        val planId = row[LessonPlansTable.id].value
        val now = Instant.now()

        dbQuery {
            LessonPlansTable.update({ LessonPlansTable.id eq planId }) {
                it[LessonPlansTable.deletedAt] = now
                it[LessonPlansTable.updatedAt] = now
            }
        }
        call.okMessage("Lesson plan deleted")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POST …/lesson-plans/{id}/complete   (marks completed + upserts SyllabusProgressTable)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.lessonPlanComplete() {
    post("/{id}/complete") {
        val ctx = call.requireTeacherContext() ?: return@post
        val (row, asg) = call.requireOwnedLessonPlan(ctx, call.parameters["id"]) ?: return@post
        val planId = row[LessonPlansTable.id].value
        val now = Instant.now()
        val today = todayIst()

        // If a curriculum unit is linked, upsert SyllabusProgressTable.
        val unitId = row[LessonPlansTable.curriculumUnitId]
        if (unitId != null) {
            val section = asg.section.ifBlank { "A" }
            dbQuery {
                val existing = SyllabusProgressTable.selectAll().where {
                    (SyllabusProgressTable.unitId eq unitId) and
                        (SyllabusProgressTable.section eq section) and
                        (SyllabusProgressTable.assignmentId eq asg.assignmentId)
                }.singleOrNull()

                if (existing != null) {
                    SyllabusProgressTable.update({
                        SyllabusProgressTable.id eq existing[SyllabusProgressTable.id]
                    }) {
                        it[isCovered] = true
                        it[SyllabusProgressTable.coveredOn] = today
                        it[coveredBy] = ctx.userId
                        it[updatedAt] = now
                    }
                } else {
                    SyllabusProgressTable.insert {
                        it[SyllabusProgressTable.id] = UUID.randomUUID()
                        it[SyllabusProgressTable.unitId] = unitId
                        it[SyllabusProgressTable.section] = section
                        it[SyllabusProgressTable.assignmentId] = asg.assignmentId
                        it[isCovered] = true
                        it[SyllabusProgressTable.coveredOn] = today
                        it[coveredBy] = ctx.userId
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                }
            }
        }

        dbQuery {
            LessonPlansTable.update({ LessonPlansTable.id eq planId }) {
                it[LessonPlansTable.status] = LpStatus.COMPLETED
                it[LessonPlansTable.completedAt] = now
                it[LessonPlansTable.updatedAt] = now
            }
        }

        val updatedRow = dbQuery {
            LessonPlansTable.selectAll().where { LessonPlansTable.id eq planId }.single()
        }
        call.ok(rowToDto(updatedRow, asg), message = "Lesson completed")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POST …/lesson-plans/{id}/skip   (marks skipped — NO syllabus progress update)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.lessonPlanSkip() {
    post("/{id}/skip") {
        val ctx = call.requireTeacherContext() ?: return@post
        val (row, asg) = call.requireOwnedLessonPlan(ctx, call.parameters["id"]) ?: return@post
        val planId = row[LessonPlansTable.id].value
        val now = Instant.now()

        dbQuery {
            LessonPlansTable.update({ LessonPlansTable.id eq planId }) {
                it[LessonPlansTable.status] = LpStatus.SKIPPED
                it[LessonPlansTable.updatedAt] = now
            }
        }

        val updatedRow = dbQuery {
            LessonPlansTable.selectAll().where { LessonPlansTable.id eq planId }.single()
        }
        call.ok(rowToDto(updatedRow, asg), message = "Lesson skipped")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET …/lesson-plans/calendar?assignmentId=&month=YYYY-MM   (calendar view)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.lessonPlanCalendar() {
    get("/calendar") {
        val ctx = call.requireTeacherContext() ?: return@get
        val assignmentParam = call.request.queryParameters["assignmentId"]
            ?: call.request.queryParameters["assignment_id"]
        val asg = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@get

        val monthStr = call.request.queryParameters["month"]
            ?: todayIst().let { "${it.year}-${it.monthValue.toString().padStart(2, '0')}" }

        val ym = runCatching { YearMonth.parse(monthStr) }.getOrNull() ?: run {
            call.fail("month must be YYYY-MM", HttpStatusCode.BadRequest, "BAD_MONTH"); return@get
        }

        val start = ym.atDay(1)
        val end = ym.atEndOfMonth()

        val rows = dbQuery {
            LessonPlansTable.selectAll().where {
                (LessonPlansTable.assignmentId eq asg.assignmentId) and
                    (LessonPlansTable.deletedAt.isNull()) and
                    (LessonPlansTable.plannedDate.isNotNull()) and
                    (LessonPlansTable.plannedDate greaterEq start) and
                    (LessonPlansTable.plannedDate lessEq end)
            }.orderBy(LessonPlansTable.plannedDate, SortOrder.ASC).toList()
        }

        val plans = rows.map { rowToDto(it, asg) }
        val byDay = plans.groupBy { it.plannedDate ?: "" }
        val days = byDay.entries.sortedBy { it.key }.map { (date, dayPlans) ->
            LpCalendarDayDto(date = date, plans = dayPlans)
        }

        call.ok(LpCalendarDto(month = monthStr, days = days), message = "Calendar loaded")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET  …/lesson-plan-templates?assignmentId=   (list own + shared)
// POST …/lesson-plan-templates                 (save template)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.templateListAndSave() {
    get {
        val ctx = call.requireTeacherContext() ?: return@get
        val assignmentParam = call.request.queryParameters["assignmentId"]
            ?: call.request.queryParameters["assignment_id"]
        val asg = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@get

        val rows = dbQuery {
            LessonPlanTemplatesTable.selectAll().where {
                (LessonPlanTemplatesTable.schoolId eq ctx.schoolId) and
                    (LessonPlanTemplatesTable.deletedAt.isNull()) and
                    (
                        (LessonPlanTemplatesTable.teacherId eq ctx.userId) or
                            (LessonPlanTemplatesTable.isShared eq true)
                    )
            }.orderBy(LessonPlanTemplatesTable.createdAt, SortOrder.DESC).toList()
        }

        val templates = rows.map { templateRowToDto(it) }
        call.ok(templates, message = if (templates.isEmpty()) "No templates yet" else "Templates loaded")
    }

    post {
        val ctx = call.requireTeacherContext() ?: return@post
        val req = runCatching { call.receive<LpSaveTemplateRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@post
        }
        val asg = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@post

        val title = req.title.trim()
        if (title.isBlank()) {
            call.fail("Template title is required", HttpStatusCode.BadRequest, "BAD_TITLE"); return@post
        }

        val now = Instant.now()
        val templateId = UUID.randomUUID()

        dbQuery {
            LessonPlanTemplatesTable.insert {
                it[LessonPlanTemplatesTable.id] = templateId
                it[LessonPlanTemplatesTable.schoolId] = ctx.schoolId
                it[LessonPlanTemplatesTable.teacherId] = ctx.userId
                it[LessonPlanTemplatesTable.assignmentId] = asg.assignmentId
                it[LessonPlanTemplatesTable.subjectName] = asg.subject
                it[LessonPlanTemplatesTable.title] = title
                it[LessonPlanTemplatesTable.objectives] = toJsonArray(req.objectives)
                it[LessonPlanTemplatesTable.activities] = activitiesToJson(req.activities)
                it[LessonPlanTemplatesTable.resources] = toJsonArray(req.resources)
                it[LessonPlanTemplatesTable.assessmentMethod] = req.assessmentMethod
                it[LessonPlanTemplatesTable.durationMinutes] = req.durationMinutes
                it[LessonPlanTemplatesTable.isShared] = req.isShared
                it[LessonPlanTemplatesTable.createdAt] = now
                it[LessonPlanTemplatesTable.updatedAt] = now
            }
        }

        val row = dbQuery {
            LessonPlanTemplatesTable.selectAll().where { LessonPlanTemplatesTable.id eq templateId }.single()
        }
        call.created(templateRowToDto(row), message = "Template saved")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DELETE …/lesson-plan-templates/{id}   (soft delete template)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.templateDelete() {
    delete("/{id}") {
        val ctx = call.requireTeacherContext() ?: return@delete
        val templateId = call.parameters["id"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: run {
            call.fail("A valid template id is required", HttpStatusCode.BadRequest, "BAD_TEMPLATE_ID")
            return@delete
        }

        val row = dbQuery {
            LessonPlanTemplatesTable.selectAll().where {
                (LessonPlanTemplatesTable.id eq templateId) and
                    (LessonPlanTemplatesTable.schoolId eq ctx.schoolId) and
                    (LessonPlanTemplatesTable.deletedAt.isNull())
            }.singleOrNull()
        } ?: run {
            call.fail("Template not found", HttpStatusCode.NotFound, "TEMPLATE_NOT_FOUND"); return@delete
        }

        // Only the author or a school admin can delete.
        val isAuthor = row[LessonPlanTemplatesTable.teacherId] == ctx.userId
        val isAdmin = ctx.role == "school_admin" || ctx.role == "admin"
        if (!isAuthor && !isAdmin) {
            call.fail("You can only delete your own templates", HttpStatusCode.Forbidden, "NOT_TEMPLATE_OWNER")
            return@delete
        }

        val now = Instant.now()
        dbQuery {
            LessonPlanTemplatesTable.update({ LessonPlanTemplatesTable.id eq templateId }) {
                it[LessonPlanTemplatesTable.deletedAt] = now
                it[LessonPlanTemplatesTable.updatedAt] = now
            }
        }
        call.okMessage("Template deleted")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POST …/lesson-plans/from-template/{templateId}   (instantiate a plan from a template)
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.lessonPlanFromTemplate() {
    post("/from-template/{templateId}") {
        val ctx = call.requireTeacherContext() ?: return@post
        val req = runCatching { call.receive<LpInstantiateRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@post
        }
        val asg = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@post

        val templateId = call.parameters["templateId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: run {
            call.fail("A valid template id is required", HttpStatusCode.BadRequest, "BAD_TEMPLATE_ID")
            return@post
        }

        val templateRow = dbQuery {
            LessonPlanTemplatesTable.selectAll().where {
                (LessonPlanTemplatesTable.id eq templateId) and
                    (LessonPlanTemplatesTable.schoolId eq ctx.schoolId) and
                    (LessonPlanTemplatesTable.deletedAt.isNull())
            }.singleOrNull()
        } ?: run {
            call.fail("Template not found", HttpStatusCode.NotFound, "TEMPLATE_NOT_FOUND"); return@post
        }

        // Template must be owned by the caller or shared within the school.
        val isAuthor = templateRow[LessonPlanTemplatesTable.teacherId] == ctx.userId
        val isShared = templateRow[LessonPlanTemplatesTable.isShared]
        if (!isAuthor && !isShared) {
            call.fail("You do not have access to this template", HttpStatusCode.Forbidden, "NOT_TEMPLATE_OWNER")
            return@post
        }

        val plannedDate = runCatching { LocalDate.parse(req.plannedDate) }.getOrNull() ?: run {
            call.fail("planned_date must be YYYY-MM-DD", HttpStatusCode.BadRequest, "BAD_DATE"); return@post
        }

        val now = Instant.now()
        val planId = UUID.randomUUID()

        dbQuery {
            LessonPlansTable.insert {
                it[LessonPlansTable.id] = planId
                it[LessonPlansTable.schoolId] = ctx.schoolId
                it[LessonPlansTable.teacherId] = ctx.userId
                it[LessonPlansTable.assignmentId] = asg.assignmentId
                it[LessonPlansTable.classId] = asg.classId ?: UUID.randomUUID()
                it[LessonPlansTable.section] = asg.section
                it[LessonPlansTable.subjectId] = asg.subjectId
                it[LessonPlansTable.subjectName] = asg.subject
                it[LessonPlansTable.curriculumUnitId] = null
                it[LessonPlansTable.title] = templateRow[LessonPlanTemplatesTable.title]
                it[LessonPlansTable.objectives] = templateRow[LessonPlanTemplatesTable.objectives]
                it[LessonPlansTable.activities] = templateRow[LessonPlanTemplatesTable.activities]
                it[LessonPlansTable.resources] = templateRow[LessonPlanTemplatesTable.resources]
                it[LessonPlansTable.assessmentMethod] = templateRow[LessonPlanTemplatesTable.assessmentMethod]
                it[LessonPlansTable.durationMinutes] = templateRow[LessonPlanTemplatesTable.durationMinutes]
                it[LessonPlansTable.plannedDate] = plannedDate
                it[LessonPlansTable.status] = LpStatus.PLANNED
                it[LessonPlansTable.templateSourceId] = templateId
                it[LessonPlansTable.createdAt] = now
                it[LessonPlansTable.updatedAt] = now
            }
        }

        val row = dbQuery {
            LessonPlansTable.selectAll().where { LessonPlansTable.id eq planId }.single()
        }
        call.created(rowToDto(row, asg), message = "Lesson plan created from template")
    }
}
