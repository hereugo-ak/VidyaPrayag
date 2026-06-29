// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/data/ReportFocusEffectivenessRepository.kt
package com.littlebridge.enrollplus.feature.reportcard.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ReportFocusEffectivenessTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

/**
 * Repository for [ReportFocusEffectivenessTable] — pure CRUD, no business logic.
 * SOLID: S (persistence only), D (parameterized queries).
 */
class ReportFocusEffectivenessRepository {

    data class FocusEffectivenessRow(
        val id: UUID,
        val schoolId: UUID,
        val focusArea: String,
        val term: String,
        val academicYearId: UUID?,
        val studentsTargeted: Int,
        val studentsImproved: Int,
        val avgDelta: Double,
        val effectivenessScore: Double,
        val sampleSize: Int,
        val confidence: String,
        val metadata: String?,
        val createdAt: Instant,
        val updatedAt: Instant,
    )

    private fun mapRow(row: org.jetbrains.exposed.sql.ResultRow) = FocusEffectivenessRow(
        id = row[ReportFocusEffectivenessTable.id].value,
        schoolId = row[ReportFocusEffectivenessTable.schoolId],
        focusArea = row[ReportFocusEffectivenessTable.focusArea],
        term = row[ReportFocusEffectivenessTable.term],
        academicYearId = row[ReportFocusEffectivenessTable.academicYearId],
        studentsTargeted = row[ReportFocusEffectivenessTable.studentsTargeted],
        studentsImproved = row[ReportFocusEffectivenessTable.studentsImproved],
        avgDelta = row[ReportFocusEffectivenessTable.avgDelta],
        effectivenessScore = row[ReportFocusEffectivenessTable.effectivenessScore],
        sampleSize = row[ReportFocusEffectivenessTable.sampleSize],
        confidence = row[ReportFocusEffectivenessTable.confidence],
        metadata = row[ReportFocusEffectivenessTable.metadata],
        createdAt = row[ReportFocusEffectivenessTable.createdAt],
        updatedAt = row[ReportFocusEffectivenessTable.updatedAt],
    )

    suspend fun upsert(
        schoolId: UUID,
        focusArea: String,
        term: String,
        academicYearId: UUID?,
        studentsTargeted: Int,
        studentsImproved: Int,
        avgDelta: Double,
        effectivenessScore: Double,
        sampleSize: Int,
        confidence: String,
        metadata: String? = null,
    ): UUID = dbQuery {
        val existing = ReportFocusEffectivenessTable.selectAll().where {
            (ReportFocusEffectivenessTable.schoolId eq schoolId) and
            (ReportFocusEffectivenessTable.focusArea eq focusArea) and
            (ReportFocusEffectivenessTable.term eq term) and
            (if (academicYearId != null)
                ReportFocusEffectivenessTable.academicYearId eq academicYearId
            else
                ReportFocusEffectivenessTable.academicYearId.isNull())
        }.singleOrNull()

        val now = Instant.now()
        if (existing != null) {
            ReportFocusEffectivenessTable.update({
                ReportFocusEffectivenessTable.id eq existing[ReportFocusEffectivenessTable.id].value
            }) {
                it[ReportFocusEffectivenessTable.studentsTargeted] = studentsTargeted
                it[ReportFocusEffectivenessTable.studentsImproved] = studentsImproved
                it[ReportFocusEffectivenessTable.avgDelta] = avgDelta
                it[ReportFocusEffectivenessTable.effectivenessScore] = effectivenessScore
                it[ReportFocusEffectivenessTable.sampleSize] = sampleSize
                it[ReportFocusEffectivenessTable.confidence] = confidence
                it[ReportFocusEffectivenessTable.metadata] = metadata
                it[ReportFocusEffectivenessTable.updatedAt] = now
            }
            existing[ReportFocusEffectivenessTable.id].value
        } else {
            ReportFocusEffectivenessTable.insert {
                it[ReportFocusEffectivenessTable.schoolId] = schoolId
                it[ReportFocusEffectivenessTable.focusArea] = focusArea
                it[ReportFocusEffectivenessTable.term] = term
                it[ReportFocusEffectivenessTable.academicYearId] = academicYearId
                it[ReportFocusEffectivenessTable.studentsTargeted] = studentsTargeted
                it[ReportFocusEffectivenessTable.studentsImproved] = studentsImproved
                it[ReportFocusEffectivenessTable.avgDelta] = avgDelta
                it[ReportFocusEffectivenessTable.effectivenessScore] = effectivenessScore
                it[ReportFocusEffectivenessTable.sampleSize] = sampleSize
                it[ReportFocusEffectivenessTable.confidence] = confidence
                it[ReportFocusEffectivenessTable.metadata] = metadata
                it[ReportFocusEffectivenessTable.createdAt] = now
                it[ReportFocusEffectivenessTable.updatedAt] = now
            }[ReportFocusEffectivenessTable.id].value
        }
    }

    suspend fun findBySchool(schoolId: UUID): List<FocusEffectivenessRow> = dbQuery {
        ReportFocusEffectivenessTable.selectAll().where {
            ReportFocusEffectivenessTable.schoolId eq schoolId
        }.orderBy(ReportFocusEffectivenessTable.effectivenessScore, org.jetbrains.exposed.sql.SortOrder.DESC)
            .map(::mapRow)
    }

    suspend fun findBySchoolAndTerm(
        schoolId: UUID,
        term: String,
        academicYearId: UUID?,
    ): List<FocusEffectivenessRow> = dbQuery {
        ReportFocusEffectivenessTable.selectAll().where {
            (ReportFocusEffectivenessTable.schoolId eq schoolId) and
            (ReportFocusEffectivenessTable.term eq term) and
            (if (academicYearId != null)
                ReportFocusEffectivenessTable.academicYearId eq academicYearId
            else
                ReportFocusEffectivenessTable.academicYearId.isNull())
        }.map(::mapRow)
    }
}
