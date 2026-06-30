package com.littlebridge.enrollplus.core.connectivity

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeNetworkMonitor(initial: NetworkStatus = NetworkStatus.Unknown) : NetworkMonitor {
    private val _status = MutableStateFlow(initial)
    override val status: StateFlow<NetworkStatus> = _status.asStateFlow()
    var startCount = 0
        private set
    var stopCount = 0
        private set

    fun setStatus(status: NetworkStatus) {
        _status.value = status
    }

    override fun start() { startCount++ }
    override fun stop() { stopCount++ }
}

class NetworkMonitorTest {

    @Test
    fun fakeMonitor_initialStatus_isUnknown() {
        val monitor = FakeNetworkMonitor()
        assertEquals(NetworkStatus.Unknown, monitor.status.value)
    }

    @Test
    fun fakeMonitor_initialStatus_canBeSpecified() {
        val monitor = FakeNetworkMonitor(NetworkStatus.Available)
        assertEquals(NetworkStatus.Available, monitor.status.value)
    }

    @Test
    fun fakeMonitor_statusTransitions_updateFlow() {
        val monitor = FakeNetworkMonitor(NetworkStatus.Unavailable)
        assertEquals(NetworkStatus.Unavailable, monitor.status.value)

        monitor.setStatus(NetworkStatus.Available)
        assertEquals(NetworkStatus.Available, monitor.status.value)

        monitor.setStatus(NetworkStatus.Unavailable)
        assertEquals(NetworkStatus.Unavailable, monitor.status.value)
    }

    @Test
    fun fakeMonitor_startStop_areIdempotent() {
        val monitor = FakeNetworkMonitor()
        monitor.start()
        monitor.start()
        assertEquals(2, monitor.startCount)

        monitor.stop()
        monitor.stop()
        assertEquals(2, monitor.stopCount)
    }

    @Test
    fun networkStatus_enumHasThreeValues() {
        val values = NetworkStatus.values()
        assertEquals(3, values.size)
        assertEquals(NetworkStatus.Available, NetworkStatus.valueOf("Available"))
        assertEquals(NetworkStatus.Unavailable, NetworkStatus.valueOf("Unavailable"))
        assertEquals(NetworkStatus.Unknown, NetworkStatus.valueOf("Unknown"))
    }
}
