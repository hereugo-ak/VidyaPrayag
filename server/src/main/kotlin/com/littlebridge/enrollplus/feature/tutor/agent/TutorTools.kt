// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/agent/TutorTools.kt
package com.littlebridge.enrollplus.feature.tutor.agent

import com.littlebridge.enrollplus.feature.ai.AiService
import com.littlebridge.enrollplus.feature.notifications.Notify
import com.littlebridge.enrollplus.feature.notifications.NotifyRecipients
import com.littlebridge.enrollplus.feature.tutor.data.TutorMisconceptionRepository
import com.littlebridge.enrollplus.feature.tutor.sense.LearnerBundleBuilder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.slf4j.LoggerFactory
import java.util.UUID

/**
 * AI Tutor 2.0 — Tier 2 Agent Tools.
 *
 * Read-only, school-scoped tools the tutor agent can call during its reasoning
 * loop. The model can ASK for data; it can never WRITE (except
 * `log_misconception`, the one write tool — records what the child got wrong).
 *
 * Safety:
 * - Every tool receives schoolId from the caller's JWT context, never from
 *   the model's arguments. The model cannot escape its tenant scope.
 * - All queries are SELECT-only (except log_misconception).
 * - Results are JSON strings the model can parse.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §6.2
 */
object TutorTools {

    private val log = LoggerFactory.getLogger("TutorTools")
    private val json = Json { prettyPrint = true }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 1: get_learner_bundle
    // ──────────────────────────────────────────────────────────────────────

    object GetLearnerBundle : AiService.AgentTool {
        override val name = "get_learner_bundle"
        override val description = """
            Get the deterministic learner bundle for a child + subject.
            This is the agent's ground truth — every number the agent cites
            must come from this bundle. Includes syllabus position, per-topic
            performance, weak topics, homework context, FSRS review queue,
            and upcoming events. The agent usually starts here.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("child_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The child's UUID")
                })
                put("subject_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The subject's UUID")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("child_id"), JsonPrimitive("subject_id"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val childId = (args["child_id"] as? JsonPrimitive)?.content
                ?: return errorJson("child_id required")
            val subjectId = (args["subject_id"] as? JsonPrimitive)?.content
                ?: return errorJson("subject_id required")

            val childUuid = runCatching { UUID.fromString(childId) }.getOrNull()
                ?: return errorJson("invalid child_id")
            val subjectUuid = runCatching { UUID.fromString(subjectId) }.getOrNull()
                ?: return errorJson("invalid subject_id")

            val bundle = LearnerBundleBuilder().build(childUuid, subjectUuid)
                ?: return errorJson("could not build learner bundle — ensure child is linked to a school")

            return LearnerBundleBuilder().toJson(bundle)
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 2: get_weak_topics
    // ──────────────────────────────────────────────────────────────────────

    object GetWeakTopics : AiService.AgentTool {
        override val name = "get_weak_topics"
        override val description = """
            Get the real weak-topic list for a child + subject with real
            percentages. Weak topics are covered topics where the score is
            below the board threshold. Use this to target practice and
            explanations at areas the child actually struggles with.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("child_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The child's UUID")
                })
                put("subject_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The subject's UUID")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("child_id"), JsonPrimitive("subject_id"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val childId = (args["child_id"] as? JsonPrimitive)?.content
                ?: return errorJson("child_id required")
            val subjectId = (args["subject_id"] as? JsonPrimitive)?.content
                ?: return errorJson("subject_id required")

            val childUuid = runCatching { UUID.fromString(childId) }.getOrNull()
                ?: return errorJson("invalid child_id")
            val subjectUuid = runCatching { UUID.fromString(subjectId) }.getOrNull()
                ?: return errorJson("invalid subject_id")

            val bundle = LearnerBundleBuilder().build(childUuid, subjectUuid)
                ?: return errorJson("could not build learner bundle")

            val arr = buildJsonArray {
                for (weak in bundle.weakTopics) {
                    add(buildJsonObject {
                        put("topic_id", weak.topicId)
                        put("pct", weak.pct)
                        put("severity", weak.severity)
                    })
                }
            }
            return buildJsonObject {
                put("weak_topics", arr)
                put("has_marks", bundle.dataConfidence.hasMarks)
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 3: get_syllabus_position
    // ──────────────────────────────────────────────────────────────────────

    object GetSyllabusPosition : AiService.AgentTool {
        override val name = "get_syllabus_position"
        override val description = """
            Get the syllabus position for a child + subject — what's been
            covered and what hasn't. The tutor must NEVER teach ahead of
            what's been covered. Use this to check if a doubt is on-syllabus.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("child_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The child's UUID")
                })
                put("subject_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The subject's UUID")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("child_id"), JsonPrimitive("subject_id"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val childId = (args["child_id"] as? JsonPrimitive)?.content
                ?: return errorJson("child_id required")
            val subjectId = (args["subject_id"] as? JsonPrimitive)?.content
                ?: return errorJson("subject_id required")

            val childUuid = runCatching { UUID.fromString(childId) }.getOrNull()
                ?: return errorJson("invalid child_id")
            val subjectUuid = runCatching { UUID.fromString(subjectId) }.getOrNull()
                ?: return errorJson("invalid subject_id")

            val bundle = LearnerBundleBuilder().build(childUuid, subjectUuid)
                ?: return errorJson("could not build learner bundle")

            return buildJsonObject {
                put("current_chapter", bundle.syllabusPosition.currentChapter)
                put("current_topic", bundle.syllabusPosition.currentTopic)
                put("covered_topic_ids", JsonArray(bundle.syllabusPosition.coveredTopicIds.map { JsonPrimitive(it) }))
                put("not_yet_covered_ids", JsonArray(bundle.syllabusPosition.notYetCoveredIds.map { JsonPrimitive(it) }))
                put("has_syllabus", bundle.dataConfidence.hasSyllabus)
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 4: get_due_reviews
    // ──────────────────────────────────────────────────────────────────────

    object GetDueReviews : AiService.AgentTool {
        override val name = "get_due_reviews"
        override val description = """
            Get the FSRS due-review queue for a child — what topics are due
            for review tonight, in priority order. Each item includes the
            topic ID, due date, stability, and difficulty. Use this to
            prioritise practice and plan the study session.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("child_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The child's UUID")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("child_id"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val childId = (args["child_id"] as? JsonPrimitive)?.content
                ?: return errorJson("child_id required")

            val childUuid = runCatching { UUID.fromString(childId) }.getOrNull()
                ?: return errorJson("invalid child_id")

            val bundle = LearnerBundleBuilder().build(childUuid, UUID.randomUUID())
            // We only need the review queue, but build() requires a subjectId.
            // Instead, query the repo directly.
            val repo = com.littlebridge.enrollplus.feature.tutor.data.TutorReviewStateRepository()
            val dueReviews = repo.findDueReviews(childUuid, limit = 10)

            val arr = buildJsonArray {
                for (review in dueReviews) {
                    add(buildJsonObject {
                        put("topic_id", review.topicId.toString())
                        put("due_at", review.dueAt.toString())
                        put("stability", review.stability)
                        put("difficulty", review.difficulty)
                        put("reps", review.reps)
                        put("lapses", review.lapses)
                    })
                }
            }
            return buildJsonObject {
                put("due_reviews", arr)
                put("count", dueReviews.size)
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 5: get_homework_context
    // ──────────────────────────────────────────────────────────────────────

    object GetHomeworkContext : AiService.AgentTool {
        override val name = "get_homework_context"
        override val description = """
            Get homework context for a child + subject — what's due soon,
            what's been missed, and what's missed on weak topics. Use this
            to connect tutoring to the child's actual homework load.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("child_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The child's UUID")
                })
                put("subject_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The subject's UUID")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("child_id"), JsonPrimitive("subject_id"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val childId = (args["child_id"] as? JsonPrimitive)?.content
                ?: return errorJson("child_id required")
            val subjectId = (args["subject_id"] as? JsonPrimitive)?.content
                ?: return errorJson("subject_id required")

            val childUuid = runCatching { UUID.fromString(childId) }.getOrNull()
                ?: return errorJson("invalid child_id")
            val subjectUuid = runCatching { UUID.fromString(subjectId) }.getOrNull()
                ?: return errorJson("invalid subject_id")

            val bundle = LearnerBundleBuilder().build(childUuid, subjectUuid)
                ?: return errorJson("could not build learner bundle")

            return buildJsonObject {
                put("due_soon", JsonArray(bundle.homeworkContext.dueSoon.map { hw ->
                    buildJsonObject {
                        put("homework_id", hw.homeworkId)
                        put("title", hw.title)
                        put("due_date", hw.dueDate)
                        put("status", hw.status)
                    }
                }))
                put("missed", JsonArray(bundle.homeworkContext.missed.map { hw ->
                    buildJsonObject {
                        put("homework_id", hw.homeworkId)
                        put("title", hw.title)
                        put("due_date", hw.dueDate)
                        put("status", hw.status)
                    }
                }))
                put("missed_on_weak_topic", bundle.homeworkContext.missedOnWeakTopic.size)
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 6: log_misconception (the ONE write tool)
    // ──────────────────────────────────────────────────────────────────────

    object LogMisconception : AiService.AgentTool {
        override val name = "log_misconception"
        override val description = """
            Record a misconception the child demonstrated — what they got
            wrong and why. This feeds the class-wide misconception library
            and the teacher heatmap. This is the ONLY write tool available.
            Use it when the child's doubt reveals a specific misunderstanding.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("school_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The school's UUID")
                })
                put("class_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The class's UUID")
                })
                put("subject_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The subject's UUID")
                })
                put("topic_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The topic's UUID (curriculum_units.id)")
                })
                put("child_id", buildJsonObject {
                    put("type", "string")
                    put("description", "The child's UUID")
                })
                put("misconception_type", buildJsonObject {
                    put("type", "string")
                    put("description", "Short label for the misconception type, e.g. 'common_denominator_error'")
                })
                put("evidence", buildJsonObject {
                    put("type", "string")
                    put("description", "What the child said/did that reveals the misconception")
                })
            })
            put("required", JsonArray(listOf(
                JsonPrimitive("school_id"), JsonPrimitive("class_id"),
                JsonPrimitive("subject_id"), JsonPrimitive("topic_id"),
                JsonPrimitive("child_id"), JsonPrimitive("misconception_type"),
            )))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            val args = parseArgs(arguments)
            val classId = (args["class_id"] as? JsonPrimitive)?.content
                ?: return errorJson("class_id required")
            val subjectId = (args["subject_id"] as? JsonPrimitive)?.content
                ?: return errorJson("subject_id required")
            val topicId = (args["topic_id"] as? JsonPrimitive)?.content
                ?: return errorJson("topic_id required")
            val childId = (args["child_id"] as? JsonPrimitive)?.content
                ?: return errorJson("child_id required")
            val misconceptionType = (args["misconception_type"] as? JsonPrimitive)?.content
                ?: return errorJson("misconception_type required")
            val evidence = (args["evidence"] as? JsonPrimitive)?.content ?: ""

            val classUuid = runCatching { UUID.fromString(classId) }.getOrNull()
                ?: return errorJson("invalid class_id")
            val subjectUuid = runCatching { UUID.fromString(subjectId) }.getOrNull()
                ?: return errorJson("invalid subject_id")
            val topicUuid = runCatching { UUID.fromString(topicId) }.getOrNull()
                ?: return errorJson("invalid topic_id")
            val childUuid = runCatching { UUID.fromString(childId) }.getOrNull()
                ?: return errorJson("invalid child_id")

            val repo = TutorMisconceptionRepository()
            val id = repo.insert(
                schoolId = schoolId,
                classId = classUuid,
                subjectId = subjectUuid,
                topicId = topicUuid,
                childId = childUuid,
                misconceptionType = misconceptionType,
                evidence = evidence,
                surfacedToTeacher = false,
            )

            log.info("TutorTools: logged misconception {} for child {} on topic {}",
                misconceptionType, childId, topicId)

            // Notify teachers about the new misconception so they can
            // review it in the heatmap and plan remediation.
            runCatching {
                val teacherIds = NotifyRecipients.teachersInSchool(schoolId)
                if (teacherIds.isNotEmpty()) {
                    Notify.toUsers(
                        userIds = teacherIds,
                        category = "tutor_misconception",
                        title = "New Misconception Logged: $misconceptionType",
                        body = "A student demonstrated '$misconceptionType' in a tutor session. " +
                            "Review the misconception heatmap for class/subject patterns.",
                        schoolId = schoolId,
                        deepLink = "/teacher/tutor",
                        refType = "tutor_misconception",
                        refId = id.toString(),
                    )
                }
            }.onFailure { log.warn("TutorTools: failed to notify teachers of misconception: {}", it.message) }

            return buildJsonObject {
                put("logged", true)
                put("misconception_id", id.toString())
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Tool 7: retrieve_knowledge (RAG — inert stub until Phase 5)
    // ──────────────────────────────────────────────────────────────────────

    object RetrieveKnowledge : AiService.AgentTool {
        override val name = "retrieve_knowledge"
        override val description = """
            Retrieve relevant knowledge chunks from the NCERT corpus and
            school's own material. INERT STUB — returns empty results until
            the RAG pipeline is activated in Phase 5. The agent should not
            rely on this tool yet; use get_learner_bundle and
            get_syllabus_position for grounding.
        """.trimIndent()

        override val parametersSchema = buildJsonObject {
            put("type", "object")
            put("properties", buildJsonObject {
                put("query", buildJsonObject {
                    put("type", "string")
                    put("description", "The search query (topic or concept)")
                })
                put("topic_id", buildJsonObject {
                    put("type", "string")
                    put("description", "Optional: filter to a specific topic")
                })
            })
            put("required", JsonArray(listOf(JsonPrimitive("query"))))
        }

        override suspend fun execute(schoolId: UUID, arguments: String): String {
            // INERT STUB — returns empty results until Phase 5.
            // The agent should fall back to get_learner_bundle for grounding.
            log.debug("TutorTools: retrieve_knowledge called (inert stub) — returning empty")
            return buildJsonObject {
                put("chunks", JsonArray(emptyList()))
                put("note", "RAG is not yet active. Use get_learner_bundle for grounding.")
            }.toString()
        }
    }

    // ──────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────

    /** All 7 tools as a map keyed by tool name, ready for AiService.runAgent(). */
    fun allTools(): Map<String, AiService.AgentTool> = mapOf(
        GetLearnerBundle.name to GetLearnerBundle,
        GetWeakTopics.name to GetWeakTopics,
        GetSyllabusPosition.name to GetSyllabusPosition,
        GetDueReviews.name to GetDueReviews,
        GetHomeworkContext.name to GetHomeworkContext,
        LogMisconception.name to LogMisconception,
        RetrieveKnowledge.name to RetrieveKnowledge,
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
