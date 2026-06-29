// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/sense/LearnerBundleBuilder.kt
package com.littlebridge.enrollplus.feature.tutor.sense

import com.littlebridge.enrollplus.db.AssessmentMarksTable
import com.littlebridge.enrollplus.db.AssessmentsTable
import com.littlebridge.enrollplus.db.CalendarEventsTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.CurriculumUnitsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.HomeworkSubmissionsTable
import com.littlebridge.enrollplus.db.HomeworkTable
import com.littlebridge.enrollplus.db.SchoolsTable
import com.littlebridge.enrollplus.db.SchoolClassesTable
import com.littlebridge.enrollplus.db.SchoolSubjectsTable
import com.littlebridge.enrollplus.db.SyllabusProgressTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import com.littlebridge.enrollplus.feature.tutor.data.TutorReviewStateRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * TIER 0 — Deterministic Learner Bundle builder.
 *
 * Computes the [LearnerBundle] in plain Kotlin/SQL, **no LLM**. This is the
 * agent's ground truth and the LAW-6 firewall: every number the agent cites
 * must trace back to a field in this bundle.
 *
 * Kill switch: [TutorKillSwitch.require] at entry with `tutor_sense`.
 *
 * SOLID:
 *   S → Single responsibility: builds the deterministic bundle.
 *   D → Depends on Exposed table abstractions, not on any AI service.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §5
 */
class LearnerBundleBuilder(
    private val reviewStateRepo: TutorReviewStateRepository = TutorReviewStateRepository(),
) {
    private val log = LoggerFactory.getLogger("TutorSense")
    private val json = Json { encodeDefaults = true }

    /**
     * Build the deterministic Learner Bundle for a child + subject.
     *
     * @param childId  the child's UUID (from ChildrenTable)
     * @param subjectId the subject's UUID (from SchoolSubjectsTable)
     * @return the grounded LearnerBundle, or null if the child/subject can't be resolved
     */
    suspend fun build(childId: UUID, subjectId: UUID): LearnerBundle? {
        TutorKillSwitch.require(TutorConstants.MODULE_SENSE)

        // 1. Resolve child → school + student code + class
        val child = dbQuery {
            ChildrenTable.selectAll().where { ChildrenTable.id eq childId }.singleOrNull()
        } ?: run {
            log.warn("LearnerBundleBuilder: child {} not found", childId)
            return null
        }

        val schoolId = child[ChildrenTable.schoolId] ?: run {
            log.warn("LearnerBundleBuilder: child {} has no school link", childId)
            return null
        }
        val studentCode = child[ChildrenTable.studentCode]

        // 2. Resolve school board for thresholds
        val board = dbQuery {
            SchoolsTable.selectAll().where { SchoolsTable.id eq schoolId }
                .singleOrNull()?.get(SchoolsTable.board) ?: "CBSE"
        } ?: "CBSE"

        // 3. Resolve classId from student record or child's currentGrade
        val student = studentCode?.let { code ->
            dbQuery {
                StudentsTable.selectAll().where {
                    (StudentsTable.schoolId eq schoolId) and
                    (StudentsTable.studentCode eq code)
                }.singleOrNull()
            }
        }
        val className = student?.get(StudentsTable.className) ?: child[ChildrenTable.currentGrade] ?: ""
        val section = student?.get(StudentsTable.section) ?: "A"

        val classId = dbQuery {
            SchoolClassesTable.selectAll().where {
                (SchoolClassesTable.schoolId eq schoolId) and
                (SchoolClassesTable.code eq className)
            }.singleOrNull()?.get(SchoolClassesTable.id)?.value
        }

        // 4. Resolve subject
        val subject = dbQuery {
            SchoolSubjectsTable.selectAll().where { SchoolSubjectsTable.id eq subjectId }
                .singleOrNull()
        } ?: run {
            log.warn("LearnerBundleBuilder: subject {} not found", subjectId)
            return null
        }

        // 5. Build syllabus position from CurriculumUnitsTable + SyllabusProgressTable
        val syllabusPosition = buildSyllabusPosition(schoolId, classId, subjectId, section)

        // 6. Build per-topic performance from AssessmentMarksTable
        val performance = buildPerformance(schoolId, subjectId, studentCode)

        // 7. Compute weak topics (covered only, pct below board threshold)
        val weakTopics = computeWeakTopics(performance, syllabusPosition.coveredTopicIds, board)

        // 8. Build homework context
        val homeworkContext = buildHomeworkContext(schoolId, subjectId, studentCode, weakTopics)

        // 9. Build review queue from FSRS state
        val reviewQueue = buildReviewQueue(childId)

        // 10. Build upcoming events
        val upcoming = buildUpcoming(schoolId)

        // 11. Data confidence
        val dataConfidence = DataConfidence(
            hasMarks = performance.perTopicScore.isNotEmpty(),
            hasSyllabus = syllabusPosition.coveredTopicIds.isNotEmpty(),
            hasHomework = homeworkContext.dueSoon.isNotEmpty() || homeworkContext.missed.isNotEmpty(),
        )

        return LearnerBundle(
            childId = childId.toString(),
            classId = classId?.toString(),
            subjectId = subjectId.toString(),
            academicYearId = null,  // resolved per-need in future ticks
            syllabusPosition = syllabusPosition,
            performance = performance,
            weakTopics = weakTopics,
            homeworkContext = homeworkContext,
            reviewQueue = reviewQueue,
            upcoming = upcoming,
            dataConfidence = dataConfidence,
        )
    }

    /** Serialize the bundle to JSON for tool consumption / caching. */
    fun toJson(bundle: LearnerBundle): String = json.encodeToString(bundle)

    // ── Private builders ────────────────────────────────────────────────

    private suspend fun buildSyllabusPosition(
        schoolId: UUID,
        classId: UUID?,
        subjectId: UUID,
        section: String,
    ): SyllabusPosition {
        if (classId == null) return SyllabusPosition()

        val units = dbQuery {
            CurriculumUnitsTable.selectAll().where {
                (CurriculumUnitsTable.schoolId eq schoolId) and
                (CurriculumUnitsTable.classId eq classId) and
                (CurriculumUnitsTable.subjectId eq subjectId) and
                (CurriculumUnitsTable.isActive eq true)
            }.orderBy(CurriculumUnitsTable.position to SortOrder.ASC).toList()
        }

        if (units.isEmpty()) return SyllabusPosition()

        val coveredIds = mutableListOf<String>()
        val notCoveredIds = mutableListOf<String>()
        var currentChapter: String? = null
        var currentTopic: String? = null

        for (unit in units) {
            val unitId = unit[CurriculumUnitsTable.id].value
            val isChapter = unit[CurriculumUnitsTable.parentId] == null
            val title = unit[CurriculumUnitsTable.title]

            val isCovered = dbQuery {
                SyllabusProgressTable.selectAll().where {
                    (SyllabusProgressTable.unitId eq unitId) and
                    (SyllabusProgressTable.section eq section) and
                    (SyllabusProgressTable.isCovered eq true)
                }.any()
            }

            if (isCovered) {
                coveredIds.add(unitId.toString())
                if (isChapter) currentChapter = title
                if (!isChapter && currentTopic == null) currentTopic = title
            } else {
                notCoveredIds.add(unitId.toString())
            }
        }

        return SyllabusPosition(
            currentChapter = currentChapter,
            currentTopic = currentTopic,
            coveredTopicIds = coveredIds,
            notYetCoveredIds = notCoveredIds,
        )
    }

    private suspend fun buildPerformance(
        schoolId: UUID,
        subjectId: UUID,
        studentCode: String?,
    ): Performance {
        if (studentCode == null) return Performance()

        // Get all published assessments for this subject (subjectId is nullable — filter with isNull)
        val assessments = dbQuery {
            AssessmentsTable.selectAll().where {
                (AssessmentsTable.schoolId eq schoolId) and
                (AssessmentsTable.subjectId eq subjectId) and
                (AssessmentsTable.isPublished eq true)
            }.toList()
        }

        if (assessments.isEmpty()) return Performance()

        // Get marks for this student across those assessments
        val marksByAssessment = mutableMapOf<UUID, Pair<Double, Int>>() // assessmentId -> (marks, maxMarks)
        for (assessment in assessments) {
            val assessmentId = assessment[AssessmentsTable.id].value
            val maxMarks = assessment[AssessmentsTable.maxMarks]

            val markRow = dbQuery {
                AssessmentMarksTable.selectAll().where {
                    (AssessmentMarksTable.assessmentId eq assessmentId) and
                    (AssessmentMarksTable.studentId eq studentCode)
                }.singleOrNull()
            }
            if (markRow != null && !markRow[AssessmentMarksTable.isAbsent]) {
                val marks = markRow[AssessmentMarksTable.marks] ?: continue
                marksByAssessment[assessmentId] = marks to maxMarks
            }
        }

        if (marksByAssessment.isEmpty()) return Performance()

        // Group by topicId if available, otherwise aggregate at subject level
        val topicScores = mutableListOf<TopicScore>()
        val byTopic = mutableMapOf<UUID?, MutableList<Pair<Double, Int>>>()

        for ((assessmentId, marksPair) in marksByAssessment) {
            val assessment = assessments.find { it[AssessmentsTable.id].value == assessmentId }
            val topicId = assessment?.get(AssessmentsTable.topicId)
            byTopic.getOrPut(topicId) { mutableListOf() }.add(marksPair)
        }

        for ((topicId, scoreList) in byTopic) {
            val totalPct = scoreList.map { (marks, max) ->
                if (max > 0) (marks / max) * 100.0 else 0.0
            }.average()
            val lastAssessed = assessments
                .filter { it[AssessmentsTable.topicId] == topicId && marksByAssessment.containsKey(it[AssessmentsTable.id].value) }
                .maxByOrNull { it[AssessmentsTable.examDate]?.toString() ?: "" }
                ?.get(AssessmentsTable.examDate)?.toString()

            topicScores.add(TopicScore(
                topicId = topicId?.toString() ?: "subject_level",
                pct = totalPct,
                attempts = scoreList.size,
                lastAssessedOn = lastAssessed,
            ))
        }

        return Performance(perTopicScore = topicScores)
    }

    private fun computeWeakTopics(
        performance: Performance,
        coveredTopicIds: List<String>,
        board: String,
    ): List<WeakTopic> {
        val coveredSet = coveredTopicIds.toSet()
        return performance.perTopicScore
            .filter { score ->
                // Only topics that are covered (isCovered = true) can be "weak"
                // Subject-level scores (topicId = "subject_level") are always included
                score.topicId == "subject_level" || coveredSet.contains(score.topicId)
            }
            .filter { score -> BoardThresholds.isWeak(score.pct, board) }
            .map { score ->
                WeakTopic(
                    topicId = score.topicId,
                    pct = score.pct,
                    severity = BoardThresholds.severity(score.pct, board),
                )
            }
            .sortedBy { it.pct }
    }

    private suspend fun buildHomeworkContext(
        schoolId: UUID,
        subjectId: UUID,
        studentCode: String?,
        weakTopics: List<WeakTopic>,
    ): HomeworkContext {
        if (studentCode == null) return HomeworkContext()

        val now = LocalDate.now()
        val soonCutoff = now.plusDays(7)

        val homeworks = dbQuery {
            HomeworkTable.selectAll().where {
                (HomeworkTable.schoolId eq schoolId) and
                (HomeworkTable.subjectId eq subjectId) and
                (HomeworkTable.isActive eq true)
            }.toList()
        }

        if (homeworks.isEmpty()) return HomeworkContext()

        val dueSoon = mutableListOf<HomeworkItem>()
        val missed = mutableListOf<HomeworkItem>()
        val weakTopicIds = weakTopics.map { it.topicId }.toSet()

        for (hw in homeworks) {
            val hwId = hw[HomeworkTable.id].value
            val dueDate = hw[HomeworkTable.dueDate]
            val title = hw[HomeworkTable.title]

            val submission = dbQuery {
                HomeworkSubmissionsTable.selectAll().where {
                    (HomeworkSubmissionsTable.homeworkId eq hwId) and
                    (HomeworkSubmissionsTable.studentId eq studentCode)
                }.singleOrNull()
            }

            val status = submission?.get(HomeworkSubmissionsTable.status) ?: "not_submitted"
            val item = HomeworkItem(
                homeworkId = hwId.toString(),
                title = title,
                dueDate = dueDate.toString(),
                status = status,
            )

            if (status == "not_submitted" && dueDate.isBefore(now)) {
                missed.add(item)
            } else if (status == "not_submitted" && !dueDate.isBefore(now) && !dueDate.isAfter(soonCutoff)) {
                dueSoon.add(item)
            }
        }

        val missedOnWeakTopic = if (weakTopicIds.isNotEmpty()) {
            missed // Simplification: if there are weak topics, all missed homework is flagged
        } else emptyList()

        return HomeworkContext(
            dueSoon = dueSoon,
            missed = missed,
            missedOnWeakTopic = missedOnWeakTopic,
        )
    }

    private suspend fun buildReviewQueue(childId: UUID): List<ReviewItem> {
        val dueReviews = reviewStateRepo.findDueReviews(childId, limit = 10)
        return dueReviews.map { state ->
            ReviewItem(
                topicId = state.topicId.toString(),
                dueAt = state.dueAt.toString(),
                stability = state.stability,
                difficulty = state.difficulty,
            )
        }
    }

    private suspend fun buildUpcoming(schoolId: UUID): Upcoming {
        val now = LocalDate.now()
        val soonCutoff = now.plusDays(14)

        val events = dbQuery {
            CalendarEventsTable.selectAll().where {
                CalendarEventsTable.schoolId eq schoolId
            }.toList()
        }

        val tests = mutableListOf<UpcomingEvent>()
        val otherEvents = mutableListOf<UpcomingEvent>()

        for (event in events) {
            val startDate = event[CalendarEventsTable.startDate]
            if (startDate.isBefore(now) || startDate.isAfter(soonCutoff)) continue

            val eventType = event[CalendarEventsTable.type]
            val item = UpcomingEvent(
                eventId = event[CalendarEventsTable.id].value.toString(),
                title = event[CalendarEventsTable.title],
                startDate = startDate.toString(),
                endDate = event[CalendarEventsTable.endDate]?.toString(),
                type = eventType,
            )

            if (eventType == "EXAM" || eventType == "ASSESSMENT") {
                tests.add(item)
            } else {
                otherEvents.add(item)
            }
        }

        return Upcoming(tests = tests, events = otherEvents)
    }
}
