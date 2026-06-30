package com.littlebridge.enrollplus.core.offline.sync

import com.littlebridge.enrollplus.core.offline.outbox.OutboxOperation

/**
 * A handler that knows how to replay one type of outbox operation.
 * Register one handler per operation type string (e.g. "ATTENDANCE_SAVE").
 */
interface OutboxOperationHandler {
    val type: String

    /**
     * Replay the operation against the server. Return true on success,
     * false on failure (the SyncEngine will retry with backoff).
     */
    suspend fun handle(op: OutboxOperation): Boolean
}
