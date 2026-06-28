/*
 * File: BrandingRouting.kt
 * Module: feature.branding
 *
 * API endpoints for the School Branding Kit (SCHOOL_BRANDING_KIT_SPEC.md §9).
 *
 *   Admin (JWT + requireSchoolAdmin):
 *     GET    /api/v1/school/branding                    — get own school branding
 *     PATCH  /api/v1/school/branding                    — update colors/assets
 *     POST   /api/v1/school/branding/reset              — reset to defaults
 *     POST   /api/v1/school/branding/subdomain          — set custom subdomain
 *     DELETE /api/v1/school/branding/subdomain          — remove subdomain
 *     GET    /api/v1/school/branding/subdomain/check     — check availability
 *
 *   Public (no auth):
 *     GET    /api/v1/branding/{schoolId}                — public branding read
 *     GET    /api/v1/branding/subdomain/{subdomain}      — resolve subdomain
 */
package com.littlebridge.enrollplus.feature.branding

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireSchoolContext
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import com.littlebridge.enrollplus.feature.branding.BrandingService.Companion.SUBDOMAIN_REGEX
import java.util.UUID

private val subdomainRegex = SUBDOMAIN_REGEX

fun Route.brandingRouting() {
    // ── Public endpoints (no auth) ──────────────────────────────────────
    route("/api/v1/branding") {

        get("/{schoolId}") {
            val schoolId = call.parameters["schoolId"]?.let {
                runCatching { UUID.fromString(it) }.getOrNull()
            } ?: run { call.fail("Invalid school id"); return@get }

            val branding = BrandingService().getPublicBranding(schoolId)
            if (branding != null) call.ok(branding, "School branding")
            else call.fail("School not found", HttpStatusCode.NotFound, "BRANDING_NOT_FOUND")
        }

        get("/subdomain/{subdomain}") {
            val subdomain = call.parameters["subdomain"]
                ?: run { call.fail("Subdomain is required"); return@get }

            val result = BrandingService().resolveSubdomain(subdomain)
            if (result != null) call.ok(result, "Subdomain resolved")
            else call.fail("School not found for this subdomain", HttpStatusCode.NotFound, "SUBDOMAIN_NOT_FOUND")
        }
    }

    // ── Admin endpoints (JWT + school admin) ────────────────────────────
    authenticate("jwt") {
        route("/api/v1/school/branding") {

            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val branding = BrandingService().getBranding(ctx.schoolId)
                call.ok(branding, "School branding")
            }

            patch {
                val ctx = call.requireSchoolAdmin() ?: return@patch
                val req = runCatching { call.receive<UpdateBrandingRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@patch }

                try {
                    val updated = BrandingService().updateBranding(ctx.schoolId, req)
                    call.ok(updated, "Branding updated")
                } catch (e: IllegalArgumentException) {
                    call.fail(e.message ?: "Invalid color format", HttpStatusCode.BadRequest, "INVALID_COLOR")
                }
            }

            post("/reset") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val result = BrandingService().resetBranding(ctx.schoolId)
                call.ok(result, "Branding reset to defaults")
            }

            // ── Subdomain management ───────────────────────────────────
            get("/subdomain/check") {
                val ctx = call.requireSchoolAdmin() ?: return@get
                val subdomain = call.request.queryParameters["subdomain"]
                    ?: run { call.fail("Subdomain parameter is required"); return@get }

                if (!subdomain.matches(subdomainRegex)) {
                    call.fail(
                        "Invalid subdomain. Use lowercase letters, numbers, and hyphens (4-32 chars).",
                        HttpStatusCode.BadRequest,
                        "INVALID_SUBDOMAIN",
                    )
                    return@get
                }

                val available = BrandingService().checkSubdomainAvailable(ctx.schoolId, subdomain)
                call.ok(mapOf("available" to available), if (available) "Subdomain available" else "Subdomain already taken")
            }

            post("/subdomain") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = runCatching { call.receive<SubdomainRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }

                if (!req.subdomain.matches(subdomainRegex)) {
                    call.fail(
                        "Invalid subdomain. Use lowercase letters, numbers, and hyphens (4-32 chars).",
                        HttpStatusCode.BadRequest,
                        "INVALID_SUBDOMAIN",
                    )
                    return@post
                }

                try {
                    val result = BrandingService().updateSubdomain(ctx.schoolId, req.subdomain)
                    call.ok(result, "Subdomain assigned")
                } catch (e: IllegalStateException) {
                    call.fail(e.message ?: "Subdomain already taken", HttpStatusCode.Conflict, "SUBDOMAIN_TAKEN")
                }
            }

            delete("/subdomain") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val removed = BrandingService().removeSubdomain(ctx.schoolId)
                if (removed) call.ok(mapOf("removed" to true), "Subdomain removed")
                else call.fail("No subdomain set", HttpStatusCode.NotFound, "SUBDOMAIN_NOT_FOUND")
            }
        }
    }
}
