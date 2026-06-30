package com.littlebridge.enrollplus.core.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Network.NWPath
import platform.Network.NWPathMonitor
import platform.Network.NWPathStatus
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_t
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class IosNetworkMonitor : NetworkMonitor {

    private val _status = MutableStateFlow(NetworkStatus.Unknown)
    override val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var monitor: NWPathMonitor? = null
    private var started = false

    override fun start() {
        if (started) return
        try {
            val monitor = nw_path_monitor_create()
            this.monitor = monitor
            val queue: dispatch_queue_t = dispatch_get_main_queue()
            nw_path_monitor_set_queue(monitor, queue)

            monitor.setUpdateHandler { path ->
                _status.value = when (path.status) {
                    NWPathStatus.NWPathStatusSatisfied -> NetworkStatus.Available
                    NWPathStatus.NWPathStatusUnsatisfied -> NetworkStatus.Unavailable
                    else -> NetworkStatus.Unknown
                }
            }
            monitor.start()
            started = true
        } catch (_: Exception) {
            _status.value = NetworkStatus.Unknown
        }
    }

    override fun stop() {
        if (!started) return
        try {
            monitor?.cancel()
        } catch (_: Exception) {
        }
        monitor = null
        started = false
    }
}
