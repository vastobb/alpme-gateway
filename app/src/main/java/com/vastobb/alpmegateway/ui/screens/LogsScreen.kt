package com.vastobb.alpmegateway.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vastobb.alpmegateway.data.RequestLog
import com.vastobb.alpmegateway.ui.components.EmptyState
import com.vastobb.alpmegateway.ui.components.KeyValueRow
import com.vastobb.alpmegateway.ui.components.MonoText
import com.vastobb.alpmegateway.ui.components.SectionHeader
import com.vastobb.alpmegateway.ui.components.StatusPill
import com.vastobb.alpmegateway.ui.components.formatClock
import com.vastobb.alpmegateway.ui.components.formatLatency
import com.vastobb.alpmegateway.ui.components.statusColor
import com.vastobb.alpmegateway.ui.theme.LocalAlpmePalette

private enum class LogFilter(val label: String) {
    ALL("All"),
    SUCCESS("Success"),
    ERRORS("Errors"),
}

@Composable
fun LogsScreen(
    logs: List<RequestLog>,
    onClear: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    var filter by remember { mutableStateOf(LogFilter.ALL) }
    val filtered = when (filter) {
        LogFilter.ALL -> logs
        LogFilter.SUCCESS -> logs.filter { it.isSuccess }
        LogFilter.ERRORS -> logs.filterNot { it.isSuccess }
    }
    val listState = rememberLazyListState()

    LaunchedEffect(logs.firstOrNull()?.id) {
        if (listState.firstVisibleItemIndex == 0) {
            listState.scrollToItem(0)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        SectionHeader(
            title = "Live logs",
            subtitle = "${logs.size} requests kept in memory",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusPill(
                        text = "Live",
                        color = palette.success,
                    )
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Icon(
                            Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = palette.textSecondary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Clear", color = palette.textSecondary)
                    }
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            LogFilter.entries.forEach { item ->
                FilterChip(
                    label = "${item.label}${if (item == LogFilter.ALL) " ${logs.size}" else ""}",
                    selected = filter == item,
                    onClick = { filter = item },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                EmptyState(
                    title = if (logs.isEmpty()) "No requests yet" else "Nothing matches this filter",
                    message = if (logs.isEmpty()) {
                        "Send an OpenAI-compatible request to see keys, models, retries and latency here."
                    } else {
                        "Switch the filter to see other requests."
                    },
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp),
            ) {
                items(filtered, key = { it.id }) { log ->
                    LogCard(log = log)
                }
            }
        }
    }
}

@Composable
private fun LogCard(log: RequestLog) {
    val palette = LocalAlpmePalette.current
    val color = statusColor(log.status, palette.success, palette.warning, palette.error)
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .animateContentSize(),
        shape = RoundedCornerShape(12.dp),
        color = palette.panelBg,
        border = BorderStroke(1.dp, palette.border),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (log.isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = log.keyName,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                            color = palette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = formatClock(log.timestamp),
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.textSecondary,
                        )
                    }
                    MonoText(
                        text = log.resolvedModel,
                        color = palette.textSecondary,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = log.status.toString(),
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = color,
                    )
                    Text(
                        text = formatLatency(log.latencyMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary,
                    )
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(7.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (log.stream) MiniChip("stream", palette.accent)
                if (log.attempts > 1) MiniChip("${log.attempts} attempts", palette.warning)
                if (log.fallback) MiniChip("fallback", palette.accent)
                MiniChip("chat", palette.textSecondary)
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = palette.codeHighlight,
                        border = BorderStroke(1.dp, palette.border),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            KeyValueRow("Requested model", log.requestedModel.ifBlank { "auto" }, mono = true)
                            Spacer(Modifier.height(6.dp))
                            KeyValueRow("Resolved model", log.resolvedModel, mono = true)
                            Spacer(Modifier.height(6.dp))
                            KeyValueRow("Key", log.keyName)
                            Spacer(Modifier.height(6.dp))
                            KeyValueRow("Endpoint", log.path, mono = true)
                            Spacer(Modifier.height(6.dp))
                            KeyValueRow("Attempts", log.attempts.toString())
                            Spacer(Modifier.height(6.dp))
                            KeyValueRow("Fallback", if (log.fallback) "Yes" else "No")
                            if (!log.error.isNullOrBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = log.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.error,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalAlpmePalette.current
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (selected) palette.btnBg else palette.hoverSurface,
        border = BorderStroke(1.dp, if (selected) palette.btnBg else palette.border),
    ) {
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.btnText else palette.textSecondary,
            )
        }
    }
}

@Composable
private fun MiniChip(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = color.copy(alpha = 0.09f),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
