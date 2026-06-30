package com.littlebridge.enrollplus.core.offline.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SyncStatus {
    IDLE,
    SYNCING,
    ERROR,
}

data class SyncState(
    val status: SyncStatus = SyncStatus.IDLE,
    val pendingCount: Int = 0,
    val oldestPendingAgeMs: Long? = null,
    val lastError: String? = null,
) {
    val isOffline: Boolean get() = status == SyncStatus.ERROR && pendingCount > 0
    val hasPending: Boolean get() = pendingCount > 0
}

class SyncStateHolder {
    private val _state = MutableStateFlow(SyncState())
    val state: StateFlow<SyncState> = _state.asStateFlow()

    fun update(state: SyncState) {
        _state.value = state
    }

    fun updatePendingCount(count: Int) {
        _state.value = _state.value.copy(pendingCount = count)
    }

    fun updateTelemetry(count: Int, oldestPendingAgeMs: Long?) {
        _state.value = _state.value.copy(pendingCount = count, oldestPendingAgeMs = oldestPendingAgeMs)
    }

    fun setSyncing() {
        _state.value = _state.value.copy(status = SyncStatus.SYNCING, lastError = null)
    }

    fun setIdle() {
        _state.value = _state.value.copy(status = SyncStatus.IDLE, lastError = null)
    }

    fun setError(reason: String) {
        _state.value = _state.value.copy(status = SyncStatus.ERROR, lastError = reason)
    }
}
