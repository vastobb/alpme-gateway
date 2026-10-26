package com.vastobb.alpmegateway.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vastobb.alpmegateway.BuildConfig
import com.vastobb.alpmegateway.data.AppThemeMode
import com.vastobb.alpmegateway.data.GatewaySettings
import com.vastobb.alpmegateway.ui.components.AlpmeCard
import com.vastobb.alpmegateway.ui.components.KeyValueRow
import com.vastobb.alpmegateway.ui.components.MonoText
import com.vastobb.alpmegateway.ui.components.SectionHeader
import com.vastobb.alpmegateway.ui.components.SectionLabel
import com.vastobb.alpmegateway.ui.components.SettingSwitchRow
import com.vastobb.alpmegateway.ui.components.StatusPill
import com.vastobb.alpmegateway.ui.theme.Graphite
import com.vastobb.alpmegateway.ui.theme.LocalAlpmePalette
import com.vastobb.alpmegateway.ui.theme.MidnightSlate
import com.vastobb.alpmegateway.ui.theme.WarmNeutral

@Composable
fun SettingsScreen(
    settings: GatewaySettings,
    isRunning: Boolean,
    endpoint: String,
    onTheme: (AppThemeMode) -> Unit,
    onStartOnBoot: (Boolean) -> Unit,
    onAutoStartProxy: (Boolean) -> Unit,
    onClearLogs: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionHeader(
            title = "Settings",
            subtitle = "Appearance, startup and local privacy",
        )

        AlpmeCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Appearance",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        "Three themes from the reference design",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            ThemeOption(
                name = "Midnight Slate",
                description = "Cool graphite with mint accents",
                mode = AppThemeMode.MIDNIGHT,
                appBg = MidnightSlate.appBg,
                panelBg = MidnightSlate.panelBg,
                text = MidnightSlate.textPrimary,
                accent = MidnightSlate.accent,
                selected = settings.theme == AppThemeMode.MIDNIGHT,
                onClick = { onTheme(AppThemeMode.MIDNIGHT) },
            )
            Spacer(Modifier.height(7.dp))
            ThemeOption(
                name = "Graphite",
                description = "Neutral monochrome with lime accents",
                mode = AppThemeMode.GRAPHITE,
                appBg = Graphite.appBg,
                panelBg = Graphite.panelBg,
                text = Graphite.textPrimary,
                accent = Graphite.accent,
                selected = settings.theme == AppThemeMode.GRAPHITE,
                onClick = { onTheme(AppThemeMode.GRAPHITE) },
            )
            Spacer(Modifier.height(7.dp))
            ThemeOption(
                name = "Warm Neutral",
                description = "Light editorial theme from the reference",
                mode = AppThemeMode.WARM,
                appBg = WarmNeutral.appBg,
                panelBg = WarmNeutral.panelBg,
                text = WarmNeutral.textPrimary,
                accent = WarmNeutral.accent,
                selected = settings.theme == AppThemeMode.WARM,
                onClick = { onTheme(AppThemeMode.WARM) },
            )
        }

        AlpmeCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.RestartAlt,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Background service",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        "Keep the gateway available while the app is closed",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
                Spacer(Modifier.weight(1f))
                StatusPill(
                    text = if (isRunning) "Running" else "Stopped",
                    color = if (isRunning) palette.success else palette.textSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            SettingSwitchRow(
                title = "Start gateway on app launch",
                description = "Bring the local server up whenever Alpme Gateway opens.",
                checked = settings.autoStartProxy,
                onCheckedChange = onAutoStartProxy,
            )
            Spacer(Modifier.height(12.dp))
            SettingSwitchRow(
                title = "Start on boot",
                description = "Bring the server back after the phone restarts. Requires notification permission.",
                checked = settings.startOnBoot,
                onCheckedChange = onStartOnBoot,
            )
        }

        AlpmeCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Server",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        "Current local endpoint",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = palette.codeHighlight,
                border = BorderStroke(1.dp, palette.border),
            ) {
                MonoText(
                    text = endpoint,
                    modifier = Modifier.fillMaxWidth().padding(11.dp),
                )
            }
            Spacer(Modifier.height(9.dp))
            KeyValueRow("Port", settings.port.toString())
            Spacer(Modifier.height(6.dp))
            KeyValueRow("LAN access", if (settings.bindLan) "Enabled" else "Localhost only")
            Spacer(Modifier.height(6.dp))
            KeyValueRow("Routing", settings.routingStrategy.name.lowercase().replace('_', ' '))
            Spacer(Modifier.height(6.dp))
            KeyValueRow("Attempts", "${settings.retryCount + 1} per request")
        }

        AlpmeCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Data & privacy",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        "Everything stays on this device",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            PrivacyRow(
                icon = Icons.Rounded.Lock,
                title = "Keys encrypted at rest",
                body = "Alpme keys are stored with EncryptedSharedPreferences backed by the Android Keystore.",
            )
            Spacer(Modifier.height(10.dp))
            PrivacyRow(
                icon = Icons.Rounded.Key,
                title = "No cloud account",
                body = "The gateway talks directly to Alpme. There is no middle server and no analytics SDK.",
            )
            Spacer(Modifier.height(10.dp))
            PrivacyRow(
                icon = Icons.Rounded.Code,
                title = "Logs stay in memory",
                body = "Request metadata is capped at 300 entries and is never written to disk.",
            )
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = onClearLogs,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            ) {
                Icon(
                    Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text("Clear in-memory logs", color = palette.textSecondary)
            }
        }

        AlpmeCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        "Alpme Gateway ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "A local, OpenAI-compatible gateway for Alpme. It rewrites the User-Agent Alpme expects, supports multiple keys, per-key models, streaming, retries and fallback routing.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/vastobb/alpme-gateway"),
                        )
                        runCatching { context.startActivity(intent) }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(
                        Icons.Rounded.Code,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("GitHub")
                }
                TextButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://alpme.com"),
                        )
                        runCatching { context.startActivity(intent) }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("Alpme")
                }
            }
        }

        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun ThemeOption(
    name: String,
    description: String,
    mode: AppThemeMode,
    appBg: Color,
    panelBg: Color,
    text: Color,
    accent: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.hoverSurface else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) palette.accent.copy(alpha = 0.45f) else palette.border),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .size(width = 58.dp, height = 38.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(appBg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 26.dp, height = 26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(panelBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(accent),
                    )
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(width = 3.dp, height = 18.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(text.copy(alpha = 0.7f)),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = palette.textPrimary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(palette.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = palette.appBg,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
) {
    val palette = LocalAlpmePalette.current
    Row {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(palette.hoverSurface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = palette.textSecondary,
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }
    }
}
