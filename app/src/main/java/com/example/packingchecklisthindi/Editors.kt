package com.example.packingchecklisthindi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.UUID

@Composable
internal fun EditorDialog(
    title: String, onDismiss: () -> Unit, confirmText: String = stringResource(R.string.save),
    confirmEnabled: Boolean = true, confirmTag: String = "confirm", onConfirm: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.padding(16.dp).widthIn(max = 560.dp).fillMaxWidth().heightIn(max = 680.dp),
            shape = MaterialTheme.shapes.extraLarge, tonalElevation = 6.dp) {
            Column {
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, stringResource(R.string.cancel)) }
                }
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onConfirm, enabled = confirmEnabled, modifier = Modifier.testTag(confirmTag)) {
                        Text(confirmText)
                    }
                }
            }
        }
    }
}

@Composable
private fun NameFields(names: Names, onChange: (Names) -> Unit, language: AppLanguage, prefix: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val order = listOf(language) + AppLanguage.entries.filterNot { it == language }
    order.forEachIndexed { index, fieldLanguage ->
        if (index == 0 || expanded) {
            val value = when (fieldLanguage) {
                AppLanguage.HINDI -> names.hi
                AppLanguage.ENGLISH -> names.en
                AppLanguage.SANSKRIT -> names.sa
            }
            val label = when (fieldLanguage) {
                AppLanguage.HINDI -> R.string.name_hi
                AppLanguage.ENGLISH -> R.string.name_en
                AppLanguage.SANSKRIT -> R.string.name_sa
            }
            OutlinedTextField(value, { text -> onChange(when (fieldLanguage) {
                AppLanguage.HINDI -> names.copy(hi = text)
                AppLanguage.ENGLISH -> names.copy(en = text)
                AppLanguage.SANSKRIT -> names.copy(sa = text)
            }) }, Modifier.fillMaxWidth().testTag("${prefix}_name_${fieldLanguage.tag}"),
                label = { Text(stringResource(label)) }, singleLine = true)
        }
        if (index == 0) TextButton(onClick = { expanded = !expanded }, modifier = Modifier.testTag("${prefix}_other_names")) {
            Text(stringResource(R.string.other_languages))
        }
    }
    if (!names.isNotBlank()) Text(stringResource(R.string.one_name_required),
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (expanded) Text(stringResource(R.string.translations_hint), style = MaterialTheme.typography.bodySmall)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItemEditor(entry: Entry?, categories: List<PackingCategory>, language: AppLanguage,
    initialCategory: String, onDismiss: () -> Unit, onSaveCategory: (PackingCategory) -> Unit, onSave: (Entry) -> Unit) {
    var hi by rememberSaveable { mutableStateOf(entry?.names?.hi.orEmpty()) }
    var en by rememberSaveable { mutableStateOf(entry?.names?.en.orEmpty()) }
    var sa by rememberSaveable { mutableStateOf(entry?.names?.sa.orEmpty()) }
    var note by rememberSaveable { mutableStateOf(entry?.note.orEmpty()) }
    var categoryId by rememberSaveable { mutableStateOf(entry?.categoryId ?: initialCategory) }
    var expanded by remember { mutableStateOf(false) }
    var newCategory by rememberSaveable { mutableStateOf(false) }
    val names = Names(hi, en, sa)
    val category = categories.find { it.id == categoryId } ?: categories.first { it.id == "OTHER" }
    EditorDialog(stringResource(if (entry == null) R.string.add_item else R.string.edit_item), onDismiss,
        confirmEnabled = names.isNotBlank(), confirmTag = "save_item", onConfirm = {
            onSave(Entry(entry?.id ?: UUID.randomUUID().toString(), names.trimmed(), category.id,
                entry?.packed ?: false, note.trim()))
        }) {
        NameFields(names, { hi = it.hi; en = it.en; sa = it.sa }, language, "item")
        ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
            OutlinedTextField(category.names.display(language), {}, readOnly = true,
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable).testTag("item_category"),
                label = { Text(stringResource(R.string.category)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) })
            ExposedDropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 280.dp)) {
                categories.forEach { c -> DropdownMenuItem(text = { Text(c.names.display(language)) },
                    onClick = { categoryId = c.id; expanded = false }) }
            }
        }
        TextButton(onClick = { newCategory = true }, modifier = Modifier.testTag("add_category_inline")) {
            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.add_category))
        }
        OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth().testTag("item_note"),
            label = { Text(stringResource(R.string.notes)) }, placeholder = { Text(stringResource(R.string.notes_hint)) },
            minLines = 2, maxLines = 5)
    }
    if (newCategory) CategoryEditor(null, categories, language, onDismiss = { newCategory = false }, onSave = {
        onSaveCategory(it); categoryId = it.id; newCategory = false
    })
}

@Composable
private fun CategoryEditor(category: PackingCategory?, categories: List<PackingCategory>, language: AppLanguage,
    onDismiss: () -> Unit, onSave: (PackingCategory) -> Unit) {
    var hi by rememberSaveable { mutableStateOf(category?.names?.hi.orEmpty()) }
    var en by rememberSaveable { mutableStateOf(category?.names?.en.orEmpty()) }
    var sa by rememberSaveable { mutableStateOf(category?.names?.sa.orEmpty()) }
    val names = Names(hi, en, sa)
    val candidate = PackingCategory(category?.id ?: "new-category", names.trimmed())
    val duplicate = categoryNameExists(categories, candidate)
    EditorDialog(stringResource(if (category == null) R.string.add_category else R.string.edit_category), onDismiss,
        confirmEnabled = names.isNotBlank() && !duplicate, confirmTag = "save_category", onConfirm = {
            onSave(candidate.copy(id = category?.id ?: UUID.randomUUID().toString()))
        }) {
        NameFields(names, { hi = it.hi; en = it.en; sa = it.sa }, language, "category")
        if (duplicate) Text(stringResource(R.string.duplicate_category), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
internal fun CategoryManager(data: PackingData, onChange: (PackingData) -> Unit, onDismiss: () -> Unit) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<String?>(null) }
    EditorDialog(stringResource(R.string.manage_categories), onDismiss, confirmText = stringResource(R.string.done),
        onConfirm = onDismiss) {
        Text(stringResource(R.string.category_hint), style = MaterialTheme.typography.bodyMedium)
        FilledTonalButton(onClick = { editingId = "" }, modifier = Modifier.fillMaxWidth().testTag("add_category")) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(stringResource(R.string.add_category))
        }
        data.categories.forEach { category ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(category.names.display(data.language), fontWeight = FontWeight.Medium)
                    Text(stringResource(R.string.category_item_count, data.items.count { it.categoryId == category.id }),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { editingId = category.id }, modifier = Modifier.testTag("edit_category_${category.id}")) {
                    Icon(Icons.Default.Edit, stringResource(R.string.edit_named_item, category.names.display(data.language)), Modifier.size(20.dp))
                }
                if (category.id != "OTHER") IconButton(onClick = { deletingId = category.id },
                    modifier = Modifier.testTag("delete_category_${category.id}")) {
                    Icon(Icons.Default.Delete, stringResource(R.string.delete_named_item, category.names.display(data.language)), Modifier.size(20.dp))
                } else Spacer(Modifier.width(48.dp))
            }
            HorizontalDivider()
        }
    }
    editingId?.let { id -> CategoryEditor(data.categories.find { it.id == id }, data.categories, data.language,
        onDismiss = { editingId = null }, onSave = { onChange(data.saveCategory(it)); editingId = null }) }
    deletingId?.let { id -> data.categories.find { it.id == id }?.let { category ->
        AlertDialog(onDismissRequest = { deletingId = null }, title = { Text(stringResource(R.string.category_delete_title)) },
            text = { Text(stringResource(R.string.category_delete_message, category.names.display(data.language),
                data.items.count { it.categoryId == id }, data.categories.first { it.id == "OTHER" }.names.display(data.language))) },
            confirmButton = { TextButton(onClick = { onChange(data.deleteCategory(id)); deletingId = null },
                modifier = Modifier.testTag("confirm_delete_category")) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text(stringResource(R.string.cancel)) } })
    } }
}

@Composable
internal fun SettingsDialog(data: PackingData, onChange: (PackingData) -> Unit, onDismiss: () -> Unit,
    onExport: () -> Unit, onImport: () -> Unit) {
    EditorDialog(stringResource(R.string.settings), onDismiss, confirmText = stringResource(R.string.done), onConfirm = onDismiss) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        AppLanguage.entries.forEach { language ->
            ChoiceRow(language.nativeName, data.language == language, "language_${language.tag}") { onChange(data.copy(language = language)) }
        }
        HorizontalDivider()
        Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        listOf(ThemeMode.SYSTEM to R.string.system_theme, ThemeMode.LIGHT to R.string.light_theme,
            ThemeMode.DARK to R.string.dark_theme).forEach { (mode, label) ->
            ChoiceRow(stringResource(label), data.theme == mode, "theme_${mode.name.lowercase()}") { onChange(data.copy(theme = mode)) }
        }
        Text(stringResource(R.string.settings_hint), style = MaterialTheme.typography.bodySmall)
        HorizontalDivider()
        Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.backup_hint), style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.export_backup)) }
        OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_backup)) }
        Text(stringResource(R.string.local_storage), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ChoiceRow(title: String, selected: Boolean, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(selected, role = Role.RadioButton, onClick = onClick)
        .testTag(tag), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected, onClick = null)
        Spacer(Modifier.width(12.dp)); Text(title)
    }
}
