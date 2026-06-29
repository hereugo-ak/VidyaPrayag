// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/data/HolisticAssessmentRepository.kt
package com.littlebridge.enrollplus.feature.reportcard.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.HolisticAssessmentsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Repository for [HolisticAssessmentsTable] — pure read, no business logic.
 * Graceful when empty — returns empty lists, never throws.
 * SOLID: S (persistence only), D (parameterized queries).
 */
class HolisticAssessmentRepository {

    data class HolisticRow(
        val id: UUID,
        val schoolId: UUID,
        val studentId: UUID,
        val classId: UUID?,
        val term: String,
        val academicYearId: UUID?,
        val assessorType: String,
        val assessorId: UUID?,
        val criticalThinking: Int?,
        val creativity: Int?,
        val communication: Int?,
        val collaboration: Int?,
        val selfAwareness: Int?,
        val socialEmotional: Int?,
        val remarks: String?,
    )

    private fun mapRow(row: org.jetbrains.exposed.sql.ResultRow) = HolisticRow(
        id = row[HolisticAssessmentsTable.id].value,
        schoolId = row[HolisticAssessmentsTable.schoolId],
        studentId = row[HolisticAssessmentsTable.studentId],
        classId = row[HolisticAssessmentsTable.classId],
        term = row[HolisticAssessmentsTable.term],
        academicYearId = row[HolisticAssessmentsTable.academicYearId],
        assessorType = row[HolisticAssessmentsTable.assessorType],
        assessorId = row[HolisticAssessmentsTable.assessorId],
        criticalThinking = row[HolisticAssessmentsTable.criticalThinking],
        creativity = row[HolisticAssessmentsTable.creativity],
        communication = row[HolisticAssessmentsTable.communication],
        collaboration = row[HolisticAssessmentsTable.collaboration],
        selfAwareness = row[HolisticAssessmentsTable.selfAwareness],
        socialEmotional = row[HolisticAssessmentsTable.socialEmotional],
        remarks = row[HolisticAssessmentsTable.remarks],
    )

    suspend fun findByStudentAndTerm(
        schoolId: UUID,
        studentId: UUID,
        term: String,
        academicYearId: UUID?,
    ): List<HolisticRow> = dbQuery {
        HolisticAssessmentsTable.selectAll().where {
            (HolisticAssessmentsTable.schoolId eq schoolId) and
            (HolisticAssessmentsTable.studentId eq studentId) and
            (HolisticAssessmentsTable.term eq term) and
            (if (academicYearId != null)
                HolisticAssessmentsTable.academicYearId eq academicYearId
            else
                HolisticAssessmentsTable.academicYearId.isNull())
        }.map(::mapRow)
    }
}
