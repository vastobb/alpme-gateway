package com.vastobb.alpmegateway.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class KeyStore(context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "alpme_gateway_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _keys = MutableStateFlow(load())
    val keys: StateFlow<List<ApiKeyConfig>> = _keys.asStateFlow()

    private fun load(): List<ApiKeyConfig> {
        val raw = prefs.getString(KEY_LIST, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<ApiKeyConfig>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun persist(keys: List<ApiKeyConfig>) {
        prefs.edit().putString(KEY_LIST, json.encodeToString(keys)).apply()
        _keys.value = keys
    }

    @Synchronized
    fun add(name: String, key: String, model: String?) {
        val entry = ApiKeyConfig(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Alpme key" },
            key = key.trim(),
            model = model?.trim()?.takeIf { it.isNotBlank() },
        )
        persist(load() + entry)
    }

    @Synchronized
    fun update(updated: ApiKeyConfig) {
        persist(load().map { if (it.id == updated.id) updated else it })
    }

    @Synchronized
    fun delete(id: String) {
        persist(load().filterNot { it.id == id })
    }

    @Synchronized
    fun setEnabled(id: String, enabled: Boolean) {
        persist(load().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    @Synchronized
    fun replaceAll(keys: List<ApiKeyConfig>) {
        persist(keys)
    }

    private companion object {
        const val KEY_LIST = "api_keys_json_v1"
    }
}
