/*
 * File: TeacherSyllabusRouting.kt
 * Module: feature.teacher
 *
 * T-402 (Doc 08 §1.2/§2/§3) — the canonical, typed, lifecycle-aware SYLLABUS
 * plane that will replace the legacy free-text `/syllabus` GET+PATCH handlers in
 * TeacherRoutingTasks.kt (which support only a coverage toggle — NO create, the
 * B-SYL-1 dead-empty defect; and scope by free-text class_name/subject, D-SYL-3).
 *
 * THE TEMPLATE/PROGRESS SPLIT LIVES HERE (Doc 08 §1.2). A unit is a row in
 * `curriculum_units` (the template, per class+subject, hierarchical chapter ▸
 * topic). Its per-section COVERAGE is a row in `syllabus_progress`, keyed UNIQUE
 * on (unit, section, assignment) so two sections of the same class track coverage
 * independently. This plane:
 *   • GET    …/syllabus?assignmentId=  → units + this assignment's coverage,
 *                                        hierarchical, with covered/total counts.
 *   • POST   …/syllabus/units          → create a chapter or topic (B-SYL-1 fix).
 *   • PATCH  …/syllabus/units/{id}     → rename / reorder (edit-mode).
 *   • PATCH  …/syllabus/progress       → the ONE-TAP toggle. Idempotent upsert;
 *                                        stamps typed covered_on (D-SYL-2/X-3) +
 *                                        covered_by; section comes from the
 *                                        authorizing assignment, never the client.
 *
 * Every read and write is scope-bound to the authorizing teacher_subject_assignment
 * via requireOwnedAssignment (X-1) — never a free-text class/section/subject. The
 * unit's class_id + subject_id MUST match the owned assignment's typed scope, so a
 * teacher cannot toggle/rename/create against a curriculum row outside their
 * allocation even if they guess its id. Scoping is enforced at THREE levels (the
 * constitution): the SQL only ever touches owned curriculum + this assignment's
 * progress (query), the response only carries that scope (API), and the screen
 * reaches this pre-scoped (UI, T-403).
 *
 * PATH NOTE (staged, converges in T-403):
 *   The legacy `teacherTaskRoutes()` still binds `GET/PATCH /api/v1/teacher/
 *   syllabus`, and Ktor forbids two handlers on the same method+path. So this
 *   typed plane mounts under a temporary `/api/v1/teacher/syllabus-typed` prefix
 *   (the T-203 `/attendance-typed`→`/attendance` and T-303 `/gradebook`→
 *   `/assessments` staging precedent). T-403 DELETES the legacy handler and
 *   converges this plane to the canonical `/syllabus` paths from Doc 08 §3 (which
 *   the shared TeacherApi client's `syllabus-typed` calls will be repointed to).
 *
 * DTOs are defined server-side (the :server module does NOT depend on :shared)
 * and mirror shared/.../teacher/domain/model/TeacherModels.kt field-for-field
 * (SyllabusLoadResponse/SyllabusLoadDto/SyllabusNodeDto/CreateSyllabusUnitRequest/
 *  UpdateSyllabusUnitRequest/ToggleSyllabusProgressRequest/SyllabusUnitMutationResponse).
 */
package com.littlebridge.vidyaprayag.feature.teacher

import com.littlebridge.vidyaprayag.core.OwnedAssignment
import com.littlebridge.vidyaprayag.core.TeacherContext
import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.requireOwnedAssignment
import com.littlebridge.vidyaprayag.core.requireTeacherContext
import com.littlebridge.vidyaprayag.db.CurriculumUnitsTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.SyllabusProgressTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// Server-side DTOs — mirror shared/.../teacher/domain/model/TeacherModels.kt
// (SyllabusLoadDto / SyllabusNodeDto / CreateSyllabusUnitRequest /
//  UpdateSyllabusUnitRequest / ToggleSyllabusProgressRequest /
//  SyllabusUnitMutationResponse) field-for-field.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class SylNodeDto(
    val id: String,
    @SerialName("parent_id") val parentId: String? = null,
    val title: String,
    val position: Int = 0,
    val depth: Int = 0,
    @SerialName("is_chapter") val isChapter: Boolean = false,
    @SerialName("is_covered") val isCovered: Boolean = false,
    @SerialName("covered_on") val coveredOn: String? = null,
    val note: String? = null,
)

@Serializable
data class SylLoadDto(
    @SerialName("assignment_id") val assignmentId: String? = null,
    @SerialName("class_name") val className: String = "",
    val section: String = "",
    val subject: String = "",
    val units: List<SylNodeDto> = emptyList(),
    @SerialName("covered_count") val coveredCount: Int = 0,
    @SerialName("total_count") val totalCount: Int = 0,
)

@Serializable
data class SylCreateUnitRequest(
    @SerialName("assignment_id") val assignmentId: String = "",
    val title: String = "",
    @SerialName("parent_id") val parentId: String? = null,
)

@Serializable
data class SylUpdateUnitRequest(
    val title: String? = null,
    val position: Int? = null,
)

@Serializable
data class SylToggleProgressRequest(
    @SerialName("assignment_id") val assignmentId: String = "",
    @SerialName("unit_id") val unitId: String = "",
    @SerialName("is_covered") val isCovered: Boolean = false,
    @SerialName("covered_on") val coveredOn: String? = null,
    val note: String? = null,
)

@Serializable
data class SylMutationData(
    val success: Boolean = true,
    val data: SylNodeDto? = null,
)

// ─────────────────────────────────────────────────────────────────────────────
// Helpers.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Resolve a curriculum unit id to its row, asserting it belongs to the caller's
 * school AND to the SAME typed scope (class_id + subject_id) as the owned
 * assignment. This is the syllabus scope gate — a unit is reachable ONLY through
 * an owned assignment whose class+subject matches it (X-1). Responds + returns
 * null on 400/404/403.
 */
private suspend fun ApplicationCall.requireOwnedUnit(
    asg: OwnedAssignment,
    schoolId: UUID,
    unitId: String?,
): ResultRow? {
    val id = unitId?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
        fail("A valid unit id is required", HttpStatusCode.BadRequest, "BAD_UNIT_ID")
        return null
    }
    val row = dbQuery {
        CurriculumUnitsTable.selectAll().where {
            (CurriculumUnitsTable.id eq id) and
                (CurriculumUnitsTable.schoolId eq schoolId) and
                (CurriculumUnitsTable.isActive eq true)
        }.singleOrNull()
    } ?: run {
        fail("Syllabus unit not found in your school", HttpStatusCode.NotFound, "UNIT_NOT_FOUND")
        return null
    }
    // The unit's typed class+subject MUST equal the owned assignment's scope
    // (X-1). Guards against toggling/renaming a unit outside the allocation.
    val scopeMatches = asg.classId != null && asg.subjectId != null &&
        row[CurriculumUnitsTable.classId] == asg.classId &&
        row[CurriculumUnitsTable.subjectId] == asg.subjectId
    if (!scopeMatches) {
        fail("This unit is not in your assigned class/subject", HttpStatusCode.Forbidden, "NOT_IN_SCOPE")
        return null
    }
    return row
}

/**
 * Build the hierarchical, ordered node list for an owned assignment: every active
 * curriculum unit for the assignment's typed class+subject, each carrying its own
 * coverage state for THIS assignment's section. Chapters (parent_id null) first in
 * `position` order; their topics nested immediately after, also in `position`
 * order. depth 0 = chapter, 1 = topic (hierarchy is at most 2 deep, Doc 08).
 */
private suspend fun loadSyllabusNodes(asg: OwnedAssignment): List<SylNodeDto> {
    val classId = asg.classId ?: return emptyList()
    val subjectId = asg.subjectId ?: return emptyList()
    return dbQuery {
        val units = CurriculumUnitsTable.selectAll().where {
            (CurriculumUnitsTable.classId eq classId) and
                (CurriculumUnitsTable.subjectId eq subjectId) and
                (CurriculumUnitsTable.isActive eq true)
        }.orderBy(CurriculumUnitsTable.position, SortOrder.ASC).toList()
        if (units.isEmpty()) return@dbQuery emptyList<SylNodeDto>()

        // This assignment's coverage rows for those units (section-scoped).
        val unitIds = units.map { it[CurriculumUnitsTable.id].value }
        val progressByUnit = SyllabusProgressTable.selectAll().where {
            (SyllabusProgressTable.assignmentId eq asg.assignmentId) and
                (SyllabusProgressTable.section eq asg.section) and
                (SyllabusProgressTable.unitId inList unitIds)
        }.associateBy { it[SyllabusProgressTable.unitId] }

        fun nodeOf(row: ResultRow, depth: Int): SylNodeDto {
            val uid = row[CurriculumUnitsTable.id].value
            val prog = progressByUnit[uid]
            return SylNodeDto(
                id = uid.toString(),
                parentId = row[CurriculumUnitsTable.parentId]?.toString(),
                title = row[CurriculumUnitsTable.title],
                position = row[CurriculumUnitsTable.position],
                depth = depth,
                isChapter = row[CurriculumUnitsTable.parentId] == null,
                isCovered = prog?.get(SyllabusProgressTable.isCovered) ?: false,
                coveredOn = prog?.get(SyllabusProgressTable.coveredOn)?.toString(),
                note = prog?.get(SyllabusProgressTable.note),
            )
        }

        // Chapters in order, each followed by its topics in order. Orphan topics
        // (parent missing/inactive) sink to the end so nothing is silently dropped.
        val byParent = units.groupBy { it[CurriculumUnitsTable.parentId] }
        val chapters = (byParent[null] ?: emptyList())
            .sortedBy { it[CurriculumUnitsTable.position] }
        val result = mutableListOf<SylNodeDto>()
        for (chapter in chapters) {
            result += nodeOf(chapter, depth = 0)
            val cid = chapter[CurriculumUnitsTable.id].value
            (byParent[cid] ?: emptyList())
                .sortedBy { it[CurriculumUnitsTable.position] }
                .forEach { result += nodeOf(it, depth = 1) }
        }
        // Topics whose parent isn't an active chapter in this scope → append flat.
        val placed = result.map { it.id }.toSet()
        units.filter { it[CurriculumUnitsTable.id].value.toString() !in placed }
            .sortedBy { it[CurriculumUnitsTable.position] }
            .forEach { result += nodeOf(it, depth = if (it[CurriculumUnitsTable.parentId] == null) 0 else 1) }
        result
    }
}

/** Re-read one unit as a node DTO for the mutation response (covered state for this section). */
private suspend fun nodeForUnit(asg: OwnedAssignment, unitId: UUID): SylNodeDto? = dbQuery {
    val row = CurriculumUnitsTable.selectAll().where {
        CurriculumUnitsTable.id eq unitId
    }.singleOrNull() ?: return@dbQuery null
    val prog = SyllabusProgressTable.selectAll().where {
        (SyllabusProgressTable.unitId eq unitId) and
            (SyllabusProgressTable.assignmentId eq asg.assignmentId) and
            (SyllabusProgressTable.section eq asg.section)
    }.singleOrNull()
    SylNodeDto(
        id = unitId.toString(),
        parentId = row[CurriculumUnitsTable.parentId]?.toString(),
        title = row[CurriculumUnitsTable.title],
        position = row[CurriculumUnitsTable.position],
        depth = if (row[CurriculumUnitsTable.parentId] == null) 0 else 1,
        isChapter = row[CurriculumUnitsTable.parentId] == null,
        isCovered = prog?.get(SyllabusProgressTable.isCovered) ?: false,
        coveredOn = prog?.get(SyllabusProgressTable.coveredOn)?.toString(),
        note = prog?.get(SyllabusProgressTable.note),
    )
}

fun Route.teacherSyllabusRouting() {
    authenticate("jwt") {
        // STAGED under `/syllabus-typed` to avoid colliding with the legacy
        // `teacherTaskRoutes()` `/syllabus` GET+PATCH handlers (Ktor forbids two
        // handlers on the same method+path). T-403 deletes the legacy handler and
        // converges this plane to the canonical `/syllabus` paths (Doc 08 §3) —
        // the T-203/T-303 staging precedent.
        route("/api/v1/teacher/syllabus-typed") {
            syllabusLoad()
            syllabusCreateUnit()
            syllabusUpdateUnit()
            syllabusToggleProgress()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GET /api/v1/teacher/syllabus-typed?assignmentId=   (units + progress, hierarchical)
// Doc 08 §3.
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.syllabusLoad() {
    get {
        val ctx = call.requireTeacherContext() ?: return@get
        val assignmentParam = call.request.queryParameters["assignmentId"]
            ?: call.request.queryParameters["assignment_id"]
        val asg = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@get

        val nodes = loadSyllabusNodes(asg)
        val covered = nodes.count { it.isCovered }
        call.ok(
            SylLoadDto(
                assignmentId = asg.assignmentId.toString(),
                className = "${asg.className}-${asg.section}".trim('-'),
                section = asg.section,
                subject = asg.subject,
                units = nodes,
                coveredCount = covered,
                totalCount = nodes.size,
            ),
            message = if (nodes.isEmpty()) "No syllabus yet" else "Syllabus loaded",
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// POST /api/v1/teacher/syllabus-typed/units   (create a chapter or topic) — B-SYL-1 fix.
// Doc 08 §3. parentId null → chapter; otherwise a topic under that chapter (the
// parent must be an owned chapter in the same scope).
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.syllabusCreateUnit() {
    post("/units") {
        val ctx = call.requireTeacherContext() ?: return@post
        val req = runCatching { call.receive<SylCreateUnitRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@post
        }
        val asg = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@post
        // Capture the typed scope as non-null locals (smart-cast doesn't cross the
        // dbQuery closure boundary on a data-class property).
        val scopeClassId = asg.classId
        val scopeSubjectId = asg.subjectId
        if (scopeClassId == null || scopeSubjectId == null) {
            call.fail("This class is not fully configured yet", HttpStatusCode.Conflict, "CLASS_NOT_CONFIGURED")
            return@post
        }
        val title = req.title.trim()
        if (title.isBlank()) {
            call.fail("Unit title is required", HttpStatusCode.BadRequest, "BAD_TITLE"); return@post
        }

        // If parentId given, it must be an owned chapter (parent_id null) in the
        // same typed scope — a topic cannot nest under a topic (≤2 deep, Doc 08).
        val parentUuid: UUID? = if (!req.parentId.isNullOrBlank()) {
            val parentRow = call.requireOwnedUnit(asg, ctx.schoolId, req.parentId) ?: return@post
            if (parentRow[CurriculumUnitsTable.parentId] != null) {
                call.fail("A topic cannot be nested under another topic", HttpStatusCode.BadRequest, "PARENT_NOT_CHAPTER")
                return@post
            }
            parentRow[CurriculumUnitsTable.id].value
        } else null

        val now = Instant.now()
        val newId = UUID.randomUUID()
        dbQuery {
            // Append at the end of its sibling group (max position + 1).
            val siblingMax = CurriculumUnitsTable.selectAll().where {
                (CurriculumUnitsTable.classId eq scopeClassId) and
                    (CurriculumUnitsTable.subjectId eq scopeSubjectId) and
                    (CurriculumUnitsTable.isActive eq true) and
                    (if (parentUuid == null) CurriculumUnitsTable.parentId.isNull()
                    else (CurriculumUnitsTable.parentId eq parentUuid))
            }.maxOfOrNull { it[CurriculumUnitsTable.position] } ?: -1

            CurriculumUnitsTable.insert {
                it[id] = newId
                it[schoolId] = ctx.schoolId
                it[classId] = scopeClassId
                it[subjectId] = scopeSubjectId
                it[parentId] = parentUuid
                it[CurriculumUnitsTable.title] = title
                it[position] = siblingMax + 1
                it[isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }
        }
        val node = nodeForUnit(asg, newId)
        call.created(
            SylMutationData(success = true, data = node),
            message = if (parentUuid == null) "Chapter added" else "Topic added",
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PATCH /api/v1/teacher/syllabus-typed/units/{id}   (rename / reorder)
// Doc 08 §3.
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.syllabusUpdateUnit() {
    patch("/units/{id}") {
        val ctx = call.requireTeacherContext() ?: return@patch
        // We need an owned assignment to scope the unit. The unit's scope is
        // derived from itself, but ownership is proven via an assignment the
        // caller holds for that class+subject. Resolve via assignmentId query/body
        // if present, else fall back to matching one of the caller's assignments.
        val assignmentParam = call.request.queryParameters["assignmentId"]
            ?: call.request.queryParameters["assignment_id"]
        val req = runCatching { call.receive<SylUpdateUnitRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@patch
        }
        val asg = call.requireOwnedAssignment(ctx, assignmentParam) ?: return@patch
        val unitRow = call.requireOwnedUnit(asg, ctx.schoolId, call.parameters["id"]) ?: return@patch
        val unitId = unitRow[CurriculumUnitsTable.id].value

        val newTitle = req.title?.trim()?.takeIf { it.isNotBlank() }
        if (req.title != null && newTitle == null) {
            call.fail("Unit title cannot be blank", HttpStatusCode.BadRequest, "BAD_TITLE"); return@patch
        }
        if (newTitle == null && req.position == null) {
            call.fail("Nothing to update", HttpStatusCode.BadRequest, "NO_OP"); return@patch
        }

        val now = Instant.now()
        dbQuery {
            CurriculumUnitsTable.update({ CurriculumUnitsTable.id eq unitId }) {
                if (newTitle != null) it[title] = newTitle
                if (req.position != null) it[position] = req.position.coerceAtLeast(0)
                it[updatedAt] = now
            }
        }
        val node = nodeForUnit(asg, unitId)
        call.ok(SylMutationData(success = true, data = node), message = "Unit updated")
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PATCH /api/v1/teacher/syllabus-typed/progress   (the ONE-TAP toggle)
// Doc 08 §3. Idempotent upsert on (unit, section, assignment); stamps typed
// covered_on (today unless an explicit past date is given) + covered_by.
// ─────────────────────────────────────────────────────────────────────────────
private fun Route.syllabusToggleProgress() {
    patch("/progress") {
        val ctx = call.requireTeacherContext() ?: return@patch
        val req = runCatching { call.receive<SylToggleProgressRequest>() }.getOrNull()
        if (req == null) {
            call.fail("Invalid request body", HttpStatusCode.BadRequest, "BAD_REQUEST"); return@patch
        }
        val asg = call.requireOwnedAssignment(ctx, req.assignmentId) ?: return@patch
        val unitRow = call.requireOwnedUnit(asg, ctx.schoolId, req.unitId) ?: return@patch
        val unitId = unitRow[CurriculumUnitsTable.id].value

        // Section is the AUTHORITATIVE one from the owned assignment, never the
        // client (X-1) — a teacher can only mark coverage for the section they own.
        val section = asg.section.ifBlank { "A" }

        // covered_on: when covering, use the explicit (past) date if valid, else
        // today (typed, D-SYL-2). When un-covering, clear it.
        val coveredOn: LocalDate? = if (!req.isCovered) {
            null
        } else {
            req.coveredOn?.takeIf { it.isNotBlank() }?.let {
                runCatching { LocalDate.parse(it) }.getOrNull() ?: run {
                    call.fail("covered_on must be YYYY-MM-DD", HttpStatusCode.BadRequest, "BAD_DATE")
                    return@patch
                }
            } ?: LocalDate.now()
        }
        // A future covered_on is nonsensical (you can't have covered it tomorrow).
        if (coveredOn != null && coveredOn.isAfter(LocalDate.now())) {
            call.fail("covered_on cannot be in the future", HttpStatusCode.BadRequest, "DATE_FUTURE")
            return@patch
        }
        val note = req.note?.takeIf { it.isNotBlank() }
        val now = Instant.now()

        dbQuery {
            val existing = SyllabusProgressTable.selectAll().where {
                (SyllabusProgressTable.unitId eq unitId) and
                    (SyllabusProgressTable.section eq section) and
                    (SyllabusProgressTable.assignmentId eq asg.assignmentId)
            }.singleOrNull()

            if (existing != null) {
                SyllabusProgressTable.update({
                    SyllabusProgressTable.id eq existing[SyllabusProgressTable.id]
                }) {
                    it[isCovered] = req.isCovered
                    it[SyllabusProgressTable.coveredOn] = coveredOn
                    it[coveredBy] = if (req.isCovered) ctx.userId else null
                    if (note != null || !req.isCovered) it[SyllabusProgressTable.note] = note
                    it[updatedAt] = now
                }
            } else {
                SyllabusProgressTable.insert {
                    it[id] = UUID.randomUUID()
                    it[SyllabusProgressTable.unitId] = unitId
                    it[SyllabusProgressTable.section] = section
                    it[assignmentId] = asg.assignmentId
                    it[isCovered] = req.isCovered
                    it[SyllabusProgressTable.coveredOn] = coveredOn
                    it[coveredBy] = if (req.isCovered) ctx.userId else null
                    it[SyllabusProgressTable.note] = note
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }
        }
        val node = nodeForUnit(asg, unitId)
        call.ok(
            SylMutationData(success = true, data = node),
            message = if (req.isCovered) "Marked covered" else "Marked not covered",
        )
    }
}
