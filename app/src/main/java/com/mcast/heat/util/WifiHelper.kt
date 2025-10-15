package com.mcast.heat.util

import android.Manifest
import android.content.Context
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
import kotlin.coroutines.resume

class WifiHelper(private val context: Context) {

    private val wifiManager: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // 使用 MutableStateFlow 来保存当前 Wi-Fi 名称
    private val _wifiName = MutableStateFlow<String?>(null)
    val wifiName: StateFlow<String?> = _wifiName

    // 使用协程来监听 Wi-Fi 状态变化并发出 Wi-Fi 名称
    suspend fun startListeningWifiChanges() {
        // 检查权限
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _wifiName.value = null // 没有权限
            return
        }

        suspendCancellableCoroutine<Unit> { continuation ->
            // Android 10+ 使用 registerNetworkCallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val networkRequest = android.net.NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()

                val networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        val wifiInfo = wifiManager.connectionInfo
                        val ssid = getSsid(wifiInfo)
                        _wifiName.value = ssid // 更新 Wi-Fi 名称
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        _wifiName.value = null // Wi-Fi 断开时清空 Wi-Fi 名称
                    }
                }

                connectivityManager.registerNetworkCallback(networkRequest, networkCallback)

                // 取消时注销回调
                continuation.invokeOnCancellation {
                    connectivityManager.unregisterNetworkCallback(networkCallback)
                }
            } else {
                // Android 9 及以下使用 WifiManager
                val wifiInfo = wifiManager.connectionInfo
                val ssid = getSsid(wifiInfo)
                _wifiName.value = ssid // 更新 Wi-Fi 名称
            }

            continuation.resume(Unit) // 恢复协程
        }
    }

    // 获取当前 Wi-Fi 名称
    private fun getSsid(wifiInfo: WifiInfo?): String? {
        if (wifiInfo == null) {
            return null
        }
        return wifiInfo.ssid.removePrefix("\"").removeSuffix("\"")
    }
}