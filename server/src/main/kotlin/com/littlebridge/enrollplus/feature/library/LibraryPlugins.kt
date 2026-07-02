/*
 * File: LibraryPlugins.kt
 * Module: feature.library
 *
 * Plugin interfaces for the library feature with default implementations.
 * These allow swapping out ISBN validation, barcode generation, fine
 * calculation, and due date calculation without modifying the service.
 */
package com.littlebridge.enrollplus.feature.library

import java.time.LocalDate
import java.time.temporal.ChronoUnit

// ── ISBN Validator ──────────────────────────────────────────────────────────

interface IsbnValidator {
    fun validate(isbn: String): Boolean
    fun normalize(isbn: String): String
}

class DefaultIsbnValidator : IsbnValidator {
    override fun validate(isbn: String): Boolean {
        val cleaned = isbn.replace(Regex("[-\\s]"), "")
        return cleaned.length == 10 && cleaned.all { it.isDigit() } ||
               (cleaned.length == 13 && cleaned.all { it.isDigit() })
    }

    override fun normalize(isbn: String): String =
        isbn.replace(Regex("[-\\s]"), "")
}

// ── Barcode Generator ───────────────────────────────────────────────────────

interface BarcodeGenerator {
    fun generate(schoolId: java.util.UUID, bookId: java.util.UUID, copyNumber: Int): String
}

class DefaultBarcodeGenerator : BarcodeGenerator {
    override fun generate(schoolId: java.util.UUID, bookId: java.util.UUID, copyNumber: Int): String {
        return "LIB-${schoolId.toString().take(8)}-${copyNumber.toString().padStart(3, '0')}"
    }
}

// ── Fine Calculator ─────────────────────────────────────────────────────────

interface FineCalculator {
    fun calculate(dueDate: LocalDate, returnDate: LocalDate, finePerDay: Double, capEnabled: Boolean, replacementCost: Double? = null): Double
}

class DefaultFineCalculator : FineCalculator {
    companion object {
        const val FINE_CAP = 500.0
    }

    override fun calculate(dueDate: LocalDate, returnDate: LocalDate, finePerDay: Double, capEnabled: Boolean, replacementCost: Double?): Double {
        val daysOverdue = ChronoUnit.DAYS.between(dueDate, returnDate).coerceAtLeast(0).toInt()
        if (daysOverdue == 0) return 0.0
        val raw = daysOverdue * finePerDay
        if (!capEnabled) return raw
        val cap = replacementCost ?: FINE_CAP
        return raw.coerceAtMost(cap)
    }
}

// ── Due Date Calculator ─────────────────────────────────────────────────────

interface DueDateCalculator {
    fun calculate(issueDate: LocalDate, loanDays: Int): LocalDate
}

class DefaultDueDateCalculator : DueDateCalculator {
    override fun calculate(issueDate: LocalDate, loanDays: Int): LocalDate {
        return issueDate.plusDays(loanDays.toLong())
    }
}

/**
 * Holiday-aware due date calculator: extends the due date past any
 * holidays or Sundays recorded in academic_calendar for the school.
 *
 * Usage: LibraryService can be constructed with this instead of
 * DefaultDueDateCalculator when holiday awareness is desired.
 */
class HolidayAwareDueDateCalculator(
    private val schoolId: java.util.UUID,
    private val holidayDates: Set<LocalDate>,
) : DueDateCalculator {
    override fun calculate(issueDate: LocalDate, loanDays: Int): LocalDate {
        var dueDate = issueDate.plusDays(loanDays.toLong())
        // Push the due date forward past any holidays or Sundays
        while (dueDate.dayOfWeek.value == 7 || dueDate in holidayDates) {
            dueDate = dueDate.plusDays(1)
        }
        return dueDate
    }
}

// ── Feature Flag Service ────────────────────────────────────────────────────

interface FeatureFlagService {
    fun isEnabled(flag: String): Boolean
}

class DefaultFeatureFlagService : FeatureFlagService {
    override fun isEnabled(flag: String): Boolean = when (flag) {
        "library_enabled" -> true
        "library.fines" -> true
        "library.reservations" -> true
        "library.wishlist" -> true
        "library.reading_goals" -> true
        "library.acquisition_requests" -> true
        "library.badges" -> true
        "library.discussions" -> true
        "library.leaderboard" -> true
        "library.featured_book" -> true
        "library.trending" -> true
        "library.quick_issue" -> true
        "library.bulk_return" -> true
        else -> false
    }
}

// ── Search Provider ──────────────────────────────────────────────────────────

/**
 * Filters for book search. All fields optional — null means no filter.
 */
data class SearchFilters(
    val category: String? = null,
    val language: String? = null,
    val tags: List<String>? = null,
    val sortBy: String = "newest",
    val availability: String = "all",
)

/**
 * Pluggable search provider for the library catalog.
 * Default implementation uses Postgres ILIKE (works on SQLite too).
 * Can be swapped for a full-text search provider (to_tsvector) or
 * Elasticsearch for large catalogs (> 50,000 books).
 */
interface SearchProvider {
    suspend fun search(
        schoolId: java.util.UUID,
        query: String,
        filters: SearchFilters,
        page: Int,
        limit: Int,
    ): Pair<List<LibraryBookRow>, Int>
}

/**
 * Default search provider — delegates to LibraryRepository.searchBooks.
 * Uses ILIKE for broad compatibility (SQLite + Postgres).
 * For Postgres deployments with GIN indexes, swap with FullTextSearchProvider.
 */
class PostgresSearchProvider(
    private val repo: LibraryRepository,
) : SearchProvider {
    override suspend fun search(
        schoolId: java.util.UUID,
        query: String,
        filters: SearchFilters,
        page: Int,
        limit: Int,
    ): Pair<List<LibraryBookRow>, Int> {
        return repo.searchBooks(
            schoolId, query, filters.category, filters.language, filters.tags,
            filters.sortBy, filters.availability, page, limit,
        )
    }
}
