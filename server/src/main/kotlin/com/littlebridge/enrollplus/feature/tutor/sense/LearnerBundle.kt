// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/sense/LearnerBundle.kt
package com.littlebridge.enrollplus.feature.tutor.sense

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

/**
 * The deterministic Learner Bundle — the agent's ground truth.
 *
 * Computed in plain Kotlin/SQL, **no LLM**, cached. This is the LAW-6 firewall:
 * every number the agent cites must trace back to a field in this bundle.
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §5.1
 */
@Serializable
data class LearnerBundle(
    val childId: String,
    val classId: String? = null,
    val subjectId: String,
    val academicYearId: String? = null,
    val syllabusPosition: SyllabusPosition,
    val performance: Performance,
    val weakTopics: List<WeakTopic>,
    val homeworkContext: HomeworkContext,
    val reviewQueue: List<ReviewItem>,
    val upcoming: Upcoming,
    val dataConfidence: DataConfidence,
)

@Serializable
data class SyllabusPosition(
    val currentChapter: String? = null,
    val currentTopic: String? = null,
    val coveredTopicIds: List<String> = emptyList(),
    val notYetCoveredIds: List<String> = emptyList(),
)

@Serializable
data class TopicScore(
    val topicId: String,
    val pct: Double,
    val attempts: Int,
    val lastAssessedOn: String? = null,
)

@Serializable
data class Performance(
    val perTopicScore: List<TopicScore> = emptyList(),
)

@Serializable
data class WeakTopic(
    val topicId: String,
    val pct: Double,
    val severity: String,  // "low" | "medium" | "high"
)

@Serializable
data class HomeworkItem(
    val homeworkId: String,
    val title: String,
    val dueDate: String? = null,
    val status: String? = null,
)

@Serializable
data class HomeworkContext(
    val dueSoon: List<HomeworkItem> = emptyList(),
    val missed: List<HomeworkItem> = emptyList(),
    val missedOnWeakTopic: List<HomeworkItem> = emptyList(),
)

@Serializable
data class ReviewItem(
    val topicId: String,
    val dueAt: String,
    val stability: Double,
    val difficulty: Double,
)

@Serializable
data class Upcoming(
    val tests: List<UpcomingEvent> = emptyList(),
    val events: List<UpcomingEvent> = emptyList(),
)

@Serializable
data class UpcomingEvent(
    val eventId: String,
    val title: String,
    val startDate: String,
    val endDate: String? = null,
    val type: String? = null,
)

@Serializable
data class DataConfidence(
    val hasMarks: Boolean = false,
    val hasSyllabus: Boolean = false,
    val hasHomework: Boolean = false,
)
