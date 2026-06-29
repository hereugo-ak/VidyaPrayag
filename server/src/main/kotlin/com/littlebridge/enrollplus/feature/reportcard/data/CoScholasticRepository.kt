// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/data/CoScholasticRepository.kt
package com.littlebridge.enrollplus.feature.reportcard.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.CoScholasticRecordsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

/**
 * Repository for [CoScholasticRecordsTable] — pure read, no business logic.
 * Graceful when empty — returns empty lists, never throws.
 * SOLID: S (persistence only), D (parameterized queries).
 */
class CoScholasticRepository {

    data class CoScholasticRow(
        val id: UUID,
        val schoolId: UUID,
        val studentId: UUID,
        val classId: UUID?,
        val term: String,
        val academicYearId: UUID?,
        val category: String,
        val activityName: String,
        val grade: String?,
        val descriptor: String?,
        val teacherRemarks: String?,
    )

    private fun mapRow(row: org.jetbrains.exposed.sql.ResultRow) = CoScholasticRow(
        id = row[CoScholasticRecordsTable.id].value,
        schoolId = row[CoScholasticRecordsTable.schoolId],
        studentId = row[CoScholasticRecordsTable.studentId],
        classId = row[CoScholasticRecordsTable.classId],
        term = row[CoScholasticRecordsTable.term],
        academicYearId = row[CoScholasticRecordsTable.academicYearId],
        category = row[CoScholasticRecordsTable.category],
        activityName = row[CoScholasticRecordsTable.activityName],
        grade = row[CoScholasticRecordsTable.grade],
        descriptor = row[CoScholasticRecordsTable.descriptor],
        teacherRemarks = row[CoScholasticRecordsTable.teacherRemarks],
    )

    suspend fun findByStudentAndTerm(
        schoolId: UUID,
        studentId: UUID,
        term: String,
        academicYearId: UUID?,
    ): List<CoScholasticRow> = dbQuery {
        CoScholasticRecordsTable.selectAll().where {
            (CoScholasticRecordsTable.schoolId eq schoolId) and
            (CoScholasticRecordsTable.studentId eq studentId) and
            (CoScholasticRecordsTable.term eq term) and
            (if (academicYearId != null)
                CoScholasticRecordsTable.academicYearId eq academicYearId
            else
                CoScholasticRecordsTable.academicYearId.isNull())
        }.map(::mapRow)
    }
}
