package com.vastobb.alpmegateway

import android.content.Context
import com.vastobb.alpmegateway.data.GatewaySettings
import com.vastobb.alpmegateway.data.KeyStore
import com.vastobb.alpmegateway.data.LogStore
import com.vastobb.alpmegateway.data.ModelCatalogRepository
import com.vastobb.alpmegateway.data.SettingsRepository
import com.vastobb.alpmegateway.net.ProxyEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settingsRepository = SettingsRepository(appContext)

    val settings = settingsRepository.settings.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = GatewaySettings(),
    )

    val keyStore = KeyStore(appContext)
    val modelCatalog = ModelCatalogRepository()
    val logStore = LogStore()

    val proxyEngine: ProxyEngine by lazy { ProxyEngine(this) }
}
