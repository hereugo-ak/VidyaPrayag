// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/narrator/NarratorTools.kt
package com.littlebridge.enrollplus.feature.reportcard.narrator

import com.littlebridge.enrollplus.db.AssessmentMarksTable
import com.littlebridge.enrollplus.db.AssessmentsTable
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ParentAchievementsTable
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.db.ReportCardDraftsTable
import com.littlebridge.enrollplus.db.ReportCardTemplatesTable
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
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * 6 read-only, school-scoped tools for the Narrator Agent.
 *
 * Each tool is tenant-scoped (schoolId injected from caller context, never
 * from model output). Tools can never write — all writes stay in Tier-3.
 *
 * SOLID:
 *   S → Each tool has one responsibility (one data lookup).
 *   L → All tools substitutable for [AiService.AgentTool].
 *   I → Tools are feature-scoped, not god-interfaces.
 */
object NarratorTools {
    private val log = LoggerFactory.getLogger("NarratorTools")
    private val json = Json { prettyPrint = true }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 1: get_last_term_report
    // ──────────────────────────────────────────────────────────────────────

    object GetLastTermReport : AiService.AgentTool {
        override val name = "get_last_term_report"
        override val description = """
            Get the student's last published report card draft. Returns the
            previous term's overall summary, subject narratives, and projection.
            Use this to reference past performance and check if projections came true.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("student_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The student's UUID")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("student_id"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val studentId = (args["student_id"] as? JsonPrimitive)?.content
                ?: return errorJson("student_id required")

            val row = dbQuery {
                ReportCardDraftsTable.selectAll().where {
                    (ReportCardDraftsTable.schoolId eq schoolId) and
                    (ReportCardDraftsTable.studentId eq UUID.fromString(studentId)) and
                    (ReportCardDraftsTable.status eq "published")
                }.orderBy(ReportCardDraftsTable.createdAt, SortOrder.DESC)
                    .firstOrNull()
            } ?: return JsonObject(mapOf("found" to JsonPrimitive(false))).toString()

            return buildJsonObject {
                put("found", true)
                put("term", row[ReportCardDraftsTable.term])
                put("ai_draft", row[ReportCardDraftsTable.aiDraft] ?: "")
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 2: get_subject_trajectory
    // ──────────────────────────────────────────────────────────────────────

    object GetSubjectTrajectory : AiService.AgentTool {
        override val name = "get_subject_trajectory"
        override val description = """
            Get the student's marks trajectory across all assessments for a subject.
            Returns a list of (exam_date, percentage) pairs ordered chronologically.
            Use this to see if the student is improving or declining in a subject.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("student_code", buildJsonObject {
                    put("type", "string")
                    put("description", "The student's code (e.g. G10A-001-3)")
                })
                put("subject", buildJsonObject {
                    put("type", "string")
                    put("description", "Subject name (optional, returns all if omitted)")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("student_code"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val code = (args["student_code"] as? JsonPrimitive)?.content
                ?: return errorJson("student_code required")
            val subjectFilter = (args["subject"] as? JsonPrimitive)?.content

            val assessMeta = dbQuery {
                AssessmentsTable.selectAll().where {
                    AssessmentsTable.schoolId eq schoolId
                }.filter { it[AssessmentsTable.isPublished] }
                    .associate { it[AssessmentsTable.id].value to
                        Triple(it[AssessmentsTable.subject], it[AssessmentsTable.maxMarks], it[AssessmentsTable.examDate]) }
            }

            val marksRows = dbQuery {
                AssessmentMarksTable.selectAll().where {
                    AssessmentMarksTable.studentId eq code
                }.toList()
            }

            val bySubject = marksRows.groupBy { row ->
                assessMeta[row[AssessmentMarksTable.assessmentId]]?.first ?: "Unknown"
            }

            val result = buildJsonObject {
                bySubject.forEach { (subject, rows) ->
                    if (subjectFilter != null && !subject.equals(subjectFilter, ignoreCase = true)) return@forEach
                    val trajectory = rows.mapNotNull { row ->
                        val meta = assessMeta[row[AssessmentMarksTable.assessmentId]] ?: return@mapNotNull null
                        val max = meta.second
                        val m = row[AssessmentMarksTable.marks] ?: return@mapNotNull null
                        if (max <= 0) return@mapNotNull null
                        buildJsonObject {
                            put("date", meta.third?.toString() ?: "")
                            put("percentage", (m / max) * 100.0)
                            put("is_absent", row[AssessmentMarksTable.isAbsent])
                        }
                    }.sortedBy { it["date"]?.toString() ?: "" }
                    put(subject, JsonArray(trajectory))
                }
            }

            return result.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 3: get_pews_context
    // ──────────────────────────────────────────────────────────────────────

    object GetPewsContext : AiService.AgentTool {
        override val name = "get_pews_context"
        override val description = """
            Get the student's latest PEWS risk snapshot. Returns risk score,
            level, attendance %, marks %, slopes, and cause family.
            Use this to understand if the student is at-risk and why.
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
            val code = (args["student_code"] as? JsonPrimitive)?.content
                ?: return errorJson("student_code required")

            val row = dbQuery {
                PewsRiskSnapshotsTable.selectAll().where {
                    (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                    (PewsRiskSnapshotsTable.studentCode eq code)
                }.orderBy(PewsRiskSnapshotsTable.runDate, SortOrder.DESC)
                    .firstOrNull()
            } ?: return JsonObject(mapOf("found" to JsonPrimitive(false))).toString()

            return buildJsonObject {
                put("found", true)
                put("risk_score", row[PewsRiskSnapshotsTable.riskScore])
                put("risk_level", row[PewsRiskSnapshotsTable.riskLevel])
                put("attendance_pct", row[PewsRiskSnapshotsTable.attendancePct] ?: 0)
                put("marks_pct", row[PewsRiskSnapshotsTable.marksPct] ?: 0)
                put("attendance_slope", row[PewsRiskSnapshotsTable.attendanceSlope] ?: 0.0)
                put("marks_slope", row[PewsRiskSnapshotsTable.marksSlope] ?: 0.0)
                put("cause_family", row[PewsRiskSnapshotsTable.causeFamily] ?: "unknown")
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 4: get_exam_topic_gaps
    // ──────────────────────────────────────────────────────────────────────

    object GetExamTopicGaps : AiService.AgentTool {
        override val name = "get_exam_topic_gaps"
        override val description = """
            Get per-question or per-topic marks for a specific exam, if available.
            Returns topic-level breakdown to identify specific weak areas.
            Graceful: returns empty if no topic-level data exists.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("student_code", buildJsonObject {
                    put("type", "string")
                    put("description", "The student's code")
                })
                put("subject", buildJsonObject {
                    put("type", "string")
                    put("description", "Subject name (optional)")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("student_code"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            // Per-question marks are not yet stored in the DB.
            // Graceful: return empty — the agent will work without topic gaps.
            return JsonObject(mapOf(
                "available" to JsonPrimitive(false),
                "message" to JsonPrimitive("Topic-level marks not yet available. Use subject-level trajectory instead.")
            )).toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 5: get_board_rubric
    // ──────────────────────────────────────────────────────────────────────

    object GetBoardRubric : AiService.AgentTool {
        override val name = "get_board_rubric"
        override val description = """
            Get the grading scale (rubric) for the school's board.
            Returns a list of {min_pct, grade, descriptor} bands.
            Use this to understand what each grade means in context.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("board", buildJsonObject {
                    put("type", "string")
                    put("description", "Board name (CBSE, ICSE, IB, STATE, NEP_HPC). Optional — defaults to school's board.")
                })
            })
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val boardParam = (args["board"] as? JsonPrimitive)?.content

            val template = dbQuery {
                ReportCardTemplatesTable.selectAll().where {
                    (ReportCardTemplatesTable.board eq (boardParam ?: "CBSE")) and
                    (ReportCardTemplatesTable.isActive eq true)
                }.firstOrNull()
            }

            if (template != null) {
                return buildJsonObject {
                    put("board", template[ReportCardTemplatesTable.board])
                    put("grading_scale", template[ReportCardTemplatesTable.gradingScale])
                }.toString()
            }

            // Fallback
            return JsonObject(mapOf(
                "board" to JsonPrimitive(boardParam ?: "CBSE"),
                "message" to JsonPrimitive("Using default CBSE 9-point scale")
            )).toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 6: get_competency_badges
    // ──────────────────────────────────────────────────────────────────────

    object GetCompetencyBadges : AiService.AgentTool {
        override val name = "get_competency_badges"
        override val description = """
            Get the student's competency badges and achievements.
            Returns badges, competencies, EI metrics, and missions.
            Use this to highlight strengths in the narrative.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("student_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The student's UUID")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("student_id"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val studentIdStr = (args["student_id"] as? JsonPrimitive)?.content
                ?: return errorJson("student_id required")

            // parent_achievements uses child_id (children table), not student_id directly.
            // Best-effort: return empty — graceful.
            // In future, map student_id → children.student_code for lookup.
            return JsonObject(mapOf(
                "badges" to JsonArray(emptyList()),
                "message" to JsonPrimitive("No competency badges linked yet.")
            )).toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    /** All 6 tools as a map keyed by tool name, ready for AiService.runAgent(). */
    fun allTools(): Map<String, AiService.AgentTool> = mapOf(
        GetLastTermReport.name to GetLastTermReport,
        GetSubjectTrajectory.name to GetSubjectTrajectory,
        GetPewsContext.name to GetPewsContext,
        GetExamTopicGaps.name to GetExamTopicGaps,
        GetBoardRubric.name to GetBoardRubric,
        GetCompetencyBadges.name to GetCompetencyBadges,
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
        JsonObject(mapOf("error" to JsonPrimitive(msg))).toString()
}
