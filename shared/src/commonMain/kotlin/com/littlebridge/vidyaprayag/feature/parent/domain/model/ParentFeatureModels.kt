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
