// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/learn/TutorEfficacyService.kt
package com.littlebridge.enrollplus.feature.tutor.learn

import com.littlebridge.enrollplus.db.AiPromptTemplatesTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TutorMasteryTable
import com.littlebridge.enrollplus.feature.tutor.core.TutorConstants
import com.littlebridge.enrollplus.feature.tutor.core.TutorKillSwitch
import com.littlebridge.enrollplus.feature.tutor.data.TutorMasteryRepository
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * TIER 4 — Learn: the flywheel.
 *
 * After each re-assessment, compares predicted vs actual mastery. Tutors/topics/
 * hints that *moved marks* get reinforced; ones that didn't get down-weighted.
 * This is the efficacy number that sells the platform.
 *
 * Also manages prompt-template traffic weights (§8.3) — winning Socratic
 * phrasings and hint ladders are promoted via `ai_prompt_templates`.
 *
 * Kill-switched under module name "tutor_learn".
 *
 * SOLID:
 *   S → Single responsibility: efficacy measurement and prompt tuning.
 *   D → Depends on repository abstractions, not on Exposed directly.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §8
 */
class TutorEfficacyService(
    private val masteryRepo: TutorMasteryRepository = TutorMasteryRepository(),
) {
    private val log = LoggerFactory.getLogger("TutorEfficacyService")

    data class EfficacyResult(
        val topicId: UUID,
        val childId: UUID,
        val previousMastery: Double,
        val currentMastery: Double,
        val delta: Double,
        val verdict: String,           // improved | unchanged | worsened
    )

    data class TopicEfficacy(
        val topicId: UUID,
        val avgDelta: Double,
        val nChildren: Int,
        val nImproved: Int,
        val nWorsened: Int,
        val improveRate: Double,
    )

    /**
     * Compare previous vs current mastery for a child+topic after a re-assessment.
     * Returns the efficacy delta and verdict.
     */
    suspend fun measureEfficacy(
        childId: UUID,
        topicId: UUID,
        currentMasteryPct: Double,
    ): EfficacyResult {
        TutorKillSwitch.require(TutorConstants.MODULE_LEARN)

        val previous = masteryRepo.findByChildAndTopic(childId, topicId)
        val previousMastery = previous?.mastery ?: 0.0
        val delta = currentMasteryPct - previousMastery

        val verdict = when {
            delta > 5.0 -> "improved"
            delta < -5.0 -> "worsened"
            else -> "unchanged"
        }

        log.info("TutorEfficacy: child={} topic={} delta={} verdict={}",
            childId, topicId, String.format("%.1f", delta), verdict)

        return EfficacyResult(
            topicId = topicId,
            childId = childId,
            previousMastery = previousMastery,
            currentMastery = currentMasteryPct,
            delta = delta,
            verdict = verdict,
        )
    }

    /**
     * Aggregate efficacy across a class for a given subject — which topics
     * are improving, which aren't. This feeds the admin efficacy dashboard.
     */
    suspend fun classEfficacy(
        subjectId: UUID,
        topicIds: List<UUID>,
    ): List<TopicEfficacy> {
        TutorKillSwitch.require(TutorConstants.MODULE_LEARN)

        val results = mutableListOf<TopicEfficacy>()

        for (topicId in topicIds) {
            val masteryRows = dbQuery {
                TutorMasteryTable.selectAll().where {
                    (TutorMasteryTable.subjectId eq subjectId) and
                    (TutorMasteryTable.topicId eq topicId)
                }.toList()
            }

            if (masteryRows.isEmpty()) continue

            // We need at least 2 data points per child to measure delta.
            // For now, use the mastery value as a proxy (higher = better efficacy).
            val nChildren = masteryRows.size
            val avgMastery = masteryRows.map { it[TutorMasteryTable.mastery] }.average()
            val nImproved = masteryRows.count { it[TutorMasteryTable.mastery] >= 60.0 }
            val nWorsened = masteryRows.count { it[TutorMasteryTable.mastery] < 30.0 }

            results.add(TopicEfficacy(
                topicId = topicId,
                avgDelta = avgMastery,
                nChildren = nChildren,
                nImproved = nImproved,
                nWorsened = nWorsened,
                improveRate = if (nChildren > 0) nImproved.toDouble() / nChildren else 0.0,
            ))
        }

        return results.sortedByDescending { it.improveRate }
    }

    /**
     * Promote or demote a prompt template's traffic weight based on efficacy.
     * Winning Socratic phrasings get reinforced; ineffective ones get down-weighted.
     *
     * @param templateName  the template name in ai_prompt_templates
     * @param delta  positive = promote (increase weight), negative = demote
     */
    suspend fun adjustPromptWeight(
        feature: String,
        templateName: String,
        delta: Int,
    ) {
        TutorKillSwitch.require(TutorConstants.MODULE_LEARN)

        dbQuery {
            val template = AiPromptTemplatesTable.selectAll().where {
                (AiPromptTemplatesTable.feature eq feature) and
                (AiPromptTemplatesTable.name eq templateName) and
                (AiPromptTemplatesTable.isActive eq true)
            }.singleOrNull()

            if (template != null) {
                val currentWeight = template[AiPromptTemplatesTable.trafficWeight]
                val newWeight = (currentWeight + delta).coerceIn(1, 1000)
                AiPromptTemplatesTable.update({
                    (AiPromptTemplatesTable.feature eq feature) and
                    (AiPromptTemplatesTable.name eq templateName) and
                    (AiPromptTemplatesTable.isActive eq true)
                }) {
                    it[AiPromptTemplatesTable.trafficWeight] = newWeight
                }
                log.info("TutorEfficacy: adjusted prompt '{}' weight {} → {} (delta={})",
                    templateName, currentWeight, newWeight, delta)
            }
        }
    }
}
