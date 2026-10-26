package com.vastobb.alpmegateway.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vastobb.alpmegateway.data.ApiKeyConfig
import com.vastobb.alpmegateway.data.GatewaySettings
import com.vastobb.alpmegateway.data.RequestLog
import com.vastobb.alpmegateway.ui.GatewayStats
import com.vastobb.alpmegateway.ui.KeyRow
import com.vastobb.alpmegateway.ui.components.AlpmeCard
import com.vastobb.alpmegateway.ui.components.CodeBlock
import com.vastobb.alpmegateway.ui.components.EmptyState
import com.vastobb.alpmegateway.ui.components.KeyValueRow
import com.vastobb.alpmegateway.ui.components.MonoText
import com.vastobb.alpmegateway.ui.components.SectionHeader
import com.vastobb.alpmegateway.ui.components.SectionLabel
import com.vastobb.alpmegateway.ui.components.StatTile
import com.vastobb.alpmegateway.ui.components.StatusDot
import com.vastobb.alpmegateway.ui.components.StatusPill
import com.vastobb.alpmegateway.ui.components.formatClock
import com.vastobb.alpmegateway.ui.components.formatLatency
import com.vastobb.alpmegateway.ui.components.statusColor
import com.vastobb.alpmegateway.ui.theme.LocalAlpmePalette

@Composable
fun DashboardScreen(
    settings: GatewaySettings,
    isRunning: Boolean,
    endpoint: String,
    stats: GatewayStats,
    keys: List<KeyRow>,
    logs: List<RequestLog>,
    onConnect: () -> Unit,
    onOpenKeys: () -> Unit,
    onOpenRouting: () -> Unit,
    onOpenLogs: () -> Unit,
    onToggle: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val activeKeys = keys.count { it.config.enabled }
    val recent = logs.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EndpointHero(
            endpoint = endpoint,
            isRunning = isRunning,
            settings = settings,
            activeKeys = activeKeys,
            onConnect = onConnect,
            onCopy = {
                clipboard.setText(AnnotatedString(endpoint))
                Toast.makeText(context, "Endpoint copied", Toast.LENGTH_SHORT).show()
            },
            onToggle = onToggle,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "Requests",
                value = stats.requests.toString(),
                icon = Icons.Rounded.Terminal,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Success",
                value = if (stats.requests == 0) "—" else "${stats.successRate}%",
                icon = Icons.Rounded.CheckCircle,
                accent = palette.success,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile(
                label = "Avg latency",
                value = if (stats.requests == 0) "—" else formatLatency(stats.avgLatencyMs),
                icon = Icons.Rounded.Timer,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Active keys",
                value = "$activeKeys/${keys.size}",
                icon = Icons.Rounded.Key,
                modifier = Modifier.weight(1f),
            )
        }

        AlpmeCard {
            SectionHeader(
                title = "Routing",
                subtitle = "Keys, model selection and failover",
                trailing = {
                    TextButton(onClick = onOpenRouting) { Text("Manage") }
                },
            )
            Spacer(Modifier.height(12.dp))
            KeyValueRow("Universal model", settings.universalModel.ifBlank { "Request model" }, mono = true)
            Spacer(Modifier.height(7.dp))
            KeyValueRow("Strategy", settings.routingStrategy.pretty())
            Spacer(Modifier.height(7.dp))
            KeyValueRow(
                "Fallback",
                if (settings.enableFallback) settings.fallbackModel else "Off",
                mono = settings.enableFallback,
                valueColor = if (settings.enableFallback) palette.textPrimary else palette.textSecondary,
            )
            Spacer(Modifier.height(7.dp))
            KeyValueRow("Attempts", "${settings.retryCount + 1} per request")
            if (settings.enableFallback) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(9.dp),
                    color = palette.accent.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.18f)),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "On 503 or 429, the gateway rotates keys and can retry with the fallback model.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                        )
                    }
                }
            }
        }

        AlpmeCard {
            SectionHeader(
                title = "API keys",
                subtitle = "$activeKeys active · ${keys.size} total",
                trailing = {
                    TextButton(onClick = onOpenKeys) { Text("Manage") }
                },
            )
            Spacer(Modifier.height(6.dp))
            if (keys.isEmpty()) {
                EmptyState(
                    title = "No keys yet",
                    message = "Add one or more Alpme keys to start routing requests.",
                )
            } else {
                keys.take(3).forEach { row ->
                    KeyMiniRow(row = row, onClick = onOpenKeys)
                }
                if (keys.size > 3) {
                    TextButton(onClick = onOpenKeys, modifier = Modifier.padding(top = 2.dp)) {
                        Text("View all ${keys.size} keys")
                    }
                }
            }
        }

        AlpmeCard {
            SectionHeader(
                title = "Live activity",
                subtitle = "Requests passing through the gateway",
                trailing = {
                    TextButton(onClick = onOpenLogs) { Text("View all") }
                },
            )
            Spacer(Modifier.height(8.dp))
            if (recent.isEmpty()) {
                EmptyState(
                    title = "Waiting for traffic",
                    message = "Point an OpenAI-compatible client at the endpoint above.",
                )
            } else {
                recent.forEach { log ->
                    LogMiniRow(log = log)
                }
            }
        }

        HowItWorksCard()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun EndpointHero(
    endpoint: String,
    isRunning: Boolean,
    settings: GatewaySettings,
    activeKeys: Int,
    onConnect: () -> Unit,
    onCopy: () -> Unit,
    onToggle: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = palette.panelBg,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            palette.panelBg,
                            palette.sidebarBg,
                        ),
                    ),
                )
                .padding(18.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(palette.btnBg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = palette.btnText,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            SectionLabel("Universal endpoint")
                            Text(
                                text = if (isRunning) "Ready for agents" else "Gateway is stopped",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = palette.textPrimary,
                            )
                        }
                    }
                    StatusPill(
                        text = if (isRunning) "Online" else "Offline",
                        color = if (isRunning) palette.success else palette.textSecondary,
                    )
                }
                Spacer(Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = palette.codeHighlight,
                    border = BorderStroke(1.dp, palette.border),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MonoText(
                            text = endpoint,
                            modifier = Modifier.weight(1f),
                            color = palette.textPrimary,
                        )
                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = palette.hoverSurface,
                        ) {
                            TextButton(onClick = onCopy, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                                Icon(
                                    Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy endpoint",
                                    modifier = Modifier.size(15.dp),
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Copy", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "One OpenAI-compatible URL. Rotate keys, pick models per key, and let agents keep running through transient 503s.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onConnect,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = palette.btnBg,
                            contentColor = palette.btnText,
                        ),
                    ) {
                        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Connect agent")
                    }
                    OutlinedButton(
                        onClick = onToggle,
                        border = BorderStroke(1.dp, palette.border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
                    ) {
                        Icon(
                            if (isRunning) Icons.Rounded.Stop else Icons.Rounded.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isRunning) "Stop" else "Start")
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    HeroMeta(Icons.Rounded.Key, "$activeKeys active keys")
                    HeroMeta(Icons.Rounded.Dns, "Port ${settings.port}")
                    HeroMeta(
                        Icons.Rounded.Speed,
                        if (settings.bindLan) "LAN enabled" else "Localhost only",
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroMeta(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    val palette = LocalAlpmePalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
    }
}

@Composable
private fun KeyMiniRow(row: KeyRow, onClick: () -> Unit) {
    val palette = LocalAlpmePalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(if (row.config.enabled) Color.Transparent else palette.hoverSurface)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatusDot(
            color = when {
                !row.config.enabled -> palette.textSecondary
                (row.runtime.lastStatus ?: 0) in 200..299 -> palette.success
                row.runtime.lastStatus != null -> palette.error
                else -> palette.textSecondary
            },
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.config.name,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = row.config.model ?: "Universal model",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = "${row.runtime.requestCount} req",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary,
        )
        Spacer(Modifier.width(6.dp))
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
            Icon(Icons.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun LogMiniRow(log: RequestLog) {
    val palette = LocalAlpmePalette.current
    val color = statusColor(log.status, palette.success, palette.warning, palette.error)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (log.isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.keyName,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = log.resolvedModel,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = log.status.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = color,
            )
            Text(
                text = "${formatClock(log.timestamp)} · ${formatLatency(log.latencyMs)}",
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun HowItWorksCard() {
    val palette = LocalAlpmePalette.current
    AlpmeCard {
        SectionHeader(
            title = "How it works",
            subtitle = "A local OpenAI-compatible routing layer",
        )
        Spacer(Modifier.height(12.dp))
        StepRow("01", "Add Alpme keys", "Keys are encrypted on-device and never leave the phone.")
        StepRow("02", "Choose models", "Set one universal model or override it per key.")
        StepRow("03", "Point agents at the URL", "The gateway adds Alpme's required User-Agent, rotates keys and logs every attempt.")
    }
}

@Composable
private fun StepRow(number: String, title: String, body: String) {
    val palette = LocalAlpmePalette.current
    Row(modifier = Modifier.padding(vertical = 7.dp)) {
        Text(
            text = number,
            style = MaterialTheme.typography.labelMedium,
            color = palette.accent,
            modifier = Modifier.width(28.dp),
        )
        Column {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectSheet(
    endpoint: String,
    token: String,
    onDismiss: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    val safeToken = token.ifBlank { "sk_gw_your_token" }
    val config = """
        {
          "${'$'}schema": "https://opencode.ai/config.json",
          "provider": {
            "alpme-gateway": {
              "npm": "@ai-sdk/openai-compatible",
              "name": "Alpme Gateway",
              "options": {
                "baseURL": "$endpoint",
                "apiKey": "$safeToken"
              },
              "models": {
                "auto": { "name": "Alpme Auto" }
              }
            }
          }
        }
    """.trimIndent()
    val curl = """
        curl "$endpoint/chat/completions" \
          -H "Authorization: Bearer $safeToken" \
          -H "Content-Type: application/json" \
          -d '{"model":"auto","messages":[{"role":"user","content":"hello"}]}'
    """.trimIndent()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.panelBg,
        contentColor = palette.textPrimary,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.9f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Text("Connect an agent", style = MaterialTheme.typography.headlineSmall, color = palette.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                "Use this endpoint from OpenCode, Cursor, a script, or any OpenAI-compatible client.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = palette.codeHighlight,
                border = BorderStroke(1.dp, palette.border),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Endpoint")
                        Spacer(Modifier.height(3.dp))
                        MonoText(endpoint)
                        Spacer(Modifier.height(8.dp))
                        SectionLabel("Gateway token")
                        Spacer(Modifier.height(3.dp))
                        MonoText(safeToken)
                    }
                    Surface(shape = CircleShape, color = palette.hoverSurface) {
                        TextButton(
                            onClick = {
                                clipboard.setText(AnnotatedString("$endpoint\n$safeToken"))
                                Toast.makeText(context, "Endpoint and token copied", Toast.LENGTH_SHORT).show()
                            },
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterTab("OpenCode", selected = tab == 0) { tab = 0 }
                FilterTab("cURL", selected = tab == 1) { tab = 1 }
            }
            Spacer(Modifier.height(10.dp))
            if (tab == 0) {
                Text(
                    "Add this block to ~/.config/opencode/opencode.json, then select alpme-gateway/auto in /models. If your OpenCode build uses the V2 `providers` shape, move the same npm/options/models entry there.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
                Spacer(Modifier.height(8.dp))
                CodeBlock(config)
                Spacer(Modifier.height(8.dp))
                Text(
                    "If OpenCode runs on a computer, keep the phone and computer on the same Wi-Fi and use the LAN endpoint shown above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            } else {
                CodeBlock(curl)
                Spacer(Modifier.height(8.dp))
                Text(
                    "The token is your gateway password, not an Alpme key. Alpme keys stay inside the app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
            }
            Spacer(Modifier.height(26.dp))
        }
    }
}

@Composable
private fun FilterTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAlpmePalette.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) palette.btnBg else palette.hoverSurface,
        border = BorderStroke(1.dp, if (selected) palette.btnBg else palette.border),
    ) {
        TextButton(
            onClick = onClick,
            colors = ButtonDefaults.textButtonColors(
                contentColor = if (selected) palette.btnText else palette.textSecondary,
            ),
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

private fun com.vastobb.alpmegateway.data.RoutingStrategy.pretty(): String = when (this) {
    com.vastobb.alpmegateway.data.RoutingStrategy.ROUND_ROBIN -> "Round robin"
    com.vastobb.alpmegateway.data.RoutingStrategy.FAILOVER -> "Priority failover"
    com.vastobb.alpmegateway.data.RoutingStrategy.LEAST_USED -> "Least used"
}
