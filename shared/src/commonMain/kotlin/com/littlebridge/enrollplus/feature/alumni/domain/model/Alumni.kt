package com.littlebridge.enrollplus.feature.alumni.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Alumni(
    val id: String,
    val schoolId: String,
    val studentId: String? = null,
    val userId: String? = null,
    val name: String,
    val graduationYear: Int,
    val lastClass: String? = null,
    val currentProfession: String? = null,
    val company: String? = null,
    val city: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val linkedinUrl: String? = null,
    val photoUrl: String? = null,
    val skills: String? = null,
    val achievements: String? = null,
    val isMentor: Boolean = false,
    val mentorExpertise: String? = null,
    val isFeatured: Boolean = false,
    val verificationStatus: String = "approved",
    val verifiedAt: String? = null,
    val showPhone: Boolean = false,
    val showEmail: Boolean = false,
    val showLinkedin: Boolean = true,
    val visibilityLevel: String = "batch",
    val profileCompleteness: Int = 0,
    val lastActiveAt: String? = null,
    val isActive: Boolean = true,
    val createdAt: String = "",
    val updatedAt: String = "",
    val careerHistory: List<CareerHistory> = emptyList()
)

@Serializable
data class CreateAlumniRequest(
    val studentId: String? = null,
    val name: String,
    val graduationYear: Int,
    val lastClass: String? = null,
    val currentProfession: String? = null,
    val company: String? = null,
    val city: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val linkedinUrl: String? = null,
    val photoUrl: String? = null,
    val skills: String? = null,
    val achievements: String? = null
)

@Serializable
data class UpdateAlumniRequest(
    val name: String? = null,
    val currentProfession: String? = null,
    val company: String? = null,
    val city: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val linkedinUrl: String? = null,
    val photoUrl: String? = null,
    val skills: String? = null,
    val achievements: String? = null,
    val isMentor: Boolean? = null,
    val mentorExpertise: String? = null,
    val isFeatured: Boolean? = null,
    val isActive: Boolean? = null
)

@Serializable
data class AlumniPrivacy(
    val showPhone: Boolean,
    val showEmail: Boolean,
    val showLinkedin: Boolean,
    val visibilityLevel: String
)

@Serializable
data class AlumniRegisterRequest(
    val schoolCode: String? = null,
    val schoolName: String? = null,
    val name: String,
    val yearOfPassing: Int,
    val admissionNumber: String,
    val email: String,
    val phone: String
)

@Serializable
data class RegistrationResult(
    val status: String,
    val alumniId: String
)

@Serializable
data class GraduateStudentsRequest(
    val studentIds: List<String>,
    val graduationYear: Int
)

@Serializable
data class AlumniListResponse(
    val alumni: List<Alumni>,
    val page: Int,
    val limit: Int,
    val total: Int
)

@Serializable
data class BulkImportResult(
    val imported: Int,
    val failed: Int,
    val errors: List<String>
)

@Serializable
data class AlumniDonationCampaign(
    val id: String,
    val schoolId: String,
    val title: String,
    val description: String? = null,
    val cause: String? = null,
    val targetAmount: Double,
    val amountRaised: Double,
    val targetBatchYear: Int? = null,
    val startDate: String,
    val endDate: String? = null,
    val status: String,
    val isActive: Boolean,
    val donorCount: Int,
    val createdAt: String = "",
    val updatedAt: String = ""
)

@Serializable
data class CreateCampaignRequest(
    val title: String,
    val description: String? = null,
    val cause: String? = null,
    val targetAmount: Double,
    val targetBatchYear: Int? = null,
    val startDate: String,
    val endDate: String? = null
)

@Serializable
data class AlumniDonation(
    val id: String,
    val schoolId: String,
    val alumniId: String,
    val alumniName: String,
    val campaignId: String? = null,
    val campaignTitle: String? = null,
    val amount: Double,
    val purpose: String? = null,
    val donationDate: String,
    val paymentMode: String? = null,
    val referenceNumber: String? = null,
    val receiptNumber: String? = null,
    val receiptIssued: Boolean = false,
    val is80gEligible: Boolean = false,
    val createdAt: String = ""
)

@Serializable
data class CreateDonationRequest(
    val alumniId: String,
    val campaignId: String? = null,
    val amount: Double,
    val purpose: String? = null,
    val donationDate: String,
    val paymentMode: String? = null,
    val referenceNumber: String? = null
)

@Serializable
data class AlumniMentorshipRequest(
    val id: String,
    val schoolId: String,
    val alumniId: String,
    val alumniName: String,
    val studentId: String,
    val studentName: String,
    val requestedBy: String,
    val requestedByName: String,
    val expertiseArea: String? = null,
    val message: String? = null,
    val status: String,
    val respondedAt: String? = null,
    val createdAt: String = ""
)

@Serializable
data class AlumniMentorship(
    val id: String,
    val schoolId: String,
    val alumniId: String,
    val alumniName: String,
    val studentId: String,
    val studentName: String,
    val requestId: String? = null,
    val status: String,
    val startDate: String,
    val endDate: String? = null,
    val notes: String? = null,
    val sessionCount: Int = 0,
    val createdAt: String = ""
)

@Serializable
data class CreateMentorshipRequest(
    val alumniId: String,
    val studentId: String,
    val startDate: String,
    val notes: String? = null
)

@Serializable
data class CareerHistory(
    val id: String,
    val alumniId: String,
    val jobTitle: String,
    val company: String,
    val industry: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isCurrent: Boolean = false,
    val createdAt: String = ""
)

@Serializable
data class CreateCareerHistoryRequest(
    val jobTitle: String,
    val company: String,
    val industry: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isCurrent: Boolean = false
)

@Serializable
data class AlumniAnalytics(
    val totalAlumni: Int,
    val activeAlumni: Int,
    val pendingVerifications: Int,
    val byGraduationYear: Map<String, Int> = emptyMap(),
    val byProfession: Map<String, Int> = emptyMap(),
    val byCity: Map<String, Int> = emptyMap(),
    val totalDonations: Double = 0.0,
    val donationCount: Int = 0,
    val activeCampaigns: Int = 0,
    val activeMentorships: Int = 0,
    val mentorshipRequestsPending: Int = 0,
    val engagementRate: Double = 0.0
)

@Serializable
data class MentorshipSettings(
    val enabled: Boolean,
    val eligibleClassIds: List<String>? = null,
    val maxMenteesPerAlumni: Int = 5,
    val requestApprovalRequired: Boolean = true
)

@Serializable
data class SchoolSearchResult(
    val schoolId: String,
    val name: String,
    val schoolCode: String,
    val city: String
)

@Serializable
data class EngagementMetrics(
    val totalAlumni: Int,
    val activeAlumni: Int,
    val engagementRate: Double,
    val mentorCount: Int,
    val featuredCount: Int,
    val profilesWithPhoto: Int
)

@Serializable
data class DonationAnalytics(
    val totalAmount: Double,
    val donationCount: Int,
    val averageDonation: Double,
    val eightyGEligibleCount: Int,
    val receiptsIssued: Int,
    val byPurpose: Map<String, Double> = emptyMap(),
    val byPaymentMode: Map<String, Double> = emptyMap()
)

@Serializable
data class CareerAnalytics(
    val byIndustry: Map<String, Int> = emptyMap(),
    val byCompany: Map<String, Int> = emptyMap()
)
