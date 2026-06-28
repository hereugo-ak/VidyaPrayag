/*
 * File: AiService.kt
 * Module: feature.ai
 *
 * THE single choke point for every LLM call in the product. No feature ever
 * talks to LlmClient or a provider directly — they call `AiService.complete(...)`
 * and the gateway does all the hard parts in one place:
 *
 *   request
 *     │
 *     ├─ 1. build cache key (sha256 of model-lane + temp + messages, school-scoped)
 *     ├─ 2. L1 cache hit?  → return cached (routing_decision=cache_l1_hit), log usage
 *     ├─ 3. resolve lane → ordered candidate providers (dual-homed)
 *     ├─ 4. PII guardrail → drop training-opt-in providers for PII prompts
 *     ├─ 5. for each candidate (priority order):
 *     │        circuit OPEN?  → skip
 *     │        key configured? → no → skip (graceful)
 *     │        call LlmClient → success?  → cache + log + recordSuccess → RETURN
 *     │                          failure?  → recordFailure → next candidate
 *     └─ 6. all exhausted → AiResult.unavailable (feature degrades gracefully)
 *
 * Every outcome writes exactly one ai_usage_log row (success | cached | failed |
 * guardrail_blocked) with provider_used + routing_decision, so the admin
 * AI-usage screen and per-school quota are sourced from one append-only table.
 *
 * Module-level singleton — no Koin (matches the rest of the server).
 */
package com.littlebridge.enrollplus.feature.ai

import com.littlebridge.enrollplus.db.AiResponseCacheTable
import com.littlebridge.enrollplus.db.AiUsageLogTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Properties
import java.util.UUID

/** The capability lanes a caller can request (provider-agnostic). */
enum class AiLane { FAST_CHAT, CLASSIFY, REASON, BATCH }

/** The outcome the calling feature sees — never throws, always degradable. */
data class AiResult(
    val ok: Boolean,
    val content: String? = null,
    val providerUsed: String? = null,
    val modelUsed: String? = null,
    val inputTokens: Int = 0,
    val outputTokens: Int = 0,
    val routingDecision: String = "direct",   // direct|cache_l1_hit|failed_over|unavailable
    val errorMessage: String? = null,
) {
    companion object {
        fun unavailable(reason: String) =
            AiResult(ok = false, routingDecision = "unavailable", errorMessage = reason)
    }
}

object AiService {
    private val log = LoggerFactory.getLogger("AiService")
    private val llm = LlmClient()

    // ── env tuning ──────────────────────────────────────────────────────────
    private val localProps: Properties by lazy {
        Properties().apply {
            runCatching {
                val f = File("local.properties")
                if (f.exists()) f.inputStream().use { load(it) }
            }
        }
    }
    private fun env(key: String): String? =
        (System.getenv(key) ?: localProps.getProperty(key))?.takeIf { it.isNotBlank() }

    private val defaultCacheTtlMin: Long get() = env("AI_DEFAULT_CACHE_TTL_MIN")?.toLongOrNull() ?: 1440L

    /**
     * Lane → ordered candidate providers (dual-homed). Matches the plan §4.3.
     * PII filtering (GuardrailService) is applied on top of this at call time,
     * so for a PII prompt the REASON lane collapses to the no-training subset
     * (Cerebras → Groq → OpenRouter).
     */
    private fun laneProviders(lane: AiLane): List<AiProvider> = when (lane) {
        AiLane.FAST_CHAT -> listOf(AiProvider.CEREBRAS, AiProvider.GROQ, AiProvider.OPENROUTER)
        AiLane.CLASSIFY  -> listOf(AiProvider.GROQ, AiProvider.CEREBRAS, AiProvider.OPENROUTER)
        AiLane.REASON    -> listOf(AiProvider.SAMBANOVA, AiProvider.MISTRAL, AiProvider.OPENROUTER,
                                   AiProvider.GROQ, AiProvider.CEREBRAS)
        AiLane.BATCH     -> listOf(AiProvider.MISTRAL, AiProvider.GROQ, AiProvider.CEREBRAS)
    }

    /**
     * Run a completion through the gateway.
     *
     * @param feature       short feature tag for usage logging (e.g. "pews")
     * @param lane          capability lane
     * @param messages      OpenAI-shaped messages (system + user)
     * @param containsPii   true ⇒ restrict to no-training providers
     * @param schoolId      tenant scope for cache + usage log (null = platform)
     * @param userId        actor for usage log (optional)
     * @param temperature   sampling temperature
     * @param maxTokens     output cap
     * @param cache         whether to read/write the L1 cache
     * @param cacheTtlMin   cache TTL (defaults to AI_DEFAULT_CACHE_TTL_MIN)
     * @param piiAllowList  optional provider allow-list (from a prompt template)
     */
    suspend fun complete(
        feature: String,
        lane: AiLane,
        messages: List<LlmMessage>,
        containsPii: Boolean = false,
        schoolId: UUID? = null,
        userId: UUID? = null,
        temperature: Double = 0.4,
        maxTokens: Int = 1024,
        cache: Boolean = true,
        cacheTtlMin: Long = defaultCacheTtlMin,
        piiAllowList: Set<String> = emptySet(),
    ): AiResult {
        val startedAll = System.currentTimeMillis()

        // Defensive: if the caller said non-PII but the text clearly carries PII,
        // upgrade to the safe lane (never leak to a training-opt-in provider).
        val effectivePii = containsPii ||
            messages.any { GuardrailService.looksLikePii(it.content) }

        // 1) candidate lane
        var candidates = laneProviders(lane)
        // 2) PII guardrail
        candidates = GuardrailService.filterProvidersForPii(candidates, effectivePii, piiAllowList)
        if (candidates.isEmpty()) {
            logUsage(feature, schoolId, userId, lane, null, null, 0, 0, 0.0,
                System.currentTimeMillis() - startedAll, "guardrail_blocked", "unavailable",
                "No PII-safe provider available")
            return AiResult.unavailable("No PII-safe provider configured for this prompt")
        }

        // 3) cache key (lane + temp + messages; school-scoped)
        val cacheKey = buildCacheKey(lane, temperature, maxTokens, messages, schoolId)

        // 4) L1 cache
        if (cache) {
            readCache(cacheKey)?.let { hit ->
                logUsage(feature, schoolId, userId, lane, hit.providerUsed, hit.modelUsed,
                    hit.inputTokens, hit.outputTokens, 0.0,
                    System.currentTimeMillis() - startedAll, "cached", "cache_l1_hit", null)
                return AiResult(
                    ok = true, content = hit.response, providerUsed = hit.providerUsed,
                    modelUsed = hit.modelUsed, inputTokens = hit.inputTokens,
                    outputTokens = hit.outputTokens, routingDecision = "cache_l1_hit"
                )
            }
        }

        // 5) try candidates in order
        var failedOver = false
        var lastError: String? = null
        for ((idx, provider) in candidates.withIndex()) {
            val model = KeyVault.modelFor(provider)

            if (!CircuitBreaker.allow(provider.code, model)) {
                log.debug("Skipping {} ({}): circuit open", provider.code, model)
                failedOver = true
                continue
            }
            val apiKey = KeyVault.keyFor(provider)
            if (apiKey == null) {
                log.debug("Skipping {}: no key configured", provider.code)
                failedOver = true
                continue
            }

            val baseUrl = KeyVault.baseUrlFor(provider)
            val extraHeaders = if (provider == AiProvider.OPENROUTER) {
                mapOf(
                    "HTTP-Referer" to "https://vidyaprayag.app",
                    "X-Title" to "VidyaPrayag",
                )
            } else emptyMap()

            val started = System.currentTimeMillis()
            val result = llm.complete(
                baseUrl = baseUrl, apiKey = apiKey, model = model, messages = messages,
                temperature = temperature, maxTokens = maxTokens, extraHeaders = extraHeaders
            )
            val latency = System.currentTimeMillis() - started

            if (result.ok && !result.content.isNullOrBlank()) {
                val validated = GuardrailService.validateResponse(result.content)
                if (validated == null) {
                    // empty/invalid post-validation → treat as failure, try next
                    CircuitBreaker.recordFailure(provider.code, model, rateLimited = false)
                    lastError = "empty_after_validation"
                    failedOver = true
                    continue
                }
                CircuitBreaker.recordSuccess(provider.code, model, latency)
                val routing = if (idx == 0 && !failedOver) "direct" else "failed_over"
                if (cache) {
                    writeCache(cacheKey, schoolId, feature, validated, result, provider.code,
                        result.modelUsed ?: model, cacheTtlMin)
                }
                logUsage(feature, schoolId, userId, lane, provider.code, result.modelUsed ?: model,
                    result.inputTokens, result.outputTokens, 0.0, latency, "success", routing, null)
                return AiResult(
                    ok = true, content = validated, providerUsed = provider.code,
                    modelUsed = result.modelUsed ?: model, inputTokens = result.inputTokens,
                    outputTokens = result.outputTokens, routingDecision = routing
                )
            } else {
                val rl = result.errorKind == LlmErrorKind.RATE_LIMITED
                CircuitBreaker.recordFailure(provider.code, model, rateLimited = rl)
                lastError = result.errorMessage ?: result.errorKind?.name
                failedOver = true
                log.debug("Provider {} failed ({}); trying next", provider.code, result.errorKind)
            }
        }

        // 6) all exhausted → graceful unavailable
        logUsage(feature, schoolId, userId, lane, null, null, 0, 0, 0.0,
            System.currentTimeMillis() - startedAll, "failed", "unavailable", lastError)
        return AiResult.unavailable(lastError ?: "All providers unavailable")
    }

    // ── cache ─────────────────────────────────────────────────────────────

    private data class CacheHit(
        val response: String, val inputTokens: Int, val outputTokens: Int,
        val providerUsed: String?, val modelUsed: String?,
    )

    private fun buildCacheKey(
        lane: AiLane, temperature: Double, maxTokens: Int,
        messages: List<LlmMessage>, schoolId: UUID?,
    ): String {
        val raw = buildString {
            append(schoolId?.toString() ?: "platform"); append('|')
            append(lane.name); append('|'); append(temperature); append('|'); append(maxTokens); append('|')
            messages.forEach { append(it.role).append(':').append(it.content).append('\n') }
        }
        return sha256(raw)
    }

    private suspend fun readCache(cacheKey: String): CacheHit? = dbQuery {
        val row = AiResponseCacheTable.selectAll().where {
            AiResponseCacheTable.cacheKey eq cacheKey
        }.singleOrNull() ?: return@dbQuery null
        val expires = row[AiResponseCacheTable.expiresAt]
        if (Instant.now().isAfter(expires)) {
            AiResponseCacheTable.deleteWhere { AiResponseCacheTable.cacheKey eq cacheKey }
            return@dbQuery null
        }
        CacheHit(
            response = row[AiResponseCacheTable.response],
            inputTokens = row[AiResponseCacheTable.inputTokens],
            outputTokens = row[AiResponseCacheTable.outputTokens],
            providerUsed = row[AiResponseCacheTable.providerUsed],
            modelUsed = row[AiResponseCacheTable.modelUsed],
        )
    }

    private suspend fun writeCache(
        cacheKey: String, schoolId: UUID?, feature: String, response: String,
        result: LlmResult, provider: String, model: String, ttlMin: Long,
    ) {
        runCatching {
            dbQuery {
                AiResponseCacheTable.deleteWhere { AiResponseCacheTable.cacheKey eq cacheKey }
                AiResponseCacheTable.insert {
                    it[AiResponseCacheTable.cacheKey] = cacheKey
                    it[AiResponseCacheTable.schoolId] = schoolId
                    it[AiResponseCacheTable.feature] = feature
                    it[AiResponseCacheTable.response] = response
                    it[inputTokens] = result.inputTokens
                    it[outputTokens] = result.outputTokens
                    it[providerUsed] = provider
                    it[modelUsed] = model
                    it[expiresAt] = Instant.now().plus(ttlMin, ChronoUnit.MINUTES)
                    it[createdAt] = Instant.now()
                }
            }
        }.onFailure { log.debug("AI cache write skipped: {}", it.message) }
    }

    // ── usage log (append-only; best-effort, never blocks the caller) ───────

    private suspend fun logUsage(
        feature: String, schoolId: UUID?, userId: UUID?, lane: AiLane,
        providerUsed: String?, modelUsed: String?, inTok: Int, outTok: Int,
        costUsd: Double, latencyMs: Long, status: String, routing: String,
        errorMessage: String?,
    ) {
        runCatching {
            dbQuery {
                AiUsageLogTable.insert {
                    it[AiUsageLogTable.schoolId] = schoolId
                    it[AiUsageLogTable.userId] = userId
                    it[AiUsageLogTable.feature] = feature
                    it[provider] = lane.name.lowercase()        // requested lane
                    it[model] = modelUsed ?: ""
                    it[AiUsageLogTable.providerUsed] = providerUsed
                    it[AiUsageLogTable.modelUsed] = modelUsed
                    it[inputTokens] = inTok
                    it[outputTokens] = outTok
                    it[AiUsageLogTable.costUsd] = costUsd
                    it[AiUsageLogTable.latencyMs] = latencyMs.toInt()
                    it[AiUsageLogTable.status] = status
                    it[routingDecision] = routing
                    it[AiUsageLogTable.errorMessage] = errorMessage
                    it[createdAt] = Instant.now()
                }
            }
        }.onFailure { log.debug("AI usage log skipped: {}", it.message) }
    }

    private fun sha256(s: String): String =
        MessageDigest.getInstance("SHA-256").digest(s.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }

    /** True if at least one provider in any lane has a key path. For health. */
    fun anyProviderConfigured(): Boolean =
        AiProvider.entries.any { KeyVault.isConfigured(it) }
}
