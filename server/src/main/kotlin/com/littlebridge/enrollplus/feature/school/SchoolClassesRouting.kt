/*
 * File: SchoolClassesRouting.kt
 * Module: feature.school
 *
 * CRUD endpoints for school classes + subjects — the admin's "Classes & Subjects"
 * management surface. This is the central place where an admin defines:
 *   - Classes (code, name, sections)
 *   - Subjects per class (name, code)
 *
 * Endpoints (all JWT-guarded + school-scoped via requireSchoolContext):
 *   GET    /api/v1/school/classes                      — list all classes
 *   POST   /api/v1/school/classes                      — create a class
 *   PUT    /api/v1/school/classes/{id}                 — update a class
 *   DELETE /api/v1/school/classes/{id}                 — delete a class
 *   GET    /api/v1/school/classes/{classId}/subjects   — list subjects for a class
 *   POST   /api/v1/school/classes/{classId}/subjects   — add a subject to a class
 *   PUT    /api/v1/school/subjects/{id}                — update a subject
 *   DELETE /api/v1/school/subjects/{id}                — delete a subject
 */
package com.littlebridge.enrollplus.feature.school

import com.littlebridge.enrollplus.core.created
import com.littlebridge.enrollplus.core.fail
import com.littlebridge.enrollplus.core.ok
import com.littlebridge.enrollplus.core.okMessage
import com.littlebridge.enrollplus.core.requireSchoolAdmin
import com.littlebridge.enrollplus.core.requireSchoolContext
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.SchoolClassesTable
import com.littlebridge.enrollplus.db.SchoolSubjectsTable
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID as JUUID

// ───────────────────────────────── DTOs ──────────────────────────────────────

@Serializable
data class SchoolClassDto(
    val id: String,
    val code: String,
    val name: String,
    val sections: List<String> = emptyList(),
    @SerialName("subject_count") val subjectCount: Int = 0,
)

@Serializable
data class SchoolClassListResponse(
    val classes: List<SchoolClassDto>,
)

@Serializable
data class CreateSchoolClassRequest(
    val code: String,
    val name: String,
    val sections: List<String> = listOf("A"),
)

@Serializable
data class UpdateSchoolClassRequest(
    val code: String,
    val name: String,
    val sections: List<String> = listOf("A"),
)

@Serializable
data class SchoolSubjectDto(
    val id: String,
    @SerialName("class_id") val classId: String,
    val name: String,
    val code: String,
)

@Serializable
data class SchoolSubjectListResponse(
    val subjects: List<SchoolSubjectDto>,
)

@Serializable
data class CreateSchoolSubjectRequest(
    val name: String,
    val code: String,
)

@Serializable
data class UpdateSchoolSubjectRequest(
    val name: String,
    val code: String,
)

// ───────────────────────────────── Helpers ───────────────────────────────────

private fun parseUuid(s: String?): JUUID? = s?.let { runCatching { JUUID.fromString(it) }.getOrNull() }

private fun sectionsToJson(sections: List<String>): String {
    if (sections.isEmpty()) return "[]"
    return "[" + sections.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" } + "]"
}

private val CLASSES_JSON = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }

private fun parseSections(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        (CLASSES_JSON.parseToJsonElement(raw) as? kotlinx.serialization.json.JsonArray)
            ?.mapNotNull { runCatching { it.jsonPrimitive.content }.getOrNull()?.takeIf { s -> s.isNotBlank() } }
            ?: emptyList()
    }.getOrElse { emptyList() }
}

// ───────────────────────────────── Routing ───────────────────────────────────

fun Route.schoolClassesRouting() {
    authenticate("jwt") {
        route("/api/v1/school/classes") {

            // ---- list all classes ----
            get {
                val ctx = call.requireSchoolContext() ?: return@get
                val list = dbQuery {
                    val classes = SchoolClassesTable.selectAll()
                        .where { SchoolClassesTable.schoolId eq ctx.schoolId }
                        .orderBy(SchoolClassesTable.name)
                        .toList()
                    classes.map { r ->
                        val cid = r[SchoolClassesTable.id].value
                        val subjectCount = SchoolSubjectsTable.selectAll()
                            .where { SchoolSubjectsTable.classId eq cid }
                            .count().toInt()
                        SchoolClassDto(
                            id = cid.toString(),
                            code = r[SchoolClassesTable.code],
                            name = r[SchoolClassesTable.name],
                            sections = parseSections(r[SchoolClassesTable.sections]),
                            subjectCount = subjectCount,
                        )
                    }
                }
                call.ok(SchoolClassListResponse(list), message = "Classes fetched")
            }

            // ---- create a class ----
            post {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val req = call.receive<CreateSchoolClassRequest>()
                if (req.code.isBlank() || req.name.isBlank()) {
                    call.fail("Code and name are required", HttpStatusCode.BadRequest, "VALIDATION")
                    return@post
                }
                val now = Instant.now()
                val newId = JUUID.randomUUID()
                val dto = dbQuery {
                    // Check duplicate code
                    val existing = SchoolClassesTable.selectAll()
                        .where {
                            (SchoolClassesTable.schoolId eq ctx.schoolId) and
                                (SchoolClassesTable.code eq req.code.trim())
                        }
                        .firstOrNull()
                    if (existing != null) return@dbQuery null

                    SchoolClassesTable.insert {
                        it[SchoolClassesTable.id] = newId
                        it[SchoolClassesTable.schoolId] = ctx.schoolId
                        it[SchoolClassesTable.code] = req.code.trim()
                        it[SchoolClassesTable.name] = req.name.trim()
                        it[SchoolClassesTable.sections] = sectionsToJson(req.sections)
                        it[SchoolClassesTable.createdAt] = now
                    }
                    SchoolClassDto(
                        id = newId.toString(),
                        code = req.code.trim(),
                        name = req.name.trim(),
                        sections = req.sections,
                        subjectCount = 0,
                    )
                }
                if (dto != null) {
                    call.created(dto, "Class created")
                } else {
                    call.fail("A class with this code already exists", HttpStatusCode.Conflict, "DUPLICATE")
                }
            }

            // ---- update a class ----
            put("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val classId = parseUuid(call.parameters["id"]) ?: run {
                    call.fail("Invalid class ID", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@put
                }
                val req = call.receive<UpdateSchoolClassRequest>()
                if (req.code.isBlank() || req.name.isBlank()) {
                    call.fail("Code and name are required", HttpStatusCode.BadRequest, "VALIDATION")
                    return@put
                }
                val dto = dbQuery {
                    val count = SchoolClassesTable.update(
                        { (SchoolClassesTable.id eq classId) and (SchoolClassesTable.schoolId eq ctx.schoolId) }
                    ) {
                        it[SchoolClassesTable.code] = req.code.trim()
                        it[SchoolClassesTable.name] = req.name.trim()
                        it[SchoolClassesTable.sections] = sectionsToJson(req.sections)
                    }
                    if (count == 0) return@dbQuery null
                    val r = SchoolClassesTable.selectAll()
                        .where { SchoolClassesTable.id eq classId }
                        .first()
                    val subjectCount = SchoolSubjectsTable.selectAll()
                        .where { SchoolSubjectsTable.classId eq classId }
                        .count().toInt()
                    SchoolClassDto(
                        id = classId.toString(),
                        code = r[SchoolClassesTable.code],
                        name = r[SchoolClassesTable.name],
                        sections = parseSections(r[SchoolClassesTable.sections]),
                        subjectCount = subjectCount,
                    )
                }
                if (dto != null) {
                    call.ok(dto, message = "Class updated")
                } else {
                    call.fail("Class not found", HttpStatusCode.NotFound, "NOT_FOUND")
                }
            }

            // ---- delete a class ----
            delete("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val classId = parseUuid(call.parameters["id"]) ?: run {
                    call.fail("Invalid class ID", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@delete
                }
                val deleted = dbQuery {
                    // Delete subjects first
                    SchoolSubjectsTable.deleteWhere { SchoolSubjectsTable.classId eq classId }
                    SchoolClassesTable.deleteWhere {
                        (SchoolClassesTable.id eq classId) and (SchoolClassesTable.schoolId eq ctx.schoolId)
                    } > 0
                }
                if (deleted) {
                    call.okMessage("Class deleted")
                } else {
                    call.fail("Class not found", HttpStatusCode.NotFound, "NOT_FOUND")
                }
            }

            // ---- list subjects for a class ----
            get("/{classId}/subjects") {
                val ctx = call.requireSchoolContext() ?: return@get
                val classId = parseUuid(call.parameters["classId"]) ?: run {
                    call.fail("Invalid class ID", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@get
                }
                val list = dbQuery {
                    SchoolSubjectsTable.selectAll()
                        .where { SchoolSubjectsTable.classId eq classId }
                        .orderBy(SchoolSubjectsTable.subName)
                        .map { r ->
                            SchoolSubjectDto(
                                id = r[SchoolSubjectsTable.id].value.toString(),
                                classId = classId.toString(),
                                name = r[SchoolSubjectsTable.subName],
                                code = r[SchoolSubjectsTable.subCode],
                            )
                        }
                }
                call.ok(SchoolSubjectListResponse(list), message = "Subjects fetched")
            }

            // ---- add a subject to a class ----
            post("/{classId}/subjects") {
                val ctx = call.requireSchoolAdmin() ?: return@post
                val classId = parseUuid(call.parameters["classId"]) ?: run {
                    call.fail("Invalid class ID", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@post
                }
                val req = call.receive<CreateSchoolSubjectRequest>()
                if (req.name.isBlank() || req.code.isBlank()) {
                    call.fail("Subject name and code are required", HttpStatusCode.BadRequest, "VALIDATION")
                    return@post
                }
                val now = Instant.now()
                val newId = JUUID.randomUUID()
                val dto = dbQuery {
                    // Verify class belongs to this school
                    val classRow = SchoolClassesTable.selectAll()
                        .where {
                            (SchoolClassesTable.id eq classId) and
                                (SchoolClassesTable.schoolId eq ctx.schoolId)
                        }
                        .firstOrNull() ?: return@dbQuery null

                    SchoolSubjectsTable.insert {
                        it[SchoolSubjectsTable.id] = newId
                        it[SchoolSubjectsTable.classId] = classId
                        it[SchoolSubjectsTable.subName] = req.name.trim()
                        it[SchoolSubjectsTable.subCode] = req.code.trim()
                        it[SchoolSubjectsTable.createdAt] = now
                    }
                    SchoolSubjectDto(
                        id = newId.toString(),
                        classId = classId.toString(),
                        name = req.name.trim(),
                        code = req.code.trim(),
                    )
                }
                if (dto != null) {
                    call.created(dto, "Subject added")
                } else {
                    call.fail("Class not found in your school", HttpStatusCode.NotFound, "NOT_FOUND")
                }
            }
        }

        // Subject-level routes (not nested under classId)
        route("/api/v1/school/subjects") {

            // ---- update a subject ----
            put("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@put
                val subjectId = parseUuid(call.parameters["id"]) ?: run {
                    call.fail("Invalid subject ID", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@put
                }
                val req = call.receive<UpdateSchoolSubjectRequest>()
                if (req.name.isBlank() || req.code.isBlank()) {
                    call.fail("Subject name and code are required", HttpStatusCode.BadRequest, "VALIDATION")
                    return@put
                }
                val dto = dbQuery {
                    // Verify subject belongs to a class in this school
                    val subjectRow = SchoolSubjectsTable.selectAll()
                        .where { SchoolSubjectsTable.id eq subjectId }
                        .firstOrNull() ?: return@dbQuery null
                    val classId = subjectRow[SchoolSubjectsTable.classId]
                    val classRow = SchoolClassesTable.selectAll()
                        .where {
                            (SchoolClassesTable.id eq classId) and
                                (SchoolClassesTable.schoolId eq ctx.schoolId)
                        }
                        .firstOrNull() ?: return@dbQuery null

                    SchoolSubjectsTable.update(
                        { SchoolSubjectsTable.id eq subjectId }
                    ) {
                        it[SchoolSubjectsTable.subName] = req.name.trim()
                        it[SchoolSubjectsTable.subCode] = req.code.trim()
                    }
                    SchoolSubjectDto(
                        id = subjectId.toString(),
                        classId = classId.toString(),
                        name = req.name.trim(),
                        code = req.code.trim(),
                    )
                }
                if (dto != null) {
                    call.ok(dto, message = "Subject updated")
                } else {
                    call.fail("Subject not found in your school", HttpStatusCode.NotFound, "NOT_FOUND")
                }
            }

            // ---- delete a subject ----
            delete("/{id}") {
                val ctx = call.requireSchoolAdmin() ?: return@delete
                val subjectId = parseUuid(call.parameters["id"]) ?: run {
                    call.fail("Invalid subject ID", HttpStatusCode.BadRequest, "BAD_REQUEST")
                    return@delete
                }
                val deleted = dbQuery {
                    // Verify subject belongs to a class in this school
                    val subjectRow = SchoolSubjectsTable.selectAll()
                        .where { SchoolSubjectsTable.id eq subjectId }
                        .firstOrNull() ?: return@dbQuery false
                    val classId = subjectRow[SchoolSubjectsTable.classId]
                    val classRow = SchoolClassesTable.selectAll()
                        .where {
                            (SchoolClassesTable.id eq classId) and
                                (SchoolClassesTable.schoolId eq ctx.schoolId)
                        }
                        .firstOrNull() ?: return@dbQuery false

                    SchoolSubjectsTable.deleteWhere { SchoolSubjectsTable.id eq subjectId } > 0
                }
                if (deleted) {
                    call.okMessage("Subject deleted")
                } else {
                    call.fail("Subject not found in your school", HttpStatusCode.NotFound, "NOT_FOUND")
                }
            }
        }
    }
}
