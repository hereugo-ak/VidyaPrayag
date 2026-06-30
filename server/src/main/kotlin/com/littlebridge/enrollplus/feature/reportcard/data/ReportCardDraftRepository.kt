// FILE: server/src/main/kotlin/com/littlebridge/enrollplus/feature/reportcard/data/ReportCardDraftRepository.kt
package com.littlebridge.enrollplus.feature.reportcard.data

import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.ReportCardDraftsTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

/**
 * Repository for [ReportCardDraftsTable] — pure CRUD, no business logic.
 *
 * SOLID:
 *   S → Single responsibility: persistence only.
 *   D → All queries use parameterized forms (Exposed DSL), no string interpolation.
 */
class ReportCardDraftRepository {
    private val log = LoggerFactory.getLogger("ReportCardDraftRepository")

    data class DraftRow(
        val id: UUID,
        val schoolId: UUID,
        val studentId: UUID,
        val classId: UUID?,
        val className: String,
        val section: String,
        val term: String,
        val academicYearId: UUID?,
        val factBundle: String,
        val factHash: String,
        val aiDraft: String?,
        val classContext: String?,
        val aiProviderUsed: String?,
        val aiModelUsed: String?,
        val tokensUsed: Int,
        val templateVersion: Int,
        val language: String,
        val status: String,
        val groundingFlags: String?,
        val editedBy: UUID?,
        val editedAt: Instant?,
        val approvedBy: UUID?,
        val approvedAt: Instant?,
        val publishedBy: UUID?,
        val publishedAt: Instant?,
        val createdAt: Instant,
        val updatedAt: Instant,
    )

    private fun mapRow(row: org.jetbrains.exposed.sql.ResultRow) = DraftRow(
        id = row[ReportCardDraftsTable.id].value,
        schoolId = row[ReportCardDraftsTable.schoolId],
        studentId = row[ReportCardDraftsTable.studentId],
        classId = row[ReportCardDraftsTable.classId],
        className = row[ReportCardDraftsTable.className],
        section = row[ReportCardDraftsTable.section],
        term = row[ReportCardDraftsTable.term],
        academicYearId = row[ReportCardDraftsTable.academicYearId],
        factBundle = row[ReportCardDraftsTable.factBundle],
        factHash = row[ReportCardDraftsTable.factHash],
        aiDraft = row[ReportCardDraftsTable.aiDraft],
        classContext = row[ReportCardDraftsTable.classContext],
        aiProviderUsed = row[ReportCardDraftsTable.aiProviderUsed],
        aiModelUsed = row[ReportCardDraftsTable.aiModelUsed],
        tokensUsed = row[ReportCardDraftsTable.tokensUsed],
        templateVersion = row[ReportCardDraftsTable.templateVersion],
        language = row[ReportCardDraftsTable.language],
        status = row[ReportCardDraftsTable.status],
        groundingFlags = row[ReportCardDraftsTable.groundingFlags],
        editedBy = row[ReportCardDraftsTable.editedBy],
        editedAt = row[ReportCardDraftsTable.editedAt],
        approvedBy = row[ReportCardDraftsTable.approvedBy],
        approvedAt = row[ReportCardDraftsTable.approvedAt],
        publishedBy = row[ReportCardDraftsTable.publishedBy],
        publishedAt = row[ReportCardDraftsTable.publishedAt],
        createdAt = row[ReportCardDraftsTable.createdAt],
        updatedAt = row[ReportCardDraftsTable.updatedAt],
    )

    suspend fun insert(
        schoolId: UUID,
        studentId: UUID,
        classId: UUID?,
        className: String,
        section: String,
        term: String,
        academicYearId: UUID?,
        factBundle: String,
        factHash: String,
        language: String = "en",
        templateVersion: Int = 1,
    ): UUID = dbQuery {
        val now = Instant.now()
        ReportCardDraftsTable.insert {
            it[ReportCardDraftsTable.schoolId] = schoolId
            it[ReportCardDraftsTable.studentId] = studentId
            it[ReportCardDraftsTable.classId] = classId
            it[ReportCardDraftsTable.className] = className
            it[ReportCardDraftsTable.section] = section
            it[ReportCardDraftsTable.term] = term
            it[ReportCardDraftsTable.academicYearId] = academicYearId
            it[ReportCardDraftsTable.factBundle] = factBundle
            it[ReportCardDraftsTable.factHash] = factHash
            it[ReportCardDraftsTable.language] = language
            it[ReportCardDraftsTable.templateVersion] = templateVersion
            it[ReportCardDraftsTable.status] = "draft"
            it[ReportCardDraftsTable.createdAt] = now
            it[ReportCardDraftsTable.updatedAt] = now
        }[ReportCardDraftsTable.id].value
    }

    suspend fun findById(id: UUID): DraftRow? = dbQuery {
        ReportCardDraftsTable.selectAll().where { ReportCardDraftsTable.id eq id }
            .singleOrNull()?.let(::mapRow)
    }

    suspend fun findByStudentAndTerm(
        schoolId: UUID,
        studentId: UUID,
        term: String,
        academicYearId: UUID?,
    ): DraftRow? = dbQuery {
        ReportCardDraftsTable.selectAll().where {
            (ReportCardDraftsTable.schoolId eq schoolId) and
            (ReportCardDraftsTable.studentId eq studentId) and
            (ReportCardDraftsTable.term eq term) and
            (if (academicYearId != null)
                ReportCardDraftsTable.academicYearId eq academicYearId
            else
                Op.TRUE)
        }.singleOrNull()?.let(::mapRow)
    }

    suspend fun findByClassAndTerm(
        schoolId: UUID,
        classId: UUID?,
        term: String,
        academicYearId: UUID?,
    ): List<DraftRow> = dbQuery {
        ReportCardDraftsTable.selectAll().where {
            (ReportCardDraftsTable.schoolId eq schoolId) and
            (ReportCardDraftsTable.term eq term) and
            (if (classId != null)
                ReportCardDraftsTable.classId eq classId
            else
                Op.TRUE) and
            (if (academicYearId != null)
                ReportCardDraftsTable.academicYearId eq academicYearId
            else
                Op.TRUE)
        }.map(::mapRow)
    }

    suspend fun findByStatus(
        schoolId: UUID,
        status: String,
        classId: UUID? = null,
    ): List<DraftRow> = dbQuery {
        ReportCardDraftsTable.selectAll().where {
            (ReportCardDraftsTable.schoolId eq schoolId) and
            (ReportCardDraftsTable.status eq status) and
            (if (classId != null)
                ReportCardDraftsTable.classId eq classId
            else
                org.jetbrains.exposed.sql.Op.TRUE)
        }.map(::mapRow)
    }

    suspend fun updateAiDraft(
        id: UUID,
        aiDraft: String,
        classContext: String?,
        providerUsed: String?,
        modelUsed: String?,
        tokensUsed: Int,
        groundingFlags: String?,
        status: String,
    ): Boolean = dbQuery {
        val updated = ReportCardDraftsTable.update({
            ReportCardDraftsTable.id eq id
        }) {
            it[ReportCardDraftsTable.aiDraft] = aiDraft
            it[ReportCardDraftsTable.classContext] = classContext
            it[ReportCardDraftsTable.aiProviderUsed] = providerUsed
            it[ReportCardDraftsTable.aiModelUsed] = modelUsed
            it[ReportCardDraftsTable.tokensUsed] = tokensUsed
            it[ReportCardDraftsTable.groundingFlags] = groundingFlags
            it[ReportCardDraftsTable.status] = status
            it[ReportCardDraftsTable.updatedAt] = Instant.now()
        }
        updated > 0
    }

    suspend fun updateEditedDraft(
        id: UUID,
        aiDraft: String,
        editedBy: UUID,
    ): Boolean = dbQuery {
        val now = Instant.now()
        val updated = ReportCardDraftsTable.update({
            ReportCardDraftsTable.id eq id
        }) {
            it[ReportCardDraftsTable.aiDraft] = aiDraft
            it[ReportCardDraftsTable.editedBy] = editedBy
            it[ReportCardDraftsTable.editedAt] = now
            it[ReportCardDraftsTable.updatedAt] = now
        }
        updated > 0
    }

    suspend fun approve(id: UUID, approvedBy: UUID): Boolean = dbQuery {
        val now = Instant.now()
        val updated = ReportCardDraftsTable.update({
            (ReportCardDraftsTable.id eq id) and
            (ReportCardDraftsTable.status inList listOf("draft", "flagged_for_review"))
        }) {
            it[ReportCardDraftsTable.status] = "approved"
            it[ReportCardDraftsTable.approvedBy] = approvedBy
            it[ReportCardDraftsTable.approvedAt] = now
            it[ReportCardDraftsTable.updatedAt] = now
        }
        updated > 0
    }

    suspend fun bulkApprove(ids: List<UUID>, approvedBy: UUID): Int = dbQuery {
        val now = Instant.now()
        var count = 0
        for (id in ids) {
            val updated = ReportCardDraftsTable.update({
                (ReportCardDraftsTable.id eq id) and
                (ReportCardDraftsTable.status inList listOf("draft", "flagged_for_review"))
            }) {
                it[ReportCardDraftsTable.status] = "approved"
                it[ReportCardDraftsTable.approvedBy] = approvedBy
                it[ReportCardDraftsTable.approvedAt] = now
                it[ReportCardDraftsTable.updatedAt] = now
            }
            if (updated > 0) count++
        }
        count
    }

    suspend fun publish(id: UUID, publishedBy: UUID): Boolean = dbQuery {
        val now = Instant.now()
        val updated = ReportCardDraftsTable.update({
            (ReportCardDraftsTable.id eq id) and
            (ReportCardDraftsTable.status eq "approved")
        }) {
            it[ReportCardDraftsTable.status] = "published"
            it[ReportCardDraftsTable.publishedBy] = publishedBy
            it[ReportCardDraftsTable.publishedAt] = now
            it[ReportCardDraftsTable.updatedAt] = now
        }
        updated > 0
    }

    suspend fun publishByClass(
        schoolId: UUID,
        classId: UUID?,
        term: String,
        academicYearId: UUID?,
        publishedBy: UUID,
        className: String? = null,
        section: String? = null,
    ): Int = dbQuery {
        val now = Instant.now()
        var count = 0
        val rows = ReportCardDraftsTable.selectAll().where {
            (ReportCardDraftsTable.schoolId eq schoolId) and
            (ReportCardDraftsTable.status eq "approved") and
            (ReportCardDraftsTable.term eq term) and
            (if (classId != null)
                ReportCardDraftsTable.classId eq classId
            else
                Op.TRUE) and
            (if (academicYearId != null)
                ReportCardDraftsTable.academicYearId eq academicYearId
            else
                Op.TRUE) and
            (if (className != null)
                ReportCardDraftsTable.className eq className
            else
                Op.TRUE) and
            (if (section != null)
                ReportCardDraftsTable.section eq section
            else
                Op.TRUE)
        }.map { it[ReportCardDraftsTable.id].value }

        for (rowId in rows) {
            val updated = ReportCardDraftsTable.update({
                ReportCardDraftsTable.id eq rowId
            }) {
                it[ReportCardDraftsTable.status] = "published"
                it[ReportCardDraftsTable.publishedBy] = publishedBy
                it[ReportCardDraftsTable.publishedAt] = now
                it[ReportCardDraftsTable.updatedAt] = now
            }
            if (updated > 0) count++
        }
        count
    }

    suspend fun findPublishedForStudent(
        schoolId: UUID,
        studentId: UUID,
        academicYearId: UUID?,
    ): List<DraftRow> = dbQuery {
        ReportCardDraftsTable.selectAll().where {
            (ReportCardDraftsTable.schoolId eq schoolId) and
            (ReportCardDraftsTable.studentId eq studentId) and
            (ReportCardDraftsTable.status eq "published") and
            (if (academicYearId != null)
                ReportCardDraftsTable.academicYearId eq academicYearId
            else
                org.jetbrains.exposed.sql.Op.TRUE)
        }.orderBy(ReportCardDraftsTable.term).map(::mapRow)
    }

    suspend fun findLastTermReport(
        schoolId: UUID,
        studentId: UUID,
        beforeTerm: String,
    ): DraftRow? = dbQuery {
        ReportCardDraftsTable.selectAll().where {
            (ReportCardDraftsTable.schoolId eq schoolId) and
            (ReportCardDraftsTable.studentId eq studentId) and
            (ReportCardDraftsTable.status eq "published") and
            (ReportCardDraftsTable.term lessEq beforeTerm)
        }.orderBy(ReportCardDraftsTable.createdAt, org.jetbrains.exposed.sql.SortOrder.DESC)
            .firstOrNull()?.let(::mapRow)
    }
}
