package com.mcast.heat.util

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

class WifiHelper(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _wifiName = MutableStateFlow<String?>(null)
    val wifiName: StateFlow<String?> = _wifiName

    suspend fun startListeningWifiChanges() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _wifiName.value = "Permission Denied"
            return
        }

        updateCurrentWifiName()

        suspendCancellableCoroutine<Unit> { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val networkRequest = android.net.NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()

                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        updateCurrentWifiName()
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        updateCurrentWifiName()
                    }
                }

                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

                continuation.invokeOnCancellation {
                    try {
                        connectivityManager.unregisterNetworkCallback(networkCallback)
                    } catch (_: Exception) {
                    }
                }

            } else {
                @Suppress("DEPRECATION")
                val wifiStateReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == WifiManager.NETWORK_STATE_CHANGED_ACTION ||
                            intent.action == ConnectivityManager.CONNECTIVITY_ACTION
                        ) {
                            updateCurrentWifiName()
                        }
                    }
                }

                @Suppress("DEPRECATION")
                val intentFilter = IntentFilter().apply {
                    addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
                    addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                }
                context.registerReceiver(wifiStateReceiver, intentFilter)

                continuation.invokeOnCancellation {
                    try {
                        context.unregisterReceiver(wifiStateReceiver)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    private fun updateCurrentWifiName() {
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _wifiName.value = "Permission Denied"
            return
        }

        var wifiInfo: WifiInfo?
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
            @Suppress("DEPRECATION")
            wifiInfo = wifiManager.connectionInfo
        } else {
            @Suppress("DEPRECATION")
            wifiInfo = wifiManager.connectionInfo
        }
        val rawSsid = wifiInfo?.ssid
        val ssid = getSsid(wifiInfo)
        if (rawSsid != null && rawSsid.contains("<unknown ssid>", ignoreCase = true)) {
            _wifiName.value = null
        } else {
            _wifiName.value = ssid
        }
    }

    private fun getSsid(wifiInfo: WifiInfo?): String? {
        return wifiInfo?.ssid?.removeSurrounding("\"")
    }
}
