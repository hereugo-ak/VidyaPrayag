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
 *   GET /api/v1/school/analytics/student-cohort
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
import com.littlebridge.vidyaprayag.core.requireSchoolContext
import com.littlebridge.vidyaprayag.db.AppConfigTable
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

/**
 * Real "star faculty": rank active faculty by their own attendance reliability
 * (PRESENT rate over the last 30 days), highest first, top 3. This is a genuine
 * server-side ranking rather than an arbitrary first-3 selection.
 */
private fun topStarFaculty(schoolId: UUID): JsonArray {
    val faculty = FacultyTable.selectAll()
        .where { (FacultyTable.schoolId eq schoolId) and (FacultyTable.isActive eq true) }
        .toList()
    if (faculty.isEmpty()) return JsonArray(emptyList())

    val cutoff = LocalDate.now().minusDays(30)
    val facultyAttendance = AttendanceRecordsTable.selectAll()
        .where {
            (AttendanceRecordsTable.schoolId eq schoolId) and
                (AttendanceRecordsTable.type eq "faculty")
        }
        .toList()
        .filter {
            runCatching { LocalDate.parse(it[AttendanceRecordsTable.date]).isAfter(cutoff) }
                .getOrDefault(false)
        }
        .groupBy { it[AttendanceRecordsTable.personId] }

    // score = present-rate*100 (falls back to a neutral 90 when no records).
    data class Ranked(val name: String, val dept: String, val pic: String?, val score: Double)
    val ranked = faculty.map { r ->
        val ext = r[FacultyTable.externalId]
        val recs = facultyAttendance[ext].orEmpty()
        val score = if (recs.isEmpty()) 90.0
            else recs.count { it[AttendanceRecordsTable.status].equals("PRESENT", true) } * 100.0 / recs.size
        Ranked(
            name = r[FacultyTable.name],
            dept = r[FacultyTable.department] ?: "",
            pic = r[FacultyTable.profilePic],
            score = score
        )
    }.sortedByDescending { it.score }.take(3)

    return buildJsonArray {
        ranked.forEachIndexed { idx, rk ->
            add(buildJsonObject {
                put("rank", idx + 1)
                put("name", rk.name)
                put("department", rk.dept)
                put("score", (kotlin.math.round(rk.score * 10) / 10.0))
                put("image_url", rk.pic?.let { JsonPrimitive(it) } ?: JsonNull)
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
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = ctx.schoolId

                val payload = dbQuery {
                    val overview = cmsObject("school_analytics_overview")
                    val trend = (overview["performance_trend"] as? JsonArray)
                        ?.mapNotNull { runCatching { it.jsonPrimitive.content.toDouble() }.getOrNull() }
                        ?: emptyList()
                    val growth = (overview["current_growth"] as? JsonPrimitive)?.content ?: "0%"

                    val cardsTpl = cmsArray("school_analytics_cards_template")
                    val livePct = avgAttendancePct(schoolId)
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
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = ctx.schoolId
                val classFilter = call.request.queryParameters["class"]

                val payload: JsonObject = dbQuery {
                    val blob = cmsObject("school_class_performance").toMutableMap()

                    val liveActive: Int = run {
                        val q = if (!classFilter.isNullOrBlank()) {
                            StudentsTable.selectAll().where {
                                (StudentsTable.schoolId eq schoolId) and
                                    (StudentsTable.isActive eq true) and
                                    (StudentsTable.className eq classFilter)
                            }
                        } else {
                            StudentsTable.selectAll().where {
                                (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true)
                            }
                        }
                        q.count().toInt()
                    }

                    if (liveActive > 0) {
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
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = ctx.schoolId

                val payload: JsonObject = dbQuery {
                    val blob = cmsObject("school_teacher_performance").toMutableMap()
                    val stars = topStarFaculty(schoolId)
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
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = ctx.schoolId
                val rawId = call.parameters["studentId"]
                    ?: run { call.fail("studentId required"); return@get }

                val payload = dbQuery {
                    // CRITICAL: scope the lookup to the caller's school so a known
                    // id/code from ANOTHER school cannot be read (cross-school leak).
                    val byUuid = runCatching { UUID.fromString(rawId) }.getOrNull()?.let { id ->
                        StudentsTable.selectAll()
                            .where { (StudentsTable.id eq id) and (StudentsTable.schoolId eq schoolId) }
                            .singleOrNull()
                    }
                    val row = byUuid ?: StudentsTable.selectAll()
                        .where { (StudentsTable.studentCode eq rawId) and (StudentsTable.schoolId eq schoolId) }
                        .singleOrNull()

                    if (row == null) return@dbQuery null

                    val studentUuid = row[StudentsTable.id].value

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
                call.requireSchoolContext() ?: return@get

                val payload = dbQuery { cmsObject("school_syllabus_coverage") }
                call.ok(payload, message = "Syllabus coverage fetched successfully")
            }

            // ============================================================
            // GET /student-cohort
            // ------------------------------------------------------------
            // Cohort-level (school-wide) student analytics — drives
            // StudentAnalyticsScreen (the dashboard view, NOT the
            // per-student drilldown).
            //
            // Live overlays:
            //   - risk.low_count is overlaid with the live count of active
            //     students for the school when available (CMS supplies the
            //     critical/medium counts since we don't yet compute risk
            //     scoring server-side).
            //   - All other fields come verbatim from CMS so ops can iterate.
            // ============================================================
            get("/student-cohort") {
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = ctx.schoolId

                val payload: JsonObject = dbQuery {
                    val blob = cmsObject("school_student_analytics_cohort").toMutableMap()

                    val liveActive: Int = StudentsTable.selectAll()
                        .where { (StudentsTable.schoolId eq schoolId) and (StudentsTable.isActive eq true) }
                        .count()
                        .toInt()

                    if (liveActive > 0) {
                        val risk = (blob["risk"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
                        val critical = (risk["critical_count"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        val medium   = (risk["medium_count"]   as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
                        // low = total active - already-flagged buckets, floored at 0
                        val low = (liveActive - critical - medium).coerceAtLeast(0)
                        risk["low_count"] = JsonPrimitive(low)
                        blob["risk"] = JsonObject(risk)
                    }
                    JsonObject(blob)
                }
                call.ok(payload, message = "Student cohort analytics fetched successfully")
            }
        }
    }
}
