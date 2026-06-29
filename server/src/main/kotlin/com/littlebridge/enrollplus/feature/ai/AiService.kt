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
import java.security.MessageDigest
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.random.Random

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
    val toolCalls: List<ToolCall>? = null,
    val finishReason: String? = null,
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
    // Delegate to the shared .env-aware resolver so AI tuning vars are read from
    // the same sources as the provider keys (.env → env → local.properties).
    private fun env(key: String): String? = com.littlebridge.enrollplus.core.EnvConfig.get(key)

    private val defaultCacheTtlMin: Long get() = env("AI_DEFAULT_CACHE_TTL_MIN")?.toLongOrNull() ?: 1440L

    /**
     * Lane → ordered candidate providers (dual-homed). Matches the plan §4.3.
     * Ordering updated June 2026 based on actual free-tier rate limits:
     *   - Groq has the highest free throughput (~14,400 RPD, 30 RPM on 70B)
     *   - Gemini Flash: 1,500 RPD, 15 RPM, 1M TPM (noTraining=false → non-PII only)
     *   - SambaNova free tier is only 20 RPD — demoted from primary to secondary
     *   - GROQ_FAST (8B model) used for FAST_CHAT: ~14,400 RPM, 500K TPM
     *   - Cerebras free tier: 5 RPM, 1M TPD — OK as secondary fast provider
     *   - OpenRouter: 50 RPD free (1,000 with $10 credit) — last resort
     *   - Mistral: ~1 RPS, ~1B TPM — good for BATCH, PII-restricted
     *
     * PII filtering (GuardrailService) is applied on top of this at call time,
     * so for a PII prompt the REASON lane collapses to the no-training subset
     * (Groq → Cerebras → OpenRouter).
     */
    private fun laneProviders(lane: AiLane): List<AiProvider> = when (lane) {
        AiLane.FAST_CHAT -> listOf(AiProvider.GROQ_FAST, AiProvider.CEREBRAS, AiProvider.GROQ, AiProvider.OPENROUTER)
        AiLane.CLASSIFY  -> listOf(AiProvider.GROQ_FAST, AiProvider.GROQ, AiProvider.CEREBRAS, AiProvider.OPENROUTER)
        AiLane.REASON    -> listOf(AiProvider.GROQ, AiProvider.GEMINI, AiProvider.SAMBANOVA, AiProvider.CEREBRAS,
                                   AiProvider.OPENROUTER, AiProvider.MISTRAL)
        AiLane.BATCH     -> listOf(AiProvider.GROQ, AiProvider.GEMINI, AiProvider.MISTRAL, AiProvider.OPENROUTER)
    }

    /**
     * Jitter the candidate list: shuffle providers whose circuits are CLOSED
     * to spread load across providers, instead of always hammering the first
     * one. Providers with OPEN circuits stay in their original position (they
     * get skipped by the caller loop anyway). This is the "diversification"
     * fix for the 429 problem — prevents all concurrent requests from hitting
     * the same free model simultaneously.
     */
    private suspend fun jitterCandidates(candidates: List<AiProvider>): List<AiProvider> {
        val (closed, open) = candidates.partition { provider ->
            val model = KeyVault.modelFor(provider)
            CircuitBreaker.stateOf(provider.code, model) == CircuitBreaker.State.CLOSED
        }
        if (closed.size <= 1) return candidates
        return closed.shuffled(Random) + open
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
     * @param tools         optional tool definitions for function calling
     * @param toolChoice    "auto" | "none" | "required" | null
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
        tools: List<ToolDefinition>? = null,
        toolChoice: String? = null,
    ): AiResult {
        val startedAll = System.currentTimeMillis()

        // Defensive: if the caller said non-PII but the text clearly carries PII,
        // upgrade to the safe lane (never leak to a training-opt-in provider).
        val effectivePii = containsPii ||
            messages.any { it.content != null && GuardrailService.looksLikePii(it.content) }

        // 1) candidate lane
        var candidates = laneProviders(lane)
        // 1b) jitter: shuffle candidates that have closed circuits to spread
        // load across providers instead of always hammering the first one.
        // Keep the priority order for OPEN-circuit providers (they're skipped anyway).
        candidates = jitterCandidates(candidates)
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
                temperature = temperature, maxTokens = maxTokens, extraHeaders = extraHeaders,
                tools = tools, toolChoice = toolChoice,
            )
            val latency = System.currentTimeMillis() - started

            // Tool-call response: return as success with toolCalls in the result
            if (result.ok && !result.toolCalls.isNullOrEmpty()) {
                CircuitBreaker.recordSuccess(provider.code, model, latency)
                val routing = if (idx == 0 && !failedOver) "direct" else "failed_over"
                logUsage(feature, schoolId, userId, lane, provider.code, result.modelUsed ?: model,
                    result.inputTokens, result.outputTokens, 0.0, latency, "success", routing, null)
                return AiResult(
                    ok = true, content = result.content, providerUsed = provider.code,
                    modelUsed = result.modelUsed ?: model, inputTokens = result.inputTokens,
                    outputTokens = result.outputTokens, routingDecision = routing,
                    toolCalls = result.toolCalls, finishReason = result.finishReason,
                )
            }

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
                // Jittered backoff on rate-limit: 200-800ms random delay to
                // avoid thundering herd on the next provider.
                if (rl) {
                    val backoffMs = 200L + Random.nextLong(600L)
                    kotlinx.coroutines.delay(backoffMs)
                }
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

    // ── Agent loop (tool-calling) ─────────────────────────────────────────

    /**
     * A tool the agent can call. Implementations MUST be read-only and
     * tenant-scoped — the [schoolId] is injected from the caller's context,
     * never from the model's output. Tools can never write; all writes stay
     * in deterministic Tier-3.
     */
    interface AgentTool {
        val name: String
        val description: String
        /** JSON Schema for the tool's parameters (OpenAI function-calling shape). */
        val parametersSchema: kotlinx.serialization.json.JsonElement

        /** Execute the tool with the raw JSON arguments string. Returns tool result text. */
        suspend fun execute(schoolId: UUID, arguments: String): String
    }

    /**
     * Result of an agent run — the final structured JSON the model produced
     * after exhausting its tool calls, or an unavailable result if the loop
     * failed or hit the step cap.
     */
    data class AgentResult(
        val ok: Boolean,
        val content: String? = null,
        val providerUsed: String? = null,
        val modelUsed: String? = null,
        val totalInputTokens: Int = 0,
        val totalOutputTokens: Int = 0,
        val stepsTaken: Int = 0,
        val toolCallsMade: Int = 0,
        val errorMessage: String? = null,
    ) {
        companion object {
            fun unavailable(reason: String) =
                AgentResult(ok = false, errorMessage = reason)
        }
    }

    /**
     * Run a tool-using agent loop:
     * 1. Send messages + tool schemas to the model.
     * 2. If the model returns tool_calls, execute each whitelisted tool,
     *    append the results, and loop (max [maxSteps], hard-capped).
     * 3. Return the final structured JSON content.
     *
     * Safety: tools are read-only and tenant-scoped. The model can ask for
     * data; it can never write. All writes stay in deterministic Tier-3.
     *
     * @param feature       feature tag for usage logging
     * @param lane          capability lane (typically REASON)
     * @param systemPrompt  the agent's system prompt
     * @param userPrompt    the initial user prompt
     * @param tools         available tools (name → implementation)
     * @param schoolId      tenant scope (injected into every tool call)
     * @param containsPii   whether the prompt carries PII
     * @param maxSteps      hard cap on tool-call rounds (default 6)
     * @param temperature   sampling temperature
     * @param maxTokens     output cap per call
     */
    suspend fun runAgent(
        feature: String,
        lane: AiLane,
        systemPrompt: String,
        userPrompt: String,
        tools: Map<String, AgentTool>,
        schoolId: UUID,
        containsPii: Boolean = true,
        maxSteps: Int = 6,
        temperature: Double = 0.4,
        maxTokens: Int = 2048,
    ): AgentResult {
        if (tools.isEmpty()) {
            // No tools → just a plain completion
            val result = complete(
                feature = feature, lane = lane,
                messages = listOf(
                    LlmMessage("system", systemPrompt),
                    LlmMessage("user", userPrompt),
                ),
                containsPii = containsPii, schoolId = schoolId,
                temperature = temperature, maxTokens = maxTokens,
            )
            return if (result.ok) {
                AgentResult(
                    ok = true, content = result.content,
                    providerUsed = result.providerUsed, modelUsed = result.modelUsed,
                    totalInputTokens = result.inputTokens,
                    totalOutputTokens = result.outputTokens,
                )
            } else {
                AgentResult.unavailable(result.errorMessage ?: "unavailable")
            }
        }

        val toolDefs = tools.values.map { tool ->
            ToolDefinition(
                function = ToolFunction(
                    name = tool.name,
                    description = tool.description,
                    parameters = tool.parametersSchema,
                )
            )
        }

        val conversation = mutableListOf<LlmMessage>(
            LlmMessage("system", systemPrompt),
            LlmMessage("user", userPrompt),
        )

        var totalIn = 0
        var totalOut = 0
        var toolCallsMade = 0
        var lastProvider: String? = null
        var lastModel: String? = null

        for (step in 1..maxSteps) {
            val result = complete(
                feature = feature, lane = lane,
                messages = conversation.toList(),
                containsPii = containsPii, schoolId = schoolId,
                temperature = temperature, maxTokens = maxTokens,
                cache = false,  // agent conversations are unique per run
                tools = toolDefs,
                toolChoice = if (step < maxSteps) "auto" else "none",
            )

            if (!result.ok) {
                log.warn("Agent loop failed at step {}: {}", step, result.errorMessage)
                return AgentResult.unavailable(result.errorMessage ?: "provider unavailable")
            }

            totalIn += result.inputTokens
            totalOut += result.outputTokens
            lastProvider = result.providerUsed
            lastModel = result.modelUsed

            val toolCalls = result.toolCalls
            if (toolCalls.isNullOrEmpty()) {
                // No tool calls → final response
                log.debug("Agent loop completed at step {} ({} tool calls, {}+{} tokens)",
                    step, toolCallsMade, totalIn, totalOut)
                return AgentResult(
                    ok = true, content = result.content,
                    providerUsed = lastProvider, modelUsed = lastModel,
                    totalInputTokens = totalIn, totalOutputTokens = totalOut,
                    stepsTaken = step, toolCallsMade = toolCallsMade,
                )
            }

            // Append the assistant's tool-call message to the conversation
            conversation.add(LlmMessage(
                role = "assistant",
                content = result.content,
                toolCalls = toolCalls,
            ))

            // Execute each tool call and append the results
            for (tc in toolCalls) {
                toolCallsMade++
                val toolName = tc.function.name
                val toolArgs = tc.function.arguments
                val tool = tools[toolName]

                val toolResult = if (tool == null) {
                    log.warn("Agent called unknown tool '{}'", toolName)
                    """{"error": "unknown tool: $toolName"}"""
                } else {
                    val executed = runCatching { tool.execute(schoolId, toolArgs) }
                    executed.onFailure { ex ->
                        log.warn("Tool '{}' execution failed: {}", toolName, ex.message)
                    }
                    executed.getOrElse { ex ->
                        """{"error": "${ex.message ?: "execution failed"}"}"""
                    }
                }

                conversation.add(LlmMessage(
                    role = "tool",
                    content = toolResult,
                    toolCallId = tc.id,
                ))
            }
        }

        // Hit the step cap — force a final completion without tools
        log.warn("Agent hit maxSteps={} ({} tool calls) — forcing final response", maxSteps, toolCallsMade)
        val finalResult = complete(
            feature = feature, lane = lane,
            messages = conversation.toList(),
            containsPii = containsPii, schoolId = schoolId,
            temperature = temperature, maxTokens = maxTokens,
            cache = false,
            tools = null,
            toolChoice = "none",
        )

        totalIn += finalResult.inputTokens
        totalOut += finalResult.outputTokens

        return if (finalResult.ok) {
            AgentResult(
                ok = true, content = finalResult.content,
                providerUsed = finalResult.providerUsed ?: lastProvider,
                modelUsed = finalResult.modelUsed ?: lastModel,
                totalInputTokens = totalIn, totalOutputTokens = totalOut,
                stepsTaken = maxSteps, toolCallsMade = toolCallsMade,
            )
        } else {
            AgentResult.unavailable("agent step cap reached and final response failed")
        }
    }
}
