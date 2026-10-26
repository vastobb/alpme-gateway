package com.vastobb.alpmegateway.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.SecureRandom
import java.util.Base64
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.gatewayDataStore by preferencesDataStore(name = "gateway_settings")

class SettingsRepository(private val context: Context) {

    private val tokenMutex = Mutex()

    private object Keys {
        val PORT = intPreferencesKey("port")
        val BIND_LAN = booleanPreferencesKey("bind_lan")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val UNIVERSAL_MODEL = stringPreferencesKey("universal_model")
        val FORCE_UNIVERSAL = booleanPreferencesKey("force_universal")
        val ENABLE_FALLBACK = booleanPreferencesKey("enable_fallback")
        val FALLBACK_MODEL = stringPreferencesKey("fallback_model")
        val ROUTING = stringPreferencesKey("routing")
        val RETRY_COUNT = intPreferencesKey("retry_count")
        val TIMEOUT_SECONDS = intPreferencesKey("timeout_seconds")
        val THEME = stringPreferencesKey("theme")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val AUTO_START_PROXY = booleanPreferencesKey("auto_start_proxy")
    }

    val settings: Flow<GatewaySettings> = context.gatewayDataStore.data.map { prefs ->
        GatewaySettings(
            port = prefs[Keys.PORT] ?: 8787,
            bindLan = prefs[Keys.BIND_LAN] ?: true,
            accessToken = prefs[Keys.ACCESS_TOKEN].orEmpty(),
            universalModel = prefs[Keys.UNIVERSAL_MODEL] ?: "deepseek-v4.1-flash",
            forceUniversalModel = prefs[Keys.FORCE_UNIVERSAL] ?: true,
            enableFallback = prefs[Keys.ENABLE_FALLBACK] ?: true,
            fallbackModel = prefs[Keys.FALLBACK_MODEL] ?: "glm-5.3-flash",
            routingStrategy = prefs[Keys.ROUTING]
                ?.let { runCatching { RoutingStrategy.valueOf(it) }.getOrNull() }
                ?: RoutingStrategy.ROUND_ROBIN,
            retryCount = (prefs[Keys.RETRY_COUNT] ?: 2).coerceIn(0, 5),
            timeoutSeconds = (prefs[Keys.TIMEOUT_SECONDS] ?: 300).coerceIn(30, 900),
            theme = prefs[Keys.THEME]
                ?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                ?: AppThemeMode.MIDNIGHT,
            startOnBoot = prefs[Keys.START_ON_BOOT] ?: false,
            autoStartProxy = prefs[Keys.AUTO_START_PROXY] ?: false,
        )
    }

    suspend fun current(): GatewaySettings = settings.first()

    suspend fun ensureAccessToken() {
        tokenMutex.withLock {
            if (current().accessToken.isBlank()) {
                update { it.copy(accessToken = newToken()) }
            }
        }
    }

    suspend fun regenerateAccessToken() {
        tokenMutex.withLock {
            update { it.copy(accessToken = newToken()) }
        }
    }

    suspend fun update(transform: (GatewaySettings) -> GatewaySettings) {
        context.gatewayDataStore.edit { prefs ->
            val current = GatewaySettings(
                port = prefs[Keys.PORT] ?: 8787,
                bindLan = prefs[Keys.BIND_LAN] ?: true,
                accessToken = prefs[Keys.ACCESS_TOKEN].orEmpty(),
                universalModel = prefs[Keys.UNIVERSAL_MODEL] ?: "deepseek-v4.1-flash",
                forceUniversalModel = prefs[Keys.FORCE_UNIVERSAL] ?: true,
                enableFallback = prefs[Keys.ENABLE_FALLBACK] ?: true,
                fallbackModel = prefs[Keys.FALLBACK_MODEL] ?: "glm-5.3-flash",
                routingStrategy = prefs[Keys.ROUTING]
                    ?.let { runCatching { RoutingStrategy.valueOf(it) }.getOrNull() }
                    ?: RoutingStrategy.ROUND_ROBIN,
                retryCount = (prefs[Keys.RETRY_COUNT] ?: 2).coerceIn(0, 5),
                timeoutSeconds = (prefs[Keys.TIMEOUT_SECONDS] ?: 300).coerceIn(30, 900),
                theme = prefs[Keys.THEME]
                    ?.let { runCatching { AppThemeMode.valueOf(it) }.getOrNull() }
                    ?: AppThemeMode.MIDNIGHT,
                startOnBoot = prefs[Keys.START_ON_BOOT] ?: false,
                autoStartProxy = prefs[Keys.AUTO_START_PROXY] ?: false,
            )
            val next = transform(current)
            prefs[Keys.PORT] = next.port.coerceIn(1024, 65535)
            prefs[Keys.BIND_LAN] = next.bindLan
            prefs[Keys.ACCESS_TOKEN] = next.accessToken
            prefs[Keys.UNIVERSAL_MODEL] = next.universalModel
            prefs[Keys.FORCE_UNIVERSAL] = next.forceUniversalModel
            prefs[Keys.ENABLE_FALLBACK] = next.enableFallback
            prefs[Keys.FALLBACK_MODEL] = next.fallbackModel
            prefs[Keys.ROUTING] = next.routingStrategy.name
            prefs[Keys.RETRY_COUNT] = next.retryCount.coerceIn(0, 5)
            prefs[Keys.TIMEOUT_SECONDS] = next.timeoutSeconds.coerceIn(30, 900)
            prefs[Keys.THEME] = next.theme.name
            prefs[Keys.START_ON_BOOT] = next.startOnBoot
            prefs[Keys.AUTO_START_PROXY] = next.autoStartProxy
        }
    }

    fun newToken(): String {
        val bytes = ByteArray(24)
        SecureRandom().nextBytes(bytes)
        return "sk_gw_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }
}
