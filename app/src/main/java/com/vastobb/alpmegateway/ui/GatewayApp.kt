package com.vastobb.alpmegateway.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vastobb.alpmegateway.ui.components.StatusPill
import com.vastobb.alpmegateway.ui.screens.ConnectSheet
import com.vastobb.alpmegateway.ui.screens.DashboardScreen
import com.vastobb.alpmegateway.ui.screens.KeysScreen
import com.vastobb.alpmegateway.ui.screens.LogsScreen
import com.vastobb.alpmegateway.ui.screens.RoutingScreen
import com.vastobb.alpmegateway.ui.screens.SettingsScreen
import com.vastobb.alpmegateway.ui.theme.LocalAlpmePalette

enum class Destination(
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD("Home", Icons.Rounded.Dashboard),
    KEYS("Keys", Icons.Rounded.Key),
    ROUTING("Routing", Icons.Rounded.Route),
    LOGS("Logs", Icons.Rounded.Terminal),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GatewayApp(viewModel: GatewayViewModel) {
    val palette = LocalAlpmePalette.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val endpoint by viewModel.endpoint.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val keyRows by viewModel.keyRows.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val catalog by viewModel.catalog.collectAsStateWithLifecycle()
    val catalogLoading by viewModel.catalogLoading.collectAsStateWithLifecycle()
    val catalogError by viewModel.catalogError.collectAsStateWithLifecycle()

    var destination by rememberSaveable { mutableStateOf(Destination.DASHBOARD) }
    var showConnect by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(lastError) {
        lastError?.let { snackbar.showSnackbar(it) }
    }

    LaunchedEffect(settings.autoStartProxy) {
        if (settings.autoStartProxy && !isRunning) {
            viewModel.startProxy()
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 760.dp
        Scaffold(
            containerColor = palette.appBg,
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                GatewayTopBar(
                    endpoint = endpoint,
                    isRunning = isRunning,
                    onToggle = viewModel::toggleProxy,
                )
            },
            bottomBar = {
                if (!wide) {
                    GatewayBottomBar(
                        destination = destination,
                        onSelect = { destination = it },
                    )
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                if (wide) {
                    NavigationRail(
                        containerColor = palette.sidebarBg,
                        modifier = Modifier.fillMaxSize().width(84.dp),
                    ) {
                        Spacer(Modifier.height(18.dp))
                        Destination.entries.forEach { item ->
                            NavigationRailItem(
                                selected = destination == item,
                                onClick = { destination = item },
                                icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(22.dp)) },
                                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = palette.btnText,
                                    selectedTextColor = palette.textPrimary,
                                    indicatorColor = palette.btnBg,
                                    unselectedIconColor = palette.textSecondary,
                                    unselectedTextColor = palette.textSecondary,
                                ),
                            )
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    when (destination) {
                        Destination.DASHBOARD -> DashboardScreen(
                            settings = settings,
                            isRunning = isRunning,
                            endpoint = endpoint,
                            stats = stats,
                            keys = keyRows,
                            logs = logs,
                            onConnect = { showConnect = true },
                            onOpenKeys = { destination = Destination.KEYS },
                            onOpenRouting = { destination = Destination.ROUTING },
                            onOpenLogs = { destination = Destination.LOGS },
                            onToggle = viewModel::toggleProxy,
                        )
                        Destination.KEYS -> KeysScreen(
                            keys = keyRows,
                            settings = settings,
                            catalog = catalog,
                            catalogLoading = catalogLoading,
                            catalogError = catalogError,
                            onAddKey = viewModel::addKey,
                            onUpdateKey = viewModel::updateKey,
                            onDeleteKey = viewModel::deleteKey,
                            onToggleKey = viewModel::setKeyEnabled,
                            onTestKey = viewModel::testKey,
                            onRefreshCatalog = viewModel::refreshCatalog,
                        )
                        Destination.ROUTING -> RoutingScreen(
                            settings = settings,
                            catalog = catalog,
                            catalogLoading = catalogLoading,
                            catalogError = catalogError,
                            onUniversalModel = viewModel::setUniversalModel,
                            onForceUniversal = viewModel::setForceUniversalModel,
                            onEnableFallback = viewModel::setEnableFallback,
                            onFallbackModel = viewModel::setFallbackModel,
                            onRoutingStrategy = viewModel::setRoutingStrategy,
                            onRetryCount = viewModel::setRetryCount,
                            onTimeout = viewModel::setTimeoutSeconds,
                            onPort = viewModel::setPort,
                            onBindLan = viewModel::setBindLan,
                            onAccessToken = viewModel::setAccessToken,
                            onRegenerateToken = viewModel::regenerateToken,
                            onRefreshCatalog = viewModel::refreshCatalog,
                        )
                        Destination.LOGS -> LogsScreen(
                            logs = logs,
                            onClear = viewModel::clearLogs,
                        )
                        Destination.SETTINGS -> SettingsScreen(
                            settings = settings,
                            isRunning = isRunning,
                            endpoint = endpoint,
                            onTheme = viewModel::setTheme,
                            onStartOnBoot = viewModel::setStartOnBoot,
                            onAutoStartProxy = viewModel::setAutoStartProxy,
                            onClearLogs = viewModel::clearLogs,
                        )
                    }
                }
            }
        }
    }

    if (showConnect) {
        ConnectSheet(
            endpoint = endpoint,
            token = settings.accessToken,
            onDismiss = { showConnect = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GatewayTopBar(
    endpoint: String,
    isRunning: Boolean,
    onToggle: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = palette.appBg,
            titleContentColor = palette.textPrimary,
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(palette.btnBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = palette.btnText,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Alpme Gateway",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isRunning) "Universal endpoint active" else "Local multi-key gateway",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                        maxLines = 1,
                    )
                }
            }
        },
        actions = {
            StatusPill(
                text = if (isRunning) "Running" else "Stopped",
                color = if (isRunning) palette.success else palette.textSecondary,
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = onToggle,
                modifier = Modifier.size(38.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isRunning) palette.error.copy(alpha = 0.16f) else palette.btnBg,
                    contentColor = if (isRunning) palette.error else palette.btnText,
                ),
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                    contentDescription = if (isRunning) "Stop gateway" else "Start gateway",
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
        },
    )
}

@Composable
private fun GatewayBottomBar(
    destination: Destination,
    onSelect: (Destination) -> Unit,
) {
    val palette = LocalAlpmePalette.current
    NavigationBar(
        containerColor = palette.sidebarBg,
        tonalElevation = 0.dp,
    ) {
        Destination.entries.forEach { item ->
            NavigationBarItem(
                selected = destination == item,
                onClick = { onSelect(item) },
                icon = { Icon(item.icon, contentDescription = item.label, modifier = Modifier.size(21.dp)) },
                label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.btnText,
                    selectedTextColor = palette.textPrimary,
                    indicatorColor = palette.btnBg,
                    unselectedIconColor = palette.textSecondary,
                    unselectedTextColor = palette.textSecondary,
                ),
            )
        }
    }
}
