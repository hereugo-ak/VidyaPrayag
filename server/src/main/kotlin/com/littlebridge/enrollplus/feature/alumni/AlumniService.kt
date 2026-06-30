/*
 * File: AlumniService.kt
 * Module: feature/alumni
 *
 * Purpose:
 *   Core business logic for the Alumni Management feature. Handles CRUD,
 *   graduation lifecycle, bulk import, SIS verification, donation campaigns,
 *   80G receipt number generation, mentorship, career history, analytics,
 *   privacy-filtered directory search, and alumni self-registration.
 *
 * Spec ref: ALUMNI_MANAGEMENT_SPEC.md §10 (Service Layer)
 */
package com.littlebridge.enrollplus.feature.alumni

import com.littlebridge.enrollplus.db.*
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.andWhere
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

// ============================================================
// DTOs
// ============================================================

@kotlinx.serialization.Serializable
data class AlumniDto(
    val id: String,
    val schoolId: String,
    val studentId: String?,
    val userId: String?,
    val name: String,
    val graduationYear: Int,
    val lastClass: String?,
    val currentProfession: String?,
    val company: String?,
    val city: String?,
    val email: String?,
    val phone: String?,
    val linkedinUrl: String?,
    val photoUrl: String?,
    val skills: String?,
    val achievements: String?,
    val isMentor: Boolean,
    val mentorExpertise: String?,
    val isFeatured: Boolean,
    val verificationStatus: String,
    val verifiedAt: String?,
    val showPhone: Boolean,
    val showEmail: Boolean,
    val showLinkedin: Boolean,
    val visibilityLevel: String,
    val profileCompleteness: Int,
    val lastActiveAt: String?,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val careerHistory: List<CareerHistoryDto> = emptyList()
)

@kotlinx.serialization.Serializable
data class CreateAlumniDto(
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

@kotlinx.serialization.Serializable
data class UpdateAlumniDto(
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

@kotlinx.serialization.Serializable
data class AlumniPrivacyDto(
    val showPhone: Boolean,
    val showEmail: Boolean,
    val showLinkedin: Boolean,
    val visibilityLevel: String  // public | batch | private
)

@kotlinx.serialization.Serializable
data class AlumniRegisterDto(
    val schoolCode: String? = null,
    val schoolName: String? = null,
    val name: String,
    val yearOfPassing: Int,
    val admissionNumber: String,
    val email: String,
    val phone: String
)

@kotlinx.serialization.Serializable
data class RegistrationResultDto(
    val status: String,  // auto_approved | pending_review
    val alumniId: String
)

@kotlinx.serialization.Serializable
data class GraduateStudentsDto(
    val studentIds: List<String>,
    val graduationYear: Int
)

@kotlinx.serialization.Serializable
data class AlumniDonationCampaignDto(
    val id: String,
    val schoolId: String,
    val title: String,
    val description: String?,
    val cause: String?,
    val targetAmount: Double,
    val amountRaised: Double,
    val targetBatchYear: Int?,
    val startDate: String,
    val endDate: String?,
    val status: String,
    val isActive: Boolean,
    val donorCount: Int,
    val createdAt: String,
    val updatedAt: String
)

@kotlinx.serialization.Serializable
data class CreateCampaignDto(
    val title: String,
    val description: String? = null,
    val cause: String? = null,
    val targetAmount: Double,
    val targetBatchYear: Int? = null,
    val startDate: String,
    val endDate: String? = null
)

@kotlinx.serialization.Serializable
data class AlumniDonationDto(
    val id: String,
    val schoolId: String,
    val alumniId: String,
    val alumniName: String,
    val campaignId: String?,
    val campaignTitle: String?,
    val amount: Double,
    val purpose: String?,
    val donationDate: String,
    val paymentMode: String?,
    val referenceNumber: String?,
    val receiptNumber: String?,
    val receiptIssued: Boolean,
    val is80gEligible: Boolean,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class CreateDonationDto(
    val alumniId: String,
    val campaignId: String? = null,
    val amount: Double,
    val purpose: String? = null,
    val donationDate: String,
    val paymentMode: String? = null,
    val referenceNumber: String? = null
)

@kotlinx.serialization.Serializable
data class AlumniMentorshipRequestDto(
    val id: String,
    val schoolId: String,
    val alumniId: String,
    val alumniName: String,
    val studentId: String,
    val studentName: String,
    val requestedBy: String,
    val requestedByName: String,
    val expertiseArea: String?,
    val message: String?,
    val status: String,
    val respondedAt: String?,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class CreateMentorshipRequestDto(
    val alumniId: String,
    val expertiseArea: String? = null,
    val message: String? = null
)

@kotlinx.serialization.Serializable
data class AlumniMentorshipDto(
    val id: String,
    val schoolId: String,
    val alumniId: String,
    val alumniName: String,
    val studentId: String,
    val studentName: String,
    val requestId: String?,
    val status: String,
    val startDate: String,
    val endDate: String?,
    val notes: String?,
    val sessionCount: Int,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class CreateMentorshipDto(
    val alumniId: String,
    val studentId: String,
    val startDate: String,
    val notes: String? = null
)

@kotlinx.serialization.Serializable
data class CareerHistoryDto(
    val id: String,
    val alumniId: String,
    val jobTitle: String,
    val company: String,
    val industry: String?,
    val startDate: String?,
    val endDate: String?,
    val isCurrent: Boolean,
    val createdAt: String
)

@kotlinx.serialization.Serializable
data class CreateCareerHistoryDto(
    val jobTitle: String,
    val company: String,
    val industry: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val isCurrent: Boolean = false
)

@kotlinx.serialization.Serializable
data class AlumniAnalyticsDto(
    val totalAlumni: Int,
    val activeAlumni: Int,
    val pendingVerifications: Int,
    val byGraduationYear: Map<String, Int>,
    val byProfession: Map<String, Int>,
    val byCity: Map<String, Int>,
    val totalDonations: Double,
    val donationCount: Int,
    val activeCampaigns: Int,
    val activeMentorships: Int,
    val mentorshipRequestsPending: Int,
    val engagementRate: Double
)

@kotlinx.serialization.Serializable
data class AlumniListResponse(
    val alumni: List<AlumniDto>,
    val page: Int,
    val limit: Int,
    val total: Int
)

@kotlinx.serialization.Serializable
data class BulkImportResultDto(
    val imported: Int,
    val failed: Int,
    val errors: List<String>
)

@kotlinx.serialization.Serializable
data class MentorshipSettingsDto(
    val enabled: Boolean,
    val eligibleClassIds: List<String>?,
    val maxMenteesPerAlumni: Int,
    val requestApprovalRequired: Boolean
)

// ============================================================
// Service
// ============================================================

class AlumniService {

    private val fmt = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.systemDefault())
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    // ── Admin: Alumni CRUD ──────────────────────────────────────

    suspend fun listAlumni(
        schoolId: UUID,
        year: Int? = null,
        profession: String? = null,
        city: String? = null,
        company: String? = null,
        industry: String? = null,
        q: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): AlumniListResponse = dbQuery {
        val offset = ((page - 1).coerceAtLeast(0) * limit).toLong()
        val query = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and
            (AlumniTable.isActive eq true)
        }

        year?.let { query.andWhere { AlumniTable.graduationYear eq it } }
        profession?.let { query.andWhere { AlumniTable.currentProfession eq it } }
        city?.let { query.andWhere { AlumniTable.city eq it } }
        company?.let { query.andWhere { AlumniTable.company eq it } }
        q?.let { searchTerm ->
            query.andWhere {
                (AlumniTable.name like "%$searchTerm%") or
                (AlumniTable.currentProfession like "%$searchTerm%") or
                (AlumniTable.company like "%$searchTerm%") or
                (AlumniTable.city like "%$searchTerm%") or
                (AlumniTable.skills like "%$searchTerm%")
            }
        }

        val total = query.count()
        val rows = query.orderBy(AlumniTable.graduationYear, SortOrder.DESC)
            .orderBy(AlumniTable.name, SortOrder.ASC)
            .limit(limit.toInt(), offset)
            .toList()

        AlumniListResponse(
            alumni = rows.map { it.toAlumniDto() },
            page = page,
            limit = limit,
            total = total.toInt()
        )
    }

    suspend fun getAlumni(schoolId: UUID, alumniId: UUID): AlumniDto? = dbQuery {
        val row = AlumniTable.selectAll().where {
            (AlumniTable.id eq alumniId) and (AlumniTable.schoolId eq schoolId)
        }.singleOrNull() ?: return@dbQuery null

        val career = AlumniCareerHistoryTable.selectAll().where {
            AlumniCareerHistoryTable.alumniId eq alumniId
        }.orderBy(AlumniCareerHistoryTable.startDate, SortOrder.DESC).toList()

        row.toAlumniDto(career.map { it.toCareerHistoryDto() })
    }

    suspend fun createAlumni(schoolId: UUID, dto: CreateAlumniDto, verifiedBy: UUID): AlumniDto = dbQuery {
        val now = Instant.now()
        val id = AlumniTable.insertAndGetId {
            it[AlumniTable.schoolId] = schoolId
            it[AlumniTable.studentId] = dto.studentId?.let { s -> runCatching { UUID.fromString(s) }.getOrNull() }
            it[AlumniTable.name] = dto.name
            it[AlumniTable.graduationYear] = dto.graduationYear
            it[AlumniTable.lastClass] = dto.lastClass
            it[AlumniTable.currentProfession] = dto.currentProfession
            it[AlumniTable.company] = dto.company
            it[AlumniTable.city] = dto.city
            it[AlumniTable.email] = dto.email
            it[AlumniTable.phone] = dto.phone
            it[AlumniTable.linkedinUrl] = dto.linkedinUrl
            it[AlumniTable.photoUrl] = dto.photoUrl
            it[AlumniTable.skills] = dto.skills
            it[AlumniTable.achievements] = dto.achievements
            it[AlumniTable.verificationStatus] = "approved"
            it[AlumniTable.verifiedAt] = now
            it[AlumniTable.verifiedBy] = verifiedBy
            it[AlumniTable.createdAt] = now
            it[AlumniTable.updatedAt] = now
        }.value

        AlumniTable.selectAll().where { AlumniTable.id eq id }.single().toAlumniDto()
    }

    suspend fun updateAlumni(schoolId: UUID, alumniId: UUID, dto: UpdateAlumniDto): AlumniDto? = dbQuery {
        val existing = AlumniTable.selectAll().where {
            (AlumniTable.id eq alumniId) and (AlumniTable.schoolId eq schoolId)
        }.singleOrNull() ?: return@dbQuery null

        AlumniTable.update({ AlumniTable.id eq alumniId }) {
            dto.name?.let { v -> it[AlumniTable.name] = v }
            dto.currentProfession?.let { v -> it[AlumniTable.currentProfession] = v }
            dto.company?.let { v -> it[AlumniTable.company] = v }
            dto.city?.let { v -> it[AlumniTable.city] = v }
            dto.email?.let { v -> it[AlumniTable.email] = v }
            dto.phone?.let { v -> it[AlumniTable.phone] = v }
            dto.linkedinUrl?.let { v -> it[AlumniTable.linkedinUrl] = v }
            dto.photoUrl?.let { v -> it[AlumniTable.photoUrl] = v }
            dto.skills?.let { v -> it[AlumniTable.skills] = v }
            dto.achievements?.let { v -> it[AlumniTable.achievements] = v }
            dto.isMentor?.let { v -> it[AlumniTable.isMentor] = v }
            dto.mentorExpertise?.let { v -> it[AlumniTable.mentorExpertise] = v }
            dto.isFeatured?.let { v -> it[AlumniTable.isFeatured] = v }
            dto.isActive?.let { v -> it[AlumniTable.isActive] = v }
            it[AlumniTable.updatedAt] = Instant.now()
        }

        AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.single().toAlumniDto()
    }

    suspend fun deactivateAlumni(schoolId: UUID, alumniId: UUID): Boolean = dbQuery {
        val updated = AlumniTable.update({
            (AlumniTable.id eq alumniId) and (AlumniTable.schoolId eq schoolId)
        }) {
            it[AlumniTable.isActive] = false
            it[AlumniTable.updatedAt] = Instant.now()
        }
        updated > 0
    }

    // ── Admin: Graduation ───────────────────────────────────────

    suspend fun graduateStudents(schoolId: UUID, dto: GraduateStudentsDto, adminId: UUID): List<AlumniDto> = dbQuery {
        val now = Instant.now()
        val gradDate = LocalDate.now()
        val results = mutableListOf<AlumniDto>()

        for (sidStr in dto.studentIds) {
            val sid = runCatching { UUID.fromString(sidStr) }.getOrNull() ?: continue
            val student = StudentsTable.selectAll().where {
                (StudentsTable.id eq sid) and (StudentsTable.schoolId eq schoolId)
            }.singleOrNull() ?: continue

            // Update enrollment status to graduated
            EnrollmentsTable.update({
                (EnrollmentsTable.studentId eq sid) and
                (EnrollmentsTable.status eq "active")
            }) {
                it[EnrollmentsTable.status] = "graduated"
                it[EnrollmentsTable.endDate] = gradDate
            }

            // Deactivate student from active roster
            StudentsTable.update({ StudentsTable.id eq sid }) {
                it[StudentsTable.isActive] = false
            }

            // Check if alumni record already exists for this student
            val existing = AlumniTable.selectAll().where {
                (AlumniTable.studentId eq sid) and (AlumniTable.schoolId eq schoolId)
            }.singleOrNull()

            val alumniId = if (existing != null) {
                existing[AlumniTable.id].value
            } else {
                AlumniTable.insertAndGetId {
                    it[AlumniTable.schoolId] = schoolId
                    it[AlumniTable.studentId] = sid
                    it[AlumniTable.name] = student[StudentsTable.fullName]
                    it[AlumniTable.graduationYear] = dto.graduationYear
                    it[AlumniTable.lastClass] = student[StudentsTable.className]
                    it[AlumniTable.verificationStatus] = "approved"
                    it[AlumniTable.verifiedAt] = now
                    it[AlumniTable.verifiedBy] = adminId
                    it[AlumniTable.createdAt] = now
                    it[AlumniTable.updatedAt] = now
                }.value
            }

            results.add(
                AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.single().toAlumniDto()
            )
        }

        results
    }

    // ── Admin: Bulk Import ──────────────────────────────────────

    suspend fun bulkImport(schoolId: UUID, rows: List<CreateAlumniDto>): BulkImportResultDto = dbQuery {
        val now = Instant.now()
        var imported = 0
        var failed = 0
        val errors = mutableListOf<String>()

        for ((index, row) in rows.withIndex()) {
            try {
                if (row.name.isBlank()) {
                    errors.add("Row ${index + 1}: name is required")
                    failed++
                    continue
                }
                AlumniTable.insert {
                    it[AlumniTable.schoolId] = schoolId
                    it[AlumniTable.studentId] = row.studentId?.let { s -> runCatching { UUID.fromString(s) }.getOrNull() }
                    it[AlumniTable.name] = row.name
                    it[AlumniTable.graduationYear] = row.graduationYear
                    it[AlumniTable.lastClass] = row.lastClass
                    it[AlumniTable.currentProfession] = row.currentProfession
                    it[AlumniTable.company] = row.company
                    it[AlumniTable.city] = row.city
                    it[AlumniTable.email] = row.email
                    it[AlumniTable.phone] = row.phone
                    it[AlumniTable.linkedinUrl] = row.linkedinUrl
                    it[AlumniTable.verificationStatus] = "approved"
                    it[AlumniTable.createdAt] = now
                    it[AlumniTable.updatedAt] = now
                }
                imported++
            } catch (e: Exception) {
                errors.add("Row ${index + 1}: ${e.message}")
                failed++
            }
        }

        BulkImportResultDto(imported = imported, failed = failed, errors = errors)
    }

    // ── Admin: Verification ─────────────────────────────────────

    suspend fun listPendingVerifications(schoolId: UUID): List<AlumniDto> = dbQuery {
        AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and
            (AlumniTable.verificationStatus eq "pending")
        }.orderBy(AlumniTable.createdAt, SortOrder.ASC).map { it.toAlumniDto() }
    }

    suspend fun verifyAlumni(schoolId: UUID, alumniId: UUID, action: String, adminId: UUID): AlumniDto? = dbQuery {
        val status = when (action) {
            "approve" -> "approved"
            "decline" -> "declined"
            else -> return@dbQuery null
        }

        val existing = AlumniTable.selectAll().where {
            (AlumniTable.id eq alumniId) and (AlumniTable.schoolId eq schoolId)
        }.singleOrNull() ?: return@dbQuery null

        AlumniTable.update({ AlumniTable.id eq alumniId }) {
            it[AlumniTable.verificationStatus] = status
            it[AlumniTable.verifiedAt] = Instant.now()
            it[AlumniTable.verifiedBy] = adminId
            it[AlumniTable.updatedAt] = Instant.now()
        }

        // If approved and no user_id yet, create app_users entry
        if (status == "approved" && existing[AlumniTable.userId] == null) {
            val email = existing[AlumniTable.email]
            val phone = existing[AlumniTable.phone]
            if (email != null || phone != null) {
                val userId = AppUsersTable.insertAndGetId {
                    it[AppUsersTable.role] = "alumni"
                    it[AppUsersTable.email] = email
                    it[AppUsersTable.phone] = phone
                    it[AppUsersTable.isActive] = true
                    it[AppUsersTable.createdAt] = Instant.now()
                    it[AppUsersTable.updatedAt] = Instant.now()
                }.value

                AlumniTable.update({ AlumniTable.id eq alumniId }) {
                    it[AlumniTable.userId] = userId
                }
            }
        }

        AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.single().toAlumniDto()
    }

    // ── Admin: Featured ─────────────────────────────────────────

    suspend fun toggleFeatured(schoolId: UUID, alumniId: UUID): AlumniDto? = dbQuery {
        val existing = AlumniTable.selectAll().where {
            (AlumniTable.id eq alumniId) and (AlumniTable.schoolId eq schoolId)
        }.singleOrNull() ?: return@dbQuery null

        AlumniTable.update({ AlumniTable.id eq alumniId }) {
            it[AlumniTable.isFeatured] = !existing[AlumniTable.isFeatured]
            it[AlumniTable.updatedAt] = Instant.now()
        }

        AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.single().toAlumniDto()
    }

    // ── Admin: Campaigns ────────────────────────────────────────

    suspend fun listCampaigns(schoolId: UUID): List<AlumniDonationCampaignDto> = dbQuery {
        AlumniDonationCampaignsTable.selectAll().where {
            AlumniDonationCampaignsTable.schoolId eq schoolId
        }.orderBy(AlumniDonationCampaignsTable.createdAt, SortOrder.DESC).map { it.toCampaignDto() }
    }

    suspend fun createCampaign(schoolId: UUID, dto: CreateCampaignDto): AlumniDonationCampaignDto = dbQuery {
        val now = Instant.now()
        val id = AlumniDonationCampaignsTable.insertAndGetId {
            it[AlumniDonationCampaignsTable.schoolId] = schoolId
            it[AlumniDonationCampaignsTable.title] = dto.title
            it[AlumniDonationCampaignsTable.description] = dto.description
            it[AlumniDonationCampaignsTable.cause] = dto.cause
            it[AlumniDonationCampaignsTable.targetAmount] = dto.targetAmount
            it[AlumniDonationCampaignsTable.amountRaised] = 0.0
            it[AlumniDonationCampaignsTable.targetBatchYear] = dto.targetBatchYear
            it[AlumniDonationCampaignsTable.startDate] = LocalDate.parse(dto.startDate)
            it[AlumniDonationCampaignsTable.endDate] = dto.endDate?.let { LocalDate.parse(it) }
            it[AlumniDonationCampaignsTable.createdAt] = now
            it[AlumniDonationCampaignsTable.updatedAt] = now
        }.value

        AlumniDonationCampaignsTable.selectAll().where {
            AlumniDonationCampaignsTable.id eq id
        }.single().toCampaignDto()
    }

    suspend fun getCampaign(schoolId: UUID, campaignId: UUID): AlumniDonationCampaignDto? = dbQuery {
        AlumniDonationCampaignsTable.selectAll().where {
            (AlumniDonationCampaignsTable.id eq campaignId) and
            (AlumniDonationCampaignsTable.schoolId eq schoolId)
        }.singleOrNull()?.toCampaignDto()
    }

    suspend fun updateCampaign(schoolId: UUID, campaignId: UUID, status: String): AlumniDonationCampaignDto? = dbQuery {
        AlumniDonationCampaignsTable.update({
            (AlumniDonationCampaignsTable.id eq campaignId) and
            (AlumniDonationCampaignsTable.schoolId eq schoolId)
        }) {
            it[AlumniDonationCampaignsTable.status] = status
            it[AlumniDonationCampaignsTable.isActive] = status == "active"
            it[AlumniDonationCampaignsTable.updatedAt] = Instant.now()
        }

        AlumniDonationCampaignsTable.selectAll().where {
            AlumniDonationCampaignsTable.id eq campaignId
        }.singleOrNull()?.toCampaignDto()
    }

    // ── Admin: Donations ────────────────────────────────────────

    suspend fun listDonations(schoolId: UUID, campaignId: UUID? = null, alumniId: UUID? = null): List<AlumniDonationDto> = dbQuery {
        val query = AlumniDonationsTable.selectAll().where {
            AlumniDonationsTable.schoolId eq schoolId
        }
        campaignId?.let { query.andWhere { AlumniDonationsTable.campaignId eq it } }
        alumniId?.let { query.andWhere { AlumniDonationsTable.alumniId eq it } }

        query.orderBy(AlumniDonationsTable.donationDate, SortOrder.DESC).toList().map { row ->
            val alumniName = AlumniTable.selectAll().where { AlumniTable.id eq row[AlumniDonationsTable.alumniId] }
                .singleOrNull()?.get(AlumniTable.name) ?: "Unknown"
            val campaignTitle = row[AlumniDonationsTable.campaignId]?.let { cid ->
                AlumniDonationCampaignsTable.selectAll().where { AlumniDonationCampaignsTable.id eq cid }
                    .singleOrNull()?.get(AlumniDonationCampaignsTable.title)
            }
            row.toDonationDto(alumniName, campaignTitle)
        }
    }

    suspend fun createDonation(schoolId: UUID, dto: CreateDonationDto): AlumniDonationDto = dbQuery {
        val now = Instant.now()
        val alumniId = UUID.fromString(dto.alumniId)
        val campaignId = dto.campaignId?.let { UUID.fromString(it) }

        // Check 80G eligibility
        val school = SchoolsTable.selectAll().where { SchoolsTable.id eq schoolId }.singleOrNull()
        val has80G = school != null &&
            school[SchoolsTable.g80RegistrationNumber] != null &&
            school[SchoolsTable.g80ValidityDate]?.isAfter(LocalDate.now()) == true

        // Cash donations > ₹2000 are not 80G eligible
        val isCash = dto.paymentMode == "cash"
        val is80gEligible = has80G && !(isCash && dto.amount > 2000.0)

        // Generate receipt number if 80G eligible
        val receiptNumber = if (is80gEligible) {
            generateReceiptNumber(schoolId, now.atZone(ZoneId.systemDefault()).year)
        } else null

        val id = AlumniDonationsTable.insertAndGetId {
            it[AlumniDonationsTable.schoolId] = schoolId
            it[AlumniDonationsTable.alumniId] = alumniId
            it[AlumniDonationsTable.campaignId] = campaignId
            it[AlumniDonationsTable.amount] = dto.amount
            it[AlumniDonationsTable.purpose] = dto.purpose
            it[AlumniDonationsTable.donationDate] = LocalDate.parse(dto.donationDate)
            it[AlumniDonationsTable.paymentMode] = dto.paymentMode
            it[AlumniDonationsTable.referenceNumber] = dto.referenceNumber
            it[AlumniDonationsTable.receiptNumber] = receiptNumber
            it[AlumniDonationsTable.receiptIssued] = receiptNumber != null
            it[AlumniDonationsTable.is80gEligible] = is80gEligible
            it[AlumniDonationsTable.createdAt] = now
            it[AlumniDonationsTable.updatedAt] = now
        }.value

        // Update campaign amount_raised if linked
        if (campaignId != null) {
            val total = AlumniDonationsTable.selectAll().where {
                (AlumniDonationsTable.campaignId eq campaignId) and
                (AlumniDonationsTable.schoolId eq schoolId)
            }.sumOf { it[AlumniDonationsTable.amount] }

            AlumniDonationCampaignsTable.update({ AlumniDonationCampaignsTable.id eq campaignId }) {
                it[AlumniDonationCampaignsTable.amountRaised] = total
                it[AlumniDonationCampaignsTable.updatedAt] = now
            }
        }

        val alumniName = AlumniTable.selectAll().where { AlumniTable.id eq alumniId }
            .single()[AlumniTable.name]
        val campaignTitle = campaignId?.let {
            AlumniDonationCampaignsTable.selectAll().where { AlumniDonationCampaignsTable.id eq it }
                .single()[AlumniDonationCampaignsTable.title]
        }

        AlumniDonationsTable.selectAll().where { AlumniDonationsTable.id eq id }
            .single().toDonationDto(alumniName, campaignTitle)
    }

    suspend fun getDonationsForAlumni(schoolId: UUID, alumniId: UUID): List<AlumniDonationDto> = dbQuery {
        val alumniName = AlumniTable.selectAll().where {
            (AlumniTable.id eq alumniId) and (AlumniTable.schoolId eq schoolId)
        }.singleOrNull()?.get(AlumniTable.name) ?: "Unknown"

        AlumniDonationsTable.selectAll().where {
            (AlumniDonationsTable.alumniId eq alumniId) and (AlumniDonationsTable.schoolId eq schoolId)
        }.orderBy(AlumniDonationsTable.donationDate, SortOrder.DESC).toList().map { row ->
            val campaignTitle = row[AlumniDonationsTable.campaignId]?.let { cid ->
                AlumniDonationCampaignsTable.selectAll().where { AlumniDonationCampaignsTable.id eq cid }
                    .singleOrNull()?.get(AlumniDonationCampaignsTable.title)
            }
            row.toDonationDto(alumniName, campaignTitle)
        }
    }

    // ── Admin: Mentorship ───────────────────────────────────────

    suspend fun listMentorships(schoolId: UUID): List<AlumniMentorshipDto> = dbQuery {
        AlumniMentorshipsTable.selectAll().where {
            AlumniMentorshipsTable.schoolId eq schoolId
        }.orderBy(AlumniMentorshipsTable.createdAt, SortOrder.DESC).toList().map { row ->
            val alumniName = AlumniTable.selectAll().where { AlumniTable.id eq row[AlumniMentorshipsTable.alumniId] }
                .singleOrNull()?.get(AlumniTable.name) ?: "Unknown"
            val studentName = StudentsTable.selectAll().where { StudentsTable.id eq row[AlumniMentorshipsTable.studentId] }
                .singleOrNull()?.get(StudentsTable.fullName) ?: "Unknown"
            row.toMentorshipDto(alumniName, studentName)
        }
    }

    suspend fun createMentorship(schoolId: UUID, dto: CreateMentorshipDto): AlumniMentorshipDto = dbQuery {
        val now = Instant.now()
        val alumniId = UUID.fromString(dto.alumniId)
        val studentId = UUID.fromString(dto.studentId)

        val id = AlumniMentorshipsTable.insertAndGetId {
            it[AlumniMentorshipsTable.schoolId] = schoolId
            it[AlumniMentorshipsTable.alumniId] = alumniId
            it[AlumniMentorshipsTable.studentId] = studentId
            it[AlumniMentorshipsTable.status] = "active"
            it[AlumniMentorshipsTable.startDate] = LocalDate.parse(dto.startDate)
            it[AlumniMentorshipsTable.notes] = dto.notes
            it[AlumniMentorshipsTable.createdAt] = now
            it[AlumniMentorshipsTable.updatedAt] = now
        }.value

        val alumniName = AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.single()[AlumniTable.name]
        val studentName = StudentsTable.selectAll().where { StudentsTable.id eq studentId }.single()[StudentsTable.fullName]

        AlumniMentorshipsTable.selectAll().where { AlumniMentorshipsTable.id eq id }
            .single().toMentorshipDto(alumniName, studentName)
    }

    suspend fun endMentorship(schoolId: UUID, mentorshipId: UUID): Boolean = dbQuery {
        val updated = AlumniMentorshipsTable.update({
            (AlumniMentorshipsTable.id eq mentorshipId) and
            (AlumniMentorshipsTable.schoolId eq schoolId)
        }) {
            it[AlumniMentorshipsTable.status] = "ended"
            it[AlumniMentorshipsTable.endDate] = LocalDate.now()
            it[AlumniMentorshipsTable.updatedAt] = Instant.now()
        }
        updated > 0
    }

    suspend fun listMentorshipRequests(schoolId: UUID): List<AlumniMentorshipRequestDto> = dbQuery {
        AlumniMentorshipRequestsTable.selectAll().where {
            AlumniMentorshipRequestsTable.schoolId eq schoolId
        }.orderBy(AlumniMentorshipRequestsTable.createdAt, SortOrder.DESC).toList().map { row ->
            val alumniName = AlumniTable.selectAll().where { AlumniTable.id eq row[AlumniMentorshipRequestsTable.alumniId] }
                .singleOrNull()?.get(AlumniTable.name) ?: "Unknown"
            val studentName = StudentsTable.selectAll().where { StudentsTable.id eq row[AlumniMentorshipRequestsTable.studentId] }
                .singleOrNull()?.get(StudentsTable.fullName) ?: "Unknown"
            val requestedByName = AppUsersTable.selectAll().where { AppUsersTable.id eq row[AlumniMentorshipRequestsTable.requestedBy] }
                .singleOrNull()?.get(AppUsersTable.fullName) ?: "Unknown"
            row.toMentorshipRequestDto(alumniName, studentName, requestedByName)
        }
    }

    // ── Admin: Mentorship Settings ──────────────────────────────

    suspend fun getMentorshipSettings(schoolId: UUID): MentorshipSettingsDto = dbQuery {
        val row = AlumniMentorshipSettingsTable.selectAll().where {
            AlumniMentorshipSettingsTable.schoolId eq schoolId
        }.singleOrNull()

        if (row != null) {
            MentorshipSettingsDto(
                enabled = row[AlumniMentorshipSettingsTable.enabled],
                eligibleClassIds = row[AlumniMentorshipSettingsTable.eligibleClassIds]
                    ?.split(",")?.filter { it.isNotBlank() },
                maxMenteesPerAlumni = row[AlumniMentorshipSettingsTable.maxMenteesPerAlumni],
                requestApprovalRequired = row[AlumniMentorshipSettingsTable.requestApprovalRequired]
            )
        } else {
            MentorshipSettingsDto(
                enabled = true,
                eligibleClassIds = null,
                maxMenteesPerAlumni = 5,
                requestApprovalRequired = true
            )
        }
    }

    suspend fun updateMentorshipSettings(schoolId: UUID, dto: MentorshipSettingsDto): MentorshipSettingsDto = dbQuery {
        val now = Instant.now()
        val existing = AlumniMentorshipSettingsTable.selectAll().where {
            AlumniMentorshipSettingsTable.schoolId eq schoolId
        }.singleOrNull()

        val classIdsStr = dto.eligibleClassIds?.joinToString(",")

        if (existing != null) {
            AlumniMentorshipSettingsTable.update({
                AlumniMentorshipSettingsTable.schoolId eq schoolId
            }) {
                it[AlumniMentorshipSettingsTable.enabled] = dto.enabled
                it[AlumniMentorshipSettingsTable.eligibleClassIds] = classIdsStr
                it[AlumniMentorshipSettingsTable.maxMenteesPerAlumni] = dto.maxMenteesPerAlumni
                it[AlumniMentorshipSettingsTable.requestApprovalRequired] = dto.requestApprovalRequired
                it[AlumniMentorshipSettingsTable.updatedAt] = now
            }
        } else {
            AlumniMentorshipSettingsTable.insert {
                it[AlumniMentorshipSettingsTable.schoolId] = schoolId
                it[AlumniMentorshipSettingsTable.enabled] = dto.enabled
                it[AlumniMentorshipSettingsTable.eligibleClassIds] = classIdsStr
                it[AlumniMentorshipSettingsTable.maxMenteesPerAlumni] = dto.maxMenteesPerAlumni
                it[AlumniMentorshipSettingsTable.requestApprovalRequired] = dto.requestApprovalRequired
                it[AlumniMentorshipSettingsTable.createdAt] = now
                it[AlumniMentorshipSettingsTable.updatedAt] = now
            }
        }

        dto
    }

    // ── Admin: Analytics ────────────────────────────────────────

    suspend fun getAnalyticsOverview(schoolId: UUID): AlumniAnalyticsDto = dbQuery {
        val total = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and (AlumniTable.isActive eq true)
        }.count().toInt()

        val ninetyDaysAgo = Instant.now().minusSeconds(90 * 86400L)
        val active = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and
            (AlumniTable.isActive eq true) and
            (AlumniTable.lastActiveAt.isNotNull()) and
            (AlumniTable.lastActiveAt greater ninetyDaysAgo)
        }.count().toInt()

        val pending = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and
            (AlumniTable.verificationStatus eq "pending")
        }.count().toInt()

        val byYear = mutableMapOf<String, Int>()
        val byProfession = mutableMapOf<String, Int>()
        val byCity = mutableMapOf<String, Int>()

        AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and (AlumniTable.isActive eq true)
        }.forEach { row ->
            val year = row[AlumniTable.graduationYear].toString()
            byYear[year] = (byYear[year] ?: 0) + 1
            row[AlumniTable.currentProfession]?.let { p ->
                byProfession[p] = (byProfession[p] ?: 0) + 1
            }
            row[AlumniTable.city]?.let { c ->
                byCity[c] = (byCity[c] ?: 0) + 1
            }
        }

        val donationRows = AlumniDonationsTable.selectAll().where {
            AlumniDonationsTable.schoolId eq schoolId
        }.toList()
        val totalDonations = donationRows.sumOf { it[AlumniDonationsTable.amount] }

        val activeCampaigns = AlumniDonationCampaignsTable.selectAll().where {
            (AlumniDonationCampaignsTable.schoolId eq schoolId) and
            (AlumniDonationCampaignsTable.status eq "active")
        }.count().toInt()

        val activeMentorships = AlumniMentorshipsTable.selectAll().where {
            (AlumniMentorshipsTable.schoolId eq schoolId) and
            (AlumniMentorshipsTable.status eq "active")
        }.count().toInt()

        val pendingRequests = AlumniMentorshipRequestsTable.selectAll().where {
            (AlumniMentorshipRequestsTable.schoolId eq schoolId) and
            (AlumniMentorshipRequestsTable.status eq "pending")
        }.count().toInt()

        val engagementRate = if (total > 0) (active.toDouble() / total * 100) else 0.0

        AlumniAnalyticsDto(
            totalAlumni = total,
            activeAlumni = active,
            pendingVerifications = pending,
            byGraduationYear = byYear,
            byProfession = byProfession,
            byCity = byCity,
            totalDonations = totalDonations,
            donationCount = donationRows.size,
            activeCampaigns = activeCampaigns,
            activeMentorships = activeMentorships,
            mentorshipRequestsPending = pendingRequests,
            engagementRate = engagementRate
        )
    }

    suspend fun getEngagementMetrics(schoolId: UUID): EngagementMetricsDto = dbQuery {
        val total = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and (AlumniTable.isActive eq true)
        }.count().toInt()
        val ninetyDaysAgo = Instant.now().minusSeconds(90 * 86400L)
        val active = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and
            (AlumniTable.isActive eq true) and
            (AlumniTable.lastActiveAt.isNotNull()) and
            (AlumniTable.lastActiveAt greater ninetyDaysAgo)
        }.count().toInt()
        val mentors = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and (AlumniTable.isMentor eq true) and (AlumniTable.isActive eq true)
        }.count().toInt()
        val featured = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and (AlumniTable.isFeatured eq true) and (AlumniTable.isActive eq true)
        }.count().toInt()
        val withPhotos = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and (AlumniTable.photoUrl.isNotNull()) and (AlumniTable.isActive eq true)
        }.count().toInt()
        EngagementMetricsDto(
            totalAlumni = total,
            activeAlumni = active,
            engagementRate = if (total > 0) active.toDouble() / total * 100 else 0.0,
            mentorCount = mentors,
            featuredCount = featured,
            profilesWithPhoto = withPhotos
        )
    }

    suspend fun getDonationAnalytics(schoolId: UUID): DonationAnalyticsDto = dbQuery {
        val rows = AlumniDonationsTable.selectAll().where {
            AlumniDonationsTable.schoolId eq schoolId
        }.toList()
        val totalAmount = rows.sumOf { it[AlumniDonationsTable.amount] }
        val byPurpose = mutableMapOf<String, Double>()
        val byMode = mutableMapOf<String, Double>()
        rows.forEach { row ->
            val purpose = row[AlumniDonationsTable.purpose] ?: "Unspecified"
            byPurpose[purpose] = (byPurpose[purpose] ?: 0.0) + row[AlumniDonationsTable.amount]
            val mode = row[AlumniDonationsTable.paymentMode] ?: "Unknown"
            byMode[mode] = (byMode[mode] ?: 0.0) + row[AlumniDonationsTable.amount]
        }
        val eightyGCount = rows.count { it[AlumniDonationsTable.is80gEligible] }
        val receiptCount = rows.count { it[AlumniDonationsTable.receiptIssued] }
        DonationAnalyticsDto(
            totalAmount = totalAmount,
            donationCount = rows.size,
            averageDonation = if (rows.isNotEmpty()) totalAmount / rows.size else 0.0,
            eightyGEligibleCount = eightyGCount,
            receiptsIssued = receiptCount,
            byPurpose = byPurpose,
            byPaymentMode = byMode
        )
    }

    suspend fun getCareerAnalytics(schoolId: UUID): CareerAnalyticsDto = dbQuery {
        val byIndustry = mutableMapOf<String, Int>()
        val byCompany = mutableMapOf<String, Int>()
        val alumniIds = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and (AlumniTable.isActive eq true)
        }.map { it[AlumniTable.id].value }

        if (alumniIds.isNotEmpty()) {
            AlumniCareerHistoryTable.selectAll().where {
                AlumniCareerHistoryTable.alumniId inList alumniIds
            }.forEach { row ->
                row[AlumniCareerHistoryTable.industry]?.let { ind ->
                    byIndustry[ind] = (byIndustry[ind] ?: 0) + 1
                }
                val company = row[AlumniCareerHistoryTable.company]
                byCompany[company] = (byCompany[company] ?: 0) + 1
            }
        }
        CareerAnalyticsDto(
            byIndustry = byIndustry,
            byCompany = byCompany
        )
    }

    suspend fun adminRespondToMentorshipRequest(schoolId: UUID, requestId: UUID, action: String): AlumniMentorshipRequestDto? = dbQuery {
        val row = AlumniMentorshipRequestsTable.selectAll().where {
            (AlumniMentorshipRequestsTable.id eq requestId) and (AlumniMentorshipRequestsTable.schoolId eq schoolId)
        }.singleOrNull() ?: return@dbQuery null

        if (row[AlumniMentorshipRequestsTable.status] != "pending") return@dbQuery null

        val newStatus = when (action) {
            "cancel" -> "cancelled"
            "force-accept" -> "accepted"
            else -> return@dbQuery null
        }

        AlumniMentorshipRequestsTable.update({ AlumniMentorshipRequestsTable.id eq requestId }) {
            it[AlumniMentorshipRequestsTable.status] = newStatus
            it[AlumniMentorshipRequestsTable.respondedAt] = Instant.now()
        }

        // If force-accepted, create the mentorship record
        if (newStatus == "accepted") {
            val alumniId = row[AlumniMentorshipRequestsTable.alumniId]
            val studentId = row[AlumniMentorshipRequestsTable.studentId]
            AlumniMentorshipsTable.insert {
                it[AlumniMentorshipsTable.schoolId] = schoolId
                it[AlumniMentorshipsTable.alumniId] = alumniId
                it[AlumniMentorshipsTable.studentId] = studentId
                it[AlumniMentorshipsTable.requestId] = requestId
                it[AlumniMentorshipsTable.status] = "active"
                it[AlumniMentorshipsTable.startDate] = LocalDate.now()
                it[AlumniMentorshipsTable.sessionCount] = 0
                it[AlumniMentorshipsTable.createdAt] = Instant.now()
                it[AlumniMentorshipsTable.updatedAt] = Instant.now()
            }
        }

        val updatedRow = AlumniMentorshipRequestsTable.selectAll().where { AlumniMentorshipRequestsTable.id eq requestId }
            .single()
        val alumniName = AlumniTable.selectAll().where { AlumniTable.id eq updatedRow[AlumniMentorshipRequestsTable.alumniId] }
            .singleOrNull()?.get(AlumniTable.name) ?: "Unknown"
        val studentName = StudentsTable.selectAll().where { StudentsTable.id eq updatedRow[AlumniMentorshipRequestsTable.studentId] }
            .singleOrNull()?.get(StudentsTable.fullName) ?: "Unknown"
        updatedRow.toMentorshipRequestDto(alumniName, studentName, alumniName)
    }

    // ── Alumni Self-Service ─────────────────────────────────────

    suspend fun getAlumniProfile(userId: UUID): AlumniDto? = dbQuery {
        val row = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery null

        val career = AlumniCareerHistoryTable.selectAll().where {
            AlumniCareerHistoryTable.alumniId eq row[AlumniTable.id].value
        }.orderBy(AlumniCareerHistoryTable.startDate, SortOrder.DESC).toList()

        row.toAlumniDto(career.map { it.toCareerHistoryDto() })
    }

    suspend fun updateAlumniProfile(userId: UUID, dto: UpdateAlumniDto): AlumniDto? = dbQuery {
        val row = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery null
        val alumniId = row[AlumniTable.id].value

        AlumniTable.update({ AlumniTable.id eq alumniId }) {
            dto.name?.let { v -> it[AlumniTable.name] = v }
            dto.currentProfession?.let { v -> it[AlumniTable.currentProfession] = v }
            dto.company?.let { v -> it[AlumniTable.company] = v }
            dto.city?.let { v -> it[AlumniTable.city] = v }
            dto.email?.let { v -> it[AlumniTable.email] = v }
            dto.phone?.let { v -> it[AlumniTable.phone] = v }
            dto.linkedinUrl?.let { v -> it[AlumniTable.linkedinUrl] = v }
            dto.photoUrl?.let { v -> it[AlumniTable.photoUrl] = v }
            dto.skills?.let { v -> it[AlumniTable.skills] = v }
            dto.achievements?.let { v -> it[AlumniTable.achievements] = v }
            dto.isMentor?.let { v -> it[AlumniTable.isMentor] = v }
            dto.mentorExpertise?.let { v -> it[AlumniTable.mentorExpertise] = v }
            it[AlumniTable.lastActiveAt] = Instant.now()
            it[AlumniTable.updatedAt] = Instant.now()
        }

        AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.single().toAlumniDto()
    }

    suspend fun updatePrivacy(userId: UUID, dto: AlumniPrivacyDto): AlumniDto? = dbQuery {
        val row = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery null
        val alumniId = row[AlumniTable.id].value

        AlumniTable.update({ AlumniTable.id eq alumniId }) {
            it[AlumniTable.showPhone] = dto.showPhone
            it[AlumniTable.showEmail] = dto.showEmail
            it[AlumniTable.showLinkedin] = dto.showLinkedin
            it[AlumniTable.visibilityLevel] = dto.visibilityLevel
            it[AlumniTable.updatedAt] = Instant.now()
        }

        AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.single().toAlumniDto()
    }

    suspend fun volunteerAsMentor(userId: UUID, expertise: String): AlumniDto? = dbQuery {
        val row = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery null
        val alumniId = row[AlumniTable.id].value

        AlumniTable.update({ AlumniTable.id eq alumniId }) {
            it[AlumniTable.isMentor] = true
            it[AlumniTable.mentorExpertise] = expertise
            it[AlumniTable.updatedAt] = Instant.now()
        }

        AlumniTable.selectAll().where { AlumniTable.id eq alumniId }.single().toAlumniDto()
    }

    suspend fun getMentorshipRequests(userId: UUID): List<AlumniMentorshipRequestDto> = dbQuery {
        val alumniRow = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery emptyList()
        val alumniId = alumniRow[AlumniTable.id].value
        val alumniName = alumniRow[AlumniTable.name]

        AlumniMentorshipRequestsTable.selectAll().where {
            (AlumniMentorshipRequestsTable.alumniId eq alumniId) and
            (AlumniMentorshipRequestsTable.status eq "pending")
        }.orderBy(AlumniMentorshipRequestsTable.createdAt, SortOrder.DESC).toList().map { row ->
            val studentName = StudentsTable.selectAll().where { StudentsTable.id eq row[AlumniMentorshipRequestsTable.studentId] }
                .singleOrNull()?.get(StudentsTable.fullName) ?: "Unknown"
            val requestedByName = AppUsersTable.selectAll().where { AppUsersTable.id eq row[AlumniMentorshipRequestsTable.requestedBy] }
                .singleOrNull()?.get(AppUsersTable.fullName) ?: "Unknown"
            row.toMentorshipRequestDto(alumniName, studentName, requestedByName)
        }
    }

    suspend fun respondToMentorshipRequest(userId: UUID, requestId: UUID, action: String): AlumniMentorshipRequestDto? = dbQuery {
        val alumniRow = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery null
        val alumniId = alumniRow[AlumniTable.id].value

        val reqRow = AlumniMentorshipRequestsTable.selectAll().where {
            (AlumniMentorshipRequestsTable.id eq requestId) and
            (AlumniMentorshipRequestsTable.alumniId eq alumniId)
        }.singleOrNull() ?: return@dbQuery null

        val status = when (action) {
            "accept" -> "accepted"
            "decline" -> "declined"
            else -> return@dbQuery null
        }

        AlumniMentorshipRequestsTable.update({ AlumniMentorshipRequestsTable.id eq requestId }) {
            it[AlumniMentorshipRequestsTable.status] = status
            it[AlumniMentorshipRequestsTable.respondedAt] = Instant.now()
            it[AlumniMentorshipRequestsTable.updatedAt] = Instant.now()
        }

        // If accepted, create mentorship
        if (status == "accepted") {
            val now = Instant.now()
            AlumniMentorshipsTable.insert {
                it[AlumniMentorshipsTable.schoolId] = reqRow[AlumniMentorshipRequestsTable.schoolId]
                it[AlumniMentorshipsTable.alumniId] = alumniId
                it[AlumniMentorshipsTable.studentId] = reqRow[AlumniMentorshipRequestsTable.studentId]
                it[AlumniMentorshipsTable.requestId] = requestId
                it[AlumniMentorshipsTable.status] = "active"
                it[AlumniMentorshipsTable.startDate] = LocalDate.now()
                it[AlumniMentorshipsTable.createdAt] = now
                it[AlumniMentorshipsTable.updatedAt] = now
            }
        }

        val alumniName = alumniRow[AlumniTable.name]
        val studentName = StudentsTable.selectAll().where { StudentsTable.id eq reqRow[AlumniMentorshipRequestsTable.studentId] }
            .singleOrNull()?.get(StudentsTable.fullName) ?: "Unknown"
        val requestedByName = AppUsersTable.selectAll().where { AppUsersTable.id eq reqRow[AlumniMentorshipRequestsTable.requestedBy] }
            .singleOrNull()?.get(AppUsersTable.fullName) ?: "Unknown"

        AlumniMentorshipRequestsTable.selectAll().where { AlumniMentorshipRequestsTable.id eq requestId }
            .single().toMentorshipRequestDto(alumniName, studentName, requestedByName)
    }

    suspend fun getOwnMentorships(userId: UUID): List<AlumniMentorshipDto> = dbQuery {
        val alumniRow = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery emptyList()
        val alumniId = alumniRow[AlumniTable.id].value
        val alumniName = alumniRow[AlumniTable.name]

        AlumniMentorshipsTable.selectAll().where {
            (AlumniMentorshipsTable.alumniId eq alumniId) and
            (AlumniMentorshipsTable.status eq "active")
        }.orderBy(AlumniMentorshipsTable.createdAt, SortOrder.DESC).toList().map { row ->
            val studentName = StudentsTable.selectAll().where { StudentsTable.id eq row[AlumniMentorshipsTable.studentId] }
                .singleOrNull()?.get(StudentsTable.fullName) ?: "Unknown"
            row.toMentorshipDto(alumniName, studentName)
        }
    }

    suspend fun getCareerHistory(userId: UUID): List<CareerHistoryDto> = dbQuery {
        val alumniRow = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery emptyList()
        val alumniId = alumniRow[AlumniTable.id].value

        AlumniCareerHistoryTable.selectAll().where {
            AlumniCareerHistoryTable.alumniId eq alumniId
        }.orderBy(AlumniCareerHistoryTable.startDate, SortOrder.DESC).map { it.toCareerHistoryDto() }
    }

    suspend fun addCareerHistory(userId: UUID, dto: CreateCareerHistoryDto): CareerHistoryDto = dbQuery {
        val alumniRow = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: throw IllegalArgumentException("Alumni not found")
        val alumniId = alumniRow[AlumniTable.id].value

        // If isCurrent, unset previous current entries
        if (dto.isCurrent) {
            AlumniCareerHistoryTable.update({
                (AlumniCareerHistoryTable.alumniId eq alumniId) and
                (AlumniCareerHistoryTable.isCurrent eq true)
            }) {
                it[AlumniCareerHistoryTable.isCurrent] = false
            }
        }

        val id = AlumniCareerHistoryTable.insertAndGetId {
            it[AlumniCareerHistoryTable.alumniId] = alumniId
            it[AlumniCareerHistoryTable.jobTitle] = dto.jobTitle
            it[AlumniCareerHistoryTable.company] = dto.company
            it[AlumniCareerHistoryTable.industry] = dto.industry
            it[AlumniCareerHistoryTable.startDate] = dto.startDate?.let { LocalDate.parse(it) }
            it[AlumniCareerHistoryTable.endDate] = dto.endDate?.let { LocalDate.parse(it) }
            it[AlumniCareerHistoryTable.isCurrent] = dto.isCurrent
            it[AlumniCareerHistoryTable.createdAt] = Instant.now()
        }.value

        AlumniCareerHistoryTable.selectAll().where { AlumniCareerHistoryTable.id eq id }
            .single().toCareerHistoryDto()
    }

    suspend fun updateCareerHistory(userId: UUID, entryId: UUID, dto: CreateCareerHistoryDto): CareerHistoryDto? = dbQuery {
        val alumniRow = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery null
        val alumniId = alumniRow[AlumniTable.id].value

        val existing = AlumniCareerHistoryTable.selectAll().where {
            (AlumniCareerHistoryTable.id eq entryId) and (AlumniCareerHistoryTable.alumniId eq alumniId)
        }.singleOrNull() ?: return@dbQuery null

        if (dto.isCurrent) {
            AlumniCareerHistoryTable.update({
                (AlumniCareerHistoryTable.alumniId eq alumniId) and
                (AlumniCareerHistoryTable.isCurrent eq true) and
                (AlumniCareerHistoryTable.id neq entryId)
            }) {
                it[AlumniCareerHistoryTable.isCurrent] = false
            }
        }

        AlumniCareerHistoryTable.update({ AlumniCareerHistoryTable.id eq entryId }) {
            it[AlumniCareerHistoryTable.jobTitle] = dto.jobTitle
            it[AlumniCareerHistoryTable.company] = dto.company
            it[AlumniCareerHistoryTable.industry] = dto.industry
            it[AlumniCareerHistoryTable.startDate] = dto.startDate?.let { d -> LocalDate.parse(d) }
            it[AlumniCareerHistoryTable.endDate] = dto.endDate?.let { d -> LocalDate.parse(d) }
            it[AlumniCareerHistoryTable.isCurrent] = dto.isCurrent
        }

        AlumniCareerHistoryTable.selectAll().where { AlumniCareerHistoryTable.id eq entryId }
            .single().toCareerHistoryDto()
    }

    suspend fun getOwnDonations(userId: UUID): List<AlumniDonationDto> = dbQuery {
        val alumniRow = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery emptyList()
        val alumniId = alumniRow[AlumniTable.id].value
        val alumniName = alumniRow[AlumniTable.name]

        AlumniDonationsTable.selectAll().where {
            AlumniDonationsTable.alumniId eq alumniId
        }.orderBy(AlumniDonationsTable.donationDate, SortOrder.DESC).toList().map { row ->
            val campaignTitle = row[AlumniDonationsTable.campaignId]?.let { cid ->
                AlumniDonationCampaignsTable.selectAll().where { AlumniDonationCampaignsTable.id eq cid }
                    .singleOrNull()?.get(AlumniDonationCampaignsTable.title)
            }
            row.toDonationDto(alumniName, campaignTitle)
        }
    }

    // ── Alumni Self-Service: Directory (privacy-filtered) ───────

    suspend fun searchDirectory(
        userId: UUID,
        year: Int? = null,
        profession: String? = null,
        city: String? = null,
        q: String? = null,
        page: Int = 1,
        limit: Int = 20
    ): AlumniListResponse = dbQuery {
        val alumniRow = AlumniTable.selectAll().where { AlumniTable.userId eq userId }
            .singleOrNull() ?: return@dbQuery AlumniListResponse(emptyList(), page, limit, 0)
        val alumniId = alumniRow[AlumniTable.id].value
        val schoolId = alumniRow[AlumniTable.schoolId]
        val myGradYear = alumniRow[AlumniTable.graduationYear]

        val offset = ((page - 1).coerceAtLeast(0) * limit).toLong()

        // Privacy filtering: exclude self, exclude private, filter by visibility_level
        val query = AlumniTable.selectAll().where {
            (AlumniTable.schoolId eq schoolId) and
            (AlumniTable.isActive eq true) and
            (AlumniTable.verificationStatus eq "approved") and
            (AlumniTable.id neq alumniId) and
            (AlumniTable.visibilityLevel neq "private")
        }

        year?.let { query.andWhere { AlumniTable.graduationYear eq it } }
        profession?.let { query.andWhere { AlumniTable.currentProfession eq it } }
        city?.let { query.andWhere { AlumniTable.city eq it } }
        q?.let { searchTerm ->
            query.andWhere {
                (AlumniTable.name like "%$searchTerm%") or
                (AlumniTable.currentProfession like "%$searchTerm%") or
                (AlumniTable.company like "%$searchTerm%") or
                (AlumniTable.city like "%$searchTerm%")
            }
        }

        val total = query.count()
        val rows = query.orderBy(AlumniTable.graduationYear, SortOrder.DESC)
            .orderBy(AlumniTable.name, SortOrder.ASC)
            .limit(limit.toInt(), offset)
            .toList()

        // Apply privacy filtering per row
        val filteredRows = rows.map { row ->
            val visibility = row[AlumniTable.visibilityLevel]
            val rowGradYear = row[AlumniTable.graduationYear]

            // batch-level: only visible if same graduation year
            if (visibility == "batch" && rowGradYear != myGradYear) {
                null
            } else {
                // Apply field-level privacy
                row.toAlumniDto(
                    includePhone = row[AlumniTable.showPhone],
                    includeEmail = row[AlumniTable.showEmail],
                    includeLinkedin = row[AlumniTable.showLinkedin]
                )
            }
        }.filterNotNull()

        AlumniListResponse(
            alumni = filteredRows,
            page = page,
            limit = limit,
            total = total.toInt()
        )
    }

    // ── Public: Self-Registration ───────────────────────────────

    suspend fun registerAlumni(dto: AlumniRegisterDto): RegistrationResultDto = dbQuery {
        // Find school by code or name
        val school = if (dto.schoolCode != null) {
            SchoolsTable.selectAll().where {
                SchoolsTable.schoolCode eq dto.schoolCode
            }.singleOrNull()
        } else if (dto.schoolName != null) {
            SchoolsTable.selectAll().where {
                SchoolsTable.name like "%${dto.schoolName}%"
            }.singleOrNull()
        } else null

        if (school == null) {
            throw IllegalArgumentException("School not found. Check your school code or name.")
        }

        val schoolId = school[SchoolsTable.id].value
        val now = Instant.now()

        // Try SIS match: student_code + school_id
        val sisMatch = StudentsTable.selectAll().where {
            (StudentsTable.schoolId eq schoolId) and
            (StudentsTable.studentCode eq dto.admissionNumber)
        }.singleOrNull()

        val verificationStatus = if (sisMatch != null) "approved" else "pending"
        val studentId = sisMatch?.get(StudentsTable.id)?.value

        val alumniId = AlumniTable.insertAndGetId {
            it[AlumniTable.schoolId] = schoolId
            it[AlumniTable.studentId] = studentId
            it[AlumniTable.name] = dto.name
            it[AlumniTable.graduationYear] = dto.yearOfPassing
            it[AlumniTable.lastClass] = sisMatch?.get(StudentsTable.className)
            it[AlumniTable.email] = dto.email
            it[AlumniTable.phone] = dto.phone
            it[AlumniTable.verificationStatus] = verificationStatus
            if (verificationStatus == "approved") {
                it[AlumniTable.verifiedAt] = now
            }
            it[AlumniTable.createdAt] = now
            it[AlumniTable.updatedAt] = now
        }.value

        // If auto-approved, create app_users entry
        if (verificationStatus == "approved") {
            val userId = AppUsersTable.insertAndGetId {
                it[AppUsersTable.role] = "alumni"
                it[AppUsersTable.email] = dto.email
                it[AppUsersTable.phone] = dto.phone
                it[AppUsersTable.isActive] = true
                it[AppUsersTable.createdAt] = now
                it[AppUsersTable.updatedAt] = now
            }.value

            AlumniTable.update({ AlumniTable.id eq alumniId }) {
                it[AlumniTable.userId] = userId
            }
        }

        RegistrationResultDto(
            status = if (verificationStatus == "approved") "auto_approved" else "pending_review",
            alumniId = alumniId.toString()
        )
    }

    // ── Public: School Search ───────────────────────────────────

    suspend fun searchSchools(query: String): List<SchoolSearchResultDto> = dbQuery {
        SchoolsTable.selectAll().where {
            (SchoolsTable.isActive eq true) and
            ((SchoolsTable.name like "%$query%") or
             (SchoolsTable.schoolCode eq query))
        }.limit(10).map { row ->
            SchoolSearchResultDto(
                schoolId = row[SchoolsTable.id].value.toString(),
                name = row[SchoolsTable.name],
                schoolCode = row[SchoolsTable.schoolCode] ?: "",
                city = row[SchoolsTable.city]
            )
        }
    }

    // ── Helpers ─────────────────────────────────────────────────

    private fun generateReceiptNumber(schoolId: UUID, year: Int): String {
        // Format: SCH-80G-YYYY-NNNNN
        // In production, this should query the last receipt number and increment
        // For now, use a random 5-digit number (collision risk is low for small schools)
        val random = (10000..99999).random()
        return "SCH-80G-$year-$random"
    }

    private fun ResultRow.toAlumniDto(
        careerHistory: List<CareerHistoryDto> = emptyList(),
        includePhone: Boolean = true,
        includeEmail: Boolean = true,
        includeLinkedin: Boolean = true
    ): AlumniDto {
        val fields = listOf(
            this[AlumniTable.name],
            this[AlumniTable.currentProfession],
            this[AlumniTable.company],
            this[AlumniTable.city],
            this[AlumniTable.email],
            this[AlumniTable.phone],
            this[AlumniTable.linkedinUrl],
            this[AlumniTable.photoUrl],
            this[AlumniTable.skills],
            this[AlumniTable.achievements]
        )
        val filled = fields.count { !it.isNullOrBlank() }
        val completeness = (filled * 100) / fields.size

        return AlumniDto(
            id = this[AlumniTable.id].value.toString(),
            schoolId = this[AlumniTable.schoolId].toString(),
            studentId = this[AlumniTable.studentId]?.toString(),
            userId = this[AlumniTable.userId]?.toString(),
            name = this[AlumniTable.name],
            graduationYear = this[AlumniTable.graduationYear],
            lastClass = this[AlumniTable.lastClass],
            currentProfession = this[AlumniTable.currentProfession],
            company = this[AlumniTable.company],
            city = this[AlumniTable.city],
            email = if (includeEmail) this[AlumniTable.email] else null,
            phone = if (includePhone) this[AlumniTable.phone] else null,
            linkedinUrl = if (includeLinkedin) this[AlumniTable.linkedinUrl] else null,
            photoUrl = this[AlumniTable.photoUrl],
            skills = this[AlumniTable.skills],
            achievements = this[AlumniTable.achievements],
            isMentor = this[AlumniTable.isMentor],
            mentorExpertise = this[AlumniTable.mentorExpertise],
            isFeatured = this[AlumniTable.isFeatured],
            verificationStatus = this[AlumniTable.verificationStatus],
            verifiedAt = this[AlumniTable.verifiedAt]?.let { fmt.format(it) },
            showPhone = this[AlumniTable.showPhone],
            showEmail = this[AlumniTable.showEmail],
            showLinkedin = this[AlumniTable.showLinkedin],
            visibilityLevel = this[AlumniTable.visibilityLevel],
            profileCompleteness = completeness,
            lastActiveAt = this[AlumniTable.lastActiveAt]?.let { fmt.format(it) },
            isActive = this[AlumniTable.isActive],
            createdAt = fmt.format(this[AlumniTable.createdAt]),
            updatedAt = fmt.format(this[AlumniTable.updatedAt]),
            careerHistory = careerHistory
        )
    }

    private fun ResultRow.toCampaignDto(): AlumniDonationCampaignDto {
        val campaignId = this[AlumniDonationCampaignsTable.id].value
        val schoolId = this[AlumniDonationCampaignsTable.schoolId]
        val donorCount = AlumniDonationsTable.selectAll().where {
            (AlumniDonationsTable.campaignId eq campaignId) and
            (AlumniDonationsTable.schoolId eq schoolId)
        }.count().toInt()

        return AlumniDonationCampaignDto(
            id = this[AlumniDonationCampaignsTable.id].value.toString(),
            schoolId = this[AlumniDonationCampaignsTable.schoolId].toString(),
            title = this[AlumniDonationCampaignsTable.title],
            description = this[AlumniDonationCampaignsTable.description],
            cause = this[AlumniDonationCampaignsTable.cause],
            targetAmount = this[AlumniDonationCampaignsTable.targetAmount],
            amountRaised = this[AlumniDonationCampaignsTable.amountRaised],
            targetBatchYear = this[AlumniDonationCampaignsTable.targetBatchYear],
            startDate = dateFmt.format(this[AlumniDonationCampaignsTable.startDate]),
            endDate = this[AlumniDonationCampaignsTable.endDate]?.let { dateFmt.format(it) },
            status = this[AlumniDonationCampaignsTable.status],
            isActive = this[AlumniDonationCampaignsTable.isActive],
            donorCount = donorCount,
            createdAt = fmt.format(this[AlumniDonationCampaignsTable.createdAt]),
            updatedAt = fmt.format(this[AlumniDonationCampaignsTable.updatedAt])
        )
    }

    private fun ResultRow.toDonationDto(alumniName: String, campaignTitle: String?): AlumniDonationDto {
        return AlumniDonationDto(
            id = this[AlumniDonationsTable.id].value.toString(),
            schoolId = this[AlumniDonationsTable.schoolId].toString(),
            alumniId = this[AlumniDonationsTable.alumniId].toString(),
            alumniName = alumniName,
            campaignId = this[AlumniDonationsTable.campaignId]?.toString(),
            campaignTitle = campaignTitle,
            amount = this[AlumniDonationsTable.amount],
            purpose = this[AlumniDonationsTable.purpose],
            donationDate = dateFmt.format(this[AlumniDonationsTable.donationDate]),
            paymentMode = this[AlumniDonationsTable.paymentMode],
            referenceNumber = this[AlumniDonationsTable.referenceNumber],
            receiptNumber = this[AlumniDonationsTable.receiptNumber],
            receiptIssued = this[AlumniDonationsTable.receiptIssued],
            is80gEligible = this[AlumniDonationsTable.is80gEligible],
            createdAt = fmt.format(this[AlumniDonationsTable.createdAt])
        )
    }

    private fun ResultRow.toMentorshipRequestDto(
        alumniName: String,
        studentName: String,
        requestedByName: String
    ): AlumniMentorshipRequestDto {
        return AlumniMentorshipRequestDto(
            id = this[AlumniMentorshipRequestsTable.id].value.toString(),
            schoolId = this[AlumniMentorshipRequestsTable.schoolId].toString(),
            alumniId = this[AlumniMentorshipRequestsTable.alumniId].toString(),
            alumniName = alumniName,
            studentId = this[AlumniMentorshipRequestsTable.studentId].toString(),
            studentName = studentName,
            requestedBy = this[AlumniMentorshipRequestsTable.requestedBy].toString(),
            requestedByName = requestedByName,
            expertiseArea = this[AlumniMentorshipRequestsTable.expertiseArea],
            message = this[AlumniMentorshipRequestsTable.message],
            status = this[AlumniMentorshipRequestsTable.status],
            respondedAt = this[AlumniMentorshipRequestsTable.respondedAt]?.let { fmt.format(it) },
            createdAt = fmt.format(this[AlumniMentorshipRequestsTable.createdAt])
        )
    }

    private fun ResultRow.toMentorshipDto(alumniName: String, studentName: String): AlumniMentorshipDto {
        return AlumniMentorshipDto(
            id = this[AlumniMentorshipsTable.id].value.toString(),
            schoolId = this[AlumniMentorshipsTable.schoolId].toString(),
            alumniId = this[AlumniMentorshipsTable.alumniId].toString(),
            alumniName = alumniName,
            studentId = this[AlumniMentorshipsTable.studentId].toString(),
            studentName = studentName,
            requestId = this[AlumniMentorshipsTable.requestId]?.toString(),
            status = this[AlumniMentorshipsTable.status],
            startDate = dateFmt.format(this[AlumniMentorshipsTable.startDate]),
            endDate = this[AlumniMentorshipsTable.endDate]?.let { dateFmt.format(it) },
            notes = this[AlumniMentorshipsTable.notes],
            sessionCount = this[AlumniMentorshipsTable.sessionCount],
            createdAt = fmt.format(this[AlumniMentorshipsTable.createdAt])
        )
    }

    private fun ResultRow.toCareerHistoryDto(): CareerHistoryDto {
        return CareerHistoryDto(
            id = this[AlumniCareerHistoryTable.id].value.toString(),
            alumniId = this[AlumniCareerHistoryTable.alumniId].toString(),
            jobTitle = this[AlumniCareerHistoryTable.jobTitle],
            company = this[AlumniCareerHistoryTable.company],
            industry = this[AlumniCareerHistoryTable.industry],
            startDate = this[AlumniCareerHistoryTable.startDate]?.let { dateFmt.format(it) },
            endDate = this[AlumniCareerHistoryTable.endDate]?.let { dateFmt.format(it) },
            isCurrent = this[AlumniCareerHistoryTable.isCurrent],
            createdAt = fmt.format(this[AlumniCareerHistoryTable.createdAt])
        )
    }
}

@kotlinx.serialization.Serializable
data class SchoolSearchResultDto(
    val schoolId: String,
    val name: String,
    val schoolCode: String,
    val city: String
)

@kotlinx.serialization.Serializable
data class EngagementMetricsDto(
    val totalAlumni: Int,
    val activeAlumni: Int,
    val engagementRate: Double,
    val mentorCount: Int,
    val featuredCount: Int,
    val profilesWithPhoto: Int
)

@kotlinx.serialization.Serializable
data class DonationAnalyticsDto(
    val totalAmount: Double,
    val donationCount: Int,
    val averageDonation: Double,
    val eightyGEligibleCount: Int,
    val receiptsIssued: Int,
    val byPurpose: Map<String, Double>,
    val byPaymentMode: Map<String, Double>
)

@kotlinx.serialization.Serializable
data class CareerAnalyticsDto(
    val byIndustry: Map<String, Int>,
    val byCompany: Map<String, Int>
)
