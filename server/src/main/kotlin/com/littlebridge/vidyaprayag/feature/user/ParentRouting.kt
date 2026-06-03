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
import org.jetbrains.exposed.sql.or
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
                        val schoolFilter = schoolIds
                            .map { schoolId -> AnnouncementsTable.schoolId eq schoolId }
                            .reduce { acc, op -> acc or op }

                        AnnouncementsTable.selectAll()
                            .where { schoolFilter }
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
