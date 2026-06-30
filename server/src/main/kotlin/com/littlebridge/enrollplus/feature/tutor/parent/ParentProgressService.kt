// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/parent/ParentProgressService.kt
package com.littlebridge.enrollplus.feature.tutor.parent

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TutorMasteryTable
import com.littlebridge.enrollplus.db.TutorSessionsTable
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import com.littlebridge.enrollplus.feature.tutor.data.TutorMasteryRepository
import com.littlebridge.enrollplus.feature.tutor.data.TutorSessionRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * Cross-role — Parent Progress Service.
 *
 * Builds the parent-facing progress card: mastery deltas, doubts resolved,
 * safety transparency. Parents see only their own child's data.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §10 (Parent progress card)
 *   "Fractions: 38% → 70% over 3 weeks. 12 doubts resolved, 0 answers given."
 *
 * Kill-switched under module name "tutor_parent_progress".
 *
 * SOLID:
 *   S → Single responsibility: aggregate child progress for parent view.
 *   D → Depends on repository abstractions.
 */
class ParentProgressService(
    private val masteryRepo: TutorMasteryRepository = TutorMasteryRepository(),
    private val sessionRepo: TutorSessionRepository = TutorSessionRepository(),
) {
    private val log = LoggerFactory.getLogger("ParentProgressService")

    data class ProgressCard(
        val childId: UUID,
        val subjectId: UUID,
        val topicProgress: List<TopicProgress>,
        val totalDoubtsResolved: Int,
        val totalAnswersGiven: Int,
        val totalSessions: Int,
        val safetyFlags: Int,
    )

    data class TopicProgress(
        val topicId: UUID,
        val currentMastery: Double,
        val source: String,
        val attempts: Int,
        val correct: Int,
    )

    /**
     * Build the progress card for a child+subject.
     */
    suspend fun buildProgressCard(
        childId: UUID,
        subjectId: UUID,
    ): ProgressCard {
        TutorKillSwitch.require(TutorConstants.MODULE_PARENT_PROGRESS)

        val masteryRows = masteryRepo.findByChildAndSubject(childId, subjectId)

        val topicProgress = masteryRows.map { m ->
            TopicProgress(
                topicId = m.topicId,
                currentMastery = m.mastery,
                source = m.source,
                attempts = m.attempts,
                correct = m.correct,
            )
        }

        // Count sessions and safety flags
        val sessions = dbQuery {
            TutorSessionsTable.selectAll().where {
                (TutorSessionsTable.childId eq childId) and
                (TutorSessionsTable.subjectId eq subjectId)
            }.toList()
        }

        val totalDoubtsResolved = sessions.count { it[TutorSessionsTable.mode] == "DOUBT" }
        val totalAnswersGiven = sessions.count { it[TutorSessionsTable.safetyFlag] == "answer_given" }
        val safetyFlags = sessions.count { it[TutorSessionsTable.safetyFlag] != null }

        log.info("ParentProgress: built card for child={} subject={} — {} topics, {} sessions",
            childId, subjectId, topicProgress.size, sessions.size)

        return ProgressCard(
            childId = childId,
            subjectId = subjectId,
            topicProgress = topicProgress,
            totalDoubtsResolved = totalDoubtsResolved,
            totalAnswersGiven = totalAnswersGiven,
            totalSessions = sessions.size,
            safetyFlags = safetyFlags,
        )
    }
}
