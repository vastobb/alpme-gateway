package com.vastobb.alpmegateway.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vastobb.alpmegateway.data.AlpmeModel
import com.vastobb.alpmegateway.data.ApiKeyConfig
import com.vastobb.alpmegateway.data.GatewaySettings
import com.vastobb.alpmegateway.data.KeyRuntime
import com.vastobb.alpmegateway.data.maskKey
import com.vastobb.alpmegateway.net.KeyTestResult
import com.vastobb.alpmegateway.ui.KeyRow
import com.vastobb.alpmegateway.ui.components.AlpmeCard
import com.vastobb.alpmegateway.ui.components.EmptyState
import com.vastobb.alpmegateway.ui.components.MonoText
import com.vastobb.alpmegateway.ui.components.SectionHeader
import com.vastobb.alpmegateway.ui.components.StatusDot
import com.vastobb.alpmegateway.ui.components.StatusPill
import com.vastobb.alpmegateway.ui.components.formatLatency
import com.vastobb.alpmegateway.ui.components.ModelPickerField
import com.vastobb.alpmegateway.ui.components.ModelPickerSheet
import com.vastobb.alpmegateway.ui.theme.LocalAlpmePalette
import kotlinx.coroutines.launch

@Composable
fun KeysScreen(
    keys: List<KeyRow>,
    settings: GatewaySettings,
    catalog: List<AlpmeModel>,
    catalogLoading: Boolean,
    catalogError: String?,
    onAddKey: (String, String, String?) -> Unit,
    onUpdateKey: (ApiKeyConfig) -> Unit,
    onDeleteKey: (String) -> Unit,
    onToggleKey: (String, Boolean) -> Unit,
    onTestKey: suspend (String) -> KeyTestResult,
    onRefreshCatalog: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    var showAdd by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ApiKeyConfig?>(null) }
    var deleting by remember { mutableStateOf<ApiKeyConfig?>(null) }
    var testResults by remember { mutableStateOf<Map<String, KeyTestResult>>(emptyMap()) }
    var testingId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionHeader(
                    title = "API keys",
                    subtitle = "${keys.count { it.config.enabled }} enabled · ${keys.size} total",
                    trailing = {
                        Button(
                            onClick = { showAdd = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.btnBg,
                                contentColor = palette.btnText,
                            ),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(5.dp))
                            Text("Add key")
                        }
                    },
                )
            }
            item {
                AlpmeCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Key,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Keys stay on this device",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.textPrimary,
                            )
                            Text(
                                "Encrypted with Android Keystore AES-256. The app masks them everywhere except the edit sheet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary,
                            )
                        }
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
                                    contentDescription = "Refresh model catalog",
                                    tint = palette.textSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    }
                    if (catalogError != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(catalogError, style = MaterialTheme.typography.bodySmall, color = palette.error)
                    }
                }
            }
            if (keys.isEmpty()) {
                item {
                    AlpmeCard {
                        EmptyState(
                            title = "Add your first Alpme key",
                            message = "You can add as many keys as you want. Requests rotate or fail over between them.",
                        )
                        Button(
                            onClick = { showAdd = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = palette.btnBg,
                                contentColor = palette.btnText,
                            ),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add API key")
                        }
                    }
                }
            }
            items(keys, key = { it.config.id }) { row ->
                KeyCard(
                    row = row,
                    universalModel = settings.universalModel,
                    testResult = testResults[row.config.id],
                    testing = testingId == row.config.id,
                    onToggle = { enabled -> onToggleKey(row.config.id, enabled) },
                    onTest = {
                        testingId = row.config.id
                        scope.launch {
                            val result = onTestKey(row.config.key)
                            testResults = testResults + (row.config.id to result)
                            testingId = null
                        }
                    },
                    onEdit = { editing = row.config },
                    onDelete = { deleting = row.config },
                )
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }

    if (showAdd) {
        KeyEditSheet(
            key = null,
            settings = settings,
            catalog = catalog,
            onDismiss = { showAdd = false },
            onSave = { name, key, model ->
                onAddKey(name, key, model)
                showAdd = false
            },
            onTestKey = onTestKey,
        )
    }

    editing?.let { key ->
        KeyEditSheet(
            key = key,
            settings = settings,
            catalog = catalog,
            onDismiss = { editing = null },
            onSave = { name, value, model ->
                onUpdateKey(key.copy(name = name, key = value, model = model))
                editing = null
            },
            onTestKey = onTestKey,
        )
    }

    deleting?.let { key ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = palette.panelBg,
            titleContentColor = palette.textPrimary,
            textContentColor = palette.textSecondary,
            title = { Text("Delete ${key.name}?") },
            text = { Text("Requests will stop using this key immediately. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteKey(key.id)
                        deleting = null
                    },
                ) {
                    Text("Delete", color = palette.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            },
        )
    }
}

@Composable
private fun KeyCard(
    row: KeyRow,
    universalModel: String,
    testResult: KeyTestResult?,
    testing: Boolean,
    onToggle: (Boolean) -> Unit,
    onTest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val palette = LocalAlpmePalette.current
    var menuOpen by remember { mutableStateOf(false) }
    val statusColor = when {
        !row.config.enabled -> palette.textSecondary
        (row.runtime.lastStatus ?: 0) in 200..299 -> palette.success
        row.runtime.lastStatus != null -> palette.error
        else -> palette.textSecondary
    }

    AlpmeCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(color = statusColor, size = 9.dp)
            Spacer(Modifier.width(9.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.config.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = maskKey(row.config.key),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = com.vastobb.alpmegateway.ui.theme.JetBrainsMono,
                    ),
                    color = palette.textSecondary,
                )
            }
            Switch(checked = row.config.enabled, onCheckedChange = onToggle)
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Key menu", tint = palette.textSecondary)
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = palette.panelBg,
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Test key") },
                        leadingIcon = { Icon(Icons.Rounded.Speed, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onTest()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = palette.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = palette.error) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Pill(
                text = row.config.model ?: "Universal: $universalModel",
                color = palette.accent,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (row.runtime.requestCount > 0) {
                Pill(text = "${row.runtime.requestCount} req", color = palette.textSecondary)
            }
            if (row.runtime.errorCount > 0) {
                Pill(text = "${row.runtime.errorCount} err", color = palette.error)
            }
        }
        Spacer(Modifier.height(11.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = when {
                        row.runtime.lastStatus == null -> "Not used yet"
                        (row.runtime.lastStatus ?: 0) in 200..299 -> "Last request OK"
                        else -> "Last request failed"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary,
                )
                Text(
                    text = buildString {
                        row.runtime.lastLatencyMs?.let { append(formatLatency(it)) }
                        row.runtime.lastStatus?.let {
                            if (isNotEmpty()) append(" · ")
                            append("HTTP $it")
                        }
                    }.ifBlank { "No traffic" },
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.textSecondary,
                )
            }
            OutlinedButton(
                onClick = onTest,
                enabled = !testing,
                border = BorderStroke(1.dp, palette.border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            ) {
                if (testing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = palette.textPrimary,
                    )
                } else {
                    Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Test", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (testResult != null) {
            Spacer(Modifier.height(9.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (testResult.ok) palette.success.copy(alpha = 0.09f) else palette.error.copy(alpha = 0.09f),
                border = BorderStroke(
                    1.dp,
                    if (testResult.ok) palette.success.copy(alpha = 0.2f) else palette.error.copy(alpha = 0.2f),
                ),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (testResult.ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = if (testResult.ok) palette.success else palette.error,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = if (testResult.ok) {
                            "Key accepted in ${formatLatency(testResult.latencyMs)}"
                        } else {
                            testResult.message
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (testResult.ok) palette.success else palette.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun Pill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(100.dp),
        color = color.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.17f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeyEditSheet(
    key: ApiKeyConfig?,
    settings: GatewaySettings,
    catalog: List<AlpmeModel>,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
    onTestKey: suspend (String) -> KeyTestResult,
) {
    val palette = LocalAlpmePalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(key?.name.orEmpty()) }
    var value by remember { mutableStateOf(key?.key.orEmpty()) }
    var useUniversal by remember { mutableStateOf(key?.model.isNullOrBlank()) }
    var model by remember { mutableStateOf(key?.model ?: settings.universalModel) }
    var visible by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<KeyTestResult?>(null) }
    var testing by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.panelBg,
        contentColor = palette.textPrimary,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.92f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
        ) {
            Text(
                text = if (key == null) "Add Alpme key" else "Edit key",
                style = MaterialTheme.typography.headlineSmall,
                color = palette.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "The key is encrypted locally and sent only to Alpme as a Bearer token.",
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                placeholder = { Text("Personal, Work, Backup…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text("Alpme API key") },
                placeholder = { Text("sk_live_…") },
                singleLine = true,
                visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { visible = !visible }) {
                        Icon(
                            if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = if (visible) "Hide key" else "Show key",
                            tint = palette.textSecondary,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Use universal model", style = MaterialTheme.typography.titleSmall, color = palette.textPrimary)
                    Text(
                        text = settings.universalModel.ifBlank { "No universal model set" },
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary,
                    )
                }
                Switch(checked = useUniversal, onCheckedChange = { useUniversal = it })
            }
            if (!useUniversal) {
                Spacer(Modifier.height(12.dp))
                ModelPickerField(
                    label = "Model for this key",
                    value = model,
                    onClick = { showPicker = true },
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        if (value.isNotBlank()) {
                            testing = true
                            scope.launch {
                                testResult = onTestKey(value.trim())
                                testing = false
                            }
                        }
                    },
                    enabled = value.isNotBlank() && !testing,
                    border = BorderStroke(1.dp, palette.border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.textPrimary),
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = palette.textPrimary,
                        )
                    } else {
                        Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Test key")
                    }
                }
            }
            testResult?.let { result ->
                Spacer(Modifier.height(9.dp))
                Text(
                    text = if (result.ok) {
                        "Key accepted · ${formatLatency(result.latencyMs)}"
                    } else {
                        result.message
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (result.ok) palette.success else palette.error,
                )
            }
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    val selectedModel = if (useUniversal || model.isBlank()) null else model
                    onSave(name.trim().ifBlank { "Alpme key" }, value.trim(), selectedModel)
                },
                enabled = value.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.btnBg,
                    contentColor = palette.btnText,
                ),
            ) {
                Text(if (key == null) "Add key" else "Save changes")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        ModelPickerSheet(
            title = "Model for this key",
            models = catalog,
            selected = model,
            allowUniversal = false,
            onDismiss = { showPicker = false },
            onSelect = {
                if (it != null) {
                    model = it
                    useUniversal = false
                }
                showPicker = false
            },
        )
    }
}
