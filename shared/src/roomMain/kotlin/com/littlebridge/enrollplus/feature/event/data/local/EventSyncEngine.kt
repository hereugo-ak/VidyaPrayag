package com.littlebridge.enrollplus.feature.event.data.local

import com.littlebridge.enrollplus.core.database.AppDatabase
import com.littlebridge.enrollplus.core.network.NetworkResult
import com.littlebridge.enrollplus.feature.event.data.repository.EventRegistrationRepositoryImpl
import com.littlebridge.enrollplus.feature.event.domain.model.CancelRegistrationRequest
import com.littlebridge.enrollplus.feature.event.domain.model.RegisterRequest
import com.littlebridge.enrollplus.feature.event.domain.model.RescheduleRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class EventSyncEngine(
    private val db: AppDatabase,
    private val repository: EventRegistrationRepositoryImpl,
) {
    companion object {
        private const val TAG = "EventSyncEngine"
        private const val MAX_ATTEMPTS = 10
        private const val POLL_INTERVAL_MS = 30_000L
    }

    private var isDraining = false

    fun start(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(POLL_INTERVAL_MS)
                runCatching { drain() }
                    .onFailure { println("[$TAG] drain failed: ${it.message}") }
            }
        }
    }

    suspend fun drain() {
        if (isDraining) return
        isDraining = true
        try {
            val pending = db.eventOutboxDao().getPending()
            if (pending.isEmpty()) return

            for (op in pending) {
                if (op.attempts >= MAX_ATTEMPTS) {
                    db.eventOutboxDao().updateStatus(
                        id = op.id,
                        status = "FAILED",
                        attempts = op.attempts,
                        lastError = "Max attempts reached",
                        updatedAt = System.currentTimeMillis(),
                    )
                    continue
                }
                processOp(op)
            }
        } finally {
            isDraining = false
        }
    }

    private suspend fun processOp(op: EventOutboxEntity) {
        val token = "offline-sync"
        val now = System.currentTimeMillis()
        val result = when (op.operation) {
            "REGISTER" -> repository.register(
                token = token,
                eventId = op.eventId,
                request = RegisterRequest(
                    slotId = op.slotId,
                    studentId = op.studentId,
                    attendeeCount = op.attendeeCount,
                ),
                clientRequestId = op.clientRequestId,
            )
            "CANCEL" -> repository.cancelRegistration(
                token = token,
                eventId = op.eventId,
                request = CancelRegistrationRequest(
                    studentId = op.studentId,
                ),
            )
            "RESCHEDULE" -> repository.reschedule(
                token = token,
                eventId = op.eventId,
                request = RescheduleRequest(
                    newSlotId = op.slotId!!,
                ),
            )
            else -> return
        }
        when (result) {
            is NetworkResult.Success -> {
                db.eventOutboxDao().updateStatus(
                    id = op.id,
                    status = "SYNCED",
                    attempts = op.attempts + 1,
                    lastError = null,
                    updatedAt = now,
                )
            }
            is NetworkResult.Error -> {
                db.eventOutboxDao().updateStatus(
                    id = op.id,
                    status = "PENDING",
                    attempts = op.attempts + 1,
                    lastError = result.message,
                    updatedAt = now,
                )
            }
            is NetworkResult.ConnectionError -> {
                // Leave as PENDING, will retry next cycle
            }
        }
    }
}
