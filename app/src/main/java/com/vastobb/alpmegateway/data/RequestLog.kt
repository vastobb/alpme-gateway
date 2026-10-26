package com.vastobb.alpmegateway.data

import java.util.UUID

data class RequestLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val keyName: String,
    val requestedModel: String,
    val resolvedModel: String,
    val status: Int,
    val latencyMs: Long,
    val stream: Boolean,
    val path: String,
    val attempts: Int = 1,
    val fallback: Boolean = false,
    val error: String? = null,
) {
    val isSuccess: Boolean get() = status in 200..299
}
