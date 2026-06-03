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
import com.littlebridge.vidyaprayag.db.ExamResultsTable
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
    @SerialName("trend_labels") val trendLabels: List<String> = emptyList(),
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

/**
 * Real performance trend: the monthly student-attendance present-rate
 * (0.0..1.0) for the last [months] calendar months, oldest-first. The UI
 * renders these as bar heights, so a 0..1 fraction is exactly what it needs.
 *
 * Returns a Pair of (trend, monthLabels) so the screen can render real x-axis
 * labels instead of hardcoded "Jan..Jun". Months with no records contribute
 * 0.0 (an honest "no data" bar) rather than a fabricated value.
 */
private fun monthlyAttendanceTrend(
    schoolId: UUID,
    months: Int = 6
): Pair<List<Double>, List<String>> {
    val rows = AttendanceRecordsTable.selectAll()
        .where {
            (AttendanceRecordsTable.schoolId eq schoolId) and
                (AttendanceRecordsTable.type eq "student")
        }
        .toList()

    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    val today = LocalDate.now()
    // Build the ordered list of (year, month) buckets, oldest-first.
    val buckets = (months - 1 downTo 0).map { back ->
        val d = today.minusMonths(back.toLong())
        d.year to d.monthValue
    }

    // Group attendance rows by (year, month).
    val byMonth = rows.groupBy {
        runCatching {
            val d = LocalDate.parse(it[AttendanceRecordsTable.date])
            d.year to d.monthValue
        }.getOrNull()
    }

    val trend = ArrayList<Double>(months)
    val labels = ArrayList<String>(months)
    for ((y, m) in buckets) {
        labels.add(monthNames[m - 1])
        val pool = byMonth[y to m].orEmpty()
        if (pool.isEmpty()) {
            trend.add(0.0)
        } else {
            val present = pool.count { it[AttendanceRecordsTable.status].equals("PRESENT", true) }
            trend.add((present.toDouble() / pool.size).let { kotlin.math.round(it * 1000) / 1000.0 })
        }
    }
    return trend to labels
}

/**
 * Parse a score string like "98", "85.5", or "A+" into a 0..100 numeric value.
 * Letter grades are mapped to representative midpoints; non-numeric / "Pending"
 * scores return null and are excluded from averages.
 */
private fun parseScore(raw: String?): Double? {
    if (raw.isNullOrBlank()) return null
    val s = raw.trim()
    s.toDoubleOrNull()?.let { return it.coerceIn(0.0, 100.0) }
    return when (s.uppercase()) {
        "A+" -> 95.0
        "A"  -> 88.0
        "B+" -> 82.0
        "B"  -> 75.0
        "C+" -> 68.0
        "C"  -> 60.0
        "D"  -> 50.0
        "E", "F" -> 35.0
        else -> null
    }
}

/**
 * Real cohort comparison: the average exam score per class for the given school,
 * normalised to a 0.0..1.0 fraction (bar height). Returns (values, labels) with
 * classes ordered by their natural class-name ordering, capped to the top 6 so
 * the chart stays readable.
 */
private fun cohortComparison(schoolId: UUID): Pair<List<Double>, List<String>> {
    val rows = ExamResultsTable.selectAll()
        .where { ExamResultsTable.schoolId eq schoolId }
        .toList()
    if (rows.isEmpty()) return emptyList<Double>() to emptyList()

    val byClass = rows.groupBy { it[ExamResultsTable.className] }
    val averages = byClass.mapNotNull { (className, recs) ->
        val scores = recs.mapNotNull { parseScore(it[ExamResultsTable.score]) }
        if (scores.isEmpty()) null
        else className to (scores.average())
    }
    if (averages.isEmpty()) return emptyList<Double>() to emptyList()

    // Stable, human-friendly ordering by class name; cap at 6 columns.
    val ordered = averages.sortedBy { it.first }.take(6)
    val values = ordered.map { kotlin.math.round(it.second / 100.0 * 1000) / 1000.0 }
    val labels = ordered.map { it.first }
    return values to labels
}

/**
 * Real daily attendance volatility: the student present-rate (0.0..1.0) for each
 * of the last [days] calendar days, oldest-first. Days with no records contribute
 * 0.0. The UI highlights the lowest bar (biggest drop) as the anomaly.
 */
private fun dailyVolatility(schoolId: UUID, days: Int = 14): List<Double> {
    val rows = AttendanceRecordsTable.selectAll()
        .where {
            (AttendanceRecordsTable.schoolId eq schoolId) and
                (AttendanceRecordsTable.type eq "student")
        }
        .toList()
    if (rows.isEmpty()) return emptyList()

    val today = LocalDate.now()
    val byDate = rows.groupBy { it[AttendanceRecordsTable.date] }
    val series = ArrayList<Double>(days)
    for (back in (days - 1) downTo 0) {
        val key = today.minusDays(back.toLong()).toString()
        val pool = byDate[key].orEmpty()
        if (pool.isEmpty()) {
            series.add(0.0)
        } else {
            val present = pool.count { it[AttendanceRecordsTable.status].equals("PRESENT", true) }
            series.add(kotlin.math.round(present.toDouble() / pool.size * 1000) / 1000.0)
        }
    }
    return series
}

/**
 * Real per-subject coverage proxy: average exam score per subject for the school,
 * as an integer percentage. Returns a list of (subjectName, percentage) ordered
 * by subject name. The syllabus screen renders these as the intelligence graph
 * bars + labels and the progress rings.
 */
private fun subjectCoverage(schoolId: UUID): List<Pair<String, Int>> {
    val rows = ExamResultsTable.selectAll()
        .where { ExamResultsTable.schoolId eq schoolId }
        .toList()
    if (rows.isEmpty()) return emptyList()

    val bySubject = rows.groupBy { it[ExamResultsTable.subject] }
    return bySubject.mapNotNull { (subject, recs) ->
        val scores = recs.mapNotNull { parseScore(it[ExamResultsTable.score]) }
        if (scores.isEmpty()) null
        else subject to scores.average().toInt().coerceIn(0, 100)
    }.sortedBy { it.first }
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
            //   performance_trend  — REAL: monthly student-attendance present
            //                        rate for the last 6 months (from
            //                        `attendance_records`). Falls back to CMS
            //                        `school_analytics_overview.performance_trend`
            //                        only when there is no live attendance data.
            //   trend_labels       — REAL: month labels matching the trend.
            //   current_growth     — REAL: last month vs previous month delta
            //                        (signed %). Falls back to CMS when no data.
            //   cards              — CMS `school_analytics_cards_template` template;
            //                        card[0].value patched with live avg attendance.
            //   insights           — CMS `school_analytics_insights`
            // ============================================================
            get("/overview") {
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = ctx.schoolId

                val payload = dbQuery {
                    val overview = cmsObject("school_analytics_overview")
                    val cmsTrend = (overview["performance_trend"] as? JsonArray)
                        ?.mapNotNull { runCatching { it.jsonPrimitive.content.toDouble() }.getOrNull() }
                        ?: emptyList()
                    val cmsGrowth = (overview["current_growth"] as? JsonPrimitive)?.content ?: "0%"

                    // --- REAL trend computed from attendance_records ---
                    val (liveTrend, liveLabels) = monthlyAttendanceTrend(schoolId, months = 6)
                    val hasLiveData = liveTrend.any { it > 0.0 }

                    val trend = if (hasLiveData) liveTrend else cmsTrend
                    val labels = if (hasLiveData) liveLabels else emptyList()

                    // --- REAL growth: last month vs previous month ---
                    val growth = if (hasLiveData && liveTrend.size >= 2) {
                        val last = liveTrend[liveTrend.size - 1]
                        val prev = liveTrend[liveTrend.size - 2]
                        val deltaPts = (last - prev) * 100.0
                        val sign = if (deltaPts >= 0) "+" else ""
                        "$sign${(kotlin.math.round(deltaPts * 10) / 10.0)}%"
                    } else cmsGrowth

                    val cardsTpl = cmsArray("school_analytics_cards_template")
                    val livePct = avgAttendancePct(schoolId)
                    val cards = patchAttendanceCard(cardsTpl, livePct)

                    val insights = cmsArray("school_analytics_insights").toList()

                    AnalyticsOverviewResponse(
                        performanceTrend = trend,
                        trendLabels = labels,
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

                    // --- REAL average + class rank from exam_results ---
                    val studentCode = row[StudentsTable.studentCode]
                    val className = row[StudentsTable.className]
                    // All exam rows for this student's class, grouped by student code.
                    val classExamRows = ExamResultsTable.selectAll()
                        .where {
                            (ExamResultsTable.schoolId eq schoolId) and
                                (ExamResultsTable.className eq className)
                        }
                        .toList()
                    val avgByStudent: Map<String, Double> = classExamRows
                        .groupBy { it[ExamResultsTable.studentId] }
                        .mapValues { (_, recs) ->
                            val scores = recs.mapNotNull { parseScore(it[ExamResultsTable.score]) }
                            if (scores.isEmpty()) 0.0 else scores.average()
                        }
                        .filterValues { it > 0.0 }

                    val liveAverage: Double? = avgByStudent[studentCode]
                    // Rank = 1 + number of classmates strictly above this student.
                    val liveRank: Int? = liveAverage?.let { mine ->
                        1 + avgByStudent.values.count { it > mine }
                    }

                    val averageStr = if (liveAverage != null)
                        "${kotlin.math.round(liveAverage).toInt()}%" else cmsAverage
                    val rankInt = liveRank ?: cmsRank

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
                            average = averageStr,
                            rank = rankInt
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
                val ctx = call.requireSchoolContext() ?: return@get
                val schoolId = ctx.schoolId

                val payload = dbQuery {
                    val blob = cmsObject("school_syllabus_coverage").toMutableMap()

                    // --- REAL per-subject coverage from exam_results ---
                    val coverage = subjectCoverage(schoolId)
                    if (coverage.isNotEmpty()) {
                        // Rebuild by_subject from live data; keep behind_by_days at 0
                        // (we don't yet track a curriculum schedule server-side).
                        blob["by_subject"] = buildJsonArray {
                            coverage.forEach { (subject, pct) ->
                                add(buildJsonObject {
                                    put("name", subject)
                                    put("percentage", pct)
                                    put("behind_by_days", 0)
                                    put("trend", if (pct >= 75) "On Target" else "Needs Attention")
                                })
                            }
                        }
                        // Overall = mean of subject coverages.
                        val overallPct = coverage.map { it.second }.average().toInt().coerceIn(0, 100)
                        blob["overall"] = buildJsonObject {
                            put("percentage", overallPct)
                            put("trend", (blob["overall"] as? JsonObject)
                                ?.get("trend")?.jsonPrimitive?.content ?: "")
                        }
                    }
                    JsonObject(blob)
                }
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

                    // --- REAL daily volatility from attendance_records (last 14 days) ---
                    val volatility = dailyVolatility(schoolId, days = 14)
                    if (volatility.any { it > 0.0 }) {
                        blob["daily_volatility"] = buildJsonArray {
                            volatility.forEach { add(JsonPrimitive(it)) }
                        }
                    }

                    // --- REAL cohort comparison: avg exam score per class ---
                    val (cohortValues, cohortLabels) = cohortComparison(schoolId)
                    if (cohortValues.isNotEmpty()) {
                        blob["cohort_comparison"] = buildJsonArray {
                            cohortValues.forEach { add(JsonPrimitive(it)) }
                        }
                        blob["cohort_labels"] = buildJsonArray {
                            cohortLabels.forEach { add(JsonPrimitive(it)) }
                        }
                    }

                    JsonObject(blob)
                }
                call.ok(payload, message = "Student cohort analytics fetched successfully")
            }
        }
    }
}
