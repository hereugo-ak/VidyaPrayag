package com.littlebridge.enrollplus.feature.scholarship.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ScholarshipScheme(
    val id: String,
    val schoolId: String? = null,
    val title: String,
    val description: String,
    val amount: String,
    val numericAmount: Double? = null,
    val scholarshipType: String = "fixed",
    val waiverPercentage: Float? = null,
    val eligibilityCriteria: String = "",
    val category: String = "Merit Based",
    val startDate: String? = null,
    val endDate: String? = null,
    val isRenewable: Boolean = false,
    val renewalPeriodMonths: Int? = null,
    val isActive: Boolean = true,
    val isCritical: Boolean = false,
    val timeLeft: String = "",
    val position: Int = 0,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ScholarshipApplication(
    val id: String,
    val scholarshipId: String? = null,
    val scholarshipTitle: String? = null,
    val parentId: String,
    val studentId: String? = null,
    val studentName: String? = null,
    val institution: String = "",
    val program: String = "",
    val status: String,
    val documentUrls: List<String> = emptyList(),
    val parentApplicationText: String? = null,
    val academicYearId: String? = null,
    val iconName: String = "school",
    val remarks: String? = null,
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
    val disbursementAmount: Double? = null,
    val disbursementDate: String? = null,
    val disbursementReference: String? = null,
    val appliedAt: String,
    val updatedAt: String,
)

@Serializable
data class ScholarshipRenewal(
    val id: String,
    val originalApplicationId: String,
    val studentId: String,
    val scholarshipId: String,
    val scholarshipTitle: String? = null,
    val schoolId: String,
    val academicYearId: String,
    val status: String,
    val documentUrls: List<String> = emptyList(),
    val appliedAt: String,
    val reviewedAt: String? = null,
    val reviewedBy: String? = null,
    val remarks: String? = null,
)

@Serializable
data class GamificationData(
    val profileStrength: Int,
    val streakDays: Int,
    val currentLevel: Int,
    val totalApplications: Int,
    val approvedCount: Int,
    val totalAwarded: Double,
)

@Serializable
data class ParentScholarshipsData(
    val scholarships: List<ScholarshipScheme>,
    val applications: List<ScholarshipApplication>,
    val gamification: GamificationData,
)

// ── Request bodies ───────────────────────────────────────────────────────────

@Serializable
data class CreateSchemeRequest(
    val title: String,
    val description: String,
    val amount: String,
    val numericAmount: Double? = null,
    val scholarshipType: String = "fixed",
    val waiverPercentage: Float? = null,
    val eligibilityCriteria: String = "",
    val category: String = "Merit Based",
    val startDate: String? = null,
    val endDate: String? = null,
    val isRenewable: Boolean = false,
    val renewalPeriodMonths: Int? = null,
    val isCritical: Boolean = false,
)

@Serializable
data class UpdateSchemeRequest(
    val title: String? = null,
    val description: String? = null,
    val amount: String? = null,
    val numericAmount: Double? = null,
    val scholarshipType: String? = null,
    val waiverPercentage: Float? = null,
    val eligibilityCriteria: String? = null,
    val category: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isRenewable: Boolean? = null,
    val renewalPeriodMonths: Int? = null,
    val isActive: Boolean? = null,
    val isCritical: Boolean? = null,
)

@Serializable
data class ApplyScholarshipRequest(
    val scholarshipId: String,
    val childId: String,
    val documents: List<String> = emptyList(),
    val applicationText: String? = null,
)

@Serializable
data class ApproveApplicationRequest(
    val remarks: String = "",
    val disbursementAmount: Double? = null,
)

@Serializable
data class RejectApplicationRequest(
    val remarks: String = "",
)

@Serializable
data class DisburseRequest(
    val amount: Double,
    val reference: String,
)

@Serializable
data class ApplyRenewalRequest(
    val scholarshipId: String,
    val originalApplicationId: String,
    val academicYearId: String,
    val childId: String,
    val documents: List<String> = emptyList(),
)

@Serializable
data class ApproveRenewalRequest(
    val remarks: String = "",
)
