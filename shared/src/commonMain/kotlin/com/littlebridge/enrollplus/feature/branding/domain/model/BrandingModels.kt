package com.littlebridge.enrollplus.feature.branding.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SchoolBranding(
    val schoolId: String,
    val schoolName: String,
    val logoUrl: String? = null,
    val logoDarkUrl: String? = null,
    val faviconUrl: String? = null,
    val appIconUrl: String? = null,
    val splashScreenUrl: String? = null,
    val primaryColor: String = "#2563EB",
    val secondaryColor: String = "#1E40AF",
    val accentColor: String = "#3B82F6",
    val customSubdomain: String? = null,
    val loginBackgroundUrl: String? = null,
    val isCustomized: Boolean = false,
)

@Serializable
data class UpdateBrandingRequest(
    val primaryColor: String? = null,
    val secondaryColor: String? = null,
    val accentColor: String? = null,
    val logoUrl: String? = null,
    val logoDarkUrl: String? = null,
    val faviconUrl: String? = null,
    val appIconUrl: String? = null,
    val splashScreenUrl: String? = null,
    val loginBackgroundUrl: String? = null,
    val isCustomized: Boolean? = null,
)

@Serializable
data class SubdomainRequest(
    val subdomain: String,
)

@Serializable
data class SubdomainResponse(
    val subdomain: String,
)

@Serializable
data class SubdomainCheckResponse(
    val available: Boolean,
)

@Serializable
data class RemoveSubdomainResponse(
    val removed: Boolean,
)

@Serializable
data class SubdomainResolution(
    val schoolId: String,
    val schoolName: String,
    val branding: SchoolBranding,
)
