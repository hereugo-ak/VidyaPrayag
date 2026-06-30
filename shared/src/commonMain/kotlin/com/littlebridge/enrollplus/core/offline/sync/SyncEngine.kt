package com.littlebridge.enrollplus.core.offline.sync

import com.littlebridge.enrollplus.core.connectivity.NetworkMonitor
import com.littlebridge.enrollplus.core.connectivity.NetworkStatus
import com.littlebridge.enrollplus.core.offline.outbox.OutboxOperation
import com.littlebridge.enrollplus.core.offline.outbox.OutboxRepository
import com.littlebridge.enrollplus.core.prefs.PreferenceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class SyncEngine(
    private val outboxRepository: OutboxRepository,
    private val networkMonitor: NetworkMonitor,
    private val syncStateHolder: SyncStateHolder,
    private val preferenceRepository: PreferenceRepository,
    private val handlers: Map<String, OutboxOperationHandler>,
) {
    private var syncJob: Job? = null
    private val scope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Default)

    fun start() {
        if (syncJob?.isActive == true) return
        networkMonitor.start()
        syncJob = scope.launch {
            // Reset any IN_FLIGHT ops from a previous crashed session
            outboxRepository.resetInFlight()

            // Update telemetry on startup
            updateTelemetry()

            // React to network status changes
            networkMonitor.status.collectLatest { status ->
                if (status == NetworkStatus.Available) {
                    drainOutbox()
                }
            }
        }
    }

    fun stop() {
        syncJob?.cancel()
        syncJob = null
        networkMonitor.stop()
    }

    /**
     * Manually trigger a sync drain (e.g. after a write is enqueued while online).
     */
    suspend fun syncNow() {
        val status = networkMonitor.status.first()
        if (status == NetworkStatus.Available || status == NetworkStatus.Unknown) {
            drainOutbox()
        }
    }

    private suspend fun drainOutbox() {
        val now = currentTimeMillis()
        val due = outboxRepository.peekDueNow(now, limit = BATCH_SIZE)
        if (due.isEmpty()) {
            syncStateHolder.setIdle()
            return
        }

        syncStateHolder.setSyncing()
        var hadFailure = false

        for (op in due) {
            if (!scope.isActive) break

            val handler = handlers[op.type]
            if (handler == null) {
                outboxRepository.markFailed(op.id, "No handler for type ${op.type}")
                hadFailure = true
                continue
            }

            outboxRepository.markInFlight(op.id)
            val success = try {
                handler.handle(op)
            } catch (e: Throwable) {
                false
            }

            if (success) {
                outboxRepository.markDone(op.id)
            } else {
                val attemptCount = op.attempts + 1
                if (attemptCount >= MAX_ATTEMPTS) {
                    outboxRepository.markFailed(op.id, "Max attempts ($MAX_ATTEMPTS) exceeded")
                } else {
                    val nextAttempt = now + backoffMs(attemptCount)
                    outboxRepository.markRetryable(op.id, nextAttempt, "Replay failed (attempt $attemptCount)")
                }
                hadFailure = true
            }
        }

        updateTelemetry()

        if (hadFailure && outboxRepository.countPending() > 0) {
            syncStateHolder.setError("Some operations failed; will retry")
            // Schedule a retry after the shortest backoff
            scope.launch {
                delay(MIN_BACKOFF_MS)
                drainOutbox()
            }
        } else {
            syncStateHolder.setIdle()
        }
    }

    private suspend fun updateTelemetry() {
        val count = outboxRepository.countPending()
        val oldestCreatedAt = outboxRepository.oldestPendingCreatedAt()
        val ageMs = if (oldestCreatedAt != null) currentTimeMillis() - oldestCreatedAt else null
        syncStateHolder.updateTelemetry(count, ageMs)
    }

    private fun backoffMs(attempt: Int): Long {
        val base = MIN_BACKOFF_MS * (1L shl (attempt - 1).coerceAtMost(MAX_BACKOFF_SHIFT))
        val jitter = (base * 10 / 100) // 10% jitter
        return (base + jitter).coerceAtMost(MAX_BACKOFF_MS)
    }

    companion object {
        private const val BATCH_SIZE = 10
        private const val MIN_BACKOFF_MS = 5_000L
        private const val MAX_BACKOFF_MS = 300_000L // 5 minutes
        private const val MAX_BACKOFF_SHIFT = 6 // cap at 5min
        private const val MAX_ATTEMPTS = 10 // dead-letter after 10 retries
    }
}

internal expect fun currentTimeMillis(): Long
