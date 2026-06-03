package com.littlebridge.vidyaprayag.feature.user

import com.littlebridge.vidyaprayag.core.ok
import com.littlebridge.vidyaprayag.core.principalUserUuid
import com.littlebridge.vidyaprayag.db.AnnouncementsTable
import com.littlebridge.vidyaprayag.db.AppConfigTable
import com.littlebridge.vidyaprayag.db.ChildrenTable
import com.littlebridge.vidyaprayag.db.DatabaseFactory.dbQuery
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.inList
import org.jetbrains.exposed.sql.selectAll

private val lenientJson = Json { ignoreUnknownKeys = true; isLenient = true }

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

@Serializable
data class TrackProgressResponse(
    val success: Boolean,
    val message: String,
    val data: TrackProgressDataDto
)

// --- Fees ---
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

// --- Scholarships ---
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

// --- Announcements ---
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

fun Route.parentRouting() {
    authenticate("jwt") {
        route("/api/v1/parent") {
            get("/track-progress") {
                // Mock data matching the UI requirements
                val data = TrackProgressDataDto(
                    childName = "Aarav",
                    overallProgress = 0.75f,
                    currentLevel = 4,
                    journeyDescription = "Developmental milestones on track for Term 2",
                    badges = listOf(
                        AchievementBadgeDto("Social Star", "workspace_premium", false, listOf(0xFFB6C7EB, 0xFF006C49)),
                        AchievementBadgeDto("Book Worm", "auto_stories", false, listOf(0xFFCBDBF5, 0xFF8293B5)),
                        AchievementBadgeDto("Fast Learner", "rocket_launch", false, listOf(0xFF4EDE93, 0xFF006C49)),
                        AchievementBadgeDto("Upcoming", "lock", true, listOf(0xFFE5EEFF, 0xFFE5EEFF))
                    ),
                    academicCompetencies = listOf(
                        AcademicCompetencyDto("Literacy", "translate", 0.85f),
                        AcademicCompetencyDto("Numeracy", "calculate", 0.70f)
                    ),
                    emotionalIntelligence = mapOf(
                        "Empathy" to 0.8f,
                        "Resilience" to 0.7f,
                        "Social" to 0.9f,
                        "Control" to 0.6f,
                        "Focus" to 0.75f,
                        "Sharing" to 0.85f
                    ),
                    playIndicators = listOf(
                        PlayIndicatorDto(
                            "Creative Expression", 
                            "Uses diverse materials to represent ideas", 
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuBJ0iy3QHsYrDK9vkmt05wDdmHmpgT8gBlcip2cJxtHhEZh8aRcsRMENEot_fma9PHySR3i7uOBCkzywjgrnyRweoIcsAippP8X0A0wqcgX-r5pfZvIL5UF_FG0Q8N_eb8FdFdPyQ48xEiykqbtT-Uh3PpA4KeOf2vv6fzHKyIidF-Y8ldvErlwE50_WVwRhhK7TMiQuKDOR9LRFN7cqu9v5ygC0nl9_0IMd4GuMkFoiDefldCGJStlfH48L5RIjTUZfLrJ-EITce_3",
                            true
                        ),
                        PlayIndicatorDto(
                            "Physical Agility", 
                            "Gross motor coordination milestones met", 
                            "https://lh3.googleusercontent.com/aida-public/AB6AXuC97aj-fXdWa8AknQylLrGHeKwdSE_wY776abXdGOHdPIyP7yGiA7uw8V8vXdYXudLVG-Sue3-oPaD7YeCkrMA9jLvA3cdnafmRz6kPJvG_QVv4_dfdXDGRVqTHRWTUUWzMrT85G_aJBx6fHQZtEKSfDOmAa0a22EmEhL6IIg4RHLERutQdBs7iO_oYDZ1Wy51bMcNXHZJ3S40pbvg_jqq0dvcBXWMeCetnJLVXn4PysxPN1MpyqE5i5yz6EgvotyPLPGpJ8xKgwtlv",
                            true
                        )
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
                        FeeAnnouncementDto("1", "Annual Sports Day Schedule", "2h ago", "Detailed itinerary released.", "94%", "24", "Campaign"),
                        FeeAnnouncementDto("2", "Weather Alert: Early Closure", "5h ago", "Campus closing at 2:00 PM.", "98%", "812", "Emergency"),
                        FeeAnnouncementDto("3", "Fee Submission Deadline", "Yesterday", "Final reminder for Q3 fees.", "62%", "3", "Payment")
                    )
                )
                call.ok(data, message = "Fees data fetched")
            }

            get("/scholarships") {
                val data = ScholarshipsDataDto(
                    scholarships = listOf(
                        ScholarshipDto("1", "Global Excellence STEM Award 2024", "Engineering or Math focus.", "$45,000", "3d : 12h", "Full Funding", true),
                        ScholarshipDto("2", "Social Impact Grant", "Community initiatives support.", "$5,000", "24h left", "Merit Based", true),
                        ScholarshipDto("3", "Bridge-to-Learning Fund", "First-gen college student aid.", "$12,000", "14 days", "International")
                    ),
                    applications = listOf(
                        ScholarshipApplicationDto("1", "University of Applied Sciences", "B.Arch - Sustainable Urbanism", "Shortlisted", "architecture"),
                        ScholarshipApplicationDto("2", "Tech Institute of Innovation", "M.Sc - AI", "Under Review", "biotech"),
                        ScholarshipApplicationDto("3", "Royal Academy of Arts", "BFA - Digital Media Design", "Received", "history_edu")
                    ),
                    profileStrength = 85,
                    streakDays = 3,
                    currentLevel = 4
                )
                call.ok(data, message = "Scholarships data fetched")
            }

            // -------- ANNOUNCEMENTS (parent-school harmony, report §9.1) --------
            // Parents now read the SAME announcements rows the school side
            // creates (AnnouncementsTable), scoped to the schools their children
            // are enrolled in. This replaces the old static/mock list so a
            // school announcement actually reaches the parent.
            get("/announcements") {
                val uid = call.principalUserUuid() ?: run {
                    call.respond(HttpStatusCode.Unauthorized); return@get
                }

                val data = dbQuery {
                    // 1) Resolve the schools this parent's active children belong to.
                    val schoolIds = ChildrenTable.selectAll()
                        .where {
                            (ChildrenTable.parentId eq uid) and (ChildrenTable.isActive eq true)
                        }
                        .mapNotNull { it[ChildrenTable.schoolId] }
                        .distinct()

                    // 2) Pull announcements for those schools, newest first.
                    val rows = if (schoolIds.isEmpty()) {
                        emptyList()
                    } else {
                        AnnouncementsTable.selectAll()
                            .where { AnnouncementsTable.schoolId inList schoolIds }
                            .orderBy(AnnouncementsTable.createdAt, SortOrder.DESC)
                            .map { row ->
                                ParentAnnouncementDto(
                                    id = row[AnnouncementsTable.id].value.toString(),
                                    title = row[AnnouncementsTable.title],
                                    description = row[AnnouncementsTable.description],
                                    date = row[AnnouncementsTable.date],
                                    category = row[AnnouncementsTable.type],
                                    // "Special" announcements are surfaced as featured.
                                    isFeatured = row[AnnouncementsTable.type]
                                        .equals("Special", ignoreCase = true),
                                    imageUrl = row[AnnouncementsTable.eventImage]
                                )
                            }
                    }

                    // 3) WhatsApp sync flag from the same app_config the school uses.
                    val flagsRaw = AppConfigTable.selectAll()
                        .where { AppConfigTable.key eq "flags" }
                        .singleOrNull()?.get(AppConfigTable.value)
                    val waEnabled = runCatching {
                        flagsRaw
                            ?.let { lenientJson.parseToJsonElement(it).jsonObject }
                            ?.get("is_whatsapp_sync_enabled")
                            ?.jsonPrimitive?.content
                            ?.toBooleanStrictOrNull()
                    }.getOrNull() ?: false

                    ParentAnnouncementsDataDto(
                        announcements = rows,
                        isWhatsAppSyncEnabled = waEnabled
                    )
                }
                call.ok(data, message = "Announcements data fetched")
            }
        }
    }
}
