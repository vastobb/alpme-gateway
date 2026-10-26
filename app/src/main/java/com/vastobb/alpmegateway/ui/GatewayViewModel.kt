package com.vastobb.alpmegateway.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vastobb.alpmegateway.AppContainer
import com.vastobb.alpmegateway.data.ApiKeyConfig
import com.vastobb.alpmegateway.data.AppThemeMode
import com.vastobb.alpmegateway.data.GatewaySettings
import com.vastobb.alpmegateway.data.KeyRuntime
import com.vastobb.alpmegateway.data.RoutingStrategy
import com.vastobb.alpmegateway.net.KeyTestResult
import com.vastobb.alpmegateway.net.KeyTester
import com.vastobb.alpmegateway.net.NetworkUtils
import com.vastobb.alpmegateway.service.ProxyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class KeyRow(
    val config: ApiKeyConfig,
    val runtime: KeyRuntime,
)

data class GatewayStats(
    val requests: Int = 0,
    val successes: Int = 0,
    val errors: Int = 0,
    val avgLatencyMs: Long = 0,
    val activeKeys: Int = 0,
) {
    val successRate: Int
        get() = if (requests == 0) 100 else ((successes.toFloat() / requests) * 100).toInt()
}

class GatewayViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<GatewaySettings> = container.settings
    val keys: StateFlow<List<ApiKeyConfig>> = container.keyStore.keys
    val logs = container.logStore.logs
    val isRunning = container.proxyEngine.isRunning
    val lastError = container.proxyEngine.lastError
    val keyRuntime = container.proxyEngine.keyRuntime
    val catalog = container.modelCatalog.models
    val catalogLoading = container.modelCatalog.loading
    val catalogError = container.modelCatalog.error

    val endpoint: StateFlow<String> = settings
        .map { withContext(Dispatchers.IO) { NetworkUtils.endpoint(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "http://127.0.0.1:8787/v1")

    val keyRows: StateFlow<List<KeyRow>> = combine(keys, keyRuntime) { configs, runtime ->
        configs.map { config -> KeyRow(config, runtime[config.id] ?: KeyRuntime()) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<GatewayStats> = combine(logs, keys) { entries, configs ->
        val successes = entries.count { it.isSuccess }
        val avg = if (entries.isEmpty()) 0 else entries.sumOf { it.latencyMs } / entries.size
        GatewayStats(
            requests = entries.size,
            successes = successes,
            errors = entries.size - successes,
            avgLatencyMs = avg,
            activeKeys = configs.count { it.enabled },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GatewayStats())

    init {
        viewModelScope.launch {
            container.settingsRepository.ensureAccessToken()
            refreshCatalog()
        }
    }

    fun startProxy() {
        ProxyService.start(container.appContext)
    }

    fun stopProxy() {
        ProxyService.stop(container.appContext)
    }

    fun toggleProxy() {
        if (isRunning.value) stopProxy() else startProxy()
    }

    fun refreshCatalog() {
        viewModelScope.launch {
            val key = keys.value.firstOrNull { it.enabled && it.key.isNotBlank() }?.key
                ?: keys.value.firstOrNull { it.key.isNotBlank() }?.key
            if (key != null) container.modelCatalog.refresh(key)
        }
    }

    suspend fun testKey(key: String): KeyTestResult = KeyTester.test(key)

    fun addKey(name: String, key: String, model: String?) {
        if (key.isBlank()) return
        container.keyStore.add(name, key, model)
        refreshCatalog()
    }

    fun updateKey(key: ApiKeyConfig) {
        container.keyStore.update(key)
        refreshCatalog()
    }

    fun deleteKey(id: String) {
        container.keyStore.delete(id)
    }

    fun setKeyEnabled(id: String, enabled: Boolean) {
        container.keyStore.setEnabled(id, enabled)
    }

    fun setUniversalModel(model: String) = updateSettings { it.copy(universalModel = model) }

    fun setForceUniversalModel(enabled: Boolean) =
        updateSettings { it.copy(forceUniversalModel = enabled) }

    fun setEnableFallback(enabled: Boolean) =
        updateSettings { it.copy(enableFallback = enabled) }

    fun setFallbackModel(model: String) = updateSettings { it.copy(fallbackModel = model) }

    fun setRoutingStrategy(strategy: RoutingStrategy) =
        updateSettings { it.copy(routingStrategy = strategy) }

    fun setRetryCount(count: Int) = updateSettings { it.copy(retryCount = count.coerceIn(0, 5)) }

    fun setTimeoutSeconds(seconds: Int) =
        updateSettings { it.copy(timeoutSeconds = seconds.coerceIn(30, 900)) }

    fun setPort(port: Int) = updateSettings(restart = true) { it.copy(port = port.coerceIn(1024, 65535)) }

    fun setBindLan(enabled: Boolean) = updateSettings(restart = true) { it.copy(bindLan = enabled) }

    fun setAccessToken(token: String) = updateSettings { it.copy(accessToken = token.trim()) }

    fun regenerateToken() {
        viewModelScope.launch {
            container.settingsRepository.update { it.copy(accessToken = container.settingsRepository.newToken()) }
        }
    }

    fun setTheme(theme: AppThemeMode) = updateSettings { it.copy(theme = theme) }

    fun setStartOnBoot(enabled: Boolean) = updateSettings { it.copy(startOnBoot = enabled) }

    fun setAutoStartProxy(enabled: Boolean) = updateSettings { it.copy(autoStartProxy = enabled) }

    fun clearLogs() = container.logStore.clear()

    private fun updateSettings(restart: Boolean = false, transform: (GatewaySettings) -> GatewaySettings) {
        viewModelScope.launch {
            container.settingsRepository.update(transform)
            if (restart && container.proxyEngine.isRunning.value) {
                container.proxyEngine.restart(container.settingsRepository.current())
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { GatewayViewModel(container) }
        }
    }
}
