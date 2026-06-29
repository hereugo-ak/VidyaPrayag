// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/sense/SenseRouting.kt
package com.littlebridge.enrollplus.feature.tutor.sense

import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.principalUserUuid
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.SchoolClassesTable
import com.littlebridge.enrollplus.db.SchoolSubjectsTable
import com.littlebridge.enrollplus.db.StudentsTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Routes for Tier 0 — Sense (LearnerBundleBuilder).
 *
 * Endpoints:
 *   GET /tutor/subjects/{childId}                  — list child's class subjects
 *   GET /tutor/learner-bundle/{childId}/{subjectId} — deterministic bundle
 *
 * Authorization: parent must own the child (verified via ChildrenTable.parentId).
 */
fun Route.senseRouting() {

    get("/tutor/subjects/{childId}") {
        val uid = call.principalUserUuid() ?: return@get call.fail(
            "Invalid token", HttpStatusCode.Unauthorized, "UNAUTHORIZED"
        )
        val childId = call.parameters["childId"]?.let {
            runCatching { UUID.fromString(it) }.getOrNull()
        } ?: return@get call.fail("Invalid childId", HttpStatusCode.BadRequest, "BAD_CHILD_ID")

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

        // Resolve schoolId + classId
        val schoolId = dbQuery {
            ChildrenTable.selectAll().where { ChildrenTable.id eq childId }
                .singleOrNull()?.get(ChildrenTable.schoolId)
        } ?: return@get call.fail(
            "Child has no school link", HttpStatusCode.BadRequest, "NO_SCHOOL"
        )

        val studentCode = dbQuery {
            ChildrenTable.selectAll().where { ChildrenTable.id eq childId }
                .singleOrNull()?.get(ChildrenTable.studentCode)
        }

        val className = dbQuery {
            studentCode?.let { code ->
                StudentsTable.selectAll().where { StudentsTable.studentCode eq code }
                    .singleOrNull()?.get(StudentsTable.className)
            } ?: ChildrenTable.selectAll().where { ChildrenTable.id eq childId }
                .singleOrNull()?.get(ChildrenTable.currentGrade)
        } ?: ""

        val classId = dbQuery {
            SchoolClassesTable.selectAll().where {
                (SchoolClassesTable.schoolId eq schoolId) and
                (SchoolClassesTable.code eq className)
            }.singleOrNull()?.get(SchoolClassesTable.id)?.value
        }

        val subjects = dbQuery {
            if (classId == null) return@dbQuery emptyList()
            SchoolSubjectsTable.selectAll().where {
                SchoolSubjectsTable.classId eq classId
            }.map { row ->
                SubjectItem(
                    subjectId = row[SchoolSubjectsTable.id].value.toString(),
                    subjectName = row[SchoolSubjectsTable.subName],
                    subjectCode = row[SchoolSubjectsTable.subCode],
                )
            }
        }

        call.ok(subjects, "Subjects (${subjects.size})")
    }

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

@Serializable
data class SubjectItem(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
)
