package com.mcast.heat.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo


object NetworkUtils {

    enum class NetworkType {
        WIFI, /* wifi网络 */
        MOBILE, /* 移动网络 */
        OTHRER, /* 其它类型的网络 */
        NULL, /* 没有连接上网络 */
    }

    private fun isConnected(context: Context): Boolean {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connMgr.activeNetworkInfo
        return networkInfo?.isConnected == true
    }

    fun getNetworkType(context: Context): NetworkType {
        val connMgr = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (isConnected(context)) {
            connMgr.allNetworks.forEach { network ->
                var type = -1
                type = connMgr.getNetworkInfo(network)?.type ?: -1

                if (type == ConnectivityManager.TYPE_WIFI) {
                    return NetworkType.WIFI
                }
                if (type == ConnectivityManager.TYPE_MOBILE) {
                    return NetworkType.MOBILE
                }
                return NetworkType.OTHRER
            }
        }
        return NetworkType.NULL
    }
}
