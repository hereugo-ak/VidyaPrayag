// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/InterventionRepositoryImpl.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsInterventionsTable
import com.littlebridge.enrollplus.feature.pews.domain.InterventionEntity
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class InterventionRepositoryImpl : InterventionRepository {

    override suspend fun create(intervention: InterventionEntity): UUID = dbQuery {
        val now = Instant.now()
        PewsInterventionsTable.insert {
            it[schoolId] = intervention.schoolId
            it[studentCode] = intervention.studentCode
            it[PewsInterventionsTable.snapshotId] = intervention.snapshotId
            it[ownerUserId] = intervention.ownerUserId
            it[actionType] = intervention.actionType
            it[status] = intervention.status
            it[PewsInterventionsTable.notes] = intervention.notes
            it[openedAt] = intervention.openedAt
            it[createdAt] = now
            it[planJson] = intervention.planJson
            it[PewsInterventionsTable.slaDays] = intervention.slaDays
            it[PewsInterventionsTable.escalationLevel] = intervention.escalationLevel
            it[PewsInterventionsTable.followUpDate] = intervention.followUpDate
            it[PewsInterventionsTable.caseFileId] = intervention.caseFileId
            it[PewsInterventionsTable.urgency] = intervention.urgency
            it[PewsInterventionsTable.causeFamily] = intervention.causeFamily
        } get PewsInterventionsTable.id
    }.value

    override suspend fun getById(schoolId: UUID, interventionId: UUID): InterventionEntity? = dbQuery {
        PewsInterventionsTable.selectAll().where {
            (PewsInterventionsTable.id eq interventionId) and
                (PewsInterventionsTable.schoolId eq schoolId)
        }.singleOrNull()?.toEntity()
    }

    override suspend fun list(
        schoolId: UUID,
        ownerUserId: UUID?,
        status: String?,
    ): List<InterventionEntity> = dbQuery {
        PewsInterventionsTable.selectAll().where {
            var cond = PewsInterventionsTable.schoolId eq schoolId
            ownerUserId?.let { cond = cond and (PewsInterventionsTable.ownerUserId eq it) }
            status?.let { cond = cond and (PewsInterventionsTable.status eq it) }
            cond
        }.orderBy(PewsInterventionsTable.openedAt, SortOrder.DESC).map { it.toEntity() }
    }

    override suspend fun hasOpenIntervention(schoolId: UUID, studentCode: String): Boolean = dbQuery {
        PewsInterventionsTable.selectAll().where {
            (PewsInterventionsTable.schoolId eq schoolId) and
                (PewsInterventionsTable.studentCode eq studentCode) and
                (PewsInterventionsTable.status inList listOf("open", "in_progress"))
        }.limit(1).any()
    }

    override suspend fun update(
        schoolId: UUID,
        interventionId: UUID,
        status: String?,
        notes: String?,
        outcome: String?,
        actionType: String?,
        planJson: String?,
        slaDays: Int?,
        escalationLevel: Int?,
        followUpDate: LocalDate?,
        ownerUserId: UUID?,
    ): Boolean = dbQuery {
        val resolving = status == "done" || status == "dismissed"
        val now = Instant.now()
        val rows = PewsInterventionsTable.update({
            (PewsInterventionsTable.id eq interventionId) and
                (PewsInterventionsTable.schoolId eq schoolId)
        }) {
            if (status != null) it[PewsInterventionsTable.status] = status
            if (notes != null) it[PewsInterventionsTable.notes] = notes
            if (actionType != null) it[PewsInterventionsTable.actionType] = actionType
            if (outcome != null) it[PewsInterventionsTable.outcome] = outcome
            if (planJson != null) it[PewsInterventionsTable.planJson] = planJson
            if (slaDays != null) it[PewsInterventionsTable.slaDays] = slaDays
            if (escalationLevel != null) it[PewsInterventionsTable.escalationLevel] = escalationLevel
            if (followUpDate != null) it[PewsInterventionsTable.followUpDate] = followUpDate
            if (ownerUserId != null) it[PewsInterventionsTable.ownerUserId] = ownerUserId
            if (resolving) it[PewsInterventionsTable.resolvedAt] = now
        }
        rows > 0
    }

    override suspend fun findPastSla(schoolId: UUID, asOf: LocalDate): List<InterventionEntity> = dbQuery {
        PewsInterventionsTable.selectAll().where {
            (PewsInterventionsTable.schoolId eq schoolId) and
                (PewsInterventionsTable.status inList listOf("open", "in_progress")) and
                (PewsInterventionsTable.followUpDate lessEq asOf) and
                (PewsInterventionsTable.escalationLevel less 2)
        }.map { it.toEntity() }
    }

    override suspend fun effectivenessCounts(schoolId: UUID): EffectivenessCounts = dbQuery {
        val rows = PewsInterventionsTable.selectAll().where {
            PewsInterventionsTable.schoolId eq schoolId
        }.toList()
        EffectivenessCounts(
            total = rows.size,
            open = rows.count { it[PewsInterventionsTable.status] in listOf("open", "in_progress") },
            done = rows.count { it[PewsInterventionsTable.status] == "done" },
            dismissed = rows.count { it[PewsInterventionsTable.status] == "dismissed" },
            improved = rows.count { it[PewsInterventionsTable.outcome] == "improved" },
            unchanged = rows.count { it[PewsInterventionsTable.outcome] == "unchanged" },
            worsened = rows.count { it[PewsInterventionsTable.outcome] == "worsened" },
        )
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toEntity(): InterventionEntity =
        InterventionEntity(
            id = this[PewsInterventionsTable.id].value,
            schoolId = this[PewsInterventionsTable.schoolId],
            studentCode = this[PewsInterventionsTable.studentCode],
            snapshotId = this[PewsInterventionsTable.snapshotId],
            ownerUserId = this[PewsInterventionsTable.ownerUserId],
            actionType = this[PewsInterventionsTable.actionType],
            status = this[PewsInterventionsTable.status],
            notes = this[PewsInterventionsTable.notes],
            openedAt = this[PewsInterventionsTable.openedAt],
            resolvedAt = this[PewsInterventionsTable.resolvedAt],
            outcome = this[PewsInterventionsTable.outcome],
            createdAt = this[PewsInterventionsTable.createdAt],
            planJson = this[PewsInterventionsTable.planJson],
            slaDays = this[PewsInterventionsTable.slaDays],
            escalationLevel = this[PewsInterventionsTable.escalationLevel],
            followUpDate = this[PewsInterventionsTable.followUpDate],
            caseFileId = this[PewsInterventionsTable.caseFileId],
            urgency = this[PewsInterventionsTable.urgency],
            causeFamily = this[PewsInterventionsTable.causeFamily],
        )
}
