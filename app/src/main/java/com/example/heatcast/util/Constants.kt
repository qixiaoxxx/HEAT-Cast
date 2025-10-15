package com.example.heatcast.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
import com.waxrain.airplaydmr.WaxPlayService
import com.waxrain.utils.Config
import java.net.NetworkInterface
import java.util.Collections



/**
 * 获取当前连接的 Wi-Fi 名称 (SSID)。
 */
fun getWifiName(context: Context): String? {
    //  检查最关键的权限
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        Log.w("WifiInfo", "权限不足 (ACCESS_FINE_LOCATION)，无法获取 Wi-Fi 名称。")
        return "权限不足"
    }

    //  获取 ConnectivityManager
    val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return null

    //  检查网络状态并确认是 Wi-Fi
    val activeNetwork = connectivityManager.activeNetwork ?: return null
    val networkCapabilities =
        connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

    if (!networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        Log.d("WifiInfo", "当前活跃网络不是 Wi-Fi。")
        return null // 如果不是 Wi-Fi 连接，则返回 null
    }

    //  获取 Wi-Fi 名称 (SSID)
    // 从 Android 8.1 (API 27) 开始，推荐使用 WifiManager 来获取
    val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return null

    // connectionInfo 在拥有 ACCESS_FINE_LOCATION 权限时是可靠的
    val wifiInfo = wifiManager.connectionInfo

    // wifiInfo.ssid 返回的名称可能包含双引号，例如 "<unknown ssid>" 或 "\"MyWiFi\""
    if (wifiInfo != null && !wifiInfo.ssid.isNullOrEmpty() && wifiInfo.ssid != "<unknown ssid>") {
        // 移除 SSID 名称前后的双引号
        return wifiInfo.ssid.trim { it == '"' }
    }

    return null
}


/**
 * 获取并构建设备名称。
 * "HEATCast_" 来自 SDK，"xxxxxx" 是 MAC 地址的后六位。
 */
@SuppressLint("MissingPermission")
fun getDeviceName(context: Context): String {
    try {
        if (WaxPlayService._config == null) {
            WaxPlayService._config = Config(context.applicationContext)
        }
        val baseName = WaxPlayService._config.nickName
        if (!baseName.isNullOrEmpty()) {
            val macSuffix = getMacAddressLast6Chars()
            if (macSuffix != null) {
                // 成功获取基础名称和 MAC 后缀
                return "${baseName}${macSuffix}"
            } else {
                // MAC 获取失败，使用 SDK 原始后缀作为备选
                val nameExtension = WaxPlayService._config.btHidDevName
                if (nameExtension.toString().isNotEmpty()) {
                    return "${baseName}${nameExtension}"
                }
            }
            return baseName
        }
    } catch (e: Exception) {
        Log.e("DeviceInfo", "从 SDK 获取名称失败", e)
    }
    return Build.MANUFACTURER
}

/**
 * 获取设备 Wi-Fi MAC 地址的最后6位字符。
 * @return 格式化后的大写字符串 (例如 "A1B2C3")，如果获取失败则返回 null。
 */
fun getMacAddressLast6Chars(): String? {
    try {
        // 遍历所有网络接口，找到名为 "wlan0" 的 Wi-Fi 接口
        val networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (nif in networkInterfaces) {
            if (nif.name.equals("wlan0", ignoreCase = true)) {
                val macBytes = nif.hardwareAddress ?: continue // 获取硬件地址，失败则跳过

                // 将字节数组转换为十六进制字符串，并取最后6位
                val macString = macBytes.joinToString("") { String.format("%02X", it) }
                return if (macString.length >= 6) macString.takeLast(6) else null
            }
        }
    } catch (ex: Exception) {
        Log.e("DeviceInfo", "获取 MAC 地址时发生异常", ex)
    }

    // 如果没有找到 "wlan0" 接口或发生异常，则返回 null
    Log.w("DeviceInfo", "未能找到 'wlan0' 接口或获取 MAC 地址失败。")
    return null
}

//获取设备AndroidId
@SuppressLint("HardwareIds")
fun getAndroidId(context: Context): String {
    return Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ANDROID_ID
    )
}
