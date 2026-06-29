// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/data/ReportCardTemplateRepository.kt
package com.littlebridge.enrollplus.feature.reportcard.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ReportCardTemplatesTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Repository for [ReportCardTemplatesTable] — pure read, no business logic.
 * SOLID: S (persistence only), D (parameterized queries).
 */
class ReportCardTemplateRepository {

    data class TemplateRow(
        val id: UUID,
        val schoolId: UUID?,
        val board: String,
        val gradeRange: String,
        val templateName: String,
        val version: Int,
        val gradingScale: String,
        val layout: String?,
        val includesHolistic: Boolean,
        val includesCoScholastic: Boolean,
        val includesAttendance: Boolean,
        val includesAiNarrative: Boolean,
        val isActive: Boolean,
    )

    private fun mapRow(row: org.jetbrains.exposed.sql.ResultRow) = TemplateRow(
        id = row[ReportCardTemplatesTable.id].value,
        schoolId = row[ReportCardTemplatesTable.schoolId],
        board = row[ReportCardTemplatesTable.board],
        gradeRange = row[ReportCardTemplatesTable.gradeRange],
        templateName = row[ReportCardTemplatesTable.templateName],
        version = row[ReportCardTemplatesTable.version],
        gradingScale = row[ReportCardTemplatesTable.gradingScale],
        layout = row[ReportCardTemplatesTable.layout],
        includesHolistic = row[ReportCardTemplatesTable.includesHolistic],
        includesCoScholastic = row[ReportCardTemplatesTable.includesCoScholastic],
        includesAttendance = row[ReportCardTemplatesTable.includesAttendance],
        includesAiNarrative = row[ReportCardTemplatesTable.includesAiNarrative],
        isActive = row[ReportCardTemplatesTable.isActive],
    )

    /**
     * Find the best matching template for a school's board and grade range.
     * Prefers school-specific templates over global ones.
     */
    suspend fun findForBoard(
        schoolId: UUID,
        board: String,
        gradeRange: String,
    ): TemplateRow? = dbQuery {
        // Try school-specific first
        val schoolSpecific = ReportCardTemplatesTable.selectAll().where {
            (ReportCardTemplatesTable.schoolId eq schoolId) and
            (ReportCardTemplatesTable.board eq board) and
            (ReportCardTemplatesTable.gradeRange eq gradeRange) and
            (ReportCardTemplatesTable.isActive eq true)
        }.singleOrNull()

        if (schoolSpecific != null) {
            return@dbQuery schoolSpecific.let(::mapRow)
        }

        // Fall back to global template
        ReportCardTemplatesTable.selectAll().where {
            (ReportCardTemplatesTable.schoolId.isNull()) and
            (ReportCardTemplatesTable.board eq board) and
            (ReportCardTemplatesTable.gradeRange eq gradeRange) and
            (ReportCardTemplatesTable.isActive eq true)
        }.singleOrNull()?.let(::mapRow)
    }

    /**
     * Find any active template for a board (ignores grade range).
     */
    suspend fun findAnyForBoard(board: String): TemplateRow? = dbQuery {
        ReportCardTemplatesTable.selectAll().where {
            (ReportCardTemplatesTable.board eq board) and
            (ReportCardTemplatesTable.isActive eq true)
        }.firstOrNull()?.let(::mapRow)
    }
}
