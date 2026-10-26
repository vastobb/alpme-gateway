package com.vastobb.alpmegateway.net

import com.vastobb.alpmegateway.data.GatewaySettings
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    fun localIpv4(): String? {
        return runCatching {
            Collections.list(NetworkInterface.getNetworkInterfaces())
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .sortedBy { nic ->
                    when {
                        nic.name.startsWith("wlan") -> 0
                        nic.name.startsWith("ap") -> 1
                        nic.name.startsWith("eth") -> 2
                        else -> 3
                    }
                }
                .flatMap { nic -> Collections.list(nic.inetAddresses).asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }
                ?.hostAddress
        }.getOrNull()
    }

    fun endpoint(settings: GatewaySettings): String {
        val host = if (settings.bindLan) localIpv4() ?: "127.0.0.1" else "127.0.0.1"
        return "http://$host:${settings.port}/v1"
    }
}
