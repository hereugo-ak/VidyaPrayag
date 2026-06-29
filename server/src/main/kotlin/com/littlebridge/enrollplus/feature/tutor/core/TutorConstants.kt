// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/core/TutorConstants.kt
package com.littlebridge.enrollplus.feature.tutor.core

/**
 * Centralized module-name constants for the AI Tutor 2.0 feature.
 *
 * These strings are used as kill-switch keys in [pews_feature_flags] and as
 * feature tags in [ai_usage_log]. Every module MUST use these constants —
 * never hardcode a string.
 *
 * SOLID: I (Interface Segregation) — each module has its own kill-switch key,
 * allowing granular control without coupling.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §4 (tier architecture), §12 (DB design)
 */
object TutorConstants {
    /** Global kill switch for the entire AI Tutor feature. */
    const val MODULE_GLOBAL             = "tutor_global"

    /** Tier 0 — Sense: deterministic Learner Bundle. */
    const val MODULE_SENSE              = "tutor_sense"

    /** Tier 1 — Triage: cheap CLASSIFY, cache, cohort dedup. */
    const val MODULE_TRIAGE             = "tutor_triage"

    /** Tier 2 — Tutor Agent: tool-using agent + TutorTurn + grounding. */
    const val MODULE_AGENT              = "tutor_agent"

    /** Tier 3 — Act: render, auto-grade, FSRS update, notify, escalate. */
    const val MODULE_ACT                = "tutor_act"

    /** Tier 4 — Learn: mastery-moved analysis, misconception rollups, prompt-priors. */
    const val MODULE_LEARN              = "tutor_learn"

    /** Ingest: OCR (photo-doubt) + Voice (Whisper via Groq). */
    const val MODULE_INGEST             = "tutor_ingest"

    /** Cross-role: Teacher class heatmap. */
    const val MODULE_TEACHER_HEATMAP    = "tutor_teacher_heatmap"

    /** Cross-role: Parent progress card + safety transparency. */
    const val MODULE_PARENT_PROGRESS    = "tutor_parent_progress"

    /** Cross-role: Admin efficacy analytics. */
    const val MODULE_ADMIN_EFFICACY     = "tutor_admin_efficacy"

    /** RAG: knowledge chunks + retrieval tool (inert until Phase 5). */
    const val MODULE_RAG                = "tutor_rag"

    /** All module names for bulk seeding of feature flags. */
    val ALL_MODULES = listOf(
        MODULE_GLOBAL, MODULE_SENSE, MODULE_TRIAGE, MODULE_AGENT,
        MODULE_ACT, MODULE_LEARN, MODULE_INGEST,
        MODULE_TEACHER_HEATMAP, MODULE_PARENT_PROGRESS,
        MODULE_ADMIN_EFFICACY, MODULE_RAG,
    )

    /** Feature tag for AI usage logging. */
    const val AI_FEATURE_TAG = "ai_tutor"

    /** TutorTurn modes (spec §6.3). */
    object TurnMode {
        const val SOCRATIC_STEP  = "SOCRATIC_STEP"
        const val HINT           = "HINT"
        const val EXPLANATION    = "EXPLANATION"
        const val PRACTICE_SET   = "PRACTICE_SET"
        const val PLAN_UPDATE    = "PLAN_UPDATE"
        const val ESCALATE       = "ESCALATE"
    }

    /** Session modes (spec §12.2). */
    object SessionMode {
        const val DOUBT      = "DOUBT"
        const val PRACTICE   = "PRACTICE"
        const val CONCEPT    = "CONCEPT"
        const val PLAN       = "PLAN"
        const val DIAGNOSTIC = "DIAGNOSTIC"
    }

    /** Mastery source (spec §12.4, LAW 6: never model-invented). */
    object MasterySource {
        const val MARKS    = "MARKS"
        const val PRACTICE = "PRACTICE"
        const val BLENDED  = "BLENDED"
    }

    /** Grounded reference sources (spec §6.3). */
    object RefSource {
        const val MARKS    = "MARKS"
        const val SYLLABUS = "SYLLABUS"
        const val NCERT    = "NCERT"
        const val RAG      = "RAG"
    }
}

/**
 * Kill-switch guard for AI Tutor 2.0 modules.
 *
 * Checks both the per-module flag AND the global "tutor_global" flag.
 * If either is killed, throws [PewsDisabledException] which the global
 * StatusPages handler maps to HTTP 503 with
 * `{"tutor":"disabled","module":"<name>"}`.
 *
 * Reuses [com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig]
 * (same pattern as ReportCardKillSwitch).
 *
 * Usage:
 *   TutorKillSwitch.require(TutorConstants.MODULE_SENSE)
 */
object TutorKillSwitch {
    fun require(moduleName: String) {
        if (com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig.isKilled(
                TutorConstants.MODULE_GLOBAL
            ) || com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig.isKilled(moduleName)
        ) {
            throw TutorDisabledException(moduleName)
        }
    }

    /** Non-throwing variant for guard checks in routes. */
    fun isDisabled(moduleName: String): Boolean =
        com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig.isKilled(
            TutorConstants.MODULE_GLOBAL
        ) || com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig.isKilled(moduleName)
}
