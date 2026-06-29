// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/act/ActRouting.kt
package com.littlebridge.enrollplus.feature.tutor.act

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.feature.tutor.agent.PracticeQuestion
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Routes for Tier 3 — Act.
 *
 * Endpoints:
 *   POST /tutor/practice/grade — auto-grade a practice answer + FSRS update
 *   GET  /tutor/plan/{childId} — get the FSRS due-review plan for tonight
 *
 * Authorization: parent must own the child (verified via ChildrenTable.parentId).
 *
 * SOLID: S (routes only — no business logic), D (service injected).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §7
 */
fun Route.actRouting() {

    post("/tutor/practice/grade") {
        val uid = call.principalUserUuid() ?: return@post call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )
        val body = call.receive<GradeRequest>()

        val childId = runCatching { UUID.fromString(body.childId) }.getOrNull()
            ?: return@post call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectId = runCatching { UUID.fromString(body.subjectId) }.getOrNull()
            ?: return@post call.fail("Invalid subjectId", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")

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

        // Resolve schoolId
        val schoolId = dbQuery {
            ChildrenTable.selectAll().where { ChildrenTable.id eq childId }
                .singleOrNull()?.get(ChildrenTable.schoolId)
        } ?: return@post call.fail(
            "Child has no school link", HttpStatusCode.BadRequest, "NO_SCHOOL"
        )

        val question = PracticeQuestion(
            questionId = body.questionId,
            stem = body.stem,
            options = body.options,
            answerKey = body.answerKey,
            topicId = body.topicId,
            difficulty = body.difficulty,
        )

        val service = TutorActService()
        val result = service.autoGrade(schoolId, childId, subjectId, question, body.childAnswer)

        call.ok(
            GradeResponse(
                correct = result.correct,
                gradePct = result.gradePct,
                feedback = result.feedback,
            ),
            if (result.correct) "Correct answer" else "Incorrect — review needed"
        )
    }

    get("/tutor/plan/{childId}") {
        val uid = call.principalUserUuid() ?: return@get call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )
        val childIdStr = call.parameters["childId"]
            ?: return@get call.fail("childId required", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val childId = runCatching { UUID.fromString(childIdStr) }.getOrNull()
            ?: return@get call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")

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

        val scheduler = FsrsScheduler()
        val dueReviews = scheduler.getDueReviews(childId, limit = 20)

        call.ok(
            dueReviews.map { r ->
                PlanItem(
                    topicId = r.topicId.toString(),
                    dueAt = r.dueAt.toString(),
                    stability = r.stability,
                    difficulty = r.difficulty,
                    reps = r.reps,
                    lapses = r.lapses,
                )
            },
            "Review plan (${dueReviews.size} items due)"
        )
    }
}

@Serializable
data class GradeRequest(
    val childId: String,
    val subjectId: String,
    val questionId: String,
    val stem: String,
    val options: List<String>? = null,
    val answerKey: String,
    val topicId: String,
    val difficulty: String,
    val childAnswer: String,
)

@Serializable
data class GradeResponse(
    val correct: Boolean,
    val gradePct: Int,
    val feedback: String,
)

@Serializable
data class PlanItem(
    val topicId: String,
    val dueAt: String,
    val stability: Double,
    val difficulty: Double,
    val reps: Int,
    val lapses: Int,
)
