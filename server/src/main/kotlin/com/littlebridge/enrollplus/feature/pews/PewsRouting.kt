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
 *   POST  /api/v1/school/pews/run                    async recompute — returns job_id immediately
 *   GET   /api/v1/school/pews/run/{jobId}            poll job status
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
import com.littlebridge.enrollplus.feature.pews.queue.PewsJobQueue
import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsConfigTable
import com.littlebridge.enrollplus.db.PewsNudgeSeenTable
import com.littlebridge.enrollplus.feature.pews.act.ActModule
import com.littlebridge.enrollplus.feature.pews.act.DraftMessageResponse
import com.littlebridge.enrollplus.feature.pews.act.SendParentMessageResponse
import com.littlebridge.enrollplus.feature.pews.caseworker.CaseFileCodec
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
import java.time.LocalDate
import java.util.UUID

// ── DTOs ────────────────────────────────────────────────────────────────────

@Serializable
data class PewsSignalDto(
    val kind: String,
    val label: String,
    val severity: Int,
    @SerialName("is_leading") val isLeading: Boolean = false,
    @SerialName("evidence_ref") val evidenceRef: String? = null,
)

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
    // PEWS 2.0 expanded fields
    val confidence: Double? = null,
    @SerialName("leading_score") val leadingScore: Int? = null,
    @SerialName("cause_family") val causeFamily: String? = null,
    @SerialName("deltas_json") val deltasJson: String? = null,
    @SerialName("has_open_intervention") val hasOpenIntervention: Boolean = false,
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
    // PEWS 2.0 — managed casework fields
    @SerialName("escalation_level") val escalationLevel: Int = 0,
    @SerialName("sla_days") val slaDays: Int? = null,
    @SerialName("follow_up_date") val followUpDate: String? = null,
    val urgency: String? = null,
    @SerialName("cause_family") val causeFamily: String? = null,
    @SerialName("plan_json") val planJson: String? = null,
    @SerialName("parent_draft_body") val parentDraftBody: String? = null,
    @SerialName("parent_draft_lang") val parentDraftLang: String? = null,
    @SerialName("initiated_by_name") val initiatedByName: String? = null,
    @SerialName("initiated_by_role") val initiatedByRole: String? = null,
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
    // Defaults mirror the shared client DTO (PewsModels.kt) and readConfig()'s
    // fallbacks so a body that omits a field deserializes to a sane value instead
    // of throwing MissingFieldException → 400. Combined with coerceInputValues on
    // the server JSON, the config PUT is now resilient to partial/null bodies.
    @SerialName("use_relative_thresholds") val useRelativeThresholds: Boolean = true,
    @SerialName("attendance_floor_pct") val attendanceFloorPct: Int = 75,
    @SerialName("marks_floor_pct") val marksFloorPct: Int = 40,
    @SerialName("leave_floor_count") val leaveFloorCount: Int = 3,
    @SerialName("run_frequency") val runFrequency: String = "daily",
    @SerialName("ai_narrative_enabled") val aiNarrativeEnabled: Boolean = true,
    @SerialName("parent_share_enabled") val parentShareEnabled: Boolean = false,
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

/** Response of GET /api/v1/school/pews/run/{jobId} — async job status. */
@Serializable
data class PewsJobStatusDto(
    @SerialName("job_id") val jobId: String,
    val status: String,        // queued|processing|completed|failed
    @SerialName("total_items") val totalItems: Int = 0,
    @SerialName("completed_items") val completedItems: Int = 0,
    val result: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("completed_at") val completedAt: String? = null,
)

/** Daily risk distribution entry for the cohort trend. */
@Serializable
data class PewsTrendPointDto(
    @SerialName("run_date") val runDate: String,
    val total: Int,
    val high: Int,
    val medium: Int,
    val watch: Int,
)

/** Response of GET /api/v1/school/pews/trend — cohort risk distribution over time + effectiveness. */
@Serializable
data class PewsEffectivenessTrendDto(
    val points: List<PewsTrendPointDto>,
    val effectiveness: PewsEffectivenessDto,
)

// ── mappers ───────────────────────────────────────────────────────────────

private fun PewsSnapshotService.StoredSnapshot.toDto() = PewsStudentDto(
    studentCode = studentCode, name = studentName, className = className, section = section,
    runDate = runDate, riskScore = riskScore, riskLevel = riskLevel,
    attendancePct = attendancePct, marksPct = marksPct, leaveCount = leaveCount,
    attendanceSlope = attendanceSlope, marksSlope = marksSlope,
    signals = signals.map { PewsSignalDto(it.kind, it.label, it.severity, it.isLeading, it.evidenceRef) },
    aiNarrative = aiNarrative, aiCause = aiCause, aiRecommendation = aiRecommendation,
    aiProviderUsed = aiProviderUsed,
    confidence = confidence, leadingScore = leadingScore,
    causeFamily = causeFamily, deltasJson = deltasJson,
    hasOpenIntervention = hasOpenIntervention,
)

private fun PewsInterventionService.InterventionView.toDto(): PewsInterventionDto {
    val caseFile = planJson?.let { runCatching { CaseFileCodec.parse(it) }.getOrNull() }
    return PewsInterventionDto(
        id = id.toString(), studentCode = studentCode, name = studentName,
        className = className, section = section, ownerUserId = ownerUserId.toString(),
        actionType = actionType, status = status, notes = notes, outcome = outcome,
        openedAt = openedAt, resolvedAt = resolvedAt,
        escalationLevel = escalationLevel, slaDays = slaDays, followUpDate = followUpDate,
        urgency = urgency, causeFamily = causeFamily, planJson = planJson,
        parentDraftBody = caseFile?.parentDraft?.body,
        parentDraftLang = caseFile?.parentDraft?.language,
        initiatedByName = initiatedByName,
        initiatedByRole = initiatedByRole,
    )
}

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
            if (!ok) {
                call.fail("Intervention not found or invalid update", HttpStatusCode.NotFound)
            } else {
                // Re-read the updated row so the client can update its list in place
                // (richer contract: returns the full PewsInterventionDto, not {updated:true}).
                val updated = interventionService.getIntervention(ctx.schoolId, id)
                if (updated == null) call.fail("Intervention not found after update", HttpStatusCode.NotFound)
                else call.ok(updated.toDto(), "Intervention updated")
            }
        }

        // School admin: generate parent draft message for an intervention
        post("/api/v1/school/pews/interventions/{id}/draft-message") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("invalid intervention id"); return@post }
            val language = call.request.queryParameters["lang"] ?: "en"
            val result = ActModule.parentDraftService.generateDraft(ctx.schoolId, id, language)
            if (result.ok) {
                call.ok(
                    DraftMessageResponse(language = result.language, body = result.body ?: ""),
                    "Parent draft generated"
                )
            } else {
                call.fail(result.errorMessage ?: "draft generation failed")
            }
        }

        // School admin: send parent message + mark intervention done
        post("/api/v1/school/pews/interventions/{id}/send-parent-message") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("invalid intervention id"); return@post }
            val adminName = dbQuery {
                AppUsersTable.selectAll().where { AppUsersTable.id eq ctx.userId }
                    .firstOrNull()?.get(AppUsersTable.fullName)
            } ?: "School Admin"
            val result = ActModule.parentDraftService.sendParentMessage(
                schoolId = ctx.schoolId,
                interventionId = id,
                senderId = ctx.userId,
                senderName = adminName,
            )
            if (result.ok) {
                call.ok(
                    SendParentMessageResponse(result.sentCount),
                    "Message sent to ${result.sentCount} parent(s)"
                )
            } else {
                call.fail(result.errorMessage ?: "failed to send message")
            }
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
            // Parse the body; on failure, surface the real reason (and log it)
            // instead of an opaque 400 so a client contract drift is debuggable.
            val parsed = runCatching { call.receive<PewsConfigDto>() }
            val body = parsed.getOrElse { err ->
                org.slf4j.LoggerFactory.getLogger("PewsRouting")
                    .warn("PEWS config PUT rejected for school {}: {}", ctx.schoolId, err.message)
                call.fail("invalid body: ${err.message}")
                return@put
            }
            writeConfig(ctx.schoolId, body)
            call.ok(readConfig(ctx.schoolId), "PEWS config updated")
        }

        post("/api/v1/school/pews/run") {
            val ctx = call.requireSchoolAdmin() ?: return@post
            val jobId = PewsJobQueue.enqueue(ctx.schoolId, ctx.userId)
            call.ok(
                mapOf("job_id" to jobId.toString(), "status" to "queued"),
                "PEWS recompute queued"
            )
        }

        get("/api/v1/school/pews/run/{jobId}") {
            val ctx = call.requireSchoolAdmin() ?: return@get
            val jobId = call.parameters["jobId"]?.let {
                runCatching { java.util.UUID.fromString(it) }.getOrNull()
            } ?: run { call.fail("invalid job id"); return@get }

            val status = PewsJobQueue.status(jobId)
            if (status == null) {
                call.fail("job not found", io.ktor.http.HttpStatusCode.NotFound)
            } else {
                call.ok(
                    PewsJobStatusDto(
                        jobId = status.jobId.toString(),
                        status = status.status,
                        totalItems = status.totalItems,
                        completedItems = status.completedItems,
                        result = status.result,
                        createdAt = status.createdAt,
                        completedAt = status.completedAt,
                    ),
                    "Job status"
                )
            }
        }

        get("/api/v1/school/pews/trend") {
            val ctx = call.requireSchoolAdmin() ?: return@get
            val days = call.request.queryParameters["days"]?.toIntOrNull()?.coerceIn(7, 90) ?: 30
            val trend = snapshotService.cohortTrend(ctx.schoolId, days)
            val eff = interventionService.effectiveness(ctx.schoolId)
            call.ok(
                PewsEffectivenessTrendDto(
                    points = trend.map { (date, total, high, medium, watch) ->
                        PewsTrendPointDto(date, total, high, medium, watch)
                    },
                    effectiveness = PewsEffectivenessDto(
                        eff.total, eff.open, eff.done, eff.dismissed,
                        eff.improved, eff.unchanged, eff.worsened,
                    ),
                ),
                "PEWS trend"
            )
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

            // Check if parent already saw this nudge for the current run date
            val runDate = LocalDate.parse(snap.runDate)
            val alreadySeen = dbQuery {
                PewsNudgeSeenTable.selectAll().where {
                    (PewsNudgeSeenTable.childId eq childId) and
                        (PewsNudgeSeenTable.parentId eq uid) and
                        (PewsNudgeSeenTable.snapshotRunDate eq runDate)
                }.limit(1).any()
            }
            if (alreadySeen) {
                call.ok(
                    PewsParentNudgeDto(
                        childName = childName, show = false, headline = "", message = "",
                        attendancePct = snap.attendancePct, actions = emptyList()),
                    "Nudge already seen"
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

        // Parent: acknowledge nudge (dismiss after viewing)
        post("/api/v1/parent/pews/{childId}/ack") {
            val uid = call.principalUserUuid()
                ?: run { call.fail("invalid token", HttpStatusCode.Unauthorized); return@post }
            val childId = call.parameters["childId"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                ?: run { call.fail("invalid child id"); return@post }

            // validate child belongs to this parent
            val child = dbQuery {
                ChildrenTable.selectAll().where {
                    (ChildrenTable.id eq EntityID(childId, ChildrenTable)) and
                        (ChildrenTable.parentId eq uid)
                }.singleOrNull()
            } ?: run { call.fail("Child not found", HttpStatusCode.NotFound, "CHILD_NOT_FOUND"); return@post }

            val schoolId = child[ChildrenTable.schoolId]
            val studentCode = child[ChildrenTable.studentCode]
            if (schoolId == null || studentCode.isNullOrBlank()) {
                call.ok(mapOf("acknowledged" to true), "No nudge to ack")
                return@post
            }

            // Get the latest snapshot run date for this school
            val runDate = snapshotService.latestRunDate(schoolId)
            if (runDate == null) {
                call.ok(mapOf("acknowledged" to true), "No snapshot to ack")
                return@post
            }

            // Upsert the seen record
            val now = Instant.now()
            dbQuery {
                val existing = PewsNudgeSeenTable.selectAll().where {
                    (PewsNudgeSeenTable.childId eq childId) and
                        (PewsNudgeSeenTable.parentId eq uid) and
                        (PewsNudgeSeenTable.snapshotRunDate eq runDate)
                }.singleOrNull()
                if (existing == null) {
                    PewsNudgeSeenTable.insert {
                        it[PewsNudgeSeenTable.childId] = childId
                        it[PewsNudgeSeenTable.parentId] = uid
                        it[PewsNudgeSeenTable.snapshotRunDate] = runDate
                        it[PewsNudgeSeenTable.seenAt] = now
                    }
                }
            }
            call.ok(mapOf("acknowledged" to true), "Nudge acknowledged")
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
