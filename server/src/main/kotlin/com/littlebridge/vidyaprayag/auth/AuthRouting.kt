package com.littlebridge.vidyaprayag.auth

import com.littlebridge.vidyaprayag.feature.auth.domain.model.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.UUID

// Mock user database
private val users = mutableListOf<SignupRequest>()

fun Route.authRouting() {
    route("/auth") {
        post("/signup") {
            try {
                val request = call.receive<SignupRequest>()
                
                if (users.any { it.contact == request.contact }) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("User already exists"))
                    return@post
                }
                
                users.add(request)
                
                val response = AuthResponse(
                    token = "mock-token-${UUID.randomUUID()}",
                    userId = UUID.randomUUID().toString(),
                    name = request.name,
                    role = request.role
                )
                
                call.respond(HttpStatusCode.Created, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request format"))
            }
        }

        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                
                val user = users.find { it.contact == request.contact && it.password == request.password && it.role == request.role }
                
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials or role"))
                    return@post
                }
                
                val response = AuthResponse(
                    token = "mock-token-${UUID.randomUUID()}",
                    userId = UUID.randomUUID().toString(),
                    name = user.name,
                    role = user.role
                )
                
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request format"))
            }
        }
    }
}
