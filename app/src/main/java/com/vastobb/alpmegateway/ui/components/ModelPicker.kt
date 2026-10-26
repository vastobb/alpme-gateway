package com.vastobb.alpmegateway.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vastobb.alpmegateway.data.AlpmeModel
import com.vastobb.alpmegateway.ui.theme.LocalAlpmePalette

@Composable
fun ModelPickerField(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Choose a model",
) {
    val palette = LocalAlpmePalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = palette.textSecondary,
        )
        Spacer(Modifier.height(5.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
            shape = MaterialTheme.shapes.medium,
            color = palette.panelBg,
            border = BorderStroke(1.dp, palette.border),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = value?.takeIf { it.isNotBlank() } ?: placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (value.isNullOrBlank()) palette.textSecondary else palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Choose model",
                    tint = palette.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelPickerSheet(
    title: String,
    models: List<AlpmeModel>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    allowUniversal: Boolean = false,
) {
    val palette = LocalAlpmePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    var manual by remember { mutableStateOf("") }
    val filtered = remember(query, models) {
        if (query.isBlank()) models else models.filter {
            it.id.contains(query, ignoreCase = true) ||
                it.displayName.contains(query, ignoreCase = true) ||
                it.ownedBy.orEmpty().contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.panelBg,
        contentColor = palette.textPrimary,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.86f)
                .imePadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(Modifier.height(18.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "The gateway sends this model to Alpme. Per-key models override the universal model.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("Search models") },
                leadingIcon = {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.textSecondary)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = manual,
                    onValueChange = { manual = it },
                    singleLine = true,
                    placeholder = { Text("Or enter a model ID") },
                    modifier = Modifier.weight(1f),
                    shape = MaterialTheme.shapes.medium,
                )
                TextButton(
                    onClick = {
                        if (manual.isNotBlank()) onSelect(manual.trim())
                    },
                ) {
                    Text("Use")
                }
            }
            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (allowUniversal) {
                    item(key = "__universal__") {
                        ModelRow(
                            title = "Use universal model",
                            subtitle = "Follow the routing screen's universal model",
                            selected = selected.isNullOrBlank(),
                            onClick = { onSelect(null) },
                        )
                    }
                }
                if (filtered.isEmpty()) {
                    item(key = "__empty__") {
                        Text(
                            text = "No catalog models loaded. Enter a model ID manually.",
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.textSecondary,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
                items(filtered, key = { it.id }) { model ->
                    ModelRow(
                        title = model.displayName,
                        subtitle = buildString {
                            append(model.id)
                            model.ownedBy?.let { append("  ·  $it") }
                            model.contextWindow?.let { append("  ·  ${formatCompact(it)} ctx") }
                        },
                        selected = model.id == selected,
                        onClick = { onSelect(model.id) },
                    )
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun ModelRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) palette.hoverSurface else palette.panelBg,
        border = BorderStroke(1.dp, if (selected) palette.textSecondary.copy(alpha = 0.4f) else palette.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Selected",
                    tint = palette.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
