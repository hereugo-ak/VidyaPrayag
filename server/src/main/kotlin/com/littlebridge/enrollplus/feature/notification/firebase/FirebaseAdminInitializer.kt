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
package com.littlebridge.enrollplus.feature.notification.firebase

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import java.io.File
import java.io.FileInputStream
import java.util.Properties


object FirebaseAdminInitializer {

    private const val APP_NAME = "vidyaprayag-server"

    @Volatile
    private var cachedApp: FirebaseApp? = null

    @Volatile
    private var attempted: Boolean = false

    @Volatile
    private var credentialSource: String? = null

    /**
     * Returns the initialized FirebaseApp or null when credentials
     * cannot be resolved.
     */
    fun app(): FirebaseApp? {
        if (attempted) return cachedApp

        synchronized(this) {
            if (attempted) return cachedApp

            cachedApp = initialise()
            attempted = true

            if (cachedApp == null) {
                println(
                    "FIREBASE_INIT: No credentials resolved — push dispatch DISABLED."
                )
            } else {
                println(
                    "FIREBASE_INIT: FirebaseApp '${cachedApp!!.name}' initialized using '$credentialSource'."
                )
            }

            return cachedApp
        }
    }

    fun isAvailable(): Boolean = app() != null

    // ------------------------------------------------------------------
    // Initialization
    // ------------------------------------------------------------------

    private fun initialise(): FirebaseApp? {

        FirebaseApp.getApps()
            .firstOrNull { it.name == APP_NAME }
            ?.let {
                println("FIREBASE_INIT: Existing FirebaseApp '$APP_NAME' found.")
                return it
            }

        val credentials = resolveCredentials()
            ?: return null

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()

        return runCatching {
            FirebaseApp.initializeApp(options, APP_NAME)
        }.onSuccess {
            println("FIREBASE_INIT: Firebase Admin SDK initialized.")
        }.onFailure {
            println(
                "FIREBASE_INIT: FirebaseApp.initializeApp failed: ${it.message}"
            )
        }.getOrNull()
    }

    // ------------------------------------------------------------------
    // Credential Resolution
    // ------------------------------------------------------------------

    private fun resolveCredentials(): GoogleCredentials? {

        // --------------------------------------------------------------
        // 1. Render / Railway / Fly.io
        // --------------------------------------------------------------
        System.getenv("FIREBASE_CREDENTIALS_JSON")
            ?.takeIf { it.isNotBlank() }
            ?.let { json ->

                println(
                    "FIREBASE_INIT: Attempting FIREBASE_CREDENTIALS_JSON"
                )

                return runCatching {
                    GoogleCredentials.fromStream(
                        json.byteInputStream(Charsets.UTF_8)
                    )
                }.onSuccess {
                    credentialSource = "FIREBASE_CREDENTIALS_JSON"

                    println(
                        "FIREBASE_INIT: Loaded credentials from FIREBASE_CREDENTIALS_JSON"
                    )
                }.onFailure {
                    println(
                        "FIREBASE_INIT: Invalid FIREBASE_CREDENTIALS_JSON: ${it.message}"
                    )
                }.getOrNull()
            }

        // --------------------------------------------------------------
        // 2. Explicit credentials file
        // --------------------------------------------------------------
        System.getenv("FIREBASE_CREDENTIALS_FILE")
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->

                println(
                    "FIREBASE_INIT: Attempting FIREBASE_CREDENTIALS_FILE"
                )

                return runCatching {
                    FileInputStream(path).use {
                        GoogleCredentials.fromStream(it)
                    }
                }.onSuccess {
                    credentialSource = "FIREBASE_CREDENTIALS_FILE"

                    println(
                        "FIREBASE_INIT: Loaded credentials from FIREBASE_CREDENTIALS_FILE ($path)"
                    )
                }.onFailure {
                    println(
                        "FIREBASE_INIT: Cannot read FIREBASE_CREDENTIALS_FILE ($path): ${it.message}"
                    )
                }.getOrNull()
            }

        // --------------------------------------------------------------
        // 3. Standard Google env var
        // --------------------------------------------------------------
        System.getenv("GOOGLE_APPLICATION_CREDENTIALS")
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->

                println(
                    "FIREBASE_INIT: Attempting GOOGLE_APPLICATION_CREDENTIALS"
                )

                return runCatching {
                    FileInputStream(path).use {
                        GoogleCredentials.fromStream(it)
                    }
                }.onSuccess {
                    credentialSource = "GOOGLE_APPLICATION_CREDENTIALS"

                    println(
                        "FIREBASE_INIT: Loaded credentials from GOOGLE_APPLICATION_CREDENTIALS ($path)"
                    )
                }.onFailure {
                    println(
                        "FIREBASE_INIT: Cannot read GOOGLE_APPLICATION_CREDENTIALS ($path): ${it.message}"
                    )
                }.getOrNull()
            }

        // --------------------------------------------------------------
        // 4. local.properties
        // --------------------------------------------------------------
        localProperty("firebase.credentials.file")
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->

                println(
                    "FIREBASE_INIT: Attempting local.properties credential"
                )

                return runCatching {
                    FileInputStream(path).use {
                        GoogleCredentials.fromStream(it)
                    }
                }.onSuccess {
                    credentialSource = "local.properties"

                    println(
                        "FIREBASE_INIT: Loaded credentials from local.properties ($path)"
                    )
                }.onFailure {
                    println(
                        "FIREBASE_INIT: Cannot read local.properties credential file ($path): ${it.message}"
                    )
                }.getOrNull()
            }

        // --------------------------------------------------------------
        // 5. Application Default Credentials
        // --------------------------------------------------------------
        println(
            "FIREBASE_INIT: Attempting Application Default Credentials"
        )

        return runCatching {
            GoogleCredentials.getApplicationDefault()
        }.onSuccess {
            credentialSource = "Application Default Credentials"

            println(
                "FIREBASE_INIT: Loaded Application Default Credentials"
            )
        }.onFailure {
            println(
                "FIREBASE_INIT: Application Default Credentials unavailable (${it.message?.take(200)})"
            )

            println(
                """
                FIREBASE_INIT: No credentials resolved.

                Supported sources:
                - FIREBASE_CREDENTIALS_JSON
                - FIREBASE_CREDENTIALS_FILE
                - GOOGLE_APPLICATION_CREDENTIALS
                - local.properties (firebase.credentials.file)
                - Application Default Credentials
                """.trimIndent()
            )
        }.getOrNull()
    }

    // ------------------------------------------------------------------
    // local.properties
    // ------------------------------------------------------------------

    private fun localProperty(key: String): String? {
        val localProperties = findLocalProperties()

        if (localProperties == null) {
            println(
                """
            FIREBASE_INIT: local.properties not found.
            Searched from:
            ${System.getProperty("user.dir")}
            """.trimIndent()
            )
            return null
        }

        println(
            "FIREBASE_INIT: Found local.properties at ${localProperties.absolutePath}"
        )

        return runCatching {
            val props = Properties()

            localProperties.inputStream().use {
                props.load(it)
            }

            props.getProperty(key)
        }.onFailure {
            println(
                "FIREBASE_INIT: Failed reading local.properties: ${it.message}"
            )
        }.getOrNull()
    }

    private fun findLocalProperties(): File? {
        var current = File(System.getProperty("user.dir")).absoluteFile

        while (true) {

            val candidate = File(current, "local.properties")

            if (candidate.exists()) {
                return candidate
            }

            current = current.parentFile ?: break
        }

        return null
    }
}
