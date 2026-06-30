/*
 * File: CircuitBreaker.kt
 * Module: feature.ai
 *
 * A per-(provider, model) circuit breaker so one dead/rate-limited free-tier
 * provider never repeatedly blocks a lane. Classic three-state machine:
 *
 *   CLOSED      → calls flow; failures counted.
 *   OPEN        → after N consecutive failures, the provider is skipped for a
 *                 cooldown window (calls "short-circuit" → fail over instantly).
 *   HALF_OPEN   → after cooldown, ONE trial call is allowed; success closes the
 *                 circuit, failure re-opens it.
 *
 * State is persisted to `ai_provider_health` (so health is visible to the admin
 * AI-usage screen and survives restarts) AND mirrored in-memory for hot-path
 * speed. The in-memory map is the source of truth for routing decisions; the DB
 * is the durable mirror + observability.
 *
 * Thresholds are env-tunable (AI_CIRCUIT_FAILS_TO_OPEN / AI_CIRCUIT_COOLDOWN_SEC)
 * with sane defaults, matching the gateway behaviour block in the plan §5.3.
 */
package com.littlebridge.enrollplus.feature.ai

import com.littlebridge.enrollplus.db.AiProviderHealthTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.io.File
import java.time.Instant
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap

object CircuitBreaker {
    private val log = LoggerFactory.getLogger("AiCircuitBreaker")

    enum class State { CLOSED, OPEN, HALF_OPEN }

    private data class Health(
        var state: State = State.CLOSED,
        var consecutiveFailures: Int = 0,
        var totalRequests: Long = 0,
        var totalFailures: Long = 0,
        var rateLimitHits: Long = 0,
        var openedAt: Instant? = null,
        var lastFailureAt: Instant? = null,
        var avgLatencyMs: Long = 0,
    )

    private val states = ConcurrentHashMap<String, Health>()

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

    private val failsToOpen: Int get() = env("AI_CIRCUIT_FAILS_TO_OPEN")?.toIntOrNull() ?: 5
    private val cooldownSec: Long get() = env("AI_CIRCUIT_COOLDOWN_SEC")?.toLongOrNull() ?: 30L

    private fun key(provider: String, model: String) = "$provider::$model"

    /**
     * True when a call to (provider, model) is allowed right now. When OPEN and
     * the cooldown has elapsed, transitions to HALF_OPEN and allows ONE trial.
     */
    fun allow(provider: String, model: String): Boolean {
        val h = states.getOrPut(key(provider, model)) { Health() }
        return when (h.state) {
            State.CLOSED, State.HALF_OPEN -> true
            State.OPEN -> {
                val opened = h.openedAt ?: Instant.EPOCH
                if (Instant.now().isAfter(opened.plusSeconds(cooldownSec))) {
                    h.state = State.HALF_OPEN
                    log.info("Circuit {} → HALF_OPEN (cooldown elapsed)", key(provider, model))
                    true
                } else {
                    false
                }
            }
        }
    }

    /** Current state (for health surfaces / routing introspection). */
    fun stateOf(provider: String, model: String): State =
        states[key(provider, model)]?.state ?: State.CLOSED

    /** Record a successful call → closes the circuit, resets the failure run. */
    suspend fun recordSuccess(provider: String, model: String, latencyMs: Long) {
        val h = states.getOrPut(key(provider, model)) { Health() }
        val prevState = h.state
        h.consecutiveFailures = 0
        h.totalRequests++
        h.state = State.CLOSED
        h.openedAt = null
        // simple rolling average
        h.avgLatencyMs = if (h.avgLatencyMs == 0L) latencyMs else (h.avgLatencyMs * 3 + latencyMs) / 4
        if (prevState != State.CLOSED) log.info("Circuit {} → CLOSED (recovered)", key(provider, model))
        persist(provider, model, h)
    }

    /**
     * Record a failure. After [failsToOpen] consecutive failures (or immediately
     * from HALF_OPEN), the circuit OPENS. Rate-limit hits are tracked separately
     * for the health screen.
     */
    suspend fun recordFailure(provider: String, model: String, rateLimited: Boolean) {
        val h = states.getOrPut(key(provider, model)) { Health() }
        h.consecutiveFailures++
        h.totalRequests++
        h.totalFailures++
        h.lastFailureAt = Instant.now()
        if (rateLimited) h.rateLimitHits++

        val shouldOpen = h.state == State.HALF_OPEN || h.consecutiveFailures >= failsToOpen
        if (shouldOpen && h.state != State.OPEN) {
            h.state = State.OPEN
            h.openedAt = Instant.now()
            log.warn("Circuit {} → OPEN ({} consecutive failures, cooldown {}s)",
                key(provider, model), h.consecutiveFailures, cooldownSec)
        }
        persist(provider, model, h)
    }

    /** Snapshot for the admin AI-health screen. */
    data class HealthSnapshot(
        val provider: String,
        val model: String,
        val state: String,
        val totalRequests: Long,
        val totalFailures: Long,
        val consecutiveFailures: Int,
        val rateLimitHits: Long,
        val avgLatencyMs: Long,
    )

    fun snapshot(): List<HealthSnapshot> =
        AiProvider.entries.map { provider ->
            val model = provider.defaultModel
            val k = key(provider.code, model)
            val h = states[k]
            if (h != null) {
                HealthSnapshot(provider.code, model, h.state.name, h.totalRequests, h.totalFailures,
                    h.consecutiveFailures, h.rateLimitHits, h.avgLatencyMs)
            } else {
                HealthSnapshot(provider.code, model, State.CLOSED.name, 0, 0, 0, 0, 0)
            }
        }

    // ── DB mirror (best-effort; never blocks routing) ────────────────────────
    private suspend fun persist(provider: String, model: String, h: Health) {
        runCatching {
            dbQuery {
                val now = Instant.now()
                val existing = AiProviderHealthTable.selectAll().where {
                    (AiProviderHealthTable.provider eq provider) and
                        (AiProviderHealthTable.model eq model)
                }.singleOrNull()
                if (existing == null) {
                    AiProviderHealthTable.insert {
                        it[AiProviderHealthTable.provider] = provider
                        it[AiProviderHealthTable.model] = model
                        it[circuitState] = h.state.name.lowercase()
                        it[totalRequests] = h.totalRequests.toInt()
                        it[totalFailures] = h.totalFailures.toInt()
                        it[consecutiveFailures] = h.consecutiveFailures
                        it[avgLatencyMs] = h.avgLatencyMs.toInt()
                        it[rateLimitHits] = h.rateLimitHits.toInt()
                        it[lastFailureAt] = h.lastFailureAt
                        it[circuitOpenedAt] = h.openedAt
                        it[lastUpdated] = now
                    }
                } else {
                    AiProviderHealthTable.update({
                        (AiProviderHealthTable.provider eq provider) and
                            (AiProviderHealthTable.model eq model)
                    }) {
                        it[circuitState] = h.state.name.lowercase()
                        it[totalRequests] = h.totalRequests.toInt()
                        it[totalFailures] = h.totalFailures.toInt()
                        it[consecutiveFailures] = h.consecutiveFailures
                        it[avgLatencyMs] = h.avgLatencyMs.toInt()
                        it[rateLimitHits] = h.rateLimitHits.toInt()
                        it[lastFailureAt] = h.lastFailureAt
                        it[circuitOpenedAt] = h.openedAt
                        it[lastUpdated] = now
                    }
                }
            }
        }.onFailure { log.debug("Circuit health persist skipped: {}", it.message) }
    }
}
