/*
 * File: AuthRouting.kt   (feature.auth — v2, spec-compliant)
 * Module: feature.auth
 *
 * Endpoints implemented (per vidya_prayag_api_spec.artifact.md §Module: User
 * Authentication):
 *   POST /api/v1/auth/check-user       (public)
 *   POST /api/v1/auth/signup           (public — issues JWT + refresh token)
 *   POST /api/v1/auth/login            (public — issues JWT)
 *   POST /api/v1/auth/send-otp         (public — MOCK, always returns 123456)
 *
 * Backward-compat aliases (the existing shared/.../AuthApi.kt still posts to
 * /auth/... without the /api/v1 prefix):
 *   POST /auth/check-user, /auth/signup, /auth/login, /auth/send-otp
 *
 * Spec-vs-legacy field mapping:
 *   - spec uses { "identifier": "..." }
 *   - legacy uses { "contact": "..." }
 *   The DTOs below accept BOTH (identifier preferred, contact as fallback).
 *
 * Hashing: SHA-256 (same as legacy). For production swap to BCrypt; this is
 *          adequate for an MVP / hackathon-grade backend.
 *
 * NOTE for FRONTEND TEAM:
 *   The legacy `/auth/...` paths are kept alive so the current app keeps working
 *   while you migrate AuthApi.kt to the new `/api/v1/auth/...` paths. Once
 *   migrated, you can delete the legacy block at the bottom of this file.
 */
package com.littlebridge.vidyaprayag.feature.auth

import com.littlebridge.vidyaprayag.core.JwtConfig
import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.UserTable
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.security.MessageDigest

// ---------- DTOs (accept both `identifier` and legacy `contact`) ----------

@Serializable
data class CheckUserDto(
    val identifier: String? = null,
    val contact: String? = null,
    val role: String? = null
) {
    fun id(): String = (identifier ?: contact ?: "").trim()
}

@Serializable
data class SignupDto(
    val name: String,
    val identifier: String? = null,
    val contact: String? = null,
    val password: String? = null,
    val otp: String? = null,
    val role: String,
    @SerialName("device_info") val deviceInfo: DeviceInfo? = null
) {
    fun id(): String = (identifier ?: contact ?: "").trim()
}

@Serializable
data class DeviceInfo(
    @SerialName("device_id") val deviceId: String? = null,
    val platform: String? = null
)

@Serializable
data class LoginDto(
    val identifier: String? = null,
    val contact: String? = null,
    val password: String? = null,
    val otp: String? = null,
    val role: String
) {
    fun id(): String = (identifier ?: contact ?: "").trim()
}

@Serializable
data class OtpDto(
    val identifier: String? = null,
    val contact: String? = null
) {
    fun id(): String = (identifier ?: contact ?: "").trim()
}

// ---------- Response DTOs ----------

@Serializable
data class CheckUserResponse(
    @SerialName("is_new_user") val isNewUser: Boolean,
    @SerialName("auth_method_required") val authMethodRequired: String, // PASSWORD | OTP | SOCIAL
    val message: String
)

@Serializable
data class AuthTokenResponse(
    val token: String,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("user_id") val userId: String,
    val name: String,
    val role: String,
    @SerialName("profile_completed") val profileCompleted: Boolean
)

@Serializable
data class OtpResponse(val message: String)

// ---------- helpers ----------

internal fun hashPassword(password: String): String {
    val md = MessageDigest.getInstance("SHA-256")
    return md.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }
}

private fun isEmail(id: String) = id.contains("@")

// ---------- Routing ----------

fun Route.authRouting() {
    val v1 = "/api/v1/auth"
    val legacy = "/auth"

    listOf(v1, legacy).forEach { base ->
        route(base) {

            // -------- check-user --------
            post("/check-user") {
                val req = call.receive<CheckUserDto>()
                val id = req.id()
                if (id.isBlank()) {
                    call.fail("Identifier is required"); return@post
                }
                val user = dbQuery {
                    UserTable.selectAll().where { UserTable.contact eq id }.singleOrNull()
                }
                val isNew = user == null
                val method = if (isEmail(id)) "PASSWORD" else "OTP"
                call.ok(
                    CheckUserResponse(
                        isNewUser = isNew,
                        authMethodRequired = method,
                        message = if (isNew) "User does not exist. Proceed to signup."
                                  else "User found. Please continue with $method."
                    ),
                    message = "Check completed"
                )
            }

            // -------- signup --------
            post("/signup") {
                val req = call.receive<SignupDto>()
                val id = req.id()
                if (id.isBlank() || req.name.isBlank() || req.role.isBlank()) {
                    call.fail("name, identifier and role are required"); return@post
                }
                val exists = dbQuery {
                    UserTable.selectAll().where { UserTable.contact eq id }.count() > 0L
                }
                if (exists) { call.fail("Account already exists", HttpStatusCode.Conflict); return@post }

                val pwHash = req.password?.takeIf { it.isNotBlank() }?.let { hashPassword(it) }
                val newId = dbQuery {
                    UserTable.insert {
                        it[name] = req.name
                        it[contact] = id
                        it[passwordHash] = pwHash
                        it[role] = req.role
                        if (isEmail(id)) it[email] = id else it[phone] = id
                        it[isEmailVerified] = isEmail(id) && pwHash != null
                        it[isPhoneVerified] = !isEmail(id) && req.otp == "123456"
                        it[profileCompleted] = false
                    } get UserTable.id
                }

                val token = JwtConfig.issueToken(newId.toString(), req.role, req.name)
                val refresh = JwtConfig.issueRefreshToken(newId.toString())
                dbQuery {
                    UserTable.update({ UserTable.id eq newId }) { it[refreshToken] = refresh }
                }
                call.created(
                    AuthTokenResponse(
                        token = token, refreshToken = refresh,
                        userId = newId.toString(), name = req.name, role = req.role,
                        profileCompleted = false
                    ),
                    message = "Account created successfully"
                )
            }

            // -------- login --------
            //
            // Accepts credentials from EITHER:
            //   (a) JSON body  (preferred, per spec):
            //       { "identifier": "...", "password": "...", "role": "..." }
            //       { "identifier": "+91...", "otp": "123456", "role": "PARENT" }
            //   (b) Query/form parameters as a fallback so that quick manual tests
            //       like  POST /api/v1/auth/login?identifier=...&otp=123456  also
            //       work (this is what tripped up the Postman re-test).
            // The body wins when both are present.
            post("/login") {
                // Try to read JSON body; if missing/invalid, fall back to empty DTO.
                val bodyReq: LoginDto? = runCatching { call.receive<LoginDto>() }.getOrNull()
                val q = call.request.queryParameters

                val req = LoginDto(
                    identifier = bodyReq?.identifier ?: q["identifier"],
                    contact    = bodyReq?.contact    ?: q["contact"],
                    password   = bodyReq?.password   ?: q["password"],
                    otp        = bodyReq?.otp        ?: q["otp"],
                    role       = bodyReq?.role       ?: q["role"] ?: ""
                )
                val id = req.id()
                if (id.isBlank()) {
                    call.fail(
                        "Identifier is required. Send JSON body {identifier,password|otp,role} " +
                        "or use ?identifier=...&password=... query params."
                    ); return@post
                }

                val row = dbQuery {
                    UserTable.selectAll().where { UserTable.contact eq id }.singleOrNull()
                } ?: run {
                    call.fail("User not found", HttpStatusCode.Unauthorized); return@post
                }

                // Email → password check; Phone → OTP check (mock "123456").
                if (isEmail(id)) {
                    val stored = row[UserTable.passwordHash]
                    if (stored == null || stored != hashPassword(req.password.orEmpty())) {
                        call.fail("Invalid password", HttpStatusCode.Unauthorized); return@post
                    }
                } else {
                    if (req.otp != "123456") {
                        call.fail("Invalid OTP code", HttpStatusCode.Unauthorized); return@post
                    }
                }

                val userId = row[UserTable.id].toString()
                val name = row[UserTable.name]
                val role = row[UserTable.role]
                val token = JwtConfig.issueToken(userId, role, name)
                val refresh = JwtConfig.issueRefreshToken(userId)
                dbQuery {
                    UserTable.update({ UserTable.id eq row[UserTable.id] }) { it[refreshToken] = refresh }
                }
                call.ok(
                    AuthTokenResponse(
                        token = token, refreshToken = refresh,
                        userId = userId, name = name, role = role,
                        profileCompleted = row[UserTable.profileCompleted]
                    ),
                    message = "Login successful"
                )
            }

            // -------- send-otp (mock) --------
            post("/send-otp") {
                // Read body if present, ignore otherwise.
                runCatching { call.receive<OtpDto>() }
                call.ok(OtpResponse("OTP sent successfully. Use 123456 in dev."), "OTP issued")
            }
        }
    }
}
