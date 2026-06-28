/*
 * File: PewsInterventionService.kt
 * Module: feature.pews
 *
 * PEWS — ACT + LEARN stages.
 *
 *   ACT:   when a student newly reaches high/medium risk, open an intervention
 *          task assigned to the right owner (the class teacher if we can resolve
 *          one, else the school admins), and fire a notification through the
 *          existing spine (Notify.toUser, category="pews"). The owner sees it in
 *          their bell + FCM with a deep link to the student card.
 *
 *   LEARN: when an owner closes an intervention they record an outcome
 *          (improved | unchanged | worsened). That outcome is what powers the
 *          effectiveness rollup the admin sees — closing the loop so PEWS learns
 *          which actions work, not just which students are at risk.
 *
 * Auto-assignment is conservative and idempotent:
 *   - Only one OPEN intervention per (school, student) at a time (no spam).
 *   - Owner = the is_class_teacher for the student's class+section if present,
 *     else any active teacher assigned to that class, else the school admins.
 *   - actionType defaults from the snapshot's dominant signal (a sensible first
 *     step the teacher can change).
 *
 * Everything is school-scoped. No fabricated students — interventions are always
 * tied to a real snapshot + real student_code.
 */
package com.littlebridge.enrollplus.feature.pews

import com.littlebridge.enrollplus.db.AppUsersTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsInterventionsTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.TeacherSubjectAssignmentsTable
import com.littlebridge.enrollplus.feature.notifications.Notify
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class PewsInterventionService {
    private val log = LoggerFactory.getLogger("PewsInterventionService")

    // ── ACT: auto-open + notify for newly at-risk students ───────────────────

    /**
     * For each high/medium snapshot, open an intervention (if none is open) and
     * notify the owner. Returns the number of interventions opened.
     */
    suspend fun actOnSnapshots(
        schoolId: UUID,
        snapshots: List<PewsSnapshot>,
        minLevel: String = "medium",
    ): Int {
        val levelRank = mapOf("watch" to 0, "medium" to 1, "high" to 2)
        val minRank = levelRank[minLevel] ?: 1
        val targets = snapshots.filter { (levelRank[it.riskLevel] ?: 0) >= minRank }

        var opened = 0
        for (snap in targets) {
            runCatching { openIfNeeded(snap) }
                .onSuccess { if (it) opened++ }
                .onFailure { log.warn("PEWS act failed for {}: {}", snap.studentCode, it.message) }
        }
        log.info("PEWS act: school={} opened {} interventions", schoolId, opened)
        return opened
    }

    private suspend fun openIfNeeded(snap: PewsSnapshot): Boolean {
        // 1) already an open intervention for this student? → skip (no spam)
        val hasOpen = dbQuery {
            PewsInterventionsTable.selectAll().where {
                (PewsInterventionsTable.schoolId eq snap.schoolId) and
                    (PewsInterventionsTable.studentCode eq snap.studentCode) and
                    (PewsInterventionsTable.status inList listOf("open", "in_progress"))
            }.limit(1).any()
        }
        if (hasOpen) return false

        // 2) resolve the owner + the snapshot id
        val owner = resolveOwner(snap.schoolId, snap.className, snap.section)
        val snapshotId = dbQuery {
            PewsRiskSnapshotsTable.selectAll().where {
                (PewsRiskSnapshotsTable.schoolId eq snap.schoolId) and
                    (PewsRiskSnapshotsTable.studentCode eq snap.studentCode) and
                    (PewsRiskSnapshotsTable.runDate eq snap.runDate)
            }.singleOrNull()?.get(PewsRiskSnapshotsTable.id)?.value
        }

        val actionType = defaultActionFor(snap)
        val now = Instant.now()

        if (owner == null) {
            // No specific owner; notify admins but still create an admin-owned task
            // so it doesn't fall through the cracks.
            val admins = schoolAdminIds(snap.schoolId)
            val ownerId = admins.firstOrNull() ?: return false
            createIntervention(snap, snapshotId, ownerId, actionType, now)
            notifyOwners(snap, admins)
            return true
        }

        createIntervention(snap, snapshotId, owner, actionType, now)
        notifyOwners(snap, listOf(owner))
        return true
    }

    private suspend fun createIntervention(
        snap: PewsSnapshot, snapshotId: UUID?, ownerId: UUID, actionType: String, now: Instant,
    ) = dbQuery {
        PewsInterventionsTable.insert {
            it[schoolId] = snap.schoolId
            it[studentCode] = snap.studentCode
            it[PewsInterventionsTable.snapshotId] = snapshotId
            it[ownerUserId] = ownerId
            it[PewsInterventionsTable.actionType] = actionType
            it[status] = "open"
            it[openedAt] = now
            it[createdAt] = now
        }
    }

    private suspend fun notifyOwners(snap: PewsSnapshot, owners: List<UUID>) {
        if (owners.isEmpty()) return
        val firstName = snap.studentName.trim().split(" ").firstOrNull() ?: "A student"
        val topSignal = snap.signals.maxByOrNull { it.severity }?.label ?: "needs attention"
        runCatching {
            Notify.toUsers(
                userIds = owners,
                category = "pews",
                title = "Early-warning: $firstName (Class ${snap.className}${snap.section})",
                body = topSignal,
                schoolId = snap.schoolId,
                deepLink = "/school/pews/student/${snap.studentCode}",
                refType = "pews_snapshot",
                refId = snap.studentCode,
            )
        }.onFailure { log.warn("PEWS notify failed: {}", it.message) }
    }

    /** Dominant-signal → first sensible action a teacher can change later. */
    private fun defaultActionFor(snap: PewsSnapshot): String {
        val top = snap.signals.maxByOrNull { it.severity }?.kind
        return when (top) {
            "attendance" -> "parent_call"
            "leave" -> "parent_call"
            "marks" -> "remedial_class"
            "trend" -> "observe"
            else -> "observe"
        }
    }

    // ── owner resolution ─────────────────────────────────────────────────────

    /** The class teacher (is_class_teacher) for class+section, else any teacher. */
    private suspend fun resolveOwner(schoolId: UUID, className: String, section: String): UUID? = dbQuery {
        val rows = TeacherSubjectAssignmentsTable.selectAll().where {
            (TeacherSubjectAssignmentsTable.schoolId eq schoolId) and
                (TeacherSubjectAssignmentsTable.className eq className) and
                (TeacherSubjectAssignmentsTable.section eq section) and
                (TeacherSubjectAssignmentsTable.isActive eq true)
        }.toList()
        // prefer the designated class teacher with a real teacher_id
        rows.firstOrNull { it[TeacherSubjectAssignmentsTable.isClassTeacher] &&
            it[TeacherSubjectAssignmentsTable.teacherId] != null }
            ?.get(TeacherSubjectAssignmentsTable.teacherId)
            ?: rows.firstOrNull { it[TeacherSubjectAssignmentsTable.teacherId] != null }
                ?.get(TeacherSubjectAssignmentsTable.teacherId)
    }

    private suspend fun schoolAdminIds(schoolId: UUID): List<UUID> = dbQuery {
        AppUsersTable.selectAll().where {
            (AppUsersTable.schoolId eq schoolId) and
                (AppUsersTable.role inList listOf("school_admin", "admin")) and
                (AppUsersTable.isActive eq true)
        }.map { it[AppUsersTable.id].value }
    }

    // ── LEARN: close an intervention with an outcome ─────────────────────────

    data class InterventionView(
        val id: UUID,
        val studentCode: String,
        val studentName: String,
        val className: String,
        val section: String,
        val ownerUserId: UUID,
        val actionType: String,
        val status: String,
        val notes: String?,
        val outcome: String?,
        val openedAt: String,
        val resolvedAt: String?,
    )

    /** Update status/notes; when status=done/dismissed, stamp resolvedAt + outcome. */
    suspend fun updateIntervention(
        schoolId: UUID,
        interventionId: UUID,
        actorUserId: UUID,
        newStatus: String?,
        notes: String?,
        outcome: String?,
        actionType: String?,
    ): Boolean {
        val allowedStatus = setOf("open", "in_progress", "done", "dismissed")
        val allowedOutcome = setOf("improved", "unchanged", "worsened")
        if (newStatus != null && newStatus !in allowedStatus) return false
        if (outcome != null && outcome !in allowedOutcome) return false

        return dbQuery {
            val row = PewsInterventionsTable.selectAll().where {
                (PewsInterventionsTable.id eq interventionId) and
                    (PewsInterventionsTable.schoolId eq schoolId)
            }.singleOrNull() ?: return@dbQuery false

            val now = Instant.now()
            val resolving = newStatus == "done" || newStatus == "dismissed"
            PewsInterventionsTable.update({
                (PewsInterventionsTable.id eq interventionId) and
                    (PewsInterventionsTable.schoolId eq schoolId)
            }) {
                if (newStatus != null) it[status] = newStatus
                if (notes != null) it[PewsInterventionsTable.notes] = notes
                if (actionType != null) it[PewsInterventionsTable.actionType] = actionType
                if (outcome != null) it[PewsInterventionsTable.outcome] = outcome
                if (resolving) it[resolvedAt] = now
            }
            true
        }
    }

    /** List interventions for a school, optionally filtered by owner/status. */
    suspend fun listInterventions(
        schoolId: UUID,
        ownerUserId: UUID? = null,
        status: String? = null,
    ): List<InterventionView> = dbQuery {
        val rows = PewsInterventionsTable.selectAll().where {
            var cond = PewsInterventionsTable.schoolId eq schoolId
            ownerUserId?.let { cond = cond and (PewsInterventionsTable.ownerUserId eq it) }
            status?.let { cond = cond and (PewsInterventionsTable.status eq it) }
            cond
        }.orderBy(PewsInterventionsTable.openedAt, SortOrder.DESC).toList()

        if (rows.isEmpty()) return@dbQuery emptyList<InterventionView>()

        // batch-load student identity by code
        val codes = rows.map { it[PewsInterventionsTable.studentCode] }.distinct()
        val studentByCode = StudentsTable.selectAll().where {
            (StudentsTable.schoolId eq schoolId) and (StudentsTable.studentCode inList codes)
        }.associateBy { it[StudentsTable.studentCode] }

        rows.map { r ->
            val code = r[PewsInterventionsTable.studentCode]
            val s = studentByCode[code]
            InterventionView(
                id = r[PewsInterventionsTable.id].value,
                studentCode = code,
                studentName = s?.get(StudentsTable.fullName) ?: code,
                className = s?.get(StudentsTable.className) ?: "",
                section = s?.get(StudentsTable.section) ?: "",
                ownerUserId = r[PewsInterventionsTable.ownerUserId],
                actionType = r[PewsInterventionsTable.actionType],
                status = r[PewsInterventionsTable.status],
                notes = r[PewsInterventionsTable.notes],
                outcome = r[PewsInterventionsTable.outcome],
                openedAt = r[PewsInterventionsTable.openedAt].toString(),
                resolvedAt = r[PewsInterventionsTable.resolvedAt]?.toString(),
            )
        }
    }

    /** Effectiveness rollup for the admin Learn view. */
    data class Effectiveness(
        val total: Int, val open: Int, val done: Int, val dismissed: Int,
        val improved: Int, val unchanged: Int, val worsened: Int,
    )

    suspend fun effectiveness(schoolId: UUID): Effectiveness = dbQuery {
        val rows = PewsInterventionsTable.selectAll().where {
            PewsInterventionsTable.schoolId eq schoolId
        }.toList()
        Effectiveness(
            total = rows.size,
            open = rows.count { it[PewsInterventionsTable.status] in listOf("open", "in_progress") },
            done = rows.count { it[PewsInterventionsTable.status] == "done" },
            dismissed = rows.count { it[PewsInterventionsTable.status] == "dismissed" },
            improved = rows.count { it[PewsInterventionsTable.outcome] == "improved" },
            unchanged = rows.count { it[PewsInterventionsTable.outcome] == "unchanged" },
            worsened = rows.count { it[PewsInterventionsTable.outcome] == "worsened" },
        )
    }
}
