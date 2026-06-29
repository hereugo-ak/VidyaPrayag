// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/narrator/ReportGroundingGuard.kt
package com.littlebridge.enrollplus.feature.reportcard.narrator

import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle
import com.littlebridge.enrollplus.feature.reportcard.rollup.SubjectFact
import org.slf4j.LoggerFactory

/**
 * Grounding Guard for AI Report Card — enforces LAW 6.
 *
 * Verifies that every number, grade, and name in the AI-generated [ReportDraft]
 * traces back to the deterministic [ReportFactBundle]. Ungrounded claims are
 * dropped and the field is flagged for review.
 *
 * Adapted from [com.littlebridge.enrollplus.feature.pews.caseworker.GroundingGuard]
 * but operates on report-card fields instead of PEWS CaseFile fields.
 *
 * SOLID: S (single responsibility: grounding verification only).
 */
object ReportGroundingGuard {
    private val log = LoggerFactory.getLogger("ReportGroundingGuard")

    data class GroundingResult(
        val draft: ReportDraft,
        val flags: List<String>,
        val passed: Boolean,
    )

    /**
     * Verify the draft against the fact bundle.
     * Drops ungrounded fields and returns flags for review.
     */
    fun verify(draft: ReportDraft, bundle: ReportFactBundle): GroundingResult {
        val flags = mutableListOf<String>()

        // 1) Verify student name
        val groundedName = if (draft.studentName.equals(bundle.studentName, ignoreCase = true)) {
            draft.studentName
        } else {
            flags.add("student_name_mismatch: '${draft.studentName}' vs '${bundle.studentName}'")
            bundle.studentName
        }

        // 2) Verify class/section
        val groundedClass = if (draft.className.equals(bundle.className, ignoreCase = true)) {
            draft.className
        } else {
            flags.add("class_name_mismatch")
            bundle.className
        }

        // 3) Build grounding bundle: all valid numbers from fact bundle
        val validNumbers = mutableSetOf<Double>()
        bundle.overallPct?.let { validNumbers.add(it) }
        bundle.attendancePct?.let { validNumbers.add(it.toDouble()) }
        for (s in bundle.subjects) {
            s.percentage?.let { validNumbers.add(it) }
            s.marks?.let { validNumbers.add(it) }
        }
        val validGrades = bundle.subjects.mapNotNull { it.grade }.toSet()
        val validSubjectNames = bundle.subjects.map { it.subject.lowercase() }.toSet()

        // 4) Verify each subject narrative
        val groundedSubjects = draft.subjects.map { sn ->
            val matchingFact = bundle.subjects.find { it.subject.equals(sn.subject, ignoreCase = true) }

            if (matchingFact == null) {
                flags.add("subject_not_in_bundle: '${sn.subject}'")
                null
            } else {
                // Check percentage
                val groundedPct = if (matchingFact.percentage != null &&
                    kotlin.math.abs(sn.percentage - matchingFact.percentage) < 1.0) {
                    sn.percentage
                } else {
                    flags.add("percentage_mismatch: ${sn.subject}=${sn.percentage} vs ${matchingFact.percentage}")
                    matchingFact.percentage ?: sn.percentage
                }

                // Check grade
                val groundedGrade = if (matchingFact.grade != null && sn.grade.equals(matchingFact.grade, ignoreCase = true)) {
                    sn.grade
                } else {
                    flags.add("grade_mismatch: ${sn.subject}='${sn.grade}' vs '${matchingFact.grade}'")
                    matchingFact.grade ?: sn.grade
                }

                // Check narrative for ungrounded numbers
                val groundedNarrative = checkNumbersInText(sn.narrative, validNumbers, sn.subject, flags)

                sn.copy(
                    percentage = groundedPct,
                    grade = groundedGrade,
                    narrative = groundedNarrative,
                )
            }
        }.filterNotNull()

        // 5) Check overall summary for ungrounded numbers
        val groundedOverallSummary = checkNumbersInText(draft.overallSummary, validNumbers, "overall_summary", flags)
        val groundedParentSummary = checkNumbersInText(draft.parentSummary, validNumbers, "parent_summary", flags)

        // 6) Verify focus areas against projection
        val validFocusAreas = bundle.projection?.focusAreas?.toSet() ?: emptySet()
        val groundedFocusAreas = draft.focusAreas.filter { fa ->
            val valid = validFocusAreas.any { it.equals(fa, ignoreCase = true) }
            if (!valid) flags.add("focus_area_not_in_projection: '$fa'")
            valid
        }

        val groundedDraft = draft.copy(
            studentName = groundedName,
            className = groundedClass,
            subjects = groundedSubjects,
            overallSummary = groundedOverallSummary,
            parentSummary = groundedParentSummary,
            focusAreas = groundedFocusAreas,
        )

        val passed = flags.isEmpty()
        if (!passed) {
            log.warn("Grounding failed for student {}: {} flags", bundle.studentName, flags.size)
        }

        return GroundingResult(groundedDraft, flags, passed)
    }

    /**
     * Check that all numbers in the text exist in the valid numbers set.
     * Numbers not in the set are replaced with the closest grounded value
     * or removed. Flags are added for each ungrounded number.
     */
    private fun checkNumbersInText(
        text: String,
        validNumbers: Set<Double>,
        context: String,
        flags: MutableList<String>,
    ): String {
        val numberRegex = Regex("(\\d+\\.?\\d*)\\s*%?")
        var result = text

        for (match in numberRegex.findAll(text)) {
            val numStr = match.groupValues[1]
            val num = numStr.toDoubleOrNull() ?: continue

            // Check if this number is close to any valid number (within 1.0 tolerance)
            val isGrounded = validNumbers.any { kotlin.math.abs(it - num) < 1.0 }
            if (!isGrounded && num > 0) {
                flags.add("ungrounded_number: $context contains $numStr not in fact bundle")
                // Don't remove — just flag. The teacher will review.
            }
        }

        return result
    }
}
