/*
 * File: AlumniRouting.kt
 * Module: feature/alumni
 *
 * Purpose:
 *   Route definitions for the Alumni Management feature. Three route groups:
 *     1. /api/v1/school/alumni/...  — admin endpoints (JWT + requireSchoolContext/Admin)
 *     2. /api/v1/alumni/...         — alumni self-service (JWT + requireAlumniContext)
 *     3. /api/v1/alumni/register    — public self-registration (no auth)
 *     4. /api/v1/alumni/schools/search — public school search (no auth)
 *
 * Spec ref: ALUMNI_MANAGEMENT_SPEC.md §7 (API Contracts) + §11 (Routing)
 */
package com.littlebridge.enrollplus.feature.alumni

import com.littlebridge.enrollplus.core.*
import com.littlebridge.enrollplus.feature.media.SupabaseStorage
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.origin
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.*
import java.util.UUID

private val alumniService = AlumniService()
private val receiptService = AlumniReceiptService()
private const val MAX_PHOTO_BYTES = 5L * 1024 * 1024 // 5 MB for profile photos

// Simple in-memory rate limiter for public registration (B10): 3 per IP per hour
private val registrationAttempts = mutableMapOf<String, MutableList<Long>>()
private const val REG_RATE_LIMIT = 3
private const val REG_RATE_WINDOW_MS = 60L * 60 * 1000 // 1 hour

private fun checkRegistrationRateLimit(ip: String): Boolean {
    val now = System.currentTimeMillis()
    val attempts = registrationAttempts.getOrPut(ip) { mutableListOf() }
    attempts.removeAll { now - it > REG_RATE_WINDOW_MS }
    if (attempts.size >= REG_RATE_LIMIT) return false
    attempts.add(now)
    return true
}

fun Route.alumniRouting() {

    // ── Public: Alumni self-registration + school search ─────────
    route("/api/v1/alumni") {

        // Public — no auth required
        // Rate limited: 3 registration attempts per IP per hour (B10)
        post("/register") {
            val ip = call.request.origin.remoteHost
            if (!checkRegistrationRateLimit(ip)) {
                call.fail("Too many registration attempts. Please try again later.", HttpStatusCode.TooManyRequests, "RATE_LIMITED")
                return@post
            }
            try {
                val req = call.receive<AlumniRegisterDto>()
                if (req.schoolCode.isNullOrBlank() && req.schoolName.isNullOrBlank()) {
                    call.fail("Either schoolCode or schoolName is required", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                if (req.name.isBlank()) {
                    call.fail("Name is required", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                val result = alumniService.registerAlumni(req)
                call.ok(result, "Registration ${result.status}")
            } catch (e: IllegalArgumentException) {
                call.fail(e.message ?: "Registration failed", HttpStatusCode.BadRequest, "REGISTRATION_ERROR")
            }
        }

        // Public — school search for registration autocomplete
        get("/schools/search") {
            val q = call.parameters["q"] ?: ""
            if (q.isBlank()) {
                call.ok(emptyList<SchoolSearchResultDto>(), "Provide a search query")
                return@get
            }
            val results = alumniService.searchSchools(q)
            call.ok(results)
        }

        // ── Alumni self-service (JWT + requireAlumniContext) ──────
        authenticate("jwt") {

            // Profile
            get("/profile") {
                val ctx = call.requireAlumniContext() ?: return@get
                val profile = alumniService.getAlumniProfile(ctx.userId)
                    ?: run { call.fail("Profile not found", HttpStatusCode.NotFound, "NOT_FOUND"); return@get }
                call.ok(profile)
            }

            patch("/profile") {
                val ctx = call.requireAlumniContext() ?: return@patch
                val req = call.receive<UpdateAlumniDto>()
                val updated = alumniService.updateAlumniProfile(ctx.userId, req)
                    ?: run { call.fail("Update failed", HttpStatusCode.NotFound, "NOT_FOUND"); return@patch }
                call.ok(updated, "Profile updated")
            }

            patch("/privacy") {
                val ctx = call.requireAlumniContext() ?: return@patch
                val req = call.receive<AlumniPrivacyDto>()
                val updated = alumniService.updatePrivacy(ctx.userId, req)
                    ?: run { call.fail("Update failed", HttpStatusCode.NotFound, "NOT_FOUND"); return@patch }
                call.ok(updated, "Privacy settings updated")
            }

            // Mentorship
            post("/mentor-volunteer") {
                val ctx = call.requireAlumniContext() ?: return@post
                val expertise = call.receiveParameters()["expertise"]
                    ?: run { call.fail("expertise is required", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@post }
                val updated = alumniService.volunteerAsMentor(ctx.userId, expertise)
                    ?: run { call.fail("Failed to volunteer", HttpStatusCode.NotFound, "NOT_FOUND"); return@post }
                call.ok(updated, "You are now registered as a mentor")
            }

            get("/mentorship-requests") {
                val ctx = call.requireAlumniContext() ?: return@get
                val requests = alumniService.getMentorshipRequests(ctx.userId)
                call.ok(requests)
            }

            patch("/mentorship-requests/{id}") {
                val ctx = call.requireAlumniContext() ?: return@patch
                val requestId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid request id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val action = call.receiveParameters()["action"]
                    ?: run { call.fail("action is required (accept|decline)", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val result = alumniService.respondToMentorshipRequest(ctx.userId, requestId, action)
                    ?: run { call.fail("Request not found or already responded", HttpStatusCode.NotFound, "NOT_FOUND"); return@patch }
                call.ok(result, "Request ${result.status}")
            }

            get("/mentorships") {
                val ctx = call.requireAlumniContext() ?: return@get
                val mentorships = alumniService.getOwnMentorships(ctx.userId)
                call.ok(mentorships)
            }

            // Career history
            get("/career-history") {
                val ctx = call.requireAlumniContext() ?: return@get
                val history = alumniService.getCareerHistory(ctx.userId)
                call.ok(history)
            }

            post("/career-history") {
                val ctx = call.requireAlumniContext() ?: return@post
                val req = call.receive<CreateCareerHistoryDto>()
                if (req.jobTitle.isBlank() || req.company.isBlank()) {
                    call.fail("jobTitle and company are required", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                val entry = alumniService.addCareerHistory(ctx.userId, req)
                call.ok(entry, "Career history added")
            }

            patch("/career-history/{id}") {
                val ctx = call.requireAlumniContext() ?: return@patch
                val entryId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid entry id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val req = call.receive<CreateCareerHistoryDto>()
                if (req.jobTitle.isBlank() || req.company.isBlank()) {
                    call.fail("jobTitle and company are required", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@patch
                }
                val updated = alumniService.updateCareerHistory(ctx.userId, entryId, req)
                    ?: run { call.fail("Career history entry not found", HttpStatusCode.NotFound, "NOT_FOUND"); return@patch }
                call.ok(updated, "Career history updated")
            }

            // Donations
            get("/donations") {
                val ctx = call.requireAlumniContext() ?: return@get
                val donations = alumniService.getOwnDonations(ctx.userId)
                call.ok(donations)
            }

            get("/campaigns") {
                val ctx = call.requireAlumniContext() ?: return@get
                // Return active campaigns for alumni's school
                val campaigns = alumniService.listCampaigns(ctx.schoolId)
                    .filter { it.status == "active" }
                call.ok(campaigns)
            }

            // Directory (privacy-filtered)
            get("/directory") {
                val ctx = call.requireAlumniContext() ?: return@get
                val year = call.parameters["year"]?.toIntOrNull()
                val profession = call.parameters["profession"]
                val city = call.parameters["city"]
                val q = call.parameters["q"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20
                val results = alumniService.searchDirectory(ctx.userId, year, profession, city, q, page, limit)
                call.ok(results)
            }

            // Photo upload (C7 — alumni can't use school media upload)
            post("/photo") {
                val ctx = call.requireAlumniContext() ?: return@post
                val multipart = call.receiveMultipart()
                var fileBytes: ByteArray? = null
                var contentType = "image/jpeg"

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FileItem -> {
                            val original = part.originalFileName ?: "photo.jpg"
                            contentType = part.contentType?.toString() ?: contentType
                            if (original.endsWith(".png", ignoreCase = true)) contentType = "image/png"
                            fileBytes = part.streamProvider().readBytes()
                        }
                        else -> {}
                    }
                    part.dispose()
                }

                val bytes = fileBytes
                if (bytes == null) {
                    call.fail("No file uploaded", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                if (bytes.size > MAX_PHOTO_BYTES) {
                    call.fail("Photo must be under 5 MB", HttpStatusCode.BadRequest, "FILE_TOO_LARGE")
                    return@post
                }

                val result = SupabaseStorage.upload(ctx.schoolId, "PROFILE", bytes, contentType)
                if (result == null) {
                    call.fail("Upload failed", HttpStatusCode.BadGateway, "UPLOAD_FAILED")
                    return@post
                }

                // Update alumni photo URL
                val updated = alumniService.updateAlumniProfile(ctx.userId, UpdateAlumniDto(photoUrl = result.url))
                call.ok(updated ?: mapOf("photoUrl" to result.url), "Photo uploaded")
            }
        }
    }

    // ── Admin endpoints (JWT + requireSchoolContext/Admin) ───────
    authenticate("jwt") {
        route("/api/v1/school/alumni") {

            // Alumni CRUD + lifecycle
            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val year = call.parameters["year"]?.toIntOrNull()
                val profession = call.parameters["profession"]
                val city = call.parameters["city"]
                val company = call.parameters["company"]
                val industry = call.parameters["industry"]
                val q = call.parameters["q"]
                val page = call.parameters["page"]?.toIntOrNull() ?: 1
                val limit = call.parameters["limit"]?.toIntOrNull() ?: 20
                val results = alumniService.listAlumni(ctx.schoolId, year, profession, city, company, industry, q, page, limit)
                call.ok(results)
            }

            post {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<CreateAlumniDto>()
                if (req.name.isBlank()) {
                    call.fail("name is required", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                val alumni = alumniService.createAlumni(ctx.schoolId, req, ctx.userId)
                call.ok(alumni, "Alumni created", HttpStatusCode.Created)
            }

            get("/{id}") {
                val ctx = call.requireSchoolContext() ?: return@get
                val alumniId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                val alumni = alumniService.getAlumni(ctx.schoolId, alumniId)
                    ?: run { call.fail("Alumni not found", HttpStatusCode.NotFound, "NOT_FOUND"); return@get }
                call.ok(alumni)
            }

            patch("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@patch
                val alumniId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val req = call.receive<UpdateAlumniDto>()
                val updated = alumniService.updateAlumni(ctx.schoolId, alumniId, req)
                    ?: run { call.fail("Alumni not found", HttpStatusCode.NotFound, "NOT_FOUND"); return@patch }
                call.ok(updated, "Alumni updated")
            }

            patch("/{id}/deactivate") {
                val ctx = call.requireSchoolAdmin() ?: return@patch
                val alumniId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val success = alumniService.deactivateAlumni(ctx.schoolId, alumniId)
                if (success) call.okMessage("Alumni deactivated")
                else call.fail("Alumni not found", HttpStatusCode.NotFound, "NOT_FOUND")
            }

            post("/graduate") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<GraduateStudentsDto>()
                if (req.studentIds.isEmpty()) {
                    call.fail("At least one student ID is required", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                val results = alumniService.graduateStudents(ctx.schoolId, req, ctx.userId)
                call.ok(results, "Graduated ${results.size} student(s)")
            }

            post("/import") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<List<CreateAlumniDto>>()
                val result = alumniService.bulkImport(ctx.schoolId, req)
                call.ok(result, "Imported ${result.imported}, failed ${result.failed}")
            }

            // Verification queue
            get("/pending") {
                val ctx = call.requireSchoolContext() ?: return@get
                val pending = alumniService.listPendingVerifications(ctx.schoolId)
                call.ok(pending)
            }

            patch("/{id}/verify") {
                val ctx = call.requireSchoolAdmin() ?: return@patch
                val alumniId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val action = call.receiveParameters()["action"]
                    ?: run { call.fail("action is required (approve|decline)", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val result = alumniService.verifyAlumni(ctx.schoolId, alumniId, action, ctx.userId)
                    ?: run { call.fail("Alumni not found", HttpStatusCode.NotFound, "NOT_FOUND"); return@patch }
                call.ok(result, "Verification ${result.verificationStatus}")
            }

            // Featured alumni
            patch("/{id}/feature") {
                val ctx = call.requireSchoolAdmin() ?: return@patch
                val alumniId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val result = alumniService.toggleFeatured(ctx.schoolId, alumniId)
                    ?: run { call.fail("Alumni not found", HttpStatusCode.NotFound, "NOT_FOUND"); return@patch }
                call.ok(result, if (result.isFeatured) "Featured" else "Unfeatured")
            }

            // ── Campaigns ───────────────────────────────────────
            get("/campaigns") {
                val ctx = call.requireSchoolContext() ?: return@get
                val campaigns = alumniService.listCampaigns(ctx.schoolId)
                call.ok(campaigns)
            }

            post("/campaigns") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<CreateCampaignDto>()
                if (req.title.isBlank()) {
                    call.fail("title is required", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                val campaign = alumniService.createCampaign(ctx.schoolId, req)
                call.ok(campaign, "Campaign created", HttpStatusCode.Created)
            }

            get("/campaigns/{id}") {
                val ctx = call.requireSchoolContext() ?: return@get
                val campaignId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                val campaign = alumniService.getCampaign(ctx.schoolId, campaignId)
                    ?: run { call.fail("Campaign not found", HttpStatusCode.NotFound, "NOT_FOUND"); return@get }
                call.ok(campaign)
            }

            patch("/campaigns/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@patch
                val campaignId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val status = call.receiveParameters()["status"]
                    ?: run { call.fail("status is required (active|closed|paused)", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val result = alumniService.updateCampaign(ctx.schoolId, campaignId, status)
                    ?: run { call.fail("Campaign not found", HttpStatusCode.NotFound, "NOT_FOUND"); return@patch }
                call.ok(result, "Campaign updated")
            }

            // ── Donations ───────────────────────────────────────
            get("/donations") {
                val ctx = call.requireSchoolContext() ?: return@get
                val campaignId = call.parameters["campaign_id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val alumniId = call.parameters["alumni_id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                val donations = alumniService.listDonations(ctx.schoolId, campaignId, alumniId)
                call.ok(donations)
            }

            get("/{id}/donations") {
                val ctx = call.requireSchoolContext() ?: return@get
                val alumniId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                val donations = alumniService.getDonationsForAlumni(ctx.schoolId, alumniId)
                call.ok(donations)
            }

            post("/donations") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<CreateDonationDto>()
                if (req.amount <= 0) {
                    call.fail("amount must be positive", HttpStatusCode.BadRequest, "VALIDATION_ERROR")
                    return@post
                }
                val donation = alumniService.createDonation(ctx.schoolId, req)
                call.ok(donation, "Donation logged", HttpStatusCode.Created)
            }

            // 80G receipt PDF download
            get("/donations/{id}/receipt") {
                val ctx = call.requireSchoolContext() ?: return@get
                val donationId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@get }
                val pdf = receiptService.generateReceipt(ctx.schoolId, donationId)
                if (pdf == null) {
                    call.fail("Receipt not available (donation may not be 80G eligible)", HttpStatusCode.NotFound, "NOT_FOUND")
                    return@get
                }
                call.respondBytes(pdf, ContentType.Application.Pdf)
            }

            // Form 10BD CSV export (annual filing)
            get("/donations/80g/form10bd") {
                val ctx = call.requireSchoolContext() ?: return@get
                val year = call.parameters["year"]?.toIntOrNull() ?: java.time.LocalDate.now().year
                val csv = receiptService.exportForm10BD(ctx.schoolId, year)
                call.respondBytes(csv, ContentType.parse("text/csv"))
            }

            // ── Mentorship ───────────────────────────────────────
            get("/mentorships") {
                val ctx = call.requireSchoolContext() ?: return@get
                val mentorships = alumniService.listMentorships(ctx.schoolId)
                call.ok(mentorships)
            }

            post("/mentorships") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<CreateMentorshipDto>()
                val mentorship = alumniService.createMentorship(ctx.schoolId, req)
                call.ok(mentorship, "Mentorship created", HttpStatusCode.Created)
            }

            patch("/mentorships/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@patch
                val mentorshipId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val success = alumniService.endMentorship(ctx.schoolId, mentorshipId)
                if (success) call.okMessage("Mentorship ended")
                else call.fail("Mentorship not found", HttpStatusCode.NotFound, "NOT_FOUND")
            }

            get("/mentorship-requests") {
                val ctx = call.requireSchoolContext() ?: return@get
                val requests = alumniService.listMentorshipRequests(ctx.schoolId)
                call.ok(requests)
            }

            patch("/mentorship-requests/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@patch
                val requestId = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid request id", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val action = call.receiveParameters()["action"]
                    ?: run { call.fail("action is required (cancel|force-accept)", HttpStatusCode.BadRequest, "VALIDATION_ERROR"); return@patch }
                val result = alumniService.adminRespondToMentorshipRequest(ctx.schoolId, requestId, action)
                    ?: run { call.fail("Request not found, already responded, or invalid action", HttpStatusCode.NotFound, "NOT_FOUND"); return@patch }
                call.ok(result, "Request ${result.status}")
            }

            // ── Mentorship Settings ─────────────────────────────
            get("/mentorship/settings") {
                val ctx = call.requireSchoolContext() ?: return@get
                val settings = alumniService.getMentorshipSettings(ctx.schoolId)
                call.ok(settings)
            }

            put("/mentorship/settings") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val req = call.receive<MentorshipSettingsDto>()
                val settings = alumniService.updateMentorshipSettings(ctx.schoolId, req)
                call.ok(settings, "Mentorship settings updated")
            }

            // ── Analytics ───────────────────────────────────────
            get("/analytics/overview") {
                val ctx = call.requireSchoolContext() ?: return@get
                val analytics = alumniService.getAnalyticsOverview(ctx.schoolId)
                call.ok(analytics)
            }

            get("/analytics/engagement") {
                val ctx = call.requireSchoolContext() ?: return@get
                val metrics = alumniService.getEngagementMetrics(ctx.schoolId)
                call.ok(metrics)
            }

            get("/analytics/donations") {
                val ctx = call.requireSchoolContext() ?: return@get
                val metrics = alumniService.getDonationAnalytics(ctx.schoolId)
                call.ok(metrics)
            }

            get("/analytics/career") {
                val ctx = call.requireSchoolContext() ?: return@get
                val metrics = alumniService.getCareerAnalytics(ctx.schoolId)
                call.ok(metrics)
            }
        }
    }
}
