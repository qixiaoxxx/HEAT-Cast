package com.mcast.heat.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.google.firebase.analytics.FirebaseAnalytics
import com.mcast.heat.BuildConfig
import com.waxrain.airplaydmr.WaxPlayService
import java.io.File
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Collections
import java.util.Date
import java.util.Locale
import androidx.core.content.edit

/**
 * 获取并构建设备名称
 * "HEATCast_" 固定 ，"xxxxxx" 是 MAC 地址的后六位。
 */
@SuppressLint("MissingPermission")
fun getDeviceName(): String {
    try {
        val baseName = "HEATCast_"
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

            return baseName
        }
    } catch (_: Exception) {
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

/**
 * @param context 上下文对象，用于获取文件目录。
 * @param eventName 要上报的事件名称 (使用 FirebaseAnalytics.Event.* 中的常量)。
 * @param params 一个可变的参数列表，由键值对 (Pair<String, String>) 组成。
 */

fun logFirebaseEvent(context: Context, eventName: String, vararg params: Pair<String, String>) {
    val bundle = Bundle()
    for ((key, value) in params) {
        bundle.putString(key, value)
    }
    FirebaseAnalytics.getInstance(context).logEvent(eventName, bundle)
}

//安装apk
fun Context.installApk(apkFile: File) {
    val fileProviderAuthority = "$packageName.fileProvider"
    val intent = Intent(Intent.ACTION_VIEW)
    intent.addCategory(Intent.CATEGORY_DEFAULT)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val type = "application/vnd.android.package-archive"

    val uri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        // 对于 Android 7.0 及以上版本，使用 FileProvider
        FileProvider.getUriForFile(this, fileProviderAuthority, apkFile).also {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        // 对于旧版本，直接使用文件 Uri
        Uri.fromFile(apkFile)
    }

    intent.setDataAndType(uri, type)

    try {
        startActivity(intent)
    } catch (e: Exception) {
        Log.e("InstallApk", "启动安装程序时出错", e)
    }
}

//获取设备网络名称
fun getManufactureModel(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    return if (model.startsWith(manufacturer)) {
        model.capitalize(Locale.getDefault())
    } else {
        manufacturer.capitalize(Locale.getDefault()) + " " + model
    }
}

/**
 * 删除指定下载目录中的所有 APK 文件。
 * @param context 上下文对象，用于获取文件目录。
 */
fun cleanupOldApks(context: Context) {
    val prefs = context.getSharedPreferences("AppUpdatePrefs", Context.MODE_PRIVATE)
    val lastRunVersionCode = prefs.getInt("last_run_version_code", 0)
    val currentVersionCode = BuildConfig.VERSION_CODE

    // 只有当 当前版本号 > 上次记录的版本号时，才认为是升级后的首次启动
    if (currentVersionCode > lastRunVersionCode) {
        Log.i(
            "Cleanup",
            "App updated from version $lastRunVersionCode to $currentVersionCode. Cleaning up old APKs."
        )

        // 在这里执行你原来的删除逻辑
        try {
            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            downloadDir?.listFiles { file ->
                file.name.endsWith(".apk", ignoreCase = true)
            }?.forEach { apkFile ->
                if (apkFile.exists()) {
                    apkFile.delete()
                    Log.i("Cleanup", "Deleted old APK: ${apkFile.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("Cleanup", "Error cleaning up APKs", e)
        }

        // 完成清理后，立即更新 SharedPreferences 中的版本号
        prefs.edit { putInt("last_run_version_code", currentVersionCode) }
    } else {
        // 如果不是升级，则不执行任何操作
        Log.i("Cleanup", "Not a new version launch. Skipping APK cleanup.")
    }
}

/**
 * 获取当前设备时区的格式化时间字符串
 * @return 形如 "yyyy-MM-dd HH:mm:ss Z" 的时间字符串。
 */
fun getCurrentFormattedTime(): String {
    val pattern = "yyyy-MM-dd HH:mm:ss Z"
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val formatter = DateTimeFormatter.ofPattern(pattern, Locale.ROOT)
        ZonedDateTime.now().format(formatter)
    } else {
        val sdf = SimpleDateFormat(pattern, Locale.ROOT)
        sdf.format(Date())
    }
}
