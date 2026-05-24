package com.littlebridge.vidyaprayag.feature.admin.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingStep(
    val id: Int,
    val title: String,
    val description: String,
    val isCompleted: Boolean = false,
    val iconUrl: String? = null
)
