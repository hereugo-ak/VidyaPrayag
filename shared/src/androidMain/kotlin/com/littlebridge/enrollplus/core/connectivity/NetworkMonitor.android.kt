package com.littlebridge.enrollplus.core.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidNetworkMonitor(
    private val context: Context
) : NetworkMonitor {

    private val _status = MutableStateFlow(NetworkStatus.Unknown)
    override val status: StateFlow<NetworkStatus> = _status.asStateFlow()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private var callback: ConnectivityManager.NetworkCallback? = null
    private var registered = false

    override fun start() {
        if (registered) return
        val cm = connectivityManager ?: run {
            _status.value = NetworkStatus.Unknown
            return
        }
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _status.value = NetworkStatus.Available
            }

            override fun onLost(network: Network) {
                _status.value = NetworkStatus.Unavailable
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                val validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                _status.value = if (validated) NetworkStatus.Available else NetworkStatus.Unavailable
            }
        }
        try {
            cm.registerNetworkCallback(
                android.net.NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build(),
                cb
            )
            registered = true
            callback = cb
            val active = cm.activeNetwork
            if (active != null) {
                val caps = cm.getNetworkCapabilities(active)
                _status.value = if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true)
                    NetworkStatus.Available else NetworkStatus.Unavailable
            } else {
                _status.value = NetworkStatus.Unavailable
            }
        } catch (e: Exception) {
            _status.value = NetworkStatus.Unknown
        }
    }

    override fun stop() {
        if (!registered) return
        try {
            callback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {
        }
        registered = false
        callback = null
    }
}
