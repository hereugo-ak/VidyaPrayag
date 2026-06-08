/*
 * File: ParentLinkRouting.kt
 * Module: feature.parent
 *
 * Endpoints (Module: Link Your Child wizard — report §5.3, SWEEP-A):
 *   GET  /api/v1/parent/schools/search?q=     (JWT — step 2: match a school by name)
 *   POST /api/v1/parent/link-child            (JWT — step 3: link a child by roll/admission no.)
 *
 * Replaces the MockV2-driven ParentLinkChildScreenV2 wizard with real data:
 *   - step 2 searches active schools by name substring (SchoolsTable);
 *   - step 3 resolves the school's canonical student (StudentsTable) by
 *     (school_id, roll_number) and links the parent to that child by creating
 *     / updating a ChildrenTable row carrying the resolved student_code.
 *
 * Spec ref: parent_api_spec.artifact.md §Module: Child Onboarding (linking).
 */
package com.littlebridge.vidyaprayag.feature.parent

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.ChildrenTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.SchoolsTable
import com.littlebridge.vidyaprayag.db.StudentsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

// ---------- DTOs ----------

@Serializable
data class SchoolMatchDto(
    val id: String,
    val name: String,
    val board: String,
    val city: String,
    @SerialName("logo_url") val logoUrl: String? = null
)

@Serializable
data class SchoolSearchResponse(
    val schools: List<SchoolMatchDto>
)

@Serializable
data class LinkChildRequest(
    @SerialName("school_id") val schoolId: String,
    @SerialName("roll_number") val rollNumber: String,
    // Optional — parent's preferred contact name/language captured in step 1.
    @SerialName("parent_name") val parentName: String? = null
)

@Serializable
data class LinkedChildDto(
    @SerialName("child_id") val childId: String,
    @SerialName("child_name") val childName: String,
    @SerialName("class_name") val className: String,
    val roll: String,
    @SerialName("school_name") val schoolName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null
)

// ---------- routing ----------

fun Route.parentLinkRouting() {
    authenticate("jwt") {
        route("/api/v1/parent") {

            // ---- GET /schools/search?q= ----
            // Step 2 of the link wizard: match active schools by name substring.
            get("/schools/search") {
                call.principalUserId() ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val query = call.request.queryParameters["q"]?.trim().orEmpty()
                val limit = (call.request.queryParameters["limit"]?.toIntOrNull() ?: 10).coerceIn(1, 50)

                val matches = dbQuery {
                    SchoolsTable.selectAll()
                        .where {
                            val active = SchoolsTable.isActive eq true
                            if (query.isBlank()) {
                                active
                            } else {
                                active and (SchoolsTable.name.lowerCase() like "%${query.lowercase()}%")
                            }
                        }
                        .orderBy(SchoolsTable.name, SortOrder.ASC)
                        .limit(limit)
                        .map { row ->
                            SchoolMatchDto(
                                id = row[SchoolsTable.id].value.toString(),
                                name = row[SchoolsTable.name],
                                board = row[SchoolsTable.board],
                                city = row[SchoolsTable.city],
                                logoUrl = row[SchoolsTable.logoUrl]
                            )
                        }
                }
                call.ok(SchoolSearchResponse(schools = matches), message = "Found ${matches.size} school(s)")
            }

            // ---- POST /link-child ----
            // Step 3: resolve the canonical student by (school_id, roll_number)
            // and link this parent to that child.
            post("/link-child") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                }
                val req = runCatching { call.receive<LinkChildRequest>() }.getOrNull() ?: run {
                    call.fail("Invalid request body"); return@post
                }
                val schoolUuid = runCatching { UUID.fromString(req.schoolId) }.getOrNull() ?: run {
                    call.fail("Invalid school_id"); return@post
                }
                if (req.rollNumber.isBlank()) {
                    call.fail("roll_number is required"); return@post
                }

                val result = dbQuery {
                    // 1) Verify school exists & is active.
                    val schoolRow = SchoolsTable.selectAll()
                        .where { (SchoolsTable.id eq schoolUuid) and (SchoolsTable.isActive eq true) }
                        .singleOrNull() ?: return@dbQuery LinkResult.SchoolNotFound

                    // 2) Resolve the canonical student by (school, roll).
                    val studentRow = StudentsTable.selectAll()
                        .where {
                            (StudentsTable.schoolId eq schoolUuid) and
                                (StudentsTable.rollNumber eq req.rollNumber) and
                                (StudentsTable.isActive eq true)
                        }
                        .singleOrNull() ?: return@dbQuery LinkResult.StudentNotFound

                    val studentCodeVal = studentRow[StudentsTable.studentCode]
                    val childNameVal = studentRow[StudentsTable.fullName]
                    val classNameVal = studentRow[StudentsTable.className]
                    val photoVal = studentRow[StudentsTable.profilePhotoUrl]
                    val schoolNameVal = schoolRow[SchoolsTable.name]
                    val now = Instant.now()

                    // 3) Upsert the parent⇄child link (idempotent on student_code).
                    val existing = ChildrenTable.selectAll()
                        .where {
                            (ChildrenTable.parentId eq uid) and (ChildrenTable.studentCode eq studentCodeVal)
                        }
                        .singleOrNull()

                    val childId: UUID = if (existing != null) {
                        val id = existing[ChildrenTable.id].value
                        ChildrenTable.update({ ChildrenTable.id eq id }) {
                            it[schoolId] = schoolUuid
                            it[childName] = childNameVal
                            it[currentGrade] = classNameVal
                            it[isActive] = true
                            it[updatedAt] = now
                        }
                        id
                    } else {
                        val newId = UUID.randomUUID()
                        ChildrenTable.insert {
                            it[id] = newId
                            it[parentId] = uid
                            it[schoolId] = schoolUuid
                            it[studentCode] = studentCodeVal
                            it[childName] = childNameVal
                            it[currentGrade] = classNameVal
                            it[interests] = "[]"
                            it[overallProgress] = 0.0
                            it[currentLevel] = 1
                            it[attendanceStatus] = "PRESENT"
                            it[isActive] = true
                            it[createdAt] = now
                            it[updatedAt] = now
                        }
                        newId
                    }

                    // RA-20: a parent who has linked a child has finished onboarding.
                    // Flip profile_completed=true so the next /login routes them straight
                    // to the dashboard instead of dumping them back into the link wizard.
                    AppUsersTable.update({ AppUsersTable.id eq uid }) {
                        it[profileCompleted] = true
                    }

                    LinkResult.Linked(
                        LinkedChildDto(
                            childId = childId.toString(),
                            childName = childNameVal,
                            className = classNameVal,
                            roll = req.rollNumber,
                            schoolName = schoolNameVal,
                            profilePhotoUrl = photoVal
                        )
                    )
                }

                when (result) {
                    is LinkResult.SchoolNotFound -> call.fail("School not found", HttpStatusCode.NotFound)
                    is LinkResult.StudentNotFound ->
                        call.fail("No student found with that roll/admission number at this school", HttpStatusCode.NotFound)
                    is LinkResult.Linked -> call.created(result.child, message = "Child linked successfully")
                }
            }
        }
    }
}

/** Internal result of the link transaction so the suspend respond happens outside dbQuery. */
private sealed interface LinkResult {
    data object SchoolNotFound : LinkResult
    data object StudentNotFound : LinkResult
    data class Linked(val child: LinkedChildDto) : LinkResult
}
