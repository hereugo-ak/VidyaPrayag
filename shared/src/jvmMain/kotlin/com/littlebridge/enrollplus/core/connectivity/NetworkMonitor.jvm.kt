package com.littlebridge.enrollplus.core.connectivity

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket

class JvmNetworkMonitor(
    private val pollIntervalMs: Long = 15_000L,
    private val connectTimeoutMs: Int = 3_000
) : NetworkMonitor {

    private val _status = MutableStateFlow(NetworkStatus.Unknown)
    override val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var pollJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun start() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                _status.value = probe()
                delay(pollIntervalMs)
            }
        }
    }

    override fun stop() {
        pollJob?.cancel()
        pollJob = null
    }

    private fun probe(): NetworkStatus {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("8.8.8.8", 53), connectTimeoutMs)
                if (socket.isConnected) NetworkStatus.Available else NetworkStatus.Unavailable
            }
        } catch (_: Exception) {
            NetworkStatus.Unavailable
        }
    }
}
