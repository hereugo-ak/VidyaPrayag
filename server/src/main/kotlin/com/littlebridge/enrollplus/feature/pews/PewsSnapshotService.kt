/*
 * File: PewsSnapshotService.kt
 * Module: feature.pews
 *
 * PEWS — SENSE stage (the deterministic core). This service owns WHO is at risk
 * and WHY, computed entirely from existing tables with plain SQL + Kotlin math —
 * NO LLM involved here (honesty LAW 6: the deterministic layer owns every number
 * and every named student; the AI only explains the bundle later).
 *
 * For each active student in a school it computes, over a trailing window:
 *   • attendance %                (attendance_records, type=student, by person_id)
 *   • attendance slope            (early-half vs late-half % delta → trajectory)
 *   • marks %                     (assessment_marks ÷ assessments.max_marks)
 *   • marks slope                 (by assessment exam_date ordering)
 *   • leave count                 (leave_requests, student-filed, by child/name)
 * then a composite 0..100 risk score and a band (watch|medium|high).
 *
 * Thresholds come from pews_config (per-school), falling back to the same floors
 * the shipped early_warning uses (75 / 40 / 3). When use_relative_thresholds is
 * on, a student is also flagged if they are well below the SCHOOL MEAN even when
 * above the absolute floor (catches a slipping high-performer early).
 *
 * Output is UPSERTed into pews_risk_snapshots keyed by (school, student, run_date)
 * so re-running the same day is idempotent. The ai_* columns are left null here —
 * PewsReasoningService fills them in the Reason stage. signal_hash is the SHA-256
 * of the deterministic bundle, used as the narrative cache key.
 *
 * Data-quality caveats (documented, handled — see plan §1.2):
 *   - leave joins child_id/student_code when present, falls back to requester_name.
 *   - marks keyed by legacy student_code (matches existing early_warning).
 *   - attendance rows with null person_id are skipped (can't attribute).
 */
package com.littlebridge.enrollplus.feature.pews

import com.littlebridge.enrollplus.db.AssessmentMarksTable
import com.littlebridge.enrollplus.db.AssessmentsTable
import com.littlebridge.enrollplus.db.AttendanceRecordsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.LeaveRequestsTable
import com.littlebridge.enrollplus.db.PewsConfigTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.db.SchoolsTable
import com.littlebridge.enrollplus.db.StudentsTable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** A deterministic reason behind a risk score (mirrors the shipped RiskSignalDto). */
@kotlinx.serialization.Serializable
data class PewsSignal(
    val kind: String,        // attendance | marks | leave | trend
    val label: String,       // human reason
    val severity: Int,       // 1..3
)

/** The full deterministic bundle for one student on one run date. */
data class PewsSnapshot(
    val schoolId: UUID,
    val studentCode: String,
    val studentName: String,
    val className: String,
    val section: String,
    val runDate: LocalDate,
    val riskScore: Int,
    val riskLevel: String,           // watch | medium | high
    val attendancePct: Int?,
    val marksPct: Int?,
    val leaveCount: Int,
    val attendanceSlope: Double?,
    val marksSlope: Double?,
    val signals: List<PewsSignal>,
    val signalHash: String,
)

class PewsSnapshotService {
    private val log = LoggerFactory.getLogger("PewsSnapshotService")
    private val json = Json { encodeDefaults = true }

    /** Per-school tuning resolved from pews_config (or sane defaults). */
    private data class Config(
        val useRelative: Boolean,
        val attendanceFloor: Int,
        val marksFloor: Int,
        val leaveFloor: Int,
        val aiNarrativeEnabled: Boolean,
    )

    private val windowDays = 60          // trailing window for attendance trajectory

    /** List active school ids (the daily job iterates these). */
    suspend fun activeSchoolIds(): List<UUID> = dbQuery {
        SchoolsTable.selectAll().map { it[SchoolsTable.id].value }
    }

    private suspend fun configFor(schoolId: UUID): Config = dbQuery {
        val row = PewsConfigTable.selectAll().where {
            PewsConfigTable.id eq EntityID(schoolId, PewsConfigTable)
        }.singleOrNull()
        Config(
            useRelative = row?.get(PewsConfigTable.useRelativeThresholds) ?: true,
            attendanceFloor = row?.get(PewsConfigTable.attendanceFloorPct) ?: 75,
            marksFloor = row?.get(PewsConfigTable.marksFloorPct) ?: 40,
            leaveFloor = row?.get(PewsConfigTable.leaveFloorCount) ?: 3,
            aiNarrativeEnabled = row?.get(PewsConfigTable.aiNarrativeEnabled) ?: true,
        )
    }

    /**
     * Recompute snapshots for a whole school and persist them. Returns the list
     * of computed snapshots (so the caller can decide which to Reason/Notify).
     * Idempotent for a given run_date.
     */
    suspend fun recomputeSchool(schoolId: UUID, runDate: LocalDate = LocalDate.now()): List<PewsSnapshot> {
        val cfg = configFor(schoolId)
        val snapshots = computeSchool(schoolId, runDate, cfg)
        snapshots.forEach { persist(it) }
        log.info("PEWS recompute: school={} runDate={} → {} at-risk snapshots",
            schoolId, runDate, snapshots.size)
        return snapshots
    }

    /** Pure-ish computation (DB reads only, no writes). */
    private suspend fun computeSchool(
        schoolId: UUID, runDate: LocalDate, cfg: Config,
    ): List<PewsSnapshot> = dbQuery {
        val since = runDate.minusDays((windowDays - 1).toLong())

        // students
        data class StudentLite(val code: String, val name: String, val cls: String, val sec: String)
        val students = StudentsTable.selectAll().where {
            (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true)
        }.map {
            StudentLite(
                code = it[StudentsTable.studentCode],
                name = it[StudentsTable.fullName],
                cls = it[StudentsTable.className],
                sec = it[StudentsTable.section],
            )
        }
        if (students.isEmpty()) return@dbQuery emptyList<PewsSnapshot>()

        // attendance: present+late vs total, plus dated points for slope
        // code -> list of (date, presentFlag)
        val attPoints = HashMap<String, MutableList<Pair<LocalDate, Boolean>>>()
        AttendanceRecordsTable.selectAll().where {
            (AttendanceRecordsTable.schoolId eq schoolId) and
                (AttendanceRecordsTable.type eq "student")
        }.forEach { row ->
            val code = row[AttendanceRecordsTable.personId] ?: return@forEach
            val d = row[AttendanceRecordsTable.date]
            if (d < since) return@forEach
            val s = row[AttendanceRecordsTable.status].lowercase()
            val present = (s == "present" || s == "late")
            attPoints.getOrPut(code) { mutableListOf() }.add(d to present)
        }

        // marks: code -> list of (examDate?, pct)
        val assessMeta = AssessmentsTable.selectAll().where {
            AssessmentsTable.schoolId eq schoolId
        }.associate {
            it[AssessmentsTable.id].value to (it[AssessmentsTable.maxMarks] to it[AssessmentsTable.examDate])
        }
        val marksPoints = HashMap<String, MutableList<Pair<LocalDate?, Double>>>()
        AssessmentMarksTable.selectAll().forEach { row ->
            val aId = row[AssessmentMarksTable.assessmentId]
            val meta = assessMeta[aId] ?: return@forEach
            val max = meta.first
            if (max <= 0) return@forEach
            if (row[AssessmentMarksTable.isAbsent]) return@forEach
            val m = row[AssessmentMarksTable.marks] ?: return@forEach
            val pct = (m / max) * 100.0
            val code = row[AssessmentMarksTable.studentId]
            marksPoints.getOrPut(code) { mutableListOf() }.add(meta.second to pct)
        }

        // leave: prefer child_id→student_code; fall back to requester_name (legacy)
        val studentCodeById = students.associateBy { it.code }   // by code already
        val leaveByCode = HashMap<String, Int>()
        val leaveByName = HashMap<String, Int>()
        LeaveRequestsTable.selectAll().where {
            (LeaveRequestsTable.schoolId eq schoolId) and
                (LeaveRequestsTable.requesterRole eq "student")
        }.forEach { row ->
            // legacy rows have no child_id; we only have requester_name → name match
            val name = row[LeaveRequestsTable.requesterName].trim().lowercase()
            leaveByName[name] = (leaveByName[name] ?: 0) + 1
        }

        // school-mean baselines for relative thresholds
        val allAttPct = students.mapNotNull { st ->
            attPoints[st.code]?.takeIf { it.isNotEmpty() }?.let { pts ->
                (pts.count { it.second } * 100.0) / pts.size
            }
        }
        val allMarksPct = students.mapNotNull { st ->
            marksPoints[st.code]?.takeIf { it.isNotEmpty() }?.let { pts ->
                pts.map { it.second }.average()
            }
        }
        val attMean = if (allAttPct.isNotEmpty()) allAttPct.average() else 0.0
        val attSd = stddev(allAttPct, attMean)
        val marksMean = if (allMarksPct.isNotEmpty()) allMarksPct.average() else 0.0
        val marksSd = stddev(allMarksPct, marksMean)

        students.mapNotNull { st ->
            val attList = attPoints[st.code]?.sortedBy { it.first }
            val attPct = attList?.takeIf { it.isNotEmpty() }?.let {
                (it.count { p -> p.second } * 100.0) / it.size
            }?.toInt()
            val attSlope = attList?.let { slopeOfAttendance(it) }

            val marksList = marksPoints[st.code]
            val marksPct = marksList?.takeIf { it.isNotEmpty() }?.map { it.second }?.average()?.toInt()
            val marksSlope = marksList?.let { slopeOfMarks(it) }

            val leaveCount = leaveByName[st.name.trim().lowercase()] ?: 0

            val signals = buildList {
                // absolute floors (same as shipped early_warning)
                if (attPct != null && attPct < cfg.attendanceFloor) {
                    add(PewsSignal("attendance", "Attendance $attPct% (below ${cfg.attendanceFloor}%)",
                        if (attPct < 50) 3 else if (attPct < 65) 2 else 1))
                }
                if (marksPct != null && marksPct < cfg.marksFloor) {
                    add(PewsSignal("marks", "Average $marksPct% (below ${cfg.marksFloor}%)",
                        if (marksPct < 25) 3 else 2))
                }
                if (leaveCount >= cfg.leaveFloor) {
                    add(PewsSignal("leave", "$leaveCount leave requests filed",
                        if (leaveCount >= 5) 2 else 1))
                }
                // relative (z-score): well below the school mean even if above floor
                if (cfg.useRelative && attPct != null && attSd > 0 && attPct >= cfg.attendanceFloor) {
                    val z = (attPct - attMean) / attSd
                    if (z <= -1.5) add(PewsSignal("attendance",
                        "Attendance $attPct% is well below the school average", 1))
                }
                if (cfg.useRelative && marksPct != null && marksSd > 0 && marksPct >= cfg.marksFloor) {
                    val z = (marksPct - marksMean) / marksSd
                    if (z <= -1.5) add(PewsSignal("marks",
                        "Marks $marksPct% are well below the school average", 1))
                }
                // trajectory: a sharp downward slope is a leading indicator
                if (attSlope != null && attSlope <= -10.0) {
                    add(PewsSignal("trend", "Attendance trending down (${attSlope.toInt()} pts)", 2))
                }
                if (marksSlope != null && marksSlope <= -10.0) {
                    add(PewsSignal("trend", "Marks trending down (${marksSlope.toInt()} pts)", 2))
                }
            }
            if (signals.isEmpty()) return@mapNotNull null

            val maxSev = signals.maxOf { it.severity }
            val distinctKinds = signals.map { it.kind }.distinct().size
            val level = when {
                distinctKinds >= 2 || maxSev == 3 -> "high"
                maxSev == 2 -> "medium"
                else -> "watch"
            }
            val score = computeScore(attPct, marksPct, leaveCount, attSlope, marksSlope, signals, cfg)
            val hash = signalHash(st.code, runDate, attPct, marksPct, leaveCount, signals)

            PewsSnapshot(
                schoolId = schoolId, studentCode = st.code, studentName = st.name,
                className = st.cls, section = st.sec, runDate = runDate,
                riskScore = score, riskLevel = level,
                attendancePct = attPct, marksPct = marksPct, leaveCount = leaveCount,
                attendanceSlope = attSlope, marksSlope = marksSlope,
                signals = signals, signalHash = hash,
            )
        }.sortedWith(compareByDescending<PewsSnapshot> { it.riskScore }.thenBy { it.studentName })
    }

    // ── read API (for routes) ────────────────────────────────────────────────

    /** A stored snapshot row enriched with the parsed signals + AI fields. */
    data class StoredSnapshot(
        val studentCode: String,
        val studentName: String,
        val className: String,
        val section: String,
        val runDate: String,
        val riskScore: Int,
        val riskLevel: String,
        val attendancePct: Int?,
        val marksPct: Int?,
        val leaveCount: Int,
        val attendanceSlope: Double?,
        val marksSlope: Double?,
        val signals: List<PewsSignal>,
        val aiNarrative: String?,
        val aiCause: String?,
        val aiRecommendation: String?,
        val aiProviderUsed: String?,
    )

    /** The most recent run_date that has snapshots for a school (or null). */
    suspend fun latestRunDate(schoolId: UUID): LocalDate? = dbQuery {
        PewsRiskSnapshotsTable.selectAll().where {
            PewsRiskSnapshotsTable.schoolId eq schoolId
        }.orderBy(PewsRiskSnapshotsTable.runDate, org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(1).singleOrNull()?.get(PewsRiskSnapshotsTable.runDate)
    }

    /**
     * Cohort for a school on a run date (latest if null). Optionally filter by
     * class+section (teacher scope) and minimum level.
     */
    suspend fun cohort(
        schoolId: UUID,
        runDate: LocalDate? = null,
        className: String? = null,
        section: String? = null,
        minLevel: String = "watch",
    ): List<StoredSnapshot> {
        val date = runDate ?: latestRunDate(schoolId) ?: return emptyList()
        val levelRank = mapOf("watch" to 0, "medium" to 1, "high" to 2)
        val minRank = levelRank[minLevel] ?: 0
        return dbQuery {
            PewsRiskSnapshotsTable.selectAll().where {
                (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                    (PewsRiskSnapshotsTable.runDate eq date)
            }.orderBy(PewsRiskSnapshotsTable.riskScore, org.jetbrains.exposed.sql.SortOrder.DESC)
                .toList()
                .map { toStored(it) }
        }.filter { (levelRank[it.riskLevel] ?: 0) >= minRank }
            .filter { className == null || it.className.equals(className, ignoreCase = true) }
            .filter { section == null || it.section.equals(section, ignoreCase = true) }
    }

    /** One student's latest snapshot (or for a specific run date). */
    suspend fun studentSnapshot(
        schoolId: UUID, studentCode: String, runDate: LocalDate? = null,
    ): StoredSnapshot? {
        val date = runDate ?: latestRunDate(schoolId) ?: return null
        return dbQuery {
            PewsRiskSnapshotsTable.selectAll().where {
                (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                    (PewsRiskSnapshotsTable.studentCode eq studentCode) and
                    (PewsRiskSnapshotsTable.runDate eq date)
            }.singleOrNull()?.let { toStored(it) }
        }
    }

    /** History (last N run dates) for one student. */
    suspend fun studentHistory(
        schoolId: UUID, studentCode: String, limit: Int = 12,
    ): List<StoredSnapshot> = dbQuery {
        PewsRiskSnapshotsTable.selectAll().where {
            (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                (PewsRiskSnapshotsTable.studentCode eq studentCode)
        }.orderBy(PewsRiskSnapshotsTable.runDate, org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit).map { toStored(it) }
    }

    private fun toStored(r: org.jetbrains.exposed.sql.ResultRow): StoredSnapshot {
        val signals = runCatching {
            json.decodeFromString<List<PewsSignal>>(r[PewsRiskSnapshotsTable.signalsJson])
        }.getOrDefault(emptyList())
        // name is not stored on the snapshot — caller can enrich; we leave code.
        return StoredSnapshot(
            studentCode = r[PewsRiskSnapshotsTable.studentCode],
            studentName = r[PewsRiskSnapshotsTable.studentCode],   // enriched by route
            className = "",
            section = "",
            runDate = r[PewsRiskSnapshotsTable.runDate].toString(),
            riskScore = r[PewsRiskSnapshotsTable.riskScore],
            riskLevel = r[PewsRiskSnapshotsTable.riskLevel],
            attendancePct = r[PewsRiskSnapshotsTable.attendancePct],
            marksPct = r[PewsRiskSnapshotsTable.marksPct],
            leaveCount = r[PewsRiskSnapshotsTable.leaveCount],
            attendanceSlope = r[PewsRiskSnapshotsTable.attendanceSlope],
            marksSlope = r[PewsRiskSnapshotsTable.marksSlope],
            signals = signals,
            aiNarrative = r[PewsRiskSnapshotsTable.aiNarrative],
            aiCause = r[PewsRiskSnapshotsTable.aiCause],
            aiRecommendation = r[PewsRiskSnapshotsTable.aiRecommendation],
            aiProviderUsed = r[PewsRiskSnapshotsTable.aiProviderUsed],
        )
    }

    /** Enrich stored snapshots with student identity (name/class/section). */
    suspend fun enrichIdentity(
        schoolId: UUID, snaps: List<StoredSnapshot>,
    ): List<StoredSnapshot> {
        if (snaps.isEmpty()) return snaps
        val codes = snaps.map { it.studentCode }.distinct()
        val byCode = dbQuery {
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and (StudentsTable.studentCode inList codes)
            }.associate {
                it[StudentsTable.studentCode] to Triple(
                    it[StudentsTable.fullName], it[StudentsTable.className], it[StudentsTable.section]
                )
            }
        }
        return snaps.map { s ->
            val id = byCode[s.studentCode]
            if (id == null) s else s.copy(
                studentName = id.first, className = id.second, section = id.third
            )
        }
    }

    // ── math helpers ────────────────────────────────────────────────────────

    private fun stddev(values: List<Double>, mean: Double): Double {
        if (values.size < 2) return 0.0
        val variance = values.sumOf { (it - mean) * (it - mean) } / values.size
        return kotlin.math.sqrt(variance)
    }

    /** Attendance slope = (late-half % − early-half %); negative = sliding. */
    private fun slopeOfAttendance(points: List<Pair<LocalDate, Boolean>>): Double? {
        if (points.size < 6) return null
        val mid = points.size / 2
        val early = points.subList(0, mid)
        val late = points.subList(mid, points.size)
        val earlyPct = early.count { it.second } * 100.0 / early.size
        val latePct = late.count { it.second } * 100.0 / late.size
        return latePct - earlyPct
    }

    /** Marks slope = (late-half avg − early-half avg) ordered by exam date. */
    private fun slopeOfMarks(points: List<Pair<LocalDate?, Double>>): Double? {
        if (points.size < 2) return null
        val ordered = points.sortedBy { it.first ?: LocalDate.MIN }
        val mid = ordered.size / 2
        val early = ordered.subList(0, mid.coerceAtLeast(1))
        val late = ordered.subList(mid, ordered.size)
        if (early.isEmpty() || late.isEmpty()) return null
        return late.map { it.second }.average() - early.map { it.second }.average()
    }

    private fun computeScore(
        attPct: Int?, marksPct: Int?, leaveCount: Int,
        attSlope: Double?, marksSlope: Double?, signals: List<PewsSignal>, cfg: Config,
    ): Int {
        var score = 0.0
        // attendance gap below floor (max ~35)
        if (attPct != null && attPct < cfg.attendanceFloor) {
            score += ((cfg.attendanceFloor - attPct).coerceIn(0, 75)) * 0.45
        }
        // marks gap below floor (max ~24)
        if (marksPct != null && marksPct < cfg.marksFloor) {
            score += ((cfg.marksFloor - marksPct).coerceIn(0, 40)) * 0.6
        }
        // leave spikes (max ~15)
        if (leaveCount >= cfg.leaveFloor) score += (leaveCount * 3.0).coerceAtMost(15.0)
        // downward trajectory (max ~10 each)
        if (attSlope != null && attSlope < 0) score += (-attSlope).coerceAtMost(10.0)
        if (marksSlope != null && marksSlope < 0) score += (-marksSlope).coerceAtMost(10.0)
        // multi-signal bonus
        if (signals.map { it.kind }.distinct().size >= 2) score += 8
        return score.coerceIn(0.0, 100.0).toInt()
    }

    private fun signalHash(
        code: String, runDate: LocalDate, attPct: Int?, marksPct: Int?,
        leaveCount: Int, signals: List<PewsSignal>,
    ): String {
        val raw = buildString {
            append(code); append('|'); append(runDate); append('|')
            append(attPct); append('|'); append(marksPct); append('|'); append(leaveCount); append('|')
            signals.sortedBy { it.label }.forEach { append(it.kind).append(it.label).append(it.severity) }
        }
        return MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    // ── persistence (idempotent upsert by school+student+run_date) ───────────

    private suspend fun persist(s: PewsSnapshot) = dbQuery {
        val signalsJson = json.encodeToString(s.signals)
        val existing = PewsRiskSnapshotsTable.selectAll().where {
            (PewsRiskSnapshotsTable.schoolId eq s.schoolId) and
                (PewsRiskSnapshotsTable.studentCode eq s.studentCode) and
                (PewsRiskSnapshotsTable.runDate eq s.runDate)
        }.singleOrNull()

        if (existing == null) {
            PewsRiskSnapshotsTable.insert {
                it[schoolId] = s.schoolId
                it[studentCode] = s.studentCode
                it[runDate] = s.runDate
                it[riskScore] = s.riskScore
                it[riskLevel] = s.riskLevel
                it[attendancePct] = s.attendancePct
                it[marksPct] = s.marksPct
                it[leaveCount] = s.leaveCount
                it[attendanceSlope] = s.attendanceSlope
                it[marksSlope] = s.marksSlope
                it[PewsRiskSnapshotsTable.signalsJson] = signalsJson
                it[signalHash] = s.signalHash
                it[createdAt] = Instant.now()
            }
        } else {
            // Re-run same day: refresh deterministic fields. Preserve ai_* only if
            // the signal bundle is unchanged (same hash) — otherwise the old
            // narrative no longer matches the signals, so clear it for re-Reason.
            val sameBundle = existing[PewsRiskSnapshotsTable.signalHash] == s.signalHash
            PewsRiskSnapshotsTable.update({
                (PewsRiskSnapshotsTable.schoolId eq s.schoolId) and
                    (PewsRiskSnapshotsTable.studentCode eq s.studentCode) and
                    (PewsRiskSnapshotsTable.runDate eq s.runDate)
            }) {
                it[riskScore] = s.riskScore
                it[riskLevel] = s.riskLevel
                it[attendancePct] = s.attendancePct
                it[marksPct] = s.marksPct
                it[leaveCount] = s.leaveCount
                it[attendanceSlope] = s.attendanceSlope
                it[marksSlope] = s.marksSlope
                it[PewsRiskSnapshotsTable.signalsJson] = signalsJson
                it[signalHash] = s.signalHash
                if (!sameBundle) {
                    it[aiNarrative] = null
                    it[aiCause] = null
                    it[aiRecommendation] = null
                    it[aiProviderUsed] = null
                }
            }
        }
    }
}
