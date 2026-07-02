package com.littlebridge.enrollplus.feature.library

import com.littlebridge.enrollplus.db.*
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Integration tests for the library lifecycle: book creation → copy
 * auto-generation → issue → return → availability increment, and the
 * overdue → fine → pay/waive flow.
 *
 * Uses an in-memory SQLite database so no external Postgres is required.
 */
class LibraryIntegrationTest {

    private val schoolId = UUID.randomUUID()
    private val adminId = UUID.randomUUID()
    private val studentId = UUID.randomUUID()
    private lateinit var repo: LibraryRepository
    private lateinit var service: LibraryService

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:sqlite::memory:", "org.sqlite.JDBC")
        transaction {
            SchemaUtils.createMissingTablesAndColumns(
                LibraryBooksTable,
                LibraryBookCopiesTable,
                LibraryIssuesTable,
                LibraryReservationsTable,
                LibraryCategoriesTable,
                LibrarySettingsTable,
                LibraryAuditLogTable,
                LibraryAnnouncementsTable,
                LibraryWishlistTable,
                LibraryReadingGoalsTable,
                LibraryAcquisitionRequestsTable,
                LibraryReadingBadgesTable,
                LibraryBookDiscussionsTable,
            )
        }
        repo = LibraryRepository()
        service = LibraryService(repo = repo)
    }

    @AfterTest
    fun teardown() {
        transaction {
            SchemaUtils.drop(
                LibraryBookDiscussionsTable,
                LibraryReadingBadgesTable,
                LibraryAcquisitionRequestsTable,
                LibraryReadingGoalsTable,
                LibraryWishlistTable,
                LibraryAnnouncementsTable,
                LibraryAuditLogTable,
                LibrarySettingsTable,
                LibraryCategoriesTable,
                LibraryReservationsTable,
                LibraryIssuesTable,
                LibraryBookCopiesTable,
                LibraryBooksTable,
            )
        }
    }

    // ── Full lifecycle: create → copies auto-generated → issue → return ──────

    @Test
    fun `create book auto-generates copies and issue-return lifecycle works`() = runBlocking {
        // 1. Create book with 3 copies
        val bookId = service.createBook(
            schoolId,
            CreateBookRequest(
                title = "Integration Test Book",
                author = "Test Author",
                isbn = "9780306406157",
                totalCopies = 3,
                category = "Science",
                replacementCost = 50.0,
            ),
            adminId,
            "Admin",
        )

        // 2. Verify copies were auto-generated
        val book = repo.findBookById(schoolId, bookId)
        assertNotNull(book)
        assertEquals(3, book.totalCopies)
        assertEquals(3, book.availableCopies)

        val copies = repo.listCopiesForBook(schoolId, bookId)
        assertEquals(3, copies.size)
        assertEquals("available", copies[0].status)

        // 3. Issue the book
        val issue = service.issueBook(
            schoolId,
            IssueBookRequest(bookId = bookId.toString(), borrowerId = studentId.toString(), borrowerType = "student", borrowerName = "Test Student"),
            adminId,
            "Admin",
        )
        assertEquals("issued", issue.status)

        // 4. Verify availability decremented
        val bookAfterIssue = repo.findBookById(schoolId, bookId)
        assertNotNull(bookAfterIssue)
        assertEquals(2, bookAfterIssue.availableCopies)

        // 5. Return the book
        service.returnBook(
            schoolId,
            ReturnBookRequest(issueId = issue.id),
            adminId,
            "Admin",
        )

        // 6. Verify availability incremented back
        val bookAfterReturn = repo.findBookById(schoolId, bookId)
        assertNotNull(bookAfterReturn)
        assertEquals(3, bookAfterReturn.availableCopies)

        // 7. Verify issue status is returned
        val issueAfter = repo.findIssueById(schoolId, UUID.fromString(issue.id))
        assertNotNull(issueAfter)
        assertEquals("returned", issueAfter.status)
    }

    // ── Overdue → fine → pay fine flow ───────────────────────────────────────

    @Test
    fun `overdue return generates fine and pay fine clears it`() = runBlocking {
        val bookId = service.createBook(
            schoolId,
            CreateBookRequest(
                title = "Overdue Test",
                totalCopies = 1,
                replacementCost = 500.0,
            ),
            adminId,
            "Admin",
        )

        // Issue with a past due date by manipulating the issue directly
        val issueId = repo.createIssue(
            schoolId, bookId, null, studentId, "student", "Student",
            LocalDate.now().minusDays(20),
            LocalDate.now().minusDays(10),
        )
        repo.updateBookAvailability(schoolId, bookId, -1)

        // Return — should generate a fine
        val result = service.returnBook(
            schoolId,
            ReturnBookRequest(issueId = issueId.toString()),
            adminId,
            "Admin",
        )
        assertTrue(result.fineAmount > 0)

        // Verify fine is pending in the DB
        val issueAfterReturn = repo.findIssueById(schoolId, issueId)
        assertNotNull(issueAfterReturn)
        assertEquals("pending", issueAfterReturn.fineStatus)

        // Pay the fine
        service.payFine(schoolId, issueId, adminId, "Admin")

        // Verify fine is paid
        val issue = repo.findIssueById(schoolId, issueId)
        assertNotNull(issue)
        assertEquals("paid", issue.fineStatus)
    }

    // ── Overdue → fine → waive fine flow ─────────────────────────────────────

    @Test
    fun `overdue return generates fine and waive fine clears it`() = runBlocking {
        val bookId = service.createBook(
            schoolId,
            CreateBookRequest(
                title = "Waive Test",
                totalCopies = 1,
                replacementCost = 500.0,
            ),
            adminId,
            "Admin",
        )

        val issueId = repo.createIssue(
            schoolId, bookId, null, studentId, "student", "Student",
            LocalDate.now().minusDays(20),
            LocalDate.now().minusDays(10),
        )
        repo.updateBookAvailability(schoolId, bookId, -1)

        val result = service.returnBook(
            schoolId,
            ReturnBookRequest(issueId = issueId.toString()),
            adminId,
            "Admin",
        )
        assertTrue(result.fineAmount > 0)

        // Waive the fine
        service.waiveFine(schoolId, issueId, "Damaged book, fine waived", adminId, "Admin")

        val issue = repo.findIssueById(schoolId, issueId)
        assertNotNull(issue)
        assertEquals("waived", issue.fineStatus)
        assertNotNull(issue.fineWaivedBy)
        assertEquals(adminId, issue.fineWaivedBy)
    }

    // ── Renewal flow: renew → renew → max reached ────────────────────────────

    @Test
    fun `renewal flow enforces max renewals`() = runBlocking {
        val bookId = service.createBook(
            schoolId,
            CreateBookRequest(title = "Renewal Test", totalCopies = 1),
            adminId,
            "Admin",
        )

        val issue = service.issueBook(
            schoolId,
            IssueBookRequest(bookId = bookId.toString(), borrowerId = studentId.toString(), borrowerType = "student", borrowerName = "Test Student"),
            adminId,
            "Admin",
        )

        // First renewal
        service.renewBook(schoolId, UUID.fromString(issue.id), adminId, "Admin")
        val after1 = repo.findIssueById(schoolId, UUID.fromString(issue.id))
        assertNotNull(after1)
        assertEquals(1, after1.renewalCount)

        // Second renewal
        service.renewBook(schoolId, UUID.fromString(issue.id), adminId, "Admin")
        val after2 = repo.findIssueById(schoolId, UUID.fromString(issue.id))
        assertNotNull(after2)
        assertEquals(2, after2.renewalCount)

        // Third renewal should fail (max is 2 by default)
        assertFailsWith<LibraryConflictException> {
            service.renewBook(schoolId, UUID.fromString(issue.id), adminId, "Admin")
        }
    }

    // ── Reservation flow: issue → return → reservation notified ──────────────

    @Test
    fun `reservation is created when book is unavailable and fulfilled on return`() = runBlocking {
        val bookId = service.createBook(
            schoolId,
            CreateBookRequest(title = "Reservation Test", totalCopies = 1),
            adminId,
            "Admin",
        )

        // Issue the only copy
        val issue = service.issueBook(
            schoolId,
            IssueBookRequest(bookId = bookId.toString(), borrowerId = studentId.toString(), borrowerType = "student", borrowerName = "Test Student"),
            adminId,
            "Admin",
        )

        // Try to reserve — should succeed since book is unavailable
        val reservation = service.reserveBook(schoolId, bookId, adminId, "Admin", "teacher")
        assertEquals("pending", reservation.status)

        // Return the book
        service.returnBook(
            schoolId,
            ReturnBookRequest(issueId = issue.id),
            adminId,
            "Admin",
        )

        // Fulfill the reservation
        service.fulfillReservation(schoolId, UUID.fromString(reservation.id), adminId, "Admin")

        // Verify reservation is fulfilled
        val reservations = service.listReservationsForUser(schoolId, adminId)
        assertTrue(reservations.any { it.id == reservation.id && it.status == "fulfilled" })
    }

    // ── Lost book flow ───────────────────────────────────────────────────────

    @Test
    fun `mark lost generates fine equal to replacement cost`() = runBlocking {
        val bookId = service.createBook(
            schoolId,
            CreateBookRequest(
                title = "Lost Book Test",
                totalCopies = 1,
                replacementCost = 75.0,
            ),
            adminId,
            "Admin",
        )

        val issue = service.issueBook(
            schoolId,
            IssueBookRequest(bookId = bookId.toString(), borrowerId = studentId.toString(), borrowerType = "student", borrowerName = "Test Student"),
            adminId,
            "Admin",
        )

        val result = service.markLost(schoolId, UUID.fromString(issue.id), adminId, "Admin")
        assertEquals(75.0, result.fineAmount)

        val issueAfter = repo.findIssueById(schoolId, UUID.fromString(issue.id))
        assertNotNull(issueAfter)
        assertEquals("lost", issueAfter.status)
        assertEquals("pending", issueAfter.fineStatus)
    }

    // ── Damage repair flow ───────────────────────────────────────────────────

    @Test
    fun `return damaged puts copy in repair and mark available allows re-issue`() = runBlocking {
        val bookId = service.createBook(
            schoolId,
            CreateBookRequest(title = "Damage Test", totalCopies = 1),
            adminId,
            "Admin",
        )

        val issue = service.issueBook(
            schoolId,
            IssueBookRequest(bookId = bookId.toString(), borrowerId = studentId.toString(), borrowerType = "student", borrowerName = "Test Student"),
            adminId,
            "Admin",
        )

        // Return damaged
        service.returnBook(
            schoolId,
            ReturnBookRequest(
                issueId = issue.id,
                returnCondition = "damaged",
                damageNotes = "Torn pages",
            ),
            adminId,
            "Admin",
        )

        // Copy should be in repair status
        val copies = repo.listCopiesForBook(schoolId, bookId)
        val copy = copies.firstOrNull { it.status == "repair" }
        assertNotNull(copy, "Copy should be in repair status")

        // Mark as available
        repo.updateCopyStatus(schoolId, copy.id, "available")

        // Should be able to issue again
        val reIssue = service.issueBook(
            schoolId,
            IssueBookRequest(bookId = bookId.toString(), borrowerId = studentId.toString(), borrowerType = "student", borrowerName = "Test Student"),
            adminId,
            "Admin",
        )
        assertEquals("issued", reIssue.status)
    }
