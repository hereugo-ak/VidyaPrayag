/*
 * File: RateLimiter.kt
 * Module: feature.ai
 *
 * Per-(provider, model) proactive rate limiter — prevents 429s BEFORE they
 * happen by tracking RPM, RPD, and TPM against each provider's free-tier
 * limits at 90% capacity (10% held as reserve for retries and burst traffic).
 *
 * Three independent limiters per provider:
 *
 *   RPM  — sliding 60-second window of request timestamps. When the window
 *          has >= effectiveRpm entries, the provider is "throttled" (skip to
 *          next candidate; the caller's circuit breaker stays CLOSED since
 *          this is proactive, not a failure).
 *
 *   RPD  — daily counter reset at UTC midnight. When the counter reaches
 *          effectiveRpd, the provider is "exhausted" for the rest of the day.
 *
 *   TPM  — sliding 60-second window of estimated token counts. When the
 *          window sum + estimated request tokens >= effectiveTpm, the provider
 *          is "token-throttled". Token estimates use maxTokens as an upper
 *          bound (we don't know actual tokens until the response arrives).
 *
 * All limits default to the provider's free-tier caps × 0.90 (10% reserve).
 * Env-tunable: AI_RATE_RESERVE_PCT (default 10), AI_RATE_RPM_OVERRIDE_<PROVIDER>,
 * AI_RATE_RPD_OVERRIDE_<PROVIDER>, AI_RATE_TPM_OVERRIDE_<PROVIDER>.
 *
 * Thread-safe via ConcurrentHashMap + synchronized sliding-window cleanup.
 * No DB persistence — limits reset on restart, which is acceptable since
 * free-tier limits are per-minute/day and a restart is rare during peak load.
 *
 * SOLID:
 *   S → Single responsibility: only decides "can this provider accept a call
 *       right now?" — does not call providers, does not route, does not cache.
 *   D → Depends on AiProvider enum for limit metadata; no other dependencies.
 */
package com.littlebridge.enrollplus.feature.ai

import com.littlebridge.enrollplus.core.EnvConfig
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object RateLimiter {
    private val log = LoggerFactory.getLogger("AiRateLimiter")

    // ── Config ──────────────────────────────────────────────────────────────

    /** Percentage of each limit to hold as reserve (default 10%). */
    private val reservePct: Int
        get() = EnvConfig.get("AI_RATE_RESERVE_PCT")?.toIntOrNull()?.coerceIn(0, 50) ?: 10

    private fun effectiveRpm(raw: Int): Int = (raw * (100 - reservePct) / 100).coerceAtLeast(1)
    private fun effectiveRpd(raw: Int): Int = (raw * (100 - reservePct) / 100).coerceAtLeast(1)
    private fun effectiveTpm(raw: Int): Int = (raw * (100 - reservePct) / 100).coerceAtLeast(1)

    /** Per-provider raw free-tier limits (from AiProvider enum / env overrides). */
    private fun rawRpm(provider: AiProvider): Int =
        EnvConfig.get("AI_RATE_RPM_OVERRIDE_${provider.name}")?.toIntOrNull()
            ?: provider.freeTierRpm
    private fun rawRpd(provider: AiProvider): Int =
        EnvConfig.get("AI_RATE_RPD_OVERRIDE_${provider.name}")?.toIntOrNull()
            ?: provider.freeTierRpd
    private fun rawTpm(provider: AiProvider): Int =
        EnvConfig.get("AI_RATE_TPM_OVERRIDE_${provider.name}")?.toIntOrNull()
            ?: provider.freeTierTpm

    // ── Per-provider state ──────────────────────────────────────────────────

    private data class ProviderState(
        /** Sliding window of request timestamps (epoch millis) for RPM. */
        val rpmWindow: MutableList<Long> = mutableListOf(),
        /** Sliding window of (timestamp, tokenEstimate) for TPM. */
        val tpmWindow: MutableList<TokenEntry> = mutableListOf(),
        /** Daily counter (resets at UTC midnight). */
        val rpdCounter: AtomicLong = AtomicLong(0),
        /** Date (UTC) of the current daily counter — for midnight reset. */
        var rpdDate: String = todayUtc(),
    )

    private data class TokenEntry(val timestamp: Long, val tokens: Int)

    private val states = ConcurrentHashMap<String, ProviderState>()
    private val keyLocks = ConcurrentHashMap<String, Any>()

    private fun key(provider: String, model: String) = "$provider::$model"
    private fun lockFor(k: String) = keyLocks.computeIfAbsent(k) { Any() }

    private fun todayUtc(): String =
        Instant.now().atZone(ZoneOffset.UTC).toLocalDate().toString()

    // ── Public API ──────────────────────────────────────────────────────────

    /**
     * Result of a rate-limit check.
     *
     * @param allowed       true if the call may proceed to the provider
     * @param reason        human-readable reason if not allowed
     * @param limitType     which limit was hit (rpm|rpd|tpm|null)
     * @param retryAfterMs  suggested delay before retrying this provider
     */
    data class CheckResult(
        val allowed: Boolean,
        val reason: String? = null,
        val limitType: String? = null,
        val retryAfterMs: Long? = null,
    )

    /**
     * Check whether (provider, model) can accept a call right now, given an
     * estimated token cost. Does NOT record the call — call [record] after the
     * LLM response arrives (or fails) to update the windows.
     *
     * @param provider       provider code
     * @param model          model name
     * @param estTokens      estimated total tokens for this call (input + max output)
     * @param aiProvider     the AiProvider enum (for limit lookup)
     */
    fun check(
        provider: String,
        model: String,
        estTokens: Int,
        aiProvider: AiProvider,
    ): CheckResult {
        val k = key(provider, model)
        val state = states.computeIfAbsent(k) { ProviderState() }

        // RPD check (daily counter)
        synchronized(lockFor(k)) {
            // Midnight reset
            val today = todayUtc()
            if (state.rpdDate != today) {
                state.rpdCounter.set(0)
                state.rpdDate = today
                log.debug("RateLimiter: RPD counter reset for {} (new UTC day)", k)
            }

            val rpdLimit = effectiveRpd(rawRpd(aiProvider))
            val rpdCurrent = state.rpdCounter.get()
            if (rpdCurrent >= rpdLimit) {
                log.debug("RateLimiter: {} exhausted RPD ({}/{})", k, rpdCurrent, rpdLimit)
                return CheckResult(
                    allowed = false,
                    reason = "daily request limit reached ($rpdCurrent/$rpdLimit)",
                    limitType = "rpd",
                    retryAfterMs = msUntilUtcMidnight(),
                )
            }
        }

        // RPM + TPM check (sliding windows)
        val now = System.currentTimeMillis()
        val windowStart = now - 60_000L

        synchronized(lockFor(k)) {
            // Clean expired entries from both windows
            state.rpmWindow.removeAll { it < windowStart }
            state.tpmWindow.removeAll { it.timestamp < windowStart }

            // RPM check
            val rpmLimit = effectiveRpm(rawRpm(aiProvider))
            if (state.rpmWindow.size >= rpmLimit) {
                val oldestInWindow = state.rpmWindow.minOrNull() ?: now
                val retryMs = (oldestInWindow + 60_000L) - now
                log.debug("RateLimiter: {} throttled RPM ({}/{}, retry in {}ms)",
                    k, state.rpmWindow.size, rpmLimit, retryMs)
                return CheckResult(
                    allowed = false,
                    reason = "per-minute request limit reached (${state.rpmWindow.size}/$rpmLimit)",
                    limitType = "rpm",
                    retryAfterMs = retryMs.coerceAtLeast(1000L),
                )
            }

            // TPM check
            val tpmLimit = effectiveTpm(rawTpm(aiProvider))
            val tpmCurrent = state.tpmWindow.sumOf { it.tokens }
            if (tpmCurrent + estTokens > tpmLimit) {
                val oldestToken = state.tpmWindow.minOfOrNull { it.timestamp } ?: now
                val retryMs = (oldestToken + 60_000L) - now
                log.debug("RateLimiter: {} throttled TPM ({}/{} + {} est, retry in {}ms)",
                    k, tpmCurrent, tpmLimit, estTokens, retryMs)
                return CheckResult(
                    allowed = false,
                    reason = "per-minute token limit reached ($tpmCurrent+$estTokens/$tpmLimit)",
                    limitType = "tpm",
                    retryAfterMs = retryMs.coerceAtLeast(1000L),
                )
            }
        }

        return CheckResult(allowed = true)
    }

    /**
     * Record a completed (or failed) call against the rate limiter windows.
     * Call this AFTER the LLM response arrives to update RPM/TPM/RPD counters
     * with actual token usage.
     *
     * @param provider       provider code
     * @param model          model name
     * @param actualTokens   actual total tokens (input + output) from the response
     * @param aiProvider     the AiProvider enum (for limit lookup)
     */
    fun record(
        provider: String,
        model: String,
        actualTokens: Int,
        aiProvider: AiProvider,
    ) {
        val k = key(provider, model)
        val state = states.computeIfAbsent(k) { ProviderState() }
        val now = System.currentTimeMillis()

        synchronized(lockFor(k)) {
            // RPM window
            state.rpmWindow.add(now)
            // TPM window
            state.tpmWindow.add(TokenEntry(now, actualTokens))
            // RPD counter
            val today = todayUtc()
            if (state.rpdDate != today) {
                state.rpdCounter.set(0)
                state.rpdDate = today
            }
            state.rpdCounter.incrementAndGet()
        }
    }

    /**
     * Snapshot for the admin AI-health screen — shows current usage vs limits
     * per provider so admins can see which providers are near exhaustion.
     */
    data class UsageSnapshot(
        val provider: String,
        val model: String,
        val rpmCurrent: Int,
        val rpmLimit: Int,
        val rpdCurrent: Long,
        val rpdLimit: Int,
        val tpmCurrent: Int,
        val tpmLimit: Int,
        val reservePct: Int,
    )

    fun snapshot(): List<UsageSnapshot> {
        val now = System.currentTimeMillis()
        val windowStart = now - 60_000L

        return AiProvider.entries.map { provider ->
            val model = provider.defaultModel
            val k = key(provider.code, model)
            val state = states[k]

            if (state != null) {
                synchronized(lockFor(k)) {
                    state.rpmWindow.removeAll { it < windowStart }
                    state.tpmWindow.removeAll { it.timestamp < windowStart }
                }
                UsageSnapshot(
                    provider = provider.code,
                    model = model,
                    rpmCurrent = state.rpmWindow.size,
                    rpmLimit = effectiveRpm(rawRpm(provider)),
                    rpdCurrent = state.rpdCounter.get(),
                    rpdLimit = effectiveRpd(rawRpd(provider)),
                    tpmCurrent = state.tpmWindow.sumOf { it.tokens },
                    tpmLimit = effectiveTpm(rawTpm(provider)),
                    reservePct = reservePct,
                )
            } else {
                UsageSnapshot(
                    provider = provider.code,
                    model = model,
                    rpmCurrent = 0,
                    rpmLimit = effectiveRpm(rawRpm(provider)),
                    rpdCurrent = 0,
                    rpdLimit = effectiveRpd(rawRpd(provider)),
                    tpmCurrent = 0,
                    tpmLimit = effectiveTpm(rawTpm(provider)),
                    reservePct = reservePct,
                )
            }
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun msUntilUtcMidnight(): Long {
        val now = Instant.now()
        val midnight = now.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
        return ChronoUnit.MILLIS.between(now, midnight)
    }
}
