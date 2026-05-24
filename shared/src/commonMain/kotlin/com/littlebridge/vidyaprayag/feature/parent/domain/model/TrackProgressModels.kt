package com.littlebridge.vidyaprayag.feature.parent.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class TrackProgressResponse(
    val success: Boolean,
    val message: String,
    val data: TrackProgressData
)

@Serializable
data class TrackProgressData(
    @SerialName("child_name") val childName: String,
    @SerialName("overall_progress") val overallProgress: Float,
    @SerialName("current_level") val currentLevel: Int,
    @SerialName("journey_description") val journeyDescription: String,
    val badges: List<AchievementBadgeData>,
    @SerialName("academic_competencies") val academicCompetencies: List<AcademicCompetencyData>,
    @SerialName("emotional_intelligence") val emotionalIntelligence: Map<String, Float>,
    @SerialName("play_indicators") val playIndicators: List<PlayIndicatorData>
)

@Serializable
data class AchievementBadgeData(
    val title: String,
    @SerialName("icon_name") val iconName: String,
    @SerialName("is_locked") val isLocked: Boolean,
    @SerialName("gradient_colors") val gradientColors: List<Long>
)

@Serializable
data class AcademicCompetencyData(
    val title: String,
    @SerialName("icon_name") val iconName: String,
    val progress: Float
)

@Serializable
data class PlayIndicatorData(
    val title: String,
    val description: String,
    @SerialName("image_url") val imageUrl: String,
    @SerialName("is_met") val isMet: Boolean
)
