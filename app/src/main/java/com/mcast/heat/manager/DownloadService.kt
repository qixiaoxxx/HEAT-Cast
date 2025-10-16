package com.mcast.heat.manager

import com.mcast.heat.data.network.HttpDns
import com.mcast.heat.data.network.SSLSocketClient
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

interface DownloadService {

    @Streaming
    @GET
    suspend fun download(@Url fileUrl: String): ResponseBody

    @Streaming
    @GET
    suspend fun getContentLength(@Url fileUrl: String): ResponseBody

    companion object {

        fun create(): DownloadService {
            val logger =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.HEADERS }
            val client = OkHttpClient.Builder()
                .addInterceptor(logger)
                .dns(HttpDns())
                .sslSocketFactory(
                    SSLSocketClient.getSSLSocketFactory(),
                    SSLSocketClient.getX509TrustManager(),
                )
                .hostnameVerifier(SSLSocketClient.getHostnameVerifier())
                .build()

            return Retrofit.Builder()
                .baseUrl("https://update.95la.com")
                .client(client)
                .build()
                .create(DownloadService::class.java)
        }
    }
}