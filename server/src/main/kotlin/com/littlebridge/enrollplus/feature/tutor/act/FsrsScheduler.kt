// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/act/FsrsScheduler.kt
package com.littlebridge.enrollplus.feature.tutor.act

import com.littlebridge.enrollplus.feature.tutor.data.TutorReviewStateRepository
import io.github.openspacedrepetition.Card
import io.github.openspacedrepetition.Rating
import io.github.openspacedrepetition.Scheduler
import io.github.openspacedrepetition.State
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * TIER 3 — FSRS Spaced Repetition Scheduler.
 *
 * Wraps the FSRS v6 algorithm ([Scheduler]) to schedule adaptive review
 * intervals per child per topic. Reads persisted [TutorReviewStateRepository.ReviewStateRow],
 * reconstructs a FSRS [Card], calls [Scheduler.reviewCard], and writes the
 * updated state back.
 *
 * SOLID:
 *   S → Single responsibility: FSRS scheduling only.
 *   D → Depends on [TutorReviewStateRepository] abstraction, not on Exposed directly.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §7 (Auto-grade & FSRS update)
 */
class FsrsScheduler(
    private val repo: TutorReviewStateRepository = TutorReviewStateRepository(),
) {
    private val log = LoggerFactory.getLogger("FsrsScheduler")
    private val scheduler: Scheduler = Scheduler.builder().build()

    /**
     * Map a 0-100 grade to an FSRS [Rating].
     *
     * - < 30 → AGAIN (forgot)
     * - 30-59 → HARD (struggled)
     * - 60-84 → GOOD (correct)
     * - 85+ → EASY (fluent)
     */
    fun gradeToRating(gradePct: Int): Rating = when {
        gradePct < 30 -> Rating.AGAIN
        gradePct < 60 -> Rating.HARD
        gradePct < 85 -> Rating.GOOD
        else -> Rating.EASY
    }

    /**
     * Review a topic: reconstruct the FSRS card from persisted state (or create
     * a new card), run the scheduler, and persist the updated state.
     *
     * @param schoolId  tenant scope
     * @param childId   the child's UUID
     * @param topicId   the topic's UUID
     * @param gradePct  0-100 grade from auto-grading
     * @return the updated review state row
     */
    suspend fun review(
        schoolId: UUID,
        childId: UUID,
        topicId: UUID,
        gradePct: Int,
    ): TutorReviewStateRepository.ReviewStateRow {
        val rating = gradeToRating(gradePct)
        val existing = repo.findByChildAndTopic(childId, topicId)

        // Reconstruct or create the FSRS card
        val card = if (existing != null) {
            Card.builder()
                .state(mapIntToState(existing.reps, existing.lastGrade))
                .step(if (existing.reps < 2) existing.reps else null)
                .stability(existing.stability)
                .difficulty(existing.difficulty)
                .due(existing.dueAt)
                .lastReview(existing.lastReviewedAt)
                .build()
        } else {
            Card.builder().build()
        }

        // Run the FSRS scheduler
        val result = scheduler.reviewCard(card, rating)
        val updatedCard = result.card()

        val newReps = (existing?.reps ?: 0) + 1
        val newLapses = (existing?.lapses ?: 0) + if (rating == Rating.AGAIN) 1 else 0

        // Persist the updated state
        repo.upsert(
            schoolId = schoolId,
            childId = childId,
            topicId = topicId,
            stability = updatedCard.stability ?: 0.0,
            difficulty = updatedCard.difficulty ?: 0.0,
            dueAt = updatedCard.due,
            reps = newReps,
            lapses = newLapses,
            lastGrade = gradePct,
            lastReviewedAt = Instant.now(),
        )

        log.info("FsrsScheduler: reviewed topic {} for child {} — rating={}, stability={}, due={}",
            topicId, childId, rating, updatedCard.stability, updatedCard.due)

        // Return the updated row
        return repo.findByChildAndTopic(childId, topicId)!!
    }

    /**
     * Get due reviews for a child, ordered by due date ascending.
     */
    suspend fun getDueReviews(
        childId: UUID,
        limit: Int = 20,
    ): List<TutorReviewStateRepository.ReviewStateRow> {
        return repo.findDueReviews(childId, limit = limit)
    }

    private fun mapIntToState(reps: Int, lastGrade: Int): State {
        if (reps == 0) return State.LEARNING
        if (lastGrade < 30) return State.RELEARNING
        return State.REVIEW
    }
}
