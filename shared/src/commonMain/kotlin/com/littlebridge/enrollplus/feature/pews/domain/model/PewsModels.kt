/*
 * File: PewsModels.kt
 * Module: feature.pews.domain.model
 *
 * Client DTOs for the Predictive Early Warning System (PEWS). These MIRROR the
 * server DTOs in server feature.pews.PewsRouting.kt exactly (same @SerialName),
 * so the same JSON decodes on both sides.
 *
 * Honesty (RA-S10 / LAW 6): every value here is a real, deterministic snapshot
 * served by GET /api/v1/school/pews/(routes). The AI fields (aiNarrative/aiCause/
 * aiRecommendation) are explanations of the provided signal bundle and may be
 * null when the LLM is unavailable — the UI must degrade gracefully and never
 * invent a student, a number, or a recommendation.
 *
 * Server routes mirrored:
 *   GET   /api/v1/school/pews/cohort?min_level=
 *   GET   /api/v1/school/pews/student/{code}
 *   GET   /api/v1/school/pews/interventions?status=
 *   PATCH /api/v1/school/pews/interventions/{id}
 *   GET   /api/v1/school/pews/effectiveness
 *   GET   /api/v1/school/pews/config
 *   PUT   /api/v1/school/pews/config
 *   POST  /api/v1/school/pews/run
 *   GET   /api/v1/teacher/pews/students
 *   GET   /api/v1/teacher/pews/interventions?status=
 *   PATCH /api/v1/teacher/pews/interventions/{id}
 *   GET   /api/v1/parent/pews/{childId}
 */
package com.littlebridge.enrollplus.feature.pews.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A single deterministic reason a student surfaced (e.g. low attendance). */
@Serializable
data class PewsSignalDto(
    val kind: String,
    val label: String,
    val severity: Int,
)

/** One student's risk snapshot for a given run date. */
@Serializable
data class PewsStudentDto(
    @SerialName("student_code") val studentCode: String,
    val name: String,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("run_date") val runDate: String,
    @SerialName("risk_score") val riskScore: Int,
    @SerialName("risk_level") val riskLevel: String,   // watch | medium | high
    @SerialName("attendance_pct") val attendancePct: Int? = null,
    @SerialName("marks_pct") val marksPct: Int? = null,
    @SerialName("leave_count") val leaveCount: Int = 0,
    @SerialName("attendance_slope") val attendanceSlope: Double? = null,
    @SerialName("marks_slope") val marksSlope: Double? = null,
    val signals: List<PewsSignalDto> = emptyList(),
    // AI explanation of the signal bundle — null when the LLM is unavailable.
    @SerialName("ai_narrative") val aiNarrative: String? = null,
    @SerialName("ai_cause") val aiCause: String? = null,
    @SerialName("ai_recommendation") val aiRecommendation: String? = null,
    @SerialName("ai_provider_used") val aiProviderUsed: String? = null,
)

/** The whole at-risk cohort for a school plus band counts. */
@Serializable
data class PewsCohortDto(
    @SerialName("run_date") val runDate: String? = null,
    val total: Int = 0,
    val high: Int = 0,
    val medium: Int = 0,
    val watch: Int = 0,
    val students: List<PewsStudentDto> = emptyList(),
    @SerialName("ai_enabled") val aiEnabled: Boolean = false,
)

/** A single student's current snapshot + history (newest first). */
@Serializable
data class PewsStudentDetailDto(
    val current: PewsStudentDto? = null,
    val history: List<PewsStudentDto> = emptyList(),
)

/** An open/closed intervention tracked against an at-risk student. */
@Serializable
data class PewsInterventionDto(
    val id: String,
    @SerialName("student_code") val studentCode: String,
    val name: String,
    @SerialName("class_name") val className: String,
    val section: String,
    @SerialName("owner_user_id") val ownerUserId: String,
    @SerialName("action_type") val actionType: String,
    val status: String,                 // open | in_progress | done | dismissed
    val notes: String? = null,
    val outcome: String? = null,        // improved | unchanged | worsened
    @SerialName("opened_at") val openedAt: String,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    // PEWS 2.0 — managed casework fields
    @SerialName("escalation_level") val escalationLevel: Int = 0,
    @SerialName("sla_days") val slaDays: Int? = null,
    @SerialName("follow_up_date") val followUpDate: String? = null,
    val urgency: String? = null,       // low | medium | high
    @SerialName("cause_family") val causeFamily: String? = null,
    @SerialName("plan_json") val planJson: String? = null,
    @SerialName("parent_draft_body") val parentDraftBody: String? = null,
    @SerialName("parent_draft_lang") val parentDraftLang: String? = null,
)

/** Body for PATCH .../interventions/{id} — all fields optional (partial update). */
@Serializable
data class UpdateInterventionRequest(
    val status: String? = null,
    val notes: String? = null,
    val outcome: String? = null,
    @SerialName("action_type") val actionType: String? = null,
)

/** Response from POST .../interventions/{id}/draft-message — vernacular parent message draft. */
@Serializable
data class ParentDraftDto(
    val language: String,
    val body: String,
)

/** Response from POST .../interventions/{id}/send-parent-message — confirms message was sent. */
@Serializable
data class SendParentMessageDto(
    @SerialName("sent_count") val sentCount: Int,
)

/** Aggregate effectiveness counts (the LEARN loop). */
@Serializable
data class PewsEffectivenessDto(
    val total: Int = 0,
    val open: Int = 0,
    val done: Int = 0,
    val dismissed: Int = 0,
    val improved: Int = 0,
    val unchanged: Int = 0,
    val worsened: Int = 0,
)

/** School-level PEWS tuning. */
@Serializable
data class PewsConfigDto(
    @SerialName("use_relative_thresholds") val useRelativeThresholds: Boolean = true,
    @SerialName("attendance_floor_pct") val attendanceFloorPct: Int = 75,
    @SerialName("marks_floor_pct") val marksFloorPct: Int = 40,
    @SerialName("leave_floor_count") val leaveFloorCount: Int = 3,
    @SerialName("run_frequency") val runFrequency: String = "daily",
    @SerialName("ai_narrative_enabled") val aiNarrativeEnabled: Boolean = true,
    @SerialName("parent_share_enabled") val parentShareEnabled: Boolean = false,
)

/** A gentle, label-free parent nudge — shown only when there's a real concern. */
@Serializable
data class PewsParentNudgeDto(
    @SerialName("child_name") val childName: String,
    val show: Boolean = false,
    val headline: String = "",
    val message: String = "",
    @SerialName("attendance_pct") val attendancePct: Int? = null,
    val actions: List<PewsParentActionDto> = emptyList(),
)

@Serializable
data class PewsParentActionDto(
    val label: String,
    @SerialName("deep_link") val deepLink: String,
)

/** Response of POST /api/v1/school/pews/run — { "at_risk": N }. */
@Serializable
data class PewsRunResultDto(
    @SerialName("at_risk") val atRisk: Int = 0,
)

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
    val points: List<PewsTrendPointDto> = emptyList(),
    val effectiveness: PewsEffectivenessDto = PewsEffectivenessDto(),
)
