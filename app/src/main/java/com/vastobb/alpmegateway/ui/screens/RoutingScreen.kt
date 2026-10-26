package com.vastobb.alpmegateway.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vastobb.alpmegateway.data.AlpmeModel
import com.vastobb.alpmegateway.data.GatewaySettings
import com.vastobb.alpmegateway.data.RoutingStrategy
import com.vastobb.alpmegateway.ui.components.AlpmeCard
import com.vastobb.alpmegateway.ui.components.MonoText
import com.vastobb.alpmegateway.ui.components.ModelPickerField
import com.vastobb.alpmegateway.ui.components.ModelPickerSheet
import com.vastobb.alpmegateway.ui.components.SectionHeader
import com.vastobb.alpmegateway.ui.components.SectionLabel
import com.vastobb.alpmegateway.ui.components.SettingSwitchRow
import com.vastobb.alpmegateway.ui.components.StatusPill
import com.vastobb.alpmegateway.ui.theme.LocalAlpmePalette

@Composable
fun RoutingScreen(
    settings: GatewaySettings,
    catalog: List<AlpmeModel>,
    catalogLoading: Boolean,
    catalogError: String?,
    onUniversalModel: (String) -> Unit,
    onForceUniversal: (Boolean) -> Unit,
    onEnableFallback: (Boolean) -> Unit,
    onFallbackModel: (String) -> Unit,
    onRoutingStrategy: (RoutingStrategy) -> Unit,
    onRetryCount: (Int) -> Unit,
    onTimeout: (Int) -> Unit,
    onPort: (Int) -> Unit,
    onBindLan: (Boolean) -> Unit,
    onAccessToken: (String) -> Unit,
    onRegenerateToken: () -> Unit,
    onRefreshCatalog: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    val clipboard = LocalClipboardManager.current
    var showUniversalPicker by remember { mutableStateOf(false) }
    var showFallbackPicker by remember { mutableStateOf(false) }
    var portText by remember(settings.port) { mutableStateOf(settings.port.toString()) }
    var tokenEditing by remember(settings.accessToken) { mutableStateOf(false) }
    var tokenText by remember(settings.accessToken) { mutableStateOf(settings.accessToken) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SectionHeader(
            title = "Routing",
            subtitle = "Choose how requests move from agents to Alpme",
            trailing = {
                IconButton(onClick = onRefreshCatalog) {
                    if (catalogLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = palette.textSecondary,
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = "Refresh catalog",
                            tint = palette.textSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
            },
        )
        if (catalogError != null) {
            Text(catalogError, style = MaterialTheme.typography.bodySmall, color = palette.error)
        }

        AlpmeCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Universal model",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        "Used by keys that do not have their own model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
                StatusPill(
                    text = if (settings.forceUniversalModel) "Forced" else "Pass-through",
                    color = if (settings.forceUniversalModel) palette.accent else palette.textSecondary,
                )
            }
            Spacer(Modifier.height(12.dp))
            ModelPickerField(
                label = "Model",
                value = settings.universalModel,
                onClick = { showUniversalPicker = true },
            )
            Spacer(Modifier.height(12.dp))
            SettingSwitchRow(
                title = "Force universal model",
                description = "Rewrite every request to the universal model, even if the client asks for another one.",
                checked = settings.forceUniversalModel,
                onCheckedChange = onForceUniversal,
            )
        }

        AlpmeCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.SwapHoriz,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "503 failover",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        "Retry across keys, then try a fallback model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
                StatusPill(
                    text = if (settings.enableFallback) "On" else "Off",
                    color = if (settings.enableFallback) palette.success else palette.textSecondary,
                )
            }
            Spacer(Modifier.height(10.dp))
            SettingSwitchRow(
                title = "Fallback on overload",
                description = "When Alpme returns 503/429, try the fallback model before giving up.",
                checked = settings.enableFallback,
                onCheckedChange = onEnableFallback,
            )
            if (settings.enableFallback) {
                Spacer(Modifier.height(10.dp))
                ModelPickerField(
                    label = "Fallback model",
                    value = settings.fallbackModel,
                    onClick = { showFallbackPicker = true },
                )
            }
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = palette.codeHighlight,
                border = BorderStroke(1.dp, palette.border),
            ) {
                Text(
                    text = "Fallbacks are marked with the X-Alpme-Fallback response header and in live logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    modifier = Modifier.padding(10.dp),
                )
            }
        }

        AlpmeCard {
            SectionHeader(
                title = "Key selection",
                subtitle = "How the gateway chooses the next key",
            )
            Spacer(Modifier.height(10.dp))
            StrategyOption(
                title = "Round robin",
                body = "Spread requests evenly across enabled keys, starting with the next one.",
                selected = settings.routingStrategy == RoutingStrategy.ROUND_ROBIN,
                onClick = { onRoutingStrategy(RoutingStrategy.ROUND_ROBIN) },
            )
            Spacer(Modifier.height(7.dp))
            StrategyOption(
                title = "Priority failover",
                body = "Use keys in the order shown on the Keys screen. Move to the next key only on failure.",
                selected = settings.routingStrategy == RoutingStrategy.FAILOVER,
                onClick = { onRoutingStrategy(RoutingStrategy.FAILOVER) },
            )
            Spacer(Modifier.height(7.dp))
            StrategyOption(
                title = "Least used",
                body = "Send each request to the key with the fewest attempts so far.",
                selected = settings.routingStrategy == RoutingStrategy.LEAST_USED,
                onClick = { onRoutingStrategy(RoutingStrategy.LEAST_USED) },
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Attempts per request",
                        style = MaterialTheme.typography.titleSmall,
                        color = palette.textPrimary,
                    )
                    Text(
                        "${settings.retryCount + 1} attempt${if (settings.retryCount == 0) "" else "s"} with key rotation",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
                StatusPill(text = "${settings.retryCount + 1}", color = palette.accent, dot = false)
            }
            Slider(
                value = settings.retryCount.toFloat(),
                onValueChange = { onRetryCount(it.toInt()) },
                valueRange = 0f..5f,
                steps = 4,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("1 attempt", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                Text("6 attempts", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
            }
        }

        AlpmeCard {
            SectionHeader(
                title = "Request timeout",
                subtitle = "How long a streaming response may take",
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(60, 180, 300, 600).forEach { seconds ->
                    TimeoutChip(
                        label = if (seconds < 60) "${seconds}s" else "${seconds / 60}m",
                        selected = settings.timeoutSeconds == seconds,
                        onClick = { onTimeout(seconds) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Current: ${settings.timeoutSeconds}s. Long tool calls and reasoning models may need more time.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }

        AlpmeCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Dns,
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Local server",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary,
                    )
                    Text(
                        "Listening on ${if (settings.bindLan) "all interfaces" else "localhost"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
                StatusPill(text = "Port ${settings.port}", color = palette.accent, dot = false)
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = portText,
                    onValueChange = { portText = it.filter(Char::isDigit).take(5) },
                    label = { Text("Port") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                )
                Button(
                    onClick = { onPort(portText.toIntOrNull() ?: settings.port) },
                    enabled = portText.toIntOrNull()?.let { it in 1024..65535 } == true,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.btnBg,
                        contentColor = palette.btnText,
                    ),
                ) {
                    Text("Apply")
                }
            }
            Spacer(Modifier.height(12.dp))
            SettingSwitchRow(
                title = "Allow LAN access",
                description = "Let other devices on your Wi-Fi use the gateway. Disable to keep it localhost-only.",
                checked = settings.bindLan,
                onCheckedChange = onBindLan,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = palette.textSecondary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(7.dp))
                SectionLabel("Gateway access token")
            }
            Spacer(Modifier.height(7.dp))
            if (tokenEditing) {
                OutlinedTextField(
                    value = tokenText,
                    onValueChange = { tokenText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    trailingIcon = {
                        TextButton(
                            onClick = {
                                onAccessToken(tokenText.trim())
                                tokenEditing = false
                            },
                            enabled = tokenText.isNotBlank(),
                        ) {
                            Text("Save")
                        }
                    },
                )
            } else {
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = palette.codeHighlight,
                    border = BorderStroke(1.dp, palette.border),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 11.dp, top = 6.dp, bottom = 6.dp, end = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonoText(
                            text = settings.accessToken.ifBlank { "No token set" },
                            modifier = Modifier.weight(1f),
                            color = palette.textPrimary,
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(settings.accessToken))
                            },
                            modifier = Modifier.size(34.dp),
                        ) {
                            Icon(
                                Icons.Rounded.ContentCopy,
                                contentDescription = "Copy token",
                                tint = palette.textSecondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        tokenText = settings.accessToken
                        tokenEditing = true
                    },
                    border = BorderStroke(1.dp, palette.border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
                ) {
                    Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Edit token")
                }
                TextButton(onClick = onRegenerateToken) {
                    Text("Regenerate")
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Use this token as the API key in your client. Alpme keys never leave the app.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
        }

        Spacer(Modifier.height(10.dp))
    }

    if (showUniversalPicker) {
        ModelPickerSheet(
            title = "Universal model",
            models = catalog,
            selected = settings.universalModel,
            allowUniversal = false,
            onDismiss = { showUniversalPicker = false },
            onSelect = {
                if (it != null) onUniversalModel(it)
                showUniversalPicker = false
            },
        )
    }

    if (showFallbackPicker) {
        ModelPickerSheet(
            title = "Fallback model",
            models = catalog,
            selected = settings.fallbackModel,
            allowUniversal = false,
            onDismiss = { showFallbackPicker = false },
            onSelect = {
                if (it != null) onFallbackModel(it)
                showFallbackPicker = false
            },
        )
    }
}

@Composable
private fun StrategyOption(
    title: String,
    body: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.hoverSurface else Color.Transparent,
        border = BorderStroke(1.dp, if (selected) palette.accent.copy(alpha = 0.4f) else palette.border),
    ) {
        Row(
            modifier = Modifier.padding(11.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (selected) palette.accent else Color.Transparent)
                    .then(
                        if (selected) Modifier else Modifier.background(Color.Transparent),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = palette.appBg,
                        modifier = Modifier.size(13.dp),
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(5.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, palette.border),
                    ) {}
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
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
}

@Composable
private fun TimeoutChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (selected) palette.btnBg else palette.hoverSurface,
        border = BorderStroke(1.dp, if (selected) palette.btnBg else palette.border),
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.btnText else palette.textSecondary,
            )
        }
    }
}
