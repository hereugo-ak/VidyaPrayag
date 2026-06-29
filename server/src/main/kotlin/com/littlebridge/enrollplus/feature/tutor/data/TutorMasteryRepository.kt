// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/data/TutorMasteryRepository.kt
package com.littlebridge.enrollplus.feature.tutor.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TutorMasteryTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

/**
 * Repository for [TutorMasteryTable] — pure persistence for grounded mastery.
 * Mastery values are derived from real marks and/or practice outcomes by the
 * service layer (LearnerBundleBuilder, TutorActService). This repo only
 * reads and writes rows.
 *
 * SOLID: S (persistence only), D (parameterized queries via Exposed).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §12.4
 */
class TutorMasteryRepository {

    data class MasteryRow(
        val id: UUID,
        val schoolId: UUID,
        val childId: UUID,
        val subjectId: UUID,
        val topicId: UUID,
        val mastery: Double,
        val source: String,
        val attempts: Int,
        val correct: Int,
        val updatedAt: Instant,
        val createdAt: Instant,
    )

    suspend fun upsert(
        schoolId: UUID,
        childId: UUID,
        subjectId: UUID,
        topicId: UUID,
        mastery: Double,
        source: String = "MARKS",
        attempts: Int = 0,
        correct: Int = 0,
    ): UUID = dbQuery {
        val existing = TutorMasteryTable.selectAll().where {
            (TutorMasteryTable.childId eq childId) and
            (TutorMasteryTable.topicId eq topicId)
        }.singleOrNull()

        if (existing != null) {
            TutorMasteryTable.update({
                (TutorMasteryTable.childId eq childId) and
                (TutorMasteryTable.topicId eq topicId)
            }) {
                it[TutorMasteryTable.mastery] = mastery
                it[TutorMasteryTable.masterySource] = source
                it[TutorMasteryTable.attempts] = attempts
                it[TutorMasteryTable.correct] = correct
                it[TutorMasteryTable.updatedAt] = Instant.now()
            }
            existing[TutorMasteryTable.id].value
        } else {
            val now = Instant.now()
            TutorMasteryTable.insert {
                it[TutorMasteryTable.schoolId] = schoolId
                it[TutorMasteryTable.childId] = childId
                it[TutorMasteryTable.subjectId] = subjectId
                it[TutorMasteryTable.topicId] = topicId
                it[TutorMasteryTable.mastery] = mastery
                it[TutorMasteryTable.masterySource] = source
                it[TutorMasteryTable.attempts] = attempts
                it[TutorMasteryTable.correct] = correct
                it[TutorMasteryTable.createdAt] = now
                it[TutorMasteryTable.updatedAt] = now
            }[TutorMasteryTable.id].value
        }
    }

    suspend fun findByChildAndTopic(
        childId: UUID,
        topicId: UUID,
    ): MasteryRow? = dbQuery {
        TutorMasteryTable.selectAll().where {
            (TutorMasteryTable.childId eq childId) and
            (TutorMasteryTable.topicId eq topicId)
        }.singleOrNull()?.let(::mapRow)
    }

    suspend fun findByChildAndSubject(
        childId: UUID,
        subjectId: UUID,
    ): List<MasteryRow> = dbQuery {
        TutorMasteryTable.selectAll().where {
            (TutorMasteryTable.childId eq childId) and
            (TutorMasteryTable.subjectId eq subjectId)
        }.map(::mapRow)
    }

    private fun mapRow(row: org.jetbrains.exposed.sql.ResultRow) = MasteryRow(
        id = row[TutorMasteryTable.id].value,
        schoolId = row[TutorMasteryTable.schoolId],
        childId = row[TutorMasteryTable.childId],
        subjectId = row[TutorMasteryTable.subjectId],
        topicId = row[TutorMasteryTable.topicId],
        mastery = row[TutorMasteryTable.mastery],
        source = row[TutorMasteryTable.masterySource],
        attempts = row[TutorMasteryTable.attempts],
        correct = row[TutorMasteryTable.correct],
        updatedAt = row[TutorMasteryTable.updatedAt],
        createdAt = row[TutorMasteryTable.createdAt],
    )
}
