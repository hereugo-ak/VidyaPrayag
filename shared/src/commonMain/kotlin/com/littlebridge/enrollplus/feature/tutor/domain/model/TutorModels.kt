package com.littlebridge.enrollplus.feature.tutor.domain.model

import kotlinx.serialization.Serializable

// ── API Response Wrappers ────────────────────────────────────────────

@Serializable
data class TutorApiResponse<T>(
    val success: Boolean,
    val message: String = "",
    val data: T? = null,
)

// ── Subjects (for subject picker) ────────────────────────────────────

@Serializable
data class SubjectItemDto(
    val subjectId: String,
    val subjectName: String,
    val subjectCode: String,
)

@Serializable
data class SubjectsResponse(
    val success: Boolean,
    val message: String = "",
    val data: List<SubjectItemDto>? = null,
)

// ── Doubt (Tier 2 Agent) ─────────────────────────────────────────────

@Serializable
data class DoubtRequest(
    val childId: String,
    val subjectId: String = "",
    val question: String,
    val mode: String = "DOUBT",
)

@Serializable
data class DoubtResponse(
    val success: Boolean,
    val message: String = "",
    val data: DoubtResultDto? = null,
)

@Serializable
data class DoubtResultDto(
    val sessionId: String? = null,
    val turn: TutorTurnDto,
    val modelUsed: Boolean,
    val providerUsed: String? = null,
    val grounded: Boolean,
    val safetyFlag: String? = null,
)

@Serializable
data class TutorTurnDto(
    val mode: String,
    val groundedRefs: List<GroundedRefDto> = emptyList(),
    val studentFacing: StudentFacingDto,
    val practice: List<PracticeQuestionDto>? = null,
    val planDelta: PlanDeltaDto? = null,
    val teacherFlag: TeacherFlagDto? = null,
    val misconception: MisconceptionLogDto? = null,
)

@Serializable
data class GroundedRefDto(
    val topicId: String,
    val source: String,
    val value: String,
)

@Serializable
data class StudentFacingDto(
    val text: String,
    val mathBlocks: List<String> = emptyList(),
    val nextPrompt: String? = null,
)

@Serializable
data class PracticeQuestionDto(
    val questionId: String,
    val stem: String,
    val options: List<String>? = null,
    val answerKey: String,
    val topicId: String,
    val difficulty: String,
)

@Serializable
data class PlanDeltaDto(
    val addReviews: List<AddReviewDto> = emptyList(),
    val adjustDifficulty: String? = null,
)

@Serializable
data class AddReviewDto(
    val topicId: String,
    val priority: String,
)

@Serializable
data class TeacherFlagDto(
    val topicId: String,
    val reason: String,
    val severity: String,
)

@Serializable
data class MisconceptionLogDto(
    val topicId: String,
    val type: String,
    val evidence: String,
)

// ── Learner Bundle (Tier 0 Sense) ────────────────────────────────────

@Serializable
data class LearnerBundleResponse(
    val success: Boolean,
    val message: String = "",
    val data: LearnerBundleDto? = null,
)

@Serializable
data class LearnerBundleDto(
    val childId: String,
    val classId: String? = null,
    val subjectId: String,
    val academicYearId: String? = null,
    val syllabusPosition: SyllabusPositionDto,
    val performance: PerformanceDto,
    val weakTopics: List<WeakTopicDto>,
    val homeworkContext: HomeworkContextDto,
    val reviewQueue: List<ReviewItemDto>,
    val upcoming: UpcomingDto,
    val dataConfidence: DataConfidenceDto,
)

@Serializable
data class SyllabusPositionDto(
    val currentChapter: String? = null,
    val currentTopic: String? = null,
    val coveredTopicIds: List<String> = emptyList(),
    val notYetCoveredIds: List<String> = emptyList(),
)

@Serializable
data class TopicScoreDto(
    val topicId: String,
    val pct: Double,
    val attempts: Int,
    val lastAssessedOn: String? = null,
)

@Serializable
data class PerformanceDto(
    val perTopicScore: List<TopicScoreDto> = emptyList(),
)

@Serializable
data class WeakTopicDto(
    val topicId: String,
    val pct: Double,
    val severity: String,
)

@Serializable
data class HomeworkContextDto(
    val dueSoon: List<HomeworkItemDto> = emptyList(),
    val missed: List<HomeworkItemDto> = emptyList(),
    val missedOnWeakTopic: List<HomeworkItemDto> = emptyList(),
)

@Serializable
data class HomeworkItemDto(
    val homeworkId: String,
    val title: String,
    val dueDate: String? = null,
    val status: String? = null,
)

@Serializable
data class ReviewItemDto(
    val topicId: String,
    val dueAt: String,
    val stability: Double,
    val difficulty: Double,
)

@Serializable
data class UpcomingDto(
    val tests: List<UpcomingEventDto> = emptyList(),
    val events: List<UpcomingEventDto> = emptyList(),
)

@Serializable
data class UpcomingEventDto(
    val eventId: String,
    val title: String,
    val startDate: String,
    val endDate: String? = null,
    val type: String? = null,
)

@Serializable
data class DataConfidenceDto(
    val hasMarks: Boolean = false,
    val hasSyllabus: Boolean = false,
    val hasHomework: Boolean = false,
)

// ── Practice Grade (Tier 3 Act) ──────────────────────────────────────

@Serializable
data class PracticeGradeRequest(
    val childId: String,
    val subjectId: String,
    val questionId: String,
    val stem: String,
    val options: List<String>? = null,
    val answerKey: String,
    val topicId: String,
    val difficulty: String,
    val childAnswer: String,
)

@Serializable
data class PracticeGradeResponse(
    val success: Boolean,
    val message: String = "",
    val data: PracticeGradeDto? = null,
)

@Serializable
data class PracticeGradeDto(
    val correct: Boolean,
    val gradePct: Int,
    val feedback: String,
)

// ── Adaptive Plan (Tier 3 Act) ───────────────────────────────────────

@Serializable
data class PlanResponse(
    val success: Boolean,
    val message: String = "",
    val data: List<PlanItemDto>? = null,
)

@Serializable
data class PlanItemDto(
    val topicId: String,
    val dueAt: String,
    val stability: Double,
    val difficulty: Double,
    val reps: Int,
    val lapses: Int,
)

// ── Teacher Heatmap (Cross-role) ─────────────────────────────────────

@Serializable
data class TeacherScopeResponse(
    val success: Boolean,
    val message: String = "",
    val data: List<TeacherScopeItemDto>? = null,
)

@Serializable
data class TeacherScopeItemDto(
    val classId: String,
    val className: String,
    val subjectId: String,
    val subjectName: String,
)

@Serializable
data class HeatmapResponse(
    val success: Boolean,
    val message: String = "",
    val data: HeatmapDto? = null,
)

@Serializable
data class HeatmapDto(
    val classId: String,
    val subjectId: String,
    val totalChildren: Int,
    val totalMisconceptions: Int,
    val cells: List<HeatmapCellDto>,
)

@Serializable
data class HeatmapCellDto(
    val topicId: String,
    val misconceptionType: String,
    val affectedChildren: Int,
    val evidence: List<String>,
    val severity: String,
)

// ── Parent Progress (Cross-role) ─────────────────────────────────────

@Serializable
data class ProgressCardResponse(
    val success: Boolean,
    val message: String = "",
    val data: ProgressCardDto? = null,
)

@Serializable
data class ProgressCardDto(
    val childId: String,
    val subjectId: String,
    val totalDoubtsResolved: Int,
    val totalAnswersGiven: Int,
    val totalSessions: Int,
    val safetyFlags: Int,
    val topics: List<TopicProgressDto>,
)

@Serializable
data class TopicProgressDto(
    val topicId: String,
    val currentMastery: Double,
    val source: String,
    val attempts: Int,
    val correct: Int,
)

// ── Efficacy (Tier 4 Learn) ──────────────────────────────────────────

@Serializable
data class EfficacyResponse(
    val success: Boolean,
    val message: String = "",
    val data: EfficacyDto? = null,
)

@Serializable
data class EfficacyDto(
    val childId: String,
    val subjectId: String,
    val topics: List<EfficacyTopicItemDto>,
)

@Serializable
data class EfficacyTopicItemDto(
    val topicId: String,
    val mastery: Double,
    val verdict: String,
)

// ── Module Status ────────────────────────────────────────────────────

@Serializable
data class ModuleStatusResponse(
    val success: Boolean,
    val message: String = "",
    val data: ModuleStatusDto? = null,
)

@Serializable
data class ModuleStatusDto(
    val totalModules: Int,
    val modules: List<ModuleStatusItemDto>,
    val globalKillSwitch: Boolean,
)

@Serializable
data class ModuleStatusItemDto(
    val moduleName: String,
    val killSwitched: Boolean,
)
