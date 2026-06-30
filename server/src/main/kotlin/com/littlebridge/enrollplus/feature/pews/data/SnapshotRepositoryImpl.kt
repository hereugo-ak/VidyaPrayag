// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/SnapshotRepositoryImpl.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsRiskSnapshotsTable
import com.littlebridge.enrollplus.db.SchoolsTable
import com.littlebridge.enrollplus.feature.pews.domain.SnapshotEntity
import com.littlebridge.enrollplus.feature.pews.domain.SignalDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class SnapshotRepositoryImpl : SnapshotRepository {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val signalSerializer = ListSerializer(SignalDto.serializer())

    override suspend fun upsert(snapshot: SnapshotEntity): UUID = dbQuery {
        val signalsJson = json.encodeToString(signalSerializer, snapshot.signals)
        val now = Instant.now()

        // Delete existing row for (school, student, runDate) then insert — idempotent upsert.
        PewsRiskSnapshotsTable.deleteWhere {
            (PewsRiskSnapshotsTable.schoolId eq snapshot.schoolId) and
                (PewsRiskSnapshotsTable.studentCode eq snapshot.studentCode) and
                (PewsRiskSnapshotsTable.runDate eq snapshot.runDate)
        }

        val id = PewsRiskSnapshotsTable.insert {
            it[schoolId] = snapshot.schoolId
            it[studentCode] = snapshot.studentCode
            it[runDate] = snapshot.runDate
            it[riskScore] = snapshot.riskScore
            it[riskLevel] = snapshot.riskLevel
            it[attendancePct] = snapshot.attendancePct
            it[marksPct] = snapshot.marksPct
            it[leaveCount] = snapshot.leaveCount
            it[attendanceSlope] = snapshot.attendanceSlope
            it[marksSlope] = snapshot.marksSlope
            it[PewsRiskSnapshotsTable.signalsJson] = signalsJson
            it[signalHash] = snapshot.signalHash
            it[PewsRiskSnapshotsTable.createdAt] = now
            it[confidence] = snapshot.confidence
            it[leadingScore] = snapshot.leadingScore
            it[causeFamily] = snapshot.causeFamily
            it[deltasJson] = snapshot.deltasJson
        } get PewsRiskSnapshotsTable.id

        id.value
    }

    override suspend fun latestRunDate(schoolId: UUID): LocalDate? = dbQuery {
        PewsRiskSnapshotsTable.selectAll()
            .where { PewsRiskSnapshotsTable.schoolId eq schoolId }
            .orderBy(PewsRiskSnapshotsTable.runDate, SortOrder.DESC)
            .limit(1)
            .firstOrNull()
            ?.get(PewsRiskSnapshotsTable.runDate)
    }

    override suspend fun cohort(schoolId: UUID, runDate: LocalDate?): List<SnapshotEntity> = dbQuery {
        val effectiveDate = runDate ?: latestRunDate(schoolId) ?: return@dbQuery emptyList()
        PewsRiskSnapshotsTable.selectAll().where {
            (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                (PewsRiskSnapshotsTable.runDate eq effectiveDate)
        }.map { it.toEntity() }
    }

    override suspend fun studentSnapshot(schoolId: UUID, studentCode: String, runDate: LocalDate?): SnapshotEntity? = dbQuery {
        if (runDate != null) {
            PewsRiskSnapshotsTable.selectAll().where {
                (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                    (PewsRiskSnapshotsTable.studentCode eq studentCode) and
                    (PewsRiskSnapshotsTable.runDate eq runDate)
            }.singleOrNull()?.toEntity()
        } else {
            PewsRiskSnapshotsTable.selectAll().where {
                (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                    (PewsRiskSnapshotsTable.studentCode eq studentCode)
            }.orderBy(PewsRiskSnapshotsTable.runDate, SortOrder.DESC).firstOrNull()?.toEntity()
        }
    }

    override suspend fun studentHistory(schoolId: UUID, studentCode: String, limit: Int): List<SnapshotEntity> = dbQuery {
        PewsRiskSnapshotsTable.selectAll().where {
            (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                (PewsRiskSnapshotsTable.studentCode eq studentCode)
        }.orderBy(PewsRiskSnapshotsTable.runDate, SortOrder.DESC).limit(limit).map { it.toEntity() }
    }

    override suspend fun previousSnapshot(schoolId: UUID, studentCode: String, beforeDate: LocalDate): SnapshotEntity? = dbQuery {
        PewsRiskSnapshotsTable.selectAll().where {
            (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                (PewsRiskSnapshotsTable.studentCode eq studentCode) and
                (PewsRiskSnapshotsTable.runDate lessEq beforeDate)
        }.orderBy(PewsRiskSnapshotsTable.runDate, SortOrder.DESC).limit(1).firstOrNull()?.toEntity()
    }

    override suspend fun updateAiFields(
        schoolId: UUID, studentCode: String, runDate: LocalDate,
        narrative: String?, cause: String?, recommendation: String?, providerUsed: String?,
    ): Boolean = dbQuery {
        val rows = PewsRiskSnapshotsTable.update({
            (PewsRiskSnapshotsTable.schoolId eq schoolId) and
                (PewsRiskSnapshotsTable.studentCode eq studentCode) and
                (PewsRiskSnapshotsTable.runDate eq runDate)
        }) {
            if (narrative != null) it[aiNarrative] = narrative
            if (cause != null) it[aiCause] = cause
            if (recommendation != null) it[aiRecommendation] = recommendation
            if (providerUsed != null) it[aiProviderUsed] = providerUsed
        }
        rows > 0
    }

    override suspend fun activeSchoolIds(): List<UUID> = dbQuery {
        SchoolsTable.selectAll().map { it[SchoolsTable.id].value }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toEntity(): SnapshotEntity {
        val signals: List<SignalDto> = try {
            json.decodeFromString(signalSerializer, this[PewsRiskSnapshotsTable.signalsJson])
        } catch (_: Exception) { emptyList() }

        return SnapshotEntity(
            id = this[PewsRiskSnapshotsTable.id].value,
            schoolId = this[PewsRiskSnapshotsTable.schoolId],
            studentCode = this[PewsRiskSnapshotsTable.studentCode],
            runDate = this[PewsRiskSnapshotsTable.runDate],
            riskScore = this[PewsRiskSnapshotsTable.riskScore],
            riskLevel = this[PewsRiskSnapshotsTable.riskLevel],
            attendancePct = this[PewsRiskSnapshotsTable.attendancePct],
            marksPct = this[PewsRiskSnapshotsTable.marksPct],
            leaveCount = this[PewsRiskSnapshotsTable.leaveCount],
            attendanceSlope = this[PewsRiskSnapshotsTable.attendanceSlope],
            marksSlope = this[PewsRiskSnapshotsTable.marksSlope],
            signals = signals,
            signalHash = this[PewsRiskSnapshotsTable.signalHash],
            aiNarrative = this[PewsRiskSnapshotsTable.aiNarrative],
            aiCause = this[PewsRiskSnapshotsTable.aiCause],
            aiRecommendation = this[PewsRiskSnapshotsTable.aiRecommendation],
            aiProviderUsed = this[PewsRiskSnapshotsTable.aiProviderUsed],
            createdAt = this[PewsRiskSnapshotsTable.createdAt],
            confidence = this[PewsRiskSnapshotsTable.confidence],
            leadingScore = this[PewsRiskSnapshotsTable.leadingScore],
            causeFamily = this[PewsRiskSnapshotsTable.causeFamily],
            deltasJson = this[PewsRiskSnapshotsTable.deltasJson],
        )
    }
}
