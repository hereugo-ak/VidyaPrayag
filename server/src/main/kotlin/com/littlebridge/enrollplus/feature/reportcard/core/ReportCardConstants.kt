// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/core/ReportCardConstants.kt
package com.littlebridge.enrollplus.feature.reportcard.core

/**
 * Centralized module-name constants for the AI Report Card 2.0 feature.
 *
 * These strings are used as kill-switch keys in [pews_feature_flags] and as
 * feature tags in [ai_usage_log]. Every module MUST use these constants —
 * never hardcode a string.
 *
 * SOLID: I (Interface Segregation) — each module has its own kill-switch key,
 * allowing granular control without coupling.
 */
object ReportCardConstants {
    /** Global kill switch for the entire report card feature. */
    const val MODULE_GLOBAL        = "reportcard"

    /** Tier 0 — deterministic rollup. */
    const val MODULE_ROLLUP        = "reportcard_rollup"

    /** Tier 1 — triage / cohort-dedup. */
    const val MODULE_TRIAGE        = "reportcard_triage"

    /** Tier 2 — narrator agent. */
    const val MODULE_NARRATOR      = "reportcard_narrator"

    /** Tier 3 — assembly & publish. */
    const val MODULE_ASSEMBLE      = "reportcard_assemble"

    /** Tier 4 — learn flywheel. */
    const val MODULE_LEARN         = "reportcard_learn"

    /** All module names for bulk registration / seeding. */
    val ALL_MODULES = listOf(
        MODULE_GLOBAL, MODULE_ROLLUP, MODULE_TRIAGE,
        MODULE_NARRATOR, MODULE_ASSEMBLE, MODULE_LEARN
    )

    /** Feature tag for AI usage logging. */
    const val AI_FEATURE_TAG = "report_card"

    /** Draft status values for the state machine. */
    object DraftStatus {
        const val DRAFT       = "draft"
        const val FLAGGED     = "flagged_for_review"
        const val APPROVED    = "approved"
        const val PUBLISHED   = "published"
        const val ARCHIVED    = "archived"
    }

    /** Movement pattern buckets from Tier 1 triage. */
    object MovementPattern {
        const val IMPROVED  = "improved"
        const val STEADY    = "steady"
        const val SLID      = "slid"
        const val VOLATILE  = "volatile"
    }

    /** Data confidence levels. */
    object Confidence {
        const val HIGH      = "high"
        const val MEDIUM    = "medium"
        const val LOW       = "low"
        const val INSUFFICIENT = "insufficient"
    }
}

/**
 * Kill-switch guard for AI Report Card 2.0 modules.
 *
 * Checks both the per-module flag AND the global "reportcard" flag.
 * If either is killed, throws [PewsDisabledException].
 *
 * Usage:
 *   ReportCardKillSwitch.require(ReportCardConstants.MODULE_ROLLUP)
 */
object ReportCardKillSwitch {
    fun require(moduleName: String) {
        if (com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig.isKilled(
                ReportCardConstants.MODULE_GLOBAL
            ) || com.littlebridge.enrollplus.feature.pews.core.KillSwitchConfig.isKilled(moduleName)
        ) {
            throw com.littlebridge.enrollplus.feature.pews.core.PewsDisabledException(moduleName)
        }
    }
}
