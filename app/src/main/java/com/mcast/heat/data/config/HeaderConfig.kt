package com.mcast.heat.data.config

import android.content.Context
import android.os.Build
import com.mcast.heat.BuildConfig
import com.mcast.heat.util.NetworkUtils
import com.mcast.heat.util.getAndroidId

object HeaderConfig {
    var header_package_name_value = ""
    var header_version_name_value = ""
    var header_version_code_value = ""
    var header_device_id_value = ""
    var header_manufacturer_value = ""
    var header_model_value = ""
    var header_width_pixels_value = ""
    var header_height_pixels_value = ""
    var header_network_type_value = ""

    fun init(context: Context) {
        header_package_name_value = BuildConfig.APPLICATION_ID
        header_version_name_value = BuildConfig.VERSION_NAME
        header_version_code_value = BuildConfig.VERSION_CODE.toString()
        header_device_id_value = getAndroidId(context)
        header_manufacturer_value = Build.MANUFACTURER
        header_model_value = Build.MODEL
        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels
        header_width_pixels_value = width.toString()
        header_height_pixels_value = height.toString()
        header_network_type_value = NetworkUtils.getNetworkType(context).toString()
    }
}
