package com.littlebridge.enrollplus.core.offline.outbox

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * No-op outbox for web targets (JS/WasmJs) which have no Room.
 * Web is online-only — enqueue is a no-op, observePending always emits empty.
 * This satisfies the OutboxRepository contract so common code never crashes.
 */
class InMemoryOutboxRepository : OutboxRepository {

    override suspend fun enqueue(op: OutboxOperation) {
        // No-op: web is online-only
    }

    override fun observePending(): Flow<List<OutboxOperation>> = flowOf(emptyList())

    override suspend fun peekDueNow(now: Long, limit: Int): List<OutboxOperation> = emptyList()

    override suspend fun markInFlight(id: String) {
        // No-op
    }

    override suspend fun markDone(id: String) {
        // No-op
    }

    override suspend fun markFailed(id: String, error: String) {
        // No-op
    }

    override suspend fun markRetryable(id: String, nextAttemptAt: Long, error: String) {
        // No-op
    }

    override suspend fun resetInFlight() {
        // No-op
    }

    override suspend fun countPending(): Int = 0

    override suspend fun oldestPendingCreatedAt(): Long? = null
}
