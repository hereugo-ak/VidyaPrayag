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
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ExamResultsTable
import com.littlebridge.enrollplus.db.FeeRecordsTable
import com.littlebridge.enrollplus.db.HomeworkSubmissionsTable
import com.littlebridge.enrollplus.db.HomeworkTable
import com.littlebridge.enrollplus.db.LeaveRequestsTable
import com.littlebridge.enrollplus.db.PewsConfigTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.db.PtmClassProgressTable
import com.littlebridge.enrollplus.db.PtmEventsTable
import com.littlebridge.enrollplus.db.SchoolsTable
import com.littlebridge.enrollplus.db.StudentHealthIncidentsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.db.TransportAssignmentsTable
import com.littlebridge.enrollplus.db.TutorMasteryTable
import com.littlebridge.enrollplus.db.TutorMisconceptionsTable
import com.littlebridge.enrollplus.feature.pews.core.KillSwitchGuard
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
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
    val kind: String,        // attendance | marks | leave | trend | homework | fees | health | exam | engagement | transport | mastery | misconception
    val label: String,       // human reason
    val severity: Int,       // 1..3
    val isLeading: Boolean = false,   // PEWS 2.0: leading indicator flag
    val evidenceRef: String? = null,  // PEWS 2.0: evidence reference (table/date)
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
    // PEWS 2.0 expanded fields
    val confidence: Double? = null,        // 0..1 — data completeness × signal agreement
    val leadingScore: Int? = null,         // weighted leading indicators (predictive)
    val causeFamily: String? = null,       // attendance|academic|disengagement|wellbeing|financial|external
    val deltasJson: String? = null,        // JSON of deltas vs last run (for auto-outcome)
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
        KillSwitchGuard.require("sense")
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
        val hwSince = runDate.minusDays(30)

        // students
        data class StudentLite(val code: String, val name: String, val cls: String, val sec: String, val id: UUID?)
        val students = StudentsTable.selectAll().where {
            (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true)
        }.map {
            StudentLite(
                code = it[StudentsTable.studentCode],
                name = it[StudentsTable.fullName],
                cls = it[StudentsTable.className],
                sec = it[StudentsTable.section],
                id = it[StudentsTable.id].value,
            )
        }
        if (students.isEmpty()) return@dbQuery emptyList<PewsSnapshot>()

        // ── v1 signals: attendance + marks ──────────────────────────────────
        val attPoints: MutableMap<String, MutableList<Pair<LocalDate, Boolean>>> = mutableMapOf()
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

        val assessMeta = AssessmentsTable.selectAll().where {
            AssessmentsTable.schoolId eq schoolId
        }.associate {
            it[AssessmentsTable.id].value to (it[AssessmentsTable.maxMarks] to it[AssessmentsTable.examDate])
        }
        val marksPoints: MutableMap<String, MutableList<Pair<LocalDate?, Double>>> = mutableMapOf()
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

        // ── v1 signal: leave (FIXED: child_id FK first, name fallback) ──────
        val childCodeByChildId = ChildrenTable.selectAll().where {
            ChildrenTable.schoolId eq schoolId
        }.associate { it[ChildrenTable.id].value to (it[ChildrenTable.studentCode] ?: "") }
        val leaveByCode = HashMap<String, Int>()
        LeaveRequestsTable.selectAll().where {
            (LeaveRequestsTable.schoolId eq schoolId) and
                (LeaveRequestsTable.requesterRole eq "student")
        }.forEach { row ->
            val childId = row[LeaveRequestsTable.childId]
            val code = if (childId != null) {
                childCodeByChildId[childId] ?: ""
            } else {
                // legacy fallback: match by requester_name → student name
                val name = row[LeaveRequestsTable.requesterName].trim().lowercase()
                students.firstOrNull { it.name.trim().lowercase() == name }?.code ?: ""
            }
            if (code.isNotEmpty()) {
                leaveByCode[code] = (leaveByCode[code] ?: 0) + 1
            }
        }

        // ── NEW signal: HW non-submission streak (leading) ──────────────────
        val hwAssigned = HomeworkTable.selectAll().where {
            (HomeworkTable.schoolId eq schoolId) and
                (HomeworkTable.isActive eq true) and
                (HomeworkTable.dueDate greaterEq hwSince)
        }.map { it[HomeworkTable.id].value to (it[HomeworkTable.className] to it[HomeworkTable.section]) }

        val hwMissedByCode = HashMap<String, Int>()
        if (hwAssigned.isNotEmpty()) {
            val hwIds = hwAssigned.map { it.first }.toSet()
            HomeworkSubmissionsTable.selectAll().where {
                HomeworkSubmissionsTable.homeworkId inList hwIds
            }.forEach { row ->
                val stCode = row[HomeworkSubmissionsTable.studentId]
                val status = row[HomeworkSubmissionsTable.status]
                if (status == "not_submitted" || status == "late") {
                    hwMissedByCode[stCode] = (hwMissedByCode[stCode] ?: 0) + 1
                }
            }
        }

        // ── NEW signal: fee status (OVERDUE) ────────────────────────────────
        val feeOverdueByCode = HashMap<String, Boolean>()
        FeeRecordsTable.selectAll().where {
            (FeeRecordsTable.schoolId eq schoolId) and
                (FeeRecordsTable.status eq "OVERDUE")
        }.forEach { row ->
            val childId = row[FeeRecordsTable.childId] ?: return@forEach
            val code = childCodeByChildId[childId] ?: return@forEach
            if (code.isNotEmpty()) feeOverdueByCode[code] = true
        }

        // ── NEW signal: health incidents ────────────────────────────────────
        val studentIdByCode = students.filter { it.id != null }.associate { it.code to it.id!! }
        val codeByStudentId = studentIdByCode.entries.associate { (k, v) -> v to k }
        val healthFlagByCode = HashMap<String, Boolean>()
        StudentHealthIncidentsTable.selectAll().where {
            (StudentHealthIncidentsTable.schoolId eq schoolId) and
                (StudentHealthIncidentsTable.date greaterEq since)
        }.forEach { row ->
            val sId = row[StudentHealthIncidentsTable.studentId]
            val code = codeByStudentId[sId] ?: return@forEach
            val severity = row[StudentHealthIncidentsTable.severity]
            if (severity == "moderate" || severity == "major") {
                healthFlagByCode[code] = true
            }
        }

        // ── NEW signal: exam term trajectory ────────────────────────────────
        val examTrendByCode = HashMap<String, Double>()
        ExamResultsTable.selectAll().where {
            (ExamResultsTable.schoolId eq schoolId) and
                (ExamResultsTable.status neq "Pending")
        }.forEach { row ->
            val code = row[ExamResultsTable.studentId]
            val trendStr = row[ExamResultsTable.trend].replace("%", "").replace("+", "")
            val trend = trendStr.toDoubleOrNull() ?: return@forEach
            // accumulate average trend across subjects
            examTrendByCode[code] = (examTrendByCode[code] ?: 0.0) + trend
        }
        // average per student
        val examCountByCode = HashMap<String, Int>()
        ExamResultsTable.selectAll().where {
            (ExamResultsTable.schoolId eq schoolId) and
                (ExamResultsTable.status neq "Pending")
        }.forEach { row ->
            val code = row[ExamResultsTable.studentId]
            examCountByCode[code] = (examCountByCode[code] ?: 0) + 1
        }
        examTrendByCode.forEach { (code, total) ->
            val cnt = examCountByCode[code] ?: 1
            examTrendByCode[code] = total / cnt
        }

        // ── NEW signal: PTM engagement (class-level turnout) ────────────────
        val ptmTurnoutByClass = HashMap<String, Double>()
        PtmEventsTable.selectAll().where {
            (PtmEventsTable.schoolId eq schoolId) and
                (PtmEventsTable.expectedParents greater 0)
        }.orderBy(PtmEventsTable.createdAt, SortOrder.DESC).limit(3).forEach { evRow ->
            val evId = evRow[PtmEventsTable.id].value
            PtmClassProgressTable.selectAll().where {
                PtmClassProgressTable.ptmEventId eq evId
            }.forEach { cpRow ->
                val cls = cpRow[PtmClassProgressTable.className]
                val total = cpRow[PtmClassProgressTable.totalCount]
                val met = cpRow[PtmClassProgressTable.metCount]
                if (total > 0) {
                    val turnout = met.toDouble() / total
                    ptmTurnoutByClass[cls] = turnout  // latest event wins
                }
            }
        }

        // ── NEW signal: transport disruption ────────────────────────────────
        val transportActiveByCode = HashMap<String, Boolean>()
        TransportAssignmentsTable.selectAll().where {
            (TransportAssignmentsTable.schoolId eq schoolId) and
                (TransportAssignmentsTable.isActive eq true)
        }.forEach { row ->
            val sId = row[TransportAssignmentsTable.studentId]
            val code = codeByStudentId[sId] ?: return@forEach
            transportActiveByCode[code] = true
        }

        // ── NEW signal: Tutor mastery (leading — academic disengagement) ────
        // Average mastery per child across all subjects/topics from tutor_mastery.
        val avgMasteryByCode = HashMap<String, Double>()
        val masteryAccumByChild = HashMap<UUID, MutableList<Double>>()
        TutorMasteryTable.selectAll().where {
            TutorMasteryTable.schoolId eq schoolId
        }.forEach { row ->
            val childId = row[TutorMasteryTable.childId]
            masteryAccumByChild.getOrPut(childId) { mutableListOf() }
                .add(row[TutorMasteryTable.mastery])
        }
        masteryAccumByChild.forEach { (childId, values) ->
            val code = childCodeByChildId[childId] ?: return@forEach
            if (code.isNotEmpty() && values.isNotEmpty()) {
                avgMasteryByCode[code] = values.average()
            }
        }

        // ── NEW signal: unresolved misconceptions (leading — academic risk) ─
        // Count of unresolved misconceptions per child from tutor_misconceptions.
        val unresolvedMisconceptionsByCode = HashMap<String, Int>()
        TutorMisconceptionsTable.selectAll().where {
            (TutorMisconceptionsTable.schoolId eq schoolId) and
                (TutorMisconceptionsTable.resolved eq false)
        }.forEach { row ->
            val childId = row[TutorMisconceptionsTable.childId]
            val code = childCodeByChildId[childId] ?: return@forEach
            if (code.isNotEmpty()) {
                unresolvedMisconceptionsByCode[code] =
                    (unresolvedMisconceptionsByCode[code] ?: 0) + 1
            }
        }

        // ── school-mean baselines for relative thresholds ───────────────────
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

        // ── previous run snapshots for deltas ───────────────────────────────
        val prevDate = PewsRiskSnapshotsTable.selectAll()
            .where { PewsRiskSnapshotsTable.schoolId eq schoolId }
            .orderBy(PewsRiskSnapshotsTable.runDate, SortOrder.DESC)
            .limit(1).firstOrNull()?.get(PewsRiskSnapshotsTable.runDate)
        val prevByCode = if (prevDate != null) {
            PewsRiskSnapshotsTable.selectAll().where {
                (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                    (PewsRiskSnapshotsTable.runDate eq prevDate)
            }.associate { it[PewsRiskSnapshotsTable.studentCode] to it }
        } else emptyMap()

        // ── assemble per-student snapshots ──────────────────────────────────
        students.mapNotNull { st ->
            val attList = attPoints[st.code]?.sortedBy { it.first }
            val attPct = attList?.takeIf { it.isNotEmpty() }?.let {
                (it.count { p -> p.second } * 100.0) / it.size
            }?.toInt()
            val attSlope = attList?.let { slopeOfAttendance(it) }

            val marksList = marksPoints[st.code]
            val marksPct = marksList?.takeIf { it.isNotEmpty() }?.map { it.second }?.average()?.toInt()
            val marksSlope = marksList?.let { slopeOfMarks(it) }

            val leaveCount = leaveByCode[st.code] ?: 0
            val hwMissed = hwMissedByCode[st.code] ?: 0
            val feeOverdue = feeOverdueByCode[st.code] ?: false
            val healthFlag = healthFlagByCode[st.code] ?: false
            val examTrend = examTrendByCode[st.code]
            val ptmTurnout = ptmTurnoutByClass[st.cls]
            val transportOk = transportActiveByCode[st.code] ?: true  // no assignment = not flagged
            val avgMastery = avgMasteryByCode[st.code]
            val unresolvedMisconceptions = unresolvedMisconceptionsByCode[st.code] ?: 0

            val signals = buildList {
                // v1: absolute floors
                if (attPct != null && attPct < cfg.attendanceFloor) {
                    add(PewsSignal("attendance", "Attendance $attPct% (below ${cfg.attendanceFloor}%)",
                        if (attPct < 50) 3 else if (attPct < 65) 2 else 1,
                        isLeading = false, evidenceRef = "attendance_records/${since}..${runDate}"))
                }
                if (marksPct != null && marksPct < cfg.marksFloor) {
                    add(PewsSignal("marks", "Average $marksPct% (below ${cfg.marksFloor}%)",
                        if (marksPct < 25) 3 else 2,
                        isLeading = false, evidenceRef = "assessment_marks/avg"))
                }
                if (leaveCount >= cfg.leaveFloor) {
                    add(PewsSignal("leave", "$leaveCount leave requests filed",
                        if (leaveCount >= 5) 2 else 1,
                        isLeading = false, evidenceRef = "leave_requests/count"))
                }
                // v1: relative z-score
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
                // v1: trajectory (leading)
                if (attSlope != null && attSlope <= -10.0) {
                    add(PewsSignal("trend", "Attendance trending down (${attSlope.toInt()} pts)", 2,
                        isLeading = true, evidenceRef = "attendance_slope/${attSlope.toInt()}"))
                }
                if (marksSlope != null && marksSlope <= -10.0) {
                    add(PewsSignal("trend", "Marks trending down (${marksSlope.toInt()} pts)", 2,
                        isLeading = true, evidenceRef = "marks_slope/${marksSlope.toInt()}"))
                }
                // NEW: HW non-submission streak (leading)
                if (hwMissed >= 2) {
                    add(PewsSignal("homework", "$hwMissed homework submissions missed in last 30 days",
                        if (hwMissed >= 4) 3 else 2,
                        isLeading = true, evidenceRef = "homework_submissions/missed=${hwMissed}"))
                }
                // NEW: fee stress (financial)
                if (feeOverdue) {
                    add(PewsSignal("fees", "Has overdue fees",
                        2, isLeading = false, evidenceRef = "fee_records/OVERDUE"))
                }
                // NEW: health flag (wellbeing)
                if (healthFlag) {
                    add(PewsSignal("health", "Recent health incident (moderate/major)",
                        2, isLeading = false, evidenceRef = "student_health_incidents/recent"))
                }
                // NEW: exam trajectory (lagging-arc)
                if (examTrend != null && examTrend <= -5.0) {
                    add(PewsSignal("exam", "Exam trend declining (${examTrend.toInt()}% avg)",
                        if (examTrend <= -10.0) 3 else 2,
                        isLeading = false, evidenceRef = "exam_results/trend=${examTrend.toInt()}%"))
                }
                // NEW: PTM engagement (modifier)
                if (ptmTurnout != null && ptmTurnout < 0.4 && ptmTurnout > 0.0) {
                    add(PewsSignal("engagement", "Low PTM turnout for class (${(ptmTurnout * 100).toInt()}%)",
                        1, isLeading = false, evidenceRef = "ptm_class_progress/turnout=${(ptmTurnout * 100).toInt()}%"))
                }
                // NEW: transport disruption (external, fixable)
                if (!transportOk) {
                    add(PewsSignal("transport", "No active transport assignment",
                        1, isLeading = false, evidenceRef = "transport_assignments/inactive"))
                }
                // NEW: low Tutor mastery (leading — academic disengagement signal)
                if (avgMastery != null && avgMastery < 40.0) {
                    add(PewsSignal("mastery", "Average tutor mastery ${avgMastery.toInt()}% (below 40%)",
                        if (avgMastery < 20.0) 3 else 2,
                        isLeading = true, evidenceRef = "tutor_mastery/avg=${avgMastery.toInt()}%"))
                }
                // NEW: unresolved misconceptions (leading — persistent learning gaps)
                if (unresolvedMisconceptions >= 2) {
                    add(PewsSignal("misconception", "$unresolvedMisconceptions unresolved misconceptions from AI Tutor",
                        if (unresolvedMisconceptions >= 5) 3 else 2,
                        isLeading = true, evidenceRef = "tutor_misconceptions/unresolved=$unresolvedMisconceptions"))
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
            val score = computeScore(attPct, marksPct, leaveCount, attSlope, marksSlope,
                hwMissed, feeOverdue, healthFlag, examTrend, avgMastery,
                unresolvedMisconceptions, signals, cfg)
            val hash = signalHash(st.code, runDate, attPct, marksPct, leaveCount, signals)

            // PEWS 2.0: confidence = data completeness × signal agreement
            val dataFields = listOf(attPct, marksPct, leaveCount.takeIf { it > 0 },
                hwMissed.takeIf { it > 0 }, feeOverdue, healthFlag, examTrend,
                ptmTurnout, transportOk, avgMastery, unresolvedMisconceptions.takeIf { it > 0 })
            val completeness = dataFields.count { it != null && it != false && it != 0 } / dataFields.size.toDouble()
            val agreement = signals.map { it.severity }.distinct().size.coerceAtMost(3) / 3.0
            val confidence = (completeness * 0.6 + agreement * 0.4).coerceIn(0.0, 1.0)

            // PEWS 2.0: leading score (weight leading indicators higher)
            val leadingScore = signals.filter { it.isLeading }.sumOf {
                when (it.severity) { 3 -> 40; 2 -> 25; else -> 10 }
            }.coerceAtMost(100)

            // PEWS 2.0: cause family bucketing
            val causeFamily = bucketCauseFamily(signals)

            // PEWS 2.0: deltas vs previous run
            val prev = prevByCode[st.code]
            val deltasJson = if (prev != null) {
                val prevAtt = prev[PewsRiskSnapshotsTable.attendancePct]
                val prevMarks = prev[PewsRiskSnapshotsTable.marksPct]
                val dAtt = if (attPct != null && prevAtt != null) attPct - prevAtt else null
                val dMarks = if (marksPct != null && prevMarks != null) marksPct - prevMarks else null
                val dLeave = leaveCount - prev[PewsRiskSnapshotsTable.leaveCount]
                val dScore = score - prev[PewsRiskSnapshotsTable.riskScore]
                json.encodeToString(mapOf(
                    "attendance_pct" to dAtt,
                    "marks_pct" to dMarks,
                    "leave_count" to dLeave,
                    "risk_score" to dScore,
                ))
            } else null

            PewsSnapshot(
                schoolId = schoolId, studentCode = st.code, studentName = st.name,
                className = st.cls, section = st.sec, runDate = runDate,
                riskScore = score, riskLevel = level,
                attendancePct = attPct, marksPct = marksPct, leaveCount = leaveCount,
                attendanceSlope = attSlope, marksSlope = marksSlope,
                signals = signals, signalHash = hash,
                confidence = confidence, leadingScore = leadingScore,
                causeFamily = causeFamily, deltasJson = deltasJson,
            )
        }.sortedWith(compareByDescending<PewsSnapshot> { it.riskScore }.thenBy { it.studentName })
    }

    /** PEWS 2.0: cheap deterministic cause-family bucketing from dominant signals. */
    private fun bucketCauseFamily(signals: List<PewsSignal>): String {
        val kinds = signals.map { it.kind }.toSet()
        return when {
            "fees" in kinds -> "financial"
            "health" in kinds -> "wellbeing"
            "transport" in kinds -> "external"
            "homework" in kinds && "attendance" in kinds -> "disengagement"
            "mastery" in kinds || "misconception" in kinds -> "academic"
            "attendance" in kinds || "leave" in kinds -> "attendance"
            "marks" in kinds || "exam" in kinds || "trend" in kinds -> "academic"
            "engagement" in kinds -> "disengagement"
            else -> "academic"
        }
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
        // PEWS 2.0 expanded fields
        val confidence: Double? = null,
        val leadingScore: Int? = null,
        val causeFamily: String? = null,
        val deltasJson: String? = null,
        val hasOpenIntervention: Boolean = false,
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

    /** Cohort risk distribution per run date (last N days) for trend analytics. */
    suspend fun cohortTrend(
        schoolId: UUID, days: Int = 30,
    ): List<TrendPoint> = dbQuery {
        val since = java.time.LocalDate.now().minusDays(days.toLong())
        val rows = PewsRiskSnapshotsTable.selectAll().where {
            (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                (PewsRiskSnapshotsTable.runDate greaterEq since)
        }.toList()

        rows.groupBy { it[PewsRiskSnapshotsTable.runDate] }
            .toSortedMap()
            .map { (date, snaps) ->
                TrendPoint(
                    runDate = date.toString(),
                    total = snaps.size,
                    high = snaps.count { it[PewsRiskSnapshotsTable.riskLevel] == "high" },
                    medium = snaps.count { it[PewsRiskSnapshotsTable.riskLevel] == "medium" },
                    watch = snaps.count { it[PewsRiskSnapshotsTable.riskLevel] == "watch" },
                )
            }
    }

    data class TrendPoint(
        val runDate: String,
        val total: Int,
        val high: Int,
        val medium: Int,
        val watch: Int,
    )

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
            confidence = r[PewsRiskSnapshotsTable.confidence],
            leadingScore = r[PewsRiskSnapshotsTable.leadingScore],
            causeFamily = r[PewsRiskSnapshotsTable.causeFamily],
            deltasJson = r[PewsRiskSnapshotsTable.deltasJson],
        )
    }

    /** Enrich stored snapshots with student identity (name/class/section) + open intervention flag. */
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
        // Batch-check for open/in_progress interventions
        val openInterventionCodes = dbQuery {
            com.littlebridge.enrollplus.db.PewsInterventionsTable.selectAll().where {
                (com.littlebridge.enrollplus.db.PewsInterventionsTable.schoolId eq schoolId) and
                    (com.littlebridge.enrollplus.db.PewsInterventionsTable.studentCode inList codes) and
                    (com.littlebridge.enrollplus.db.PewsInterventionsTable.status inList listOf("open", "in_progress"))
            }.map { it[com.littlebridge.enrollplus.db.PewsInterventionsTable.studentCode] }.toSet()
        }
        return snaps.map { s ->
            val id = byCode[s.studentCode]
            val enriched = if (id == null) s else s.copy(
                studentName = id.first, className = id.second, section = id.third
            )
            if (s.studentCode in openInterventionCodes) enriched.copy(hasOpenIntervention = true) else enriched
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
        attSlope: Double?, marksSlope: Double?,
        hwMissed: Int, feeOverdue: Boolean, healthFlag: Boolean, examTrend: Double?,
        avgMastery: Double?, unresolvedMisconceptions: Int,
        signals: List<PewsSignal>, cfg: Config,
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
        // PEWS 2.0: HW non-submission (max ~12)
        if (hwMissed >= 2) score += (hwMissed * 3.0).coerceAtMost(12.0)
        // PEWS 2.0: fee stress (flat +8)
        if (feeOverdue) score += 8.0
        // PEWS 2.0: health flag (flat +6)
        if (healthFlag) score += 6.0
        // PEWS 2.0: exam trajectory decline (max ~8)
        if (examTrend != null && examTrend < 0) score += (-examTrend).coerceAtMost(8.0)
        // PEWS 2.0: low Tutor mastery (max ~10)
        if (avgMastery != null && avgMastery < 40.0) {
            score += ((40.0 - avgMastery).coerceIn(0.0, 40.0)) * 0.25
        }
        // PEWS 2.0: unresolved misconceptions (max ~10)
        if (unresolvedMisconceptions >= 2) {
            score += (unresolvedMisconceptions * 2.0).coerceAtMost(10.0)
        }
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
            signals.sortedBy { it.label }.forEach {
                append(it.kind).append(it.label).append(it.severity).append(it.isLeading).append(it.evidenceRef ?: "")
            }
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
                it[confidence] = s.confidence
                it[leadingScore] = s.leadingScore
                it[causeFamily] = s.causeFamily
                it[deltasJson] = s.deltasJson
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
                it[confidence] = s.confidence
                it[leadingScore] = s.leadingScore
                it[causeFamily] = s.causeFamily
                it[deltasJson] = s.deltasJson
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
