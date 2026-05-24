/*
 * File: Application.kt
 * Module: server entry point
 *
 * Wires up all Ktor plugins and feature routes for the VidyaPrayag backend.
 *
 * Plugins installed (in order):
 *   1. IgnoreTrailingSlash       — /foo and /foo/ map to the same handler
 *   2. CORS                      — open during dev so the Compose web target
 *                                   and your phone can hit the local server
 *   3. CallLogging               — basic stdout logging of every request
 *   4. ContentNegotiation        — kotlinx.serialization JSON
 *   5. Authentication ("jwt")    — JWT bearer via core/SecurityModule
 *   6. StatusPages               — uniform error envelopes
 *
 * Routes mounted:
 *   - GET  /                              — liveness greeting
 *   - landingRouting()                    — /api/v1/content/landing
 *   - appStatusRouting()                  — /api/v1/config/app-status
 *   - authRouting()                       — /api/v1/auth/... (+ legacy /auth/...)
 *   - supportRouting()                    — /api/v1/content/support
 *   - userDetailsRouting()                — /api/v1/user/details
 *   - userProfileRouting()                — /api/v1/user/profile[…]
 *   - onboardingRouting()                 — /api/v1/onboarding/...
 *   - announcementRouting()               — /api/v1/school/announcements[…]
 *   - admissionRouting()                  — /api/v1/admissions/enquiries[…]
 *   - schoolRouting()                     — /api/v1/school/{analytics,calendar,holidays,attendance/daily}
 *   - parentOnboardingRouting()           — /api/v1/parent/onboarding/{metadata, child-info, preference-options}
 *   - parentDashboardRouting()            — /api/v1/parent/dashboard
 *   - trackProgressRouting()              — /api/v1/parent/track-progress
 *   - parentFeesRouting()                 — /api/v1/parent/fees
 *
 * On boot:
 *   DatabaseFactory.init() creates/migrates all tables and seeds CMS + demo data.
 *
 * Manual DevOps steps (one-time):
 *   - Set DATABASE_URL in .env or env (postgres://… or jdbc:postgresql://…)
 *   - Set JWT_SECRET to a strong random value in production
 */
package com.littlebridge.vidyaprayag

import com.littlebridge.vidyaprayag.core.configureErrorHandling
import com.littlebridge.vidyaprayag.core.configureJwt
import com.littlebridge.vidyaprayag.db.DatabaseFactory
import com.littlebridge.vidyaprayag.feature.admissions.admissionRouting
import com.littlebridge.vidyaprayag.feature.announcements.announcementRouting
import com.littlebridge.vidyaprayag.feature.auth.authRouting
import com.littlebridge.vidyaprayag.feature.auth.otpAdminRouting
import com.littlebridge.vidyaprayag.feature.config.appStatusRouting
import com.littlebridge.vidyaprayag.feature.content.landingRouting
import com.littlebridge.vidyaprayag.feature.content.supportRouting
import com.littlebridge.vidyaprayag.feature.onboarding.onboardingRouting
import com.littlebridge.vidyaprayag.feature.parent.parentDashboardRouting
import com.littlebridge.vidyaprayag.feature.parent.parentFeesRouting
import com.littlebridge.vidyaprayag.feature.parent.parentOnboardingRouting
import com.littlebridge.vidyaprayag.feature.parent.trackProgressRouting
import com.littlebridge.vidyaprayag.feature.school.schoolRouting
import com.littlebridge.vidyaprayag.feature.user.parentRouting
import com.littlebridge.vidyaprayag.feature.user.userDetailsRouting
import com.littlebridge.vidyaprayag.feature.user.userProfileRouting
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun main() {
    DatabaseFactory.init()
    val port = System.getenv("PORT")?.toIntOrNull() ?: SERVER_PORT
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(IgnoreTrailingSlash)

    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowHeader("App-Version")
        allowHeader("Platform")
        allowHeader("Device-Id")
        allowHeader("Accept-Language")
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }

    install(CallLogging)

    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
        })
    }

    install(Authentication) { configureJwt() }

    install(StatusPages) { configureErrorHandling() }

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()} — VidyaPrayag API v1 is live")
        }

        // Public
        landingRouting()
        appStatusRouting()
        authRouting()
        supportRouting()

        // Ops-only — entire route group is unmounted (404) unless
        // OTP_ADMIN_TOKEN env var is set. See feature/auth/OtpAdminRouting.kt.
        // Safe to leave wired up on Render free tier — no overhead when
        // the token env is empty.
        otpAdminRouting()

        // Authenticated
        userDetailsRouting()
        userProfileRouting()
        parentRouting()
        onboardingRouting()
        announcementRouting()
        admissionRouting()
        schoolRouting()

        // Parent ecosystem (parent_api_spec.artifact.md)
        parentOnboardingRouting()    // /api/v1/parent/onboarding/{metadata, child-info, preference-options}
        parentDashboardRouting()     // /api/v1/parent/dashboard
        trackProgressRouting()       // /api/v1/parent/track-progress
        parentFeesRouting()          // /api/v1/parent/fees
    }
}
