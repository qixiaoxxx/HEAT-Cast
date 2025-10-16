package com.mcast.heat.data.network

import com.mcast.heat.data.HEADER_DEVICE_ID
import com.mcast.heat.data.HEADER_HEIGHT_PIXELS
import com.mcast.heat.data.HEADER_MANUFACTURER
import com.mcast.heat.data.HEADER_MODEL
import com.mcast.heat.data.HEADER_NETWORK_TYPE
import com.mcast.heat.data.HEADER_PACKAGE_NAME
import com.mcast.heat.data.HEADER_VERSION_CODE
import com.mcast.heat.data.HEADER_VERSION_NAME
import com.mcast.heat.data.HEADER_WIDTH_PIXELS
import com.mcast.heat.data.config.HeaderConfig
import okhttp3.Interceptor
import okhttp3.Response


class HeaderInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val oldRequest = chain.request() // 获取旧连接
        val requestBuilder = oldRequest.newBuilder() // 建立新的构建者
        // 将旧请求的请求方法和请求体设置到新请求中
        requestBuilder.method(oldRequest.method, oldRequest.body)
        // 获取旧请求的头
        val headers = oldRequest.headers
        val names = headers.names()
        for (name in names) {
            val value = headers[name]
            // 将旧请求的头设置到新请求中
            if (value != null) {
                requestBuilder.header(name, value)
            }
        }
        // 添加额外的自定义公共请求头
        requestBuilder.apply {
            header(HEADER_PACKAGE_NAME, HeaderConfig.header_package_name_value)
            header(HEADER_VERSION_NAME, HeaderConfig.header_version_name_value)
            header(HEADER_VERSION_CODE, HeaderConfig.header_version_code_value)
            header(HEADER_DEVICE_ID, HeaderConfig.header_device_id_value)
            header(HEADER_MANUFACTURER, HeaderConfig.header_manufacturer_value)
            header(HEADER_MODEL, HeaderConfig.header_model_value)
            header(HEADER_WIDTH_PIXELS, HeaderConfig.header_width_pixels_value)
            header(HEADER_HEIGHT_PIXELS, HeaderConfig.header_height_pixels_value)
            header(HEADER_NETWORK_TYPE, HeaderConfig.header_network_type_value)
        }
        // 建立新请求连接
        val newRequest = requestBuilder.build()
        return chain.proceed(newRequest)
    }
}