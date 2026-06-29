// FILE: src/main/kotlin/com/littlebridge/enrollplus/feature/pews/data/CaseFileRepositoryImpl.kt
package com.littlebridge.enrollplus.feature.pews.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.PewsCaseFilesTable
import com.littlebridge.enrollplus.feature.pews.domain.CaseFileEntity
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.time.Instant
import java.util.UUID

class CaseFileRepositoryImpl : CaseFileRepository {

    override suspend fun create(caseFile: CaseFileEntity): UUID = dbQuery {
        val now = Instant.now()
        PewsCaseFilesTable.insert {
            it[schoolId] = caseFile.schoolId
            it[PewsCaseFilesTable.snapshotId] = caseFile.snapshotId
            it[studentCode] = caseFile.studentCode
            it[caseFileJson] = caseFile.caseFileJson
            it[narrative] = caseFile.narrative
            it[urgency] = caseFile.urgency
            it[skipReason] = caseFile.skipReason
            it[parentDraftJson] = caseFile.parentDraftJson
            it[parentDraftLang] = caseFile.parentDraftLang
            it[providerUsed] = caseFile.providerUsed
            it[modelUsed] = caseFile.modelUsed
            it[groundingPassed] = caseFile.groundingPassed
            it[PewsCaseFilesTable.createdAt] = now
        } get PewsCaseFilesTable.id
    }.value

    override suspend fun getById(schoolId: UUID, caseFileId: UUID): CaseFileEntity? = dbQuery {
        PewsCaseFilesTable.selectAll().where {
            (PewsCaseFilesTable.id eq caseFileId) and
                (PewsCaseFilesTable.schoolId eq schoolId)
        }.singleOrNull()?.toEntity()
    }

    override suspend fun latestForStudent(schoolId: UUID, studentCode: String): CaseFileEntity? = dbQuery {
        PewsCaseFilesTable.selectAll().where {
            (PewsCaseFilesTable.schoolId eq schoolId) and
                (PewsCaseFilesTable.studentCode eq studentCode)
        }.orderBy(PewsCaseFilesTable.createdAt, SortOrder.DESC).firstOrNull()?.toEntity()
    }

    override suspend fun historyForStudent(schoolId: UUID, studentCode: String, limit: Int): List<CaseFileEntity> = dbQuery {
        PewsCaseFilesTable.selectAll().where {
            (PewsCaseFilesTable.schoolId eq schoolId) and
                (PewsCaseFilesTable.studentCode eq studentCode)
        }.orderBy(PewsCaseFilesTable.createdAt, SortOrder.DESC).limit(limit).map { it.toEntity() }
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toEntity(): CaseFileEntity =
        CaseFileEntity(
            id = this[PewsCaseFilesTable.id].value,
            schoolId = this[PewsCaseFilesTable.schoolId],
            snapshotId = this[PewsCaseFilesTable.snapshotId],
            studentCode = this[PewsCaseFilesTable.studentCode],
            caseFileJson = this[PewsCaseFilesTable.caseFileJson],
            narrative = this[PewsCaseFilesTable.narrative],
            urgency = this[PewsCaseFilesTable.urgency],
            skipReason = this[PewsCaseFilesTable.skipReason],
            parentDraftJson = this[PewsCaseFilesTable.parentDraftJson],
            parentDraftLang = this[PewsCaseFilesTable.parentDraftLang],
            providerUsed = this[PewsCaseFilesTable.providerUsed],
            modelUsed = this[PewsCaseFilesTable.modelUsed],
            groundingPassed = this[PewsCaseFilesTable.groundingPassed],
            createdAt = this[PewsCaseFilesTable.createdAt],
        )
}
