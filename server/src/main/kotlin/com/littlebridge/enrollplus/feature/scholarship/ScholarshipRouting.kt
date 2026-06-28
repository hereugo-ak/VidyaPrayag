/*
 * File: ScholarshipRouting.kt
 * Module: feature.scholarship
 *
 * API endpoints for the Scholarship Workflow feature (SCHOLARSHIP_WORKFLOW_SPEC.md §9).
 *
 *   Admin (school-scoped, JWT + requireSchoolContext):
 *     GET    /api/v1/school/scholarships                        — list schemes
 *     POST   /api/v1/school/scholarships                        — create scheme
 *     PUT    /api/v1/school/scholarships/{id}                   — update scheme
 *     DELETE /api/v1/school/scholarships/{id}                   — deactivate scheme
 *     GET    /api/v1/school/scholarship-applications            — list applications (filterable)
 *     GET    /api/v1/school/scholarship-applications/{id}       — application detail
 *     POST   /api/v1/school/scholarship-applications/{id}/approve   — approve application
 *     POST   /api/v1/school/scholarship-applications/{id}/reject    — reject application
 *     POST   /api/v1/school/scholarship-applications/{id}/disburse  — record disbursement
 *     GET    /api/v1/school/scholarship-renewals                — list renewals (filterable)
 *     POST   /api/v1/school/scholarship-renewals/{id}/approve   — approve renewal
 *     POST   /api/v1/school/scholarship-renewals/{id}/reject    — reject renewal
 *
 *   Parent (JWT):
 *     GET    /api/v1/parent/scholarships                        — available scholarships + gamification
 *     POST   /api/v1/parent/scholarships/apply                  — apply for scholarship
 *     GET    /api/v1/parent/scholarships/applications           — parent's applications
 *     POST   /api/v1/parent/scholarships/{id}/renew             — apply for renewal
 */
package com.littlebridge.enrollplus.feature.scholarship

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireSchoolContext
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import java.util.UUID

fun Route.scholarshipRouting() {
    authenticate("jwt") {

        // ── Admin: Scheme Management ──────────────────────────────────────
        route("/api/v1/school/scholarships") {

            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val activeOnly = call.request.queryParameters["all"]?.toBooleanStrictOrNull() != true
                val schemes = ScholarshipService().listSchemes(ctx.schoolId, activeOnly)
                call.ok(schemes, "Scholarship schemes (${schemes.size})")
            }

            post {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = runCatching { call.receive<CreateSchemeRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                if (req.title.isBlank()) {
                    call.fail("Title is required"); return@post
                }
                if (req.scholarshipType == "partial_waiver" && (req.waiverPercentage == null || req.waiverPercentage !in 0f..100f)) {
                    call.fail("Waiver percentage (0-100) is required for partial_waiver type"); return@post
                }
                val scheme = ScholarshipService().createScheme(ctx.schoolId, ctx.userId, req)
                call.ok(scheme, "Scholarship scheme created", HttpStatusCode.Created)
            }

            put("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid scheme id"); return@put }
                val req = runCatching { call.receive<UpdateSchemeRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@put }
                val updated = ScholarshipService().updateScheme(ctx.schoolId, id, req)
                if (updated) call.okMessage("Scheme updated") else call.fail("Scheme not found", HttpStatusCode.NotFound)
            }

            delete("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid scheme id"); return@delete }
                val deleted = ScholarshipService().deleteScheme(ctx.schoolId, id)
                if (deleted) call.okMessage("Scheme deactivated") else call.fail("Scheme not found", HttpStatusCode.NotFound)
            }
        }

        // ── Admin: Application Review ─────────────────────────────────────
        route("/api/v1/school/scholarship-applications") {

            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val status = call.request.queryParameters["status"]
                val applications = ScholarshipService().listApplications(ctx.schoolId, status)
                call.ok(applications, "Applications (${applications.size})")
            }

            get("/{id}") {
                val ctx = call.requireSchoolContext() ?: return@get
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid application id"); return@get }
                val app = ScholarshipService().getApplication(ctx.schoolId, id)
                if (app != null) call.ok(app, "Application detail")
                else call.fail("Application not found", HttpStatusCode.NotFound, "APPLICATION_NOT_FOUND")
            }

            post("/{id}/approve") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid application id"); return@post }
                val req = runCatching { call.receive<ApproveApplicationRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val result = ScholarshipService().approveApplication(ctx.schoolId, id, ctx.userId, req)
                if (result != null) call.ok(result, "Application approved")
                else call.fail("Application not found or not pending", HttpStatusCode.BadRequest, "NOT_FOUND_OR_NOT_PENDING")
            }

            post("/{id}/reject") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid application id"); return@post }
                val req = runCatching { call.receive<RejectApplicationRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val result = ScholarshipService().rejectApplication(ctx.schoolId, id, ctx.userId, req)
                if (result != null) call.ok(result, "Application rejected")
                else call.fail("Application not found or not pending", HttpStatusCode.BadRequest, "NOT_FOUND_OR_NOT_PENDING")
            }

            post("/{id}/disburse") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid application id"); return@post }
                val req = runCatching { call.receive<DisburseRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                if (req.amount <= 0) { call.fail("Amount must be positive"); return@post }
                if (req.reference.isBlank()) { call.fail("Reference is required"); return@post }
                val result = ScholarshipService().disburse(ctx.schoolId, id, ctx.userId, req)
                if (result != null) call.ok(result, "Disbursement recorded")
                else call.fail("Application not found or not approved", HttpStatusCode.BadRequest, "NOT_APPROVED")
            }
        }

        // ── Admin: Renewals ───────────────────────────────────────────────
        route("/api/v1/school/scholarship-renewals") {

            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val status = call.request.queryParameters["status"]
                val renewals = ScholarshipService().listRenewals(ctx.schoolId, status)
                call.ok(renewals, "Renewals (${renewals.size})")
            }

            post("/{id}/approve") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid renewal id"); return@post }
                val req = runCatching { call.receive<ApproveRenewalRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val result = ScholarshipService().approveRenewal(ctx.schoolId, id, ctx.userId, req)
                if (result != null) call.ok(result, "Renewal approved")
                else call.fail("Renewal not found or not pending", HttpStatusCode.BadRequest, "NOT_FOUND_OR_NOT_PENDING")
            }

            post("/{id}/reject") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val id = call.parameters["id"]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid renewal id"); return@post }
                val req = runCatching { call.receive<RejectApplicationRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val result = ScholarshipService().rejectRenewal(ctx.schoolId, id, ctx.userId, req)
                if (result != null) call.ok(result, "Renewal rejected")
                else call.fail("Renewal not found or not pending", HttpStatusCode.BadRequest, "NOT_FOUND_OR_NOT_PENDING")
            }
        }

        // ── Parent: Scholarships ──────────────────────────────────────────
        route("/api/v1/parent/scholarships") {

            get {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get
                }
                val data = ScholarshipService().getParentScholarships(uid)
                call.ok(data, "Scholarships data fetched")
            }

            post("/apply") {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post
                }
                val req = runCatching { call.receive<ApplyScholarshipRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                if (req.scholarshipId.isBlank()) { call.fail("Scholarship ID is required"); return@post }
                if (req.childId.isBlank()) { call.fail("Child ID is required"); return@post }
                val result = ScholarshipService().applyScholarship(uid, req)
                if (result != null) call.ok(result, "Application submitted", HttpStatusCode.Created)
                else call.fail("Could not apply — scholarship may be inactive, expired, or already applied for", HttpStatusCode.BadRequest, "APPLY_FAILED")
            }

            get("/applications") {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@get
                }
                val apps = ScholarshipService().getParentApplications(uid)
                call.ok(apps, "Applications (${apps.size})")
            }

            post("/{id}/renew") {
                val uid = call.principalUserUuid() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"); return@post
                }
                val req = runCatching { call.receive<ApplyRenewalRequest>() }.getOrNull()
                    ?: run { call.fail("Invalid request body"); return@post }
                val result = ScholarshipService().applyRenewal(uid, req)
                if (result != null) call.ok(result, "Renewal submitted", HttpStatusCode.Created)
                else call.fail("Could not renew — scholarship may not be renewable or original application not approved", HttpStatusCode.BadRequest, "RENEW_FAILED")
            }
        }
    }
}
