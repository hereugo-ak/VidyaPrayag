/*
 * File: KeyVault.kt
 * Module: feature.ai
 *
 * Two-layer provider-key management (PEWS_AI_GATEWAY_IMPLEMENTATION_PLAN §4):
 *
 *   Layer 1 — BOOTSTRAP (env, on Render)        Layer 2 — RUNTIME (DB, encrypted)
 *   ───────────────────────────────────────     ─────────────────────────────────────
 *   AI_<PROVIDER>_API_KEY  (raw key)       ───►  ai_provider_config.api_key_encrypted
 *   AI_ENCRYPTION_KEY      (32-byte hex)          (AES-256-GCM, IV-prefixed, base64)
 *   AI_<PROVIDER>_BASE_URL (optional)             KeyVault decrypts on use + in-mem cache
 *
 * On boot, `bootstrapFromEnv()` reads each AI_<PROVIDER>_API_KEY, encrypts it
 * with EncryptionService, and idempotently UPSERTs the matching
 * ai_provider_config row(s). At runtime, callers ask `keyFor(provider)` which
 * decrypts from the DB once and caches the plaintext in memory — so a rotation
 * (admin updates the row, then calls `invalidate`) goes live without a redeploy.
 *
 * Design choices (consistent with the rest of the server):
 *   - Module-level singleton `object` — NO Koin (matches Notify, PulseWeeklyJob).
 *   - Env read = System.getenv → root local.properties fallback (matches
 *     DatabaseFactory.resolve / OtpService.env), so local dev works too.
 *   - Keys are NEVER logged in plaintext (only masked) and never returned in
 *     an API response.
 *   - Graceful degradation: a missing/blank key → that provider is simply
 *     "not configured"; the lane skips it. The product never hard-fails on AI.
 */
package com.littlebridge.enrollplus.feature.ai

import com.littlebridge.enrollplus.db.AiProviderConfigTable
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

/**
 * The five OpenAI-compatible free-tier providers we dual-home across. The
 * `tier` is the default lane this provider primarily serves; the `noTraining`
 * flag gates whether a PII-bearing prompt may ever reach it (privacy routing).
 */
enum class AiProvider(
    val code: String,
    val defaultBaseUrl: String,
    val defaultModelEnv: String,
    val defaultModel: String,
    val tier: String,
    /** false ⇒ provider trains on inputs (PII-restricted): Mistral/SambaNova. */
    val noTraining: Boolean,
) {
    CEREBRAS(
        code = "cerebras",
        defaultBaseUrl = "https://api.cerebras.ai/v1",
        defaultModelEnv = "AI_MODEL_CEREBRAS",
        defaultModel = "gpt-oss-120b",
        tier = "fast",
        noTraining = true,
    ),
    GROQ(
        code = "groq",
        defaultBaseUrl = "https://api.groq.com/openai/v1",
        defaultModelEnv = "AI_MODEL_GROQ_REASON",
        defaultModel = "llama-3.3-70b-versatile",
        tier = "reason",
        noTraining = true,
    ),
    SAMBANOVA(
        code = "sambanova",
        defaultBaseUrl = "https://api.sambanova.ai/v1",
        defaultModelEnv = "AI_MODEL_SAMBANOVA",
        defaultModel = "DeepSeek-V3.1",
        tier = "reason",
        // SambaNova free tier may use inputs for product improvement → treat as
        // training-opt-in (PII-restricted) unless an operator overrides.
        noTraining = false,
    ),
    MISTRAL(
        code = "mistral",
        defaultBaseUrl = "https://api.mistral.ai/v1",
        defaultModelEnv = "AI_MODEL_MISTRAL",
        defaultModel = "mistral-large-latest",
        tier = "batch",
        // Mistral "La Plateforme" free Experiment plan trains on data → opt-in.
        noTraining = false,
    ),
    OPENROUTER(
        code = "openrouter",
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        defaultModelEnv = "AI_MODEL_OPENROUTER",
        defaultModel = "meta-llama/llama-3.3-70b-instruct:free",
        tier = "reason",
        noTraining = true,
    );

    /** env var holding the raw API key for this provider. */
    val apiKeyEnv: String get() = "AI_${name}_API_KEY"

    /** env var optionally overriding the OpenAI-compatible base URL. */
    val baseUrlEnv: String get() = "AI_${name}_BASE_URL"

    companion object {
        fun fromCode(code: String): AiProvider? =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) }
    }
}

object KeyVault {
    private val log = LoggerFactory.getLogger("AiKeyVault")
    private val encryption = EncryptionService()

    /** Decrypted plaintext key cache, keyed by provider code. */
    private val keyCache = ConcurrentHashMap<String, String>()
    /** Resolved base-url cache (env override or DB or baked default). */
    private val baseUrlCache = ConcurrentHashMap<String, String>()

    @Volatile
    private var bootstrapped = false

    // ------------------------------------------------------------------
    // Env reading (System.getenv → root local.properties fallback)
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Bootstrap — env → encrypt → upsert ai_provider_config (idempotent)
    // ------------------------------------------------------------------

    /**
     * Seed/refresh one ai_provider_config row per provider from the
     * AI_<PROVIDER>_API_KEY env vars. Idempotent: re-encrypts and updates the
     * existing row when the env key changed; inserts when absent; leaves rows
     * for providers with no env key untouched (so an admin-only key isn't wiped).
     * Safe to call once at boot. Never logs a plaintext key.
     */
    suspend fun bootstrapFromEnv() {
        if (bootstrapped) return
        bootstrapped = true

        if (!encryption.isConfigured) {
            log.warn(
                "AI_ENCRYPTION_KEY not set — provider keys will be seeded in DEV " +
                    "passthrough mode. Set AI_ENCRYPTION_KEY in production (openssl rand -hex 32)."
            )
        }

        var seeded = 0
        for (provider in AiProvider.entries) {
            val rawKey = env(provider.apiKeyEnv)
            val baseUrl = env(provider.baseUrlEnv) ?: provider.defaultBaseUrl
            val model = env(provider.defaultModelEnv) ?: provider.defaultModel
            baseUrlCache[provider.code] = baseUrl

            if (rawKey == null) {
                log.info("AI provider {} has no {} set — skipping (lane will skip it).",
                    provider.code, provider.apiKeyEnv)
                continue
            }

            val encrypted = encryption.encrypt(rawKey)
            runCatching { upsertProviderRow(provider, model, encrypted, baseUrl) }
                .onSuccess {
                    keyCache[provider.code] = rawKey        // warm the cache from env
                    seeded++
                    log.info("AI provider {} seeded (model={}, key={}, noTraining={})",
                        provider.code, model, encryption.mask(rawKey), provider.noTraining)
                }
                .onFailure { log.warn("Failed to seed AI provider {}: {}", provider.code, it.message) }
        }
        log.info("KeyVault bootstrap complete — {}/{} providers configured.",
            seeded, AiProvider.entries.size)
    }

    private suspend fun upsertProviderRow(
        provider: AiProvider,
        model: String,
        encryptedKey: String,
        baseUrl: String,
    ) = dbQuery {
        val now = Instant.now()
        val existing = AiProviderConfigTable.selectAll().where {
            (AiProviderConfigTable.provider eq provider.code) and
                (AiProviderConfigTable.model eq model)
        }.singleOrNull()

        if (existing == null) {
            AiProviderConfigTable.insert {
                it[AiProviderConfigTable.provider] = provider.code
                it[AiProviderConfigTable.model] = model
                it[apiKeyEncrypted] = encryptedKey
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
                it[apiKeyEncrypted] = encryptedKey
                it[AiProviderConfigTable.baseUrl] = baseUrl
                it[noTraining] = provider.noTraining
                it[updatedAt] = now
            }
        }
    }

    // ------------------------------------------------------------------
    // Runtime lookups (decrypt-on-demand, cached)
    // ------------------------------------------------------------------

    /**
     * The plaintext API key for [provider], or null if the provider has no key
     * configured (env nor DB). Decrypts from ai_provider_config once, then
     * serves the in-memory cache. NEVER log the returned value.
     */
    suspend fun keyFor(provider: AiProvider): String? {
        keyCache[provider.code]?.let { return it }

        // 1) DB (the hot path / rotation source of truth once bootstrapped)
        val encrypted = dbQuery {
            AiProviderConfigTable.selectAll().where {
                (AiProviderConfigTable.provider eq provider.code) and
                    (AiProviderConfigTable.isActive eq true)
            }.orderBy(AiProviderConfigTable.priority)
                .firstOrNull()
                ?.get(AiProviderConfigTable.apiKeyEncrypted)
        }
        if (!encrypted.isNullOrBlank()) {
            encryption.decrypt(encrypted)?.let { plain ->
                keyCache[provider.code] = plain
                return plain
            }
        }

        // 2) Last-ditch env fallback (DB row missing but env present)
        env(provider.apiKeyEnv)?.let { raw ->
            keyCache[provider.code] = raw
            return raw
        }
        return null
    }

    /** The active model id for [provider] (DB row, else env, else baked default). */
    suspend fun modelFor(provider: AiProvider): String {
        val dbModel = dbQuery {
            AiProviderConfigTable.selectAll().where {
                (AiProviderConfigTable.provider eq provider.code) and
                    (AiProviderConfigTable.isActive eq true)
            }.orderBy(AiProviderConfigTable.priority)
                .firstOrNull()
                ?.get(AiProviderConfigTable.model)
        }
        return dbModel ?: env(provider.defaultModelEnv) ?: provider.defaultModel
    }

    /** OpenAI-compatible base URL for [provider] (env override or baked default). */
    fun baseUrlFor(provider: AiProvider): String =
        baseUrlCache.getOrPut(provider.code) {
            env(provider.baseUrlEnv) ?: provider.defaultBaseUrl
        }

    /**
     * Cheap (non-suspending) "do we have any key path at all" check for health
     * surfaces and graceful skip. Uses the warmed cache or env; a provider whose
     * key lives ONLY in the DB (admin-added, never env) is confirmed via
     * [keyFor] on first use, which then warms the cache.
     */
    fun isConfigured(provider: AiProvider): Boolean =
        keyCache.containsKey(provider.code) || env(provider.apiKeyEnv) != null

    /** Suspending, authoritative variant that also consults the DB. */
    suspend fun isConfiguredNow(provider: AiProvider): Boolean = keyFor(provider) != null

    /** Drop the cached plaintext for [provider] so the next use re-reads the DB. */
    fun invalidate(provider: AiProvider) {
        keyCache.remove(provider.code)
        baseUrlCache.remove(provider.code)
        log.info("KeyVault cache invalidated for provider {}", provider.code)
    }

    /** Drop the entire cache (e.g. after AI_ENCRYPTION_KEY rotation + re-seed). */
    fun invalidateAll() {
        keyCache.clear()
        baseUrlCache.clear()
        log.info("KeyVault cache fully invalidated")
    }

    /** For health/admin display only — masked, never the real key. */
    fun maskedKeyFor(rawKey: String?): String = encryption.mask(rawKey)
}
