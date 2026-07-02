/*
 * File: LibraryEvents.kt
 * Module: feature.library
 *
 * Domain events + simple in-memory event bus for the library feature.
 *
 * Per spec §8.13, 19 domain events are defined. The EventBus interface
 * allows subscribers to react to domain events for side effects:
 *   - Notifications (already handled inline in LibraryService)
 *   - Audit log (already handled inline in LibraryService)
 *   - Cache invalidation (via CacheInvalidationSubscriber)
 *
 * The in-memory implementation is thread-safe and synchronous — events
 * are dispatched to all subscribers on the calling thread. For async
 * dispatch, wrap subscriber logic in a coroutine launch.
 *
 * Usage in LibraryService:
 *   LibraryEventBus.publish(BookIssued(schoolId, bookId, copyId, borrowerId, ...))
 *
 * Usage for subscribers:
 *   LibraryEventBus.subscribe { event ->
 *       when (event) {
 *           is BookReturned -> invalidateCache(event.schoolId, event.bookId)
 *           ...
 *       }
 *   }
 */
package com.littlebridge.enrollplus.feature.library

import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

// ── Domain Events (spec §8.13) ──────────────────────────────────────────────

sealed class LibraryEvent {
    abstract val schoolId: UUID
    abstract val timestamp: Instant
    abstract val actorId: UUID
    abstract val actorName: String
}

data class BookIssued(
    override val schoolId: UUID,
    val bookId: UUID,
    val copyId: UUID?,
    val borrowerId: UUID,
    val borrowerName: String,
    val dueDate: LocalDate,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BookReturned(
    override val schoolId: UUID,
    val bookId: UUID,
    val issueId: UUID,
    val borrowerId: UUID,
    val fineAmount: Double,
    val returnCondition: String?,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BookRenewed(
    override val schoolId: UUID,
    val bookId: UUID,
    val issueId: UUID,
    val borrowerId: UUID,
    val newDueDate: LocalDate,
    val renewalCount: Int,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BookReserved(
    override val schoolId: UUID,
    val bookId: UUID,
    val reservationId: UUID,
    val reservedBy: UUID,
    val reservedByName: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class ReservationAvailable(
    override val schoolId: UUID,
    val bookId: UUID,
    val reservationId: UUID,
    val reservedBy: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class ReservationFulfilled(
    override val schoolId: UUID,
    val bookId: UUID,
    val reservationId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class ReservationCancelled(
    override val schoolId: UUID,
    val bookId: UUID,
    val reservationId: UUID,
    val cancelledBy: UUID,
    val reason: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BookMarkedLost(
    override val schoolId: UUID,
    val bookId: UUID,
    val issueId: UUID,
    val borrowerId: UUID,
    val replacementCost: Double,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class FinePaid(
    override val schoolId: UUID,
    val issueId: UUID,
    val bookId: UUID,
    val borrowerId: UUID,
    val amount: Double,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class FineWaived(
    override val schoolId: UUID,
    val issueId: UUID,
    val bookId: UUID,
    val borrowerId: UUID,
    val amount: Double,
    val reason: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BookDamaged(
    override val schoolId: UUID,
    val bookId: UUID,
    val issueId: UUID,
    val copyId: UUID?,
    val damageNotes: String?,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BulkImportCompleted(
    override val schoolId: UUID,
    val successCount: Int,
    val failureCount: Int,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class CategoryCreated(
    override val schoolId: UUID,
    val categoryId: UUID,
    val categoryName: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class CategoryUpdated(
    override val schoolId: UUID,
    val categoryId: UUID,
    val categoryName: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class CategoryDeleted(
    override val schoolId: UUID,
    val categoryId: UUID,
    val categoryName: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class SettingsUpdated(
    override val schoolId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class AnnouncementCreated(
    override val schoolId: UUID,
    val announcementId: UUID,
    val title: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class AnnouncementExpired(
    override val schoolId: UUID,
    val announcementId: UUID,
    val title: String,
    override val actorId: UUID = UUID(0, 0),
    override val actorName: String = "system",
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class AuditLogWritten(
    override val schoolId: UUID,
    val auditLogId: UUID,
    val action: String,
    val entityType: String,
    val entityId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

// ── Additional events (beyond spec §8.13 — for completeness) ───────────────

data class BookCreated(
    override val schoolId: UUID,
    val bookId: UUID,
    val title: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BookUpdated(
    override val schoolId: UUID,
    val bookId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BookDeleted(
    override val schoolId: UUID,
    val bookId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BookArchived(
    override val schoolId: UUID,
    val bookId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BookUnarchived(
    override val schoolId: UUID,
    val bookId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class CoverUploaded(
    override val schoolId: UUID,
    val bookId: UUID,
    val coverUrl: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class CopyAdded(
    override val schoolId: UUID,
    val bookId: UUID,
    val copyId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class CopyRepaired(
    override val schoolId: UUID,
    val bookId: UUID,
    val copyId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class WishlistAdded(
    override val schoolId: UUID,
    val bookId: UUID,
    val userId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class WishlistRemoved(
    override val schoolId: UUID,
    val bookId: UUID,
    val userId: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class ReadingGoalSet(
    override val schoolId: UUID,
    val studentId: UUID,
    val goal: Int,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class BadgeAwarded(
    override val schoolId: UUID,
    val studentId: UUID,
    val badgeType: String,
    val badgeName: String,
    override val actorId: UUID = UUID(0, 0),
    override val actorName: String = "system",
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class AcquisitionRequestSubmitted(
    override val schoolId: UUID,
    val requestId: UUID,
    val title: String,
    val requestedBy: UUID,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

data class AcquisitionStatusChanged(
    override val schoolId: UUID,
    val requestId: UUID,
    val oldStatus: String,
    val newStatus: String,
    override val actorId: UUID,
    override val actorName: String,
    override val timestamp: Instant = Instant.now(),
) : LibraryEvent()

// ── Event Bus ───────────────────────────────────────────────────────────────

typealias LibraryEventSubscriber = (LibraryEvent) -> Unit

/**
 * Simple in-memory event bus for library domain events.
 * Thread-safe via CopyOnWriteArrayList.
 *
 * Subscribers are called synchronously on the publishing thread.
 * For async handling, wrap subscriber logic in a coroutine launch.
 */
object LibraryEventBus {

    private val subscribers = CopyOnWriteArrayList<LibraryEventSubscriber>()
    private val typedSubscribers = ConcurrentHashMap<Class<out LibraryEvent>, CopyOnWriteArrayList<LibraryEventSubscriber>>()

    /**
     * Subscribe to ALL library events.
     */
    fun subscribe(subscriber: LibraryEventSubscriber) {
        subscribers.add(subscriber)
    }

    /**
     * Subscribe to a specific event type.
     */
    fun <T : LibraryEvent> subscribeTo(eventClass: Class<T>, subscriber: (T) -> Unit) {
        typedSubscribers
            .computeIfAbsent(eventClass) { CopyOnWriteArrayList() }
            .add { event -> @Suppress("UNCHECKED_CAST") subscriber(event as T) }
    }

    /**
     * Unsubscribe a previously registered subscriber.
     */
    fun unsubscribe(subscriber: LibraryEventSubscriber) {
        subscribers.remove(subscriber)
    }

    /**
     * Publish an event to all subscribers.
     * Exceptions in subscribers are caught and logged — never propagate.
     */
    fun publish(event: LibraryEvent) {
        // Global subscribers
        for (sub in subscribers) {
            runCatching { sub(event) }
                .onFailure { System.err.println("[LibraryEventBus] Subscriber error: ${it.message}") }
        }

        // Typed subscribers
        val typed = typedSubscribers[event::class.java]
        if (typed != null) {
            for (sub in typed) {
                runCatching { sub(event) }
                    .onFailure { System.err.println("[LibraryEventBus] Typed subscriber error: ${it.message}") }
            }
        }
    }

    /**
     * Clear all subscribers — primarily for testing.
     */
    fun clear() {
        subscribers.clear()
        typedSubscribers.clear()
    }
}
