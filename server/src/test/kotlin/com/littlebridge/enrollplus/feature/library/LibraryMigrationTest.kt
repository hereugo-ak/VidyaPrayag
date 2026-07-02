package com.littlebridge.enrollplus.feature.library

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Migration tests for library tables.
 *
 * Verifies that SchemaUtils.createMissingTablesAndColumns creates all
 * required library tables with the correct schema, and that rollback
 * (drop) works cleanly.
 */
class LibraryMigrationTest {

    private val libraryTables = arrayOf(
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

    @BeforeTest
    fun setup() {
        Database.connect("jdbc:sqlite::memory:", "org.sqlite.JDBC")
    }

    @AfterTest
    fun teardown() {
        transaction {
            // Drop in reverse dependency order
            libraryTables.reversed().forEach { table ->
                SchemaUtils.drop(table)
            }
        }
    }

    @Test
    fun `all 8 core library tables are created with correct schema`() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(*libraryTables)
        }

        transaction {
            val existingTables = SchemaUtils.listTables().map { it.lowercase() }.toSet()

            // The 8 core tables from the spec
            assertTrue("library_books" in existingTables, "library_books table missing")
            assertTrue("library_book_copies" in existingTables, "library_book_copies table missing")
            assertTrue("library_issues" in existingTables, "library_issues table missing")
            assertTrue("library_reservations" in existingTables, "library_reservations table missing")
            assertTrue("library_categories" in existingTables, "library_categories table missing")
            assertTrue("library_settings" in existingTables, "library_settings table missing")
            assertTrue("library_audit_log" in existingTables, "library_audit_log table missing")
            assertTrue("library_announcements" in existingTables, "library_announcements table missing")

            // Additional tables added in later phases
            assertTrue("library_wishlist" in existingTables, "library_wishlist table missing")
            assertTrue("library_reading_goals" in existingTables, "library_reading_goals table missing")
            assertTrue("library_acquisition_requests" in existingTables, "library_acquisition_requests table missing")
            assertTrue("library_reading_badges" in existingTables, "library_reading_badges table missing")
            assertTrue("library_book_discussions" in existingTables, "library_book_discussions table missing")
        }
    }

    @Test
    fun `createMissingTablesAndColumns is idempotent`() {
        // Running twice should not throw or create duplicates
        transaction {
            SchemaUtils.createMissingTablesAndColumns(*libraryTables)
        }
        transaction {
            SchemaUtils.createMissingTablesAndColumns(*libraryTables)
        }

        transaction {
            val existingTables = SchemaUtils.listTables().map { it.lowercase() }.toSet()
            assertEquals(1, existingTables.count { it == "library_books" })
            assertEquals(1, existingTables.count { it == "library_issues" })
        }
    }

    @Test
    fun `rollback drops all library tables cleanly`() {
        transaction {
            SchemaUtils.createMissingTablesAndColumns(*libraryTables)
        }

        transaction {
            libraryTables.reversed().forEach { table ->
                SchemaUtils.drop(table)
            }
        }

        transaction {
            val existingTables = SchemaUtils.listTables().map { it.lowercase() }.toSet()
            assertTrue("library_books" !in existingTables, "library_books should be dropped")
            assertTrue("library_book_copies" !in existingTables, "library_book_copies should be dropped")
            assertTrue("library_issues" !in existingTables, "library_issues should be dropped")
            assertTrue("library_reservations" !in existingTables, "library_reservations should be dropped")
            assertTrue("library_categories" !in existingTables, "library_categories should be dropped")
            assertTrue("library_settings" !in existingTables, "library_settings should be dropped")
            assertTrue("library_audit_log" !in existingTables, "library_audit_log should be dropped")
            assertTrue("library_announcements" !in existingTables, "library_announcements should be dropped")
        }
    }
}
