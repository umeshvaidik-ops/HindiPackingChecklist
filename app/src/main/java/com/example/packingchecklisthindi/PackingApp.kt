package com.example.packingchecklisthindi

import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.Locale

internal fun readBackup(stream: InputStream): String {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = stream.read(buffer)
        if (count < 0) break
        require(out.size() + count <= 2 * 1024 * 1024)
        out.write(buffer, 0, count)
    }
    return out.toString(Charsets.UTF_8.name())
}

@Composable
internal fun PackingApp(store: Store) {
    var data by remember { mutableStateOf(store.load()) }
    val activityContext = LocalContext.current
    val deviceConfiguration = LocalConfiguration.current
    val localizedContext = remember(activityContext, data.language, deviceConfiguration) {
        val config = Configuration(deviceConfiguration)
        config.setLocale(Locale.forLanguageTag(data.language.tag))
        activityContext.createConfigurationContext(config)
    }
    val dark = when (data.theme) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    SideEffect {
        (activityContext as? ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = if (dark) SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                else SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT))
    }
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFF81D5C6), onPrimary = Color(0xFF00382F),
        primaryContainer = Color(0xFF005047), onPrimaryContainer = Color(0xFFA0F2E3),
        secondary = Color(0xFFEBC089), background = Color(0xFF101918),
        surface = Color(0xFF101918), surfaceContainer = Color(0xFF1B2624),
        surfaceContainerLow = Color(0xFF16211F)
    ) else lightColorScheme(
        primary = Color(0xFF006B5F), onPrimary = Color.White,
        primaryContainer = Color(0xFFC4EEE3), onPrimaryContainer = Color(0xFF00382F),
        secondary = Color(0xFF8B5D27), background = Color(0xFFF6FAF7),
        surface = Color(0xFFF6FAF7), surfaceContainer = Color(0xFFEBF2ED),
        surfaceContainerLow = Color(0xFFF0F6F2)
    )
    CompositionLocalProvider(LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration) {
        MaterialTheme(colorScheme = colors) {
            ChecklistScreen(data = data, onChange = { data = it; store.save(it) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistScreen(data: PackingData, onChange: (PackingData) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val latestData by rememberUpdatedState(data)
    val latestChange by rememberUpdatedState(onChange)
    var undoJob by remember { mutableStateOf<Job?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var categoryFilter by rememberSaveable { mutableStateOf("") }
    var editingItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoriesOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var restoreOpen by rememberSaveable { mutableStateOf(false) }
    var moreOpen by remember { mutableStateOf(false) }
    var categoryMenuOpen by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<PackingData?>(null) }
    val categoriesById = data.categories.associateBy { it.id }
    val selectedCategory = categoriesById[categoryFilter]
    val visible = data.items.filter {
        it.matches(query, categoriesById[it.categoryId]) &&
            (filter == 0 || (filter == 1 && !it.packed) || (filter == 2 && it.packed)) &&
            (selectedCategory == null || it.categoryId == selectedCategory.id)
    }
    val count = data.items.count { it.packed }
    val fraction = if (data.items.isEmpty()) 0f else count.toFloat() / data.items.size
    val animated by animateFloatAsState(fraction, tween(450), label = "packing progress")
    fun announce(resource: Int) { scope.launch { snackbar.showSnackbar(context.getString(resource)) } }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            val snapshot = latestData
            scope.launch {
                val success = withContext(Dispatchers.IO) { runCatching {
                    context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter(Charsets.UTF_8)?.use {
                        it.write(PackingJson.encode(snapshot))
                    } ?: error("Cannot open backup destination")
                }.isSuccess }
                snackbar.showSnackbar(context.getString(if (success) R.string.backup_saved else R.string.backup_failed))
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val imported = withContext(Dispatchers.IO) { runCatching {
                context.contentResolver.openInputStream(uri)?.use { PackingJson.decode(readBackup(it)) }
                    ?: error("Cannot open backup")
            }.getOrNull() }
            if (imported == null) snackbar.showSnackbar(context.getString(R.string.invalid_backup))
            else pendingImport = imported
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(painterResource(R.drawable.ic_brand), null,
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)))
                    Column {
                        Text(stringResource(R.string.app_name), fontSize = 19.sp, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.subtitle), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }, actions = {
                IconButton(onClick = { settingsOpen = true }, modifier = Modifier.testTag("settings")) {
                    Icon(Icons.Default.Settings, stringResource(R.string.settings))
                }
                Box {
                    IconButton(onClick = { moreOpen = true }, modifier = Modifier.testTag("more")) {
                        Icon(Icons.Default.MoreVert, stringResource(R.string.reset))
                    }
                    DropdownMenu(moreOpen, { moreOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.manage_categories)) },
                            onClick = { moreOpen = false; categoriesOpen = true })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text(stringResource(R.string.uncheck_all)) }, onClick = {
                            moreOpen = false; onChange(data.uncheckAll()); announce(R.string.reset_done)
                        })
                        DropdownMenuItem(text = { Text(stringResource(R.string.restore_defaults)) },
                            onClick = { moreOpen = false; restoreOpen = true })
                    }
                }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { editingItemId = "" }, modifier = Modifier.testTag("add_item")) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_item))
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("checklist"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item(key = "progress") {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.progress_title), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Text(stringResource(R.string.progress_count, count, data.items.size),
                                    modifier = Modifier.testTag("progress_count"), style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("${(fraction * 100).toInt()}%", fontSize = 30.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth().height(8.dp),
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                        Text(if (data.items.isNotEmpty() && count == data.items.size) stringResource(R.string.all_packed)
                            else stringResource(R.string.remaining_count, data.items.size - count),
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
            item(key = "search") {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("search"),
                    label = { Text(stringResource(R.string.search_hint)) }, singleLine = true,
                    shape = RoundedCornerShape(16.dp), leadingIcon = { Icon(Icons.Default.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Close, stringResource(R.string.clear_search))
                    } })
            }
            item(key = "filters") {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(R.string.filter_all, R.string.filter_remaining, R.string.filter_packed).forEachIndexed { index, label ->
                        FilterChip(filter == index, { filter = index }, label = { Text(stringResource(label)) },
                            modifier = Modifier.testTag("filter_$index"))
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        OutlinedButton(onClick = { categoryMenuOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedCategory?.names?.display(data.language) ?: stringResource(R.string.all_categories),
                                Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.KeyboardArrowDown, null)
                        }
                        DropdownMenu(categoryMenuOpen, { categoryMenuOpen = false }, Modifier.heightIn(max = 320.dp)) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.all_categories)) },
                                onClick = { categoryFilter = ""; categoryMenuOpen = false })
                            data.categories.forEach { c ->
                                DropdownMenuItem(text = { Text(c.names.display(data.language)) },
                                    onClick = { categoryFilter = c.id; categoryMenuOpen = false })
                            }
                        }
                    }
                    TextButton(onClick = { categoriesOpen = true }, modifier = Modifier.testTag("categories")) {
                        Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.categories))
                    }
                }
            }
            if (visible.isEmpty()) item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(if (data.items.isEmpty()) R.string.empty_title else R.string.empty_filter),
                        style = MaterialTheme.typography.titleMedium)
                    if (data.items.isEmpty()) Text(stringResource(R.string.empty_message))
                    else TextButton(onClick = { query = ""; filter = 0; categoryFilter = "" }) {
                        Text(stringResource(R.string.clear_filters))
                    }
                }
            }
            data.categories.forEach { category ->
                val group = visible.filter { it.categoryId == category.id }
                if (group.isNotEmpty()) {
                    item(key = "heading-${category.id}") {
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(category.names.display(data.language), Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${group.count { it.packed }}/${group.size}", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    items(group, key = { "entry-${it.id}" }) { entry ->
                        ItemCard(entry, data.language, onToggle = { checked -> onChange(data.saveItem(entry.copy(packed = checked))) },
                            onEdit = { editingItemId = entry.id }, onDelete = {
                                val index = data.items.indexOfFirst { it.id == entry.id }
                                onChange(data.copy(items = data.items.filterNot { it.id == entry.id }))
                                undoJob?.cancel()
                                undoJob = scope.launch {
                                    val result = snackbar.showSnackbar(context.getString(R.string.item_deleted),
                                        context.getString(R.string.undo), duration = SnackbarDuration.Long)
                                    if (result == SnackbarResult.ActionPerformed && latestData.items.none { it.id == entry.id }) {
                                        val restored = entry.copy(categoryId = entry.categoryId.takeIf { id ->
                                            latestData.categories.any { it.id == id } } ?: "OTHER")
                                        val list = latestData.items.toMutableList().apply { add(index.coerceIn(0, size), restored) }
                                        latestChange(latestData.copy(items = list))
                                    }
                                }
                            })
                    }
                }
            }
        }
    }
    editingItemId?.let { id ->
        ItemEditor(data.items.find { it.id == id }, data.categories, data.language,
            initialCategory = selectedCategory?.id ?: "OTHER", onDismiss = { editingItemId = null },
            onSaveCategory = { onChange(latestData.saveCategory(it)) }, onSave = { entry ->
                onChange(latestData.saveItem(entry)); editingItemId = null
            })
    }
    if (categoriesOpen) CategoryManager(data, onChange, onDismiss = { categoriesOpen = false })
    if (settingsOpen) SettingsDialog(data, onChange, onDismiss = { settingsOpen = false },
        onExport = { exportLauncher.launch("PackingChecklist-backup.json") },
        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) })
    if (restoreOpen) AlertDialog(onDismissRequest = { restoreOpen = false },
        title = { Text(stringResource(R.string.restore_title)) }, text = { Text(stringResource(R.string.restore_message)) },
        confirmButton = { TextButton(onClick = {
            undoJob?.cancel(); onChange(data.restoreDefaults()); query = ""; categoryFilter = ""; filter = 0; restoreOpen = false
        }) { Text(stringResource(R.string.restore_defaults)) } },
        dismissButton = { TextButton(onClick = { restoreOpen = false }) { Text(stringResource(R.string.cancel)) } })
    pendingImport?.let { incoming ->
        AlertDialog(onDismissRequest = { pendingImport = null }, title = { Text(stringResource(R.string.import_title)) },
            text = { Text(stringResource(R.string.import_message, incoming.categories.size, incoming.items.size)) },
            confirmButton = { TextButton(onClick = {
                undoJob?.cancel(); onChange(incoming); pendingImport = null; settingsOpen = false
                query = ""; categoryFilter = ""; filter = 0; announce(R.string.import_success)
            }) { Text(stringResource(R.string.import_backup)) } },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun ItemCard(entry: Entry, language: AppLanguage, onToggle: (Boolean) -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    val name = entry.names.display(language)
    val toggleLabel = stringResource(R.string.toggle_packed, name)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(entry.packed, onToggle, Modifier.testTag("packed_${entry.id}").semantics { contentDescription = toggleLabel })
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(name, fontWeight = FontWeight.Medium,
                    textDecoration = if (entry.packed) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (entry.packed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                if (entry.note.isNotBlank()) Text(entry.note, Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit, modifier = Modifier.testTag("edit_${entry.id}")) {
                Icon(Icons.Default.Edit, stringResource(R.string.edit_named_item, name), Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_${entry.id}")) {
                Icon(Icons.Default.Delete, stringResource(R.string.delete_named_item, name), Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
