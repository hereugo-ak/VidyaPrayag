/*
 * File: ResultsRouting.kt
 * Module: feature.school
 *
 * Endpoints (all JWT):
 *   GET  /api/v1/school/results?test=...&class=...&subject=...
 *   POST /api/v1/school/results
 *
 * Spec ref: school_api_spec.artifact.md §Module: Results
 *
 * GET aggregates the filtered slice of `exam_results` (school + test +
 * class + subject) into a `summary` block:
 *   class_average      → arithmetic mean of numeric `score` values
 *   average_trend      → CMS-curated for now (real signal lives in a
 *                        future analytics service)
 *   exceeding_count    → count where status == "Exceeding"
 *   meeting_count      → count where status == "Meeting"
 *   below_count        → count where status == "Below"
 *
 * `available_tests` / `available_classes` / `available_subjects` come from
 * CMS key `school_results_filters` so ops control curriculum metadata
 * without backend redeploys.
 *
 * POST does a portable upsert (Exposed has no cross-DB UPSERT yet) — we
 * select on the unique tuple `(school, test, class, subject, student_id)`
 * and either UPDATE the existing row or INSERT a new one.  The endpoint
 * is fully idempotent: publishing the same test twice updates rather
 * than duplicates.
 */
package com.littlebridge.vidyaprayag.feature.school

import com.littlebridge.vidyaprayag.core.created
import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppConfigTable
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.ExamResultsTable
import com.littlebridge.vidyaprayag.db.StudentsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

// ---------------- DTOs ----------------

@Serializable
data class ResultsFiltersDto(
    @SerialName("selected_test") val selectedTest: String,
    @SerialName("selected_class") val selectedClass: String,
    @SerialName("selected_subject") val selectedSubject: String,
    @SerialName("available_tests") val availableTests: List<String>,
    @SerialName("available_classes") val availableClasses: List<String>,
    @SerialName("available_subjects") val availableSubjects: List<String>
)

@Serializable
data class ResultsSummaryDto(
    @SerialName("class_average") val classAverage: String,
    @SerialName("average_trend") val averageTrend: String,
    @SerialName("exceeding_count") val exceedingCount: Int,
    @SerialName("meeting_count") val meetingCount: Int,
    @SerialName("below_count") val belowCount: Int
)

@Serializable
data class ResultStudentDto(
    val id: String,
    val name: String,
    @SerialName("image_url") val imageUrl: String? = null,
    val attendance: String,
    val score: String,
    val status: String,
    val trend: String
)

@Serializable
data class ResultsResponse(
    val filters: ResultsFiltersDto,
    val summary: ResultsSummaryDto,
    val students: List<ResultStudentDto>
)

@Serializable
data class PublishResultsRow(
    @SerialName("student_id") val studentId: String,
    @SerialName("student_name") val studentName: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val attendance: String? = null,
    val score: String,
    val status: String? = null,
    val trend: String? = null
)

@Serializable
data class PublishResultsDto(
    val test: String,
    @SerialName("class") val className: String,
    val subject: String,
    val results: List<PublishResultsRow>
)

@Serializable
data class PublishResultsResponse(val upserted: Int)

// ---------------- helpers ----------------

private val LENIENT_JSON = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

private fun resolveSchoolId(uid: UUID): UUID? = AppUsersTable
    .selectAll().where { AppUsersTable.id eq uid }
    .singleOrNull()
    ?.get(AppUsersTable.schoolId)

private fun cmsArrayStrings(rootKey: String, leafKey: String): List<String> {
    val raw = AppConfigTable.selectAll()
        .where { AppConfigTable.key eq rootKey }
        .singleOrNull()
        ?.get(AppConfigTable.value)
        ?: return emptyList()
    return runCatching {
        val root = LENIENT_JSON.parseToJsonElement(raw)
        (root as? kotlinx.serialization.json.JsonObject)?.get(leafKey)?.let {
            (it as? JsonArray)?.mapNotNull { el -> el.jsonPrimitive.content }
        } ?: emptyList()
    }.getOrElse { emptyList() }
}

private fun numericScoreOrNull(score: String): Double? =
    score.trim().trimEnd('%').toDoubleOrNull()

fun Route.resultsRouting() {
    authenticate("jwt") {
        route("/api/v1/school/results") {

            // -------- GET --------
            get {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get }

                val test = call.request.queryParameters["test"].orEmpty()
                val classFilter = call.request.queryParameters["class"].orEmpty()
                val subject = call.request.queryParameters["subject"].orEmpty()

                val payload = dbQuery {
                    val schoolId = resolveSchoolId(uid)

                    val availableTests = cmsArrayStrings("school_results_filters", "available_tests")
                    val availableClasses = cmsArrayStrings("school_results_filters", "available_classes")
                    val availableSubjects = cmsArrayStrings("school_results_filters", "available_subjects")

                    val resolvedTest    = if (test.isBlank())    availableTests.firstOrNull().orEmpty()    else test
                    val resolvedClass   = if (classFilter.isBlank()) availableClasses.firstOrNull().orEmpty() else classFilter
                    val resolvedSubject = if (subject.isBlank()) availableSubjects.firstOrNull().orEmpty() else subject

                    val rows = if (schoolId == null) emptyList() else {
                        ExamResultsTable.selectAll()
                            .where {
                                (ExamResultsTable.schoolId eq schoolId) and
                                    (ExamResultsTable.test eq resolvedTest) and
                                    (ExamResultsTable.className eq resolvedClass) and
                                    (ExamResultsTable.subject eq resolvedSubject)
                            }
                            .orderBy(ExamResultsTable.studentName, SortOrder.ASC)
                            .toList()
                    }

                    val students = rows.map { r ->
                        ResultStudentDto(
                            id = r[ExamResultsTable.studentId],
                            name = r[ExamResultsTable.studentName],
                            imageUrl = r[ExamResultsTable.imageUrl],
                            attendance = r[ExamResultsTable.attendance],
                            score = r[ExamResultsTable.score],
                            status = r[ExamResultsTable.status],
                            trend = r[ExamResultsTable.trend]
                        )
                    }

                    val numericScores = rows.mapNotNull { numericScoreOrNull(it[ExamResultsTable.score]) }
                    val classAverage = if (numericScores.isEmpty()) "0.0"
                                       else "%.1f".format(numericScores.average())

                    val exceeding = rows.count { it[ExamResultsTable.status].equals("Exceeding", true) }
                    val meeting   = rows.count { it[ExamResultsTable.status].equals("Meeting",   true) }
                    val below     = rows.count { it[ExamResultsTable.status].equals("Below",     true) }

                    ResultsResponse(
                        filters = ResultsFiltersDto(
                            selectedTest = resolvedTest,
                            selectedClass = resolvedClass,
                            selectedSubject = resolvedSubject,
                            availableTests = availableTests,
                            availableClasses = availableClasses,
                            availableSubjects = availableSubjects
                        ),
                        summary = ResultsSummaryDto(
                            classAverage = classAverage,
                            averageTrend = "+0.0%", // placeholder until analytics pipeline lands
                            exceedingCount = exceeding,
                            meetingCount = meeting,
                            belowCount = below
                        ),
                        students = students
                    )
                }
                call.ok(payload, message = "Results fetched successfully")
            }

            // -------- PUBLISH (bulk upsert) --------
            post {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post }
                val req = call.receive<PublishResultsDto>()
                if (req.test.isBlank() || req.className.isBlank() || req.subject.isBlank()) {
                    call.fail("test, class, subject are required"); return@post
                }
                if (req.results.isEmpty()) {
                    call.fail("results must not be empty"); return@post
                }

                val schoolId = dbQuery { resolveSchoolId(uid) }
                    ?: run { call.fail("User not associated with any school", HttpStatusCode.NotFound); return@post }

                val now = Instant.now()
                val upserted = dbQuery {
                    var count = 0
                    req.results.forEach { row ->
                        // Default student_name lookup against `students` table if not provided.
                        val resolvedName = row.studentName ?: StudentsTable.selectAll()
                            .where { StudentsTable.studentCode eq row.studentId }
                            .singleOrNull()
                            ?.get(StudentsTable.fullName)
                            ?: row.studentId
                        val resolvedAttendance = row.attendance ?: "0%"
                        val resolvedStatus = row.status ?: "Pending"
                        val resolvedTrend = row.trend ?: "0%"
                        val resolvedImage = row.imageUrl

                        val existing = ExamResultsTable.selectAll().where {
                            (ExamResultsTable.schoolId eq schoolId) and
                                (ExamResultsTable.test eq req.test) and
                                (ExamResultsTable.className eq req.className) and
                                (ExamResultsTable.subject eq req.subject) and
                                (ExamResultsTable.studentId eq row.studentId)
                        }.singleOrNull()

                        if (existing == null) {
                            ExamResultsTable.insert {
                                it[id] = UUID.randomUUID()
                                it[ExamResultsTable.schoolId] = schoolId
                                it[test] = req.test
                                it[className] = req.className
                                it[subject] = req.subject
                                it[studentId] = row.studentId
                                it[studentName] = resolvedName
                                it[imageUrl] = resolvedImage
                                it[attendance] = resolvedAttendance
                                it[score] = row.score
                                it[status] = resolvedStatus
                                it[trend] = resolvedTrend
                                it[createdAt] = now
                                it[updatedAt] = now
                            }
                        } else {
                            ExamResultsTable.update({ ExamResultsTable.id eq existing[ExamResultsTable.id].value }) {
                                it[studentName] = resolvedName
                                if (resolvedImage != null) it[imageUrl] = resolvedImage
                                it[attendance] = resolvedAttendance
                                it[score] = row.score
                                it[status] = resolvedStatus
                                it[trend] = resolvedTrend
                                it[updatedAt] = now
                            }
                        }
                        count++
                    }
                    count
                }
                call.created(PublishResultsResponse(upserted), message = "Results published")
            }
        }
    }
}
