package com.mcast.heat.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.waxrain.airplaydmr.WaxPlayService
import java.io.File
import java.net.NetworkInterface
import java.util.Collections
import java.util.Locale

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


//安装apk
fun Context.installApk(apkFile: File) {
    val fileProviderAuthority = "$packageName.fileprovider"
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
    // 获取应用专用的下载目录，这是存放 APK 的推荐位置
    val downloadDir =
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
    if (downloadDir != null && downloadDir.exists() && downloadDir.isDirectory) {
        // 列出目录中所有的 .apk 文件
        val oldApks = downloadDir.listFiles { _, name -> name.lowercase().endsWith(".apk") }

        if (oldApks.isNullOrEmpty()) {
            Log.d("Cleanup", "没有找到需要清理的旧 APK 文件。")
            return
        }

        // 遍历并删除每个 APK 文件
        for (apkFile in oldApks) {
            try {
                if (apkFile.delete()) {
                    Log.i("Cleanup", "成功删除旧的 APK 文件: ${apkFile.name}")
                } else {
                    Log.w("Cleanup", "删除旧的 APK 文件失败: ${apkFile.name}")
                }
            } catch (e: SecurityException) {
                Log.e("Cleanup", "删除 APK 文件时出现安全异常: ${apkFile.name}", e)
            }
        }
    } else {
        Log.w("Cleanup", "下载目录不存在或无法访问。")
    }
}