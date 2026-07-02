/*
 * File: LibraryCache.kt
 * Module: feature.library
 *
 * In-memory TTL cache with LRU eviction for the library feature.
 * Per spec §17 Caching Layer:
 *   - Cache backend: Redis (or in-memory LRU for single-instance deployments)
 *   - Cache stampede prevention: single-flight pattern
 *   - Event-driven invalidation via LibraryEventBus
 *
 * This is the in-memory LRU implementation. When Redis is configured
 * (REDIS_URL env var), swap with RedisLibraryCache.
 *
 * Usage in LibraryService:
 *   val cached = LibraryCache.getOrPut(key, ttlMinutes = 5) { expensiveQuery() }
 *   LibraryCache.invalidate(key)
 */
package com.littlebridge.enrollplus.feature.library

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object LibraryCache {

    private data class CacheEntry(
        val value: Any?,
        val expiresAt: Long,
        val createdAt: Long,
    )

    private val store = ConcurrentHashMap<String, CacheEntry>()
    private val locks = ConcurrentHashMap<String, Mutex>()
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)

    // LRU tracking — approximate by cleaning expired entries when store exceeds budget
    private const val MAX_ENTRIES = 5_000

    fun get(key: String): Any? {
        val entry = store[key] ?: run { missCount.incrementAndGet(); return null }
        if (System.currentTimeMillis() > entry.expiresAt) {
            store.remove(key)
            missCount.incrementAndGet()
            return null
        }
        hitCount.incrementAndGet()
        return entry.value
    }

    fun put(key: String, value: Any?, ttlMinutes: Int) {
        if (store.size > MAX_ENTRIES) evictExpired()
        store[key] = CacheEntry(
            value = value,
            expiresAt = System.currentTimeMillis() + ttlMinutes * 60_000,
            createdAt = System.currentTimeMillis(),
        )
    }

    /**
     * Single-flight get-or-put: if multiple coroutines request the same key
     * simultaneously, only one executes the loader; others wait for the result.
     * Spec §17: "Cache stampede prevention: single-flight pattern"
     */
    suspend fun <T> getOrPut(key: String, ttlMinutes: Int, loader: suspend () -> T): T {
        @Suppress("UNCHECKED_CAST")
        val cached = get(key) as? T
        if (cached != null) return cached

        val mutex = locks.computeIfAbsent(key) { Mutex() }
        return mutex.withLock {
            // Double-check after acquiring lock
            @Suppress("UNCHECKED_CAST")
            val doubleChecked = get(key) as? T
            if (doubleChecked != null) return@withLock doubleChecked

            val value = loader()
            put(key, value, ttlMinutes)
            value
        }.also {
            locks.remove(key)
        }
    }

    fun invalidate(key: String) {
        store.remove(key)
    }

    /**
     * Invalidate all keys matching a prefix pattern (e.g. "library:search:schoolId:*").
     * Spec §17: "Cache invalidation: event-driven via CacheInvalidationSubscriber"
     */
    fun invalidatePattern(pattern: String) {
        val prefix = pattern.substringBefore("*")
        if (pattern.endsWith("*")) {
            store.keys.filter { it.startsWith(prefix) }.forEach { store.remove(it) }
        } else {
            store.remove(pattern)
        }
    }

    fun invalidateAll() {
        store.clear()
    }

    fun stats(): CacheStats {
        val hits = hitCount.get()
        val misses = missCount.get()
        val total = hits + misses
        return CacheStats(
            size = store.size,
            hitRate = if (total > 0) hits.toDouble() / total else 0.0,
            hits = hits,
            misses = misses,
        )
    }

    private fun evictExpired() {
        val now = System.currentTimeMillis()
        store.entries.removeIf { it.value.expiresAt < now }
        // If still over budget, remove oldest entries (approximate LRU)
        if (store.size > MAX_ENTRIES) {
            store.entries
                .sortedBy { it.value.createdAt }
                .take(store.size - MAX_ENTRIES)
                .forEach { store.remove(it.key) }
        }
    }
}

data class CacheStats(
    val size: Int,
    val hitRate: Double,
    val hits: Long,
    val misses: Long,
)

// ── Event-driven cache invalidation (spec §17) ──────────────────────────────
// Call LibraryCacheInit.register() at application startup to wire up
// automatic cache invalidation on domain events.

object LibraryCacheInit {

    fun register() {
        LibraryEventBus.subscribe { event ->
            when (event) {
                is BookCreated, is BookUpdated, is BookDeleted, is BookArchived, is BookUnarchived,
                is CopyAdded, is CopyRepaired -> {
                    LibraryCache.invalidatePattern(LibraryCacheKeys.invalidateBookSearch(event.schoolId))
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateDashboard(event.schoolId))
                }
                is BookIssued, is BookReturned, is BookMarkedLost -> {
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateDashboard(event.schoolId))
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateTrending(event.schoolId))
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateFines(event.schoolId))
                }
                is FinePaid, is FineWaived -> {
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateFines(event.schoolId))
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateDashboard(event.schoolId))
                }
                is BookDamaged -> {
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateDashboard(event.schoolId))
                }
                is SettingsUpdated -> {
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateSettings(event.schoolId))
                }
                is CategoryCreated, is CategoryUpdated, is CategoryDeleted -> {
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateCategories(event.schoolId))
                }
                is AnnouncementCreated, is AnnouncementExpired -> {
                    LibraryCache.invalidate(LibraryCacheKeys.invalidateAnnouncements(event.schoolId))
                }
                is BadgeAwarded, is ReadingGoalSet -> {
                    // Student profile cache is keyed by studentId, invalidate broadly
                    LibraryCache.invalidatePattern("$PREFIX:profile:${event.schoolId}:*")
                }
                else -> {}
            }
        }
    }

    private const val PREFIX = "library"
}
