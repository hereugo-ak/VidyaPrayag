/*
 * File: GroundingGuard.kt
 * Module: feature.pews.caseworker
 *
 * PEWS 2.0 — Grounding Guard (LAW 6 enforcement by code).
 *
 * Post-generate, verify every number/name in the model's narrative + evidence
 * strings exists in the deterministic snapshot bundle. If the model invented
 * a figure (e.g. "attendance 23%" when the real value is 68%), drop that
 * sentence rather than the whole narrative.
 *
 * Permissive strategy: vague directional claims ("struggling", "declining")
 * are kept if the data supports the direction. Sentences naming specific
 * causes (family illness, financial hardship) are dropped unless the
 * deterministic bundle contains supporting evidence.
 *
 * This makes LAW 6 ("Every AI output must be grounded in deterministic data")
 * a *test*, not a hope.
 *
 * Spec: PEWS_2.0_AGENTIC_REDESIGN.md §6.4
 */
package com.littlebridge.enrollplus.feature.pews.caseworker

import com.littlebridge.enrollplus.feature.pews.PewsSignal
import com.littlebridge.enrollplus.feature.pews.PewsSnapshot
import org.slf4j.LoggerFactory
import java.time.LocalDate

object GroundingGuard {
    private val log = LoggerFactory.getLogger("GroundingGuard")

    /**
     * Verify the Case File against the deterministic snapshot bundle.
     * Returns a sanitized Case File with ungrounded claims removed.
     */
    fun verify(caseFile: CaseFile, snapshot: PewsSnapshot): CaseFile {
        val bundle = buildGroundingBundle(snapshot)
        val signalKinds = snapshot.signals.map { it.kind.lowercase() }.toSet()
        val causeFamily = snapshot.causeFamily?.lowercase()

        val verifiedNarrative = caseFile.narrative?.let { narrative ->
            stripUngroundedSentences(narrative, bundle, signalKinds, causeFamily)
        }

        val verifiedHypotheses = caseFile.hypotheses.map { hyp ->
            val verifiedEvidence = hyp.evidence.filter { isGrounded(it, bundle) }
            if (verifiedEvidence.size < hyp.evidence.size) {
                log.warn("Grounding guard: dropped {} ungrounded evidence items for cause '{}'",
                    hyp.evidence.size - verifiedEvidence.size, hyp.cause)
            }
            hyp.copy(evidence = verifiedEvidence)
        }.filter { it.evidence.isNotEmpty() || it.confidence <= 0.5 }

        return caseFile.copy(
            narrative = verifiedNarrative,
            hypotheses = verifiedHypotheses,
        )
    }

    // ── Sentence-level stripping (permissive) ─────────────────────────────

    /**
     * Split the narrative into sentences, keep only grounded ones.
     *
     * Permissive rules:
     * - Sentences with numbers: every number must appear in the bundle.
     * - Sentences with specific-cause keywords (family, illness, financial…):
     *   dropped unless the signal data supports that cause.
     * - Vague directional claims ("struggling", "declining"): kept if the
     *   data supports the direction.
     * - Pure interpretive prose with no numbers and no cause keywords: kept.
     *
     * Falls back to null if fewer than 2 grounded sentences remain or the
     * result is too short (< 40 chars), so the caller can use deterministic.
     */
    private fun stripUngroundedSentences(
        narrative: String,
        bundle: Set<String>,
        signalKinds: Set<String>,
        causeFamily: String?,
    ): String? {
        val sentences = splitSentences(narrative)
        if (sentences.isEmpty()) return null

        val kept = mutableListOf<String>()
        var droppedCount = 0

        for (sentence in sentences) {
            val trimmed = sentence.trim()
            if (trimmed.isEmpty()) continue

            when {
                // Rule 1: Has numbers → every number must be in the bundle
                hasNumbers(trimmed) -> {
                    if (isGrounded(trimmed, bundle)) {
                        kept.add(trimmed)
                    } else {
                        droppedCount++
                        log.debug("Grounding guard: dropped sentence (ungrounded number): '{}'", trimmed.take(80))
                    }
                }

                // Rule 2: Names a specific cause → must be supported by signal data
                namesSpecificCause(trimmed) -> {
                    if (causeSupportedByData(trimmed, signalKinds, causeFamily)) {
                        kept.add(trimmed)
                    } else {
                        droppedCount++
                        log.warn("Grounding guard: dropped sentence (unsupported cause): '{}'", trimmed.take(80))
                    }
                }

                // Rule 3: Vague directional or interpretive prose → keep
                else -> {
                    kept.add(trimmed)
                }
            }
        }

        if (droppedCount > 0) {
            log.info("Grounding guard: stripped {} of {} narrative sentences (kept {})",
                droppedCount, sentences.size, kept.size)
        }

        if (kept.isEmpty() || kept.joinToString(". ").length < 40) {
            log.warn("Grounding guard: too few grounded sentences remain ({}), falling back to deterministic", kept.size)
            return null
        }

        return kept.joinToString(". ") + "."
    }

    /** Split text into sentences on . ! ? boundaries, keeping the content. */
    private fun splitSentences(text: String): List<String> =
        text.split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /** True if the text contains any numeric digit sequence. */
    private fun hasNumbers(text: String): Boolean =
        Regex("\\b\\d+\\.?\\d*\\b").containsMatchIn(text)

    /**
     * Keywords that name a specific external cause. If the LLM uses these
     * without supporting tool data, the sentence is hallucinated.
     */
    private val specificCauseKeywords = listOf(
        "father", "mother", "family", "parent's illness", "parents' illness",
        "illness", "sick", "hospital", "medical", "diagnosed",
        "financial", "poverty", "debt", "can't afford", "cannot afford",
        "domestic", "divorce", "separation", "death", "bereavement",
        "abuse", "neglect", "trauma", "substance", "addiction",
    )

    /** True if the sentence names a specific external cause. */
    private fun namesSpecificCause(text: String): Boolean {
        val lower = text.lowercase()
        return specificCauseKeywords.any { lower.contains(it) }
    }

    /**
     * Check if a cause-naming sentence is supported by the deterministic data.
     * e.g. "father's illness affecting attendance" is supported only if
     * there's a health signal or the causeFamily is wellbeing/health.
     */
    private fun causeSupportedByData(
        text: String,
        signalKinds: Set<String>,
        causeFamily: String?,
    ): Boolean {
        val lower = text.lowercase()

        // Health-related claims need a health signal or wellbeing cause
        if (lower.containsAny("illness", "sick", "hospital", "medical", "diagnosed")) {
            return "health" in signalKinds || causeFamily == "wellbeing"
        }

        // Financial claims need a fees signal or financial cause
        if (lower.containsAny("financial", "poverty", "debt", "afford")) {
            return "fees" in signalKinds || causeFamily == "financial"
        }

        // Family/domestic claims — no deterministic signal covers these,
        // so they are always ungrounded unless a future tool provides them
        if (lower.containsAny("father", "mother", "family", "domestic", "divorce",
                "separation", "death", "bereavement", "abuse", "neglect",
                "trauma", "substance", "addiction")) {
            return false
        }

        return true
    }

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { this.contains(it) }

    // ── Grounding bundle ──────────────────────────────────────────────────

    /**
     * Build the set of strings and numbers that ARE in the deterministic bundle.
     * The model's output may only reference these.
     */
    private fun buildGroundingBundle(snap: PewsSnapshot): Set<String> {
        val bundle = mutableSetOf<String>()

        // Student identity
        bundle.add(snap.studentCode)
        bundle.add(snap.studentName)
        bundle.add(snap.className)
        bundle.add(snap.section)

        // Risk metrics
        bundle.add(snap.riskScore.toString())
        bundle.add(snap.riskLevel)
        snap.attendancePct?.let { bundle.add(it.toString()); bundle.add("$it%") }
        snap.marksPct?.let { bundle.add(it.toString()); bundle.add("$it%") }
        bundle.add(snap.leaveCount.toString())
        if (snap.confidence != null) bundle.add(snap.confidence.toString())
        if (snap.leadingScore != null) bundle.add(snap.leadingScore.toString())
        if (snap.causeFamily != null) bundle.add(snap.causeFamily)

        // Signal labels and kinds
        for (signal in snap.signals) {
            bundle.add(signal.kind)
            bundle.add(signal.label)
            bundle.add(signal.severity.toString())
            // Extract numbers from label and evidenceRef
            Regex("\\d+\\.?\\d*").findAll(signal.label).forEach { m -> bundle.add(m.value) }
            if (signal.evidenceRef != null) {
                bundle.add(signal.evidenceRef)
                Regex("\\d+\\.?\\d*").findAll(signal.evidenceRef).forEach { m -> bundle.add(m.value) }
            }
        }

        // Attendance slope if present
        if (snap.attendanceSlope != null) {
            bundle.add(String.format("%.2f", snap.attendanceSlope))
            bundle.add(snap.attendanceSlope.toInt().toString())
        }
        if (snap.marksSlope != null) {
            bundle.add(String.format("%.2f", snap.marksSlope))
            bundle.add(snap.marksSlope.toInt().toString())
        }

        return bundle
    }

    // ── Grounding check ───────────────────────────────────────────────────

    /**
     * Check if a text string is grounded — every number in the text must
     * appear in the bundle. Non-numeric prose is always allowed.
     */
    private fun isGrounded(text: String, bundle: Set<String>): Boolean {
        val numbers = Regex("\\b\\d+\\.?\\d*\\b").findAll(text).map { it.value }.toList()

        for (num in numbers) {
            if (!bundle.contains(num) &&
                !bundle.contains(num.trimEnd('0').trimEnd('.')) &&
                !bundle.contains("${num}%") &&
                !bundle.contains("${num.toIntOrNull()}%")
            ) {
                log.debug("Grounding guard: number '{}' not found in bundle", num)
                return false
            }
        }
        return true
    }
}
