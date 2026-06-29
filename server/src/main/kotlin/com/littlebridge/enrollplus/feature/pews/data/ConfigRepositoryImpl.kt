// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/ConfigRepositoryImpl.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsConfigTable
import com.littlebridge.enrollplus.feature.pews.domain.PewsConfigEntity
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.util.UUID

class ConfigRepositoryImpl : ConfigRepository {

    override suspend fun read(schoolId: UUID): PewsConfigEntity = dbQuery {
        // PewsConfigTable uses id = schoolId (one row per school).
        PewsConfigTable.selectAll().where { PewsConfigTable.id eq schoolId }
            .firstOrNull()
            ?.toEntity()
            ?: PewsConfigEntity() // sane defaults
    }

    override suspend fun write(schoolId: UUID, config: PewsConfigEntity): Boolean = dbQuery {
        val now = Instant.now()
        // Delete + insert = idempotent upsert (id = schoolId).
        PewsConfigTable.deleteWhere { PewsConfigTable.id eq schoolId }
        PewsConfigTable.insert {
            it[PewsConfigTable.id] = schoolId
            it[useRelativeThresholds] = config.useRelativeThresholds
            it[attendanceFloorPct] = config.attendanceFloorPct
            it[marksFloorPct] = config.marksFloorPct
            it[leaveFloorCount] = config.leaveFloorCount
            it[runFrequency] = config.runFrequency
            it[aiNarrativeEnabled] = config.aiNarrativeEnabled
            it[parentShareEnabled] = config.parentShareEnabled
            it[updatedAt] = now
        }
        true
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toEntity(): PewsConfigEntity =
        PewsConfigEntity(
            useRelativeThresholds = this[PewsConfigTable.useRelativeThresholds],
            attendanceFloorPct = this[PewsConfigTable.attendanceFloorPct],
            marksFloorPct = this[PewsConfigTable.marksFloorPct],
            leaveFloorCount = this[PewsConfigTable.leaveFloorCount],
            runFrequency = this[PewsConfigTable.runFrequency],
            aiNarrativeEnabled = this[PewsConfigTable.aiNarrativeEnabled],
            parentShareEnabled = this[PewsConfigTable.parentShareEnabled],
        )
}
