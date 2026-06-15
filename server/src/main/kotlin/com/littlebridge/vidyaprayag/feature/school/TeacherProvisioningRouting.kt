/*
 * File: TeacherProvisioningRouting.kt
 * Module: feature.school
 *
 * Fixes audit finding C (§3.1): there was previously NO code path anywhere that
 * created a `teacher` app_users row, so login could never find a teacher and
 * the entire /api/v1/teacher/... surface + teacher portal were dead in
 * production. This adds a school-admin-only endpoint that provisions a teacher
 * account scoped to the admin's own school.
 *
 * Endpoints (JWT + school-scoped via requireSchoolContext):
 *   POST   /api/v1/school/teachers                     create a teacher app_users row
 *   GET    /api/v1/school/teachers                     list active teachers in the admin's school
 *   DELETE /api/v1/school/teachers/{id}                deactivate (soft-delete) a teacher (RA-22)
 *   POST   /api/v1/school/teachers/{id}/reset-password reissue an initial password (RA-32)
 *
 * A teacher created here can then log in via:
 *   - email + password   (if an email + initial_password were supplied), OR
 *   - phone + OTP         (if a phone identifier was supplied; OTP via /send-otp)
 */
package com.littlebridge.vidyaprayag.feature.school

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.okMessage
import com.littlebridge.vidyaprayag.core.requireSchoolAdmin
import com.littlebridge.vidyaprayag.core.requireSchoolContext
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.DeviceTokensTable
import com.littlebridge.vidyaprayag.db.FacultyTable
import com.littlebridge.vidyaprayag.db.NotificationsTable
import com.littlebridge.vidyaprayag.db.TeacherSubjectAssignmentsTable
import com.littlebridge.vidyaprayag.db.UserSessionsTable
import com.littlebridge.vidyaprayag.feature.auth.hashPassword
import com.littlebridge.vidyaprayag.feature.auth.normaliseIdentifier
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID

@Serializable
data class CreateTeacherDto(
    val name: String,
    val identifier: String,                               // email OR phone
    @SerialName("initial_password") val initialPassword: String? = null
)

@Serializable
data class TeacherAccountDto(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val role: String,
    @SerialName("school_id") val schoolId: String
)

@Serializable
data class TeacherListResponse(val teachers: List<TeacherAccountDto>)

/**
 * RA-32: response for a credential reset. Carries the freshly-generated
 * plaintext password EXACTLY ONCE so the admin can hand it to the teacher;
 * it is never stored in plaintext server-side (only the hash is persisted).
 */
@Serializable
data class TeacherCredentialDto(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("initial_password") val initialPassword: String
)

private fun isEmail(id: String) = id.contains("@")

/**
 * RA-32: generate a human-readable but high-entropy initial password
 * (no ambiguous chars like 0/O/1/l/I) using [SecureRandom]. ~12 chars from a
 * 56-symbol alphabet ≈ 69 bits of entropy — strong enough for a one-time
 * credential the teacher is expected to change after first login.
 */
private val resetPwRng = SecureRandom()
private fun generateInitialPassword(length: Int = 12): String {
    val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#%"
    return buildString(length) {
        repeat(length) { append(alphabet[resetPwRng.nextInt(alphabet.length)]) }
    }
}

/**
 * BUGFIX (admin-created teachers were invisible in Supabase `faculty`):
 * onboarding mirrors every provisioned teacher into the `faculty` roster via the
 * SAME `external_id = "U-<userId>"` convention (OnboardingRouting.ensureFacultyRow),
 * but this admin-facing POST /api/v1/school/teachers endpoint only ever wrote an
 * `app_users` row. As a result:
 *   - the teacher never appeared in the `faculty` table the admin was looking at, and
 *   - SchoolAnalyticsRouting (which joins attendance to faculty on external_id) could
 *     never surface that teacher's accountability/efficiency metrics.
 *
 * This helper makes the admin create path mirror onboarding exactly. It is
 * idempotent on external_id, so re-provisioning is safe. MUST run inside dbQuery {}.
 */
private fun ensureFacultyRow(schoolId: UUID, userId: UUID, name: String) {
    val externalId = "U-$userId"
    val exists = FacultyTable.selectAll()
        .where { FacultyTable.externalId eq externalId }
        .firstOrNull()
    if (exists != null) return
    FacultyTable.insert {
        it[FacultyTable.schoolId] = schoolId
        it[FacultyTable.externalId] = externalId
        it[FacultyTable.userId] = userId
        it[FacultyTable.name] = name.trim()
        it[isActive] = true
        it[createdAt] = Instant.now()
    }
}

fun Route.teacherProvisioningRouting() {
    authenticate("jwt") {
        route("/api/v1/school/teachers") {

            // ---- create a teacher account (privileged: RA-39) ----
            post {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = runCatching { call.receive<CreateTeacherDto>() }.getOrNull()
                    ?: run { call.fail("Invalid body"); return@post }

                val id = normaliseIdentifier(req.identifier)
                if (req.name.isBlank() || id.isBlank()) {
                    call.fail("name and identifier are required", HttpStatusCode.BadRequest)
                    return@post
                }
                if (isEmail(id) && req.initialPassword.isNullOrBlank()) {
                    call.fail(
                        "initial_password is required when provisioning a teacher by email",
                        HttpStatusCode.BadRequest
                    )
                    return@post
                }

                val existing = dbQuery {
                    AppUsersTable.selectAll()
                        .where { (AppUsersTable.phone eq id) or (AppUsersTable.email eq id) }
                        .firstOrNull()
                }
                if (existing != null) {
                    call.fail("An account with this identifier already exists", HttpStatusCode.Conflict, "USER_EXISTS")
                    return@post
                }

                val newId = UUID.randomUUID()
                val now = Instant.now()
                dbQuery {
                    AppUsersTable.insert {
                        it[AppUsersTable.id] = newId
                        it[fullName] = req.name.trim()
                        it[role] = "teacher"
                        it[schoolId] = ctx.schoolId
                        if (isEmail(id)) {
                            it[email] = id
                            it[passwordHash] = hashPassword(req.initialPassword!!)
                            it[isEmailVerified] = true
                        } else {
                            it[phone] = id
                            it[isPhoneVerified] = true
                        }
                        it[profileCompleted] = false
                        // RA-54: provisioned teachers must change their generated
                        // initial password on first login. This flag is the
                        // server-side gate signal NavGraphV2 reads; the
                        // POST /auth/change-password endpoint clears it.
                        it[mustChangePassword] = true
                        it[isActive] = true
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
                    // BUGFIX: mirror the new teacher into the `faculty` roster so it
                    // shows up in Supabase and is visible to SchoolAnalyticsRouting
                    // (which joins attendance → faculty on external_id = "U-<userId>").
                    ensureFacultyRow(ctx.schoolId, newId, req.name)
                }

                call.created(
                    TeacherAccountDto(
                        id = newId.toString(),
                        name = req.name.trim(),
                        email = if (isEmail(id)) id else null,
                        phone = if (!isEmail(id)) id else null,
                        role = "teacher",
                        schoolId = ctx.schoolId.toString()
                    ),
                    message = "Teacher account created"
                )
            }

            // ---- list ACTIVE teachers in the admin's school ----
            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val teachers = dbQuery {
                    AppUsersTable.selectAll()
                        .where {
                            (AppUsersTable.schoolId eq ctx.schoolId) and
                                (AppUsersTable.role eq "teacher") and
                                (AppUsersTable.isActive eq true)
                        }
                        .map {
                            TeacherAccountDto(
                                id = it[AppUsersTable.id].value.toString(),
                                name = it[AppUsersTable.fullName],
                                email = it[AppUsersTable.email],
                                phone = it[AppUsersTable.phone],
                                role = it[AppUsersTable.role],
                                schoolId = ctx.schoolId.toString()
                            )
                        }
                }
                call.ok(TeacherListResponse(teachers), message = "Teachers fetched successfully")
            }

            // ---- HARD-delete a teacher (RA-22) ----
            // FIX (admin remove leaves Supabase rows behind): previously this
            // only flipped is_active=false, so the app_users row (and the
            // teacher's sessions/tokens/assignments) stayed in Supabase forever.
            // Admin removal now performs a real DELETE inside one transaction:
            //   - sessions + device tokens + class/subject assignments are
            //     purged here (no DB-level FK exists for them);
            //   - authored content (assessments / homework / syllabus / marks)
            //     is PRESERVED — the DB FKs are ON DELETE SET NULL, so school
            //     academic history survives with the author cleared;
            //   - teacher_periods rows cascade away via their FK.
            // IDOR-safe: every statement is constrained to ctx.schoolId, so an
            // admin can only delete teachers belonging to their OWN school.
            delete("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete   // privileged: RA-39
                val teacherId = call.parameters["id"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("A valid teacher id is required", HttpStatusCode.BadRequest, "BAD_TEACHER_ID"); return@delete }

                val now = Instant.now()
                val deleted = dbQuery {
                    // Confirm the teacher exists in THIS school first.
                    val row = AppUsersTable.selectAll()
                        .where {
                            (AppUsersTable.id eq teacherId) and
                                (AppUsersTable.schoolId eq ctx.schoolId) and
                                (AppUsersTable.role eq "teacher")
                        }
                        .firstOrNull() ?: return@dbQuery false

                    // Kill the teacher's live sessions FIRST (immediate lockout even
                    // if a later statement fails), then purge auth artefacts.
                    UserSessionsTable.update({ UserSessionsTable.userId eq teacherId }) {
                        it[revokedAt] = now
                    }
                    UserSessionsTable.deleteWhere { UserSessionsTable.userId eq teacherId }
                    DeviceTokensTable.deleteWhere { DeviceTokensTable.userId eq teacherId }
                    // The teacher's personal notification inbox is meaningless once
                    // the account is gone — purge it so no orphan rows linger.
                    NotificationsTable.deleteWhere { NotificationsTable.userId eq teacherId }

                    // Remove the teacher's class/subject assignments (no DB FK — must
                    // be cleaned manually or they linger as orphans in Supabase).
                    TeacherSubjectAssignmentsTable.deleteWhere {
                        (TeacherSubjectAssignmentsTable.schoolId eq ctx.schoolId) and
                            (TeacherSubjectAssignmentsTable.teacherId eq teacherId)
                    }

                    // BUGFIX: remove the mirrored `faculty` row too. It is keyed by
                    // external_id = "U-<userId>" (set on create / onboarding); without
                    // this the deleted teacher would linger forever in the faculty
                    // roster and keep showing up in analytics. School-scoped for IDOR.
                    FacultyTable.deleteWhere {
                        (FacultyTable.schoolId eq ctx.schoolId) and
                            (FacultyTable.externalId eq "U-$teacherId")
                    }

                    // Finally the account row itself. Tables with real FKs to
                    // app_users (assessments / homework / syllabus_units → SET NULL,
                    // teacher_periods → CASCADE) are handled by Postgres.
                    AppUsersTable.deleteWhere {
                        (AppUsersTable.id eq teacherId) and
                            (AppUsersTable.schoolId eq ctx.schoolId) and
                            (AppUsersTable.role eq "teacher")
                    }
                    true
                }

                if (!deleted) {
                    call.fail("Teacher not found in your school", HttpStatusCode.NotFound, "TEACHER_NOT_FOUND")
                    return@delete
                }
                call.okMessage("Teacher removed")
            }

            // ---- reissue a teacher's initial password (RA-32) ----
            // Recovers the "lost credential" case: once the admin dismisses the
            // create-teacher dialog the initial password is gone (only its hash
            // is stored). This generates a NEW secure password, persists only the
            // hash, revokes the teacher's live sessions, and returns the plaintext
            // ONCE so the admin can hand it over.
            //
            // IDOR-safe: every lookup/update is constrained to ctx.schoolId, so an
            // admin can only reset teachers in their OWN school. Email teachers
            // only — phone teachers authenticate via OTP (no password to reset).
            post("/{id}/reset-password") {
                val ctx = call.requireSchoolAdmin() ?: return@post   // privileged: RA-39
                val teacherId = call.parameters["id"]
                    ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("A valid teacher id is required", HttpStatusCode.BadRequest, "BAD_TEACHER_ID"); return@post }

                val now = Instant.now()
                val newPassword = generateInitialPassword()

                val result = dbQuery {
                    val row = AppUsersTable.selectAll()
                        .where {
                            (AppUsersTable.id eq teacherId) and
                                (AppUsersTable.schoolId eq ctx.schoolId) and
                                (AppUsersTable.role eq "teacher") and
                                (AppUsersTable.isActive eq true)
                        }
                        .firstOrNull() ?: return@dbQuery null

                    val email = row[AppUsersTable.email]
                    if (email.isNullOrBlank()) {
                        // Phone-only teacher: there is no password to reset.
                        return@dbQuery TeacherCredentialDto("", row[AppUsersTable.fullName], "", "")
                    }

                    AppUsersTable.update({ AppUsersTable.id eq teacherId }) {
                        it[passwordHash] = hashPassword(newPassword)
                        it[isEmailVerified] = true
                        it[updatedAt] = now
                    }
                    // Revoke live sessions so the old credential can't keep a foothold.
                    UserSessionsTable.update({ UserSessionsTable.userId eq teacherId }) {
                        it[revokedAt] = now
                    }

                    TeacherCredentialDto(
                        id = teacherId.toString(),
                        name = row[AppUsersTable.fullName],
                        email = email,
                        initialPassword = newPassword
                    )
                }

                when {
                    result == null ->
                        call.fail("Teacher not found in your school", HttpStatusCode.NotFound, "TEACHER_NOT_FOUND")
                    result.id.isBlank() ->
                        call.fail(
                            "This teacher signs in with phone + OTP, so there is no password to reset. Ask them to log in with their phone number.",
                            HttpStatusCode.Conflict,
                            "TEACHER_USES_OTP"
                        )
                    else ->
                        call.ok(result, message = "New initial password issued")
                }
            }
        }
    }
}
