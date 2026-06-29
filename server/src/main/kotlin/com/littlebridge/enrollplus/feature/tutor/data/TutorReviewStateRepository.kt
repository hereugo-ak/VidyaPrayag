// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/tutor/data/TutorReviewStateRepository.kt
package com.littlebridge.enrollplus.feature.tutor.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.TutorReviewStateTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

/**
 * Repository for [TutorReviewStateTable] — pure persistence for the FSRS spine.
 * The [FsrsScheduler] owns the business logic (FSRS algorithm); this repo
 * only reads and writes rows.
 *
 * SOLID: S (persistence only), D (parameterized queries via Exposed).
 *
 * Spec: AI_TUTOR_2.0_AGENTIC_REDESIGN.md §12.3
 */
class TutorReviewStateRepository {

    data class ReviewStateRow(
        val id: UUID,
        val schoolId: UUID,
        val childId: UUID,
        val topicId: UUID,
        val stability: Double,
        val difficulty: Double,
        val dueAt: Instant,
        val reps: Int,
        val lapses: Int,
        val lastGrade: Int,
        val lastReviewedAt: Instant?,
        val createdAt: Instant,
        val updatedAt: Instant,
    )

    suspend fun upsert(
        schoolId: UUID,
        childId: UUID,
        topicId: UUID,
        stability: Double,
        difficulty: Double,
        dueAt: Instant,
        reps: Int,
        lapses: Int,
        lastGrade: Int,
        lastReviewedAt: Instant?,
    ): UUID = dbQuery {
        val existing = TutorReviewStateTable.selectAll().where {
            (TutorReviewStateTable.childId eq childId) and
            (TutorReviewStateTable.topicId eq topicId)
        }.singleOrNull()

        if (existing != null) {
            TutorReviewStateTable.update({
                (TutorReviewStateTable.childId eq childId) and
                (TutorReviewStateTable.topicId eq topicId)
            }) {
                it[TutorReviewStateTable.stability] = stability
                it[TutorReviewStateTable.difficulty] = difficulty
                it[TutorReviewStateTable.dueAt] = dueAt
                it[TutorReviewStateTable.reps] = reps
                it[TutorReviewStateTable.lapses] = lapses
                it[TutorReviewStateTable.lastGrade] = lastGrade
                it[TutorReviewStateTable.lastReviewedAt] = lastReviewedAt
                it[TutorReviewStateTable.updatedAt] = Instant.now()
            }
            existing[TutorReviewStateTable.id].value
        } else {
            val now = Instant.now()
            TutorReviewStateTable.insert {
                it[TutorReviewStateTable.schoolId] = schoolId
                it[TutorReviewStateTable.childId] = childId
                it[TutorReviewStateTable.topicId] = topicId
                it[TutorReviewStateTable.stability] = stability
                it[TutorReviewStateTable.difficulty] = difficulty
                it[TutorReviewStateTable.dueAt] = dueAt
                it[TutorReviewStateTable.reps] = reps
                it[TutorReviewStateTable.lapses] = lapses
                it[TutorReviewStateTable.lastGrade] = lastGrade
                it[TutorReviewStateTable.lastReviewedAt] = lastReviewedAt
                it[TutorReviewStateTable.createdAt] = now
                it[TutorReviewStateTable.updatedAt] = now
            }[TutorReviewStateTable.id].value
        }
    }

    suspend fun findByChildAndTopic(
        childId: UUID,
        topicId: UUID,
    ): ReviewStateRow? = dbQuery {
        TutorReviewStateTable.selectAll().where {
            (TutorReviewStateTable.childId eq childId) and
            (TutorReviewStateTable.topicId eq topicId)
        }.singleOrNull()?.let(::mapRow)
    }

    suspend fun findDueReviews(
        childId: UUID,
        now: Instant = Instant.now(),
        limit: Int = 20,
    ): List<ReviewStateRow> = dbQuery {
        TutorReviewStateTable.selectAll().where {
            (TutorReviewStateTable.childId eq childId) and
            (TutorReviewStateTable.dueAt lessEq now)
        }.orderBy(TutorReviewStateTable.dueAt, org.jetbrains.exposed.sql.SortOrder.ASC)
            .limit(limit)
            .map(::mapRow)
    }

    private fun mapRow(row: org.jetbrains.exposed.sql.ResultRow) = ReviewStateRow(
        id = row[TutorReviewStateTable.id].value,
        schoolId = row[TutorReviewStateTable.schoolId],
        childId = row[TutorReviewStateTable.childId],
        topicId = row[TutorReviewStateTable.topicId],
        stability = row[TutorReviewStateTable.stability],
        difficulty = row[TutorReviewStateTable.difficulty],
        dueAt = row[TutorReviewStateTable.dueAt],
        reps = row[TutorReviewStateTable.reps],
        lapses = row[TutorReviewStateTable.lapses],
        lastGrade = row[TutorReviewStateTable.lastGrade],
        lastReviewedAt = row[TutorReviewStateTable.lastReviewedAt],
        createdAt = row[TutorReviewStateTable.createdAt],
        updatedAt = row[TutorReviewStateTable.updatedAt],
    )
}
