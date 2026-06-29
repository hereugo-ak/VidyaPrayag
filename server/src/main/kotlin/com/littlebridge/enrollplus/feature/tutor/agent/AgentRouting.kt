// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/agent/AgentRouting.kt
package com.littlebridge.enrollplus.feature.tutor.agent

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Routes for Tier 2 — Agent (TutorAgentService).
 *
 * Endpoints:
 *   POST /tutor/doubt — resolve a doubt with the agent loop
 *
 * Authorization: parent must own the child (verified via ChildrenTable.parentId).
 *
 * SOLID: S (routes only — no business logic), D (service injected).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.4
 */
fun Route.agentRouting() {

    post("/tutor/doubt") {
        val uid = call.principalUserUuid() ?: return@post call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )
        val body = call.receive<DoubtRequest>()

        val childId = runCatching { UUID.fromString(body.childId) }.getOrNull()
            ?: return@post call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectId = body.subjectId.takeIf { it.isNotBlank() }?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        }

        if (body.question.isBlank()) {
            return@post call.fail("Question is required", HttpStatusCode.BadRequest, "BAD_QUESTION")
        }

        // Ownership check: parent must own this child
        val ownsChild = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and
                (ChildrenTable.parentId eq uid) and
                (ChildrenTable.isActive eq true)
            }.any()
        }
        if (!ownsChild) return@post call.fail(
            "Child not found", HttpStatusCode.NotFound, "CHILD_NOT_FOUND"
        )

        // Resolve schoolId from child
        val schoolId = dbQuery {
            ChildrenTable.selectAll().where { ChildrenTable.id eq childId }
                .singleOrNull()?.get(ChildrenTable.schoolId)
        } ?: return@post call.fail(
            "Child has no school link", HttpStatusCode.BadRequest, "NO_SCHOOL"
        )

        val service = TutorAgentService()
        val result = service.resolveDoubt(
            schoolId = schoolId,
            childId = childId,
            subjectId = subjectId,
            question = body.question,
        )

        call.ok(
            DoubtResponse(
                sessionId = result.sessionId?.toString(),
                turn = result.turn,
                modelUsed = result.modelUsed,
                providerUsed = result.providerUsed,
                grounded = result.grounded,
                safetyFlag = result.safetyFlag,
            ),
            if (result.modelUsed) "Doubt resolved" else "Doubt resolved (deterministic fallback)"
        )
    }
}

@Serializable
data class DoubtRequest(
    val childId: String,
    val subjectId: String,
    val question: String,
)

@Serializable
data class DoubtResponse(
    val sessionId: String?,
    val turn: TutorTurn,
    val modelUsed: Boolean,
    val providerUsed: String?,
    val grounded: Boolean,
    val safetyFlag: String?,
)
