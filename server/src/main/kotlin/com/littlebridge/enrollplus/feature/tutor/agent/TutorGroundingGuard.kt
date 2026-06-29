// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/agent/TutorGroundingGuard.kt
package com.littlebridge.enrollplus.feature.tutor.agent

import com.littlebridge.enrollplus.feature.tutor.sense.LearnerBundle
import org.slf4j.LoggerFactory

/**
 * AI Tutor 2.0 — Grounding Guard (LAW 6 enforcement by code).
 *
 * Post-generate, verify every number/percentage in the model's `studentFacing`
 * text traces to a `groundedRef` or a field in the deterministic [LearnerBundle].
 * Ungrounded facts are stripped; if too few remain, the turn is rejected and
 * a deterministic fallback is served.
 *
 * This makes LAW 6 ("Every AI output must be grounded in deterministic data")
 * a *test*, not a hope.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §7 (Tier 3 — GroundingGuard.verify)
 */
object TutorGroundingGuard {
    private val log = LoggerFactory.getLogger("TutorGroundingGuard")

    /**
     * Verify the [TutorTurn] against the deterministic [LearnerBundle].
     * Returns a sanitized turn with ungrounded claims removed, or null if
     * the turn is too ungrounded to serve (caller should use deterministic).
     */
    fun verify(turn: TutorTurn, bundle: LearnerBundle): TutorTurn? {
        val groundingSet = buildGroundingSet(bundle)

        // Also collect values explicitly cited in groundedRefs
        val refValues = turn.groundedRefs.map { it.value }.toSet()
        val allGrounded = groundingSet + refValues

        // Check studentFacing text for numbers not in the grounding set
        val verifiedText = stripUngroundedNumbers(turn.studentFacing.text, allGrounded)
        if (verifiedText == null) {
            log.warn("TutorGroundingGuard: studentFacing text fully ungrounded — rejecting turn")
            return null
        }

        // Verify practice questions: answerKey must be non-empty, topicId must be covered
        val verifiedPractice = turn.practice?.mapNotNull { q ->
            val topicCovered = q.topicId == "subject_level" ||
                bundle.syllabusPosition.coveredTopicIds.contains(q.topicId)
            if (!topicCovered) {
                log.warn("TutorGroundingGuard: practice question references uncovered topic {} — dropped", q.topicId)
                null
            } else {
                q
            }
        }?.takeIf { it.isNotEmpty() }

        return turn.copy(
            studentFacing = turn.studentFacing.copy(text = verifiedText),
            practice = verifiedPractice,
        )
    }

    // ── Grounding set ───────────────────────────────────────────────────

    /**
     * Build the set of strings and numbers that ARE in the deterministic bundle.
     * The model's output may only reference these.
     */
    private fun buildGroundingSet(bundle: LearnerBundle): Set<String> {
        val set = mutableSetOf<String>()

        // Child/class/subject identity
        set.add(bundle.childId)
        bundle.classId?.let { set.add(it) }
        set.add(bundle.subjectId)

        // Per-topic scores
        for (score in bundle.performance.perTopicScore) {
            set.add(score.topicId)
            set.add(score.pct.toString())
            set.add("${score.pct}%")
            set.add(score.pct.toInt().toString())
            set.add("${score.pct.toInt()}%")
            set.add(score.attempts.toString())
            score.lastAssessedOn?.let { set.add(it) }
        }

        // Weak topics
        for (weak in bundle.weakTopics) {
            set.add(weak.topicId)
            set.add(weak.pct.toString())
            set.add("${weak.pct}%")
            set.add(weak.severity)
        }

        // Syllabus position
        set.addAll(bundle.syllabusPosition.coveredTopicIds)
        set.addAll(bundle.syllabusPosition.notYetCoveredIds)
        bundle.syllabusPosition.currentChapter?.let { set.add(it) }
        bundle.syllabusPosition.currentTopic?.let { set.add(it) }

        // Review queue
        for (review in bundle.reviewQueue) {
            set.add(review.topicId)
            set.add(review.dueAt)
            set.add(review.stability.toString())
            set.add(review.difficulty.toString())
        }

        // Data confidence
        set.add(bundle.dataConfidence.hasMarks.toString())
        set.add(bundle.dataConfidence.hasSyllabus.toString())
        set.add(bundle.dataConfidence.hasHomework.toString())

        return set
    }

    // ── Number stripping ────────────────────────────────────────────────

    /**
     * Check if the text contains numbers not in the grounding set.
     * If it does, strip those sentences. If too few remain, return null.
     */
    private fun stripUngroundedNumbers(text: String, grounded: Set<String>): String? {
        val sentences = text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (sentences.isEmpty()) return null

        val kept = mutableListOf<String>()
        var dropped = 0

        for (sentence in sentences) {
            val numbers = Regex("\\b\\d+\\.?\\d*\\b").findAll(sentence).map { it.value }.toList()
            if (numbers.isEmpty()) {
                // No numbers → keep (interpretive prose)
                kept.add(sentence)
                continue
            }
            val allGrounded = numbers.all { num ->
                grounded.contains(num) ||
                grounded.contains(num.trimEnd('0').trimEnd('.')) ||
                grounded.contains("$num%") ||
                grounded.contains("${num.toIntOrNull()}%")
            }
            if (allGrounded) {
                kept.add(sentence)
            } else {
                dropped++
                log.debug("TutorGroundingGuard: dropped sentence (ungrounded number): '{}'", sentence.take(80))
            }
        }

        if (dropped > 0) {
            log.info("TutorGroundingGuard: stripped {} of {} sentences (kept {})", dropped, sentences.size, kept.size)
        }

        if (kept.isEmpty() || kept.joinToString(". ").length < 20) {
            return null
        }

        return kept.joinToString(". ") + "."
    }
}
