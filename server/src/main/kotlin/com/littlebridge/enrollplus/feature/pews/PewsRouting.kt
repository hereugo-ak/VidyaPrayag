/*
 * File: PewsRouting.kt
 * Module: feature.pews
 *
 * The PEWS API across all three roles. Every endpoint is scoped from the JWT
 * (never the request body); teacher endpoints are class-scoped; the parent
 * endpoint is child-scoped AND gated behind pews_config.parent_share_enabled.
 *
 * SCHOOL ADMIN (requireSchoolAdmin)
 *   GET   /api/v1/school/pews/cohort                 whole-school at-risk cohort (+AI fields)
 *   GET   /api/v1/school/pews/student/{code}         one student detail + history
 *   GET   /api/v1/school/pews/interventions          all interventions (filter status/owner)
 *   PATCH /api/v1/school/pews/interventions/{id}     update status/notes/outcome (Learn)
 *   GET   /api/v1/school/pews/effectiveness          outcome rollup (Learn)
 *   GET   /api/v1/school/pews/config                 read per-school tuning
 *   PUT   /api/v1/school/pews/config                 update tuning (thresholds, flags)
 *   POST  /api/v1/school/pews/run                    manual recompute for THIS school
 *
 * TEACHER (requireTeacherContext)
 *   GET   /api/v1/teacher/pews/students              own-class at-risk students (+AI)
 *   GET   /api/v1/teacher/pews/interventions         interventions owned by me
 *   PATCH /api/v1/teacher/pews/interventions/{id}    update my intervention (Learn)
 *
 * PARENT (child-scoped; only if parent_share_enabled)
 *   GET   /api/v1/parent/pews/{childId}              gentle, non-clinical nudge for own child
 *
 * Honesty LAW 6: numbers come from the deterministic snapshot; the parent view
 * is deliberately label-free (no "risk" word, no score) — it surfaces a gentle
 * prompt + deep links to the existing attendance / message-teacher screens.
 */
package com.littlebridge.enrollplus.feature.pews

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireTeacherContext
import com.littlebridge.enrollplus.core.teacherAssignmentsFor
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsConfigTable
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

// ── DTOs ────────────────────────────────────────────────────────────────────

@Serializable
data class PewsSignalDto(val kind: String, val label: String, val severity: Int)

@Serializable
data class PewsStudentDto(
    @SerialName("student_code") val studentCode: String,
    val name: String,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("run_date") val runDate: String,
    @SerialName("risk_score") val riskScore: Int,
    @SerialName("risk_level") val riskLevel: String,
    @SerialName("attendance_pct") val attendancePct: Int?,
    @SerialName("marks_pct") val marksPct: Int?,
    @SerialName("leave_count") val leaveCount: Int,
    @SerialName("attendance_slope") val attendanceSlope: Double?,
    @SerialName("marks_slope") val marksSlope: Double?,
    val signals: List<PewsSignalDto>,
    @SerialName("ai_narrative") val aiNarrative: String?,
    @SerialName("ai_cause") val aiCause: String?,
    @SerialName("ai_recommendation") val aiRecommendation: String?,
    @SerialName("ai_provider_used") val aiProviderUsed: String?,
)

@Serializable
data class PewsCohortDto(
    @SerialName("run_date") val runDate: String?,
    val total: Int,
    val high: Int,
    val medium: Int,
    val watch: Int,
    val students: List<PewsStudentDto>,
    @SerialName("ai_enabled") val aiEnabled: Boolean,
)

@Serializable
data class PewsStudentDetailDto(
    val current: PewsStudentDto?,
    val history: List<PewsStudentDto>,
)

@Serializable
data class PewsInterventionDto(
    val id: String,
    @SerialName("student_code") val studentCode: String,
    val name: String,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("action_type") val actionType: String,
    val status: String,
    val notes: String?,
    val outcome: String?,
    @SerialName("opened_at") val openedAt: String,
    @SerialName("resolved_at") val resolvedAt: String?,
)

@Serializable
data class UpdateInterventionRequest(
    val status: String? = null,
    val notes: String? = null,
    val outcome: String? = null,
    @SerialName("action_type") val actionType: String? = null,
)

@Serializable
data class PewsEffectivenessDto(
    val total: Int, val open: Int, val done: Int, val dismissed: Int,
    val improved: Int, val unchanged: Int, val worsened: Int,
)

@Serializable
data class PewsConfigDto(
    @SerialName("use_relative_thresholds") val useRelativeThresholds: Boolean,
    @SerialName("attendance_floor_pct") val attendanceFloorPct: Int,
    @SerialName("marks_floor_pct") val marksFloorPct: Int,
    @SerialName("leave_floor_count") val leaveFloorCount: Int,
    @SerialName("run_frequency") val runFrequency: String,
    @SerialName("ai_narrative_enabled") val aiNarrativeEnabled: Boolean,
    @SerialName("parent_share_enabled") val parentShareEnabled: Boolean,
)

@Serializable
data class PewsParentNudgeDto(
    @SerialName("child_name") val childName: String,
    val show: Boolean,                 // false → parent sees nothing (no concern, or sharing off)
    val headline: String,
    val message: String,
    @SerialName("attendance_pct") val attendancePct: Int?,
    val actions: List<PewsParentActionDto>,
)

@Serializable
data class PewsParentActionDto(val label: String, @SerialName("deep_link") val deepLink: String)

// ── mappers ───────────────────────────────────────────────────────────────

private fun PewsSnapshotService.StoredSnapshot.toDto() = PewsStudentDto(
    studentCode = studentCode, name = studentName, className = className, section = section,
    runDate = runDate, riskScore = riskScore, riskLevel = riskLevel,
    attendancePct = attendancePct, marksPct = marksPct, leaveCount = leaveCount,
    attendanceSlope = attendanceSlope, marksSlope = marksSlope,
    signals = signals.map { PewsSignalDto(it.kind, it.label, it.severity) },
    aiNarrative = aiNarrative, aiCause = aiCause, aiRecommendation = aiRecommendation,
    aiProviderUsed = aiProviderUsed,
)

private fun PewsInterventionService.InterventionView.toDto() = PewsInterventionDto(
    id = id.toString(), studentCode = studentCode, name = studentName,
    className = className, section = section, ownerUserId = ownerUserId.toString(),
    actionType = actionType, status = status, notes = notes, outcome = outcome,
    openedAt = openedAt, resolvedAt = resolvedAt,
)

// ── config helpers ──────────────────────────────────────────────────────────

private suspend fun readConfig(schoolId: UUID): PewsConfigDto = dbQuery {
    val row = PewsConfigTable.selectAll().where {
        PewsConfigTable.id eq EntityID(schoolId, PewsConfigTable)
    }.singleOrNull()
    PewsConfigDto(
        useRelativeThresholds = row?.get(PewsConfigTable.useRelativeThresholds) ?: true,
        attendanceFloorPct = row?.get(PewsConfigTable.attendanceFloorPct) ?: 75,
        marksFloorPct = row?.get(PewsConfigTable.marksFloorPct) ?: 40,
        leaveFloorCount = row?.get(PewsConfigTable.leaveFloorCount) ?: 3,
        runFrequency = row?.get(PewsConfigTable.runFrequency) ?: "daily",
        aiNarrativeEnabled = row?.get(PewsConfigTable.aiNarrativeEnabled) ?: true,
        parentShareEnabled = row?.get(PewsConfigTable.parentShareEnabled) ?: false,
    )
}

private suspend fun writeConfig(schoolId: UUID, c: PewsConfigDto) = dbQuery {
    val now = Instant.now()
    val existing = PewsConfigTable.selectAll().where {
        PewsConfigTable.id eq EntityID(schoolId, PewsConfigTable)
    }.singleOrNull()
    if (existing == null) {
        PewsConfigTable.insert {
            it[id] = EntityID(schoolId, PewsConfigTable)
            it[useRelativeThresholds] = c.useRelativeThresholds
            it[attendanceFloorPct] = c.attendanceFloorPct.coerceIn(0, 100)
            it[marksFloorPct] = c.marksFloorPct.coerceIn(0, 100)
            it[leaveFloorCount] = c.leaveFloorCount.coerceIn(0, 100)
            it[runFrequency] = if (c.runFrequency == "weekly") "weekly" else "daily"
            it[aiNarrativeEnabled] = c.aiNarrativeEnabled
            it[parentShareEnabled] = c.parentShareEnabled
            it[updatedAt] = now
        }
    } else {
        PewsConfigTable.update({ PewsConfigTable.id eq EntityID(schoolId, PewsConfigTable) }) {
            it[useRelativeThresholds] = c.useRelativeThresholds
            it[attendanceFloorPct] = c.attendanceFloorPct.coerceIn(0, 100)
            it[marksFloorPct] = c.marksFloorPct.coerceIn(0, 100)
            it[leaveFloorCount] = c.leaveFloorCount.coerceIn(0, 100)
            it[runFrequency] = if (c.runFrequency == "weekly") "weekly" else "daily"
            it[aiNarrativeEnabled] = c.aiNarrativeEnabled
            it[parentShareEnabled] = c.parentShareEnabled
            it[updatedAt] = now
        }
    }
}

private fun List<PewsSnapshotService.StoredSnapshot>.toCohort(runDate: String?, aiEnabled: Boolean) =
    PewsCohortDto(
        runDate = runDate,
        total = size,
        high = count { it.riskLevel == "high" },
        medium = count { it.riskLevel == "medium" },
        watch = count { it.riskLevel == "watch" },
        students = map { it.toDto() },
        aiEnabled = aiEnabled,
    )

// ── Routes ────────────────────────────────────────────────────────────────

fun Route.pewsRouting() {
    val snapshotService = PewsSnapshotService()
    val interventionService = PewsInterventionService()

    authenticate("jwt") {

        // ============================ SCHOOL ADMIN ============================

        get("/api/v1/school/pews/cohort") {
            val ctx = call.requireSchoolAdmin() ?: return@get
            val minLevel = call.request.queryParameters["min_level"] ?: "watch"
            val cohort = snapshotService.cohort(ctx.schoolId, minLevel = minLevel)
            val enriched = snapshotService.enrichIdentity(ctx.schoolId, cohort)
            val cfg = readConfig(ctx.schoolId)
            call.ok(enriched.toCohort(enriched.firstOrNull()?.runDate, cfg.aiNarrativeEnabled),
                "PEWS cohort")
        }

        get("/api/v1/school/pews/student/{code}") {
            val ctx = call.requireSchoolAdmin() ?: return@get
            val code = call.parameters["code"] ?: run { call.fail("student code required"); return@get }
            val current = snapshotService.studentSnapshot(ctx.schoolId, code)
            val history = snapshotService.studentHistory(ctx.schoolId, code)
            val enrichedCurrent = current?.let { snapshotService.enrichIdentity(ctx.schoolId, listOf(it)).first() }
            val enrichedHistory = snapshotService.enrichIdentity(ctx.schoolId, history)
            call.ok(
                PewsStudentDetailDto(
                    current = enrichedCurrent?.toDto(),
                    history = enrichedHistory.map { it.toDto() },
                ),
                "PEWS student detail"
            )
        }

        get("/api/v1/school/pews/interventions") {
            val ctx = call.requireSchoolAdmin() ?: return@get
            val status = call.request.queryParameters["status"]
            val rows = interventionService.listInterventions(ctx.schoolId, status = status)
            call.ok(rows.map { it.toDto() }, "PEWS interventions")
        }

        patch("/api/v1/school/pews/interventions/{id}") {
            val ctx = call.requireSchoolAdmin() ?: return@patch
            val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("invalid intervention id"); return@patch }
            val actor = call.principalUserUuid() ?: run { call.fail("invalid token", HttpStatusCode.Unauthorized); return@patch }
            val body = runCatching { call.receive<UpdateInterventionRequest>() }.getOrNull()
                ?: run { call.fail("invalid body"); return@patch }
            val ok = interventionService.updateIntervention(
                ctx.schoolId, id, actor, body.status, body.notes, body.outcome, body.actionType)
            if (!ok) call.fail("Intervention not found or invalid update", HttpStatusCode.NotFound)
            else call.ok(mapOf("updated" to true), "Intervention updated")
        }

        get("/api/v1/school/pews/effectiveness") {
            val ctx = call.requireSchoolAdmin() ?: return@get
            val e = interventionService.effectiveness(ctx.schoolId)
            call.ok(
                PewsEffectivenessDto(e.total, e.open, e.done, e.dismissed,
                    e.improved, e.unchanged, e.worsened),
                "PEWS effectiveness"
            )
        }

        get("/api/v1/school/pews/config") {
            val ctx = call.requireSchoolAdmin() ?: return@get
            call.ok(readConfig(ctx.schoolId), "PEWS config")
        }

        put("/api/v1/school/pews/config") {
            val ctx = call.requireSchoolAdmin() ?: return@put
            val body = runCatching { call.receive<PewsConfigDto>() }.getOrNull()
                ?: run { call.fail("invalid body"); return@put }
            writeConfig(ctx.schoolId, body)
            call.ok(readConfig(ctx.schoolId), "PEWS config updated")
        }

        post("/api/v1/school/pews/run") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val count = PewsDailyJob.runSchool(ctx.schoolId)
            call.ok(mapOf("at_risk" to count), "PEWS recompute complete")
        }

        // ============================== TEACHER ===============================

        get("/api/v1/teacher/pews/students") {
            val ctx = call.requireTeacherContext() ?: return@get
            // own classes (assignment-scoped); admins acting as teacher see all
            val assignments = teacherAssignmentsFor(ctx)
            if (assignments.isEmpty()) {
                call.ok(emptyList<PewsStudentDto>(), "No assigned classes")
                return@get
            }
            val classKeys = assignments.map { it.className.lowercase() to it.section.lowercase() }.toSet()
            val cohort = snapshotService.cohort(ctx.schoolId, minLevel = "watch")
            val enriched = snapshotService.enrichIdentity(ctx.schoolId, cohort)
            val mine = enriched.filter {
                (it.className.lowercase() to it.section.lowercase()) in classKeys
            }
            call.ok(mine.map { it.toDto() }, "My at-risk students")
        }

        get("/api/v1/teacher/pews/interventions") {
            val ctx = call.requireTeacherContext() ?: return@get
            val status = call.request.queryParameters["status"]
            val rows = interventionService.listInterventions(
                ctx.schoolId, ownerUserId = ctx.userId, status = status)
            call.ok(rows.map { it.toDto() }, "My interventions")
        }

        patch("/api/v1/teacher/pews/interventions/{id}") {
            val ctx = call.requireTeacherContext() ?: return@patch
            val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("invalid intervention id"); return@patch }
            val body = runCatching { call.receive<UpdateInterventionRequest>() }.getOrNull()
                ?: run { call.fail("invalid body"); return@patch }
            // ownership: a teacher may only touch interventions assigned to them
            val owned = interventionService.listInterventions(ctx.schoolId, ownerUserId = ctx.userId)
                .any { it.id == id }
            if (!owned && ctx.role !in setOf("school_admin", "admin")) {
                call.fail("This intervention is not assigned to you", HttpStatusCode.Forbidden, "NOT_OWNER")
                return@patch
            }
            val ok = interventionService.updateIntervention(
                ctx.schoolId, id, ctx.userId, body.status, body.notes, body.outcome, body.actionType)
            if (!ok) call.fail("Intervention not found or invalid update", HttpStatusCode.NotFound)
            else call.ok(mapOf("updated" to true), "Intervention updated")
        }

        // ============================== PARENT ================================

        get("/api/v1/parent/pews/{childId}") {
            val uid = call.principalUserUuid()
                ?: run { call.fail("invalid token", HttpStatusCode.Unauthorized); return@get }
            val childId = call.parameters["childId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("invalid child id"); return@get }

            // resolve the child → must belong to this parent; carry school + code
            val child = dbQuery {
                ChildrenTable.selectAll().where {
                    (ChildrenTable.id eq EntityID(childId, ChildrenTable)) and
                        (ChildrenTable.parentId eq uid)
                }.singleOrNull()
            } ?: run { call.fail("Child not found", HttpStatusCode.NotFound, "CHILD_NOT_FOUND"); return@get }

            val schoolId = child[ChildrenTable.schoolId]
            val studentCode = child[ChildrenTable.studentCode]
            val childName = child[ChildrenTable.childName]

            // sharing gate
            val shareEnabled = schoolId?.let { readConfig(it).parentShareEnabled } ?: false
            if (schoolId == null || studentCode.isNullOrBlank() || !shareEnabled) {
                call.ok(
                    PewsParentNudgeDto(
                        childName = childName, show = false, headline = "", message = "",
                        attendancePct = null, actions = emptyList()),
                    "No nudge"
                )
                return@get
            }

            val snap = snapshotService.studentSnapshot(schoolId, studentCode)
            // Parent sees a nudge ONLY when there's a real concern. No "risk"
            // word, no score — a gentle, supportive prompt with helpful actions.
            if (snap == null || snap.riskLevel == "watch") {
                call.ok(
                    PewsParentNudgeDto(
                        childName = childName, show = false, headline = "", message = "",
                        attendancePct = snap?.attendancePct, actions = emptyList()),
                    "No nudge"
                )
                return@get
            }

            val message = buildParentMessage(childName, snap)
            call.ok(
                PewsParentNudgeDto(
                    childName = childName,
                    show = true,
                    headline = "A gentle check-in about $childName",
                    message = message,
                    attendancePct = snap.attendancePct,
                    actions = listOf(
                        PewsParentActionDto("View attendance", "/parent/child/$childId/attendance"),
                        PewsParentActionDto("Message teacher", "/parent/messages"),
                    ),
                ),
                "Parent nudge"
            )
        }
    }
}

/** Supportive, non-clinical parent message grounded in the deterministic data. */
private fun buildParentMessage(childName: String, snap: PewsSnapshotService.StoredSnapshot): String {
    val first = childName.trim().split(" ").firstOrNull() ?: childName
    val bits = mutableListOf<String>()
    snap.attendancePct?.let { if (it < 75) bits.add("$first's attendance has dipped recently") }
    if (snap.leaveCount >= 3) bits.add("there have been a few leave days")
    snap.marksPct?.let { if (it < 40) bits.add("some recent assessments were challenging") }
    val lead = if (bits.isEmpty()) "We'd love to partner with you on $first's progress."
    else bits.joinToString(", ").replaceFirstChar { it.uppercase() } + "."
    return "$lead A quick chat with the class teacher can help us support $first together."
}
