/*
 * File: FirebaseAdminInitializer.kt
 * Module: feature.notification.firebase
 *
 * Single, lazy initialiser for the Firebase Admin SDK on the server side.
 *
 * WHY A DEDICATED INITIALIZER
 *   The Admin SDK only needs to be initialised ONCE per JVM. Scattering
 *   FirebaseApp.initializeApp() calls across services is a recipe for
 *   double-init exceptions and for credentials leaking into logs. This
 *   object is the ONLY sanctioned entry point — NotificationService calls
 *   [app] to obtain the initialised FirebaseApp (or null when credentials
 *   are absent).
 *
 * CREDENTIAL RESOLUTION (first non-blank wins):
 *   1. FIREBASE_CREDENTIALS_FILE  → absolute path to a service-account JSON
 *                                   on disk (Render / Docker secret mount).
 *   2. FIREBASE_CREDENTIALS_JSON  → the full service-account JSON content in
 *                                   an env var (handy for one-off deploys
 *                                   without a mounted file).
 *   3. GOOGLE_APPLICATION_CREDENTIALS → Google ADC convention (the Admin
 *                                   SDK's own fallback path; we delegate to
 *                                   FirebaseOptions.builder().setCredentials()
 *                                   with ApplicationDefault via getApplicationDefault()).
 *
 * GRACEFUL DEGRADATION
 *   When none of the above resolve, the initializer logs a single WARNING
 *   and [app] returns null. NotificationService then degrades to a no-op
 *   (returns sent=0) instead of crashing the boot. This lets local dev
 *   (SQLite, no Firebase project) run the rest of the API surface unchanged
 *   and lets the migration ship without forcing every operator to wire
 *   Firebase before the next deploy.
 */
package com.littlebridge.vidyaprayag.feature.notification.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.FileInputStream

object FirebaseAdminInitializer {

    private const val APP_NAME = "vidyaprayag-server"

    /** Cached result of the first init attempt — null = not yet attempted. */
    @Volatile
    private var cachedApp: FirebaseApp? = null

    /** True once init has been attempted (success OR graceful failure). */
    @Volatile
    private var attempted: Boolean = false

    /**
     * The initialised FirebaseApp, or null when credentials are unavailable
     * (local dev / not-yet-configured production). Safe to call repeatedly —
     * the first call performs the init, subsequent calls return the cache.
     */
    fun app(): FirebaseApp? {
        if (attempted) return cachedApp
        synchronized(this) {
            if (attempted) return cachedApp
            cachedApp = initialise()
            attempted = true
            if (cachedApp == null) {
                // Single warning — do NOT spam on every dispatch (NotificationService
                // also short-circuits when app()==null).
                println("FIREBASE_INIT: No credentials resolved — push dispatch is DISABLED. Set FIREBASE_CREDENTIALS_FILE / FIREBASE_CREDENTIALS_JSON / GOOGLE_APPLICATION_CREDENTIALS to enable.")
            } else {
                println("FIREBASE_INIT: FirebaseApp '${cachedApp!!.name}' initialised — push dispatch ENABLED.")
            }
            return cachedApp
        }
    }

    /** Convenience: true when [app] would return a non-null FirebaseApp. */
    fun isAvailable(): Boolean = app() != null

    // ------------------------------------------------------------------
    // Internal
    // ------------------------------------------------------------------

    private fun initialise(): FirebaseApp? {
        if (FirebaseApp.getApps().any { it.name == APP_NAME }) {
            return FirebaseApp.getInstance(APP_NAME)
        }

        // Resolve credentials to a GoogleCredentials directly (NOT an
        // InputStream). This lets the ADC path — GoogleCredentials.
        // getApplicationDefault(), which has no underlying stream — flow
        // through the same single initialise() path as the file/JSON paths.
        val credentials = resolveCredentials() ?: return null

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        return runCatching { FirebaseApp.initializeApp(options, APP_NAME) }
            .onFailure { println("FIREBASE_INIT: FirebaseApp.initializeApp failed: ${it.message}") }
            .getOrNull()
    }

    /**
     * Resolve credentials to a [GoogleCredentials] per the order documented in
     * the file header. Returns null when no credential source is configured so
     * the caller can degrade to a no-op push dispatch.
     *
     * Returning the credentials object (rather than a raw InputStream) is what
     * lets the ADC path work: GoogleCredentials.getApplicationDefault() builds
     * credentials from the environment metadata server / well-known files and
     * has no stream to hand back. The previous stream-only design forced that
     * path to bail out confusingly; resolving to GoogleCredentials unifies all
     * four sources behind one FirebaseOptions.builder().setCredentials() call.
     */
    private fun resolveCredentials(): GoogleCredentials? {
        // 1) Explicit service-account file path (Render / Docker secret mount).
        System.getenv("FIREBASE_CREDENTIALS_FILE")?.takeIf { it.isNotBlank() }
            ?.let { path ->
                return runCatching {
                    FileInputStream(path).use { GoogleCredentials.fromStream(it) }
                }.onFailure {
                    println("FIREBASE_INIT: FIREBASE_CREDENTIALS_FILE set but unreadable: ${it.message}")
                }.getOrNull()
            }

        // 2) Inline service-account JSON content (handy for one-off deploys
        //    without a mounted file).
        System.getenv("FIREBASE_CREDENTIALS_JSON")?.takeIf { it.isNotBlank() }
            ?.let { json ->
                return runCatching {
                    GoogleCredentials.fromStream(json.byteInputStream(Charsets.UTF_8))
                }.onFailure {
                    println("FIREBASE_INIT: FIREBASE_CREDENTIALS_JSON set but invalid: ${it.message}")
                }.getOrNull()
            }

        // 3) GOOGLE_APPLICATION_CREDENTIALS — the Google ADC convention env
        //    var pointing at a service-account JSON file. fromStream is used
        //    (not getApplicationDefault) so a malformed path is reported as a
        //    read error rather than a silent fall-through to metadata-server
        //    probing, which is confusing on a developer laptop.
        System.getenv("GOOGLE_APPLICATION_CREDENTIALS")?.takeIf { it.isNotBlank() }
            ?.let { path ->
                return runCatching {
                    FileInputStream(path).use { GoogleCredentials.fromStream(it) }
                }.onFailure {
                    println("FIREBASE_INIT: GOOGLE_APPLICATION_CREDENTIALS set but unreadable: ${it.message}")
                }.getOrNull()
            }

        // 4) Application Default Credentials — works on GCE / Cloud Run /
        //    Compute Engine / Cloud Shell where the metadata server hands out
        //    tokens. getApplicationDefault() throws when no ADC is available
        //    (e.g. a bare dev laptop with none of the env vars above); we
        //    catch that and return null so the caller degrades cleanly.
        return runCatching { GoogleCredentials.getApplicationDefault() }
            .onFailure {
                // Expected on local dev — only log at debug-ish verbosity to
                // avoid noise. The single boot-time WARNING in [app] covers
                // the "push disabled" user-facing message.
                println("FIREBASE_INIT: Application Default Credentials unavailable (${it.message?.take(120)}); no other credential source set.")
            }
            .getOrNull()
    }
}
