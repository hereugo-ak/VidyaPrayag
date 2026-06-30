/*
 * File: CaseworkerTools.kt
 * Module: feature.pews.caseworker
 *
 * PEWS 2.0 — Tier 2 Caseworker Agent Tools.
 *
 * Six read-only, school-scoped tools the caseworker agent can call during
 * its reasoning loop. The model can ASK for data; it can never WRITE.
 * All writes stay in deterministic Tier-3 (Act).
 *
 * Safety:
 * - Every tool receives schoolId from the caller's JWT context, never from
 *   the model's arguments. The model cannot escape its tenant scope.
 * - All queries are SELECT-only — no inserts, updates, or deletes.
 * - Results are JSON strings the model can parse.
 */
package com.littlebridge.enrollplus.feature.pews.caseworker

import com.littlebridge.enrollplus.db.AcademicCalendarTable
import com.littlebridge.enrollplus.db.CalendarEventsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.HomeworkSubmissionsTable
import com.littlebridge.enrollplus.db.HomeworkTable
import com.littlebridge.enrollplus.db.MessageStatusTable
import com.littlebridge.enrollplus.db.MessagesTable
import com.littlebridge.enrollplus.db.MessageThreadsTable
import com.littlebridge.enrollplus.db.PewsInterventionsTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.db.ReportFocusEffectivenessTable
import com.littlebridge.enrollplus.db.StudentsTable
import com.littlebridge.enrollplus.feature.ai.AiService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.util.UUID

object CaseworkerTools {

    private val log = LoggerFactory.getLogger("CaseworkerTools")
    private val json = Json { prettyPrint = true }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 1: get_student_history
    // ──────────────────────────────────────────────────────────────────────

    object GetStudentHistory : AiService.AgentTool {
        override val name = "get_student_history"
        override val description = """
            Get the risk snapshot history for a student. Returns the last N
            snapshots with risk score, level, attendance, marks, signals, and
            cause family. Use this to see trends over time.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("student_code", buildJsonObject {
                    put("type", "string")
                    put("description", "The student's code (e.g. G10A-001-3)")
                })
                put("n", buildJsonObject {
                    put("type", "integer")
                    put("description", "Number of past snapshots to return (default 12)")
                    put("default", 12)
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("student_code"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val code = (args["student_code"] as? JsonPrimitive)?.content ?: return errorJson("student_code required")
            val n = (args["n"] as? JsonPrimitive)?.content?.toIntOrNull() ?: 12

            val rows = dbQuery {
                PewsRiskSnapshotsTable.selectAll().where {
                    (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                        (PewsRiskSnapshotsTable.studentCode eq code)
                }.orderBy(PewsRiskSnapshotsTable.runDate, SortOrder.DESC)
                    .limit(n).toList()
            }

            val arr = buildJsonArray {
                for (r in rows) {
                    add(buildJsonObject {
                        put("run_date", r[PewsRiskSnapshotsTable.runDate].toString())
                        put("risk_score", r[PewsRiskSnapshotsTable.riskScore])
                        put("risk_level", r[PewsRiskSnapshotsTable.riskLevel])
                        put("attendance_pct", r[PewsRiskSnapshotsTable.attendancePct])
                        put("marks_pct", r[PewsRiskSnapshotsTable.marksPct])
                        put("leave_count", r[PewsRiskSnapshotsTable.leaveCount])
                        put("confidence", r[PewsRiskSnapshotsTable.confidence])
                        put("leading_score", r[PewsRiskSnapshotsTable.leadingScore])
                        put("cause_family", r[PewsRiskSnapshotsTable.causeFamily])
                        put("ai_narrative", r[PewsRiskSnapshotsTable.aiNarrative])
                        put("ai_cause", r[PewsRiskSnapshotsTable.aiCause])
                        put("ai_recommendation", r[PewsRiskSnapshotsTable.aiRecommendation])
                    })
                }
            }
            return buildJsonObject {
                put("student_code", code)
                put("history", arr)
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 2: get_past_interventions
    // ──────────────────────────────────────────────────────────────────────

    object GetPastInterventions : AiService.AgentTool {
        override val name = "get_past_interventions"
        override val description = """
            Get all past interventions for a student — what was tried, by whom,
            the outcome, and when. Use this to avoid repeating failed actions
            and to build on what was already attempted.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("student_code", buildJsonObject {
                    put("type", "string")
                    put("description", "The student's code")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("student_code"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val code = (args["student_code"] as? JsonPrimitive)?.content ?: return errorJson("student_code required")

            val rows = dbQuery {
                PewsInterventionsTable.selectAll().where {
                    (PewsInterventionsTable.schoolId eq schoolId) and
                        (PewsInterventionsTable.studentCode eq code)
                }.orderBy(PewsInterventionsTable.openedAt, SortOrder.DESC).toList()
            }

            val arr = buildJsonArray {
                for (r in rows) {
                    add(buildJsonObject {
                        put("action_type", r[PewsInterventionsTable.actionType])
                        put("status", r[PewsInterventionsTable.status])
                        put("outcome", r[PewsInterventionsTable.outcome])
                        put("notes", r[PewsInterventionsTable.notes])
                        put("opened_at", r[PewsInterventionsTable.openedAt].toString())
                        put("resolved_at", r[PewsInterventionsTable.resolvedAt]?.toString())
                        put("urgency", r[PewsInterventionsTable.urgency])
                        put("cause_family", r[PewsInterventionsTable.causeFamily])
                    })
                }
            }
            return buildJsonObject {
                put("student_code", code)
                put("interventions", arr)
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 3: get_similar_resolved_cases
    // ──────────────────────────────────────────────────────────────────────

    object GetSimilarResolvedCases : AiService.AgentTool {
        override val name = "get_similar_resolved_cases"
        override val description = """
            Get effectiveness statistics for resolved interventions matching a
            cause family in this school. Returns which action types worked
            (improved vs unchanged vs worsened) and how many times each was
            tried. Use this to recommend actions that have historically worked
            for similar cases.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("cause_family", buildJsonObject {
                    put("type", "string")
                    put("description", "The cause family to query: attendance|academic|disengagement|wellbeing|financial|external")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("cause_family"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val causeFamily = (args["cause_family"] as? JsonPrimitive)?.content ?: return errorJson("cause_family required")

            val rows = dbQuery {
                PewsInterventionsTable.selectAll().where {
                    (PewsInterventionsTable.schoolId eq schoolId) and
                        (PewsInterventionsTable.causeFamily eq causeFamily) and
                        (PewsInterventionsTable.status inList listOf("done", "dismissed"))
                }.toList()
            }

            val byAction = rows.groupBy { it[PewsInterventionsTable.actionType] }
            val statsArr = buildJsonArray {
                for ((action, group) in byAction) {
                    val improved = group.count { it[PewsInterventionsTable.outcome] == "improved" }
                    val unchanged = group.count { it[PewsInterventionsTable.outcome] == "unchanged" }
                    val worsened = group.count { it[PewsInterventionsTable.outcome] == "worsened" }
                    val total = group.size
                    add(buildJsonObject {
                        put("action_type", action)
                        put("n_tried", total)
                        put("n_improved", improved)
                        put("n_unchanged", unchanged)
                        put("n_worsened", worsened)
                        put("improve_rate", if (total > 0) String.format("%.2f", improved.toDouble() / total) else "0.0")
                    })
                }
            }
            return buildJsonObject {
                put("cause_family", causeFamily)
                put("effectiveness", statsArr)
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 4: get_calendar_context
    // ──────────────────────────────────────────────────────────────────────

    object GetCalendarContext : AiService.AgentTool {
        override val name = "get_calendar_context"
        override val description = """
            Get calendar context for a date — whether it's exam week, a holiday,
            or a school event. Use this to time interventions appropriately
            (e.g. don't nag parents during exam week).
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("date", buildJsonObject {
                    put("type", "string")
                    put("description", "Date in YYYY-MM-DD format (default: today)")
                })
            })
            put("required", JsonArray(emptyList()))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val dateStr = (args["date"] as? JsonPrimitive)?.content
            val date = try { dateStr?.let { LocalDate.parse(it) } ?: LocalDate.now() } catch (e: Exception) { LocalDate.now() }

            val events = dbQuery {
                CalendarEventsTable.selectAll().where {
                    (CalendarEventsTable.schoolId eq schoolId) and
                        (CalendarEventsTable.status eq "PUBLISHED") and
                        (CalendarEventsTable.isActive eq true) and
                        (CalendarEventsTable.startDate lessEq date) and
                        (CalendarEventsTable.endDate greaterEq date)
                }.toList()
            }

            val legacyCalendar = dbQuery {
                AcademicCalendarTable.selectAll().where {
                    (AcademicCalendarTable.schoolId eq schoolId) and
                        (AcademicCalendarTable.date eq date.toString())
                }.toList()
            }

            val eventsArr = buildJsonArray {
                for (e in events) {
                    add(buildJsonObject {
                        put("title", e[CalendarEventsTable.title])
                        put("type", e[CalendarEventsTable.type])
                        put("start_date", e[CalendarEventsTable.startDate].toString())
                        put("end_date", e[CalendarEventsTable.endDate].toString())
                        put("is_milestone", e[CalendarEventsTable.isMilestone])
                        put("audience", e[CalendarEventsTable.audience])
                    })
                }
                for (c in legacyCalendar) {
                    add(buildJsonObject {
                        put("title", c[AcademicCalendarTable.eventTitle])
                        put("type", if (c[AcademicCalendarTable.isHoliday]) "HOLIDAY" else "SCHOOL_EVENT")
                        put("date", c[AcademicCalendarTable.date])
                        put("is_holiday", c[AcademicCalendarTable.isHoliday])
                    })
                }
            }

            val isExamWeek = events.any { it[CalendarEventsTable.type] == "EXAM" }
            val isHoliday = events.any { it[CalendarEventsTable.type] == "HOLIDAY" } ||
                legacyCalendar.any { it[AcademicCalendarTable.isHoliday] }

            return buildJsonObject {
                put("date", date.toString())
                put("is_exam_week", isExamWeek)
                put("is_holiday", isHoliday)
                put("events", eventsArr)
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 5: get_parent_responsiveness
    // ──────────────────────────────────────────────────────────────────────

    object GetParentResponsiveness : AiService.AgentTool {
        override val name = "get_parent_responsiveness"
        override val description = """
            Get parent communication responsiveness for a student — how many
            messages were sent, delivered, and read, and the read rate. Use
            this to choose the best communication channel and to gauge
            parent engagement.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("student_code", buildJsonObject {
                    put("type", "string")
                    put("description", "The student's code")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("student_code"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val code = (args["student_code"] as? JsonPrimitive)?.content ?: return errorJson("student_code required")

            val student = dbQuery {
                StudentsTable.selectAll().where {
                    (StudentsTable.schoolId eq schoolId) and
                        (StudentsTable.studentCode eq code)
                }.singleOrNull()
            }

            if (student == null) {
                return buildJsonObject {
                    put("student_code", code)
                    put("error", "student not found")
                }.toString()
            }

            val parentPhone = student[StudentsTable.parentPhone]
            val className = student[StudentsTable.className]
            val section = student[StudentsTable.section]

            val threads = dbQuery {
                MessageThreadsTable.selectAll().where {
                    (MessageThreadsTable.schoolId eq schoolId) and
                        (MessageThreadsTable.senderName like "%${student[StudentsTable.fullName].split(" ").firstOrNull() ?: ""}%")
                }.toList()
            }

            val totalMessages = threads.size
            val unreadCount = threads.sumOf { it[MessageThreadsTable.unreadCount] }
            val readThreads = threads.count { it[MessageThreadsTable.isRead] }

            val messageStatuses = if (threads.isNotEmpty()) {
                val threadIds = threads.map { it[MessageThreadsTable.id].value }
                dbQuery {
                    MessageStatusTable.selectAll().where {
                        MessageStatusTable.messageId inList threadIds
                    }.toList()
                }
            } else emptyList()

            val sentCount = messageStatuses.size
            val readCount = messageStatuses.count { it[MessageStatusTable.status] == "READ" }
            val deliveredCount = messageStatuses.count { it[MessageStatusTable.status] == "DELIVERED" }
            val readRate = if (sentCount > 0) String.format("%.2f", readCount.toDouble() / sentCount) else "N/A"

            return buildJsonObject {
                put("student_code", code)
                put("student_name", student[StudentsTable.fullName])
                put("class", className)
                put("section", section)
                put("parent_phone_present", parentPhone != null)
                put("threads_total", totalMessages)
                put("threads_read", readThreads)
                put("threads_unread", unreadCount)
                put("messages_sent", sentCount)
                put("messages_delivered", deliveredCount)
                put("messages_read", readCount)
                put("read_rate", readRate)
                put("responsiveness_level", when {
                    sentCount == 0 -> "no_contact"
                    readRate == "N/A" -> "unknown"
                    sentCount > 0 && readCount.toDouble() / sentCount >= 0.7 -> "responsive"
                    sentCount > 0 && readCount.toDouble() / sentCount >= 0.3 -> "moderate"
                    else -> "low"
                })
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 6: get_homework_detail
    // ──────────────────────────────────────────────────────────────────────

    object GetHomeworkDetail : AiService.AgentTool {
        override val name = "get_homework_detail"
        override val description = """
            Get homework submission details for a student — which subjects have
            missed submissions, late submissions, and the non-submission streak.
            Use this to identify specific subjects where the student is
            disengaging.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("student_code", buildJsonObject {
                    put("type", "string")
                    put("description", "The student's code")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("student_code"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val code = (args["student_code"] as? JsonPrimitive)?.content ?: return errorJson("student_code required")

            val homeworkRows = dbQuery {
                HomeworkTable.selectAll().where {
                    (HomeworkTable.schoolId eq schoolId) and
                        (HomeworkTable.isActive eq true)
                }.toList()
            }

            val homeworkIds = homeworkRows.map { it[HomeworkTable.id].value }
            if (homeworkIds.isEmpty()) {
                return buildJsonObject {
                    put("student_code", code)
                    put("submissions", JsonArray(emptyList()))
                    put("summary", buildJsonObject {
                        put("total_assigned", 0)
                        put("total_submitted", 0)
                        put("total_not_submitted", 0)
                        put("total_late", 0)
                        put("non_submission_rate", "N/A")
                    })
                }.toString()
            }

            val submissions = dbQuery {
                HomeworkSubmissionsTable.selectAll().where {
                    (HomeworkSubmissionsTable.homeworkId inList homeworkIds) and
                        (HomeworkSubmissionsTable.studentId eq code)
                }.toList()
            }

            val hwById = homeworkRows.associateBy { it[HomeworkTable.id].value }
            val bySubject = HashMap<String, MutableList<Map<String, Any?>>>()
            for (s in submissions) {
                val hwId = s[HomeworkSubmissionsTable.homeworkId]
                val hw = hwById[hwId] ?: continue
                val subject = hw[HomeworkTable.subject]
                val status = s[HomeworkSubmissionsTable.status]
                bySubject.getOrPut(subject) { mutableListOf() }.add(mapOf(
                    "title" to hw[HomeworkTable.title],
                    "due_date" to hw[HomeworkTable.dueDate].toString(),
                    "status" to status,
                    "submitted_at" to s[HomeworkSubmissionsTable.submittedAt]?.toString(),
                    "grade" to s[HomeworkSubmissionsTable.grade],
                ))
            }

            val subjectsArr = buildJsonArray {
                for ((subject, items) in bySubject.toSortedMap()) {
                    val total = items.size
                    val submitted = items.count { it["status"] == "submitted" }
                    val late = items.count { it["status"] == "late" }
                    val notSubmitted = items.count { it["status"] == "not_submitted" }
                    add(buildJsonObject {
                        put("subject", subject)
                        put("total", total)
                        put("submitted", submitted)
                        put("late", late)
                        put("not_submitted", notSubmitted)
                        put("non_submission_rate", if (total > 0) String.format("%.2f", notSubmitted.toDouble() / total) else "N/A")
                    })
                }
            }

            val totalAssigned = homeworkIds.size
            val totalSubmitted = submissions.count { it[HomeworkSubmissionsTable.status] == "submitted" }
            val totalLate = submissions.count { it[HomeworkSubmissionsTable.status] == "late" }
            val totalNotSubmitted = totalAssigned - totalSubmitted - totalLate

            return buildJsonObject {
                put("student_code", code)
                put("subjects", subjectsArr)
                put("summary", buildJsonObject {
                    put("total_assigned", totalAssigned)
                    put("total_submitted", totalSubmitted)
                    put("total_not_submitted", totalNotSubmitted)
                    put("total_late", totalLate)
                    put("non_submission_rate", if (totalAssigned > 0) String.format("%.2f", totalNotSubmitted.toDouble() / totalAssigned) else "N/A")
                })
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 7: get_report_card_focus_priors
    // ──────────────────────────────────────────────────────────────────────

    object GetReportCardFocusPriors : AiService.AgentTool {
        override val name = "get_report_card_focus_priors"
        override val description = """
            Get effectiveness data from AI Report Card focus-area recommendations.
            Returns which focus areas (attendance, academic_consistency, foundational_skills,
            engagement) have historically led to student improvement when targeted.
            Use this to prioritise interventions on areas that have proven effective
            for this school.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("focus_area", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional: filter to a specific focus area. If omitted, returns all.")
                })
            })
            put("required", JsonArray(emptyList()))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val focusAreaFilter = (args["focus_area"] as? JsonPrimitive)?.content

            val rows = dbQuery {
                ReportFocusEffectivenessTable.selectAll().where {
                    val base = ReportFocusEffectivenessTable.schoolId eq schoolId
                    if (focusAreaFilter != null) {
                        base and (ReportFocusEffectivenessTable.focusArea eq focusAreaFilter)
                    } else {
                        base
                    }
                }.orderBy(ReportFocusEffectivenessTable.effectivenessScore, SortOrder.DESC).toList()
            }

            val arr = buildJsonArray {
                for (r in rows) {
                    add(buildJsonObject {
                        put("focus_area", r[ReportFocusEffectivenessTable.focusArea])
                        put("term", r[ReportFocusEffectivenessTable.term])
                        put("students_targeted", r[ReportFocusEffectivenessTable.studentsTargeted])
                        put("students_improved", r[ReportFocusEffectivenessTable.studentsImproved])
                        put("effectiveness_score", String.format("%.2f", r[ReportFocusEffectivenessTable.effectivenessScore]))
                        put("avg_delta", String.format("%.1f", r[ReportFocusEffectivenessTable.avgDelta]))
                        put("confidence", r[ReportFocusEffectivenessTable.confidence])
                    })
                }
            }

            return buildJsonObject {
                put("focus_priors", arr)
                put("summary", buildJsonObject {
                    put("total_focus_areas", rows.size)
                    put("best_area", rows.firstOrNull()?.get(ReportFocusEffectivenessTable.focusArea) ?: "none")
                    put("best_score", rows.firstOrNull()?.let {
                        String.format("%.2f", it[ReportFocusEffectivenessTable.effectivenessScore])
                    } ?: "N/A")
                })
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    /** All 7 tools as a map keyed by tool name, ready for AiService.runAgent(). */
    fun allTools(): Map<String, AiService.AgentTool> = mapOf(
        GetStudentHistory.name to GetStudentHistory,
        GetPastInterventions.name to GetPastInterventions,
        GetSimilarResolvedCases.name to GetSimilarResolvedCases,
        GetCalendarContext.name to GetCalendarContext,
        GetParentResponsiveness.name to GetParentResponsiveness,
        GetHomeworkDetail.name to GetHomeworkDetail,
        GetReportCardFocusPriors.name to GetReportCardFocusPriors,
    )

    private fun parseArgs(raw: String): Map<String, kotlinx.serialization.json.JsonElement> {
        return try {
            val obj = json.parseToJsonElement(raw) as JsonObject
            obj.toMap()
        } catch (e: Exception) {
            log.warn("Failed to parse tool arguments: {}", e.message)
            emptyMap()
        }
    }

    private fun errorJson(msg: String): String =
        buildJsonObject { put("error", msg) }.toString()
}
