package com.vastobb.alpmegateway.data

import com.vastobb.alpmegateway.net.HttpClients
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.Request

class ModelCatalogRepository {

    private val json = Json { ignoreUnknownKeys = true }

    private val _models = MutableStateFlow<List<AlpmeModel>>(emptyList())
    val models: StateFlow<List<AlpmeModel>> = _models.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun refresh(apiKey: String): Result<List<AlpmeModel>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            val message = "Add an Alpme key to load the model catalog."
            _error.value = message
            return@withContext Result.failure(IllegalStateException(message))
        }
        _loading.value = true
        _error.value = null
        try {
            val request = Request.Builder()
                .url("https://alpme.com/v1/models")
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "Alpme/1.0.0")
                .header("Accept", "application/json")
                .get()
                .build()
            HttpClients.data.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val message = "Model catalog returned HTTP ${response.code}"
                    _error.value = message
                    return@withContext Result.failure(IOException(message))
                }
                val body = response.body?.string().orEmpty()
                val parsed = parseModels(body)
                _models.value = parsed
                Result.success(parsed)
            }
        } catch (t: Throwable) {
            val message = t.message ?: "Could not load model catalog"
            _error.value = message
            Result.failure(t)
        } finally {
            _loading.value = false
        }
    }

    private fun parseModels(body: String): List<AlpmeModel> {
        val root = json.parseToJsonElement(body).jsonObject
        val data = root["data"]?.jsonArray ?: return emptyList()
        return data.mapNotNull { element ->
            val obj = element.jsonObject
            val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val pricing = obj["pricing"]?.takeIf { it !is JsonNull }?.jsonObject
            AlpmeModel(
                id = id,
                displayName = obj["display_name"]?.jsonPrimitive?.contentOrNull ?: id,
                contextWindow = obj["context_window"]?.jsonPrimitive?.longOrNull,
                maxOutputTokens = obj["max_output_tokens"]?.jsonPrimitive?.longOrNull,
                ownedBy = obj["owned_by"]?.jsonPrimitive?.contentOrNull,
                inputPrice = pricing?.get("input")?.jsonPrimitive?.doubleOrNull,
                outputPrice = pricing?.get("output")?.jsonPrimitive?.doubleOrNull,
                cachedInputPrice = pricing?.get("cached_input")?.jsonPrimitive?.doubleOrNull,
            )
        }.sortedBy { it.id }
    }
}
