package com.littlebridge.enrollplus.feature.library

import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
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
 * Security tests for library operations.
 *
 * Verifies:
 * 1. School-scoped isolation: books from school A are invisible to school B.
 * 2. Borrower-scoped access: a student cannot view another student's issues.
 */
class LibrarySecurityTest {

    private val schoolA = UUID.randomUUID()
    private val schoolB = UUID.randomUUID()
    private val adminA = UUID.randomUUID()
    private val studentA1 = UUID.randomUUID()
    private val studentA2 = UUID.randomUUID()
    private val studentB = UUID.randomUUID()
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

    // ── School-scoped isolation ──────────────────────────────────────────────

    @Test
    fun `book created in school A is invisible to school B`() = runBlocking {
        val bookId = service.createBook(
            schoolA,
            CreateBookRequest(title = "School A Book", totalCopies = 2),
            adminA,
            "Admin A",
        )

        // School A can see it
        val bookA = repo.findBookById(schoolA, bookId)
        assertNotNull(bookA)
        assertEquals("School A Book", bookA.title)

        // School B cannot see it
        val bookB = repo.findBookById(schoolB, bookId)
        assertNull(bookB)
    }

    @Test
    fun `issue from school B on book created in school A fails`() = runBlocking {
        val bookId = service.createBook(
            schoolA,
            CreateBookRequest(title = "Cross-school Book", totalCopies = 1),
            adminA,
            "Admin A",
        )

        // School B trying to issue school A's book should fail (not found)
        assertFailsWith<LibraryNotFoundException> {
            service.issueBook(
                schoolB,
                IssueBookRequest(
                    bookId = bookId.toString(),
                    borrowerId = studentB.toString(),
                    borrowerType = "student",
                    borrowerName = "Student B",
                ),
                adminA,
                "Admin A",
            )
        }
    }

    @Test
    fun `search in school B does not return books from school A`() = runBlocking {
        service.createBook(
            schoolA,
            CreateBookRequest(title = "Secret School A Book", totalCopies = 1),
            adminA,
            "Admin A",
        )
        service.createBook(
            schoolB,
            CreateBookRequest(title = "School B Book", totalCopies = 1),
            adminA,
            "Admin B",
        )

        val (resultsB, totalB) = repo.searchBooks(
            schoolB, "", null, null, null, "newest", "all", 1, 20,
        )
        assertEquals(1, totalB)
        assertEquals("School B Book", resultsB[0].title)

        val (resultsA, totalA) = repo.searchBooks(
            schoolA, "", null, null, null, "newest", "all", 1, 20,
        )
        assertEquals(1, totalA)
        assertEquals("Secret School A Book", resultsA[0].title)
    }

    // ── Borrower-scoped access ───────────────────────────────────────────────

    @Test
    fun `student cannot view another students issues`() = runBlocking {
        val bookId = service.createBook(
            schoolA,
            CreateBookRequest(title = "Borrower Isolation Test", totalCopies = 2),
            adminA,
            "Admin A",
        )

        // Student 1 borrows the book
        val issue1 = service.issueBook(
            schoolA,
            IssueBookRequest(
                bookId = bookId.toString(),
                borrowerId = studentA1.toString(),
                borrowerType = "student",
                borrowerName = "Student A1",
            ),
            adminA,
            "Admin A",
        )

        // Student 2 borrows the same book
        val issue2 = service.issueBook(
            schoolA,
            IssueBookRequest(
                bookId = bookId.toString(),
                borrowerId = studentA2.toString(),
                borrowerType = "student",
                borrowerName = "Student A2",
            ),
            adminA,
            "Admin A",
        )

        // Student 1 can see their own issue
        val issuesForS1 = repo.listIssuesForBorrower(schoolA, studentA1, null)
        assertEquals(1, issuesForS1.size)
        assertEquals(UUID.fromString(issue1.id), issuesForS1[0].id)

        // Student 2 can see their own issue
        val issuesForS2 = repo.listIssuesForBorrower(schoolA, studentA2, null)
        assertEquals(1, issuesForS2.size)
        assertEquals(UUID.fromString(issue2.id), issuesForS2[0].id)

        // Student 1 cannot see Student 2's issues
        assertTrue(issuesForS1.none { it.id == UUID.fromString(issue2.id) })
    }

    @Test
    fun `waive fine requires valid reason`() = runBlocking {
        val bookId = service.createBook(
            schoolA,
            CreateBookRequest(title = "Waive Security Test", totalCopies = 1, replacementCost = 100.0),
            adminA,
            "Admin A",
        )

        val issueId = repo.createIssue(
            schoolA, bookId, null, studentA1, "student", "Student",
            java.time.LocalDate.now().minusDays(20),
            java.time.LocalDate.now().minusDays(10),
        )
        repo.updateBookAvailability(schoolA, bookId, -1)
        service.returnBook(
            schoolA,
            ReturnBookRequest(issueId = issueId.toString()),
            adminA,
            "Admin A",
        )

        // Short reason should be rejected
        assertFailsWith<LibraryValidationException> {
            service.waiveFine(schoolA, issueId, "no", adminA, "Admin A")
        }

        // Blank reason should be rejected
        assertFailsWith<LibraryValidationException> {
            service.waiveFine(schoolA, issueId, "", adminA, "Admin A")
        }
    }
}
