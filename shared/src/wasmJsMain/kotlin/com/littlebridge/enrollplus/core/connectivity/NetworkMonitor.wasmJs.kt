package com.littlebridge.enrollplus.core.connectivity

import kotlinx.browser.window
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.w3c.dom.events.Event

class WasmJsNetworkMonitor : NetworkMonitor {

    private val _status = MutableStateFlow(
        if (window.navigator.onLine) NetworkStatus.Available else NetworkStatus.Unavailable
    )
    override val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private var onlineListener: ((Event) -> Unit)? = null
    private var offlineListener: ((Event) -> Unit)? = null
    private var listening = false

    override fun start() {
        if (listening) return
        onlineListener = { _status.value = NetworkStatus.Available }
        offlineListener = { _status.value = NetworkStatus.Unavailable }
        window.addEventListener("online", onlineListener!!)
        window.addEventListener("offline", offlineListener!!)
        _status.value = if (window.navigator.onLine) NetworkStatus.Available else NetworkStatus.Unavailable
        listening = true
    }

    override fun stop() {
        if (!listening) return
        onlineListener?.let { window.removeEventListener("online", it) }
        offlineListener?.let { window.removeEventListener("offline", it) }
        onlineListener = null
        offlineListener = null
        listening = false
    }
}
