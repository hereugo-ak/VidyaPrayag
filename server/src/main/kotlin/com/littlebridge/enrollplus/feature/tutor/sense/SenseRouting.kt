// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/sense/SenseRouting.kt
package com.littlebridge.enrollplus.feature.tutor.sense

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Routes for Tier 0 — Sense (LearnerBundleBuilder).
 *
 * Endpoints:
 *   GET /tutor/learner-bundle/{childId}/{subjectId} — deterministic bundle
 *
 * Authorization: parent must own the child (verified via ChildrenTable.parentId).
 *
 * SOLID: S (routes only — no business logic), D (service injected).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §5
 */
fun Route.senseRouting() {

    get("/tutor/learner-bundle/{childId}/{subjectId}") {
        val uid = call.principalUserUuid() ?: return@get call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )
        val childId = call.parameters["childId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@get call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")
        val subjectId = call.parameters["subjectId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@get call.fail("Invalid subjectId", HttpStatusCode.BadRequest, "BAD_SUBJECT_ID")

        // Ownership check: parent must own this child
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

        val builder = LearnerBundleBuilder()
        val bundle = builder.build(childId, subjectId) ?: return@get call.fail(
            "Could not build learner bundle — ensure child is linked to a school",
            HttpStatusCode.BadRequest, "BUNDLE_BUILD_FAILED"
        )

        call.ok(bundle, "Learner bundle")
    }
}
