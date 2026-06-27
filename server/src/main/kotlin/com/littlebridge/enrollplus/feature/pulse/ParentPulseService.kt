/*
 * File: ParentPulseService.kt
 * Module: feature.pulse
 *
 * Core service for the Parent Pulse feature (PARENT_PULSE_SPEC.md).
 * Aggregates a week's worth of school activity for a (parent, student) pair
 * into a single pulse record, generates a narrative summary (template-based
 * fallback when AI is unavailable), and persists it.
 *
 * Data sources:
 *   - AttendanceRecordsTable  → attendance % for the week
 *   - AssessmentsTable + AssessmentMarksTable → marks published this week
 *   - HomeworkTable + HomeworkSubmissionsTable → homework due/completed
 *   - AnnouncementsTable → announcements count for the child's class
 *   - MessageThreadsTable → unread messages (denormalized unreadCount)
 *   - CalendarEventsTable → upcoming events in next 7 days
 */
package com.littlebridge.enrollplus.feature.pulse

import com.littlebridge.enrollplus.db.AnnouncementsTable
import com.littlebridge.enrollplus.db.AssessmentMarksTable
import com.littlebridge.enrollplus.db.AssessmentsTable
import com.littlebridge.enrollplus.db.AttendanceRecordsTable
import com.littlebridge.enrollplus.db.CalendarEventsTable
import com.littlebridge.enrollplus.db.ChildrenTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.HomeworkSubmissionsTable
import com.littlebridge.enrollplus.db.HomeworkTable
import com.littlebridge.enrollplus.db.MessageThreadsTable
import com.littlebridge.enrollplus.db.ParentPulsesTable
import com.littlebridge.enrollplus.db.StudentsTable
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

private val pulseLog = LoggerFactory.getLogger("ParentPulseService")
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

// ---------- DTOs ----------

@Serializable
data class MarkEntry(
    val subject: String,
    val test: String,
    val marks: Double,
    val max: Int,
)

@Serializable
data class UpcomingEvent(
    val title: String,
    val date: String,
)

@Serializable
data class PulseDto(
    val id: String,
    val studentName: String,
    val weekRange: String,
    val weekStartDate: String,
    val weekEndDate: String,
    val attendancePercentage: Double?,
    val attendanceTrend: String?,
    val marksSummary: List<MarkEntry>,
    val homeworkPending: Int,
    val homeworkCompleted: Int,
    val announcementsCount: Int,
    val unreadMessages: Int,
    val upcomingEvents: List<UpcomingEvent>,
    val aiNarrative: String,
    val actionableItems: List<String>,
    val createdAt: String,
)

// ---------- Internal aggregation result ----------

data class PulseAggregate(
    val studentName: String,
    val studentCode: String,
    val className: String,
    val attendancePercentage: Double?,
    val marksSummary: List<MarkEntry>,
    val homeworkPending: Int,
    val homeworkCompleted: Int,
    val announcementsCount: Int,
    val unreadMessages: Int,
    val upcomingEvents: List<UpcomingEvent>,
    val actionableItems: List<String>,
)

// ---------- Service ----------

class ParentPulseService {

    /**
     * Generate a pulse for a single (parent, student) pair for the given week.
     * Idempotent: if a pulse already exists for this (parent, student, week),
     * it is updated in place.
     *
     * @return the generated PulseDto, or null if the child/student could not be resolved.
     */
    suspend fun generatePulse(
        parentId: UUID,
        childId: UUID,
        weekStart: LocalDate,
    ): PulseDto? {
        val weekEnd = weekStart.plusDays(6)
        pulseLog.info("generatePulse: parentId={}, childId={}, week={} to {}", parentId, childId, weekStart, weekEnd)

        // 1. Resolve the child → student identity
        val child = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and (ChildrenTable.parentId eq parentId)
            }.firstOrNull()
        } ?: run {
            pulseLog.warn("generatePulse: no child found for childId={}, parentId={}", childId, parentId)
            return null
        }

        val studentCode = child[ChildrenTable.studentCode]
            ?: run {
                pulseLog.warn("generatePulse: child {} has no studentCode — skipping", childId)
                return null
            }
        val schoolId = child[ChildrenTable.schoolId]
            ?: run {
                pulseLog.warn("generatePulse: child {} has no schoolId — skipping", childId)
                return null
            }
        val studentName = child[ChildrenTable.childName]

        // Resolve the students.id UUID (needed for attendance + typed marks)
        val studentRow = dbQuery {
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and (StudentsTable.studentCode eq studentCode)
            }.firstOrNull()
        }
        val studentUuid = studentRow?.get(StudentsTable.id)?.value
        val className = studentRow?.get(StudentsTable.className)
            ?: child[ChildrenTable.currentGrade]
            ?: "Unknown"

        // 2. Aggregate week's data
        val attendancePct = fetchAttendancePercentage(schoolId, studentUuid, studentCode, weekStart, weekEnd)
        val marks = fetchMarks(schoolId, studentCode, studentUuid, weekStart, weekEnd)
        val homework = fetchHomeworkStatus(schoolId, studentCode, studentUuid, weekStart, weekEnd)
        val announcementsCount = fetchAnnouncementsCount(schoolId, className, weekStart, weekEnd)
        val unreadCount = fetchUnreadMessages(parentId)
        val upcomingEvents = fetchUpcomingEvents(schoolId, weekEnd)

        // 3. Fetch previous week's pulse for trend comparison
        val prevPulse = dbQuery {
            ParentPulsesTable.selectAll().where {
                (ParentPulsesTable.parentId eq parentId) and
                    (ParentPulsesTable.studentId eq (studentUuid ?: childId)) and
                    (ParentPulsesTable.weekStartDate eq weekStart.minusWeeks(1))
            }.firstOrNull()
        }
        val prevAttendance = prevPulse?.get(ParentPulsesTable.attendancePercentage)
        val attendanceTrend = computeTrend(attendancePct, prevAttendance)

        // 4. Build actionable items
        val actionableItems = buildActionableItems(
            homeworkPending = homework.first,
            unreadMessages = unreadCount,
            upcomingEvents = upcomingEvents,
        )

        // 5. Build narrative (template-based fallback — AI integration later)
        val narrative = buildFallbackNarrative(
            studentName = studentName,
            attendancePct = attendancePct,
            prevAttendance = prevAttendance,
            marks = marks,
            homeworkPending = homework.first,
            homeworkCompleted = homework.second,
            unreadMessages = unreadCount,
            upcomingEvents = upcomingEvents,
        )

        val marksJson = if (marks.isNotEmpty()) json.encodeToString(ListSerializer(MarkEntry.serializer()), marks) else null
        val eventsJson = if (upcomingEvents.isNotEmpty()) json.encodeToString(ListSerializer(UpcomingEvent.serializer()), upcomingEvents) else null
        val actionableJson = if (actionableItems.isNotEmpty()) json.encodeToString(ListSerializer(String.serializer()), actionableItems) else null

        // 6. Persist (upsert: insert or update if exists for this week)
        val now = Instant.now()
        val existingId = dbQuery {
            ParentPulsesTable.selectAll().where {
                (ParentPulsesTable.parentId eq parentId) and
                    (ParentPulsesTable.studentId eq (studentUuid ?: childId)) and
                    (ParentPulsesTable.weekStartDate eq weekStart)
            }.firstOrNull()?.get(ParentPulsesTable.id)?.value
        }

        if (existingId != null) {
            dbQuery {
                ParentPulsesTable.update({ ParentPulsesTable.id eq existingId }) {
                    it[ParentPulsesTable.schoolId] = schoolId
                    it[ParentPulsesTable.studentName] = studentName
                    it[ParentPulsesTable.weekEndDate] = weekEnd
                    it[ParentPulsesTable.attendancePercentage] = attendancePct
                    it[ParentPulsesTable.attendanceTrend] = attendanceTrend
                    it[ParentPulsesTable.marksSummary] = marksJson
                    it[ParentPulsesTable.homeworkPending] = homework.first
                    it[ParentPulsesTable.homeworkCompleted] = homework.second
                    it[ParentPulsesTable.announcementsCount] = announcementsCount
                    it[ParentPulsesTable.unreadMessages] = unreadCount
                    it[ParentPulsesTable.upcomingEvents] = eventsJson
                    it[ParentPulsesTable.aiNarrative] = narrative
                    it[ParentPulsesTable.actionableItems] = actionableJson
                    it[ParentPulsesTable.modelUsed] = null
                    it[ParentPulsesTable.tokensUsed] = null
                    it[ParentPulsesTable.createdAt] = now
                }
            }
            pulseLog.info("generatePulse: updated existing pulse {}", existingId)
        } else {
            dbQuery {
                ParentPulsesTable.insert {
                    it[ParentPulsesTable.schoolId] = schoolId
                    it[ParentPulsesTable.parentId] = parentId
                    it[ParentPulsesTable.studentId] = studentUuid ?: childId
                    it[ParentPulsesTable.studentName] = studentName
                    it[ParentPulsesTable.weekStartDate] = weekStart
                    it[ParentPulsesTable.weekEndDate] = weekEnd
                    it[ParentPulsesTable.attendancePercentage] = attendancePct
                    it[ParentPulsesTable.attendanceTrend] = attendanceTrend
                    it[ParentPulsesTable.marksSummary] = marksJson
                    it[ParentPulsesTable.homeworkPending] = homework.first
                    it[ParentPulsesTable.homeworkCompleted] = homework.second
                    it[ParentPulsesTable.announcementsCount] = announcementsCount
                    it[ParentPulsesTable.unreadMessages] = unreadCount
                    it[ParentPulsesTable.upcomingEvents] = eventsJson
                    it[ParentPulsesTable.aiNarrative] = narrative
                    it[ParentPulsesTable.actionableItems] = actionableJson
                    it[ParentPulsesTable.modelUsed] = null
                    it[ParentPulsesTable.tokensUsed] = null
                    it[ParentPulsesTable.createdAt] = now
                }
            }
            pulseLog.info("generatePulse: inserted new pulse for {} ({})", studentName, studentCode)
        }

        // 7. Return the DTO
        return fetchPulseDto(parentId, studentUuid ?: childId, weekStart)
    }

    /**
     * Batch-generate pulses for all active children across all schools.
     * Called by the weekly job. Returns the number of pulses generated.
     */
    suspend fun batchGenerateAll(weekStart: LocalDate): Int {
        val children = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.isActive eq true) and (ChildrenTable.studentCode.isNotNull())
            }.map { row ->
                BatchChild(
                    childId = row[ChildrenTable.id].value,
                    parentId = row[ChildrenTable.parentId],
                    studentCode = row[ChildrenTable.studentCode]!!,
                    schoolId = row[ChildrenTable.schoolId],
                )
            }
        }

        pulseLog.info("batchGenerateAll: {} active children with student codes", children.size)
        var count = 0
        for (child in children) {
            if (child.schoolId == null) continue
            runCatching {
                generatePulse(child.parentId, child.childId, weekStart)
            }.onSuccess { if (it != null) count++ }
                .onFailure { pulseLog.warn("batchGenerateAll: failed for child {}: {}", child.childId, it.message) }
        }
        pulseLog.info("batchGenerateAll: generated {} pulses", count)
        return count
    }

    /**
     * Fetch the latest pulse for a (parentId, studentId) pair.
     * studentId here is the children.id (parent's child record).
     */
    suspend fun getLatestPulse(parentId: UUID, childId: UUID): PulseDto? {
        // Resolve the student UUID from the child record
        val child = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and (ChildrenTable.parentId eq parentId)
            }.firstOrNull()
        } ?: return null

        val studentCode = child[ChildrenTable.studentCode] ?: return null
        val schoolId = child[ChildrenTable.schoolId] ?: return null

        val studentUuid = dbQuery {
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and (StudentsTable.studentCode eq studentCode)
            }.firstOrNull()?.get(StudentsTable.id)?.value
        } ?: childId

        val pulse = dbQuery {
            ParentPulsesTable.selectAll().where {
                (ParentPulsesTable.parentId eq parentId) and
                    (ParentPulsesTable.studentId eq studentUuid)
            }.orderBy(ParentPulsesTable.weekStartDate, SortOrder.DESC).firstOrNull()
        } ?: return null

        return rowToDto(pulse)
    }

    /**
     * Fetch pulse history for a (parentId, studentId) pair.
     */
    suspend fun getPulseHistory(parentId: UUID, childId: UUID, weeks: Int = 12): List<PulseDto> {
        val child = dbQuery {
            ChildrenTable.selectAll().where {
                (ChildrenTable.id eq childId) and (ChildrenTable.parentId eq parentId)
            }.firstOrNull()
        } ?: return emptyList()

        val studentCode = child[ChildrenTable.studentCode] ?: return emptyList()
        val schoolId = child[ChildrenTable.schoolId] ?: return emptyList()

        val studentUuid = dbQuery {
            StudentsTable.selectAll().where {
                (StudentsTable.schoolId eq schoolId) and (StudentsTable.studentCode eq studentCode)
            }.firstOrNull()?.get(StudentsTable.id)?.value
        } ?: childId

        val pulses = dbQuery {
            ParentPulsesTable.selectAll().where {
                (ParentPulsesTable.parentId eq parentId) and
                    (ParentPulsesTable.studentId eq studentUuid)
            }.orderBy(ParentPulsesTable.weekStartDate, SortOrder.DESC).limit(weeks).toList()
        }

        return pulses.map { rowToDto(it) }
    }

    // ---------- Aggregation queries ----------

    private suspend fun fetchAttendancePercentage(
        schoolId: UUID,
        studentUuid: UUID?,
        studentCode: String,
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Double? {
        if (studentUuid == null) return null
        val records = dbQuery {
            AttendanceRecordsTable.selectAll().where {
                (AttendanceRecordsTable.schoolId eq schoolId) and
                    (AttendanceRecordsTable.studentId eq studentUuid) and
                    (AttendanceRecordsTable.date greaterEq weekStart) and
                    (AttendanceRecordsTable.date lessEq weekEnd) and
                    (AttendanceRecordsTable.type eq "student")
            }.toList()
        }
        if (records.isEmpty()) return null
        val present = records.count { it[AttendanceRecordsTable.status] in listOf("present", "late") }
        return (present.toDouble() / records.size * 100).coerceIn(0.0, 100.0)
    }

    private suspend fun fetchMarks(
        schoolId: UUID,
        studentCode: String,
        studentUuid: UUID?,
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): List<MarkEntry> {
        val weekStartInstant = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val weekEndInstant = weekEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()

        // Find assessments published this week
        val assessments = dbQuery {
            AssessmentsTable.selectAll().where {
                (AssessmentsTable.schoolId eq schoolId) and
                    (AssessmentsTable.isPublished eq true) and
                    (AssessmentsTable.publishedAt greaterEq weekStartInstant) and
                    (AssessmentsTable.publishedAt lessEq weekEndInstant)
            }.toList()
        }

        if (assessments.isEmpty()) return emptyList()

        val result = mutableListOf<MarkEntry>()
        for (a in assessments) {
            val aId = a[AssessmentsTable.id].value
            // Try typed studentRef first, then legacy studentId (student_code)
            val markRow = dbQuery {
                if (studentUuid != null) {
                    AssessmentMarksTable.selectAll().where {
                        (AssessmentMarksTable.assessmentId eq aId) and
                            ((AssessmentMarksTable.studentRef eq studentUuid) or
                                (AssessmentMarksTable.studentId eq studentCode))
                    }.firstOrNull()
                } else {
                    AssessmentMarksTable.selectAll().where {
                        (AssessmentMarksTable.assessmentId eq aId) and
                            (AssessmentMarksTable.studentId eq studentCode)
                    }.firstOrNull()
                }
            } ?: continue

            val marks = markRow[AssessmentMarksTable.marks] ?: continue
            if (markRow[AssessmentMarksTable.isAbsent]) continue

            result.add(MarkEntry(
                subject = a[AssessmentsTable.subject],
                test = a[AssessmentsTable.name],
                marks = marks,
                max = a[AssessmentsTable.maxMarks],
            ))
        }
        return result
    }

    private suspend fun fetchHomeworkStatus(
        schoolId: UUID,
        studentCode: String,
        studentUuid: UUID?,
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Pair<Int, Int> {
        // Homework due this week
        val homeworkDue = dbQuery {
            HomeworkTable.selectAll().where {
                (HomeworkTable.schoolId eq schoolId) and
                    (HomeworkTable.dueDate greaterEq weekStart) and
                    (HomeworkTable.dueDate lessEq weekEnd) and
                    (HomeworkTable.isActive eq true)
            }.map { it[HomeworkTable.id].value }
        }

        if (homeworkDue.isEmpty()) return 0 to 0

        var pending = 0
        var completed = 0

        for (hwId in homeworkDue) {
            val submission = dbQuery {
                if (studentUuid != null) {
                    HomeworkSubmissionsTable.selectAll().where {
                        (HomeworkSubmissionsTable.homeworkId eq hwId) and
                            ((HomeworkSubmissionsTable.studentUuid eq studentUuid) or
                                (HomeworkSubmissionsTable.studentId eq studentCode))
                    }.firstOrNull()
                } else {
                    HomeworkSubmissionsTable.selectAll().where {
                        (HomeworkSubmissionsTable.homeworkId eq hwId) and
                            (HomeworkSubmissionsTable.studentId eq studentCode)
                    }.firstOrNull()
                }
            }

            if (submission == null) {
                pending++
            } else {
                val status = submission[HomeworkSubmissionsTable.status]
                if (status == "submitted" || status == "graded") completed++
                else pending++
            }
        }

        return pending to completed
    }

    private suspend fun fetchAnnouncementsCount(
        schoolId: UUID,
        className: String,
        weekStart: LocalDate,
        weekEnd: LocalDate,
    ): Int {
        val weekStartStr = weekStart.toString()
        val weekEndStr = weekEnd.toString()
        // Count announcements for the school this week (ALL_SCHOOL + CLASS/SECTION targeting this class)
        val count = dbQuery {
            AnnouncementsTable.selectAll().where {
                (AnnouncementsTable.schoolId eq schoolId) and
                    (AnnouncementsTable.date greaterEq weekStartStr) and
                    (AnnouncementsTable.date lessEq weekEndStr)
            }.toList()
        }
        // Filter: ALL_SCHOOL counts for everyone; CLASS/SECTION counts if it targets this class
        return count.count { row ->
            val audienceType = row[AnnouncementsTable.audienceType]
            when (audienceType) {
                "ALL_SCHOOL" -> true
                "CLASS", "SECTION" -> {
                    val filter = row[AnnouncementsTable.audienceFilter]
                    if (filter.isNullOrBlank()) true
                    else filter.contains(className, ignoreCase = true)
                }
                "STUDENT" -> {
                    val filter = row[AnnouncementsTable.audienceFilter]
                    if (filter.isNullOrBlank()) false
                    else true // Can't precisely match without parsing JSON; count it
                }
                else -> false
            }
        }
    }

    private suspend fun fetchUnreadMessages(parentId: UUID): Int {
        return dbQuery {
            MessageThreadsTable.selectAll().where {
                (MessageThreadsTable.ownerUserId eq parentId) and
                    (MessageThreadsTable.isArchived eq false)
            }.sumOf { it[MessageThreadsTable.unreadCount] }
        }
    }

    private suspend fun fetchUpcomingEvents(
        schoolId: UUID,
        weekEnd: LocalDate,
    ): List<UpcomingEvent> {
        val horizon = weekEnd.plusDays(7)
        val events = dbQuery {
            CalendarEventsTable.selectAll().where {
                (CalendarEventsTable.schoolId eq schoolId) and
                    (CalendarEventsTable.status eq "PUBLISHED") and
                    (CalendarEventsTable.startDate greater weekEnd) and
                    (CalendarEventsTable.startDate lessEq horizon) and
                    (CalendarEventsTable.isActive eq true)
            }.orderBy(CalendarEventsTable.startDate, SortOrder.ASC).limit(5).toList()
        }
        return events.map {
            UpcomingEvent(
                title = it[CalendarEventsTable.title],
                date = it[CalendarEventsTable.startDate].toString(),
            )
        }
    }

    // ---------- Trend + narrative ----------

    internal fun computeTrend(current: Double?, previous: Double?): String? {
        if (current == null || previous == null) return null
        return when {
            current > previous + 0.5 -> "up"
            current < previous - 0.5 -> "down"
            else -> "stable"
        }
    }

    internal fun buildActionableItems(
        homeworkPending: Int,
        unreadMessages: Int,
        upcomingEvents: List<UpcomingEvent>,
    ): List<String> {
        val items = mutableListOf<String>()
        if (homeworkPending > 0) items.add("$homeworkPending homework submission(s) pending")
        if (unreadMessages > 0) items.add("$unreadMessages unread message(s)")
        upcomingEvents.take(2).forEach { items.add("Upcoming: ${it.title} on ${it.date}") }
        return items
    }

    /**
     * Template-based narrative. This is the primary path now (no AI configured).
     * When AI is available later, call AiService.complete() first and fall back
     * to this on failure.
     */
    internal fun buildFallbackNarrative(
        studentName: String,
        attendancePct: Double?,
        prevAttendance: Double?,
        marks: List<MarkEntry>,
        homeworkPending: Int,
        homeworkCompleted: Int,
        unreadMessages: Int,
        upcomingEvents: List<UpcomingEvent>,
    ): String {
        val parts = mutableListOf<String>()

        // Attendance
        if (attendancePct != null) {
            val trendStr = if (prevAttendance != null) {
                when {
                    attendancePct > prevAttendance + 0.5 -> " (up from ${"%.0f".format(prevAttendance)}% last week)"
                    attendancePct < prevAttendance - 0.5 -> " (down from ${"%.0f".format(prevAttendance)}% last week)"
                    else -> " (same as last week)"
                }
            } else ""
            parts.add("${studentName}'s attendance was ${"%.0f".format(attendancePct)}%$trendStr")
        }

        // Marks highlight
        if (marks.isNotEmpty()) {
            val top = marks.maxByOrNull { it.marks / it.max }!!
            val pct = (top.marks / top.max * 100).toInt()
            parts.add("scored ${top.marks.toInt()}/${top.max} in ${top.subject} ($pct%)")
        }

        // Homework
        if (homeworkCompleted > 0 || homeworkPending > 0) {
            parts.add("$homeworkCompleted homework completed, $homeworkPending pending")
        }

        // Messages
        if (unreadMessages > 0) {
            parts.add("$unreadMessages unread message(s)")
        }

        // Upcoming events
        if (upcomingEvents.isNotEmpty()) {
            val first = upcomingEvents.first()
            parts.add("upcoming: ${first.title} on ${first.date}")
        }

        if (parts.isEmpty()) {
            return "No activity recorded for $studentName this week."
        }

        // Join into a warm, parent-friendly sentence
        return parts.joinToString(", ") + "."
    }

    // ---------- Row mapping ----------

    private fun rowToDto(row: org.jetbrains.exposed.sql.ResultRow): PulseDto {
        val marks = row[ParentPulsesTable.marksSummary]?.let {
            runCatching { json.decodeFromString(ListSerializer(MarkEntry.serializer()), it) }.getOrDefault(emptyList())
        } ?: emptyList()

        val events = row[ParentPulsesTable.upcomingEvents]?.let {
            runCatching { json.decodeFromString(ListSerializer(UpcomingEvent.serializer()), it) }.getOrDefault(emptyList())
        } ?: emptyList()

        val actionable = row[ParentPulsesTable.actionableItems]?.let {
            runCatching { json.decodeFromString(ListSerializer(String.serializer()), it) }.getOrDefault(emptyList())
        } ?: emptyList()

        val weekStart = row[ParentPulsesTable.weekStartDate]
        val weekEnd = row[ParentPulsesTable.weekEndDate]
        val weekRange = formatWeekRange(weekStart, weekEnd)

        return PulseDto(
            id = row[ParentPulsesTable.id].value.toString(),
            studentName = row[ParentPulsesTable.studentName],
            weekRange = weekRange,
            weekStartDate = weekStart.toString(),
            weekEndDate = weekEnd.toString(),
            attendancePercentage = row[ParentPulsesTable.attendancePercentage],
            attendanceTrend = row[ParentPulsesTable.attendanceTrend],
            marksSummary = marks,
            homeworkPending = row[ParentPulsesTable.homeworkPending],
            homeworkCompleted = row[ParentPulsesTable.homeworkCompleted],
            announcementsCount = row[ParentPulsesTable.announcementsCount],
            unreadMessages = row[ParentPulsesTable.unreadMessages],
            upcomingEvents = events,
            aiNarrative = row[ParentPulsesTable.aiNarrative],
            actionableItems = actionable,
            createdAt = row[ParentPulsesTable.createdAt].toString(),
        )
    }

    private suspend fun fetchPulseDto(parentId: UUID, studentId: UUID, weekStart: LocalDate): PulseDto? {
        val row = dbQuery {
            ParentPulsesTable.selectAll().where {
                (ParentPulsesTable.parentId eq parentId) and
                    (ParentPulsesTable.studentId eq studentId) and
                    (ParentPulsesTable.weekStartDate eq weekStart)
            }.firstOrNull()
        } ?: return null
        return rowToDto(row)
    }

    internal fun formatWeekRange(start: LocalDate, end: LocalDate): String {
        val startMonth = start.month.toString().lowercase().replaceFirstChar { it.uppercase() }.take(3)
        val endMonth = end.month.toString().lowercase().replaceFirstChar { it.uppercase() }.take(3)
        return if (start.month == end.month) {
            "${startMonth} ${start.dayOfMonth} - ${end.dayOfMonth}, ${end.year}"
        } else {
            "${startMonth} ${start.dayOfMonth} - ${endMonth} ${end.dayOfMonth}, ${end.year}"
        }
    }

    // ---------- Batch helper ----------

    private data class BatchChild(
        val childId: UUID,
        val parentId: UUID,
        val studentCode: String,
        val schoolId: UUID?,
    )

    companion object {
        /**
         * Compute the start of the current week (Monday).
         */
        fun currentWeekStart(today: LocalDate = LocalDate.now()): LocalDate {
            return today.with(DayOfWeek.MONDAY)
        }
    }
}
