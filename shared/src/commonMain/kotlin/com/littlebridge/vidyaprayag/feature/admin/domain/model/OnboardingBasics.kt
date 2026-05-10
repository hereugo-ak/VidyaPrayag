package com.littlebridge.vidyaprayag.feature.admin.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class OnboardingBasics(
    val schoolName: String = "",
    val boardAffiliation: String = "",
    val officialEmail: String = "",
    val contactNumber: String = "",
    val countryCode: String = "+91",
    val address: String = "Education Lane, Knowledge Hub, Sector 42, New Delhi - 110001"
)
