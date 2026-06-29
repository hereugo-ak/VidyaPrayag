// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/narrator/ReportDraft.kt
package com.littlebridge.enrollplus.feature.reportcard.narrator

import com.littlebridge.enrollplus.feature.reportcard.rollup.ReportFactBundle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * Structured Report Draft — the output of the Tier-2 Narrator Agent.
 *
 * The agent produces this JSON after gathering context via tools. The
 * GroundingGuard then verifies every number/grade/name against the
 * [ReportFactBundle]. Ungrounded fields are dropped and the draft is
 * flagged for review.
 *
 * Spec: AI_REPORT_CARD_2.0_AGENTIC_REDESIGN.md §6.3
 */
@Serializable
data class ReportDraft(
    val studentName: String,
    val className: String,
    val section: String,
    val term: String,
    val subjects: List<SubjectNarrative> = emptyList(),
    val overallSummary: String = "",
    val parentSummary: String = "",
    val projectionNote: String = "",
    val focusAreas: List<String> = emptyList(),
    val strengths: List<String> = emptyList(),
    val improvementAreas: List<String> = emptyList(),
    val teacherNote: String = "",
) {
    companion object {
        private val log = LoggerFactory.getLogger("ReportDraft")
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromJson(s: String): ReportDraft? = runCatching {
            json.decodeFromString<ReportDraft>(s)
        }.getOrElse {
            log.warn("Failed to parse ReportDraft JSON: {}", it.message)
            null
        }

        fun toJson(d: ReportDraft): String = json.encodeToString(d)
    }
}

@Serializable
data class SubjectNarrative(
    val subject: String,
    val grade: String,
    val percentage: Double,
    val narrative: String,
    val movement: String = "steady",
    val teacherNote: String = "",
)

/**
 * Build a deterministic fallback draft from the fact bundle alone.
 * Used when AI is unavailable or fails — ensures the feature always works.
 */
fun deterministicDraft(bundle: ReportFactBundle, language: String = "en"): ReportDraft {
    val subjects = bundle.subjects.map { sf ->
        SubjectNarrative(
            subject = sf.subject,
            grade = sf.grade ?: "N/A",
            percentage = sf.percentage ?: 0.0,
            narrative = buildString {
                append("${sf.subject}: ${sf.grade ?: "N/A"} (${sf.percentage?.toInt() ?: 0}%). ")
                when (sf.movement) {
                    "improved" -> append("Good improvement from last term. ")
                    "slid" -> append("Performance has declined — needs focused support. ")
                    "volatile" -> append("Performance has been inconsistent. ")
                    else -> append("Steady performance. ")
                }
                if (sf.isAbsent) append("Was absent for this assessment. ")
            },
            movement = sf.movement,
        )
    }

    val overallSummary = buildString {
        append("${bundle.studentName} achieved ${bundle.overallPct?.toInt() ?: 0}% overall ")
        append("(${bundle.overallGrade ?: "N/A"}) in ${bundle.term}. ")
        if (bundle.trajectoryLabel == "improved") append("Showing encouraging improvement. ")
        else if (bundle.trajectoryLabel == "slid") append("Needs additional support to recover. ")
        if (bundle.attendancePct != null && bundle.attendancePct < 75) {
            append("Attendance at ${bundle.attendancePct}% needs attention. ")
        }
    }

    val parentSummary = buildString {
        append("Your child ${bundle.studentName} scored ${bundle.overallPct?.toInt() ?: 0}% ")
        append("this term. ")
        if (bundle.projection != null) {
            append("Next term projection: ${bundle.projection.likelyGrade} ")
            append("(${bundle.projection.likelyPercentageRange}%). ")
            if (bundle.projection.atRisk) append("Please work with teachers on focus areas. ")
        }
    }

    return ReportDraft(
        studentName = bundle.studentName,
        className = bundle.className,
        section = bundle.section,
        term = bundle.term,
        subjects = subjects,
        overallSummary = overallSummary,
        parentSummary = parentSummary,
        projectionNote = bundle.projection?.basis ?: "",
        focusAreas = bundle.projection?.focusAreas ?: emptyList(),
        strengths = bundle.subjects.filter { it.movement == "improved" }.map { it.subject },
        improvementAreas = bundle.subjects.filter { it.movement == "slid" }.map { it.subject },
        teacherNote = "",
    )
}
