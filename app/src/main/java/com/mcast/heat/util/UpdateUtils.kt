package com.mcast.heat.util


import android.content.Context
import android.content.SharedPreferences

object UpdateUtils {
    private const val PREFS_NAME = "update_prefs"
    private const val KEY_PENDING_APK_PATH = "pending_apk_path"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 保存待安装的 APK 文件路径
     */
    fun setPendingApkPath(context: Context, path: String?) {
        getPrefs(context).edit().putString(KEY_PENDING_APK_PATH, path).apply()
    }

    /**
     * 获取待安装的 APK 文件路径
     */
    fun getPendingApkPath(context: Context): String? {
        return getPrefs(context).getString(KEY_PENDING_APK_PATH, null)
    }
}