package com.vastobb.alpmegateway.data

import kotlinx.serialization.Serializable

@Serializable
data class ApiKeyConfig(
    val id: String,
    val name: String,
    val key: String,
    val model: String? = null,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
)

data class KeyRuntime(
    val requestCount: Int = 0,
    val errorCount: Int = 0,
    val lastStatus: Int? = null,
    val lastLatencyMs: Long? = null,
    val lastUsedAt: Long? = null,
) {
    val successRate: Float
        get() = if (requestCount == 0) 0f else (requestCount - errorCount).toFloat() / requestCount
}

fun maskKey(key: String): String {
    if (key.length <= 12) return "••••••••"
    return key.take(8) + "••••••••" + key.takeLast(4)
}
