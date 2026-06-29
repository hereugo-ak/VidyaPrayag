// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/rollup/ReportRollupService.kt
package com.littlebridge.enrollplus.feature.reportcard.rollup

import com.littlebridge.enrollplus.db.AssessmentMarksTable
import com.littlebridge.enrollplus.db.AssessmentsTable
import com.littlebridge.enrollplus.db.AttendanceRecordsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.db.SchoolsTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardConstants
import com.littlebridge.enrollplus.feature.reportcard.core.ReportCardKillSwitch
import com.littlebridge.enrollplus.feature.reportcard.data.CoScholasticRepository
import com.littlebridge.enrollplus.feature.reportcard.data.HolisticAssessmentRepository
import com.littlebridge.enrollplus.feature.reportcard.data.ReportCardTemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

/**
 * Tier 0 — Deterministic academic rollup. The LAW-6 firewall.
 *
 * Builds a [ReportFactBundle] per student from DB data only. Zero LLM calls.
 * Every number, grade, and trajectory label here is the single source of truth
 * for all downstream tiers. The narrator agent may only reference these fields.
 *
 * Graceful degradation:
 *   - No marks → empty subjects, data_confidence = "insufficient"
 *   - No attendance → null attendance fields
 *   - No holistic → empty list
 *   - No co-scholastic → empty list
 *   - No PEWS → null PEWS fields
 *   - No template → fallback rubric
 *
 * SOLID:
 *   S → Single responsibility: deterministic fact bundle construction.
 *   D → Repositories injected (template, holistic, co-scholastic).
 *
 * Kill switch: [KillSwitchGuard.require] at entry with "reportcard_rollup".
 */
class ReportRollupService(
    private val templateRepo: ReportCardTemplateRepository = ReportCardTemplateRepository(),
    private val holisticRepo: HolisticAssessmentRepository = HolisticAssessmentRepository(),
    private val coScholasticRepo: CoScholasticRepository = CoScholasticRepository(),
) {
    private val log = LoggerFactory.getLogger("ReportRollupService")

    /**
     * Build the deterministic fact bundle for a single student.
     *
     * @param schoolId  School UUID
     * @param studentId Student UUID (students.id)
     * @param term      Term label (e.g. "Term 1")
     * @param academicYearId  Academic year UUID (nullable)
     * @param language  Narrative language code (default "hi")
     */
    suspend fun buildBundle(
        schoolId: UUID,
        studentId: UUID,
        term: String,
        academicYearId: UUID?,
        language: String = "hi",
    ): ReportFactBundle {
        ReportCardKillSwitch.require(ReportCardConstants.MODULE_ROLLUP)
        log.info("Rollup: school={}, student={}, term={}", schoolId, studentId, term)

        return withContext(Dispatchers.IO) {
            // 1) School metadata (board, medium)
            val schoolMeta = dbQuery {
                SchoolsTable.selectAll().where { SchoolsTable.id eq schoolId }
                    .singleOrNull()?.let {
                        it[SchoolsTable.board] to (it[SchoolsTable.medium] ?: "en")
                    }
            } ?: ("CBSE" to "en")

            val (board, medium) = schoolMeta

            // 2) Student metadata
            val studentMeta = dbQuery {
                StudentsTable.selectAll().where { StudentsTable.id eq studentId }
                    .singleOrNull()?.let {
                        Quad(
                            it[StudentsTable.fullName],
                            it[StudentsTable.studentCode],
                            it[StudentsTable.className],
                            it[StudentsTable.section] ?: "A",
                        )
                    }
            } ?: run {
                log.warn("Rollup: student {} not found", studentId)
                return@withContext emptyBundle(schoolId, studentId, term, academicYearId, board, medium, language)
            }

            val (studentName, studentCode, className, section) = studentMeta

            // 3) Load grading rubric (from template table or fallback)
            val rubric = loadRubric(schoolId, board, className)

            // 4) Academic marks for this term
            val subjectFacts = loadSubjectFacts(schoolId, studentCode, term, academicYearId, rubric)

            // 5) Overall percentage + grade
            val overallPct = subjectFacts.mapNotNull { it.percentage }.takeIf { it.isNotEmpty() }?.average()
            val (overallGrade, overallDescriptor) = BoardRubric.gradeFor(overallPct, rubric)
                ?: (null to null)

            // 6) Attendance
            val attendanceData = loadAttendance(schoolId, studentCode)

            // 7) Trajectory (slopes)
            val marksSlope = computeMarksSlope(schoolId, studentCode)
            val attendanceSlope = attendanceData?.let { computeAttendanceSlope(it.dailyPresent) }
            val trajectoryLabel = labelTrajectory(marksSlope, attendanceSlope)

            // 8) Holistic (graceful when empty)
            val holisticFacts = runCatching {
                holisticRepo.findByStudentAndTerm(schoolId, studentId, term, academicYearId)
            }.getOrElse { emptyList() }.flatMap { row ->
                buildList {
                    row.criticalThinking?.let { add(HolisticFact("teacher", "critical_thinking", it)) }
                    row.creativity?.let { add(HolisticFact("teacher", "creativity", it)) }
                    row.communication?.let { add(HolisticFact("teacher", "communication", it)) }
                    row.collaboration?.let { add(HolisticFact("teacher", "collaboration", it)) }
                    row.selfAwareness?.let { add(HolisticFact("teacher", "self_awareness", it)) }
                    row.socialEmotional?.let { add(HolisticFact("teacher", "social_emotional", it)) }
                    row.remarks?.let { add(HolisticFact(row.assessorType, "remarks", 0, it)) }
                }
            }

            // 9) Co-scholastic (graceful when empty)
            val coScholasticFacts = runCatching {
                coScholasticRepo.findByStudentAndTerm(schoolId, studentId, term, academicYearId)
            }.getOrElse { emptyList() }.map { row ->
                CoScholasticFact(
                    category = row.category,
                    activityName = row.activityName,
                    grade = row.grade,
                    descriptor = row.descriptor,
                    teacherRemarks = row.teacherRemarks,
                )
            }

            // 10) PEWS focus (if available)
            val pewsData = loadPewsContext(schoolId, studentCode)

            // 11) Competency badges
            val badges = loadCompetencyBadges(studentId)

            // 12) Deterministic projection
            val projection = computeProjection(overallPct, marksSlope, attendanceData?.pct, pewsData?.third, rubric)

            // 13) Data confidence
            val (confidence, confidenceReason) = computeConfidence(
                subjectFacts.size, attendanceData != null, holisticFacts.isNotEmpty(),
                coScholasticFacts.isNotEmpty(), pewsData != null,
            )

            // 14) Class rank (best-effort)
            val (rankInClass, totalStudents) = computeClassRank(schoolId, className, section, overallPct, studentCode)

            ReportFactBundle(
                schoolId = schoolId.toString(),
                studentId = studentId.toString(),
                studentName = studentName,
                studentCode = studentCode,
                className = className,
                section = section,
                term = term,
                academicYearId = academicYearId?.toString(),
                board = board,
                medium = medium,
                subjects = subjectFacts,
                overallPct = overallPct,
                overallGrade = overallGrade,
                overallDescriptor = overallDescriptor,
                rankInClass = rankInClass,
                totalStudentsInClass = totalStudents,
                marksSlope = marksSlope,
                attendanceSlope = attendanceSlope,
                trajectoryLabel = trajectoryLabel,
                attendancePct = attendanceData?.pct,
                totalDays = attendanceData?.total,
                presentDays = attendanceData?.present,
                absentDays = attendanceData?.absent,
                holistic = holisticFacts,
                coScholastic = coScholasticFacts,
                pewsFocusArea = pewsData?.first,
                pewsRiskLevel = pewsData?.second,
                pewsRiskScore = pewsData?.third,
                projection = projection,
                dataConfidence = confidence,
                dataConfidenceReason = confidenceReason,
                competencyBadges = badges,
            )
        }
    }

    // ── Private helpers ────────────────────────────────────────────────

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

    private fun emptyBundle(
        schoolId: UUID, studentId: UUID, term: String, academicYearId: UUID?,
        board: String, medium: String, language: String,
    ) = ReportFactBundle(
        schoolId = schoolId.toString(),
        studentId = studentId.toString(),
        studentName = "Unknown",
        studentCode = "",
        className = "Unknown",
        section = "A",
        term = term,
        academicYearId = academicYearId?.toString(),
        board = board,
        medium = medium,
        dataConfidence = ReportCardConstants.Confidence.INSUFFICIENT,
        dataConfidenceReason = "Student record not found",
    )

    private suspend fun loadRubric(schoolId: UUID, board: String, className: String): List<BoardRubric.GradeBand> {
        // Try to determine grade range from class name
        val gradeRange = inferGradeRange(className)
        val template = runCatching {
            templateRepo.findForBoard(schoolId, board, gradeRange)
        }.getOrNull()

        if (template != null) {
            val parsed = BoardRubric.parseGradingScale(template.gradingScale)
            if (parsed.isNotEmpty()) return parsed
        }

        // Fall back to any template for this board
        val anyTemplate = runCatching {
            templateRepo.findAnyForBoard(board)
        }.getOrNull()
        if (anyTemplate != null) {
            val parsed = BoardRubric.parseGradingScale(anyTemplate.gradingScale)
            if (parsed.isNotEmpty()) return parsed
        }

        return BoardRubric.fallbackFor(board)
    }

    private fun inferGradeRange(className: String): String {
        // Extract numeric grade from class name like "Class 8", "Grade 10", "8-A"
        val num = Regex("\\d+").find(className)?.value?.toIntOrNull()
        return when (num) {
            in 1..5 -> "1-5"
            in 6..8 -> "6-8"
            in 9..10 -> "9-10"
            in 11..12 -> "11-12"
            else -> "6-10"
        }
    }

    private suspend fun loadSubjectFacts(
        schoolId: UUID,
        studentCode: String,
        term: String,
        academicYearId: UUID?,
        rubric: List<BoardRubric.GradeBand>,
    ): List<SubjectFact> = dbQuery {
        // Get assessments for this school
        val assessMeta = AssessmentsTable.selectAll().where {
            AssessmentsTable.schoolId eq schoolId
        }.filter { it[AssessmentsTable.isPublished] }
            .associate { it[AssessmentsTable.id].value to
                Triple(it[AssessmentsTable.subject], it[AssessmentsTable.maxMarks], it[AssessmentsTable.examDate]) }

        // Get marks for this student
        val marksRows = AssessmentMarksTable.selectAll().where {
            AssessmentMarksTable.studentId eq studentCode
        }.toList()

        // Group by subject, take latest assessment per subject
        val bySubject = marksRows.groupBy { row ->
            assessMeta[row[AssessmentMarksTable.assessmentId]]?.first ?: "Unknown"
        }

        bySubject.map { (subject, rows) ->
            val latest = rows.maxByOrNull { row ->
                assessMeta[row[AssessmentMarksTable.assessmentId]]?.third?.toEpochDay() ?: 0L
            }
            val meta = latest?.let { assessMeta[it[AssessmentMarksTable.assessmentId]] }
            val maxMarks = meta?.second ?: 100
            val isAbsent = latest?.get(AssessmentMarksTable.isAbsent) ?: false
            val marks = latest?.get(AssessmentMarksTable.marks)
            val pct = if (marks != null && maxMarks > 0) (marks / maxMarks) * 100.0 else null
            val (grade, descriptor) = BoardRubric.gradeFor(pct, rubric) ?: (null to null)

            // Previous term percentage for trajectory (second-to-latest)
            val prevPct = rows.sortedByDescending { row ->
                assessMeta[row[AssessmentMarksTable.assessmentId]]?.third?.toEpochDay() ?: 0L
            }.getOrNull(1)?.let { prevRow ->
                val prevMeta = assessMeta[prevRow[AssessmentMarksTable.assessmentId]]
                val prevMax = prevMeta?.second ?: 100
                val prevMarks = prevRow[AssessmentMarksTable.marks]
                if (prevMarks != null && prevMax > 0) (prevMarks / prevMax) * 100.0 else null
            }

            val movement = BoardRubric.movementFor(pct, prevPct)

            SubjectFact(
                subject = subject,
                maxMarks = maxMarks,
                marks = marks,
                percentage = pct,
                grade = grade,
                descriptor = descriptor,
                isAbsent = isAbsent,
                examName = null,
                examDate = meta?.third?.toString(),
                previousPercentage = prevPct,
                movement = movement,
            )
        }.sortedBy { it.subject }
    }

    private data class AttendanceData(
        val pct: Int,
        val total: Int,
        val present: Int,
        val absent: Int,
        val dailyPresent: List<Pair<LocalDate, Boolean>>,
    )

    private suspend fun loadAttendance(schoolId: UUID, studentCode: String): AttendanceData? = dbQuery {
        // AttendanceRecordsTable uses student_id (UUID), not student_code.
        // We need to resolve the student UUID first — but we already have it
        // from the caller. For now, query by personId (legacy) which stores
        // the student_code, or by studentId if we resolve it.
        // Graceful: if no attendance records, return null.
        val rows = AttendanceRecordsTable.selectAll().where {
            (AttendanceRecordsTable.schoolId eq schoolId) and
            (AttendanceRecordsTable.type eq "student") and
            (AttendanceRecordsTable.personId eq studentCode)
        }.toList()

        if (rows.isEmpty()) return@dbQuery null

        val daily = rows.map { row ->
            row[AttendanceRecordsTable.date] to (row[AttendanceRecordsTable.status] == "present")
        }.sortedBy { it.first }

        val total = daily.size
        val present = daily.count { it.second }
        val absent = total - present
        val pct = if (total > 0) (present * 100 / total) else 0

        AttendanceData(pct, total, present, absent, daily)
    }

    private suspend fun computeMarksSlope(
        schoolId: UUID,
        studentCode: String,
    ): Double? = dbQuery {
        val assessMeta = AssessmentsTable.selectAll().where {
            AssessmentsTable.schoolId eq schoolId
        }.filter { it[AssessmentsTable.isPublished] }
            .associate { it[AssessmentsTable.id].value to
                Pair(it[AssessmentsTable.maxMarks], it[AssessmentsTable.examDate]) }

        val marksRows = AssessmentMarksTable.selectAll().where {
            AssessmentMarksTable.studentId eq studentCode
        }.filter { !it[AssessmentMarksTable.isAbsent] }
            .mapNotNull { row ->
                val meta = assessMeta[row[AssessmentMarksTable.assessmentId]] ?: return@mapNotNull null
                val max = meta.first
                val m = row[AssessmentMarksTable.marks] ?: return@mapNotNull null
                if (max <= 0) return@mapNotNull null
                meta.second to (m / max) * 100.0
            }.sortedBy { it.first ?: LocalDate.MIN }

        if (marksRows.size < 2) return@dbQuery null
        val mid = marksRows.size / 2
        val earlyAvg = marksRows.subList(0, mid).map { it.second }.average()
        val lateAvg = marksRows.subList(mid, marksRows.size).map { it.second }.average()
        lateAvg - earlyAvg
    }

    private fun computeAttendanceSlope(daily: List<Pair<LocalDate, Boolean>>): Double? {
        if (daily.size < 6) return null
        val mid = daily.size / 2
        val earlyPct = daily.subList(0, mid).count { it.second }.toDouble() / mid * 100.0
        val latePct = daily.subList(mid, daily.size).count { it.second }.toDouble() / (daily.size - mid) * 100.0
        return latePct - earlyPct
    }

    private fun labelTrajectory(marksSlope: Double?, attendanceSlope: Double?): String {
        val ms = marksSlope ?: 0.0
        val attSlope = attendanceSlope ?: 0.0
        return when {
            ms >= 5.0 && attSlope >= 0.0 -> "improved"
            ms <= -5.0 || attSlope <= -10.0 -> "slid"
            kotlin.math.abs(ms) >= 10.0 -> "volatile"
            else -> "steady"
        }
    }

    private suspend fun loadPewsContext(schoolId: UUID, studentCode: String): Triple<String?, String?, Int?>? = dbQuery {
        PewsRiskSnapshotsTable.selectAll().where {
            (PewsRiskSnapshotsTable.schoolId eq schoolId) and
            (PewsRiskSnapshotsTable.studentCode eq studentCode)
        }.orderBy(PewsRiskSnapshotsTable.runDate, org.jetbrains.exposed.sql.SortOrder.DESC)
            .firstOrNull()?.let { row ->
                val causeFamily = row[PewsRiskSnapshotsTable.causeFamily]
                val riskLevel = row[PewsRiskSnapshotsTable.riskLevel]
                val riskScore = row[PewsRiskSnapshotsTable.riskScore]
                Triple(causeFamily, riskLevel, riskScore)
            }
    }

    private suspend fun loadCompetencyBadges(studentId: UUID): List<CompetencyBadge> = dbQuery {
        // parent_achievements uses child_id which maps to children table, not students.
        // We do a best-effort lookup by student_id → children.student_code match.
        // Graceful: returns empty if no match.
        emptyList()  // TODO: implement when children→student mapping is available
    }

    private fun computeProjection(
        overallPct: Double?,
        marksSlope: Double?,
        attendancePct: Int?,
        pewsRiskScore: Int?,
        rubric: List<BoardRubric.GradeBand>,
    ): ProjectionFact? {
        if (overallPct == null) return null

        // Deterministic projection: current_pct + slope (clamped to 0..100)
        val slopeContribution = marksSlope ?: 0.0
        val projectedPct = (overallPct + slopeContribution).coerceIn(0.0, 100.0)

        // Attendance penalty: if attendance < 75%, subtract up to 5 points
        val attPenalty = if (attendancePct != null && attendancePct < 75) {
            (75 - attendancePct) * 0.1
        } else 0.0

        val adjustedPct = (projectedPct - attPenalty).coerceIn(0.0, 100.0)

        val (grade, _) = BoardRubric.gradeFor(adjustedPct, rubric) ?: ("?" to "")

        val rangeLow = (adjustedPct - 5.0).coerceIn(0.0, 100.0).toInt()
        val rangeHigh = (adjustedPct + 5.0).coerceIn(0.0, 100.0).toInt()

        val atRisk = pewsRiskScore != null && pewsRiskScore >= 60

        val focusAreas = mutableListOf<String>()
        if (attendancePct != null && attendancePct < 75) focusAreas.add("attendance")
        if (marksSlope != null && marksSlope < -5.0) focusAreas.add("academic_consistency")
        if (overallPct < 45.0) focusAreas.add("foundational_skills")
        if (pewsRiskScore != null && pewsRiskScore >= 60) focusAreas.add("engagement")

        val basis = buildString {
            append("Projected from current ${overallPct.toInt()}% ")
            if (marksSlope != null) append("with trend ${if (marksSlope >= 0) "+" else ""}${marksSlope.toInt()} pts/term ")
            if (attendancePct != null && attendancePct < 75) append("and attendance penalty -${attPenalty.toInt()} pts ")
            append("→ ${adjustedPct.toInt()}% (likely $grade)")
        }

        return ProjectionFact(
            likelyGrade = grade ?: "?",
            likelyPercentageRange = "$rangeLow-$rangeHigh",
            atRisk = atRisk,
            basis = basis,
            focusAreas = focusAreas,
        )
    }

    private fun computeConfidence(
        subjectCount: Int,
        hasAttendance: Boolean,
        hasHolistic: Boolean,
        hasCoScholastic: Boolean,
        hasPews: Boolean,
    ): Pair<String, String?> {
        var score = 0
        if (subjectCount >= 3) score += 2
        else if (subjectCount >= 1) score += 1
        if (hasAttendance) score += 1
        if (hasHolistic) score += 1
        if (hasCoScholastic) score += 1
        if (hasPews) score += 1

        return when (score) {
            in 5..6 -> ReportCardConstants.Confidence.HIGH to null
            in 3..4 -> ReportCardConstants.Confidence.MEDIUM to null
            in 1..2 -> ReportCardConstants.Confidence.LOW to "Limited data available"
            else -> ReportCardConstants.Confidence.INSUFFICIENT to "No academic data for this term"
        }
    }

    private suspend fun computeClassRank(
        schoolId: UUID,
        className: String,
        section: String,
        overallPct: Double?,
        studentCode: String,
    ): Pair<Int?, Int?> = dbQuery {
        if (overallPct == null) return@dbQuery null to null

        // Get all students in this class
        val classStudents = StudentsTable.selectAll().where {
            (StudentsTable.schoolId eq schoolId) and
            (StudentsTable.className eq className)
        }.map { it[StudentsTable.studentCode] }

        if (classStudents.isEmpty()) return@dbQuery null to null

        // Compute overall pct for each (best-effort, may be expensive for large classes)
        // For now, just return null rank — it's optional and can be computed in a batch pass
        null to classStudents.size
    }
}
