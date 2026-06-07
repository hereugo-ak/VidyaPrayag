/*
 * File: TeacherProvisioningRouting.kt
 * Module: feature.school
 *
 * Fixes audit finding C (§3.1): there was previously NO code path anywhere that
 * created a `teacher` app_users row, so login could never find a teacher and
 * the entire /api/v1/teacher/* surface + teacher portal were dead in
 * production. This adds a school-admin-only endpoint that provisions a teacher
 * account scoped to the admin's own school.
 *
 * Endpoints (JWT + school-scoped via requireSchoolContext):
 *   POST /api/v1/school/teachers          create a teacher app_users row
 *   GET  /api/v1/school/teachers          list teachers in the admin's school
 *
 * A teacher created here can then log in via:
 *   - email + password   (if an email + initial_password were supplied), OR
 *   - phone + OTP         (if a phone identifier was supplied; OTP via /send-otp)
 */
package com.littlebridge.vidyaprayag.feature.school

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.requireSchoolContext
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
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
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
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

private fun isEmail(id: String) = id.contains("@")

fun Route.teacherProvisioningRouting() {
    authenticate("jwt") {
        route("/api/v1/school/teachers") {

            // ---- create a teacher account ----
            post {
                val ctx = call.requireSchoolContext() ?: return@post
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
                        it[isActive] = true
                        it[createdAt] = now
                        it[updatedAt] = now
                    }
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

            // ---- list teachers in the admin's school ----
            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val teachers = dbQuery {
                    AppUsersTable.selectAll()
                        .where { (AppUsersTable.schoolId eq ctx.schoolId) and (AppUsersTable.role eq "teacher") }
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
        }
    }
}
