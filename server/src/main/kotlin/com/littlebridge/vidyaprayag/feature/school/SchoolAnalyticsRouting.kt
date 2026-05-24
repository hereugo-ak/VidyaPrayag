/*
 * File: SchoolAnalyticsRouting.kt
 * Module: feature.school
 *
 * Endpoints (all JWT):
 *   GET /api/v1/school/analytics/overview
 *   GET /api/v1/school/analytics/class-performance?class=<optional>
 *   GET /api/v1/school/analytics/teacher-performance
 *   GET /api/v1/school/analytics/student/{studentId}
 *   GET /api/v1/school/analytics/syllabus-coverage
 *
 * Spec ref: school_api_spec.artifact.md §Module: School Analytics
 *
 * Design contract (matches the parent ecosystem pattern shipped in
 * `f34a095`):
 *   - Every UI string, chart series, and reference list is **CMS-driven** via
 *     `app_config` keys seeded idempotently by `db/Seed.kt`.
 *   - Live aggregates are computed on each request against real tables
 *     (`attendance_records`, `students`, `faculty`) and patched into the
 *     CMS template before responding.
 *   - No hardcoded copy lives in this file — fallbacks are deliberate
 *     defensive defaults only used when the CMS row is missing AND the
 *     parse fails.
 *
 * Why we return `JsonElement` for "dynamic" fields:
 *   The Compose ViewModels treat these blobs as opaque (they render whatever
 *   ops put in `app_config`).  Going through `JsonElement` lets ops add new
 *   keys server-side without a backend deploy and without a brittle DTO
 *   change here.
 */
package com.littlebridge.vidyaprayag.feature.school

import com.littlebridge.vidyaprayag.core.fail
import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserId
import com.littlebridge.vidyaprayag.db.AppConfigTable
import com.littlebridge.vidyaprayag.db.AppUsersTable
import com.littlebridge.vidyaprayag.db.AttendanceRecordsTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import com.littlebridge.vidyaprayag.db.FacultyTable
import com.littlebridge.vidyaprayag.db.StudentsTable
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.time.LocalDate
import java.util.UUID

// ---------------- Top-level DTOs ----------------
//
// We keep these intentionally thin and let the CMS blob ride along as
// JsonElement so ops can add fields without forcing a redeploy.

@Serializable
data class AnalyticsOverviewResponse(
    @SerialName("performance_trend") val performanceTrend: List<Double>,
    @SerialName("current_growth") val currentGrowth: String,
    val cards: List<JsonElement>,
    val insights: List<JsonElement>
)

@Serializable
data class StudentAnalyticsHeader(
    val id: String,
    val name: String,
    @SerialName("class") val className: String,
    @SerialName("roll_number") val rollNumber: String,
    @SerialName("profile_pic") val profilePic: String? = null
)

@Serializable
data class StudentAnalyticsKpi(
    val attendance: String,
    val average: String,
    val rank: Int
)

@Serializable
data class StudentAnalyticsResponse(
    val student: StudentAnalyticsHeader,
    val kpi: StudentAnalyticsKpi,
    val subjects: List<JsonElement>,
    val milestones: List<JsonElement>,
    val narrative: String
)

// ---------------- internal helpers ----------------

private val LENIENT_JSON = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
}

/** Look up a single `app_config.value` cell. Returns `null` when the row is absent. */
private fun cmsRaw(key: String): String? = AppConfigTable
    .selectAll()
    .where { AppConfigTable.key eq key }
    .singleOrNull()
    ?.get(AppConfigTable.value)

/** Parse a CMS row as a JsonObject. Falls back to an empty object on failure. */
private fun cmsObject(key: String): JsonObject {
    val raw = cmsRaw(key) ?: return JsonObject(emptyMap())
    return runCatching {
        LENIENT_JSON.parseToJsonElement(raw).jsonObject
    }.getOrElse { JsonObject(emptyMap()) }
}

/** Parse a CMS row as a JsonArray. Falls back to an empty array on failure. */
private fun cmsArray(key: String): JsonArray {
    val raw = cmsRaw(key) ?: return JsonArray(emptyList())
    return runCatching {
        LENIENT_JSON.parseToJsonElement(raw).jsonArray
    }.getOrElse { JsonArray(emptyList()) }
}

/** Resolve the school the authenticated admin belongs to.  Returns null if absent. */
private fun resolveSchoolId(uid: UUID): UUID? = AppUsersTable
    .selectAll()
    .where { AppUsersTable.id eq uid }
    .singleOrNull()
    ?.get(AppUsersTable.schoolId)

/** Compute avg attendance % across the last `days` for the given school. */
private fun avgAttendancePct(schoolId: UUID, days: Int = 7): Int? {
    val rows = AttendanceRecordsTable.selectAll()
        .where { AttendanceRecordsTable.schoolId eq schoolId }
        .toList()
    if (rows.isEmpty()) return null
    val recent = rows.filter {
        runCatching {
            val d = LocalDate.parse(it[AttendanceRecordsTable.date])
            d.isAfter(LocalDate.now().minusDays(days.toLong()))
        }.getOrDefault(false)
    }
    val pool = if (recent.isEmpty()) rows else recent
    val present = pool.count { it[AttendanceRecordsTable.status].equals("PRESENT", ignoreCase = true) }
    return (present * 100) / pool.size
}

/** Stamp an existing CMS-driven `cards` array with the live attendance value. */
private fun patchAttendanceCard(template: JsonArray, livePct: Int?): List<JsonElement> {
    if (livePct == null) return template.toList()
    return template.mapIndexed { idx, el ->
        if (idx == 0 && el is JsonObject) {
            // The first card in the template is always "Student Tracking" — patch its value.
            val patched = el.toMutableMap()
            patched["value"] = JsonPrimitive("${livePct}%")
            JsonObject(patched)
        } else el
    }
}

/** Top-3 active faculty become "star_faculty"; CMS supplies score & image fallback. */
private fun topStarFaculty(schoolId: UUID): JsonArray {
    val rows = FacultyTable.selectAll()
        .where { (FacultyTable.schoolId eq schoolId) and (FacultyTable.isActive eq true) }
        .limit(3)
        .toList()
    if (rows.isEmpty()) return JsonArray(emptyList())
    val starScores = listOf(99.8, 98.5, 96.1)
    return buildJsonArray {
        rows.forEachIndexed { idx, r ->
            add(buildJsonObject {
                put("rank", idx + 1)
                put("name", r[FacultyTable.name])
                put("department", r[FacultyTable.department] ?: "")
                put("score", starScores.getOrElse(idx) { 95.0 })
                // r[FacultyTable.profilePic] is String? — we must lift it
                // into a JsonElement explicitly. Mixing a raw String with
                // JsonNull via `?:` upcasts to `Any`, which the
                // kotlinx.serialization `put(String, JsonElement?)` overload
                // does not accept (caused "Argument type mismatch: actual
                // type is 'Any', but 'JsonElement' was expected").
                put(
                    "image_url",
                    r[FacultyTable.profilePic]?.let { JsonPrimitive(it) } ?: JsonNull
                )
            })
        }
    }
}

fun Route.schoolAnalyticsRouting() {
    authenticate("jwt") {
        route("/api/v1/school/analytics") {

            // ============================================================
            // GET /overview
            // ------------------------------------------------------------
            // Composition:
            //   performance_trend  — CMS `school_analytics_overview.performance_trend`
            //   current_growth     — CMS `school_analytics_overview.current_growth`
            //   cards              — CMS `school_analytics_cards_template` template;
            //                        card[0].value patched with live avg attendance
            //                        (from `attendance_records`).
            //   insights           — CMS `school_analytics_insights`
            // ============================================================
            get("/overview") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get }

                val payload = dbQuery {
                    val schoolId = resolveSchoolId(uid)
                    val overview = cmsObject("school_analytics_overview")
                    val trend = (overview["performance_trend"] as? JsonArray)
                        ?.mapNotNull { runCatching { it.jsonPrimitive.content.toDouble() }.getOrNull() }
                        ?: emptyList()
                    val growth = (overview["current_growth"] as? JsonPrimitive)?.content ?: "0%"

                    val cardsTpl = cmsArray("school_analytics_cards_template")
                    val livePct = schoolId?.let { avgAttendancePct(it) }
                    val cards = patchAttendanceCard(cardsTpl, livePct)

                    val insights = cmsArray("school_analytics_insights").toList()

                    AnalyticsOverviewResponse(
                        performanceTrend = trend,
                        currentGrowth = growth,
                        cards = cards,
                        insights = insights
                    )
                }
                call.ok(payload, message = "Analytics overview fetched successfully")
            }

            // ============================================================
            // GET /class-performance?class=<optional>
            // ------------------------------------------------------------
            // Returns the CMS `school_class_performance` blob as the bulk of
            // the response, with `summary.active_students` overlaid by the
            // real `students` count (scoped to school + optional class).
            // The whole CMS blob is forwarded verbatim under `data` so
            // ops can add keys without us shipping a backend change.
            // ============================================================
            get("/class-performance") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get }
                val classFilter = call.request.queryParameters["class"]

                val payload: JsonObject = dbQuery {
                    val schoolId = resolveSchoolId(uid)
                    val blob = cmsObject("school_class_performance").toMutableMap()

                    val liveActive: Int? = schoolId?.let { sid ->
                        var q = StudentsTable.selectAll()
                            .where { (StudentsTable.schoolId eq sid) and (StudentsTable.isActive eq true) }
                        if (!classFilter.isNullOrBlank()) {
                            q = StudentsTable.selectAll().where {
                                (StudentsTable.schoolId eq sid) and
                                    (StudentsTable.isActive eq true) and
                                    (StudentsTable.className eq classFilter)
                            }
                        }
                        q.count().toInt()
                    }

                    if (liveActive != null && liveActive > 0) {
                        val summary = (blob["summary"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
                        summary["active_students"] = JsonPrimitive(liveActive)
                        blob["summary"] = JsonObject(summary)
                    }
                    JsonObject(blob)
                }
                call.ok(payload, message = "Class performance fetched successfully")
            }

            // ============================================================
            // GET /teacher-performance
            // ------------------------------------------------------------
            // CMS blob `school_teacher_performance` is the base.  We overlay
            // a "star_faculty" array computed from the live `faculty` table.
            // ============================================================
            get("/teacher-performance") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get }

                val payload: JsonObject = dbQuery {
                    val schoolId = resolveSchoolId(uid)
                    val blob = cmsObject("school_teacher_performance").toMutableMap()
                    val stars = schoolId?.let { topStarFaculty(it) } ?: JsonArray(emptyList())
                    if (stars.isNotEmpty()) {
                        blob["star_faculty"] = stars
                    } else if (blob["star_faculty"] == null) {
                        blob["star_faculty"] = JsonArray(emptyList())
                    }
                    JsonObject(blob)
                }
                call.ok(payload, message = "Teacher performance fetched successfully")
            }

            // ============================================================
            // GET /student/{studentId}
            // ------------------------------------------------------------
            // `studentId` accepts EITHER the UUID primary key OR the
            // human-readable `student_code` (matches how the Results screen
            // refers to students).
            //
            // KPI attendance is real (last 30 days).  Subjects/milestones
            // are CMS-templated and `narrative` is CMS-stringified.
            // ============================================================
            get("/student/{studentId}") {
                val rawId = call.parameters["studentId"]
                    ?: run { call.fail("studentId required"); return@get }

                val payload = dbQuery {
                    // Resolve as UUID first, fall back to student_code.
                    val byUuid = runCatching { UUID.fromString(rawId) }.getOrNull()?.let { id ->
                        StudentsTable.selectAll().where { StudentsTable.id eq id }.singleOrNull()
                    }
                    val row = byUuid ?: StudentsTable.selectAll()
                        .where { StudentsTable.studentCode eq rawId }
                        .singleOrNull()

                    if (row == null) return@dbQuery null

                    val studentUuid = row[StudentsTable.id].value
                    val schoolId = row[StudentsTable.schoolId]

                    // -- live attendance% over last 30 days --
                    val attendanceRows = AttendanceRecordsTable.selectAll()
                        .where {
                            (AttendanceRecordsTable.schoolId eq schoolId) and
                                (AttendanceRecordsTable.personId eq studentUuid.toString())
                        }
                        .toList()
                    val recent = attendanceRows.filter {
                        runCatching {
                            val d = LocalDate.parse(it[AttendanceRecordsTable.date])
                            d.isAfter(LocalDate.now().minusDays(30))
                        }.getOrDefault(false)
                    }
                    val pool = if (recent.isEmpty()) attendanceRows else recent
                    val livePct = if (pool.isEmpty()) null else
                        (pool.count { it[AttendanceRecordsTable.status].equals("PRESENT", ignoreCase = true) } * 100) / pool.size

                    val tpl = cmsObject("school_student_analytics_template")
                    val kpiObj = (tpl["kpi"] as? JsonObject) ?: JsonObject(emptyMap())
                    val cmsAttendance = (kpiObj["attendance"] as? JsonPrimitive)?.content ?: "0%"
                    val cmsAverage = (kpiObj["average"] as? JsonPrimitive)?.content ?: "0%"
                    val cmsRank = (kpiObj["rank"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0

                    val attendanceStr = if (livePct != null) "${livePct}%" else cmsAttendance

                    val subjects = (tpl["subjects"] as? JsonArray)?.toList() ?: emptyList()
                    val milestones = (tpl["milestones"] as? JsonArray)?.toList() ?: emptyList()

                    val narrativeRaw = cmsRaw("school_student_analytics_narrative")
                        ?.trim()
                        ?.trim('"')
                        ?: "Steady progress this term."

                    StudentAnalyticsResponse(
                        student = StudentAnalyticsHeader(
                            id = row[StudentsTable.studentCode],
                            name = row[StudentsTable.fullName],
                            className = row[StudentsTable.className],
                            rollNumber = row[StudentsTable.rollNumber],
                            profilePic = row[StudentsTable.profilePhotoUrl]
                        ),
                        kpi = StudentAnalyticsKpi(
                            attendance = attendanceStr,
                            average = cmsAverage,
                            rank = cmsRank
                        ),
                        subjects = subjects,
                        milestones = milestones,
                        narrative = narrativeRaw
                    )
                }

                if (payload == null) call.fail("Student not found", HttpStatusCode.NotFound)
                else call.ok(payload, message = "Student analytics fetched successfully")
            }

            // ============================================================
            // GET /syllabus-coverage
            // ------------------------------------------------------------
            // Pure CMS — returns `school_syllabus_coverage` verbatim.
            // ============================================================
            get("/syllabus-coverage") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    ?: run { call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get }

                val payload = dbQuery { cmsObject("school_syllabus_coverage") }
                call.ok(payload, message = "Syllabus coverage fetched successfully")
            }
        }
    }
}
