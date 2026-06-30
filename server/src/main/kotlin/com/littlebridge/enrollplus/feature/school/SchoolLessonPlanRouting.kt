/*
 * File: SchoolLessonPlanRouting.kt
 * Module: feature.school
 *
 * LESSON_PLANNING_SPEC.md (P1-20) — the school-admin LESSON PLAN review plane.
 * Read-only, school-scoped. Admins can filter by teacher, class, date range,
 * and subject.
 *
 *   GET /api/v1/school/lesson-plans?teacher_id=&class_id=&from=&to=&subject=
 *
 * DTOs are defined server-side and mirror the teacher-side Lp* DTOs.
 */
package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.db.CurriculumUnitsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.LessonPlansTable
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Server-side DTOs (admin review — read-only subset of LpPlanDto)
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class SchLpPlanDto(
    val id: String,
    @SerialName("teacher_id") val teacherId: String,
    @SerialName("assignment_id") val assignmentId: String,
    @SerialName("class_id") val classId: String,
    val section: String = "",
    @SerialName("subject_name") val subjectName: String,
    @SerialName("curriculum_unit_id") val curriculumUnitId: String? = null,
    @SerialName("curriculum_unit_title") val curriculumUnitTitle: String? = null,
    val title: String,
    val objectives: String,
    val activities: String?,
    val resources: String?,
    @SerialName("assessment_method") val assessmentMethod: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 45,
    @SerialName("homework_id") val homeworkId: String? = null,
    @SerialName("planned_date") val plannedDate: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    val status: String = "planned",
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
)

@Serializable
data class SchLpListResponse(
    val success: Boolean = true,
    val data: List<SchLpPlanDto> = emptyList(),
)

// ─────────────────────────────────────────────────────────────────────────────
// Route registration
// ─────────────────────────────────────────────────────────────────────────────

fun Route.schoolLessonPlanRouting() {
    authenticate("jwt") {
        get("/api/v1/school/lesson-plans") {
            val ctx = call.requireSchoolContext() ?: return@get

            val teacherId = call.request.queryParameters["teacher_id"]
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            val classId = call.request.queryParameters["class_id"]
                ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            val fromStr = call.request.queryParameters["from"]
            val toStr = call.request.queryParameters["to"]
            val subject = call.request.queryParameters["subject"]

            val fromDate = fromStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            val toDate = toStr?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

            val rows = dbQuery {
                LessonPlansTable.selectAll().where {
                    val base = (LessonPlansTable.schoolId eq ctx.schoolId) and
                        (LessonPlansTable.deletedAt.isNull())
                    var pred: org.jetbrains.exposed.sql.Op<Boolean> = base
                    if (teacherId != null) pred = pred and (LessonPlansTable.teacherId eq teacherId)
                    if (classId != null) pred = pred and (LessonPlansTable.classId eq classId)
                    if (fromDate != null) pred = pred and (LessonPlansTable.plannedDate greaterEq fromDate)
                    if (toDate != null) pred = pred and (LessonPlansTable.plannedDate lessEq toDate)
                    if (!subject.isNullOrBlank()) pred = pred and (LessonPlansTable.subjectName eq subject)
                    pred
                }.orderBy(LessonPlansTable.plannedDate, SortOrder.DESC).toList()
            }

            val plans = rows.map { row ->
                val unitId = row[LessonPlansTable.curriculumUnitId]
                val unitTitle = if (unitId != null) {
                    dbQuery {
                        CurriculumUnitsTable.selectAll().where {
                            CurriculumUnitsTable.id eq unitId
                        }.singleOrNull()?.get(CurriculumUnitsTable.title)
                    }
                } else null

                SchLpPlanDto(
                    id = row[LessonPlansTable.id].value.toString(),
                    teacherId = row[LessonPlansTable.teacherId].toString(),
                    assignmentId = row[LessonPlansTable.assignmentId].toString(),
                    classId = row[LessonPlansTable.classId].toString(),
                    section = row[LessonPlansTable.section],
                    subjectName = row[LessonPlansTable.subjectName],
                    curriculumUnitId = unitId?.toString(),
                    curriculumUnitTitle = unitTitle,
                    title = row[LessonPlansTable.title],
                    objectives = row[LessonPlansTable.objectives],
                    activities = row[LessonPlansTable.activities],
                    resources = row[LessonPlansTable.resources],
                    assessmentMethod = row[LessonPlansTable.assessmentMethod],
                    durationMinutes = row[LessonPlansTable.durationMinutes],
                    homeworkId = row[LessonPlansTable.homeworkId]?.toString(),
                    plannedDate = row[LessonPlansTable.plannedDate]?.toString(),
                    completedAt = row[LessonPlansTable.completedAt]?.toString(),
                    status = row[LessonPlansTable.status],
                    createdAt = row[LessonPlansTable.createdAt].toString(),
                    updatedAt = row[LessonPlansTable.updatedAt].toString(),
                )
            }

            call.ok(plans, message = if (plans.isEmpty()) "No lesson plans found" else "Lesson plans loaded")
        }
    }
}
