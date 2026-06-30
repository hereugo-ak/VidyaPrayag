package com.littlebridge.enrollplus.core.offline.outbox

import kotlinx.coroutines.flow.Flow

interface OutboxRepository {
    suspend fun enqueue(op: OutboxOperation)
    fun observePending(): Flow<List<OutboxOperation>>
    suspend fun peekDueNow(now: Long, limit: Int): List<OutboxOperation>
    suspend fun markInFlight(id: String)
    suspend fun markDone(id: String)
    suspend fun markFailed(id: String, error: String)
    suspend fun markRetryable(id: String, nextAttemptAt: Long, error: String)
    suspend fun resetInFlight()
    suspend fun countPending(): Int
    suspend fun oldestPendingCreatedAt(): Long?
}
