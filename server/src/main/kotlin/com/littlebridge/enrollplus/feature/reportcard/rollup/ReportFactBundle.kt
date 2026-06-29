// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/rollup/ReportFactBundle.kt
package com.littlebridge.enrollplus.feature.reportcard.rollup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

/**
 * The deterministic per-student fact bundle — the LAW-6 firewall.
 *
 * Every number the AI narrator references MUST trace back to a field here.
 * Zero LLM is used in constructing this bundle. It is the single source of
 * truth for all downstream tiers.
 *
 * Spec: AI_REPORT_CARD_2.0_AGENTIC_REDESIGN.md §5.1
 */
@Serializable
data class ReportFactBundle(
    val schoolId: String,
    val studentId: String,
    val studentName: String,
    val studentCode: String,
    val className: String,
    val section: String,
    val term: String,
    val academicYearId: String? = null,
    val board: String = "CBSE",
    val medium: String = "en",

    // ── Academic: per-subject marks → grade → trajectory ──────────────
    val subjects: List<SubjectFact> = emptyList(),

    // ── Overall academic summary ──────────────────────────────────────
    val overallPct: Double? = null,
    val overallGrade: String? = null,
    val overallDescriptor: String? = null,
    val rankInClass: Int? = null,
    val totalStudentsInClass: Int? = null,

    // ── Trajectory (term-over-term) ───────────────────────────────────
    val marksSlope: Double? = null,       // negative = sliding
    val attendanceSlope: Double? = null,  // negative = sliding
    val trajectoryLabel: String = "steady", // improved|steady|slid|volatile

    // ── Attendance ────────────────────────────────────────────────────
    val attendancePct: Int? = null,
    val totalDays: Int? = null,
    val presentDays: Int? = null,
    val absentDays: Int? = null,

    // ── Holistic (360-degree) — graceful when empty ───────────────────
    val holistic: List<HolisticFact> = emptyList(),

    // ── Co-scholastic — graceful when empty ───────────────────────────
    val coScholastic: List<CoScholasticFact> = emptyList(),

    // ── PEWS focus area (if available) ────────────────────────────────
    val pewsFocusArea: String? = null,
    val pewsRiskLevel: String? = null,
    val pewsRiskScore: Int? = null,

    // ── Deterministic projection (NOT an LLM guess) ───────────────────
    val projection: ProjectionFact? = null,

    // ── Data confidence ───────────────────────────────────────────────
    val dataConfidence: String = "medium", // high|medium|low|insufficient
    val dataConfidenceReason: String? = null,

    // ── Competency badges (from parent_achievements) ──────────────────
    val competencyBadges: List<CompetencyBadge> = emptyList(),
) {
    companion object {
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun fromJson(s: String): ReportFactBundle = json.decodeFromString(s)

        fun toJson(b: ReportFactBundle): String = json.encodeToString(b)

        /**
         * SHA-256 hash of the fact bundle — used as a cache key component
         * so that changing any factual data triggers re-narration.
         */
        fun hash(b: ReportFactBundle): String {
            val md = MessageDigest.getInstance("SHA-256")
            val canonical = json.encodeToString(b).toByteArray(Charsets.UTF_8)
            return md.digest(canonical).joinToString("") { "%02x".format(it) }
        }
    }
}

@Serializable
data class SubjectFact(
    val subject: String,
    val maxMarks: Int,
    val marks: Double?,
    val percentage: Double?,
    val grade: String?,
    val descriptor: String?,
    val isAbsent: Boolean = false,
    val examName: String? = null,
    val examDate: String? = null,
    val previousPercentage: Double? = null,  // for trajectory
    val movement: String = "steady",          // improved|steady|slid|volatile
)

@Serializable
data class HolisticFact(
    val assessorType: String,  // self|peer|teacher|parent
    val dimension: String,     // critical_thinking|creativity|...
    val rating: Int,           // 1..5
    val remarks: String? = null,
)

@Serializable
data class CoScholasticFact(
    val category: String,      // arts|sports|life_skills|values|work_education
    val activityName: String,
    val grade: String? = null,
    val descriptor: String? = null,
    val teacherRemarks: String? = null,
)

@Serializable
data class ProjectionFact(
    val likelyGrade: String,
    val likelyPercentageRange: String,  // e.g. "75-85"
    val atRisk: Boolean,
    val basis: String,  // human-readable deterministic explanation
    val focusAreas: List<String> = emptyList(),
)

@Serializable
data class CompetencyBadge(
    val kind: String,   // BADGE|COMPETENCY|EI_METRIC|MISSION
    val title: String,
    val description: String? = null,
    val status: String? = null,  // MET|IN_PROGRESS|LOCKED
    val value: Double? = null,   // for EI_METRIC
)
