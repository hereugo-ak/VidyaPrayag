package com.littlebridge.enrollplus.feature.library

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDate
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Performance tests for library operations.
 *
 * Uses in-memory SQLite with a seeded catalog to verify:
 * 1. Search with pagination returns correct results within reasonable time.
 * 2. Concurrent issue requests do not produce negative copy counts.
 */
class LibraryPerformanceTest {

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

    // ── Search pagination with 100 books ─────────────────────────────────────

    @Test
    fun `search with pagination returns correct page from large catalog`() = runBlocking {
        // Seed 100 books
        for (i in 1..100) {
            service.createBook(
                schoolId,
                CreateBookRequest(
                    title = "Science Book $i",
                    author = "Author $i",
                    category = "Science",
                    totalCopies = 1,
                ),
                adminId,
                "Admin",
            )
        }

        val start = System.currentTimeMillis()
        val (results, total) = repo.searchBooks(
            schoolId, "Science", null, null, null, "newest", "all", 1, 20,
        )
        val elapsed = System.currentTimeMillis() - start

        assertEquals(20, results.size)
        assertEquals(100, total)
        // In-memory SQLite should be well under 500ms even for 100 rows.
        assertTrue(elapsed < 5000, "Search took ${elapsed}ms — expected < 5000ms on SQLite")
    }

    @Test
    fun `search second page returns different results`() = runBlocking {
        for (i in 1..50) {
            service.createBook(
                schoolId,
                CreateBookRequest(
                    title = "History Book $i",
                    category = "History",
                    totalCopies = 1,
                ),
                adminId,
                "Admin",
            )
        }

        val (page1, _) = repo.searchBooks(
            schoolId, "History", null, null, null, "newest", "all", 1, 20,
        )
        val (page2, _) = repo.searchBooks(
            schoolId, "History", null, null, null, "newest", "all", 2, 20,
        )

        assertEquals(20, page1.size)
        assertEquals(20, page2.size)
        // No overlap between pages
        val page1Ids = page1.map { it.id }.toSet()
        val page2Ids = page2.map { it.id }.toSet()
        assertTrue(page1Ids.intersect(page2Ids).isEmpty(), "Pages should not overlap")
    }

    // ── Concurrent issue safety ──────────────────────────────────────────────

    @Test
    fun `concurrent issue of same book does not produce negative copies`() = runBlocking {
        val bookId = service.createBook(
            schoolId,
            CreateBookRequest(title = "Concurrent Test", totalCopies = 1),
            adminId,
            "Admin",
        )

        // Launch 5 concurrent issue attempts for the same book (only 1 copy)
        val results = kotlinx.coroutines.coroutineScope {
            (1..5).map {
                async {
                    runCatching {
                        service.issueBook(
                            schoolId,
                            IssueBookRequest(
                                bookId = bookId.toString(),
                                borrowerId = UUID.randomUUID().toString(),
                                borrowerType = "student",
                                borrowerName = "Random Student",
                            ),
                            adminId,
                            "Admin",
                        )
                    }
                }
            }.awaitAll()
        }

        // Only 1 should succeed, the rest should fail with conflict
        val successes = results.filter { it.isSuccess }
        val failures = results.filter { it.isFailure }

        assertEquals(1, successes.size, "Exactly one issue should succeed")
        assertEquals(4, failures.size, "Four issues should fail")

        // Verify available copies is 0, not negative
        val book = repo.findBookById(schoolId, bookId)
        assertNotNull(book)
        assertEquals(0, book.availableCopies)
        assertTrue(book.availableCopies >= 0, "Available copies must never be negative")
    }
}
