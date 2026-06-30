// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/core/ReportCardConfig.kt
package com.littlebridge.enrollplus.feature.reportcard.core

import com.littlebridge.enrollplus.core.EnvConfig

/**
 * Centralized configuration for the AI Report Card 2.0 feature.
 *
 * All values are sourced from environment variables (via [EnvConfig]) with
 * sensible defaults. No hardcoded values anywhere in the feature — every
 * service reads from this object.
 *
 * Supported env vars:
 *   REPORTCARD_ENABLED           — master toggle ("true"/"false", default "true")
 *   AI_BATCH_CONCURRENCY         — max parallel students in batch job (default 5)
 *   NARRATOR_MAX_STEPS           — max agent tool-call steps (default 6)
 *   NARRATOR_TEMPERATURE         — LLM sampling temperature (default 0.3)
 *   NARRATOR_MAX_TOKENS          — max output tokens per student (default 2048)
 *   TRIAGE_CLASSIFY_MODEL        — optional model override for Tier-1 classify
 *   CACHE_TTL_MINUTES            — response cache TTL in minutes (default 1440)
 *   REPORTCARD_FALLBACK_ON_AI_FAIL — use deterministic fallback when AI fails (default "true")
 *
 * SOLID: S (single responsibility: configuration only).
 */
object ReportCardConfig {

    // ── Runtime overrides (set via PUT /report-card/term-config) ────────
    @Volatile private var currentTermOverride: String? = null
    @Volatile private var termWindowDaysOverride: Int? = null
    @Volatile private var enabledOverride: Boolean? = null
    @Volatile private var fallbackOverride: Boolean? = null

    fun updateConfig(
        currentTerm: String? = null,
        termWindowDays: Int? = null,
        enabled: Boolean? = null,
        fallbackOnAiFail: Boolean? = null,
    ) {
        currentTermOverride = currentTerm
        termWindowDaysOverride = termWindowDays
        enabledOverride = enabled
        fallbackOverride = fallbackOnAiFail
    }

    val enabled: Boolean
        get() = enabledOverride ?: EnvConfig.get("REPORTCARD_ENABLED", "true").equals("true", ignoreCase = true)

    val batchConcurrency: Int
        get() = EnvConfig.get("AI_BATCH_CONCURRENCY")?.toIntOrNull() ?: 5

    val narratorMaxSteps: Int
        get() = EnvConfig.get("NARRATOR_MAX_STEPS")?.toIntOrNull() ?: 6

    val narratorTemperature: Double
        get() = EnvConfig.get("NARRATOR_TEMPERATURE")?.toDoubleOrNull() ?: 0.3

    val narratorMaxTokens: Int
        get() = EnvConfig.get("NARRATOR_MAX_TOKENS")?.toIntOrNull() ?: 2048

    val triageClassifyModel: String?
        get() = EnvConfig.get("TRIAGE_CLASSIFY_MODEL")

    val cacheTtlMinutes: Long
        get() = EnvConfig.get("CACHE_TTL_MINUTES")?.toLongOrNull() ?: 1440L

    val fallbackOnAiFail: Boolean
        get() = fallbackOverride ?: EnvConfig.get("REPORTCARD_FALLBACK_ON_AI_FAIL", "true").equals("true", ignoreCase = true)

    /** Current term label for scheduled auto-generation (e.g. "Term 1"). Null disables auto-trigger. */
    val currentTerm: String?
        get() = currentTermOverride ?: EnvConfig.get("REPORTCARD_CURRENT_TERM")

    /** Days before term end to start auto-generation (default 7). */
    val termWindowDays: Int
        get() = termWindowDaysOverride ?: (EnvConfig.get("REPORTCARD_TERM_WINDOW_DAYS")?.toIntOrNull() ?: 7)

    /** Retry backoff delays in milliseconds for AI provider failures. */
    val retryBackoffMs: List<Long>
        get() = listOf(1000L, 2000L, 4000L)
}
