/*
 * File: LibraryCacheKeys.kt
 * Module: feature.library
 *
 * Centralized cache key management for the library feature.
 * Per spec §8.14, 8 cache key patterns are defined with TTLs.
 *
 * Cache invalidation is event-driven via LibraryEventBus —
 * subscribe to domain events and call the appropriate invalidate* methods.
 *
 * Usage:
 *   val key = LibraryCacheKeys.bookSearch(schoolId, query, category, page, tags)
 *   cache.getOrPut(key, ttlMinutes = 5) { service.searchBooks(...) }
 *
 *   LibraryEventBus.subscribe { event ->
 *       when (event) {
 *           is BookCreated, is BookUpdated, is BookDeleted ->
 *               LibraryCacheKeys.invalidateBookSearch(event.schoolId)
 *           is SettingsUpdated ->
 *               LibraryCacheKeys.invalidateSettings(event.schoolId)
 *           ...
 *       }
 *   }
 */
package com.littlebridge.enrollplus.feature.library

import java.util.UUID

object LibraryCacheKeys {

    private const val PREFIX = "library"

    // ── Key patterns (spec §8.14) ───────────────────────────────────────────

    /**
     * Book catalog search results. Cached for 5 minutes.
     * Keyed by: schoolId + query + category + page + tags + sortBy + availability + language
     */
    fun bookSearch(
        schoolId: UUID,
        query: String,
        category: String?,
        language: String?,
        tags: List<String>?,
        sortBy: String,
        availability: String,
        page: Int,
        limit: Int,
    ): String = buildString {
        append("$PREFIX:search:$schoolId")
        append(":q=${query.take(200).lowercase().trim()}")
        append(":cat=${category ?: "all"}")
        append(":lang=${language ?: "all"}")
        append(":tags=${tags?.sorted()?.joinToString(",") ?: "none"}")
        append(":sort=$sortBy")
        append(":avail=$availability")
        append(":p=$page")
        append(":l=$limit")
    }

    const val SEARCH_TTL_MINUTES = 5

    /**
     * Dashboard counts. Cached for 1 minute.
     * Keyed by: schoolId
     */
    fun dashboard(schoolId: UUID): String = "$PREFIX:dashboard:$schoolId"

    const val DASHBOARD_TTL_MINUTES = 1

    /**
     * Outstanding fines summary. Cached for 2 minutes.
     * Keyed by: schoolId
     */
    fun outstandingFines(schoolId: UUID): String = "$PREFIX:fines:$schoolId"

    const val FINES_TTL_MINUTES = 2

    /**
     * Trending books. Cached for 1 hour.
     * Keyed by: schoolId
     */
    fun trending(schoolId: UUID): String = "$PREFIX:trending:$schoolId"

    const val TRENDING_TTL_MINUTES = 60

    /**
     * Library settings. Cached for 10 minutes.
     * Keyed by: schoolId. Invalidated on update.
     */
    fun settings(schoolId: UUID): String = "$PREFIX:settings:$schoolId"

    const val SETTINGS_TTL_MINUTES = 10

    /**
     * Categories. Cached for 30 minutes.
     * Keyed by: schoolId. Invalidated on create/update/delete/reorder.
     */
    fun categories(schoolId: UUID): String = "$PREFIX:categories:$schoolId"

    const val CATEGORIES_TTL_MINUTES = 30

    /**
     * Active announcements. Cached for 5 minutes.
     * Keyed by: schoolId
     */
    fun activeAnnouncements(schoolId: UUID): String = "$PREFIX:announcements:$schoolId"

    const val ANNOUNCEMENTS_TTL_MINUTES = 5

    /**
     * Student library profile. Cached for 2 minutes.
     * Keyed by: studentId
     */
    fun studentProfile(schoolId: UUID, studentId: UUID): String = "$PREFIX:profile:$schoolId:$studentId"

    const val PROFILE_TTL_MINUTES = 2

    // ── Invalidation helpers ────────────────────────────────────────────────

    /**
     * Invalidate all cached search results for a school.
     * Call on: BookCreated, BookUpdated, BookDeleted, BookArchived, BookUnarchived, CopyAdded, CopyRepaired
     */
    fun invalidateBookSearch(schoolId: UUID): String = "$PREFIX:search:$schoolId:*"

    /**
     * Invalidate dashboard cache for a school.
     * Call on: BookCreated, BookDeleted, BookIssued, BookReturned, BookMarkedLost, CopyAdded
     */
    fun invalidateDashboard(schoolId: UUID): String = "$PREFIX:dashboard:$schoolId"

    /**
     * Invalidate outstanding fines cache for a school.
     * Call on: FinePaid, FineWaived, BookReturned, BookMarkedLost
     */
    fun invalidateFines(schoolId: UUID): String = "$PREFIX:fines:$schoolId"

    /**
     * Invalidate trending cache for a school.
     * Call on: BookIssued, BookReturned
     */
    fun invalidateTrending(schoolId: UUID): String = "$PREFIX:trending:$schoolId"

    /**
     * Invalidate settings cache for a school.
     * Call on: SettingsUpdated
     */
    fun invalidateSettings(schoolId: UUID): String = "$PREFIX:settings:$schoolId"

    /**
     * Invalidate categories cache for a school.
     * Call on: CategoryCreated, CategoryUpdated, CategoryDeleted
     */
    fun invalidateCategories(schoolId: UUID): String = "$PREFIX:categories:$schoolId"

    /**
     * Invalidate announcements cache for a school.
     * Call on: AnnouncementCreated, AnnouncementExpired
     */
    fun invalidateAnnouncements(schoolId: UUID): String = "$PREFIX:announcements:$schoolId"

    /**
     * Invalidate student profile cache.
     * Call on: BookReturned, BadgeAwarded, ReadingGoalSet
     */
    fun invalidateStudentProfile(schoolId: UUID, studentId: UUID): String = "$PREFIX:profile:$schoolId:$studentId"

    /**
     * Invalidate all library caches for a school (e.g. on onboarding reset).
     */
    fun invalidateAll(schoolId: UUID): String = "$PREFIX:*:$schoolId*"
}
