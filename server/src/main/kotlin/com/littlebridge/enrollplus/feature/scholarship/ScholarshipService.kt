/*
 * File: ScholarshipService.kt
 * Module: feature.scholarship
 *
 * Core service for the Scholarship Workflow feature (SCHOLARSHIP_WORKFLOW_SPEC.md).
 *
 * Handles:
 *   - Scheme management (CRUD by admin)
 *   - Application submission (parent applies with documents)
 *   - Approval/rejection workflow (admin reviews)
 *   - Disbursement tracking (admin records disbursement)
 *   - Renewal management (parent applies for renewal, admin reviews)
 *   - Fee integration (auto-apply waiver on approval via FeeService)
 *   - Notifications (push + in-app on status changes)
 *
 * Follows SOLID principles:
 *   - Single responsibility: scholarship workflow logic
 *   - Dependency inversion: depends on FeeService (interface), NotificationService
 *   - Open/closed: new scholarship types can be added without modifying existing logic
 *
 * Plug-and-play: registered in Koin, routing calls this service, no direct DB
 * access from routing layer.
 */
package com.littlebridge.enrollplus.feature.scholarship

import com.littlebridge.enrollplus.db.*
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.feature.fee.FeeService
import com.littlebridge.enrollplus.feature.notification.dto.SendNotificationRequest
import com.littlebridge.enrollplus.feature.notification.service.NotificationService
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable
data class ScholarshipSchemeDto(
    val id: String,
    val schoolId: String? = null,
    val title: String,
    val description: String,
    val amount: String,                        // display string (legacy compat)
    val numericAmount: Double? = null,         // numeric amount for fixed type
    val scholarshipType: String = "fixed",     // fixed | full_waiver | partial_waiver
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
data class ScholarshipApplicationDto(
    val id: String,
    val scholarshipId: String? = null,
    val scholarshipTitle: String? = null,
    val parentId: String,
    val studentId: String? = null,
    val studentName: String? = null,
    val institution: String = "",
    val program: String = "",
    val status: String,                        // PENDING | APPROVED | REJECTED | DISBURSED
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
data class ScholarshipRenewalDto(
    val id: String,
    val originalApplicationId: String,
    val studentId: String,
    val scholarshipId: String,
    val scholarshipTitle: String? = null,
    val schoolId: String,
    val academicYearId: String,
    val status: String,                        // pending | approved | rejected
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
data class ParentScholarshipsResponse(
    val scholarships: List<ScholarshipSchemeDto>,
    val applications: List<ScholarshipApplicationDto>,
    val gamification: GamificationData,
)

// ── Request bodies ───────────────────────────────────────────────────────────

@Serializable
data class CreateSchemeRequest(
    val title: String,
    val description: String,
    val amount: String,                        // display string
    val numericAmount: Double? = null,
    val scholarshipType: String = "fixed",     // fixed | full_waiver | partial_waiver
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

// ── Service ──────────────────────────────────────────────────────────────────

class ScholarshipService(
    private val feeService: FeeService = FeeService(),
    private val notificationService: NotificationService? = null,
) {

    private val json = Json { ignoreUnknownKeys = true }

    // ── Admin: Scheme Management ─────────────────────────────────────────

    suspend fun listSchemes(schoolId: UUID, activeOnly: Boolean = true): List<ScholarshipSchemeDto> = dbQuery {
        val query = ScholarshipsTable.selectAll()
            .where { ScholarshipsTable.schoolId eq schoolId }
        if (activeOnly) {
            query.andWhere { ScholarshipsTable.isActive eq true }
        }
        query.orderBy(ScholarshipsTable.position, SortOrder.ASC)
            .map { rowToScheme(it) }
    }

    suspend fun createScheme(schoolId: UUID, adminId: UUID, req: CreateSchemeRequest): ScholarshipSchemeDto = dbQuery {
        val now = Instant.now()
        val id = ScholarshipsTable.insert {
            it[ScholarshipsTable.schoolId] = schoolId
            it[title] = req.title
            it[description] = req.description
            it[amount] = req.amount
            it[numericAmount] = req.numericAmount
            it[scholarshipType] = req.scholarshipType
            it[waiverPercentage] = req.waiverPercentage
            it[eligibilityCriteria] = req.eligibilityCriteria
            it[category] = req.category
            it[startDate] = req.startDate
            it[endDate] = req.endDate
            it[isRenewable] = req.isRenewable
            it[renewalPeriodMonths] = req.renewalPeriodMonths
            it[isCritical] = req.isCritical
            it[isActive] = true
            it[position] = 0
            it[createdAt] = now
            it[updatedAt] = now
        }[ScholarshipsTable.id].value

        ScholarshipsTable.selectAll().where { ScholarshipsTable.id eq id }.single().let { rowToScheme(it) }
    }

    suspend fun updateScheme(schoolId: UUID, schemeId: UUID, req: UpdateSchemeRequest): Boolean = dbQuery {
        val updates = mutableMapOf<Column<*>, Any>()
        req.title?.let { updates[ScholarshipsTable.title] = it }
        req.description?.let { updates[ScholarshipsTable.description] = it }
        req.amount?.let { updates[ScholarshipsTable.amount] = it }
        req.numericAmount?.let { updates[ScholarshipsTable.numericAmount] = it }
        req.scholarshipType?.let { updates[ScholarshipsTable.scholarshipType] = it }
        req.waiverPercentage?.let { updates[ScholarshipsTable.waiverPercentage] = it }
        req.eligibilityCriteria?.let { updates[ScholarshipsTable.eligibilityCriteria] = it }
        req.category?.let { updates[ScholarshipsTable.category] = it }
        req.startDate?.let { updates[ScholarshipsTable.startDate] = it }
        req.endDate?.let { updates[ScholarshipsTable.endDate] = it }
        req.isRenewable?.let { updates[ScholarshipsTable.isRenewable] = it }
        req.renewalPeriodMonths?.let { updates[ScholarshipsTable.renewalPeriodMonths] = it }
        req.isActive?.let { updates[ScholarshipsTable.isActive] = it }
        req.isCritical?.let { updates[ScholarshipsTable.isCritical] = it }
        updates[ScholarshipsTable.updatedAt] = Instant.now()

        if (updates.isEmpty()) return@dbQuery false

        val count = ScholarshipsTable.update({
            (ScholarshipsTable.id eq schemeId) and (ScholarshipsTable.schoolId eq schoolId)
        }) { stmt ->
            updates.forEach { (col, value) ->
                @Suppress("UNCHECKED_CAST")
                stmt[col as Column<Any>] = value
            }
        }
        count > 0
    }

    suspend fun deleteScheme(schoolId: UUID, schemeId: UUID): Boolean = dbQuery {
        val count = ScholarshipsTable.update({
            (ScholarshipsTable.id eq schemeId) and (ScholarshipsTable.schoolId eq schoolId)
        }) {
            it[isActive] = false
            it[updatedAt] = Instant.now()
        }
        count > 0
    }

    // ── Admin: Application Review ─────────────────────────────────────────

    suspend fun listApplications(schoolId: UUID, status: String? = null): List<ScholarshipApplicationDto> = dbQuery {
        // Join with scholarships to get school-scoped applications
        val query = ScholarshipApplicationsTable.join(
            ScholarshipsTable,
            JoinType.INNER,
            ScholarshipApplicationsTable.scholarshipId,
            ScholarshipsTable.id,
        ).selectAll()
            .where { ScholarshipsTable.schoolId eq schoolId }

        if (status != null) {
            query.andWhere { ScholarshipApplicationsTable.status eq status }
        }

        query.orderBy(ScholarshipApplicationsTable.createdAt, SortOrder.DESC)
            .map { rowToApplication(it) }
    }

    suspend fun getApplication(schoolId: UUID, applicationId: UUID): ScholarshipApplicationDto? = dbQuery {
        val row = ScholarshipApplicationsTable.join(
            ScholarshipsTable,
            JoinType.INNER,
            ScholarshipApplicationsTable.scholarshipId,
            ScholarshipsTable.id,
        ).selectAll()
            .where {
                (ScholarshipApplicationsTable.id eq applicationId) and
                (ScholarshipsTable.schoolId eq schoolId)
            }
            .singleOrNull()
            ?: return@dbQuery null
        rowToApplication(row)
    }

    suspend fun approveApplication(schoolId: UUID, applicationId: UUID, adminId: UUID, req: ApproveApplicationRequest): ScholarshipApplicationDto? = dbQuery {
        // Verify the application belongs to this school
        val row = ScholarshipApplicationsTable.join(
            ScholarshipsTable,
            JoinType.INNER,
            ScholarshipApplicationsTable.scholarshipId,
            ScholarshipsTable.id,
        ).selectAll()
            .where {
                (ScholarshipApplicationsTable.id eq applicationId) and
                (ScholarshipsTable.schoolId eq schoolId) and
                (ScholarshipApplicationsTable.status eq "PENDING")
            }
            .singleOrNull()
            ?: return@dbQuery null

        val scholarshipId = row[ScholarshipApplicationsTable.scholarshipId]
        val studentId = row[ScholarshipApplicationsTable.studentId]
        val parentId = row[ScholarshipApplicationsTable.parentId]
        val scholarshipTitle = row[ScholarshipsTable.title]
        val now = Instant.now()

        // Update application status
        ScholarshipApplicationsTable.update({ ScholarshipApplicationsTable.id eq applicationId }) {
            it[status] = "APPROVED"
            it[reviewedAt] = now
            it[reviewedBy] = adminId
            it[remarks] = req.remarks
            it[disbursementAmount] = req.disbursementAmount
            it[updatedAt] = now
        }

        // Apply fee waiver if we have scholarship + student IDs
        if (scholarshipId != null && studentId != null) {
            try {
                feeService.applyScholarship(studentId, scholarshipId, schoolId)
            } catch (e: Exception) {
                // Log error but don't fail the approval — fee integration is best-effort
                println("SCHOLARSHIP: Fee integration failed for application $applicationId: ${e.message}")
            }
        }

        // Send notification to parent
        notificationService?.send(
            SendNotificationRequest(
                title = "Scholarship Approved!",
                body = "Scholarship '$scholarshipTitle' has been approved. ${req.remarks}",
                userIds = listOf(parentId.toString()),
                deepLink = "vidyaprayag://parent/scholarships",
                data = mapOf("type" to "scholarship_approved", "applicationId" to applicationId.toString()),
            )
        )

        // Return updated application
        ScholarshipApplicationsTable.selectAll()
            .where { ScholarshipApplicationsTable.id eq applicationId }
            .single()
            .let { rowToApplication(it) }
    }

    suspend fun rejectApplication(schoolId: UUID, applicationId: UUID, adminId: UUID, req: RejectApplicationRequest): ScholarshipApplicationDto? = dbQuery {
        val row = ScholarshipApplicationsTable.join(
            ScholarshipsTable,
            JoinType.INNER,
            ScholarshipApplicationsTable.scholarshipId,
            ScholarshipsTable.id,
        ).selectAll()
            .where {
                (ScholarshipApplicationsTable.id eq applicationId) and
                (ScholarshipsTable.schoolId eq schoolId) and
                (ScholarshipApplicationsTable.status eq "PENDING")
            }
            .singleOrNull()
            ?: return@dbQuery null

        val parentId = row[ScholarshipApplicationsTable.parentId]
        val scholarshipTitle = row[ScholarshipsTable.title]
        val now = Instant.now()

        ScholarshipApplicationsTable.update({ ScholarshipApplicationsTable.id eq applicationId }) {
            it[status] = "REJECTED"
            it[reviewedAt] = now
            it[reviewedBy] = adminId
            it[remarks] = req.remarks
            it[updatedAt] = now
        }

        notificationService?.send(
            SendNotificationRequest(
                title = "Scholarship Update",
                body = "Scholarship '$scholarshipTitle' application was not approved. ${req.remarks}",
                userIds = listOf(parentId.toString()),
                deepLink = "vidyaprayag://parent/scholarships",
                data = mapOf("type" to "scholarship_rejected", "applicationId" to applicationId.toString()),
            )
        )

        ScholarshipApplicationsTable.selectAll()
            .where { ScholarshipApplicationsTable.id eq applicationId }
            .single()
            .let { rowToApplication(it) }
    }

    suspend fun disburse(schoolId: UUID, applicationId: UUID, adminId: UUID, req: DisburseRequest): ScholarshipApplicationDto? = dbQuery {
        val row = ScholarshipApplicationsTable.join(
            ScholarshipsTable,
            JoinType.INNER,
            ScholarshipApplicationsTable.scholarshipId,
            ScholarshipsTable.id,
        ).selectAll()
            .where {
                (ScholarshipApplicationsTable.id eq applicationId) and
                (ScholarshipsTable.schoolId eq schoolId) and
                (ScholarshipApplicationsTable.status eq "APPROVED")
            }
            .singleOrNull()
            ?: return@dbQuery null

        val parentId = row[ScholarshipApplicationsTable.parentId]
        val scholarshipTitle = row[ScholarshipsTable.title]
        val now = Instant.now()

        ScholarshipApplicationsTable.update({ ScholarshipApplicationsTable.id eq applicationId }) {
            it[status] = "DISBURSED"
            it[disbursementAmount] = req.amount
            it[disbursementDate] = now
            it[disbursementReference] = req.reference
            it[updatedAt] = now
        }

        notificationService?.send(
            SendNotificationRequest(
                title = "Scholarship Disbursed!",
                body = "Scholarship '$scholarshipTitle' disbursed. Amount: ${req.amount}. Reference: ${req.reference}",
                userIds = listOf(parentId.toString()),
                deepLink = "vidyaprayag://parent/scholarships",
                data = mapOf("type" to "scholarship_disbursed", "applicationId" to applicationId.toString()),
            )
        )

        ScholarshipApplicationsTable.selectAll()
            .where { ScholarshipApplicationsTable.id eq applicationId }
            .single()
            .let { rowToApplication(it) }
    }

    // ── Admin: Renewals ───────────────────────────────────────────────────

    suspend fun listRenewals(schoolId: UUID, status: String? = null): List<ScholarshipRenewalDto> = dbQuery {
        val query = ScholarshipRenewalsTable.join(
            ScholarshipsTable,
            JoinType.INNER,
            ScholarshipRenewalsTable.scholarshipId,
            ScholarshipsTable.id,
        ).selectAll()
            .where { ScholarshipRenewalsTable.schoolId eq schoolId }

        if (status != null) {
            query.andWhere { ScholarshipRenewalsTable.status eq status }
        }

        query.orderBy(ScholarshipRenewalsTable.appliedAt, SortOrder.DESC)
            .map { rowToRenewal(it) }
    }

    suspend fun approveRenewal(schoolId: UUID, renewalId: UUID, adminId: UUID, req: ApproveRenewalRequest): ScholarshipRenewalDto? = dbQuery {
        val row = ScholarshipRenewalsTable.join(
            ScholarshipsTable,
            JoinType.INNER,
            ScholarshipRenewalsTable.scholarshipId,
            ScholarshipsTable.id,
        ).selectAll()
            .where {
                (ScholarshipRenewalsTable.id eq renewalId) and
                (ScholarshipRenewalsTable.schoolId eq schoolId) and
                (ScholarshipRenewalsTable.status eq "pending")
            }
            .singleOrNull()
            ?: return@dbQuery null

        val studentId = row[ScholarshipRenewalsTable.studentId]
        val scholarshipId = row[ScholarshipRenewalsTable.scholarshipId]
        val scholarshipTitle = row[ScholarshipsTable.title]
        val now = Instant.now()

        ScholarshipRenewalsTable.update({ ScholarshipRenewalsTable.id eq renewalId }) {
            it[status] = "approved"
            it[reviewedAt] = now
            it[reviewedBy] = adminId
            it[remarks] = req.remarks
        }

        // Apply fee waiver for renewal
        try {
            feeService.applyScholarship(studentId, scholarshipId, schoolId)
        } catch (e: Exception) {
            println("SCHOLARSHIP: Fee integration failed for renewal $renewalId: ${e.message}")
        }

        // Find parent to notify
        val originalApp = ScholarshipApplicationsTable.selectAll()
            .where { ScholarshipApplicationsTable.id eq row[ScholarshipRenewalsTable.originalApplicationId] }
            .singleOrNull()
        val parentId = originalApp?.get(ScholarshipApplicationsTable.parentId)

        if (parentId != null) {
            notificationService?.send(
                SendNotificationRequest(
                    title = "Scholarship Renewal Approved!",
                    body = "Renewal for '$scholarshipTitle' has been approved for the new academic year.",
                    userIds = listOf(parentId.toString()),
                    deepLink = "vidyaprayag://parent/scholarships",
                    data = mapOf("type" to "scholarship_renewal_approved", "renewalId" to renewalId.toString()),
                )
            )
        }

        ScholarshipRenewalsTable.selectAll()
            .where { ScholarshipRenewalsTable.id eq renewalId }
            .single()
            .let { rowToRenewal(it) }
    }

    suspend fun rejectRenewal(schoolId: UUID, renewalId: UUID, adminId: UUID, req: RejectApplicationRequest): ScholarshipRenewalDto? = dbQuery {
        val row = ScholarshipRenewalsTable.join(
            ScholarshipsTable,
            JoinType.INNER,
            ScholarshipRenewalsTable.scholarshipId,
            ScholarshipsTable.id,
        ).selectAll()
            .where {
                (ScholarshipRenewalsTable.id eq renewalId) and
                (ScholarshipRenewalsTable.schoolId eq schoolId) and
                (ScholarshipRenewalsTable.status eq "pending")
            }
            .singleOrNull()
            ?: return@dbQuery null

        val scholarshipTitle = row[ScholarshipsTable.title]
        val now = Instant.now()

        ScholarshipRenewalsTable.update({ ScholarshipRenewalsTable.id eq renewalId }) {
            it[status] = "rejected"
            it[reviewedAt] = now
            it[reviewedBy] = adminId
            it[remarks] = req.remarks
        }

        val originalApp = ScholarshipApplicationsTable.selectAll()
            .where { ScholarshipApplicationsTable.id eq row[ScholarshipRenewalsTable.originalApplicationId] }
            .singleOrNull()
        val parentId = originalApp?.get(ScholarshipApplicationsTable.parentId)

        if (parentId != null) {
            notificationService?.send(
                SendNotificationRequest(
                    title = "Scholarship Renewal Update",
                    body = "Renewal for '$scholarshipTitle' was not approved. ${req.remarks}",
                    userIds = listOf(parentId.toString()),
                    deepLink = "vidyaprayag://parent/scholarships",
                    data = mapOf("type" to "scholarship_renewal_rejected", "renewalId" to renewalId.toString()),
                )
            )
        }

        ScholarshipRenewalsTable.selectAll()
            .where { ScholarshipRenewalsTable.id eq renewalId }
            .single()
            .let { rowToRenewal(it) }
    }

    // ── Parent: Apply + View ──────────────────────────────────────────────

    suspend fun applyScholarship(parentId: UUID, req: ApplyScholarshipRequest): ScholarshipApplicationDto? = dbQuery {
        val scholarshipId = UUID.fromString(req.scholarshipId)
        val studentId = UUID.fromString(req.childId)

        // Validate scholarship exists and is active
        val scholarship = ScholarshipsTable.selectAll()
            .where {
                (ScholarshipsTable.id eq scholarshipId) and (ScholarshipsTable.isActive eq true)
            }
            .singleOrNull()
            ?: return@dbQuery null

        // Check for existing active application (not REJECTED)
        val existing = ScholarshipApplicationsTable.selectAll()
            .where {
                (ScholarshipApplicationsTable.scholarshipId eq scholarshipId) and
                (ScholarshipApplicationsTable.studentId eq studentId) and
                (ScholarshipApplicationsTable.status neq "REJECTED")
            }
            .any()

        if (existing) return@dbQuery null  // Already applied

        // Check if application window is open (end date not passed)
        val endDate = scholarship[ScholarshipsTable.endDate]
        if (endDate != null && endDate.isNotBlank()) {
            val today = LocalDate.now().toString()
            if (today > endDate) return@dbQuery null  // Window closed
        }

        val now = Instant.now()
        val documentUrlsJson = if (req.documents.isNotEmpty()) json.encodeToString(req.documents) else null

        val id = ScholarshipApplicationsTable.insert {
            it[ScholarshipApplicationsTable.parentId] = parentId
            it[ScholarshipApplicationsTable.scholarshipId] = scholarshipId
            it[ScholarshipApplicationsTable.studentId] = studentId
            it[ScholarshipApplicationsTable.institution] = scholarship[ScholarshipsTable.title]
            it[ScholarshipApplicationsTable.program] = scholarship[ScholarshipsTable.category]
            it[ScholarshipApplicationsTable.status] = "PENDING"
            it[ScholarshipApplicationsTable.iconName] = "school"
            it[ScholarshipApplicationsTable.documentUrls] = documentUrlsJson
            it[ScholarshipApplicationsTable.parentApplicationText] = req.applicationText
            it[ScholarshipApplicationsTable.position] = 0
            it[ScholarshipApplicationsTable.createdAt] = now
            it[ScholarshipApplicationsTable.updatedAt] = now
        }[ScholarshipApplicationsTable.id].value

        ScholarshipApplicationsTable.selectAll()
            .where { ScholarshipApplicationsTable.id eq id }
            .single()
            .let { rowToApplication(it) }
    }

    suspend fun getParentApplications(parentId: UUID): List<ScholarshipApplicationDto> = dbQuery {
        ScholarshipApplicationsTable.selectAll()
            .where { ScholarshipApplicationsTable.parentId eq parentId }
            .orderBy(ScholarshipApplicationsTable.createdAt, SortOrder.DESC)
            .map { rowToApplication(it) }
    }

    suspend fun applyRenewal(parentId: UUID, req: ApplyRenewalRequest): ScholarshipRenewalDto? = dbQuery {
        val scholarshipId = UUID.fromString(req.scholarshipId)
        val originalAppId = UUID.fromString(req.originalApplicationId)
        val studentId = UUID.fromString(req.childId)
        val academicYearId = UUID.fromString(req.academicYearId)

        // Validate original application was approved
        val originalApp = ScholarshipApplicationsTable.selectAll()
            .where {
                (ScholarshipApplicationsTable.id eq originalAppId) and
                (ScholarshipApplicationsTable.parentId eq parentId) and
                (ScholarshipApplicationsTable.status eq "APPROVED" or (ScholarshipApplicationsTable.status eq "DISBURSED"))
            }
            .singleOrNull()
            ?: return@dbQuery null

        // Validate scholarship is renewable
        val scholarship = ScholarshipsTable.selectAll()
            .where { ScholarshipsTable.id eq scholarshipId }
            .singleOrNull()
            ?: return@dbQuery null

        if (!scholarship[ScholarshipsTable.isRenewable]) return@dbQuery null

        val schoolId = scholarship[ScholarshipsTable.schoolId] ?: return@dbQuery null
        val now = Instant.now()
        val documentUrlsJson = if (req.documents.isNotEmpty()) json.encodeToString(req.documents) else null

        val id = ScholarshipRenewalsTable.insert {
            it[ScholarshipRenewalsTable.originalApplicationId] = originalAppId
            it[ScholarshipRenewalsTable.studentId] = studentId
            it[ScholarshipRenewalsTable.scholarshipId] = scholarshipId
            it[ScholarshipRenewalsTable.schoolId] = schoolId
            it[ScholarshipRenewalsTable.academicYearId] = academicYearId
            it[ScholarshipRenewalsTable.status] = "pending"
            it[ScholarshipRenewalsTable.documentUrls] = documentUrlsJson
            it[ScholarshipRenewalsTable.appliedAt] = now
        }[ScholarshipRenewalsTable.id].value

        ScholarshipRenewalsTable.selectAll()
            .where { ScholarshipRenewalsTable.id eq id }
            .single()
            .let { rowToRenewal(it) }
    }

    // ── Parent: Gamified Scholarships Response ────────────────────────────

    suspend fun getParentScholarships(parentId: UUID): ParentScholarshipsResponse = dbQuery {
        // Get all active scholarships (school-scoped via parent's children)
        val childSchoolIds = ChildrenTable
            .innerJoin(ParentChildLinksTable, { ChildrenTable.parentId eq ParentChildLinksTable.parentId })
            .selectAll()
            .where { ParentChildLinksTable.parentId eq parentId }
            .map { it[ChildrenTable.schoolId] }
            .distinct()

        val scholarships = if (childSchoolIds.isNotEmpty()) {
            ScholarshipsTable.selectAll()
                .where {
                    (ScholarshipsTable.isActive eq true) and
                    (ScholarshipsTable.schoolId inList childSchoolIds)
                }
                .orderBy(ScholarshipsTable.position, SortOrder.ASC)
                .map { rowToScheme(it) }
        } else {
            // Fallback: show all active scholarships (for legacy compat)
            ScholarshipsTable.selectAll()
                .where { ScholarshipsTable.isActive eq true }
                .orderBy(ScholarshipsTable.position, SortOrder.ASC)
                .map { rowToScheme(it) }
        }

        // Get parent's applications
        val applications = ScholarshipApplicationsTable.selectAll()
            .where { ScholarshipApplicationsTable.parentId eq parentId }
            .orderBy(ScholarshipApplicationsTable.createdAt, SortOrder.DESC)
            .map { rowToApplication(it) }

        // Gamification: profile strength based on application footprint
        val totalApplications = applications.size
        val approvedCount = applications.count { it.status in listOf("APPROVED", "DISBURSED") }
        val totalAwarded = applications.filter { it.disbursementAmount != null }.sumOf { it.disbursementAmount ?: 0.0 }

        // Profile strength: 40 base + 15 per application, capped at 100
        val profileStrength = (40 + totalApplications * 15).coerceAtMost(100)

        // Current level: based on approved count
        val currentLevel = when {
            approvedCount >= 5 -> 5
            approvedCount >= 3 -> 4
            approvedCount >= 2 -> 3
            approvedCount >= 1 -> 2
            else -> 1
        }

        // Streak days: derived from application activity (simplified)
        val streakDays = totalApplications.coerceAtLeast(0)

        ParentScholarshipsResponse(
            scholarships = scholarships,
            applications = applications,
            gamification = GamificationData(
                profileStrength = profileStrength,
                streakDays = streakDays,
                currentLevel = currentLevel,
                totalApplications = totalApplications,
                approvedCount = approvedCount,
                totalAwarded = totalAwarded,
            ),
        )
    }

    // ── Row Mappers ───────────────────────────────────────────────────────

    private fun rowToScheme(row: ResultRow): ScholarshipSchemeDto = ScholarshipSchemeDto(
        id = row[ScholarshipsTable.id].value.toString(),
        schoolId = row[ScholarshipsTable.schoolId]?.toString(),
        title = row[ScholarshipsTable.title],
        description = row[ScholarshipsTable.description],
        amount = row[ScholarshipsTable.amount],
        numericAmount = row[ScholarshipsTable.numericAmount],
        scholarshipType = row[ScholarshipsTable.scholarshipType],
        waiverPercentage = row[ScholarshipsTable.waiverPercentage],
        eligibilityCriteria = row[ScholarshipsTable.eligibilityCriteria],
        category = row[ScholarshipsTable.category],
        startDate = row[ScholarshipsTable.startDate],
        endDate = row[ScholarshipsTable.endDate],
        isRenewable = row[ScholarshipsTable.isRenewable],
        renewalPeriodMonths = row[ScholarshipsTable.renewalPeriodMonths],
        isActive = row[ScholarshipsTable.isActive],
        isCritical = row[ScholarshipsTable.isCritical],
        timeLeft = row[ScholarshipsTable.timeLeft],
        position = row[ScholarshipsTable.position],
        createdAt = row[ScholarshipsTable.createdAt].toString(),
        updatedAt = row[ScholarshipsTable.updatedAt].toString(),
    )

    private fun rowToApplication(row: ResultRow): ScholarshipApplicationDto {
        val docUrlsText = row.getOrNull(ScholarshipApplicationsTable.documentUrls)
        val docUrls = try {
            if (docUrlsText != null && docUrlsText.isNotBlank()) {
                json.decodeFromString<List<String>>(docUrlsText)
            } else emptyList()
            } catch (e: Exception) {
                emptyList()
            }

        val scholarshipTitle = row.getOrNull(ScholarshipsTable.title)
        val studentName = row.getOrNull(ScholarshipApplicationsTable.studentId)?.let { sid ->
            ChildrenTable.selectAll().where { ChildrenTable.id eq sid }.singleOrNull()
                ?.get(ChildrenTable.childName)
        }

        return ScholarshipApplicationDto(
            id = row[ScholarshipApplicationsTable.id].value.toString(),
            scholarshipId = row[ScholarshipApplicationsTable.scholarshipId]?.toString(),
            scholarshipTitle = scholarshipTitle,
            parentId = row[ScholarshipApplicationsTable.parentId].toString(),
            studentId = row[ScholarshipApplicationsTable.studentId]?.toString(),
            studentName = studentName,
            institution = row[ScholarshipApplicationsTable.institution],
            program = row[ScholarshipApplicationsTable.program],
            status = row[ScholarshipApplicationsTable.status],
            documentUrls = docUrls,
            parentApplicationText = row[ScholarshipApplicationsTable.parentApplicationText],
            academicYearId = row[ScholarshipApplicationsTable.academicYearId]?.toString(),
            iconName = row[ScholarshipApplicationsTable.iconName],
            remarks = row[ScholarshipApplicationsTable.remarks],
            reviewedAt = row[ScholarshipApplicationsTable.reviewedAt]?.toString(),
            reviewedBy = row[ScholarshipApplicationsTable.reviewedBy]?.toString(),
            disbursementAmount = row[ScholarshipApplicationsTable.disbursementAmount],
            disbursementDate = row[ScholarshipApplicationsTable.disbursementDate]?.toString(),
            disbursementReference = row[ScholarshipApplicationsTable.disbursementReference],
            appliedAt = row[ScholarshipApplicationsTable.createdAt].toString(),
            updatedAt = row[ScholarshipApplicationsTable.updatedAt].toString(),
        )
    }

    private fun rowToRenewal(row: ResultRow): ScholarshipRenewalDto {
        val docUrlsText = row.getOrNull(ScholarshipRenewalsTable.documentUrls)
        val docUrls = try {
            if (docUrlsText != null && docUrlsText.isNotBlank()) {
                json.decodeFromString<List<String>>(docUrlsText)
            } else emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        val scholarshipTitle = row.getOrNull(ScholarshipsTable.title)

        return ScholarshipRenewalDto(
            id = row[ScholarshipRenewalsTable.id].value.toString(),
            originalApplicationId = row[ScholarshipRenewalsTable.originalApplicationId].toString(),
            studentId = row[ScholarshipRenewalsTable.studentId].toString(),
            scholarshipId = row[ScholarshipRenewalsTable.scholarshipId].toString(),
            scholarshipTitle = scholarshipTitle,
            schoolId = row[ScholarshipRenewalsTable.schoolId].toString(),
            academicYearId = row[ScholarshipRenewalsTable.academicYearId].toString(),
            status = row[ScholarshipRenewalsTable.status],
            documentUrls = docUrls,
            appliedAt = row[ScholarshipRenewalsTable.appliedAt].toString(),
            reviewedAt = row[ScholarshipRenewalsTable.reviewedAt]?.toString(),
            reviewedBy = row[ScholarshipRenewalsTable.reviewedBy]?.toString(),
            remarks = row[ScholarshipRenewalsTable.remarks],
        )
    }
}
