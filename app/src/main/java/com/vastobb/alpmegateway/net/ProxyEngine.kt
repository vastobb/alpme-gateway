package com.vastobb.alpmegateway.net

import com.vastobb.alpmegateway.AppContainer
import com.vastobb.alpmegateway.data.ApiKeyConfig
import com.vastobb.alpmegateway.data.GatewaySettings
import com.vastobb.alpmegateway.data.KeyRuntime
import com.vastobb.alpmegateway.data.RequestLog
import com.vastobb.alpmegateway.data.RoutingStrategy
import fi.iki.elonen.NanoHTTPD
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

class ProxyEngine(private val container: AppContainer) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val roundRobin = AtomicInteger(0)
    private var server: NanoHTTPD? = null
    private var startedAt: Long = 0
    @Volatile private var upstreamClient: OkHttpClient? = null
    @Volatile private var upstreamTimeout: Int = -1

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _keyRuntime = MutableStateFlow<Map<String, KeyRuntime>>(emptyMap())
    val keyRuntime: StateFlow<Map<String, KeyRuntime>> = _keyRuntime.asStateFlow()

    @Synchronized
    fun start(settings: GatewaySettings = container.settings.value): Boolean {
        if (_isRunning.value) return true
        _lastError.value = null
        return try {
            val host = if (settings.bindLan) "0.0.0.0" else "127.0.0.1"
            val instance = object : NanoHTTPD(host, settings.port) {
                override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response = handle(session)
            }
            instance.start(0, false)
            server = instance
            startedAt = System.currentTimeMillis()
            _isRunning.value = true
            true
        } catch (t: Throwable) {
            _lastError.value = t.message ?: "Could not start the gateway on port ${settings.port}"
            _isRunning.value = false
            false
        }
    }

    @Synchronized
    fun stop() {
        try {
            server?.stop()
        } catch (_: Throwable) {
        }
        server = null
        _isRunning.value = false
        startedAt = 0
    }

    @Synchronized
    fun restart(settings: GatewaySettings = container.settings.value): Boolean {
        stop()
        return start(settings)
    }

    fun endpoint(settings: GatewaySettings = container.settings.value): String =
        NetworkUtils.endpoint(settings)

    fun currentEndpoint(): String = NetworkUtils.endpoint(container.settings.value)

    private fun handle(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val path = session.uri.substringBefore('?')
        val cors = corsHeaders()

        if (session.method == NanoHTTPD.Method.OPTIONS) {
            return NanoHTTPD.newFixedLengthResponse(
                NanoHTTPD.Response.Status.NO_CONTENT,
                "text/plain",
                "",
            ).apply { cors.forEach { addHeader(it.first, it.second) } }
        }

        if (path == "/health") {
            val settings = container.settings.value
            val body = buildJsonObject {
                put("status", if (_isRunning.value) "ok" else "stopped")
                put("endpoint", endpoint(settings))
                put("keys", container.keyStore.keys.value.count { it.enabled })
                put("version", "1.0.0")
                put("uptime_seconds", if (startedAt == 0L) 0 else (System.currentTimeMillis() - startedAt) / 1000)
            }.toString()
            return fixed(200, "application/json", body, cors)
        }

        val settings = container.settings.value
        if (!isAuthorized(session, settings)) {
            return error(
                status = 401,
                message = "Invalid gateway token. Use the token shown in the app as the API key.",
                code = "invalid_gateway_token",
                cors = cors,
            )
        }

        return when {
            path == "/v1/models" && session.method == NanoHTTPD.Method.GET -> modelsResponse(settings, cors)
            path == "/v1/chat/completions" && session.method == NanoHTTPD.Method.POST ->
                chatCompletions(session, settings, cors)
            path == "/v1/responses" || path == "/v1/messages" ->
                error(
                    status = 404,
                    message = "This gateway exposes the OpenAI-compatible /v1/chat/completions endpoint.",
                    code = "unsupported_endpoint",
                    cors = cors,
                )
            else -> error(
                status = 404,
                message = "Unknown endpoint: $path",
                code = "not_found",
                cors = cors,
            )
        }
    }

    private fun isAuthorized(session: NanoHTTPD.IHTTPSession, settings: GatewaySettings): Boolean {
        val expected = settings.accessToken
        if (expected.isBlank()) return false
        val auth = session.headers["authorization"].orEmpty()
        val bearer = if (auth.startsWith("Bearer ", ignoreCase = true)) auth.substring(7).trim() else auth.trim()
        val apiKey = session.headers["x-api-key"].orEmpty().trim()
        return bearer == expected || apiKey == expected
    }

    private fun modelsResponse(
        settings: GatewaySettings,
        cors: List<Pair<String, String>>,
    ): NanoHTTPD.Response {
        val catalog = container.modelCatalog.models.value
        val seen = LinkedHashSet<String>()
        seen += "auto"
        seen += "universal"
        if (settings.universalModel.isNotBlank()) seen += settings.universalModel
        if (settings.enableFallback && settings.fallbackModel.isNotBlank()) seen += settings.fallbackModel
        catalog.forEach { seen += it.id }
        val data = buildJsonArray {
            seen.forEach { id ->
                add(
                    buildJsonObject {
                        put("id", id)
                        put("object", "model")
                        put("created", 0)
                        put("owned_by", "alpme-gateway")
                    },
                )
            }
        }
        val body = buildJsonObject {
            put("object", "list")
            put("data", data)
        }.toString()
        return fixed(200, "application/json", body, cors)
    }

    private fun chatCompletions(
        session: NanoHTTPD.IHTTPSession,
        settings: GatewaySettings,
        cors: List<Pair<String, String>>,
    ): NanoHTTPD.Response {
        val bytes = readBody(session) ?: return error(
            status = 411,
            message = "Content-Length is required.",
            code = "length_required",
            cors = cors,
        )
        val body = runCatching { json.parseToJsonElement(bytes.decodeToString()).jsonObject }
            .getOrElse {
                return error(400, "Request body must be valid JSON.", "invalid_json", cors)
            }

        val requestedModel = runCatching {
            body["model"]?.jsonPrimitive?.contentOrNull
        }.getOrNull().orEmpty()
        val stream = runCatching {
            body["stream"]?.jsonPrimitive?.booleanOrNull
        }.getOrNull() ?: false

        val enabledKeys = container.keyStore.keys.value.filter { it.enabled && it.key.isNotBlank() }
        if (enabledKeys.isEmpty()) {
            container.logStore.add(
                RequestLog(
                    keyName = "No key",
                    requestedModel = requestedModel,
                    resolvedModel = "—",
                    status = 503,
                    latencyMs = 0,
                    stream = stream,
                    path = "/v1/chat/completions",
                    error = "No enabled Alpme keys",
                ),
            )
            return error(
                status = 503,
                message = "No enabled Alpme keys. Add at least one key in the app.",
                code = "no_keys",
                cors = cors,
            )
        }

        val orderedKeys = orderKeys(enabledKeys, settings)
        val maxAttempts = (settings.retryCount + 1).coerceAtLeast(1)
        val started = System.currentTimeMillis()
        var lastStatus = 503
        var lastError = "Alpme did not return a response."
        var lastKey: ApiKeyConfig? = null
        var lastModel = requestedModel
        var attemptNo = 0
        var usedFallback = false
        val attemptDetails = mutableListOf<String>()

        fun attempt(
            keys: List<ApiKeyConfig>,
            forceModel: String?,
            attemptsAllowed: Int,
        ): NanoHTTPD.Response? {
            var attemptIndex = 0
            while (attemptIndex < attemptsAllowed) {
                val key = keys[attemptIndex % keys.size]
                attemptIndex++
                attemptNo++
                val resolved = resolveModel(key, settings, requestedModel, forceModel)
                lastKey = key
                lastModel = resolved
                val payload = withModel(body, resolved).toString()
                val attemptStarted = System.currentTimeMillis()
                val result = callUpstream(
                    key = key,
                    payload = payload,
                    stream = stream,
                    settings = settings,
                )
                val latency = System.currentTimeMillis() - started
                val attemptLatency = System.currentTimeMillis() - attemptStarted
                attemptDetails += buildString {
                    append("$attemptNo. ")
                    append(key.name)
                    append(" · ")
                    append(resolved)
                    append(" · HTTP ")
                    append(result.status)
                    append(" · ")
                    append(attemptLatency)
                    append("ms")
                    if (forceModel != null) append(" · fallback")
                    if (result.response == null && !result.error.isNullOrBlank()) {
                        append(" · ")
                        append(result.error)
                    }
                }
                lastStatus = result.status
                val upstream = result.response
                when {
                    upstream != null && upstream.isSuccessful -> {
                        recordRuntime(key, result.status, attemptLatency, success = true)
                        val upstreamBody = upstream.body
                        if (stream && upstreamBody != null) {
                            val response = NanoHTTPD.newChunkedResponse(
                                customStatus(200, "OK"),
                                upstream.header("Content-Type", "text/event-stream"),
                                upstreamBody.byteStream(),
                            )
                            response.addHeader("Cache-Control", "no-cache")
                            response.addHeader("X-Alpme-Key", key.name)
                            response.addHeader("X-Alpme-Model", resolved)
                            if (usedFallback) response.addHeader("X-Alpme-Fallback", "true")
                            cors.forEach { response.addHeader(it.first, it.second) }
                            log(
                                key = key,
                                requestedModel = requestedModel,
                                resolvedModel = resolved,
                                status = 200,
                                latencyMs = latency,
                                stream = true,
                                attempts = attemptNo,
                                fallback = usedFallback,
                                attemptDetails = attemptDetails.joinToString("\n"),
                            )
                            return response
                        }
                        val responseBody = upstreamBody?.string().orEmpty()
                        upstream.close()
                        val response = fixed(result.status, "application/json", responseBody, cors)
                        response.addHeader("X-Alpme-Key", key.name)
                        response.addHeader("X-Alpme-Model", resolved)
                        if (usedFallback) response.addHeader("X-Alpme-Fallback", "true")
                        log(
                            key = key,
                            requestedModel = requestedModel,
                            resolvedModel = resolved,
                            status = result.status,
                            latencyMs = latency,
                            stream = false,
                            attempts = attemptNo,
                            fallback = usedFallback,
                            attemptDetails = attemptDetails.joinToString("\n"),
                        )
                        return response
                    }
                    upstream != null -> {
                        val errorBody = runCatching { upstream.body?.string().orEmpty() }.getOrDefault("")
                        lastError = extractError(errorBody) ?: "HTTP ${result.status}"
                        upstream.close()
                        recordRuntime(key, result.status, attemptLatency, success = false)
                    }
                    else -> {
                        lastError = result.error ?: "Network error"
                        recordRuntime(key, result.status, attemptLatency, success = false)
                    }
                }
                val retryNextKey = result.status in RETRYABLE_STATUS || result.status in AUTH_STATUS
                if (!retryNextKey) break
                if (attemptIndex < attemptsAllowed && result.status in RETRYABLE_STATUS) {
                    val backoffMs = when (result.status) {
                        503, 429 -> minOf(750L * (1L shl (attemptIndex - 1).coerceAtMost(3)), 4_000L)
                        else -> 250L
                    }
                    try {
                        Thread.sleep(backoffMs)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        break
                    }
                }
            }
            return null
        }

        val firstPass = attempt(orderedKeys, null, maxAttempts)
        if (firstPass != null) return firstPass

        if (settings.enableFallback && settings.fallbackModel.isNotBlank() &&
            settings.fallbackModel != lastModel && lastStatus in RETRYABLE_STATUS
        ) {
            usedFallback = true
            val fallbackKeys = orderKeys(enabledKeys, settings)
            val secondPass = attempt(fallbackKeys, settings.fallbackModel, maxAttempts)
            if (secondPass != null) return secondPass
        }

        val latency = System.currentTimeMillis() - started
        val key = lastKey
        if (key != null) {
            log(
                key = key,
                requestedModel = requestedModel,
                resolvedModel = lastModel,
                status = lastStatus,
                latencyMs = latency,
                stream = stream,
                attempts = attemptNo,
                fallback = usedFallback,
                error = lastError,
                attemptDetails = attemptDetails.joinToString("\n"),
            )
        }
        return error(
            status = lastStatus.takeIf { it in 400..599 } ?: 503,
            message = "All Alpme attempts failed: $lastError",
            code = "all_attempts_failed",
            cors = cors,
        )
    }

    private data class UpstreamResult(
        val status: Int,
        val response: Response? = null,
        val error: String? = null,
    )

    @Synchronized
    private fun clientFor(settings: GatewaySettings): OkHttpClient {
        val existing = upstreamClient
        if (existing != null && upstreamTimeout == settings.timeoutSeconds) return existing
        val created = HttpClients.upstream(settings.timeoutSeconds)
        upstreamClient = created
        upstreamTimeout = settings.timeoutSeconds
        return created
    }

    private fun callUpstream(
        key: ApiKeyConfig,
        payload: String,
        stream: Boolean,
        settings: GatewaySettings,
    ): UpstreamResult {
        return try {
            val request = Request.Builder()
                .url("https://alpme.com/v1/chat/completions")
                .header("Authorization", "Bearer ${key.key}")
                .header("User-Agent", "Alpme/1.0.0")
                .header("Content-Type", "application/json")
                .header("Accept", if (stream) "text/event-stream" else "application/json")
                .header("Accept-Encoding", "identity")
                .post(payload.toRequestBody("application/json".toMediaType()))
                .build()
            val response = clientFor(settings).newCall(request).execute()
            UpstreamResult(status = response.code, response = response)
        } catch (t: Throwable) {
            UpstreamResult(status = 502, error = t.message ?: "Network error")
        }
    }

    private fun orderKeys(keys: List<ApiKeyConfig>, settings: GatewaySettings): List<ApiKeyConfig> {
        return when (settings.routingStrategy) {
            RoutingStrategy.ROUND_ROBIN -> {
                val raw = roundRobin.getAndIncrement()
                val start = ((raw % keys.size) + keys.size) % keys.size
                List(keys.size) { index -> keys[(start + index) % keys.size] }
            }
            RoutingStrategy.FAILOVER -> keys
            RoutingStrategy.LEAST_USED -> keys.sortedBy { key ->
                _keyRuntime.value[key.id]?.requestCount ?: 0
            }
        }
    }

    private fun resolveModel(
        key: ApiKeyConfig,
        settings: GatewaySettings,
        requestedModel: String,
        forced: String?,
    ): String {
        if (!forced.isNullOrBlank()) return forced
        key.model?.takeIf { it.isNotBlank() }?.let { return it }
        if (settings.forceUniversalModel && settings.universalModel.isNotBlank()) {
            return settings.universalModel
        }
        if (isAutoAlias(requestedModel)) {
            return settings.universalModel.ifBlank { DEFAULT_MODEL }
        }
        return requestedModel
    }

    private fun isAutoAlias(model: String): Boolean {
        return model.isBlank() ||
            model == "auto" ||
            model == "universal" ||
            model.endsWith("/auto") ||
            model.endsWith("/universal")
    }

    private fun withModel(body: JsonObject, model: String): JsonObject {
        return buildJsonObject {
            body.forEach { (key, value) -> put(key, value) }
            put("model", model)
        }
    }

    private fun recordRuntime(key: ApiKeyConfig, status: Int, latency: Long, success: Boolean) {
        _keyRuntime.update { current ->
            val previous = current[key.id] ?: KeyRuntime()
            current + (
                key.id to previous.copy(
                    requestCount = previous.requestCount + 1,
                    errorCount = previous.errorCount + if (success) 0 else 1,
                    lastStatus = status,
                    lastLatencyMs = latency,
                    lastUsedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    private fun log(
        key: ApiKeyConfig,
        requestedModel: String,
        resolvedModel: String,
        status: Int,
        latencyMs: Long,
        stream: Boolean,
        attempts: Int,
        fallback: Boolean,
        error: String? = null,
        attemptDetails: String? = null,
    ) {
        container.logStore.add(
            RequestLog(
                keyName = key.name,
                requestedModel = requestedModel,
                resolvedModel = resolvedModel,
                status = status,
                latencyMs = latencyMs,
                stream = stream,
                path = "/v1/chat/completions",
                attempts = attempts,
                fallback = fallback,
                error = error,
                attemptDetails = attemptDetails,
            ),
        )
    }

    private fun readBody(session: NanoHTTPD.IHTTPSession): ByteArray? {
        val length = session.headers["content-length"]?.toIntOrNull() ?: return null
        if (length <= 0) return ByteArray(0)
        val bytes = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val read = session.inputStream.read(bytes, offset, length - offset)
            if (read <= 0) break
            offset += read
        }
        return if (offset == length) bytes else bytes.copyOf(offset)
    }

    private fun extractError(body: String): String? {
        return runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            val errorElement = root["error"]
            val nested = (errorElement as? JsonObject)
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
            val direct = (errorElement as? JsonPrimitive)?.contentOrNull
            val top = (root["message"] as? JsonPrimitive)?.contentOrNull
            nested ?: top ?: direct
        }.getOrNull()
    }

    private fun fixed(
        status: Int,
        mime: String,
        body: String,
        cors: List<Pair<String, String>>,
    ): NanoHTTPD.Response {
        val response = NanoHTTPD.newFixedLengthResponse(
            customStatus(status, statusText(status)),
            mime,
            body,
        )
        cors.forEach { response.addHeader(it.first, it.second) }
        return response
    }

    private fun error(
        status: Int,
        message: String,
        code: String,
        cors: List<Pair<String, String>>,
    ): NanoHTTPD.Response {
        val body = buildJsonObject {
            put(
                "error",
                buildJsonObject {
                    put("message", message)
                    put("type", "server_error")
                    put("code", code)
                },
            )
        }.toString()
        return fixed(status, "application/json", body, cors)
    }

    private fun corsHeaders(): List<Pair<String, String>> = listOf(
        "Access-Control-Allow-Origin" to "*",
        "Access-Control-Allow-Headers" to "Authorization, Content-Type, X-API-Key",
        "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
    )

    private fun customStatus(code: Int, text: String): NanoHTTPD.Response.IStatus {
        return object : NanoHTTPD.Response.IStatus {
            override fun getRequestStatus(): Int = code
            override fun getDescription(): String = "$code $text"
        }
    }

    private fun statusText(code: Int): String = when (code) {
        200 -> "OK"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        404 -> "Not Found"
        411 -> "Length Required"
        429 -> "Too Many Requests"
        500 -> "Internal Server Error"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        504 -> "Gateway Timeout"
        else -> "Status"
    }

    private companion object {
        const val DEFAULT_MODEL = "deepseek-v4.1-flash"
        val RETRYABLE_STATUS = setOf(404, 429, 500, 502, 503, 504)
        val AUTH_STATUS = setOf(401, 403)
    }
}
