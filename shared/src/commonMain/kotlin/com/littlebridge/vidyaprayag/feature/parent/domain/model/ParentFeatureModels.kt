package com.littlebridge.vidyaprayag.feature.parent.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

// --- Fees ---
@Serializable
data class FeeResponse(
    val success: Boolean,
    val data: FeeData
)

@Serializable
data class FeeData(
    @SerialName("total_collected") val totalCollected: String,
    @SerialName("collection_progress") val collectionProgress: Float,
    @SerialName("outstanding_fees") val outstandingFees: String,
    @SerialName("overdue_count") val overdueCount: Int,
    val announcements: List<FeeAnnouncementDto>
)

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

// --- Scholarships ---
@Serializable
data class ScholarshipsResponse(
    val success: Boolean,
    val data: ScholarshipsData
)

@Serializable
data class ScholarshipsData(
    val scholarships: List<ScholarshipDto>,
    val applications: List<ScholarshipApplicationDto>,
    @SerialName("profile_strength") val profileStrength: Int,
    @SerialName("streak_days") val streakDays: Int,
    @SerialName("current_level") val currentLevel: Int
)

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

// --- Announcements ---
@Serializable
data class ParentAnnouncementsResponse(
    val success: Boolean,
    val data: ParentAnnouncementsData
)

@Serializable
data class ParentAnnouncementsData(
    val announcements: List<ParentAnnouncementDto>,
    @SerialName("is_whatsapp_sync_enabled") val isWhatsAppSyncEnabled: Boolean
)

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

// --- Notifications (report §5.3 — replaces MockV2.notifications) ---
@Serializable
data class ParentNotificationsResponse(
    val success: Boolean,
    val data: ParentNotificationsData
)

@Serializable
data class ParentNotificationsData(
    val notifications: List<ParentNotificationDto>,
    @SerialName("unread_count") val unreadCount: Int
)

@Serializable
data class ParentNotificationDto(
    val id: String,
    val category: String, // "fees" | "academic" | "attendance" | "announcement"
    val title: String,
    val body: String,
    val time: String,
    val unread: Boolean = true
)

// --- Link Your Child wizard (report §5.3 — replaces MockV2.childForParent/school) ---
@Serializable
data class SchoolSearchResponse(
    val success: Boolean,
    val data: SchoolSearchData
)

@Serializable
data class SchoolSearchData(
    val schools: List<SchoolMatchDto>
)

@Serializable
data class SchoolMatchDto(
    val id: String,
    val name: String,
    val board: String,
    val city: String,
    @SerialName("logo_url") val logoUrl: String? = null
)

@Serializable
data class LinkChildRequest(
    @SerialName("school_id") val schoolId: String,
    @SerialName("roll_number") val rollNumber: String,
    @SerialName("parent_name") val parentName: String? = null
)

@Serializable
data class LinkChildResponse(
    val success: Boolean,
    val data: LinkedChildDto
)

@Serializable
data class LinkedChildDto(
    @SerialName("child_id") val childId: String,
    @SerialName("child_name") val childName: String,
    @SerialName("class_name") val className: String,
    val roll: String,
    @SerialName("school_name") val schoolName: String,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null
)
