package com.vastobb.alpmegateway.net

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

data class KeyTestResult(
    val ok: Boolean,
    val status: Int?,
    val latencyMs: Long,
    val message: String,
)

object KeyTester {

    suspend fun test(apiKey: String): KeyTestResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url("https://alpme.com/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "Alpme/1.0.0")
                .header("Accept", "application/json")
                .get()
                .build()
            HttpClients.data.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    KeyTestResult(true, response.code, latency, "Key accepted")
                } else {
                    KeyTestResult(false, response.code, latency, extractMessage(body) ?: "HTTP ${response.code}")
                }
            }
        } catch (t: Throwable) {
            KeyTestResult(
                ok = false,
                status = null,
                latencyMs = System.currentTimeMillis() - start,
                message = t.message ?: "Network error",
            )
        }
    }

    private fun extractMessage(body: String): String? {
        val marker = "\"message\""
        val index = body.indexOf(marker)
        if (index < 0) return null
        val start = body.indexOf('"', index + marker.length + 1)
        if (start < 0) return null
        val end = body.indexOf('"', start + 1)
        if (end < 0) return null
        return body.substring(start + 1, end)
    }
}
