// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/triage/TriageRouting.kt
package com.littlebridge.enrollplus.feature.tutor.triage

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
 * Routes for Tier 1 — Triage.
 *
 * Endpoints:
 *   POST /tutor/triage — classify an inbound event
 *
 * Authorization: parent must own the child (verified via ChildrenTable.parentId).
 *
 * SOLID: S (routes only — no business logic), D (service injected).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.1
 */
fun Route.triageRouting() {

    post("/tutor/triage") {
        val uid = call.principalUserUuid() ?: return@post call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )
        val body = call.receive<TriageRequest>()

        val childId = runCatching { UUID.fromString(body.childId) }.getOrNull()
            ?: return@post call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectId = runCatching { UUID.fromString(body.subjectId) }.getOrNull()
            ?: return@post call.fail("Invalid subjectId", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")

        if (body.question.isBlank()) {
            return@post call.fail("Question is required", HttpStatusCode.BadRequest, "BAD_QUESTION")
        }

        // Ownership check
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

        val service = TutorTriageService()
        val result = service.classify(
            schoolId = schoolId,
            childId = childId,
            subjectId = subjectId,
            question = body.question,
        )

        call.ok(
            TriageResponse(
                intent = result.intent,
                onSyllabus = result.onSyllabus,
                knownMisconception = result.knownMisconception,
                misconceptionType = result.misconceptionType,
                skipAgent = result.skipAgent,
                skipReason = result.skipReason,
                modelUsed = result.modelUsed,
                providerUsed = result.providerUsed,
            ),
            "Triage complete"
        )
    }
}

@Serializable
data class TriageRequest(
    val childId: String,
    val subjectId: String,
    val question: String,
)

@Serializable
data class TriageResponse(
    val intent: String,
    val onSyllabus: Boolean,
    val knownMisconception: Boolean,
    val misconceptionType: String?,
    val skipAgent: Boolean,
    val skipReason: String?,
    val modelUsed: Boolean,
    val providerUsed: String?,
)
