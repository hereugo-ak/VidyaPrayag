package com.littlebridge.enrollplus.feature.healthcheck

import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val uptimeSeconds: Long,
)

private val startTime = System.currentTimeMillis()

fun Route.healthCheckRouting() {
    route("/api/v1/health") {
        get {
            call.respond(
                HealthResponse(
                    status = "ok",
                    timestamp = Instant.now().toString(),
                    uptimeSeconds = (System.currentTimeMillis() - startTime) / 1000,
                )
            )
        }
    }
}
