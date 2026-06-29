/*
 * File: EnvConfig.kt
 * Module: core
 *
 * Single, dotenv-aware configuration resolver for the whole server.
 *
 * THE BUG THIS FIXES
 * ──────────────────
 * The project documents every secret (DB creds, AI provider keys, …) in
 * `.env` (see `.env.example`). `DatabaseFactory` correctly read those via the
 * `dotenv-kotlin` library, but `KeyVault` / `EncryptionService` only looked at
 * `System.getenv()` + `local.properties`. On a typical local run (Android
 * Studio / `./gradlew run` on Windows) a `.env` file is NOT loaded into the JVM
 * process environment, so `System.getenv("AI_CEREBRAS_API_KEY")` is null even
 * though the key is sitting in `.env`. Result: `KeyVault.bootstrapFromEnv()`
 * seeded 0 providers, `AiService.anyProviderConfigured()` returned false, and
 * every PEWS run logged "PEWS reasoning skipped … no AI provider configured" —
 * exactly the symptom in the backend log, even after the user filled in all the
 * keys.
 *
 * THE FIX
 * ───────
 * Resolve config from the SAME three sources DatabaseFactory uses, in the same
 * order, through one shared helper:
 *
 *     1. .env            (via dotenv-kotlin — the documented path)
 *     2. System.getenv() (Render / shell-exported env in production)
 *     3. local.properties at the repo root (Android-Studio DX convenience)
 *
 * Values are sanitised (trim + strip a single pair of surrounding quotes) so a
 * key pasted as `AI_GROQ_API_KEY="gsk_…"` still works. Blank → treated as
 * "unset" so an empty placeholder line never counts as configured.
 */
package com.littlebridge.enrollplus.core

import io.github.cdimascio.dotenv.dotenv
import java.io.File
object EnvConfig {

    // dotenv is resilient: missing/malformed .env is fine (we fall through to
    // System.getenv / local.properties). Built once, lazily.
    private val dotenv by lazy {
        runCatching {
            dotenv {
                ignoreIfMalformed = true
                ignoreIfMissing = true
            }
        }.getOrNull()
    }

    // NOTE: We read local.properties manually instead of using Properties.load()
    // because Properties.load() treats backslashes as escape characters — on
    // Windows, a path like C:\Users\HP\firebase\creds.json becomes
    // C:UsersHPirebasecreds.json (\f → form-feed, \U → stripped). Manual
    // line-by-line parsing preserves backslashes in file paths.
    private val localProps: Map<String, String> by lazy {
        val candidates = listOf(
            File("local.properties"),
            File("../local.properties"),
            File(System.getProperty("user.dir"), "local.properties"),
        )
        val props = mutableMapOf<String, String>()
        candidates.firstOrNull { it.isFile }?.let { f ->
            runCatching {
                f.readLines().forEach { line ->
                    val trimmed = line.trim()
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) return@forEach
                    val eqIdx = trimmed.indexOf('=')
                    val colonIdx = trimmed.indexOf(':')
                    val sepIdx = when {
                        eqIdx < 0 -> colonIdx
                        colonIdx < 0 -> eqIdx
                        else -> minOf(eqIdx, colonIdx)
                    }
                    if (sepIdx <= 0) return@forEach
                    val key = trimmed.substring(0, sepIdx).trim()
                    val value = trimmed.substring(sepIdx + 1).trim()
                    if (key.isNotEmpty()) props[key] = value
                }
            }
        }
        props
    }

    /**
     * Resolve [key] from .env → environment → local.properties, sanitised.
     * Returns null when unset/blank in all three sources.
     */
    fun get(key: String): String? =
        (dotenv?.get(key) ?: System.getenv(key) ?: localProps[key])
            ?.let(::sanitize)
            ?.takeIf { it.isNotBlank() }

    /** [get] with a fallback when the key is unset/blank. */
    fun get(key: String, default: String): String = get(key) ?: default

    private fun sanitize(raw: String): String {
        var v = raw.trim()
        if (v.length >= 2 &&
            ((v.startsWith("\"") && v.endsWith("\"")) ||
                (v.startsWith("'") && v.endsWith("'")))
        ) {
            v = v.substring(1, v.length - 1).trim()
        }
        return v
    }
}
