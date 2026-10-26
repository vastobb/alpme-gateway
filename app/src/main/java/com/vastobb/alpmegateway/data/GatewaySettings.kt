package com.vastobb.alpmegateway.data

enum class RoutingStrategy {
    ROUND_ROBIN,
    FAILOVER,
    LEAST_USED,
}

enum class AppThemeMode {
    MIDNIGHT,
    GRAPHITE,
    WARM,
}

data class GatewaySettings(
    val port: Int = 8787,
    val bindLan: Boolean = true,
    val accessToken: String = "",
    val universalModel: String = "deepseek-v4.1-flash",
    val forceUniversalModel: Boolean = true,
    val enableFallback: Boolean = true,
    val fallbackModel: String = "deepseek-v4.1-flash",
    val routingStrategy: RoutingStrategy = RoutingStrategy.ROUND_ROBIN,
    val retryCount: Int = 2,
    val timeoutSeconds: Int = 300,
    val theme: AppThemeMode = AppThemeMode.MIDNIGHT,
    val startOnBoot: Boolean = false,
    val autoStartProxy: Boolean = false,
) {
    val endpointPath: String get() = "/v1"
}
