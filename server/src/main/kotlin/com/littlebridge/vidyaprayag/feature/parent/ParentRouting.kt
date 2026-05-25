package com.littlebridge.vidyaprayag.feature.parent

import com.littlebridge.vidyaprayag.core.*
import com.littlebridge.vidyaprayag.db.*
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.exposed.sql.*
import java.time.LocalTime
import java.util.*

// --- TRACK PROGRESS ---
@Serializable
data class AchievementBadgeDto(
    val title: String,
    @SerialName("icon_name") val iconName: String,
    @SerialName("is_locked") val isLocked: Boolean,
    @SerialName("gradient_colors") val gradientColors: List<Long>
)

@Serializable
data class AcademicCompetencyDto(
    val title: String,
    @SerialName("icon_name") val iconName: String,
    val progress: Float
)

@Serializable
data class PlayIndicatorDto(
    val title: String,
    val description: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("is_met") val isMet: Boolean
)

@Serializable
data class TrackProgressDataDto(
    @SerialName("child_name") val childName: String,
    @SerialName("overall_progress") val overallProgress: Float,
    @SerialName("current_level") val currentLevel: Int,
    @SerialName("journey_description") val journeyDescription: String,
    val badges: List<AchievementBadgeDto>,
    @SerialName("academic_competencies") val academicCompetencies: List<AcademicCompetencyDto>,
    @SerialName("emotional_intelligence") val emotionalIntelligence: Map<String, Float>,
    @SerialName("play_indicators") val playIndicators: List<PlayIndicatorDto>
)

// --- FEES ---
@Serializable
data class FeeAnnouncementDto(
    val id: String,
    val title: String,
    val time: String,
    val description: String,
    @SerialName("open_rate") val openRate: String,
    val engagement: String,
    val type: String
)

@Serializable
data class FeeDataDto(
    @SerialName("total_collected") val totalCollected: String,
    @SerialName("collection_progress") val collectionProgress: Float,
    @SerialName("outstanding_fees") val outstandingFees: String,
    @SerialName("overdue_count") val overdueCount: Int,
    val announcements: List<FeeAnnouncementDto>
)

// --- SCHOLARSHIPS ---
@Serializable
data class ScholarshipDto(
    val id: String,
    val title: String,
    val description: String,
    val amount: String,
    @SerialName("time_left") val timeLeft: String,
    val category: String,
    @SerialName("is_critical") val isCritical: Boolean = false
)

@Serializable
data class ScholarshipApplicationDto(
    val id: String,
    val institution: String,
    val program: String,
    val status: String,
    @SerialName("icon_name") val iconName: String
)

@Serializable
data class ScholarshipsDataDto(
    val scholarships: List<ScholarshipDto>,
    val applications: List<ScholarshipApplicationDto>,
    @SerialName("profile_strength") val profileStrength: Int,
    @SerialName("streak_days") val streakDays: Int,
    @SerialName("current_level") val currentLevel: Int
)

// --- ANNOUNCEMENTS ---
@Serializable
data class ParentAnnouncementDto(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val category: String,
    @SerialName("is_featured") val isFeatured: Boolean = false,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
data class ParentAnnouncementsDataDto(
    val announcements: List<ParentAnnouncementDto>,
    @SerialName("is_whatsapp_sync_enabled") val isWhatsAppSyncEnabled: Boolean
)

// --- DASHBOARD ---
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
    val type: String
)

@Serializable
data class FeaturedSchoolDto(
    val id: String,
    val name: String,
    val rating: Float,
    val location: String,
    val image: String? = null
)

@Serializable
data class ParentDashboardDataDto(
    val greeting: String,
    @SerialName("child_summary") val childSummary: ChildSummaryDto,
    val alerts: List<DashboardAlertDto>,
    @SerialName("featured_schools") val featuredSchools: List<FeaturedSchoolDto>,
    @SerialName("curation_logic") val curationLogic: String
)

// --- CAREER PATH ---
@Serializable
data class CareerTagDto(val label: String, @SerialName("is_highlight") val isHighlight: Boolean)

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
data class CareerStatsDto(
    @SerialName("predicted_count") val predictedCount: Int,
    @SerialName("top_match") val topMatch: CareerTopMatchDto
)

@Serializable
data class CareerAppBarDto(val title: String, @SerialName("is_floating") val isFloating: Boolean, @SerialName("is_transparent") val isTransparent: Boolean, @SerialName("background_colors") val backgroundColors: List<String>)

@Serializable
data class CareerLayoutDto(@SerialName("vertical_padding") val verticalPadding: Int, @SerialName("horizontal_padding") val horizontalPadding: Int, @SerialName("system_nav_padding") val systemNavPadding: Boolean)

@Serializable
data class CareerScreenConfigDto(@SerialName("app_bar") val appBar: CareerAppBarDto, val layout: CareerLayoutDto, @SerialName("celebration_icon") val celebrationIcon: String, @SerialName("main_heading") val mainHeading: String, @SerialName("sub_heading_template") val subHeadingTemplate: String, @SerialName("footer_description") val footerDescription: String)

@Serializable
data class CareerActionButtonDto(val text: String, val action: String)

@Serializable
data class CareerActionButtonsDto(val primary: CareerActionButtonDto, val secondary: CareerActionButtonDto)

@Serializable
data class CareerPathDataDto(
    @SerialName("screen_config") val screenConfig: CareerScreenConfigDto,
    @SerialName("career_stats") val careerStats: CareerStatsDto,
    @SerialName("action_buttons") val actionButtons: CareerActionButtonsDto
)

// --- DAILY STATUS ---
@Serializable
data class TopicCoveredDto(val subject: String, val title: String, val description: String)

@Serializable
data class HomeworkTaskDto(val subject: String, val title: String, val description: String, @SerialName("is_critical") val isCritical: Boolean)

@Serializable
data class UpcomingTestDto(val month: String, val day: String, val subject: String, val topic: String, @SerialName("is_secondary") val isSecondary: Boolean)

@Serializable
data class DailyStatusDataDto(
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

// --- REPORTS ---
@Serializable
data class ReportAssessmentItemDto(
    val subject: String,
    val date: String,
    val score: Int,
    @SerialName("total_score") val totalScore: Int,
    @SerialName("class_average") val classAverage: Int,
    @SerialName("icon_name") val iconName: String
)

@Serializable
data class ParentReportsDataDto(
    @SerialName("child_name") val childName: String,
    @SerialName("term_narrative") val termNarrative: String,
    @SerialName("average_score") val averageScore: Int,
    @SerialName("global_subject_rank") val globalSubjectRank: Int,
    @SerialName("total_students") val totalStudents: Int,
    @SerialName("improvement_trend") val improvementTrend: Float,
    @SerialName("monthly_scores") val monthlyScores: List<Int>,
    @SerialName("assessment_history") val assessmentHistory: List<ReportAssessmentItemDto>,
    @SerialName("teacher_remarks") val teacherRemarks: String,
    @SerialName("lead_instructor") val leadInstructor: String,
    @SerialName("pews_status") val pewsStatus: String,
    @SerialName("pews_alert") val pewsAlert: String,
    @SerialName("learning_streak") val learningStreak: Int,
    @SerialName("skill_trajectory") val skillTrajectory: Map<String, Int>
)

// --- PTM ---
@Serializable
data class PtmTimeSlotDto(val time: String, @SerialName("is_available") val isAvailable: Boolean, @SerialName("is_selected") val isSelected: Boolean)

@Serializable
data class PtmBookingDto(val subject: String, val teacher: String, val date: String, val time: String, @SerialName("icon_name") val iconName: String)

@Serializable
data class PtmSchedulingDataDto(
    @SerialName("selected_month") val selectedMonth: String,
    @SerialName("selected_date") val selectedDate: Int,
    @SerialName("ptm_window_days") val ptmWindowDays: List<Int>,
    @SerialName("teacher_name") val teacherName: String,
    @SerialName("teacher_subject") val teacherSubject: String,
    @SerialName("teacher_image_url") val teacherImageUrl: String,
    val slots: List<PtmTimeSlotDto>,
    val bookings: List<PtmBookingDto>
)

// --- ONBOARDING DTOs ---
@Serializable
data class OnboardingScreenConfigDto(
    @SerialName("header_title") val headerTitle: String,
    @SerialName("header_subtitle") val headerSubtitle: String,
    @SerialName("progress_label") val progressLabel: String,
    @SerialName("progress_value") val progressValue: Double
)

@Serializable
data class OnboardingMetadataResponseDto(
    @SerialName("screen_config") val screenConfig: OnboardingScreenConfigDto,
    @SerialName("available_grades") val availableGrades: List<String>,
    @SerialName("available_interests") val availableInterests: List<String>,
    @SerialName("footer_text") val footerText: String
)

@Serializable
data class OnboardingChildInfoRequestDto(
    @SerialName("child_name") val childName: String,
    @SerialName("date_of_birth") val dateOfBirth: String? = null,
    val gender: String? = null,
    @SerialName("current_grade") val currentGrade: String? = null,
    val interests: List<String> = emptyList()
)

@Serializable
data class OnboardingChildInfoResponseDto(
    @SerialName("child_id") val childId: String,
    @SerialName("child_name") val childName: String,
    @SerialName("next_step") val nextStep: String
)

@Serializable
data class OnboardingFocusAreaDto(val id: String, val title: String, val icon: String)

@Serializable
data class OnboardingBudgetConfigDto(@SerialName("min_value") val minValue: Int, @SerialName("max_value") val maxValue: Int, @SerialName("default_range") val defaultRange: List<Int>, @SerialName("currency_symbol") val currencySymbol: String)

@Serializable
data class OnboardingPreferencesResponseDto(@SerialName("available_boards") val availableBoards: List<String>, @SerialName("available_focus_areas") val availableFocusAreas: List<OnboardingFocusAreaDto>, @SerialName("budget_config") val budgetConfig: OnboardingBudgetConfigDto)

// --- HELPERS ---
private fun timeOfDayGreeting(): String {
    val hour = LocalTime.now().hour
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else      -> "Good Evening"
    }
}
private fun firstName(full: String?): String = full?.trim()?.split(" ")?.firstOrNull().orEmpty()
private fun formatMoney(amount: Double, currency: String): String {
    val symbol = when (currency.uppercase()) {
        "USD" -> "$"
        "INR" -> "₹"
        "EUR" -> "€"
        "GBP" -> "£"
        else  -> "$"
    }
    return "$symbol${"%,d".format(amount.toLong())}"
}

// --- MAIN ROUTING ---
fun Route.parentRouting() {
    authenticate("jwt") {
        route("/api/v1/parent") {
            
            get("/dashboard") {
                val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                    call.fail("Invalid token", HttpStatusCode.Unauthorized); return@get
                }
                val nameClaim = call.principalName()
                val payload = dbQuery {
                    val user = AppUsersTable.selectAll().where { AppUsersTable.id eq uid }.singleOrNull()
                    val displayFirstName = firstName(nameClaim ?: user?.get(AppUsersTable.fullName))
                    val greeting = if (displayFirstName.isBlank()) timeOfDayGreeting() else "${timeOfDayGreeting()}, $displayFirstName"
                    val childRow = ChildrenTable.selectAll().where { (ChildrenTable.parentId eq uid) and (ChildrenTable.isActive eq true) }.orderBy(ChildrenTable.createdAt, SortOrder.ASC).firstOrNull()
                    val childSummary = childRow?.let {
                        ChildSummaryDto(it[ChildrenTable.id].value.toString(), it[ChildrenTable.childName], (it[ChildrenTable.overallProgress]).toFloat(), it[ChildrenTable.currentLevel], it[ChildrenTable.attendanceStatus], it[ChildrenTable.profilePic])
                    } ?: ChildSummaryDto("0", "Aarav", 0.75f, 4, "PRESENT")
                    val alerts = listOf(DashboardAlertDto("fees_overdue", "Fees Due", "$1,200", "CRITICAL"))
                    val schools = SchoolsTable.selectAll().where { SchoolsTable.isActive eq true }.limit(5).map {
                        FeaturedSchoolDto(it[SchoolsTable.id].value.toString(), it[SchoolsTable.name], 4.5f, it[SchoolsTable.city], it[SchoolsTable.logoUrl])
                    }
                    ParentDashboardDataDto(greeting, childSummary, alerts, schools, "Curation aligned with NEP 2020.")
                }
                call.ok(payload, message = "Dashboard fetched")
            }

            get("/track-progress") {
                val data = TrackProgressDataDto(
                    childName = "Aarav",
                    overallProgress = 0.75f,
                    currentLevel = 4,
                    journeyDescription = "Developmental milestones on track for Term 2",
                    badges = listOf(
                        AchievementBadgeDto("Social Star", "workspace_premium", false, listOf(0xFFB6C7EB, 0xFF006C49)),
                        AchievementBadgeDto("Book Worm", "auto_stories", false, listOf(0xFFCBDBF5, 0xFF8293B5)),
                        AchievementBadgeDto("Fast Learner", "rocket_launch", false, listOf(0xFF4EDE93, 0xFF006C49))
                    ),
                    academicCompetencies = listOf(
                        AcademicCompetencyDto("Literacy", "translate", 0.85f),
                        AcademicCompetencyDto("Numeracy", "calculate", 0.70f)
                    ),
                    emotionalIntelligence = mapOf("Empathy" to 0.8f, "Resilience" to 0.7f, "Social" to 0.9f),
                    playIndicators = listOf(
                        PlayIndicatorDto("Creative Expression", "Uses diverse materials", "https://lh3.googleusercontent.com/aida-public/AB6AXuBJ0iy3QHsYrDK9vkmt05wDdmHmpgT8gBlcip2cJxtHhEZh8aRcsRMENEot_fma9PHySR3i7uOBCkzywjgrnyRweoIcsAippP8X0A0wqcgX-r5pfZvIL5UF_FG0Q8N_eb8FdFdPyQ48xEiykqbtT-Uh3PpA4KeOf2vv6fzHKyIidF-Y8ldvErlwE50_WVwRhhK7TMiQuKDOR9LRFN7cqu9v5ygC0nl9_0IMd4GuMkFoiDefldCGJStlfH48L5RIjTUZfLrJ-EITce_3", true)
                    )
                )
                call.ok(data, message = "Track progress data fetched")
            }

            get("/fees") {
                val data = FeeDataDto(
                    totalCollected = "$428,500",
                    collectionProgress = 0.85f,
                    outstandingFees = "$72,120",
                    overdueCount = 145,
                    announcements = listOf(
                        FeeAnnouncementDto("1", "Fee Submission Deadline", "Yesterday", "Final reminder for Q3 fees.", "62%", "3", "Payment")
                    )
                )
                call.ok(data, message = "Fees data fetched")
            }

            get("/scholarships") {
                val data = ScholarshipsDataDto(
                    scholarships = listOf(
                        ScholarshipDto("1", "Global Excellence STEM Award 2024", "Engineering focus.", "$45,000", "3d : 12h", "Full Funding", true)
                    ),
                    applications = listOf(
                        ScholarshipApplicationDto("1", "University of Applied Sciences", "B.Arch", "Shortlisted", "architecture")
                    ),
                    profileStrength = 85,
                    streakDays = 3,
                    currentLevel = 4
                )
                call.ok(data, message = "Scholarships data fetched")
            }

            get("/announcements") {
                val data = ParentAnnouncementsDataDto(
                    announcements = listOf(
                        ParentAnnouncementDto("1", "Annual Sports Day 2024", "Join us for athletic excellence.", "Oct 24, 2023", "Events", true, "https://lh3.googleusercontent.com/aida-public/AB6AXuBJ0iy3QHsYrDK9vkmt05wDdmHmpgT8gBlcip2cJxtHhEZh8aRcsRMENEot_fma9PHySR3i7uOBCkzywjgrnyRweoIcsAippP8X0A0wqcgX-r5pfZvIL5UF_FG0Q8N_eb8FdFdPyQ48xEiykqbtT-Uh3PpA4KeOf2vv6fzHKyIidF-Y8ldvErlwE50_WVwRhhK7TMiQuKDOR9LRFN7cqu9v5ygC0nl9_0IMd4GuMkFoiDefldCGJStlfH48L5RIjTUZfLrJ-EITce_3")
                    ),
                    isWhatsAppSyncEnabled = true
                )
                call.ok(data, message = "Announcements data fetched")
            }

            get("/career-path") {
                val data = CareerPathDataDto(
                    screenConfig = CareerScreenConfigDto(
                        appBar = CareerAppBarDto("Future Roadmap", true, true, listOf("#10B9811A", "#FFFFFF")),
                        layout = CareerLayoutDto(24, 24, true),
                        celebrationIcon = "verified",
                        mainHeading = "Career Insight!",
                        subHeadingTemplate = "We've found {count} career paths matching {name}'s profile!",
                        footerDescription = "We've analyzed thousands of data points to predict the perfect fit."
                    ),
                    careerStats = CareerStatsDto(
                        predictedCount = 12,
                        topMatch = CareerTopMatchDto("Aerospace Engineering", 98, "https://lh3.googleusercontent.com/aida-public/AB6AXuCRHBDXZqXjScvsWfe04wMOkXm0iLVFISpCreRVHsvmAdATV5-07X8DFkoQA3eq3_LQSVJwxq4Fhe55Iw5jMr74EN4JV3HOx20G2cr-38dGzZCxnzwNyC87XWd8BiFYNn5io4mPYa0xr6-ZQKxIIH_u8ZnAga7QnnlViM5ykSHGmq800q6fR4tIekTK-MTSyKmACFc3S4IP_vCIgYfxcteuqCEaA84-z5YTcEukwMXb77cS_Efnp4kDUJVM7ZU3Hm3OLckYvKZWioiP", "High Growth Industry", "Global Market", listOf(CareerTagDto("STEM Focus", true), CareerTagDto("Innovation", false)))
                    ),
                    actionButtons = CareerActionButtonsDto(CareerActionButtonDto("Explore Detailed Roadmap", "OPEN_DETAILS"), CareerActionButtonDto("Recalibrate Profile", "RESTART_ONBOARDING"))
                )
                call.ok(data, message = "Career path data fetched")
            }

            get("/daily-status") {
                val data = DailyStatusDataDto(
                    childName = "Aarav",
                    absenceAlert = "Child not checked in as of 08:30 AM",
                    attendancePercentage = 92,
                    attendanceNote = "Target 95% for early release privilege.",
                    topicsCovered = listOf(TopicCoveredDto("Mathematics", "Introduction to Sets", "Venn diagrams.")),
                    homeworkTasks = listOf(HomeworkTaskDto("Science", "Photosynthesis Project", "Draw the cycle.", true)),
                    upcomingTests = listOf(UpcomingTestDto("Oct", "28", "Calculus", "Differentiation", false)),
                    streakDays = 12,
                    streakMessage = "Consistent on-time check-ins!",
                    schoolMessage = "Signed laboratory forms due Friday."
                )
                call.ok(data, message = "Daily status data fetched")
            }

            get("/reports") {
                val data = ParentReportsDataDto(
                    childName = "Aarav",
                    termNarrative = "Aarav is showing exceptional growth in logical reasoning.",
                    averageScore = 92,
                    globalSubjectRank = 4,
                    totalStudents = 450,
                    improvementTrend = 12.4f,
                    monthlyScores = listOf(78, 82, 85, 89, 92),
                    assessmentHistory = listOf(ReportAssessmentItemDto("Mathematics", "Oct 12", 94, 100, 72, "functions")),
                    teacherRemarks = "Outstanding focus during lab sessions.",
                    leadInstructor = "Dr. Sarah Henderson",
                    pewsStatus = "STABLE",
                    pewsAlert = "Benchmarked with top-tier performance.",
                    learningStreak = 45,
                    skillTrajectory = mapOf("Logic" to 85, "STEM" to 94)
                )
                call.ok(data, message = "Reports data fetched")
            }

            get("/ptm") {
                val data = PtmSchedulingDataDto(
                    selectedMonth = "OCTOBER 2023",
                    selectedDate = 12,
                    ptmWindowDays = listOf(10, 11, 12, 13, 14, 15, 16),
                    teacherName = "Dr. Sarah Henderson",
                    teacherSubject = "Advanced Mathematics",
                    teacherImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDQ0yM8_iW_y-qE0E3Tz4yX3-V1d9p-4e-3zG_s-4j8z_6u_7k-9l-0z-x-y-z",
                    slots = listOf(PtmTimeSlotDto("09:00 AM", true, false), PtmTimeSlotDto("09:45 AM", true, true)),
                    bookings = listOf(PtmBookingDto("Advanced Mathematics", "Dr. Sarah Henderson", "Oct 12, 2023", "09:45 AM", "school"))
                )
                call.ok(data, message = "PTM data fetched")
            }
            
            route("/onboarding") {
                get("/metadata") {
                    val response = OnboardingMetadataResponseDto(
                        screenConfig = OnboardingScreenConfigDto("Let's build a profile.", "We curate the best path.", "Step 1 of 3", 0.33),
                        availableGrades = listOf("Grade 1", "Grade 2", "Nursery"),
                        availableInterests = listOf("Music", "STEM", "Sports"),
                        footerText = "Your data is secure."
                    )
                    call.ok(response, message = "Onboarding metadata fetched")
                }
                post("/child-info") {
                    val uid = call.principalUserId()?.let { runCatching { UUID.fromString(it) }.getOrNull() } ?: run {
                        call.fail("Invalid token", HttpStatusCode.Unauthorized); return@post
                    }
                    val req = runCatching { call.receive<OnboardingChildInfoRequestDto>() }.getOrNull() ?: run {
                        call.fail("Invalid body"); return@post
                    }
                    val newId = UUID.randomUUID()
                    dbQuery {
                        ChildrenTable.insert {
                            it[id] = newId; it[parentId] = uid; it[childName] = req.childName; it[currentLevel] = 1; it[isActive] = true
                        }
                    }
                    call.created(OnboardingChildInfoResponseDto(newId.toString(), req.childName, "PREFERENCES"), message = "Child profile created")
                }
                get("/preference-options") {
                    call.ok(OnboardingPreferencesResponseDto(listOf("CBSE", "ICSE"), listOf(OnboardingFocusAreaDto("acad", "Academics", "school")), OnboardingBudgetConfigDto(0, 10000, listOf(2000, 5000), "$")), message = "Options fetched")
                }
            }
        }
    }
}
