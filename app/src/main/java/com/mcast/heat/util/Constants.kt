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
import androidx.core.content.edit
import com.google.firebase.analytics.FirebaseAnalytics
import com.mcast.heat.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * 获取并构建设备名称
 * "HEATCast_" 固定 ，"xxxx" 是从设备唯一标识中提取的后缀。
 */
@SuppressLint("HardwareIds")
fun getDeviceName(context: Context): String {
    try {
        val baseName = "HEATCast_"
        // 使用 ANDROID_ID 作为稳定的设备标识来源
        val androidId = getAndroidId(context)
        // 取 ANDROID_ID 的后四位作为后缀，如果长度不足则使用整个 ID
        val idSuffix = if (androidId.length >= 4) androidId.takeLast(4) else androidId

        return "${baseName}${idSuffix.uppercase(Locale.ROOT)}"
    } catch (e: Exception) {
        // 异常情况下，回退到使用设备型号
        Log.e("DeviceInfo", "获取设备名称时发生异常", e)
        return Build.MANUFACTURER
    }
}

// 获取设备AndroidId
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

/**
 * 将总秒数格式化为易读的时长字符串。
 * @param totalSeconds 总时长（秒）。
 * @return 格式化后的时长字符串（如 45s, 2m23s, 1h30m48s）。
 */
fun formatDurationFromSeconds(totalSeconds: Long): String {
    if (totalSeconds < 0) return "0s"
    if (totalSeconds == 0L) return "0s"

    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    val builder = StringBuilder()

    if (hours > 0) {
        builder.append(hours).append("h")
    }

    if (minutes > 0) {
        builder.append(minutes).append("m")
    }

    // 如果总时长小于1分钟，或者秒数不为0，则添加秒
    if (totalSeconds < 60 || seconds > 0) {
        builder.append(seconds).append("s")
    }

    // 如果构建后为空（逻辑上不太可能，除非totalSeconds > 0但所有部分都为0），则返回0s
    return if (builder.isEmpty()) "0s" else builder.toString()
}

/**
 * 计算两个带时区格式的时间字符串之间的秒数差。
 * @return 时长（秒），如果解析失败会抛出异常
 */
fun calculateDurationToLong(startTimeString: String, endTimeString: String): Long {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", java.util.Locale.ROOT)
    try {
        val startDate = format.parse(startTimeString)
        val endDate = format.parse(endTimeString)
        if (startDate != null && endDate != null) {
            val diffInMillis = endDate.time - startDate.time
            return diffInMillis / 1000
        } else {
            throw IllegalArgumentException("Date parsing returned null")
        }
    } catch (e: Exception) {
        throw IllegalArgumentException("Error calculating duration: ${e.message}")
    }
}