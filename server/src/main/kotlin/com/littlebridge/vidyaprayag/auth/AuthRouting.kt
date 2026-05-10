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
import java.util.UUID

fun Route.authRouting() {
    route("/auth") {
        post("/signup") {
            try {
                val request = call.receive<SignupRequest>()
                
                val userExists = dbQuery {
                    UserTable.select(UserTable.id)
                        .where { UserTable.contact eq request.contact }
                        .count() > 0
                }

                if (userExists) {
                    call.respond(HttpStatusCode.Conflict, ErrorResponse("User already exists"))
                    return@post
                }
                
                val userId = dbQuery {
                    UserTable.insert {
                        it[name] = request.name
                        it[contact] = request.contact
                        it[password] = request.password
                        it[role] = request.role
                    } get UserTable.id
                }
                
                val response = AuthResponse(
                    token = "mock-token-${UUID.randomUUID()}",
                    userId = userId.toString(),
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
                
                val user = dbQuery {
                    UserTable.selectAll()
                        .where { 
                            (UserTable.contact eq request.contact) and 
                            (UserTable.password eq request.password) and 
                            (UserTable.role eq request.role) 
                        }
                        .map {
                            AuthResponse(
                                token = "mock-token-${UUID.randomUUID()}",
                                userId = it[UserTable.id].toString(),
                                name = it[UserTable.name],
                                role = it[UserTable.role]
                            )
                        }
                        .singleOrNull()
                }
                
                if (user == null) {
                    call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid credentials or role"))
                    return@post
                }
                
                call.respond(HttpStatusCode.OK, user)
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid request format"))
            }
        }
    }
}
