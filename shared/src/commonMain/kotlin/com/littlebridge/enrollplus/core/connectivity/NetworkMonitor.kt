package com.littlebridge.enrollplus.core.connectivity

import kotlinx.coroutines.flow.StateFlow

enum class NetworkStatus { Available, Unavailable, Unknown }

interface NetworkMonitor {
    val status: StateFlow<NetworkStatus>
    fun start()
    fun stop()
}
