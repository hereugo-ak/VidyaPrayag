/*
 * File: AiRouting.kt
 * Module: feature.ai
 *
 * Admin surface for the AI gateway (PEWS_AI_GATEWAY_IMPLEMENTATION_PLAN §6 last
 * acceptance row: "Admin AI-usage screen shows per-school tokens + provider
 * health"). Two audiences:
 *
 *   SCHOOL ADMIN (requireSchoolAdmin) — sees THEIR school's AI usage rollup only:
 *     GET  /api/v1/school/ai/usage           — token + call counts for this school
 *
 *   PLATFORM ADMIN (requirePlatformAdmin) — manages the shared provider registry:
 *     GET  /api/v1/admin/ai/providers        — provider rows (keys MASKED) + health
 *     GET  /api/v1/admin/ai/health           — live circuit-breaker snapshot
 *     POST /api/v1/admin/ai/providers/{provider}/rotate  — set a new key (encrypted),
 *                                              invalidate cache → live without redeploy
 *
 * Keys are NEVER returned in plaintext — only masked (sk-****…****). The rotate
 * endpoint takes the raw key in the body, encrypts at rest via KeyVault, and
 * invalidates the in-memory cache so the next call picks it up.
 */
package com.littlebridge.enrollplus.feature.ai

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requirePlatformAdmin
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.db.AiProviderConfigTable
import com.littlebridge.enrollplus.db.AiUsageLogTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.temporal.ChronoUnit

// ── DTOs ────────────────────────────────────────────────────────────────────

@Serializable
data class AiUsageRollupDto(
    @kotlinx.serialization.SerialName("total_calls") val totalCalls: Int,
    @kotlinx.serialization.SerialName("success_calls") val successCalls: Int,
    @kotlinx.serialization.SerialName("cached_calls") val cachedCalls: Int,
    @kotlinx.serialization.SerialName("failed_calls") val failedCalls: Int,
    @kotlinx.serialization.SerialName("input_tokens") val inputTokens: Long,
    @kotlinx.serialization.SerialName("output_tokens") val outputTokens: Long,
    @kotlinx.serialization.SerialName("by_feature") val byFeature: List<AiUsageFeatureDto>,
    @kotlinx.serialization.SerialName("window_days") val windowDays: Int,
)

@Serializable
data class AiUsageFeatureDto(
    val feature: String,
    val calls: Int,
    @kotlinx.serialization.SerialName("input_tokens") val inputTokens: Long,
    @kotlinx.serialization.SerialName("output_tokens") val outputTokens: Long,
)

@Serializable
data class AiProviderDto(
    val provider: String,
    val model: String,
    @kotlinx.serialization.SerialName("base_url") val baseUrl: String,
    @kotlinx.serialization.SerialName("is_active") val isActive: Boolean,
    val tier: String,
    @kotlinx.serialization.SerialName("no_training") val noTraining: Boolean,
    @kotlinx.serialization.SerialName("key_configured") val keyConfigured: Boolean,
    @kotlinx.serialization.SerialName("circuit_state") val circuitState: String,
)

@Serializable
data class AiHealthDto(
    val provider: String,
    val model: String,
    val state: String,
    @kotlinx.serialization.SerialName("total_requests") val totalRequests: Long,
    @kotlinx.serialization.SerialName("total_failures") val totalFailures: Long,
    @kotlinx.serialization.SerialName("rate_limit_hits") val rateLimitHits: Long,
    @kotlinx.serialization.SerialName("avg_latency_ms") val avgLatencyMs: Long,
)

@Serializable
data class RotateKeyRequest(
    @kotlinx.serialization.SerialName("api_key") val apiKey: String,
    val model: String? = null,
    @kotlinx.serialization.SerialName("base_url") val baseUrl: String? = null,
)

// ── Routes ────────────────────────────────────────────────────────────────

fun Route.aiRouting() {
    authenticate("jwt") {

        // ---- SCHOOL ADMIN: this school's AI usage rollup ----
        get("/api/v1/school/ai/usage") {
            val ctx = call.requireSchoolAdmin() ?: return@get
            val windowDays = (call.request.queryParameters["days"]?.toIntOrNull() ?: 30).coerceIn(1, 365)
            val since = Instant.now().minus(windowDays.toLong(), ChronoUnit.DAYS)

            val rollup = dbQuery {
                val rows = AiUsageLogTable.selectAll().where {
                    (AiUsageLogTable.schoolId eq ctx.schoolId) and
                        (AiUsageLogTable.createdAt greater since)
                }.toList()

                val byFeatureMap = HashMap<String, IntArray>()      // feature -> [calls, in, out]
                var totalCalls = 0; var success = 0; var cached = 0; var failed = 0
                var inTok = 0L; var outTok = 0L
                rows.forEach { r ->
                    totalCalls++
                    when (r[AiUsageLogTable.status]) {
                        "success" -> success++
                        "cached" -> cached++
                        else -> failed++
                    }
                    val ri = r[AiUsageLogTable.inputTokens]
                    val ro = r[AiUsageLogTable.outputTokens]
                    inTok += ri; outTok += ro
                    val f = r[AiUsageLogTable.feature]
                    val arr = byFeatureMap.getOrPut(f) { intArrayOf(0, 0, 0) }
                    arr[0]++; arr[1] += ri; arr[2] += ro
                }
                AiUsageRollupDto(
                    totalCalls = totalCalls, successCalls = success, cachedCalls = cached,
                    failedCalls = failed, inputTokens = inTok, outputTokens = outTok,
                    byFeature = byFeatureMap.map { (f, a) ->
                        AiUsageFeatureDto(f, a[0], a[1].toLong(), a[2].toLong())
                    }.sortedByDescending { it.calls },
                    windowDays = windowDays
                )
            }
            call.ok(rollup, "AI usage")
        }

        // ---- PLATFORM ADMIN: provider registry (keys masked) ----
        get("/api/v1/admin/ai/providers") {
            call.requirePlatformAdmin() ?: return@get
            val rows = dbQuery {
                AiProviderConfigTable.selectAll().toList().map { r ->
                    val providerCode = r[AiProviderConfigTable.provider]
                    val model = r[AiProviderConfigTable.model]
                    val enc = r[AiProviderConfigTable.apiKeyEncrypted]
                    AiProviderDto(
                        provider = providerCode,
                        model = model,
                        baseUrl = r[AiProviderConfigTable.baseUrl],
                        isActive = r[AiProviderConfigTable.isActive],
                        tier = r[AiProviderConfigTable.tier],
                        noTraining = r[AiProviderConfigTable.noTraining],
                        keyConfigured = enc.isNotBlank(),
                        circuitState = CircuitBreaker.stateOf(providerCode, model).name.lowercase(),
                    )
                }
            }
            call.ok(rows, "AI providers")
        }

        // ---- PLATFORM ADMIN: live circuit-breaker health ----
        get("/api/v1/admin/ai/health") {
            call.requirePlatformAdmin() ?: return@get
            val snap = CircuitBreaker.snapshot().map {
                AiHealthDto(
                    provider = it.provider, model = it.model, state = it.state.lowercase(),
                    totalRequests = it.totalRequests, totalFailures = it.totalFailures,
                    rateLimitHits = it.rateLimitHits, avgLatencyMs = it.avgLatencyMs,
                )
            }
            call.ok(snap, "AI provider health")
        }

        // ---- PLATFORM ADMIN: rotate a provider key (live, no redeploy) ----
        post("/api/v1/admin/ai/providers/{provider}/rotate") {
            call.requirePlatformAdmin() ?: return@post
            val providerCode = call.parameters["provider"] ?: run {
                call.fail("provider is required"); return@post
            }
            val provider = AiProvider.fromCode(providerCode) ?: run {
                call.fail("Unknown provider: $providerCode", HttpStatusCode.BadRequest, "UNKNOWN_PROVIDER")
                return@post
            }
            val body = runCatching { call.receive<RotateKeyRequest>() }.getOrNull() ?: run {
                call.fail("Invalid request body"); return@post
            }
            if (body.apiKey.isBlank()) {
                call.fail("api_key must not be blank"); return@post
            }

            val encryption = EncryptionService()
            val encrypted = encryption.encrypt(body.apiKey)
            val model = body.model ?: KeyVault.modelFor(provider)
            val baseUrl = body.baseUrl ?: KeyVault.baseUrlFor(provider)

            dbQuery {
                val now = Instant.now()
                val existing = AiProviderConfigTable.selectAll().where {
                    (AiProviderConfigTable.provider eq provider.code) and
                        (AiProviderConfigTable.model eq model)
                }.singleOrNull()
                if (existing == null) {
                    AiProviderConfigTable.insert {
                        it[AiProviderConfigTable.provider] = provider.code
                        it[AiProviderConfigTable.model] = model
                        it[apiKeyEncrypted] = encrypted
                        it[AiProviderConfigTable.baseUrl] = baseUrl
                        it[isActive] = true
                        it[priority] = 0
                        it[tier] = provider.tier
                        it[noTraining] = provider.noTraining
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                } else {
                    AiProviderConfigTable.update({
                        (AiProviderConfigTable.provider eq provider.code) and
                            (AiProviderConfigTable.model eq model)
                    }) {
                        it[apiKeyEncrypted] = encrypted
                        it[AiProviderConfigTable.baseUrl] = baseUrl
                        it[updatedAt] = now
                    }
                }
            }
            // Invalidate cache so the very next call uses the new key.
            KeyVault.invalidate(provider)

            call.ok(
                mapOf(
                    "provider" to provider.code,
                    "model" to model,
                    "masked_key" to encryption.mask(body.apiKey),
                ),
                "Provider key rotated"
            )
        }
    }
}
