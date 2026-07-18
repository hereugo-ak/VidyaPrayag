package com.littlebridge.enrollplus.feature.admin.domain.model

import kotlinx.serialization.Serializable

/**
 * Typed contract for the four premium Home KPI sheets.
 * All labels, values, filters and chart points are supplied by the school-scoped API.
 */
@Serializable
data class HomeAnalyticsStat(
    val label: String = "",
    val value: String = "",
    val supportingText: String = "",
    val direction: String = "flat",
)

@Serializable
data class HomeAnalyticsPoint(
    val label: String = "",
    val value: Double = 0.0,
)

@Serializable
data class HomeAnalyticsBreakdown(
    val id: String = "",
    val label: String = "",
    val value: Double = 0.0,
    val displayValue: String = "",
)

@Serializable
data class HomeAnalyticsFilter(
    val id: String = "all",
    val label: String = "All",
)

@Serializable
data class AdminHomeAnalytics(
    val type: String = "fee",
    val title: String = "",
    val filterLabel: String = "",
    val selectedFilter: String = "all",
    val filters: List<HomeAnalyticsFilter> = emptyList(),
    val stats: List<HomeAnalyticsStat> = emptyList(),
    val distributionTitle: String = "",
    val distributionCenter: String = "",
    val distributionUnit: String = "",
    val distribution: List<HomeAnalyticsBreakdown> = emptyList(),
    val trendTitle: String = "",
    val trend: List<HomeAnalyticsPoint> = emptyList(),
    val breakdownTitle: String = "",
    val breakdown: List<HomeAnalyticsBreakdown> = emptyList(),
)

/** Backend-owned setup state shown only until all five school setup tasks have completed. */
@Serializable
data class AdminSetupStep(
    val key: String = "",
    val label: String = "",
    val status: String = "PENDING", // PENDING | IN_PROGRESS | COMPLETED
    val currentCount: Int = 0,
    val targetCount: Int? = null,
)

@Serializable
data class AdminSetupProgress(
    val setupComplete: Boolean = false,
    val completedSteps: Int = 0,
    val totalSteps: Int = 5,
    val steps: List<AdminSetupStep> = emptyList(),
)

@Serializable
data class UpdateAdminSetupStepRequest(
    val status: String,
)
