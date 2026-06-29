// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/data/TutorMisconceptionRepository.kt
package com.littlebridge.enrollplus.feature.tutor.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TutorMisconceptionsTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

/**
 * Repository for [TutorMisconceptionsTable] — pure persistence for the
 * class-wide misconception library. The agent's logMisconception tool writes
 * here; the Teacher Heatmap reads from here.
 *
 * SOLID: S (persistence only), D (parameterized queries via Exposed).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §12.5
 */
class TutorMisconceptionRepository {

    data class MisconceptionRow(
        val id: UUID,
        val schoolId: UUID,
        val classId: UUID,
        val subjectId: UUID,
        val topicId: UUID,
        val childId: UUID,
        val misconceptionType: String,
        val evidence: String,
        val resolved: Boolean,
        val surfacedToTeacher: Boolean,
        val createdAt: Instant,
    )

    suspend fun insert(
        schoolId: UUID,
        classId: UUID,
        subjectId: UUID,
        topicId: UUID,
        childId: UUID,
        misconceptionType: String,
        evidence: String = "",
        surfacedToTeacher: Boolean = false,
    ): UUID = dbQuery {
        TutorMisconceptionsTable.insert {
            it[TutorMisconceptionsTable.schoolId] = schoolId
            it[TutorMisconceptionsTable.classId] = classId
            it[TutorMisconceptionsTable.subjectId] = subjectId
            it[TutorMisconceptionsTable.topicId] = topicId
            it[TutorMisconceptionsTable.childId] = childId
            it[TutorMisconceptionsTable.misconceptionType] = misconceptionType
            it[TutorMisconceptionsTable.evidence] = evidence
            it[TutorMisconceptionsTable.surfacedToTeacher] = surfacedToTeacher
            it[TutorMisconceptionsTable.createdAt] = Instant.now()
        }[TutorMisconceptionsTable.id].value
    }

    suspend fun findByClassAndSubject(
        classId: UUID,
        subjectId: UUID,
        includeResolved: Boolean = false,
    ): List<MisconceptionRow> = dbQuery {
        TutorMisconceptionsTable.selectAll().where {
            (TutorMisconceptionsTable.classId eq classId) and
            (TutorMisconceptionsTable.subjectId eq subjectId) and
            (if (includeResolved) org.jetbrains.exposed.sql.Op.TRUE else TutorMisconceptionsTable.resolved eq false)
        }.orderBy(TutorMisconceptionsTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
            .map(::mapRow)
    }

    suspend fun findByChild(
        childId: UUID,
        includeResolved: Boolean = false,
    ): List<MisconceptionRow> = dbQuery {
        TutorMisconceptionsTable.selectAll().where {
            (TutorMisconceptionsTable.childId eq childId) and
            (if (includeResolved) org.jetbrains.exposed.sql.Op.TRUE else TutorMisconceptionsTable.resolved eq false)
        }.orderBy(TutorMisconceptionsTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
            .map(::mapRow)
    }

    suspend fun markResolved(id: UUID) = dbQuery {
        TutorMisconceptionsTable.update({ TutorMisconceptionsTable.id eq id }) {
            it[TutorMisconceptionsTable.resolved] = true
        }
    }

    suspend fun surfaceToTeacher(id: UUID) = dbQuery {
        TutorMisconceptionsTable.update({ TutorMisconceptionsTable.id eq id }) {
            it[TutorMisconceptionsTable.surfacedToTeacher] = true
        }
    }

    private fun mapRow(row: org.jetbrains.exposed.sql.ResultRow) = MisconceptionRow(
        id = row[TutorMisconceptionsTable.id].value,
        schoolId = row[TutorMisconceptionsTable.schoolId],
        classId = row[TutorMisconceptionsTable.classId],
        subjectId = row[TutorMisconceptionsTable.subjectId],
        topicId = row[TutorMisconceptionsTable.topicId],
        childId = row[TutorMisconceptionsTable.childId],
        misconceptionType = row[TutorMisconceptionsTable.misconceptionType],
        evidence = row[TutorMisconceptionsTable.evidence],
        resolved = row[TutorMisconceptionsTable.resolved],
        surfacedToTeacher = row[TutorMisconceptionsTable.surfacedToTeacher],
        createdAt = row[TutorMisconceptionsTable.createdAt],
    )
}
