package com.littlebridge.enrollplus.core.offline.outbox

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomOutboxRepository(
    private val dao: OutboxDao,
) : OutboxRepository {

    override suspend fun enqueue(op: OutboxOperation) {
        dao.insert(op.toEntity())
    }

    override fun observePending(): Flow<List<OutboxOperation>> =
        dao.observePending().map { list -> list.map { it.toDomain() } }

    override suspend fun peekDueNow(now: Long, limit: Int): List<OutboxOperation> =
        dao.peekDueNow(now, limit).map { it.toDomain() }

    override suspend fun markInFlight(id: String) {
        dao.markInFlight(id)
    }

    override suspend fun markDone(id: String) {
        dao.markDone(id)
    }

    override suspend fun markFailed(id: String, error: String) {
        dao.markFailed(id, error)
    }

    override suspend fun markRetryable(id: String, nextAttemptAt: Long, error: String) {
        dao.markRetryable(id, nextAttemptAt, error)
    }

    override suspend fun resetInFlight() {
        dao.resetInFlight()
    }

    override suspend fun countPending(): Int {
        return dao.countPending()
    }

    override suspend fun oldestPendingCreatedAt(): Long? {
        return dao.oldestPendingCreatedAt()
    }
}
