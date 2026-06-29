/*
 * File: ManagedCaseworkService.kt
 * Module: feature.pews.act
 *
 * PEWS 2.0 — Tier 3 Act: managed casework.
 *
 * Upgrades the v1 "open a task + notify" to:
 *   1. Sequenced plan persistence — the Case File's plan[] is stored as JSON
 *      on the intervention row, with SLA days and urgency.
 *   2. SLA + escalation ladder — overdue high-urgency cases escalate to admin.
 *   3. Follow-up checkpoint — auto-schedules a re-check date.
 *   4. Calendar-aware suppression — respects skip_reason from the Case File.
 *
 * The escalation ladder (deterministic, config-driven):
 *   open ──(SLA_1 elapsed, no progress)──► remind owner (push)
 *        ──(SLA_2 elapsed)──────────────► escalate to admin + co-own
 *        ──(worsened on next snapshot)───► bump urgency, surface top-of-cohort
 *
 * Kill-switched under module name "act".
 *
 * Spec: PEWS_2.0_AGENTIC_REDESIGN.md §7
 */
package com.littlebridge.enrollplus.feature.pews.act

import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsInterventionsTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.pews.PewsSnapshot
import com.littlebridge.enrollplus.feature.pews.caseworker.CaseFile
import com.littlebridge.enrollplus.feature.pews.caseworker.CaseFileCodec
import com.littlebridge.enrollplus.feature.pews.caseworker.CaseworkerService
import com.littlebridge.enrollplus.feature.pews.core.KillSwitchGuard
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class ManagedCaseworkService {
    private val log = LoggerFactory.getLogger("ManagedCaseworkService")

    // ── SLA defaults (days) ────────────────────────────────────────────────

    private val slaByUrgency = mapOf(
        "high" to 2,
        "medium" to 5,
        "low" to 10,
    )

    // ── Escalation thresholds (days beyond SLA) ───────────────────────────

    private val REMIND_AFTER_SLA_MULTIPLE = 1   // remind at SLA elapsed
    private val ESCALATE_AFTER_SLA_MULTIPLE = 2  // escalate at 2× SLA

    // ── Main entry: act on caseworker results ──────────────────────────────

    /**
     * For each caseworker result, open or update an intervention with the
     * sequenced plan, SLA, urgency, and follow-up date. Suppresses cases
     * where the Case File has a skip_reason (calendar-aware).
     *
     * Returns the number of interventions opened or updated.
     */
    suspend fun actOnCaseFiles(
        schoolId: UUID,
        snapshots: List<PewsSnapshot>,
        caseResults: List<CaseworkerService.CaseworkerResult>,
    ): Int {
        KillSwitchGuard.require("act")

        val snapshotByCode = snapshots.associateBy { it.studentCode }
        var acted = 0

        for (result in caseResults) {
            val snap = snapshotByCode[result.studentCode] ?: continue
            val caseFile = result.caseFile

            // Calendar-aware suppression: if the Case File says skip, don't open
            if (caseFile.skipReason != null) {
                log.info("Act: suppressing intervention for {} — skip_reason: {}",
                    result.studentCode, caseFile.skipReason)
                continue
            }

            runCatching { openOrUpdateIntervention(schoolId, snap, caseFile) }
                .onSuccess { if (it) acted++ }
                .onFailure { log.warn("Act: failed for {}: {}", result.studentCode, it.message) }
        }

        log.info("Act: school={} → {} interventions opened/updated", schoolId, acted)
        return acted
    }

    /**
     * Fallback: act on snapshots without Case Files (v1-compatible path).
     * Opens interventions for medium+ risk students with default SLA.
     */
    suspend fun actOnSnapshots(
        schoolId: UUID,
        snapshots: List<PewsSnapshot>,
        minLevel: String = "medium",
    ): Int {
        KillSwitchGuard.require("act")

        val levelRank = mapOf("watch" to 0, "medium" to 1, "high" to 2)
        val minRank = levelRank[minLevel] ?: 1
        val targets = snapshots.filter { (levelRank[it.riskLevel] ?: 0) >= minRank }

        var acted = 0
        for (snap in targets) {
            runCatching { openOrUpdateIntervention(schoolId, snap, null) }
                .onSuccess { if (it) acted++ }
                .onFailure { log.warn("Act: failed for {}: {}", snap.studentCode, it.message) }
        }

        log.info("Act (v1 fallback): school={} → {} interventions opened/updated", schoolId, acted)
        return acted
    }

    // ── Intervention open/update ───────────────────────────────────────────

    private suspend fun openOrUpdateIntervention(
        schoolId: UUID,
        snap: PewsSnapshot,
        caseFile: CaseFile?,
    ): Boolean {
        // Check if there's already an open intervention for this student
        val existing = dbQuery {
            PewsInterventionsTable.selectAll().where {
                (PewsInterventionsTable.schoolId eq schoolId) and
                    (PewsInterventionsTable.studentCode eq snap.studentCode) and
                    (PewsInterventionsTable.status inList listOf("open", "in_progress"))
            }.singleOrNull()
        }

        if (existing != null) {
            // Update the existing intervention with Case File data if available
            if (caseFile != null) {
                val planJson = CaseFileCodec.encode(caseFile)
                val slaDays = caseFile.plan.firstOrNull()?.slaDays ?: slaByUrgency[caseFile.urgency] ?: 5
                val followUp = computeFollowUpDate(slaDays)

                dbQuery {
                    PewsInterventionsTable.update({
                        (PewsInterventionsTable.id eq existing[PewsInterventionsTable.id])
                    }) {
                        it[PewsInterventionsTable.planJson] = planJson
                        it[PewsInterventionsTable.slaDays] = slaDays
                        it[PewsInterventionsTable.urgency] = caseFile.urgency
                        it[PewsInterventionsTable.causeFamily] = snap.causeFamily
                        it[PewsInterventionsTable.followUpDate] = followUp
                    }
                }
                log.debug("Act: updated existing intervention for {} with Case File", snap.studentCode)
            }
            return true  // updated an existing intervention
        }

        // Open a new intervention
        val owner = resolveOwner(schoolId, snap.className, snap.section)
        val admins = schoolAdminIds(schoolId)
        val ownerId = owner ?: admins.firstOrNull() ?: return false

        val rawAction = caseFile?.plan?.firstOrNull()?.action
            ?: defaultActionFor(snap)
        val actionType = rawAction.take(32)
        val urgency = caseFile?.urgency ?: when (snap.riskLevel) {
            "high" -> "high"
            "medium" -> "medium"
            else -> "low"
        }
        val slaDays = caseFile?.plan?.firstOrNull()?.slaDays ?: slaByUrgency[urgency] ?: 5
        val planJson = caseFile?.let { CaseFileCodec.encode(it) }
        val followUp = computeFollowUpDate(slaDays)
        val now = Instant.now()

        // Get snapshot ID
        val snapshotId = dbQuery {
            com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable.selectAll().where {
                (com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable.schoolId eq schoolId) and
                    (com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable.studentCode eq snap.studentCode) and
                    (com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable.runDate eq snap.runDate)
            }.singleOrNull()?.get(com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable.id)?.value
        }

        dbQuery {
            PewsInterventionsTable.insert {
                it[PewsInterventionsTable.schoolId] = schoolId
                it[PewsInterventionsTable.studentCode] = snap.studentCode
                it[PewsInterventionsTable.snapshotId] = snapshotId
                it[PewsInterventionsTable.ownerUserId] = ownerId
                it[PewsInterventionsTable.actionType] = actionType
                it[PewsInterventionsTable.status] = "open"
                it[PewsInterventionsTable.openedAt] = now
                it[PewsInterventionsTable.createdAt] = now
                it[PewsInterventionsTable.planJson] = planJson
                it[PewsInterventionsTable.slaDays] = slaDays
                it[PewsInterventionsTable.urgency] = urgency
                it[PewsInterventionsTable.causeFamily] = snap.causeFamily
                it[PewsInterventionsTable.followUpDate] = followUp
                it[PewsInterventionsTable.escalationLevel] = 0
            }
        }

        // Notify the owner
        notifyOwner(snap, ownerId, urgency, caseFile)
        return true
    }

    // ── Escalation ladder ──────────────────────────────────────────────────

    /**
     * Check all open interventions for SLA breaches and escalate as needed.
     * Should be called once per day (e.g. from the daily job).
     *
     * Ladder:
     *   0 (normal) → 1 (reminded) at SLA elapsed: push notification to owner
     *   1 (reminded) → 2 (escalated) at 2× SLA: notify admin + co-own
     */
    suspend fun runEscalationSweep(schoolId: UUID): Int {
        KillSwitchGuard.require("act")

        val now = Instant.now()
        val openInterventions = dbQuery {
            PewsInterventionsTable.selectAll().where {
                (PewsInterventionsTable.schoolId eq schoolId) and
                    (PewsInterventionsTable.status inList listOf("open", "in_progress"))
            }.toList()
        }

        var escalated = 0
        for (row in openInterventions) {
            val slaDays = row[PewsInterventionsTable.slaDays] ?: 5
            val openedAt = row[PewsInterventionsTable.openedAt]
            val escalationLevel = row[PewsInterventionsTable.escalationLevel]
            val ownerId = row[PewsInterventionsTable.ownerUserId]
            val studentCode = row[PewsInterventionsTable.studentCode]
            val urgency = row[PewsInterventionsTable.urgency] ?: "medium"
            val actionType = row[PewsInterventionsTable.actionType]

            val daysElapsed = ChronoUnit.DAYS.between(openedAt, now)

            // Level 0 → 1: remind owner at SLA elapsed
            if (escalationLevel == 0 && daysElapsed >= slaDays * REMIND_AFTER_SLA_MULTIPLE) {
                dbQuery {
                    PewsInterventionsTable.update({
                        PewsInterventionsTable.id eq row[PewsInterventionsTable.id]
                    }) {
                        it[PewsInterventionsTable.escalationLevel] = 1
                    }
                }
                runCatching {
                    Notify.toUser(
                        userId = ownerId,
                        category = "pews_escalation",
                        title = "SLA reminder: $studentCode ($actionType)",
                        body = "This $urgency case is past its ${slaDays}-day SLA. Please update its status.",
                        schoolId = schoolId,
                        deepLink = "/school/pews/student/$studentCode",
                        refType = "pews_intervention",
                        refId = studentCode,
                    )
                }.onFailure { log.warn("Escalation notify failed: {}", it.message) }
                escalated++
                log.info("Escalation: {} reminded ({} days past SLA)", studentCode, daysElapsed - slaDays)
            }

            // Level 1 → 2: escalate to admin at 2× SLA
            if (escalationLevel <= 1 && daysElapsed >= slaDays * ESCALATE_AFTER_SLA_MULTIPLE) {
                val admins = schoolAdminIds(schoolId)
                dbQuery {
                    PewsInterventionsTable.update({
                        PewsInterventionsTable.id eq row[PewsInterventionsTable.id]
                    }) {
                        it[PewsInterventionsTable.escalationLevel] = 2
                    }
                }
                runCatching {
                    Notify.toUsers(
                        userIds = admins,
                        category = "pews_escalation",
                        title = "ESCALATED: $studentCode unowned past 2× SLA",
                        body = "$urgency case ($actionType) for $studentCode is ${daysElapsed} days old " +
                            "(SLA was $slaDays days). Owner has not resolved it.",
                        schoolId = schoolId,
                        deepLink = "/school/pews/student/$studentCode",
                        refType = "pews_intervention",
                        refId = studentCode,
                    )
                }.onFailure { log.warn("Escalation admin notify failed: {}", it.message) }
                escalated++
                log.warn("Escalation: {} escalated to admin ({} days past SLA)", studentCode, daysElapsed - slaDays)
            }
        }

        if (escalated > 0) {
            log.info("Escalation sweep: school={} → {} interventions escalated", schoolId, escalated)
        }
        return escalated
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private fun computeFollowUpDate(slaDays: Int): LocalDate =
        LocalDate.now().plusDays(slaDays.toLong())

    private fun defaultActionFor(snap: PewsSnapshot): String {
        val top = snap.signals.maxByOrNull { it.severity }?.kind
        return when (top) {
            "attendance" -> "parent_call"
            "leave" -> "parent_call"
            "marks" -> "remedial_class"
            "homework" -> "parent_message"
            "fees" -> "fee_counselling"
            "health" -> "counselling"
            "engagement" -> "mentor_pairing"
            "trend" -> "observe"
            else -> "observe"
        }
    }

    private suspend fun resolveOwner(schoolId: UUID, className: String, section: String): UUID? = dbQuery {
        val rows = com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.selectAll().where {
            (com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.schoolId eq schoolId) and
                (com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.className eq className) and
                (com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.section eq section) and
                (com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.isActive eq true)
        }.toList()
        rows.firstOrNull { it[com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.isClassTeacher] &&
            it[com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.teacherId] != null }
            ?.get(com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.teacherId)
            ?: rows.firstOrNull { it[com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.teacherId] != null }
                ?.get(com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable.teacherId)
    }

    private suspend fun schoolAdminIds(schoolId: UUID): List<UUID> = dbQuery {
        AppUsersTable.selectAll().where {
            (AppUsersTable.schoolId eq schoolId) and
                (AppUsersTable.role inList listOf("school_admin", "admin")) and
                (AppUsersTable.isActive eq true)
        }.map { it[AppUsersTable.id].value }
    }

    private suspend fun notifyOwner(
        snap: PewsSnapshot,
        ownerId: UUID,
        urgency: String,
        caseFile: CaseFile?,
    ) {
        val firstName = snap.studentName.trim().split(" ").firstOrNull() ?: "A student"
        val topSignal = snap.signals.maxByOrNull { it.severity }?.label ?: "needs attention"
        val title = if (urgency == "high") {
            "URGENT: $firstName (Class ${snap.className}${snap.section})"
        } else {
            "Early-warning: $firstName (Class ${snap.className}${snap.section})"
        }
        val body = caseFile?.narrative ?: topSignal

        runCatching {
            Notify.toUser(
                userId = ownerId,
                category = "pews",
                title = title,
                body = body,
                schoolId = snap.schoolId,
                deepLink = "/school/pews/student/${snap.studentCode}",
                refType = "pews_snapshot",
                refId = snap.studentCode,
            )
        }.onFailure { log.warn("Act notify failed: {}", it.message) }
    }
}
