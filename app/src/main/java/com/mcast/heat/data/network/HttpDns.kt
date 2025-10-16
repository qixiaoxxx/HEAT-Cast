package com.mcast.heat.data.network

import android.util.Log
import com.qiniu.android.dns.DnsManager
import com.qiniu.android.dns.NetworkInfo
import com.qiniu.android.dns.dns.DohResolver
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

data class Resolver(val url: String, val resolver: DohResolver)

class HttpDns : Dns {
    companion object {
        private const val TAG = "HttpDns"
    }

    private val resolvers: Array<Resolver> = arrayOf(
        Resolver("https://1.1.1.1/dns-query", DohResolver("https://1.1.1.1/dns-query", 5)),
        Resolver(
            "https://unfiltered.adguard-dns.com/dns-query",
            DohResolver("https://unfiltered.adguard-dns.com/dns-query", 5)
        ),
        Resolver("https://8.8.8.8/dns-query", DohResolver("https://8.8.8.8/dns-query", 5)),
    )

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String): List<InetAddress> {
        for (resolver in resolvers) {
            try {
                Log.i(TAG, "Trying resolver: ${resolver.url}")
                val dnsManager = DnsManager(NetworkInfo.normal, arrayOf(resolver.resolver))
                val ips = dnsManager.queryRecords(hostname)
                if (!ips.isNullOrEmpty()) {
                    val result = mutableListOf<InetAddress>()
                    for (ip in ips) {
                        Log.i(TAG, "lookup hostname ip : $ip")
                        result.addAll(InetAddress.getAllByName(ip.value))
                    }
                    Log.i(TAG, "Dns result from ${resolver.url}: $result")
                    return result
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error using resolver ${resolver.url}: $e")
            }
        }
        // 所有备用DNS服务器都出错后使用默认解析
        Log.i(TAG, "All custom DNS resolvers failed, using system default DNS")
        return Dns.SYSTEM.lookup(hostname)
    }
}
