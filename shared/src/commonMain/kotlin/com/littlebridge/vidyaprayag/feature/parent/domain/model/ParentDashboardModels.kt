package com.littlebridge.vidyaprayag.feature.parent.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// --- Dashboard ---
@Serializable
data class ParentDashboardResponse(
    val success: Boolean,
    val message: String,
    val data: ParentDashboardData
)

@Serializable
data class ParentDashboardData(
    val greeting: String,
    @SerialName("child_summary") val childSummary: ChildSummaryDto,
    val alerts: List<DashboardAlertDto>,
    @SerialName("featured_schools") val featuredSchools: List<DashboardSchoolDto>,
    @SerialName("curation_logic") val curationLogic: String
)

@Serializable
data class ChildSummaryDto(
    val id: String,
    val name: String,
    @SerialName("overall_progress") val overallProgress: Float,
    @SerialName("current_level") val currentLevel: Int,
    @SerialName("attendance_status") val attendanceStatus: String,
    @SerialName("profile_pic") val profilePic: String? = null
)

@Serializable
data class DashboardAlertDto(
    val id: String,
    val title: String,
    val value: String,
    val type: String // "CRITICAL", "INFO"
)

@Serializable
data class DashboardSchoolDto(
    val id: String,
    val name: String,
    val rating: Float,
    val location: String,
    val image: String? = null
)

// --- Career Path ---
@Serializable
data class CareerPathResponse(
    val success: Boolean,
    val message: String,
    val data: CareerPathData
)

@Serializable
data class CareerPathData(
    @SerialName("screen_config") val screenConfig: CareerScreenConfig,
    @SerialName("career_stats") val careerStats: CareerStatsDto,
    @SerialName("action_buttons") val actionButtons: CareerActionButtons
)

@Serializable
data class CareerScreenConfig(
    @SerialName("app_bar") val appBar: CareerAppBarConfig,
    val layout: CareerLayoutConfig,
    @SerialName("celebration_icon") val celebrationIcon: String,
    @SerialName("main_heading") val mainHeading: String,
    @SerialName("sub_heading_template") val subHeadingTemplate: String,
    @SerialName("footer_description") val footerDescription: String
)

@Serializable
data class CareerAppBarConfig(
    val title: String,
    @SerialName("is_floating") val isFloating: Boolean,
    @SerialName("is_transparent") val isTransparent: Boolean,
    @SerialName("background_colors") val backgroundColors: List<String>
)

@Serializable
data class CareerLayoutConfig(
    @SerialName("vertical_padding") val verticalPadding: Int,
    @SerialName("horizontal_padding") val horizontalPadding: Int,
    @SerialName("system_nav_padding") val systemNavPadding: Boolean
)

@Serializable
data class CareerStatsDto(
    @SerialName("predicted_count") val predictedCount: Int,
    @SerialName("top_match") val topMatch: CareerTopMatchDto
)

@Serializable
data class CareerTopMatchDto(
    val title: String,
    @SerialName("match_percentage") val matchPercentage: Int,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("industry_growth_label") val industryGrowthLabel: String,
    @SerialName("industry_growth_value") val industryGrowthValue: String,
    val tags: List<CareerTagDto>
)

@Serializable
data class CareerTagDto(
    val label: String,
    @SerialName("is_highlight") val isHighlight: Boolean
)

@Serializable
data class CareerActionButtons(
    val primary: CareerActionButtonDto,
    val secondary: CareerActionButtonDto
)

@Serializable
data class CareerActionButtonDto(
    val text: String,
    val action: String
)

// --- Daily Status ---
@Serializable
data class DailyStatusResponse(
    val success: Boolean,
    val message: String,
    val data: DailyStatusData
)

@Serializable
data class DailyStatusData(
    @SerialName("child_name") val childName: String,
    @SerialName("absence_alert") val absenceAlert: String? = null,
    @SerialName("attendance_percentage") val attendancePercentage: Int,
    @SerialName("attendance_note") val attendanceNote: String,
    @SerialName("topics_covered") val topicsCovered: List<TopicCoveredDto>,
    @SerialName("homework_tasks") val homeworkTasks: List<HomeworkTaskDto>,
    @SerialName("upcoming_tests") val upcomingTests: List<UpcomingTestDto>,
    @SerialName("streak_days") val streakDays: Int,
    @SerialName("streak_message") val streakMessage: String,
    @SerialName("school_message") val schoolMessage: String
)

@Serializable
data class TopicCoveredDto(
    val subject: String,
    val title: String,
    val description: String
)

@Serializable
data class HomeworkTaskDto(
    val subject: String,
    val title: String,
    val description: String,
    @SerialName("is_critical") val isCritical: Boolean
)

@Serializable
data class UpcomingTestDto(
    val month: String,
    val day: String,
    val subject: String,
    val topic: String,
    @SerialName("is_secondary") val isSecondary: Boolean
)

// --- Reports ---
@Serializable
data class ParentReportsResponse(
    val success: Boolean,
    val message: String,
    val data: ParentReportsData
)

@Serializable
data class ParentReportsData(
    @SerialName("child_name") val childName: String,
    @SerialName("term_narrative") val termNarrative: String,
    @SerialName("average_score") val averageScore: Int,
    @SerialName("global_subject_rank") val globalSubjectRank: Int,
    @SerialName("total_students") val totalStudents: Int,
    @SerialName("improvement_trend") val improvementTrend: Float,
    @SerialName("monthly_scores") val monthlyScores: List<Int>,
    @SerialName("assessment_history") val assessmentHistory: List<AssessmentItemDto>,
    @SerialName("teacher_remarks") val teacherRemarks: String,
    @SerialName("lead_instructor") val leadInstructor: String,
    @SerialName("pews_status") val pewsStatus: String,
    @SerialName("pews_alert") val pewsAlert: String,
    @SerialName("learning_streak") val learningStreak: Int,
    @SerialName("skill_trajectory") val skillTrajectory: Map<String, Int>
)

@Serializable
data class AssessmentItemDto(
    val subject: String,
    val date: String,
    val score: Int,
    @SerialName("total_score") val totalScore: Int,
    @SerialName("class_average") val classAverage: Int,
    @SerialName("icon_name") val iconName: String
)

// --- PTM Scheduling ---
@Serializable
data class PtmSchedulingResponse(
    val success: Boolean,
    val message: String,
    val data: PtmSchedulingData
)

@Serializable
data class PtmSchedulingData(
    @SerialName("selected_month") val selectedMonth: String,
    @SerialName("selected_date") val selectedDate: Int,
    @SerialName("ptm_window_days") val ptmWindowDays: List<Int>,
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("teacher_subject") val teacherSubject: String,
    @SerialName("teacher_image_url") val teacherImageUrl: String,
    val slots: List<PtmTimeSlotDto>,
    val bookings: List<PtmBookingDto>
)

@Serializable
data class PtmTimeSlotDto(
    val time: String,
    @SerialName("is_available") val isAvailable: Boolean,
    @SerialName("is_selected") val isSelected: Boolean
)

@Serializable
data class PtmBookingDto(
    val subject: String,
    val teacher: String,
    val date: String,
    val time: String,
    @SerialName("icon_name") val iconName: String
)
