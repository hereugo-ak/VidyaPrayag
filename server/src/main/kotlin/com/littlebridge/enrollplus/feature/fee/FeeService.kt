/*
 * File: FeeService.kt
 * Module: feature.fee
 *
 * New service for fee operations, created as part of SCHOLARSHIP_WORKFLOW_SPEC.md
 * to handle scholarship waiver/discount application on fee records.
 *
 * Follows SOLID principles: single responsibility (fee operations), depends on
 * DB via Exposed (no direct coupling to other services).
 *
 * Called by ScholarshipService.onApproval() to apply waivers to fee records.
 */
package com.littlebridge.enrollplus.feature.fee

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.FeeRecordsTable
import com.littlebridge.enrollplus.db.ScholarshipsTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class FeeService {

    /**
     * Apply a scholarship waiver/discount to fee records for a student.
     *
     * Called by ScholarshipService when an application is approved. Updates
     * the student's DUE/OVERDUE fee records with the scholarship waiver.
     *
     * Scholarship types:
     *   - fixed: reduces fee by [numericAmount] (fixed amount)
     *   - full_waiver: sets fee amount to 0 (100% waiver)
     *   - partial_waiver: reduces fee by [waiverPercentage]% of original amount
     *
     * @param studentId   the student whose fees should be waived
     * @param scholarshipId the scholarship scheme being applied
     * @param schoolId    the school scope
     * @return count of fee records updated
     */
    suspend fun applyScholarship(
        studentId: UUID,
        scholarshipId: UUID,
        schoolId: UUID
    ): Int = dbQuery {
        // Fetch the scholarship to get type and amount/percentage
        val scholarship = ScholarshipsTable.selectAll()
            .where { ScholarshipsTable.id eq scholarshipId }
            .singleOrNull()
            ?: return@dbQuery 0

        val scholarshipType = scholarship[ScholarshipsTable.scholarshipType]
        val waiverPercentage = scholarship[ScholarshipsTable.waiverPercentage]
        val numericAmount = scholarship[ScholarshipsTable.numericAmount] ?: 0.0

        // Find all DUE/OVERDUE fee records for this student in this school
        val feeRecords = FeeRecordsTable.selectAll()
            .where {
                (FeeRecordsTable.childId eq studentId) and
                (FeeRecordsTable.schoolId eq schoolId) and
                ((FeeRecordsTable.status eq "DUE") or (FeeRecordsTable.status eq "OVERDUE"))
            }
            .toList()

        if (feeRecords.isEmpty()) return@dbQuery 0

        var updatedCount = 0
        feeRecords.forEach { row ->
            val feeId = row[FeeRecordsTable.id].value
            val originalAmount = row[FeeRecordsTable.amount]

            val waiverAmount = when (scholarshipType) {
                "full_waiver" -> originalAmount
                "partial_waiver" -> {
                    val pct = waiverPercentage ?: 0f
                    originalAmount * (pct / 100.0)
                }
                else -> { // "fixed"
                    minOf(numericAmount, originalAmount)
                }
            }

            val newAmount = (originalAmount - waiverAmount).coerceAtLeast(0.0)
            val newStatus = if (newAmount <= 0.0) "PAID" else row[FeeRecordsTable.status]

            FeeRecordsTable.update({ FeeRecordsTable.id eq feeId }) {
                it[FeeRecordsTable.originalAmount] = originalAmount
                it[FeeRecordsTable.scholarshipId] = scholarshipId
                it[FeeRecordsTable.scholarshipType] = scholarshipType
                it[FeeRecordsTable.scholarshipAmount] = waiverAmount
                it[FeeRecordsTable.amount] = newAmount
                it[FeeRecordsTable.status] = newStatus
                it[FeeRecordsTable.updatedAt] = Instant.now()
            }
            updatedCount++
        }

        updatedCount
    }

    /**
     * Remove a scholarship waiver from fee records (for rollback if needed).
     */
    suspend fun removeScholarship(
        studentId: UUID,
        scholarshipId: UUID,
        schoolId: UUID
    ): Int = dbQuery {
        val records = FeeRecordsTable.selectAll()
            .where {
                (FeeRecordsTable.childId eq studentId) and
                (FeeRecordsTable.schoolId eq schoolId) and
                (FeeRecordsTable.scholarshipId eq scholarshipId)
            }
            .toList()

        var updatedCount = 0
        records.forEach { row ->
            val feeId = row[FeeRecordsTable.id].value
            val original = row[FeeRecordsTable.originalAmount] ?: row[FeeRecordsTable.amount]

            FeeRecordsTable.update({ FeeRecordsTable.id eq feeId }) {
                it[FeeRecordsTable.amount] = original
                it[FeeRecordsTable.status] = "DUE"
                it[FeeRecordsTable.scholarshipId] = null
                it[FeeRecordsTable.scholarshipType] = null
                it[FeeRecordsTable.scholarshipAmount] = null
                it[FeeRecordsTable.originalAmount] = null
                it[FeeRecordsTable.updatedAt] = Instant.now()
            }
            updatedCount++
        }

        updatedCount
    }
}
