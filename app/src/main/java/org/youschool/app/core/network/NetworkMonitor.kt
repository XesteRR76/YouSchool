package org.youschool.app.core.network

import android.content.Context
import android.os.Build
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NetworkMonitor(
    context: Context,
    private val onNetworkRestored: suspend () -> Unit = {}
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isOnline = MutableStateFlow(checkInitialOnline())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)

    private fun checkCaps(network: Network, caps: NetworkCapabilities? = null): Boolean {
        val currentCaps = caps ?: connectivityManager.getNetworkCapabilities(network) ?: return false
        val hasInternet = currentCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = currentCaps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isVpn = currentCaps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)

        if (isVpn) {
            // If active network is a VPN, ensure there is an underlying physical connection (WiFi, Cellular, Ethernet)
            val hasPhysical = connectivityManager.allNetworks.any { net ->
                if (net == network) return@any false
                val c = connectivityManager.getNetworkCapabilities(net) ?: return@any false
                !c.hasTransport(NetworkCapabilities.TRANSPORT_VPN) &&
                        c.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        c.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            if (!hasPhysical) {
                return false
            }
        }

        return hasInternet && isValidated
    }

    private fun checkInitialOnline(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        return checkCaps(activeNetwork)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateOnlineState()
        }

        override fun onLost(network: Network) {
            updateOnlineState()
        }

        override fun onUnavailable() {
            updateOnlineState()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            updateOnlineState()
        }
    }

    private fun updateOnlineState() {
        val online = checkInitialOnline()
        val wasOffline = !_isOnline.value
        _isOnline.value = online
        if (wasOffline && online) {
            scope.launch {
                onNetworkRestored()
            }
        }
    }

    fun start() {
        try {
            val request = NetworkRequest.Builder().build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    connectivityManager.registerDefaultNetworkCallback(networkCallback)
                }
            } catch (_: Exception) {}
        }
        _isOnline.value = checkInitialOnline()
    }

    fun stop() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
