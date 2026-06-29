// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/data/TutorSessionRepository.kt
package com.littlebridge.enrollplus.feature.tutor.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TutorSessionsTable
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

/**
 * Repository for [TutorSessionsTable] — pure persistence, no business logic.
 * SOLID: S (persistence only), D (parameterized queries via Exposed).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §12.2
 */
class TutorSessionRepository {

    data class SessionRow(
        val id: UUID,
        val schoolId: UUID,
        val childId: UUID,
        val subjectId: UUID?,
        val academicYearId: UUID?,
        val mode: String,
        val intentClass: String?,
        val turns: String,
        val groundedRefs: String,
        val providerUsed: String?,
        val tokensUsed: Int,
        val cacheHit: Boolean,
        val safetyFlag: String?,
        val createdAt: Instant,
        val updatedAt: Instant,
    )

    suspend fun insert(
        schoolId: UUID,
        childId: UUID,
        subjectId: UUID? = null,
        academicYearId: UUID? = null,
        mode: String = "DOUBT",
        intentClass: String? = null,
        turns: String = "[]",
        groundedRefs: String = "[]",
        providerUsed: String? = null,
        tokensUsed: Int = 0,
        cacheHit: Boolean = false,
        safetyFlag: String? = null,
    ): UUID = dbQuery {
        val now = Instant.now()
        TutorSessionsTable.insert {
            it[TutorSessionsTable.schoolId] = schoolId
            it[TutorSessionsTable.childId] = childId
            it[TutorSessionsTable.subjectId] = subjectId
            it[TutorSessionsTable.academicYearId] = academicYearId
            it[TutorSessionsTable.mode] = mode
            it[TutorSessionsTable.intentClass] = intentClass
            it[TutorSessionsTable.turns] = turns
            it[TutorSessionsTable.groundedRefs] = groundedRefs
            it[TutorSessionsTable.providerUsed] = providerUsed
            it[TutorSessionsTable.tokensUsed] = tokensUsed
            it[TutorSessionsTable.cacheHit] = cacheHit
            it[TutorSessionsTable.safetyFlag] = safetyFlag
            it[TutorSessionsTable.createdAt] = now
            it[TutorSessionsTable.updatedAt] = now
        }[TutorSessionsTable.id].value
    }

    suspend fun updateTurns(
        id: UUID,
        turns: String,
        groundedRefs: String,
        tokensUsed: Int,
        safetyFlag: String? = null,
    ) = dbQuery {
        TutorSessionsTable.update({ TutorSessionsTable.id eq id }) {
            it[TutorSessionsTable.turns] = turns
            it[TutorSessionsTable.groundedRefs] = groundedRefs
            it[TutorSessionsTable.tokensUsed] = tokensUsed
            if (safetyFlag != null) it[TutorSessionsTable.safetyFlag] = safetyFlag
            it[TutorSessionsTable.updatedAt] = Instant.now()
        }
    }

    suspend fun findById(id: UUID): SessionRow? = dbQuery {
        TutorSessionsTable.selectAll().where { TutorSessionsTable.id eq id }
            .singleOrNull()?.let(::mapRow)
    }

    suspend fun findByChildAndSubject(
        childId: UUID,
        subjectId: UUID,
        limit: Int = 20,
    ): List<SessionRow> = dbQuery {
        TutorSessionsTable.selectAll().where {
            (TutorSessionsTable.childId eq childId) and
            (TutorSessionsTable.subjectId eq subjectId)
        }.orderBy(TutorSessionsTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
            .limit(limit)
            .map(::mapRow)
    }

    private fun mapRow(row: org.jetbrains.exposed.sql.ResultRow) = SessionRow(
        id = row[TutorSessionsTable.id].value,
        schoolId = row[TutorSessionsTable.schoolId],
        childId = row[TutorSessionsTable.childId],
        subjectId = row[TutorSessionsTable.subjectId],
        academicYearId = row[TutorSessionsTable.academicYearId],
        mode = row[TutorSessionsTable.mode],
        intentClass = row[TutorSessionsTable.intentClass],
        turns = row[TutorSessionsTable.turns],
        groundedRefs = row[TutorSessionsTable.groundedRefs],
        providerUsed = row[TutorSessionsTable.providerUsed],
        tokensUsed = row[TutorSessionsTable.tokensUsed],
        cacheHit = row[TutorSessionsTable.cacheHit],
        safetyFlag = row[TutorSessionsTable.safetyFlag],
        createdAt = row[TutorSessionsTable.createdAt],
        updatedAt = row[TutorSessionsTable.updatedAt],
    )
}
