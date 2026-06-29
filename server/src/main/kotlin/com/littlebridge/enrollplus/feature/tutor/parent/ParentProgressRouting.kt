// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/parent/ParentProgressRouting.kt
package com.littlebridge.enrollplus.feature.tutor.parent

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Routes for Parent Progress (cross-role).
 *
 * Endpoints:
 *   GET /tutor/progress/{childId}/{subjectId} — get the parent progress card
 *
 * Authorization: parent must own the child.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §10 (Parent progress card)
 */
fun Route.parentProgressRouting() {

    get("/tutor/progress/{childId}/{subjectId}") {
        val uid = call.principalUserUuid() ?: return@get call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )
        val childIdStr = call.parameters["childId"]
            ?: return@get call.fail("childId required", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectIdStr = call.parameters["subjectId"]
            ?: return@get call.fail("subjectId required", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")
        val childId = runCatching { UUID.fromString(childIdStr) }.getOrNull()
            ?: return@get call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectId = runCatching { UUID.fromString(subjectIdStr) }.getOrNull()
            ?: return@get call.fail("Invalid subjectId", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")

        // Ownership check
        val ownsChild = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and
                (ChildrenTable.parentId eq uid) and
                (ChildrenTable.isActive eq true)
            }.any()
        }
        if (!ownsChild) return@get call.fail(
            "Child not found", HttpStatusCode.NotFound, "CHILD_NOT_FOUND"
        )

        val service = ParentProgressService()
        val card = service.buildProgressCard(childId, subjectId)

        call.ok(
            ProgressCardResponse(
                childId = card.childId.toString(),
                subjectId = card.subjectId.toString(),
                totalDoubtsResolved = card.totalDoubtsResolved,
                totalAnswersGiven = card.totalAnswersGiven,
                totalSessions = card.totalSessions,
                safetyFlags = card.safetyFlags,
                topics = card.topicProgress.map { t ->
                    TopicProgressResponse(
                        topicId = t.topicId.toString(),
                        currentMastery = t.currentMastery,
                        source = t.source,
                        attempts = t.attempts,
                        correct = t.correct,
                    )
                }
            ),
            "Progress card"
        )
    }
}

@Serializable
data class ProgressCardResponse(
    val childId: String,
    val subjectId: String,
    val totalDoubtsResolved: Int,
    val totalAnswersGiven: Int,
    val totalSessions: Int,
    val safetyFlags: Int,
    val topics: List<TopicProgressResponse>,
)

@Serializable
data class TopicProgressResponse(
    val topicId: String,
    val currentMastery: Double,
    val source: String,
    val attempts: Int,
    val correct: Int,
)
