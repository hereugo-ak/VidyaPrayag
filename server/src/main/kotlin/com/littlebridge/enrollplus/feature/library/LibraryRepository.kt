/*
 * File: LibraryRepository.kt
 * Module: feature.library
 *
 * Persistence layer for all 13 library tables. The ONLY place that reads
 * or writes library rows. Services talk to the DB through this repository
 * so all invariants (school scoping, soft-delete, copy count sync) stay
 * in one spot.
 *
 * Pattern follows OtpGatewayDeviceRepository / SmsRequestRepository:
 *   - dbQuery { } for suspended transactions
 *   - ResultRow.toXxx() extension for projection mapping
 *   - data class row projections for service consumption
 */
package com.littlebridge.enrollplus.feature.library

import com.littlebridge.enrollplus.db.*
import com.littlebridge.enrollplus.db.DatabaseFactory.dbQuery
import com.littlebridge.enrollplus.db.DatabaseFactory.readQuery
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greaterEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.SqlExpressionBuilder.isNotNull
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.TransactionManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ── Row projections ──────────────────────────────────────────────────────────

data class LibraryBookRow(
    val id: UUID,
    val schoolId: UUID,
    val isbn: String?,
    val title: String,
    val author: String?,
    val publisher: String?,
    val category: String?,
    val tags: String?,
    val totalCopies: Int,
    val availableCopies: Int,
    val shelfLocation: String?,
    val coverUrl: String?,
    val replacementCost: Double?,
    val seriesName: String?,
    val seriesNumber: Int?,
    val language: String,
    val isArchived: Boolean,
    val synopsis: String?,
    val pageCount: Int?,
    val deletedAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LibraryCopyRow(
    val id: UUID,
    val schoolId: UUID,
    val bookId: UUID,
    val copyNumber: Int,
    val barcode: String?,
    val condition: String,
    val status: String,
)

data class LibraryIssueRow(
    val id: UUID,
    val schoolId: UUID,
    val bookId: UUID,
    val copyId: UUID?,
    val borrowerId: UUID,
    val borrowerType: String,
    val borrowerName: String,
    val issueDate: LocalDate,
    val dueDate: LocalDate,
    val returnDate: LocalDate?,
    val returnCondition: String?,
    val damageNotes: String?,
    val renewalCount: Int,
    val fineAmount: Double,
    val fineStatus: String,
    val finePaidAt: Instant?,
    val fineWaivedBy: UUID?,
    val fineWaivedReason: String?,
    val status: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LibraryReservationRow(
    val id: UUID,
    val schoolId: UUID,
    val bookId: UUID,
    val reservedBy: UUID,
    val reservedByName: String,
    val reservedByType: String,
    val status: String,
    val createdAt: Instant,
    val fulfilledAt: Instant?,
)

data class LibraryCategoryRow(
    val id: UUID,
    val schoolId: UUID,
    val name: String,
    val color: String,
    val icon: String,
    val displayOrder: Int,
)

data class LibrarySettingsRow(
    val id: UUID,
    val schoolId: UUID,
    val defaultLoanDays: Int,
    val finePerDay: Double,
    val maxBooksPerStudent: Int,
    val maxRenewals: Int,
    val reservationTimeoutDays: Int,
    val dueReminderDays: Int,
    val fineCapEnabled: Boolean,
    val quickIssueEnabled: Boolean,
    val bulkReturnEnabled: Boolean,
    val featuredBookId: UUID?,
    val featuredType: String?,
    val featuredUpdatedAt: Instant?,
    val leaderboardEnabled: Boolean,
)

data class LibraryAuditLogRow(
    val id: UUID,
    val schoolId: UUID,
    val actorId: UUID?,
    val actorName: String,
    val action: String,
    val entityType: String,
    val entityId: UUID?,
    val metadata: String?,
    val previousState: String?,
    val newState: String?,
    val hash: String,
    val createdAt: Instant,
)

data class LibraryAnnouncementRow(
    val id: UUID,
    val schoolId: UUID,
    val title: String,
    val message: String,
    val audience: String,
    val createdBy: UUID?,
    val createdByName: String,
    val expiresAt: Instant?,
    val isActive: Boolean,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LibraryWishlistRow(
    val id: UUID,
    val schoolId: UUID,
    val studentId: UUID,
    val bookId: UUID,
    val createdAt: Instant,
)

data class LibraryReadingGoalRow(
    val id: UUID,
    val schoolId: UUID,
    val studentId: UUID,
    val goalCount: Int,
    val period: String,
    val targetYear: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LibraryAcquisitionRow(
    val id: UUID,
    val schoolId: UUID,
    val requestedBy: UUID,
    val requestedByName: String,
    val requestedByType: String,
    val title: String,
    val author: String?,
    val isbn: String?,
    val publisher: String?,
    val reason: String?,
    val estimatedCost: Double?,
    val status: String,
    val approvedBy: UUID?,
    val approvedAt: Instant?,
    val orderLink: String?,
    val orderedAt: Instant?,
    val receivedAt: Instant?,
    val convertedBookId: UUID?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

data class LibraryBadgeRow(
    val id: UUID,
    val schoolId: UUID,
    val studentId: UUID,
    val badgeType: String,
    val earnedAt: Instant,
)

data class LibraryDiscussionRow(
    val id: UUID,
    val schoolId: UUID,
    val bookId: UUID,
    val studentId: UUID,
    val studentName: String,
    val message: String,
    val createdAt: Instant,
    val deletedAt: Instant?,
    val deletedBy: UUID?,
)

// ── Repository ───────────────────────────────────────────────────────────────

class LibraryRepository {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    // ── Books ──────────────────────────────────────────────────────────────

    suspend fun createBook(
        schoolId: UUID, isbn: String?, title: String, author: String?, publisher: String?,
        category: String?, tags: List<String>?, totalCopies: Int, shelfLocation: String?,
        coverUrl: String?, replacementCost: Double?, seriesName: String?, seriesNumber: Int?,
        language: String, synopsis: String?, pageCount: Int?,
    ): UUID = dbQuery {
        val now = Instant.now()
        LibraryBooksTable.insert {
            it[LibraryBooksTable.schoolId] = schoolId
            it[LibraryBooksTable.isbn] = isbn
            it[LibraryBooksTable.title] = title
            it[LibraryBooksTable.author] = author
            it[LibraryBooksTable.publisher] = publisher
            it[LibraryBooksTable.category] = category
            it[LibraryBooksTable.tags] = tags?.let { t -> json.encodeToString(t) }
            it[LibraryBooksTable.totalCopies] = totalCopies
            it[LibraryBooksTable.availableCopies] = totalCopies
            it[LibraryBooksTable.shelfLocation] = shelfLocation
            it[LibraryBooksTable.coverUrl] = coverUrl
            it[LibraryBooksTable.replacementCost] = replacementCost
            it[LibraryBooksTable.seriesName] = seriesName
            it[LibraryBooksTable.seriesNumber] = seriesNumber
            it[LibraryBooksTable.language] = language
            it[LibraryBooksTable.synopsis] = synopsis
            it[LibraryBooksTable.pageCount] = pageCount
            it[createdAt] = now
            it[updatedAt] = now
        }[LibraryBooksTable.id].value
    }

    suspend fun findBookById(schoolId: UUID, bookId: UUID): LibraryBookRow? = dbQuery {
        LibraryBooksTable.selectAll()
            .where { (LibraryBooksTable.id eq bookId) and (LibraryBooksTable.schoolId eq schoolId) and (LibraryBooksTable.deletedAt.isNull()) }
            .map { it.toBookRow() }
            .singleOrNull()
    }

    suspend fun updateBook(schoolId: UUID, bookId: UUID, updates: Map<String, Any?>): Int = dbQuery {
        val now = Instant.now()
        LibraryBooksTable.update({
            (LibraryBooksTable.id eq bookId) and (LibraryBooksTable.schoolId eq schoolId) and (LibraryBooksTable.deletedAt.isNull())
        }) {
            updates.forEach { (k, v) ->
                when (k) {
                    "title" -> it[LibraryBooksTable.title] = v as String
                    "author" -> it[LibraryBooksTable.author] = v as String?
                    "publisher" -> it[LibraryBooksTable.publisher] = v as String?
                    "category" -> it[LibraryBooksTable.category] = v as String?
                    "tags" -> it[LibraryBooksTable.tags] = (v as? List<String>)?.let { t -> json.encodeToString(t) }
                    "shelfLocation" -> it[LibraryBooksTable.shelfLocation] = v as String?
                    "coverUrl" -> it[LibraryBooksTable.coverUrl] = v as String?
                    "replacementCost" -> it[LibraryBooksTable.replacementCost] = v as? Double
                    "seriesName" -> it[LibraryBooksTable.seriesName] = v as String?
                    "seriesNumber" -> it[LibraryBooksTable.seriesNumber] = v as? Int
                    "language" -> it[LibraryBooksTable.language] = v as String
                    "synopsis" -> it[LibraryBooksTable.synopsis] = v as String?
                    "pageCount" -> it[LibraryBooksTable.pageCount] = v as? Int
                    "totalCopies" -> it[LibraryBooksTable.totalCopies] = v as Int
                    "isArchived" -> it[LibraryBooksTable.isArchived] = v as Boolean
                }
            }
            it[updatedAt] = now
        }
    }

    suspend fun softDeleteBook(schoolId: UUID, bookId: UUID): Int = dbQuery {
        LibraryBooksTable.update({
            (LibraryBooksTable.id eq bookId) and (LibraryBooksTable.schoolId eq schoolId) and (LibraryBooksTable.deletedAt.isNull())
        }) {
            it[deletedAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun searchBooks(
        schoolId: UUID, query: String, category: String?, language: String?, tags: List<String>?, sortBy: String, availability: String, page: Int, limit: Int,
    ): Pair<List<LibraryBookRow>, Int> = readQuery {
        var q = LibraryBooksTable.selectAll()
            .where { (LibraryBooksTable.schoolId eq schoolId) and (LibraryBooksTable.deletedAt.isNull()) and (LibraryBooksTable.isArchived eq false) }

        if (query.isNotBlank()) {
            val pattern = "%${query}%"
            q = q.andWhere {
                (LibraryBooksTable.title like pattern) or
                (LibraryBooksTable.author like pattern) or
                (LibraryBooksTable.isbn like pattern)
            }
        }
        if (!category.isNullOrBlank()) {
            q = q.andWhere { LibraryBooksTable.category eq category }
        }
        if (!language.isNullOrBlank()) {
            q = q.andWhere { LibraryBooksTable.language eq language }
        }
        if (!tags.isNullOrEmpty()) {
            for (tag in tags) {
                val tagPattern = "%\"$tag\"%"
                q = q.andWhere { LibraryBooksTable.tags like tagPattern }
            }
        }
        when (availability) {
            "available" -> q = q.andWhere { LibraryBooksTable.availableCopies greaterEq 1 }
            "unavailable" -> q = q.andWhere { LibraryBooksTable.availableCopies eq 0 }
        }

        val total = q.count().toInt()

        val sorted = when (sortBy) {
            "title" -> q.orderBy(LibraryBooksTable.title to SortOrder.ASC)
            "author" -> q.orderBy(LibraryBooksTable.author to SortOrder.ASC)
            "newest" -> q.orderBy(LibraryBooksTable.createdAt to SortOrder.DESC)
            "trending" -> q.orderBy(LibraryBooksTable.availableCopies to SortOrder.ASC)
            "popularity" -> q.orderBy(LibraryBooksTable.availableCopies to SortOrder.ASC)
            else -> q.orderBy(LibraryBooksTable.createdAt to SortOrder.DESC)
        }

        val rows = sorted.limit(limit, ((page - 1) * limit).toLong())
            .map { it.toBookRow() }
        Pair(rows, total)
    }

    suspend fun updateBookAvailability(schoolId: UUID, bookId: UUID, delta: Int): Int = dbQuery {
        val book = LibraryBooksTable.selectAll()
            .where { (LibraryBooksTable.id eq bookId) and (LibraryBooksTable.schoolId eq schoolId) }
            .singleOrNull() ?: return@dbQuery 0
        val current = book[LibraryBooksTable.availableCopies]
        val newCount = (current + delta).coerceAtLeast(0)
        LibraryBooksTable.update({ LibraryBooksTable.id eq bookId }) {
            it[LibraryBooksTable.availableCopies] = newCount
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun decrementBookTotalCopies(schoolId: UUID, bookId: UUID): Int = dbQuery {
        val book = LibraryBooksTable.selectAll()
            .where { (LibraryBooksTable.id eq bookId) and (LibraryBooksTable.schoolId eq schoolId) }
            .singleOrNull() ?: return@dbQuery 0
        val current = book[LibraryBooksTable.totalCopies]
        val newCount = (current - 1).coerceAtLeast(0)
        LibraryBooksTable.update({ LibraryBooksTable.id eq bookId }) {
            it[LibraryBooksTable.totalCopies] = newCount
            it[updatedAt] = Instant.now()
        }
    }

    // ── Copies ─────────────────────────────────────────────────────────────

    suspend fun createCopy(schoolId: UUID, bookId: UUID, copyNumber: Int, barcode: String?, condition: String): UUID = dbQuery {
        val now = Instant.now()
        LibraryBookCopiesTable.insert {
            it[LibraryBookCopiesTable.schoolId] = schoolId
            it[LibraryBookCopiesTable.bookId] = bookId
            it[LibraryBookCopiesTable.copyNumber] = copyNumber
            it[LibraryBookCopiesTable.barcode] = barcode
            it[LibraryBookCopiesTable.condition] = condition
            it[status] = "available"
            it[createdAt] = now
            it[updatedAt] = now
        }[LibraryBookCopiesTable.id].value
    }

    suspend fun findCopyById(schoolId: UUID, copyId: UUID): LibraryCopyRow? = dbQuery {
        LibraryBookCopiesTable.selectAll()
            .where { (LibraryBookCopiesTable.id eq copyId) and (LibraryBookCopiesTable.schoolId eq schoolId) }
            .map { it.toCopyRow() }
            .singleOrNull()
    }

    suspend fun findCopyByBarcode(schoolId: UUID, barcode: String): LibraryCopyRow? = dbQuery {
        LibraryBookCopiesTable.selectAll()
            .where { (LibraryBookCopiesTable.barcode eq barcode) and (LibraryBookCopiesTable.schoolId eq schoolId) }
            .map { it.toCopyRow() }
            .singleOrNull()
    }

    suspend fun listCopiesForBook(schoolId: UUID, bookId: UUID): List<LibraryCopyRow> = dbQuery {
        LibraryBookCopiesTable.selectAll()
            .where { (LibraryBookCopiesTable.bookId eq bookId) and (LibraryBookCopiesTable.schoolId eq schoolId) }
            .orderBy(LibraryBookCopiesTable.copyNumber to SortOrder.ASC)
            .map { it.toCopyRow() }
    }

    suspend fun updateCopyStatus(schoolId: UUID, copyId: UUID, status: String): Int = dbQuery {
        LibraryBookCopiesTable.update({
            (LibraryBookCopiesTable.id eq copyId) and (LibraryBookCopiesTable.schoolId eq schoolId)
        }) {
            it[LibraryBookCopiesTable.status] = status
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun updateCopyStatusConditional(schoolId: UUID, copyId: UUID, expectedStatus: String, newStatus: String): Int = dbQuery {
        LibraryBookCopiesTable.update({
            (LibraryBookCopiesTable.id eq copyId) and
            (LibraryBookCopiesTable.schoolId eq schoolId) and
            (LibraryBookCopiesTable.status eq expectedStatus)
        }) {
            it[LibraryBookCopiesTable.status] = newStatus
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun nextCopyNumber(bookId: UUID): Int = dbQuery {
        val max = LibraryBookCopiesTable.selectAll()
            .where { LibraryBookCopiesTable.bookId eq bookId }
            .maxOfOrNull { it[LibraryBookCopiesTable.copyNumber] } ?: 0
        max + 1
    }

    // ── Issues ─────────────────────────────────────────────────────────────

    suspend fun createIssue(
        schoolId: UUID, bookId: UUID, copyId: UUID?, borrowerId: UUID, borrowerType: String,
        borrowerName: String, issueDate: LocalDate, dueDate: LocalDate,
    ): UUID = dbQuery {
        val now = Instant.now()
        LibraryIssuesTable.insert {
            it[LibraryIssuesTable.schoolId] = schoolId
            it[LibraryIssuesTable.bookId] = bookId
            it[LibraryIssuesTable.copyId] = copyId
            it[LibraryIssuesTable.borrowerId] = borrowerId
            it[LibraryIssuesTable.borrowerType] = borrowerType
            it[LibraryIssuesTable.borrowerName] = borrowerName
            it[LibraryIssuesTable.issueDate] = issueDate
            it[LibraryIssuesTable.dueDate] = dueDate
            it[status] = "issued"
            it[fineStatus] = "none"
            it[createdAt] = now
            it[updatedAt] = now
        }[LibraryIssuesTable.id].value
    }

    suspend fun findIssueById(schoolId: UUID, issueId: UUID): LibraryIssueRow? = dbQuery {
        LibraryIssuesTable.selectAll()
            .where { (LibraryIssuesTable.id eq issueId) and (LibraryIssuesTable.schoolId eq schoolId) }
            .map { it.toIssueRow() }
            .singleOrNull()
    }

    suspend fun findActiveIssueForCopy(schoolId: UUID, copyId: UUID): LibraryIssueRow? = dbQuery {
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.copyId eq copyId) and
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.status eq "issued")
            }
            .map { it.toIssueRow() }
            .singleOrNull()
    }

    suspend fun returnIssue(
        schoolId: UUID, issueId: UUID, returnDate: LocalDate, returnCondition: String?, damageNotes: String?,
        fineAmount: Double, fineStatus: String,
    ): Int = dbQuery {
        LibraryIssuesTable.update({
            (LibraryIssuesTable.id eq issueId) and (LibraryIssuesTable.schoolId eq schoolId) and (LibraryIssuesTable.status eq "issued")
        }) {
            it[LibraryIssuesTable.returnDate] = returnDate
            it[LibraryIssuesTable.returnCondition] = returnCondition
            it[LibraryIssuesTable.damageNotes] = damageNotes
            it[LibraryIssuesTable.fineAmount] = fineAmount
            it[LibraryIssuesTable.fineStatus] = fineStatus
            it[status] = "returned"
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun renewIssue(schoolId: UUID, issueId: UUID, newDueDate: LocalDate): Int = dbQuery {
        val current = LibraryIssuesTable.selectAll()
            .where { (LibraryIssuesTable.id eq issueId) and (LibraryIssuesTable.schoolId eq schoolId) }
            .singleOrNull()?.get(LibraryIssuesTable.renewalCount) ?: 0
        LibraryIssuesTable.update({
            (LibraryIssuesTable.id eq issueId) and (LibraryIssuesTable.schoolId eq schoolId) and (LibraryIssuesTable.status eq "issued")
        }) {
            it[LibraryIssuesTable.dueDate] = newDueDate
            it[renewalCount] = current + 1
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun markIssueLost(schoolId: UUID, issueId: UUID, fineAmount: Double): Int = dbQuery {
        LibraryIssuesTable.update({
            (LibraryIssuesTable.id eq issueId) and (LibraryIssuesTable.schoolId eq schoolId) and (LibraryIssuesTable.status eq "issued")
        }) {
            it[status] = "lost"
            it[LibraryIssuesTable.fineAmount] = fineAmount
            it[LibraryIssuesTable.fineStatus] = "pending"
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun payFine(schoolId: UUID, issueId: UUID): Int = dbQuery {
        LibraryIssuesTable.update({
            (LibraryIssuesTable.id eq issueId) and (LibraryIssuesTable.schoolId eq schoolId) and (LibraryIssuesTable.fineStatus eq "pending")
        }) {
            it[fineStatus] = "paid"
            it[finePaidAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun waiveFine(schoolId: UUID, issueId: UUID, waivedBy: UUID, reason: String): Int = dbQuery {
        LibraryIssuesTable.update({
            (LibraryIssuesTable.id eq issueId) and (LibraryIssuesTable.schoolId eq schoolId) and (LibraryIssuesTable.fineStatus eq "pending")
        }) {
            it[fineStatus] = "waived"
            it[LibraryIssuesTable.fineWaivedBy] = waivedBy
            it[LibraryIssuesTable.fineWaivedReason] = reason
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun listIssues(schoolId: UUID, status: String?, page: Int, limit: Int): Pair<List<LibraryIssueRow>, Int> = dbQuery {
        var q = LibraryIssuesTable.selectAll().where { LibraryIssuesTable.schoolId eq schoolId }
        if (!status.isNullOrBlank()) {
            q = q.andWhere { LibraryIssuesTable.status eq status }
        }
        val total = q.count().toInt()
        val rows = q.orderBy(LibraryIssuesTable.createdAt to SortOrder.DESC)
            .limit(limit, ((page - 1) * limit).toLong())
            .map { it.toIssueRow() }
        Pair(rows, total)
    }

    suspend fun listIssuesWithFines(schoolId: UUID): List<LibraryIssueRow> = dbQuery {
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.fineAmount greater 0.0)
            }
            .orderBy(LibraryIssuesTable.createdAt to SortOrder.DESC)
            .map { it.toIssueRow() }
    }

    suspend fun listIssuesForBorrower(schoolId: UUID, borrowerId: UUID, status: String?): List<LibraryIssueRow> = dbQuery {
        var q = LibraryIssuesTable.selectAll().where {
            (LibraryIssuesTable.borrowerId eq borrowerId) and (LibraryIssuesTable.schoolId eq schoolId)
        }
        if (!status.isNullOrBlank()) {
            q = q.andWhere { LibraryIssuesTable.status eq status }
        }
        q.orderBy(LibraryIssuesTable.createdAt to SortOrder.DESC).map { it.toIssueRow() }
    }

    suspend fun countActiveIssuesForBorrower(schoolId: UUID, borrowerId: UUID): Int = dbQuery {
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.borrowerId eq borrowerId) and
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.status eq "issued")
            }
            .count().toInt()
    }

    suspend fun listOverdueIssues(schoolId: UUID): List<LibraryIssueRow> = dbQuery {
        val today = LocalDate.now()
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.status eq "issued") and
                (LibraryIssuesTable.dueDate less today)
            }
            .orderBy(LibraryIssuesTable.dueDate to SortOrder.ASC)
            .map { it.toIssueRow() }
    }

    suspend fun listIssuesDueInDays(schoolId: UUID, days: Int): List<LibraryIssueRow> = dbQuery {
        val target = LocalDate.now().plusDays(days.toLong())
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.status eq "issued") and
                (LibraryIssuesTable.dueDate eq target)
            }
            .map { it.toIssueRow() }
    }

    // ── Reservations ────────────────────────────────────────────────────────

    suspend fun createReservation(
        schoolId: UUID, bookId: UUID, reservedBy: UUID, reservedByName: String, reservedByType: String,
    ): UUID = dbQuery {
        LibraryReservationsTable.insert {
            it[LibraryReservationsTable.schoolId] = schoolId
            it[LibraryReservationsTable.bookId] = bookId
            it[LibraryReservationsTable.reservedBy] = reservedBy
            it[LibraryReservationsTable.reservedByName] = reservedByName
            it[LibraryReservationsTable.reservedByType] = reservedByType
            it[status] = "pending"
            it[createdAt] = Instant.now()
        }[LibraryReservationsTable.id].value
    }

    suspend fun listReservationsForBook(schoolId: UUID, bookId: UUID, status: String?): List<LibraryReservationRow> = dbQuery {
        var q = LibraryReservationsTable.selectAll().where {
            (LibraryReservationsTable.bookId eq bookId) and (LibraryReservationsTable.schoolId eq schoolId)
        }
        if (!status.isNullOrBlank()) {
            q = q.andWhere { LibraryReservationsTable.status eq status }
        }
        q.orderBy(LibraryReservationsTable.reservedByType to SortOrder.DESC)
            .orderBy(LibraryReservationsTable.createdAt to SortOrder.ASC)
            .map { it.toReservationRow() }
    }

    suspend fun listReservationsForUser(schoolId: UUID, userId: UUID): List<LibraryReservationRow> = dbQuery {
        LibraryReservationsTable.selectAll()
            .where { (LibraryReservationsTable.reservedBy eq userId) and (LibraryReservationsTable.schoolId eq schoolId) }
            .orderBy(LibraryReservationsTable.createdAt to SortOrder.DESC)
            .map { it.toReservationRow() }
    }

    suspend fun updateReservationStatus(schoolId: UUID, reservationId: UUID, status: String): Int = dbQuery {
        LibraryReservationsTable.update({
            (LibraryReservationsTable.id eq reservationId) and (LibraryReservationsTable.schoolId eq schoolId)
        }) {
            it[LibraryReservationsTable.status] = status
            if (status == "fulfilled") it[fulfilledAt] = Instant.now()
        }
    }

    suspend fun cancelReservation(schoolId: UUID, reservationId: UUID, userId: UUID): Int = dbQuery {
        LibraryReservationsTable.update({
            (LibraryReservationsTable.id eq reservationId) and
            (LibraryReservationsTable.schoolId eq schoolId) and
            (LibraryReservationsTable.reservedBy eq userId) and
            (LibraryReservationsTable.status eq "pending")
        }) {
            it[status] = "cancelled"
        }
    }

    // ── Categories ──────────────────────────────────────────────────────────

    suspend fun listCategories(schoolId: UUID): List<LibraryCategoryRow> = dbQuery {
        LibraryCategoriesTable.selectAll()
            .where { LibraryCategoriesTable.schoolId eq schoolId }
            .orderBy(LibraryCategoriesTable.displayOrder to SortOrder.ASC)
            .map { it.toCategoryRow() }
    }

    suspend fun createCategory(schoolId: UUID, name: String, color: String, icon: String): UUID = dbQuery {
        val maxOrder = LibraryCategoriesTable.selectAll()
            .where { LibraryCategoriesTable.schoolId eq schoolId }
            .maxOfOrNull { it[LibraryCategoriesTable.displayOrder] } ?: 0
        LibraryCategoriesTable.insert {
            it[LibraryCategoriesTable.schoolId] = schoolId
            it[LibraryCategoriesTable.name] = name
            it[LibraryCategoriesTable.color] = color
            it[LibraryCategoriesTable.icon] = icon
            it[displayOrder] = maxOrder + 1
            it[createdAt] = Instant.now()
        }[LibraryCategoriesTable.id].value
    }

    suspend fun updateCategory(schoolId: UUID, categoryId: UUID, name: String?, color: String?, icon: String?): Int = dbQuery {
        LibraryCategoriesTable.update({
            (LibraryCategoriesTable.id eq categoryId) and (LibraryCategoriesTable.schoolId eq schoolId)
        }) {
            if (name != null) it[LibraryCategoriesTable.name] = name
            if (color != null) it[LibraryCategoriesTable.color] = color
            if (icon != null) it[LibraryCategoriesTable.icon] = icon
        }
    }

    suspend fun deleteCategory(schoolId: UUID, categoryId: UUID): Int = dbQuery {
        LibraryCategoriesTable.deleteWhere {
            (LibraryCategoriesTable.id eq categoryId) and (LibraryCategoriesTable.schoolId eq schoolId)
        }
    }

    suspend fun reorderCategories(schoolId: UUID, orders: List<Pair<UUID, Int>>): Int = dbQuery {
        var count = 0
        orders.forEach { (id, order) ->
            count += LibraryCategoriesTable.update({
                (LibraryCategoriesTable.id eq id) and (LibraryCategoriesTable.schoolId eq schoolId)
            }) {
                it[displayOrder] = order
            }
        }
        count
    }

    // ── Settings ────────────────────────────────────────────────────────────

    suspend fun getSettings(schoolId: UUID): LibrarySettingsRow? = dbQuery {
        LibrarySettingsTable.selectAll()
            .where { LibrarySettingsTable.schoolId eq schoolId }
            .map { it.toSettingsRow() }
            .singleOrNull()
    }

    suspend fun upsertSettings(schoolId: UUID, updates: Map<String, Any?>): LibrarySettingsRow? = dbQuery {
        val existing = LibrarySettingsTable.selectAll()
            .where { LibrarySettingsTable.schoolId eq schoolId }
            .singleOrNull()

        if (existing == null) {
            LibrarySettingsTable.insert {
                it[LibrarySettingsTable.schoolId] = schoolId
                applySettingsUpdatesTo(it, updates)
                it[createdAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }
        } else {
            LibrarySettingsTable.update({ LibrarySettingsTable.schoolId eq schoolId }) {
                applySettingsUpdatesTo(it, updates)
                it[updatedAt] = Instant.now()
            }
        }
        LibrarySettingsTable.selectAll()
            .where { LibrarySettingsTable.schoolId eq schoolId }
            .map { it.toSettingsRow() }
            .singleOrNull()
    }

    private fun applySettingsUpdatesTo(
        it: org.jetbrains.exposed.sql.statements.UpdateBuilder<*>,
        updates: Map<String, Any?>
    ) {
        updates.forEach { (k, v) ->
            if (v != null) {
                when (k) {
                    "defaultLoanDays" -> it[LibrarySettingsTable.defaultLoanDays] = v as Int
                    "finePerDay" -> it[LibrarySettingsTable.finePerDay] = v as Double
                    "maxBooksPerStudent" -> it[LibrarySettingsTable.maxBooksPerStudent] = v as Int
                    "maxRenewals" -> it[LibrarySettingsTable.maxRenewals] = v as Int
                    "reservationTimeoutDays" -> it[LibrarySettingsTable.reservationTimeoutDays] = v as Int
                    "dueReminderDays" -> it[LibrarySettingsTable.dueReminderDays] = v as Int
                    "fineCapEnabled" -> it[LibrarySettingsTable.fineCapEnabled] = v as Boolean
                    "quickIssueEnabled" -> it[LibrarySettingsTable.quickIssueEnabled] = v as Boolean
                    "bulkReturnEnabled" -> it[LibrarySettingsTable.bulkReturnEnabled] = v as Boolean
                    "leaderboardEnabled" -> it[LibrarySettingsTable.leaderboardEnabled] = v as Boolean
                }
            }
        }
    }

    // ── Audit Log ───────────────────────────────────────────────────────────

    suspend fun appendAuditLog(
        schoolId: UUID, actorId: UUID?, actorName: String, action: String,
        entityType: String, entityId: UUID?, metadata: Map<String, Any?>? = null,
        previousState: String? = null, newState: String? = null,
    ): UUID = dbQuery {
        // ── Genesis seeding (spec §9 Audit Log) ──────────────────────────────
        // Ensure a GENESIS entry exists for this school before appending any
        // real audit entry. The genesis hash is deterministic per school so
        // the chain can be verified from a known anchor.
        val existingCount = LibraryAuditLogTable.selectAll()
            .where { LibraryAuditLogTable.schoolId eq schoolId }
            .count()

        if (existingCount == 0L) {
            val genesisPayload = "GENESIS|$schoolId"
            val genesisHash = java.security.MessageDigest.getInstance("SHA-256")
                .digest(genesisPayload.toByteArray())
                .joinToString("") { "%02x".format(it) }
            LibraryAuditLogTable.insert {
                it[LibraryAuditLogTable.schoolId] = schoolId
                it[LibraryAuditLogTable.actorId] = null
                it[LibraryAuditLogTable.actorName] = "system"
                it[LibraryAuditLogTable.action] = "GENESIS"
                it[LibraryAuditLogTable.entityType] = "school"
                it[LibraryAuditLogTable.entityId] = schoolId
                it[LibraryAuditLogTable.metadata] = null
                it[LibraryAuditLogTable.previousState] = null
                it[LibraryAuditLogTable.newState] = null
                it[LibraryAuditLogTable.hash] = genesisHash
                it[createdAt] = Instant.now()
            }
        }

        val lastHash = LibraryAuditLogTable.selectAll()
            .where { LibraryAuditLogTable.schoolId eq schoolId }
            .orderBy(LibraryAuditLogTable.createdAt to SortOrder.DESC)
            .limit(1)
            .firstOrNull()?.get(LibraryAuditLogTable.hash) ?: ""

        val payload = buildString {
            append(lastHash).append('|')
            append(actorName).append('|')
            append(action).append('|')
            append(entityType).append('|')
            append(entityId?.toString() ?: "").append('|')
            append(metadata?.let { json.encodeToString(it) } ?: "")
        }
        val hash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(payload.toByteArray())
            .joinToString("") { "%02x".format(it) }

        LibraryAuditLogTable.insert {
            it[LibraryAuditLogTable.schoolId] = schoolId
            it[LibraryAuditLogTable.actorId] = actorId
            it[LibraryAuditLogTable.actorName] = actorName
            it[LibraryAuditLogTable.action] = action
            it[LibraryAuditLogTable.entityType] = entityType
            it[LibraryAuditLogTable.entityId] = entityId
            it[LibraryAuditLogTable.metadata] = metadata?.let { m -> json.encodeToString(m) }
            it[LibraryAuditLogTable.previousState] = previousState
            it[LibraryAuditLogTable.newState] = newState
            it[LibraryAuditLogTable.hash] = hash
            it[createdAt] = Instant.now()
        }[LibraryAuditLogTable.id].value
    }

    suspend fun listAuditLog(schoolId: UUID, page: Int, limit: Int): Pair<List<LibraryAuditLogRow>, Int> = readQuery {
        val q = LibraryAuditLogTable.selectAll().where { LibraryAuditLogTable.schoolId eq schoolId }
        val total = q.count().toInt()
        val rows = q.orderBy(LibraryAuditLogTable.createdAt to SortOrder.DESC)
            .limit(limit, ((page - 1) * limit).toLong())
            .map { it.toAuditLogRow() }
        Pair(rows, total)
    }

    // ── Cursor-based pagination (spec §17) ──────────────────────────────────
    suspend fun listAuditLogCursor(
        schoolId: UUID, cursor: Pair<java.time.Instant, UUID>?, limit: Int,
    ): Pair<List<LibraryAuditLogRow>, Boolean> = readQuery {
        var q = LibraryAuditLogTable.selectAll().where { LibraryAuditLogTable.schoolId eq schoolId }
        if (cursor != null) {
            q = q.andWhere {
                (LibraryAuditLogTable.createdAt less cursor.first) or
                    ((LibraryAuditLogTable.createdAt eq cursor.first) and (LibraryAuditLogTable.id less cursor.second))
            }
        }
        val rows = q.orderBy(LibraryAuditLogTable.createdAt to SortOrder.DESC, LibraryAuditLogTable.id to SortOrder.DESC)
            .limit(limit + 1)
            .map { it.toAuditLogRow() }
        val hasMore = rows.size > limit
        val data = if (hasMore) rows.dropLast(1) else rows
        Pair(data, hasMore)
    }

    suspend fun listIssuesCursor(
        schoolId: UUID, status: String?, cursor: Pair<java.time.Instant, UUID>?, limit: Int,
    ): Pair<List<LibraryIssueRow>, Boolean> = readQuery {
        var q = LibraryIssuesTable.selectAll().where { LibraryIssuesTable.schoolId eq schoolId }
        if (!status.isNullOrBlank()) {
            q = q.andWhere { LibraryIssuesTable.status eq status }
        }
        if (cursor != null) {
            q = q.andWhere {
                (LibraryIssuesTable.createdAt less cursor.first) or
                    ((LibraryIssuesTable.createdAt eq cursor.first) and (LibraryIssuesTable.id less cursor.second))
            }
        }
        val rows = q.orderBy(LibraryIssuesTable.createdAt to SortOrder.DESC, LibraryIssuesTable.id to SortOrder.DESC)
            .limit(limit + 1)
            .map { it.toIssueRow() }
        val hasMore = rows.size > limit
        val data = if (hasMore) rows.dropLast(1) else rows
        Pair(data, hasMore)
    }

    // ── Announcements ───────────────────────────────────────────────────────

    suspend fun listAnnouncements(schoolId: UUID, activeOnly: Boolean): List<LibraryAnnouncementRow> = dbQuery {
        var q = LibraryAnnouncementsTable.selectAll().where { LibraryAnnouncementsTable.schoolId eq schoolId }
        if (activeOnly) {
            q = q.andWhere { LibraryAnnouncementsTable.isActive eq true }
        }
        q.orderBy(LibraryAnnouncementsTable.createdAt to SortOrder.DESC).map { it.toAnnouncementRow() }
    }

    suspend fun createAnnouncement(
        schoolId: UUID, title: String, message: String, audience: String,
        createdBy: UUID?, createdByName: String, expiresAt: Instant?,
    ): UUID = dbQuery {
        val now = Instant.now()
        LibraryAnnouncementsTable.insert {
            it[LibraryAnnouncementsTable.schoolId] = schoolId
            it[LibraryAnnouncementsTable.title] = title
            it[LibraryAnnouncementsTable.message] = message
            it[LibraryAnnouncementsTable.audience] = audience
            it[LibraryAnnouncementsTable.createdBy] = createdBy
            it[LibraryAnnouncementsTable.createdByName] = createdByName
            it[LibraryAnnouncementsTable.expiresAt] = expiresAt
            it[isActive] = true
            it[createdAt] = now
            it[updatedAt] = now
        }[LibraryAnnouncementsTable.id].value
    }

    suspend fun updateAnnouncement(
        schoolId: UUID, announcementId: UUID, title: String?, message: String?,
        audience: String?, expiresAt: Instant?, isActive: Boolean?,
    ): Int = dbQuery {
        LibraryAnnouncementsTable.update({
            (LibraryAnnouncementsTable.id eq announcementId) and (LibraryAnnouncementsTable.schoolId eq schoolId)
        }) {
            if (title != null) it[LibraryAnnouncementsTable.title] = title
            if (message != null) it[LibraryAnnouncementsTable.message] = message
            if (audience != null) it[LibraryAnnouncementsTable.audience] = audience
            if (expiresAt != null) it[LibraryAnnouncementsTable.expiresAt] = expiresAt
            if (isActive != null) it[LibraryAnnouncementsTable.isActive] = isActive
            it[updatedAt] = Instant.now()
        }
    }

    // ── Wishlist ────────────────────────────────────────────────────────────

    suspend fun addToWishlist(schoolId: UUID, studentId: UUID, bookId: UUID): UUID = dbQuery {
        LibraryWishlistTable.insert {
            it[LibraryWishlistTable.schoolId] = schoolId
            it[LibraryWishlistTable.studentId] = studentId
            it[LibraryWishlistTable.bookId] = bookId
            it[createdAt] = Instant.now()
        }[LibraryWishlistTable.id].value
    }

    suspend fun removeFromWishlist(schoolId: UUID, studentId: UUID, bookId: UUID): Int = dbQuery {
        LibraryWishlistTable.deleteWhere {
            (LibraryWishlistTable.schoolId eq schoolId) and
            (LibraryWishlistTable.studentId eq studentId) and
            (LibraryWishlistTable.bookId eq bookId)
        }
    }

    suspend fun listWishlist(schoolId: UUID, studentId: UUID): List<LibraryWishlistRow> = dbQuery {
        LibraryWishlistTable.selectAll()
            .where { (LibraryWishlistTable.studentId eq studentId) and (LibraryWishlistTable.schoolId eq schoolId) }
            .orderBy(LibraryWishlistTable.createdAt to SortOrder.DESC)
            .map { it.toWishlistRow() }
    }

    suspend fun listWishlistForBook(schoolId: UUID, bookId: UUID): List<LibraryWishlistRow> = dbQuery {
        LibraryWishlistTable.selectAll()
            .where { (LibraryWishlistTable.bookId eq bookId) and (LibraryWishlistTable.schoolId eq schoolId) }
            .orderBy(LibraryWishlistTable.createdAt to SortOrder.ASC)
            .map { it.toWishlistRow() }
    }

    suspend fun countWishlist(schoolId: UUID, studentId: UUID): Int = dbQuery {
        LibraryWishlistTable.selectAll()
            .where { (LibraryWishlistTable.studentId eq studentId) and (LibraryWishlistTable.schoolId eq schoolId) }
            .count().toInt()
    }

    // ── Reading Goals ───────────────────────────────────────────────────────

    suspend fun getReadingGoal(schoolId: UUID, studentId: UUID, period: String, targetYear: Int): LibraryReadingGoalRow? = dbQuery {
        LibraryReadingGoalsTable.selectAll()
            .where {
                (LibraryReadingGoalsTable.studentId eq studentId) and
                (LibraryReadingGoalsTable.schoolId eq schoolId) and
                (LibraryReadingGoalsTable.period eq period) and
                (LibraryReadingGoalsTable.targetYear eq targetYear)
            }
            .map { it.toReadingGoalRow() }
            .singleOrNull()
    }

    suspend fun upsertReadingGoal(schoolId: UUID, studentId: UUID, goalCount: Int, period: String, targetYear: Int): UUID = dbQuery {
        val existing = LibraryReadingGoalsTable.selectAll()
            .where {
                (LibraryReadingGoalsTable.studentId eq studentId) and
                (LibraryReadingGoalsTable.schoolId eq schoolId) and
                (LibraryReadingGoalsTable.period eq period) and
                (LibraryReadingGoalsTable.targetYear eq targetYear)
            }
            .singleOrNull()

        if (existing == null) {
            LibraryReadingGoalsTable.insert {
                it[LibraryReadingGoalsTable.schoolId] = schoolId
                it[LibraryReadingGoalsTable.studentId] = studentId
                it[LibraryReadingGoalsTable.goalCount] = goalCount
                it[LibraryReadingGoalsTable.period] = period
                it[LibraryReadingGoalsTable.targetYear] = targetYear
                it[createdAt] = Instant.now()
                it[updatedAt] = Instant.now()
            }[LibraryReadingGoalsTable.id].value
        } else {
            LibraryReadingGoalsTable.update({
                (LibraryReadingGoalsTable.id eq existing[LibraryReadingGoalsTable.id].value)
            }) {
                it[LibraryReadingGoalsTable.goalCount] = goalCount
                it[updatedAt] = Instant.now()
            }
            existing[LibraryReadingGoalsTable.id].value
        }
    }

    // ── Acquisition Requests ────────────────────────────────────────────────

    suspend fun createAcquisitionRequest(
        schoolId: UUID, requestedBy: UUID, requestedByName: String, requestedByType: String,
        title: String, author: String?, isbn: String?, publisher: String?,
        reason: String?, estimatedCost: Double?,
    ): UUID = dbQuery {
        val now = Instant.now()
        LibraryAcquisitionRequestsTable.insert {
            it[LibraryAcquisitionRequestsTable.schoolId] = schoolId
            it[LibraryAcquisitionRequestsTable.requestedBy] = requestedBy
            it[LibraryAcquisitionRequestsTable.requestedByName] = requestedByName
            it[LibraryAcquisitionRequestsTable.requestedByType] = requestedByType
            it[LibraryAcquisitionRequestsTable.title] = title
            it[LibraryAcquisitionRequestsTable.author] = author
            it[LibraryAcquisitionRequestsTable.isbn] = isbn
            it[LibraryAcquisitionRequestsTable.publisher] = publisher
            it[LibraryAcquisitionRequestsTable.reason] = reason
            it[LibraryAcquisitionRequestsTable.estimatedCost] = estimatedCost
            it[status] = "pending"
            it[createdAt] = now
            it[updatedAt] = now
        }[LibraryAcquisitionRequestsTable.id].value
    }

    suspend fun listAcquisitionRequests(schoolId: UUID, status: String?): List<LibraryAcquisitionRow> = dbQuery {
        var q = LibraryAcquisitionRequestsTable.selectAll()
            .where { LibraryAcquisitionRequestsTable.schoolId eq schoolId }
        if (!status.isNullOrBlank()) {
            q = q.andWhere { LibraryAcquisitionRequestsTable.status eq status }
        }
        q.orderBy(LibraryAcquisitionRequestsTable.createdAt to SortOrder.DESC).map { it.toAcquisitionRow() }
    }

    suspend fun updateAcquisitionStatus(
        schoolId: UUID, requestId: UUID, status: String, approvedBy: UUID?, orderLink: String?,
    ): Int = dbQuery {
        LibraryAcquisitionRequestsTable.update({
            (LibraryAcquisitionRequestsTable.id eq requestId) and (LibraryAcquisitionRequestsTable.schoolId eq schoolId)
        }) {
            it[LibraryAcquisitionRequestsTable.status] = status
            if (approvedBy != null && status in listOf("approved", "rejected")) {
                it[LibraryAcquisitionRequestsTable.approvedBy] = approvedBy
                it[LibraryAcquisitionRequestsTable.approvedAt] = Instant.now()
            }
            if (orderLink != null) it[LibraryAcquisitionRequestsTable.orderLink] = orderLink
            if (status == "ordered") it[LibraryAcquisitionRequestsTable.orderedAt] = Instant.now()
            if (status == "received") it[LibraryAcquisitionRequestsTable.receivedAt] = Instant.now()
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun setConvertedBookId(schoolId: UUID, requestId: UUID, bookId: UUID): Int = dbQuery {
        LibraryAcquisitionRequestsTable.update({
            (LibraryAcquisitionRequestsTable.id eq requestId) and
            (LibraryAcquisitionRequestsTable.schoolId eq schoolId)
        }) {
            it[LibraryAcquisitionRequestsTable.convertedBookId] = bookId
            it[updatedAt] = Instant.now()
        }
    }

    suspend fun findAcquisitionRequest(schoolId: UUID, requestId: UUID): LibraryAcquisitionRow? = dbQuery {
        LibraryAcquisitionRequestsTable.selectAll()
            .where {
                (LibraryAcquisitionRequestsTable.id eq requestId) and
                (LibraryAcquisitionRequestsTable.schoolId eq schoolId)
            }
            .map { it.toAcquisitionRow() }
            .singleOrNull()
    }

    // ── Badges ──────────────────────────────────────────────────────────────

    suspend fun listBadges(schoolId: UUID, studentId: UUID): List<LibraryBadgeRow> = dbQuery {
        LibraryReadingBadgesTable.selectAll()
            .where { (LibraryReadingBadgesTable.studentId eq studentId) and (LibraryReadingBadgesTable.schoolId eq schoolId) }
            .map { it.toBadgeRow() }
    }

    suspend fun awardBadge(schoolId: UUID, studentId: UUID, badgeType: String): Boolean = dbQuery {
        val existing = LibraryReadingBadgesTable.selectAll()
            .where {
                (LibraryReadingBadgesTable.studentId eq studentId) and
                (LibraryReadingBadgesTable.schoolId eq schoolId) and
                (LibraryReadingBadgesTable.badgeType eq badgeType)
            }
            .singleOrNull()
        if (existing != null) {
            false
        } else {
            LibraryReadingBadgesTable.insert {
                it[LibraryReadingBadgesTable.schoolId] = schoolId
                it[LibraryReadingBadgesTable.studentId] = studentId
                it[LibraryReadingBadgesTable.badgeType] = badgeType
                it[earnedAt] = Instant.now()
            }
            true
        }
    }

    // ── Discussions ─────────────────────────────────────────────────────────

    suspend fun listDiscussions(schoolId: UUID, bookId: UUID): List<LibraryDiscussionRow> = dbQuery {
        LibraryBookDiscussionsTable.selectAll()
            .where {
                (LibraryBookDiscussionsTable.bookId eq bookId) and
                (LibraryBookDiscussionsTable.schoolId eq schoolId) and
                (LibraryBookDiscussionsTable.deletedAt.isNull())
            }
            .orderBy(LibraryBookDiscussionsTable.createdAt to SortOrder.ASC)
            .map { it.toDiscussionRow() }
    }

    suspend fun postDiscussion(schoolId: UUID, bookId: UUID, studentId: UUID, studentName: String, message: String): UUID = dbQuery {
        LibraryBookDiscussionsTable.insert {
            it[LibraryBookDiscussionsTable.schoolId] = schoolId
            it[LibraryBookDiscussionsTable.bookId] = bookId
            it[LibraryBookDiscussionsTable.studentId] = studentId
            it[LibraryBookDiscussionsTable.studentName] = studentName
            it[LibraryBookDiscussionsTable.message] = message
            it[createdAt] = Instant.now()
        }[LibraryBookDiscussionsTable.id].value
    }

    suspend fun deleteDiscussion(schoolId: UUID, discussionId: UUID, deletedBy: UUID): Int = dbQuery {
        LibraryBookDiscussionsTable.update({
            (LibraryBookDiscussionsTable.id eq discussionId) and
            (LibraryBookDiscussionsTable.schoolId eq schoolId) and
            (LibraryBookDiscussionsTable.deletedAt.isNull())
        }) {
            it[deletedAt] = Instant.now()
            it[LibraryBookDiscussionsTable.deletedBy] = deletedBy
        }
    }

    // ── Dashboard counts ────────────────────────────────────────────────────

    suspend fun countBooks(schoolId: UUID): Int = dbQuery {
        LibraryBooksTable.selectAll()
            .where { (LibraryBooksTable.schoolId eq schoolId) and (LibraryBooksTable.deletedAt.isNull()) }
            .count().toInt()
    }

    suspend fun sumTotalCopies(schoolId: UUID): Int = dbQuery {
        LibraryBooksTable.selectAll()
            .where { (LibraryBooksTable.schoolId eq schoolId) and (LibraryBooksTable.deletedAt.isNull()) }
            .sumOf { it[LibraryBooksTable.totalCopies] }
    }

    suspend fun sumAvailableCopies(schoolId: UUID): Int = dbQuery {
        LibraryBooksTable.selectAll()
            .where { (LibraryBooksTable.schoolId eq schoolId) and (LibraryBooksTable.deletedAt.isNull()) }
            .sumOf { it[LibraryBooksTable.availableCopies] }
    }

    suspend fun countIssued(schoolId: UUID): Int = dbQuery {
        LibraryIssuesTable.selectAll()
            .where { (LibraryIssuesTable.schoolId eq schoolId) and (LibraryIssuesTable.status eq "issued") }
            .count().toInt()
    }

    suspend fun countOverdue(schoolId: UUID): Int = dbQuery {
        val today = LocalDate.now()
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.status eq "issued") and
                (LibraryIssuesTable.dueDate less today)
            }
            .count().toInt()
    }

    suspend fun countActiveReservations(schoolId: UUID): Int = dbQuery {
        LibraryReservationsTable.selectAll()
            .where { (LibraryReservationsTable.schoolId eq schoolId) and (LibraryReservationsTable.status eq "pending") }
            .count().toInt()
    }

    suspend fun countLost(schoolId: UUID): Int = dbQuery {
        LibraryIssuesTable.selectAll()
            .where { (LibraryIssuesTable.schoolId eq schoolId) and (LibraryIssuesTable.status eq "lost") }
            .count().toInt()
    }

    suspend fun countOutstandingFines(schoolId: UUID): Pair<Int, Double> = dbQuery {
        val rows = LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.fineStatus eq "pending")
            }
            .toList()
        rows.size to rows.sumOf { it[LibraryIssuesTable.fineAmount] }
    }

    suspend fun listOutstandingFines(schoolId: UUID, page: Int, limit: Int): Pair<List<LibraryIssueRow>, Int> = dbQuery {
        val q = LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.fineStatus eq "pending") and
                (LibraryIssuesTable.fineAmount greater 0.0)
            }
        val total = q.count().toInt()
        val rows = q.orderBy(LibraryIssuesTable.createdAt to SortOrder.DESC)
            .limit(limit, ((page - 1) * limit).toLong())
            .map { it.toIssueRow() }
        Pair(rows, total)
    }

    suspend fun finesCollectedThisMonth(schoolId: UUID): Double = dbQuery {
        val monthStart = Instant.now()
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .withDayOfMonth(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.fineStatus eq "paid") and
                (LibraryIssuesTable.finePaidAt greaterEq monthStart)
            }
            .sumOf { it[LibraryIssuesTable.fineAmount] }
    }

    suspend fun deleteCopy(schoolId: UUID, copyId: UUID): Int = dbQuery {
        LibraryBookCopiesTable.deleteWhere {
            (LibraryBookCopiesTable.id eq copyId) and
            (LibraryBookCopiesTable.schoolId eq schoolId) and
            (LibraryBookCopiesTable.status eq "available")
        }
    }

    suspend fun listAllAuditHashes(schoolId: UUID): List<Triple<UUID, String, String>> = dbQuery {
        LibraryAuditLogTable.selectAll()
            .where { LibraryAuditLogTable.schoolId eq schoolId }
            .orderBy(LibraryAuditLogTable.createdAt to SortOrder.ASC)
            .map { Triple(it[LibraryAuditLogTable.id].value, it[LibraryAuditLogTable.hash], it[LibraryAuditLogTable.actorName] + "|" + it[LibraryAuditLogTable.action] + "|" + it[LibraryAuditLogTable.entityType] + "|" + (it[LibraryAuditLogTable.entityId]?.toString() ?: "") + "|" + (it[LibraryAuditLogTable.metadata] ?: "")) }
    }

    suspend fun setFeaturedBook(schoolId: UUID, bookId: UUID, type: String): Int = dbQuery {
        LibrarySettingsTable.update({
            LibrarySettingsTable.schoolId eq schoolId
        }) {
            it[LibrarySettingsTable.featuredBookId] = bookId
            it[LibrarySettingsTable.featuredType] = type
            it[LibrarySettingsTable.featuredUpdatedAt] = Instant.now()
        }
    }

    suspend fun clearFeaturedBook(schoolId: UUID): Int = dbQuery {
        LibrarySettingsTable.update({
            LibrarySettingsTable.schoolId eq schoolId
        }) {
            it[LibrarySettingsTable.featuredBookId] = null
            it[LibrarySettingsTable.featuredType] = null
            it[LibrarySettingsTable.featuredUpdatedAt] = null
        }
    }

    suspend fun countReturnedByBorrower(schoolId: UUID, borrowerId: UUID): Int = dbQuery {
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.borrowerId eq borrowerId) and
                (LibraryIssuesTable.status eq "returned")
            }
            .count().toInt()
    }

    // ── Background-job helpers ────────────────────────────────────────────────

    suspend fun expireStaleReservations(schoolId: UUID, timeoutDays: Int): Int = dbQuery {
        val cutoff = Instant.now().minus(timeoutDays.toLong(), java.time.temporal.ChronoUnit.DAYS)
        LibraryReservationsTable.update({
            (LibraryReservationsTable.schoolId eq schoolId) and
            (LibraryReservationsTable.status eq "pending") and
            (LibraryReservationsTable.createdAt less cutoff)
        }) {
            it[status] = "expired"
        }
    }

    suspend fun deactivateExpiredAnnouncements(schoolId: UUID): Int = dbQuery {
        val now = Instant.now()
        LibraryAnnouncementsTable.update({
            (LibraryAnnouncementsTable.schoolId eq schoolId) and
            (LibraryAnnouncementsTable.isActive eq true) and
            (LibraryAnnouncementsTable.expiresAt.isNotNull()) and
            (LibraryAnnouncementsTable.expiresAt less now)
        }) {
            it[isActive] = false
        }
    }

    suspend fun deleteOldAuditLogs(schoolId: UUID, retentionDays: Int): Int = dbQuery {
        val cutoff = Instant.now().minus(retentionDays.toLong(), java.time.temporal.ChronoUnit.DAYS)
        LibraryAuditLogTable.deleteWhere {
            (LibraryAuditLogTable.schoolId eq schoolId) and
            (LibraryAuditLogTable.createdAt less cutoff)
        }
    }

    // Spec §16: Issues retention 5yr — only returned/closed issues
    suspend fun deleteOldIssues(schoolId: UUID, retentionDays: Int): Int = dbQuery {
        val cutoff = java.time.LocalDate.now().minusDays(retentionDays.toLong())
        LibraryIssuesTable.deleteWhere {
            (LibraryIssuesTable.schoolId eq schoolId) and
            (LibraryIssuesTable.status inList listOf("returned", "lost")) and
            (LibraryIssuesTable.returnDate lessEq cutoff)
        }
    }

    suspend fun deleteOldReservations(schoolId: UUID, retentionDays: Int): Int = dbQuery {
        val cutoff = Instant.now().minus(retentionDays.toLong(), java.time.temporal.ChronoUnit.DAYS)
        LibraryReservationsTable.deleteWhere {
            (LibraryReservationsTable.schoolId eq schoolId) and
            (LibraryReservationsTable.status inList listOf("fulfilled", "cancelled", "expired")) and
            (LibraryReservationsTable.createdAt less cutoff)
        }
    }

    suspend fun deleteOldFines(schoolId: UUID, retentionDays: Int): Int = dbQuery {
        val cutoff = Instant.now().minus(retentionDays.toLong(), java.time.temporal.ChronoUnit.DAYS)
        LibraryIssuesTable.deleteWhere {
            (LibraryIssuesTable.schoolId eq schoolId) and
            (LibraryIssuesTable.status eq "returned") and
            (LibraryIssuesTable.fineStatus inList listOf("paid", "waived")) and
            (LibraryIssuesTable.fineAmount greater 0.0) and
            (LibraryIssuesTable.updatedAt less cutoff)
        }
    }

    suspend fun deleteOldAnnouncements(schoolId: UUID, retentionDays: Int): Int = dbQuery {
        val cutoff = Instant.now().minus(retentionDays.toLong(), java.time.temporal.ChronoUnit.DAYS)
        LibraryAnnouncementsTable.deleteWhere {
            (LibraryAnnouncementsTable.schoolId eq schoolId) and
            (LibraryAnnouncementsTable.isActive eq false) and
            (LibraryAnnouncementsTable.createdAt less cutoff)
        }
    }

    suspend fun deleteOldAcquisitionRequests(schoolId: UUID, retentionDays: Int): Int = dbQuery {
        val cutoff = Instant.now().minus(retentionDays.toLong(), java.time.temporal.ChronoUnit.DAYS)
        LibraryAcquisitionRequestsTable.deleteWhere {
            (LibraryAcquisitionRequestsTable.schoolId eq schoolId) and
            (LibraryAcquisitionRequestsTable.status inList listOf("received", "rejected")) and
            (LibraryAcquisitionRequestsTable.updatedAt less cutoff)
        }
    }

    suspend fun listAllBorrowerIdsWithIssues(schoolId: UUID): List<Pair<UUID, String>> = dbQuery {
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.borrowerType eq "student")
            }
            .map { it[LibraryIssuesTable.borrowerId] to it[LibraryIssuesTable.borrowerName] }
            .distinct()
    }

    suspend fun listAllActiveSchoolIds(): List<UUID> = dbQuery {
        com.littlebridge.enrollplus.db.SchoolsTable.selectAll()
            .where { com.littlebridge.enrollplus.db.SchoolsTable.isActive eq true }
            .map { it[com.littlebridge.enrollplus.db.SchoolsTable.id].value }
    }

    // ── Trending / Recommendations / Repair ───────────────────────────────────

    suspend fun countIssuesSince(schoolId: UUID, since: LocalDate): List<Pair<UUID, Int>> = dbQuery {
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.issueDate greaterEq since)
            }
            .map { it[LibraryIssuesTable.bookId] }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
    }

    /**
     * Refresh the trending materialized view (spec §17).
     * Only runs on Postgres; silently no-ops on SQLite.
     */
    suspend fun refreshTrendingMaterializedView() {
        if (!DatabaseFactory.isPostgres) return
        dbQuery {
            TransactionManager.current().exec("REFRESH MATERIALIZED VIEW CONCURRENTLY library_trending_mv")
        }
    }

    suspend fun listCopiesByStatus(schoolId: UUID, status: String): List<LibraryCopyRow> = dbQuery {
        LibraryBookCopiesTable.selectAll()
            .where {
                (LibraryBookCopiesTable.schoolId eq schoolId) and
                (LibraryBookCopiesTable.status eq status)
            }
            .orderBy(LibraryBookCopiesTable.copyNumber to SortOrder.ASC)
            .map { it.toCopyRow() }
    }

    suspend fun updateCopyCondition(schoolId: UUID, copyId: UUID, condition: String): Int = dbQuery {
        LibraryBookCopiesTable.update({
            (LibraryBookCopiesTable.id eq copyId) and (LibraryBookCopiesTable.schoolId eq schoolId)
        }) {
            it[LibraryBookCopiesTable.condition] = condition
        }
    }

    suspend fun listBooksByCategory(schoolId: UUID, category: String, limit: Int): List<LibraryBookRow> = dbQuery {
        LibraryBooksTable.selectAll()
            .where {
                (LibraryBooksTable.schoolId eq schoolId) and
                (LibraryBooksTable.category eq category) and
                (LibraryBooksTable.deletedAt.isNull()) and
                (LibraryBooksTable.isArchived eq false)
            }
            .limit(limit)
            .map { it.toBookRow() }
    }

    suspend fun listReturnedBooksByBorrower(schoolId: UUID, borrowerId: UUID): List<UUID> = dbQuery {
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.borrowerId eq borrowerId) and
                (LibraryIssuesTable.status eq "returned")
            }
            .map { it[LibraryIssuesTable.bookId] }
            .distinct()
    }

    // ── GDPR right to access: data export queries (spec §16) ────────────────

    suspend fun listIssuesByBorrower(schoolId: UUID, borrowerId: UUID): List<LibraryIssueRow> = dbQuery {
        LibraryIssuesTable.selectAll()
            .where {
                (LibraryIssuesTable.schoolId eq schoolId) and
                (LibraryIssuesTable.borrowerId eq borrowerId)
            }
            .orderBy(LibraryIssuesTable.issueDate to SortOrder.DESC)
            .map { it.toIssueRow() }
    }

    suspend fun listReservationsByBorrower(schoolId: UUID, borrowerId: UUID): List<LibraryReservationRow> = dbQuery {
        LibraryReservationsTable.selectAll()
            .where {
                (LibraryReservationsTable.schoolId eq schoolId) and
                (LibraryReservationsTable.reservedBy eq borrowerId)
            }
            .orderBy(LibraryReservationsTable.createdAt to SortOrder.DESC)
            .map { it.toReservationRow() }
    }

    suspend fun listWishlistByBorrower(schoolId: UUID, borrowerId: UUID): List<LibraryWishlistRow> = dbQuery {
        LibraryWishlistTable.selectAll()
            .where {
                (LibraryWishlistTable.schoolId eq schoolId) and
                (LibraryWishlistTable.studentId eq borrowerId)
            }
            .orderBy(LibraryWishlistTable.createdAt to SortOrder.DESC)
            .map { it.toWishlistRow() }
    }

    suspend fun listReadingGoalsByBorrower(schoolId: UUID, borrowerId: UUID): List<LibraryReadingGoalRow> = dbQuery {
        LibraryReadingGoalsTable.selectAll()
            .where {
                (LibraryReadingGoalsTable.schoolId eq schoolId) and
                (LibraryReadingGoalsTable.studentId eq borrowerId)
            }
            .orderBy(LibraryReadingGoalsTable.targetYear to SortOrder.DESC)
            .map { it.toReadingGoalRow() }
    }

    suspend fun listBadgesByBorrower(schoolId: UUID, borrowerId: UUID): List<LibraryBadgeRow> = dbQuery {
        LibraryReadingBadgesTable.selectAll()
            .where {
                (LibraryReadingBadgesTable.schoolId eq schoolId) and
                (LibraryReadingBadgesTable.studentId eq borrowerId)
            }
            .orderBy(LibraryReadingBadgesTable.earnedAt to SortOrder.DESC)
            .map { it.toBadgeRow() }
    }

    suspend fun listDiscussionsByBorrower(schoolId: UUID, borrowerId: UUID): List<LibraryDiscussionRow> = dbQuery {
        LibraryBookDiscussionsTable.selectAll()
            .where {
                (LibraryBookDiscussionsTable.schoolId eq schoolId) and
                (LibraryBookDiscussionsTable.studentId eq borrowerId) and
                (LibraryBookDiscussionsTable.deletedAt.isNull())
            }
            .orderBy(LibraryBookDiscussionsTable.createdAt to SortOrder.DESC)
            .map { it.toDiscussionRow() }
    }

    // ── Row mappers ─────────────────────────────────────────────────────────

    private fun ResultRow.toBookRow() = LibraryBookRow(
        id = this[LibraryBooksTable.id].value,
        schoolId = this[LibraryBooksTable.schoolId],
        isbn = this[LibraryBooksTable.isbn],
        title = this[LibraryBooksTable.title],
        author = this[LibraryBooksTable.author],
        publisher = this[LibraryBooksTable.publisher],
        category = this[LibraryBooksTable.category],
        tags = this[LibraryBooksTable.tags],
        totalCopies = this[LibraryBooksTable.totalCopies],
        availableCopies = this[LibraryBooksTable.availableCopies],
        shelfLocation = this[LibraryBooksTable.shelfLocation],
        coverUrl = this[LibraryBooksTable.coverUrl],
        replacementCost = this[LibraryBooksTable.replacementCost],
        seriesName = this[LibraryBooksTable.seriesName],
        seriesNumber = this[LibraryBooksTable.seriesNumber],
        language = this[LibraryBooksTable.language],
        isArchived = this[LibraryBooksTable.isArchived],
        synopsis = this[LibraryBooksTable.synopsis],
        pageCount = this[LibraryBooksTable.pageCount],
        deletedAt = this[LibraryBooksTable.deletedAt],
        createdAt = this[LibraryBooksTable.createdAt],
        updatedAt = this[LibraryBooksTable.updatedAt],
    )

    private fun ResultRow.toCopyRow() = LibraryCopyRow(
        id = this[LibraryBookCopiesTable.id].value,
        schoolId = this[LibraryBookCopiesTable.schoolId],
        bookId = this[LibraryBookCopiesTable.bookId],
        copyNumber = this[LibraryBookCopiesTable.copyNumber],
        barcode = this[LibraryBookCopiesTable.barcode],
        condition = this[LibraryBookCopiesTable.condition],
        status = this[LibraryBookCopiesTable.status],
    )

    private fun ResultRow.toIssueRow() = LibraryIssueRow(
        id = this[LibraryIssuesTable.id].value,
        schoolId = this[LibraryIssuesTable.schoolId],
        bookId = this[LibraryIssuesTable.bookId],
        copyId = this[LibraryIssuesTable.copyId],
        borrowerId = this[LibraryIssuesTable.borrowerId],
        borrowerType = this[LibraryIssuesTable.borrowerType],
        borrowerName = this[LibraryIssuesTable.borrowerName],
        issueDate = this[LibraryIssuesTable.issueDate],
        dueDate = this[LibraryIssuesTable.dueDate],
        returnDate = this[LibraryIssuesTable.returnDate],
        returnCondition = this[LibraryIssuesTable.returnCondition],
        damageNotes = this[LibraryIssuesTable.damageNotes],
        renewalCount = this[LibraryIssuesTable.renewalCount],
        fineAmount = this[LibraryIssuesTable.fineAmount],
        fineStatus = this[LibraryIssuesTable.fineStatus],
        finePaidAt = this[LibraryIssuesTable.finePaidAt],
        fineWaivedBy = this[LibraryIssuesTable.fineWaivedBy],
        fineWaivedReason = this[LibraryIssuesTable.fineWaivedReason],
        status = this[LibraryIssuesTable.status],
        createdAt = this[LibraryIssuesTable.createdAt],
        updatedAt = this[LibraryIssuesTable.updatedAt],
    )

    private fun ResultRow.toReservationRow() = LibraryReservationRow(
        id = this[LibraryReservationsTable.id].value,
        schoolId = this[LibraryReservationsTable.schoolId],
        bookId = this[LibraryReservationsTable.bookId],
        reservedBy = this[LibraryReservationsTable.reservedBy],
        reservedByName = this[LibraryReservationsTable.reservedByName],
        reservedByType = this[LibraryReservationsTable.reservedByType],
        status = this[LibraryReservationsTable.status],
        createdAt = this[LibraryReservationsTable.createdAt],
        fulfilledAt = this[LibraryReservationsTable.fulfilledAt],
    )

    private fun ResultRow.toCategoryRow() = LibraryCategoryRow(
        id = this[LibraryCategoriesTable.id].value,
        schoolId = this[LibraryCategoriesTable.schoolId],
        name = this[LibraryCategoriesTable.name],
        color = this[LibraryCategoriesTable.color],
        icon = this[LibraryCategoriesTable.icon],
        displayOrder = this[LibraryCategoriesTable.displayOrder],
    )

    private fun ResultRow.toSettingsRow() = LibrarySettingsRow(
        id = this[LibrarySettingsTable.id].value,
        schoolId = this[LibrarySettingsTable.schoolId],
        defaultLoanDays = this[LibrarySettingsTable.defaultLoanDays],
        finePerDay = this[LibrarySettingsTable.finePerDay],
        maxBooksPerStudent = this[LibrarySettingsTable.maxBooksPerStudent],
        maxRenewals = this[LibrarySettingsTable.maxRenewals],
        reservationTimeoutDays = this[LibrarySettingsTable.reservationTimeoutDays],
        dueReminderDays = this[LibrarySettingsTable.dueReminderDays],
        fineCapEnabled = this[LibrarySettingsTable.fineCapEnabled],
        quickIssueEnabled = this[LibrarySettingsTable.quickIssueEnabled],
        bulkReturnEnabled = this[LibrarySettingsTable.bulkReturnEnabled],
        featuredBookId = this[LibrarySettingsTable.featuredBookId],
        featuredType = this[LibrarySettingsTable.featuredType],
        featuredUpdatedAt = this[LibrarySettingsTable.featuredUpdatedAt],
        leaderboardEnabled = this[LibrarySettingsTable.leaderboardEnabled],
    )

    private fun ResultRow.toAuditLogRow() = LibraryAuditLogRow(
        id = this[LibraryAuditLogTable.id].value,
        schoolId = this[LibraryAuditLogTable.schoolId],
        actorId = this[LibraryAuditLogTable.actorId],
        actorName = this[LibraryAuditLogTable.actorName],
        action = this[LibraryAuditLogTable.action],
        entityType = this[LibraryAuditLogTable.entityType],
        entityId = this[LibraryAuditLogTable.entityId],
        metadata = this[LibraryAuditLogTable.metadata],
        previousState = this[LibraryAuditLogTable.previousState],
        newState = this[LibraryAuditLogTable.newState],
        hash = this[LibraryAuditLogTable.hash],
        createdAt = this[LibraryAuditLogTable.createdAt],
    )

    private fun ResultRow.toAnnouncementRow() = LibraryAnnouncementRow(
        id = this[LibraryAnnouncementsTable.id].value,
        schoolId = this[LibraryAnnouncementsTable.schoolId],
        title = this[LibraryAnnouncementsTable.title],
        message = this[LibraryAnnouncementsTable.message],
        audience = this[LibraryAnnouncementsTable.audience],
        createdBy = this[LibraryAnnouncementsTable.createdBy],
        createdByName = this[LibraryAnnouncementsTable.createdByName],
        expiresAt = this[LibraryAnnouncementsTable.expiresAt],
        isActive = this[LibraryAnnouncementsTable.isActive],
        createdAt = this[LibraryAnnouncementsTable.createdAt],
        updatedAt = this[LibraryAnnouncementsTable.updatedAt],
    )

    private fun ResultRow.toWishlistRow() = LibraryWishlistRow(
        id = this[LibraryWishlistTable.id].value,
        schoolId = this[LibraryWishlistTable.schoolId],
        studentId = this[LibraryWishlistTable.studentId],
        bookId = this[LibraryWishlistTable.bookId],
        createdAt = this[LibraryWishlistTable.createdAt],
    )

    private fun ResultRow.toReadingGoalRow() = LibraryReadingGoalRow(
        id = this[LibraryReadingGoalsTable.id].value,
        schoolId = this[LibraryReadingGoalsTable.schoolId],
        studentId = this[LibraryReadingGoalsTable.studentId],
        goalCount = this[LibraryReadingGoalsTable.goalCount],
        period = this[LibraryReadingGoalsTable.period],
        targetYear = this[LibraryReadingGoalsTable.targetYear],
        createdAt = this[LibraryReadingGoalsTable.createdAt],
        updatedAt = this[LibraryReadingGoalsTable.updatedAt],
    )

    private fun ResultRow.toAcquisitionRow() = LibraryAcquisitionRow(
        id = this[LibraryAcquisitionRequestsTable.id].value,
        schoolId = this[LibraryAcquisitionRequestsTable.schoolId],
        requestedBy = this[LibraryAcquisitionRequestsTable.requestedBy],
        requestedByName = this[LibraryAcquisitionRequestsTable.requestedByName],
        requestedByType = this[LibraryAcquisitionRequestsTable.requestedByType],
        title = this[LibraryAcquisitionRequestsTable.title],
        author = this[LibraryAcquisitionRequestsTable.author],
        isbn = this[LibraryAcquisitionRequestsTable.isbn],
        publisher = this[LibraryAcquisitionRequestsTable.publisher],
        reason = this[LibraryAcquisitionRequestsTable.reason],
        estimatedCost = this[LibraryAcquisitionRequestsTable.estimatedCost],
        status = this[LibraryAcquisitionRequestsTable.status],
        approvedBy = this[LibraryAcquisitionRequestsTable.approvedBy],
        approvedAt = this[LibraryAcquisitionRequestsTable.approvedAt],
        orderLink = this[LibraryAcquisitionRequestsTable.orderLink],
        orderedAt = this[LibraryAcquisitionRequestsTable.orderedAt],
        receivedAt = this[LibraryAcquisitionRequestsTable.receivedAt],
        convertedBookId = this[LibraryAcquisitionRequestsTable.convertedBookId],
        createdAt = this[LibraryAcquisitionRequestsTable.createdAt],
        updatedAt = this[LibraryAcquisitionRequestsTable.updatedAt],
    )

    private fun ResultRow.toBadgeRow() = LibraryBadgeRow(
        id = this[LibraryReadingBadgesTable.id].value,
        schoolId = this[LibraryReadingBadgesTable.schoolId],
        studentId = this[LibraryReadingBadgesTable.studentId],
        badgeType = this[LibraryReadingBadgesTable.badgeType],
        earnedAt = this[LibraryReadingBadgesTable.earnedAt],
    )

    // ── GDPR: Right to be forgotten ──────────────────────────────────────────
    // Spec §16: "When student is deactivated, library data is anonymized
    //   (borrower_name → 'Deactivated Student'); reading history retained for
    //   school records but PII removed."

    suspend fun anonymizeBorrower(borrowerId: UUID): Int = dbQuery {
        val placeholder = "Deactivated Student"
        var count = 0
        count += LibraryIssuesTable.update({ LibraryIssuesTable.borrowerId eq borrowerId }) {
            it[LibraryIssuesTable.borrowerName] = placeholder
        }
        count += LibraryReservationsTable.update({ LibraryReservationsTable.reservedBy eq borrowerId }) {
            it[LibraryReservationsTable.reservedByName] = placeholder
        }
        count += LibraryAcquisitionRequestsTable.update({ LibraryAcquisitionRequestsTable.requestedBy eq borrowerId }) {
            it[LibraryAcquisitionRequestsTable.requestedByName] = placeholder
        }
        count += LibraryBookDiscussionsTable.update({ LibraryBookDiscussionsTable.studentId eq borrowerId }) {
            it[LibraryBookDiscussionsTable.studentName] = placeholder
        }
        count
    }

    private fun ResultRow.toDiscussionRow() = LibraryDiscussionRow(
        id = this[LibraryBookDiscussionsTable.id].value,
        schoolId = this[LibraryBookDiscussionsTable.schoolId],
        bookId = this[LibraryBookDiscussionsTable.bookId],
        studentId = this[LibraryBookDiscussionsTable.studentId],
        studentName = this[LibraryBookDiscussionsTable.studentName],
        message = this[LibraryBookDiscussionsTable.message],
        createdAt = this[LibraryBookDiscussionsTable.createdAt],
        deletedAt = this[LibraryBookDiscussionsTable.deletedAt],
        deletedBy = this[LibraryBookDiscussionsTable.deletedBy],
    )
}
