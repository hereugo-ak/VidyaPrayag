// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/EffectivenessPriorsRepositoryImpl.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsEffectivenessPriorsTable
import com.littlebridge.enrollplus.feature.pews.domain.EffectivenessPriorEntity
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class EffectivenessPriorsRepositoryImpl : EffectivenessPriorsRepository {

    override suspend fun get(schoolId: UUID, causeFamily: String, actionType: String): EffectivenessPriorEntity? = dbQuery {
        PewsEffectivenessPriorsTable.selectAll().where {
            (PewsEffectivenessPriorsTable.schoolId eq schoolId) and
                (PewsEffectivenessPriorsTable.causeFamily eq causeFamily) and
                (PewsEffectivenessPriorsTable.actionType eq actionType)
        }.singleOrNull()?.toEntity()
    }

    override suspend fun listForSchool(schoolId: UUID): List<EffectivenessPriorEntity> = dbQuery {
        PewsEffectivenessPriorsTable.selectAll().where {
            PewsEffectivenessPriorsTable.schoolId eq schoolId
        }.orderBy(PewsEffectivenessPriorsTable.improveRate, SortOrder.DESC).map { it.toEntity() }
    }

    override suspend fun listForCauseFamily(schoolId: UUID, causeFamily: String): List<EffectivenessPriorEntity> = dbQuery {
        PewsEffectivenessPriorsTable.selectAll().where {
            (PewsEffectivenessPriorsTable.schoolId eq schoolId) and
                (PewsEffectivenessPriorsTable.causeFamily eq causeFamily)
        }.orderBy(PewsEffectivenessPriorsTable.improveRate, SortOrder.DESC).map { it.toEntity() }
    }

    override suspend fun recordOutcome(
        schoolId: UUID,
        causeFamily: String,
        actionType: String,
        improved: Boolean,
        daysToImprove: Double?,
    ): Boolean = dbQuery {
        val existing = PewsEffectivenessPriorsTable.selectAll().where {
            (PewsEffectivenessPriorsTable.schoolId eq schoolId) and
                (PewsEffectivenessPriorsTable.causeFamily eq causeFamily) and
                (PewsEffectivenessPriorsTable.actionType eq actionType)
        }.singleOrNull()

        val now = Instant.now()

        if (existing == null) {
            // Insert new prior
            val nTried = 1
            val nImproved = if (improved) 1 else 0
            PewsEffectivenessPriorsTable.insert {
                it[PewsEffectivenessPriorsTable.schoolId] = schoolId
                it[PewsEffectivenessPriorsTable.causeFamily] = causeFamily
                it[PewsEffectivenessPriorsTable.actionType] = actionType
                it[PewsEffectivenessPriorsTable.nTried] = nTried
                it[PewsEffectivenessPriorsTable.nImproved] = nImproved
                it[PewsEffectivenessPriorsTable.improveRate] = nImproved.toDouble() / nTried
                it[PewsEffectivenessPriorsTable.avgDaysToImprove] = if (improved && daysToImprove != null) daysToImprove else 0.0
                it[PewsEffectivenessPriorsTable.updatedAt] = now
            }
            true
        } else {
            // Update existing prior
            val oldTried = existing[PewsEffectivenessPriorsTable.nTried]
            val oldImproved = existing[PewsEffectivenessPriorsTable.nImproved]
            val oldAvgDays = existing[PewsEffectivenessPriorsTable.avgDaysToImprove]

            val newTried = oldTried + 1
            val newImproved = oldImproved + if (improved) 1 else 0
            val newRate = newImproved.toDouble() / newTried

            // Running average of days-to-improve (only for improved cases)
            val newAvgDays = if (improved && daysToImprove != null) {
                if (oldImproved > 0) {
                    ((oldAvgDays * oldImproved) + daysToImprove) / newImproved
                } else {
                    daysToImprove
                }
            } else {
                oldAvgDays
            }

            PewsEffectivenessPriorsTable.update({
                (PewsEffectivenessPriorsTable.schoolId eq schoolId) and
                    (PewsEffectivenessPriorsTable.causeFamily eq causeFamily) and
                    (PewsEffectivenessPriorsTable.actionType eq actionType)
            }) {
                it[PewsEffectivenessPriorsTable.nTried] = newTried
                it[PewsEffectivenessPriorsTable.nImproved] = newImproved
                it[PewsEffectivenessPriorsTable.improveRate] = newRate
                it[PewsEffectivenessPriorsTable.avgDaysToImprove] = newAvgDays
                it[PewsEffectivenessPriorsTable.updatedAt] = now
            }
            true
        }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toEntity(): EffectivenessPriorEntity =
        EffectivenessPriorEntity(
            id = this[PewsEffectivenessPriorsTable.id].value,
            schoolId = this[PewsEffectivenessPriorsTable.schoolId],
            causeFamily = this[PewsEffectivenessPriorsTable.causeFamily],
            actionType = this[PewsEffectivenessPriorsTable.actionType],
            nTried = this[PewsEffectivenessPriorsTable.nTried],
            nImproved = this[PewsEffectivenessPriorsTable.nImproved],
            improveRate = this[PewsEffectivenessPriorsTable.improveRate],
            avgDaysToImprove = this[PewsEffectivenessPriorsTable.avgDaysToImprove],
            updatedAt = this[PewsEffectivenessPriorsTable.updatedAt],
        )
}
