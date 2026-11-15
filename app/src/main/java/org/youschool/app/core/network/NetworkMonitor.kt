package org.youschool.app.core.network

import android.content.Context
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

    private fun checkInitialOnline(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val wasOffline = !_isOnline.value
            _isOnline.value = true
            if (wasOffline) {
                scope.launch {
                    onNetworkRestored()
                }
            }
        }

        override fun onLost(network: Network) {
            _isOnline.value = checkInitialOnline()
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            val wasOffline = !_isOnline.value
            _isOnline.value = hasInternet
            if (wasOffline && hasInternet) {
                scope.launch {
                    onNetworkRestored()
                }
            }
        }
    }

    fun start() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Fallback or ignore if already registered
        }
    }

    fun stop() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            // Ignore
        }
    }
}
