package com.littlebridge.vidyaprayag.auth

import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.UserTable
import com.littlebridge.vidyaprayag.feature.auth.domain.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.security.MessageDigest
import java.util.UUID

fun hashPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.fold("") { str, it -> str + "%02x".format(it) }
}

fun Route.authRouting() {
    route("auth") {
        post("check-user") {
            try {
                val request = call.receive<CheckUserRequest>()
                val identifier = request.identifier.trim()
                
                println("DEBUG: [check-user] Received request for: '$identifier'")
                
                val user = dbQuery {
                    UserTable.selectAll()
                        .where { UserTable.contact eq identifier }
                        .singleOrNull()
                }

                val isEmail = identifier.contains("@")
                val flow = when {
                    user != null && isEmail -> AuthFlow.LOGIN_EMAIL
                    user == null && isEmail -> AuthFlow.SIGNUP_EMAIL
                    user != null && !isEmail -> AuthFlow.LOGIN_PHONE
                    else -> AuthFlow.SIGNUP_PHONE
                }

                println("DEBUG: [check-user] Identifier: '$identifier', User found: ${user != null}, Flow: $flow")
                call.respond(HttpStatusCode.OK, UserFlowResponse(flow))
            } catch (e: ContentTransformationException) {
                println("DEBUG: Content transformation error: ${e.message}")
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid data format: ${e.message}"))
            } catch (e: Exception) {
                println("DEBUG: Unexpected error in /check-user: ${e.message}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Server error: ${e.message}"))
            }
        }

        post("signup") {
            try {
                val request = call.receive<SignupRequest>()
                val identifier = request.contact.trim()
                
                println("DEBUG: [signup] Received request for: '$identifier'")
                
                val userExists = dbQuery {
                    UserTable.selectAll()
                        .where { UserTable.contact eq identifier }
                        .count() > 0
                }

                if (userExists) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("Account already exists"))
                    return@post
                }
                
                val hashedPw = request.password?.let { hashPassword(it) }

                val userId = dbQuery {
                    UserTable.insert {
                        it[name] = request.name
                        it[contact] = identifier
                        it[passwordHash] = hashedPw
                        it[role] = request.role
                        if (identifier.contains("@")) {
                            it[email] = identifier
                        } else {
                            it[phone] = identifier
                        }
                    } get UserTable.id
                }
                
                call.respond(HttpStatusCode.Created, AuthResponse(
                    token = "token-${UUID.randomUUID()}",
                    userId = userId.toString(),
                    name = request.name,
                    role = request.role
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Signup failed: ${e.message}"))
            }
        }

        post("login") {
            try {
                val request = call.receive<LoginRequest>()
                val identifier = request.contact.trim()
                
                println("DEBUG: [login] Received request for: '$identifier'")
                
                val user = dbQuery {
                    UserTable.selectAll()
                        .where { UserTable.contact eq identifier }
                        .singleOrNull()
                }

                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("User not found"))
                    return@post
                }

                // If email, check password
                if (identifier.contains("@")) {
                    val password = request.password ?: ""
                    val storedHash = user[UserTable.passwordHash]
                    
                    if (storedHash != hashPassword(password)) {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid password"))
                        return@post
                    }
                } else {
                    // If phone, check OTP (mocked to "123456" for now)
                    if (request.otp != "123456") {
                        call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid OTP code"))
                        return@post
                    }
                }

                call.respond(AuthResponse(
                    token = "token-${UUID.randomUUID()}",
                    userId = user[UserTable.id].toString(),
                    name = user[UserTable.name],
                    role = user[UserTable.role]
                ))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Login failed: ${e.message}"))
            }
        }

        post("send-otp") {
            // Mock OTP sending
            println("DEBUG: [send-otp] Mocking OTP for contact")
            call.respond(OtpResponse("OTP sent successfully to 123456 (Mock)"))
        }
    }
}
