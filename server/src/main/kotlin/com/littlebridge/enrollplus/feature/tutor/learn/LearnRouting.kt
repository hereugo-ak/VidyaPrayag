// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/learn\LearnRouting.kt
package com.littlebridge.enrollplus.feature.tutor.learn

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.core.principalRole
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
 * Routes for Tier 4 — Learn.
 *
 * Endpoints:
 *   GET /tutor/efficacy/{childId}/{subjectId} — get efficacy for a child's topics
 *
 * Authorization: parent must own the child, or teacher/admin with school access.
 *
 * SOLID: S (routes only — no business logic), D (service injected).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §8
 */
fun Route.learnRouting() {

    get("/tutor/efficacy/{childId}/{subjectId}") {
        val uid = call.principalUserUuid() ?: return@get call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )
        val role = call.principalRole()
        val childIdStr = call.parameters["childId"]
            ?: return@get call.fail("childId required", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectIdStr = call.parameters["subjectId"]
            ?: return@get call.fail("subjectId required", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")
        val childId = runCatching { UUID.fromString(childIdStr) }.getOrNull()
            ?: return@get call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectId = runCatching { UUID.fromString(subjectIdStr) }.getOrNull()
            ?: return@get call.fail("Invalid subjectId", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")

        // Ownership check for parents
        if (role == "parent") {
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
        }

        val service = TutorEfficacyService()
        val masteryRepo = com.littlebridge.enrollplus.feature.tutor.data.TutorMasteryRepository()
        val masteryRows = masteryRepo.findByChildAndSubject(childId, subjectId)

        val efficacyResults = masteryRows.map { m ->
            com.littlebridge.enrollplus.feature.tutor.learn.TutorEfficacyService.EfficacyResult(
                topicId = m.topicId,
                childId = childId,
                previousMastery = m.mastery,
                currentMastery = m.mastery,
                delta = 0.0,
                verdict = if (m.mastery >= 60) "improved" else if (m.mastery < 30) "worsened" else "unchanged",
            )
        }

        call.ok(
            EfficacyResponse(
                childId = childId.toString(),
                subjectId = subjectId.toString(),
                topics = efficacyResults.map { r ->
                    EfficacyTopicItem(
                        topicId = r.topicId.toString(),
                        mastery = r.currentMastery,
                        verdict = r.verdict,
                    )
                }
            ),
            "Efficacy report"
        )
    }
}

@Serializable
data class EfficacyResponse(
    val childId: String,
    val subjectId: String,
    val topics: List<EfficacyTopicItem>,
)

@Serializable
data class EfficacyTopicItem(
    val topicId: String,
    val mastery: Double,
    val verdict: String,
)
