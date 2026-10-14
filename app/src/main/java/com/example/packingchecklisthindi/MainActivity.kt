package com.example.packingchecklisthindi

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

internal enum class Category(val label: String) {
    DOCUMENTS("दस्तावेज़"), CLOTHES("कपड़े"), TOILETRIES("प्रसाधन सामग्री"),
    ELECTRONICS("इलेक्ट्रॉनिक्स"), MEDICINES("दवाइयाँ"), OTHER("अन्य")
}
internal data class Entry(val id: String, val hi: String, val en: String, val category: Category, val packed: Boolean = false)
internal fun defaults(): List<Entry> {
    val specs = listOf(
        Triple(Category.DOCUMENTS, "आधार कार्ड", "Aadhaar card"),
        Triple(Category.DOCUMENTS, "टिकट", "Tickets"),
        Triple(Category.DOCUMENTS, "पहचान पत्र", "Photo ID"),
        Triple(Category.DOCUMENTS, "पासपोर्ट", "Passport"),
        Triple(Category.DOCUMENTS, "होटल बुकिंग", "Hotel reservation"),
        Triple(Category.CLOTHES, "टी-शर्ट", "T-shirts"),
        Triple(Category.CLOTHES, "पैंट", "Trousers"),
        Triple(Category.CLOTHES, "अंतर्वस्त्र", "Underwear"),
        Triple(Category.CLOTHES, "मोज़े", "Socks"),
        Triple(Category.CLOTHES, "जूते", "Shoes"),
        Triple(Category.CLOTHES, "रात के कपड़े", "Sleepwear"),
        Triple(Category.TOILETRIES, "टूथब्रश", "Toothbrush"),
        Triple(Category.TOILETRIES, "टूथपेस्ट", "Toothpaste"),
        Triple(Category.TOILETRIES, "साबुन", "Soap"),
        Triple(Category.TOILETRIES, "शैम्पू", "Shampoo"),
        Triple(Category.TOILETRIES, "तौलिया", "Towel"),
        Triple(Category.ELECTRONICS, "मोबाइल फ़ोन", "Mobile phone"),
        Triple(Category.ELECTRONICS, "चार्जर", "Charger"),
        Triple(Category.ELECTRONICS, "पावर बैंक", "Power bank"),
        Triple(Category.ELECTRONICS, "ईयरफ़ोन", "Earphones"),
        Triple(Category.MEDICINES, "नियमित दवाइयाँ", "Daily medicines"),
        Triple(Category.MEDICINES, "प्राथमिक उपचार किट", "First aid kit"),
        Triple(Category.MEDICINES, "दर्द निवारक", "Pain reliever"),
        Triple(Category.MEDICINES, "बैंडेज", "Bandages"),
        Triple(Category.OTHER, "पानी की बोतल", "Water bottle"),
        Triple(Category.OTHER, "छाता", "Umbrella"),
        Triple(Category.OTHER, "धूप का चश्मा", "Sunglasses")
    )
    return specs.mapIndexed { index, t -> Entry("default-$index", t.second, t.third, t.first) }
}
internal class Store(context: Context) {
    private val pref = context.getSharedPreferences("packing_hindi", Context.MODE_PRIVATE)
    fun load(): List<Entry> {
        val raw = pref.getString("items", null) ?: return defaults()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Entry(o.getString("id"), o.getString("hi"), o.optString("en"),
                    Category.valueOf(o.getString("category")), o.optBoolean("packed"))
            }
        } catch (_: Exception) { defaults() }
    }
    fun save(list: List<Entry>) {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id); put("hi", e.hi); put("en", e.en)
                put("category", e.category.name); put("packed", e.packed)
            })
        }
        pref.edit().putString("items", arr.toString()).apply()
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = Store(applicationContext)
        setContent { MaterialTheme(colorScheme = lightColorScheme()) { App(store) } }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun App(store: Store) {
    val entries = remember { mutableStateListOf<Entry>().apply { addAll(store.load()) } }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var addOpen by rememberSaveable { mutableStateOf(false) }
    var resetOpen by rememberSaveable { mutableStateOf(false) }
    fun update(list: List<Entry>) { entries.clear(); entries.addAll(list); store.save(entries.toList()) }
    val count = entries.count { it.packed }
    val fraction = if (entries.isEmpty()) 0f else count.toFloat() / entries.size
    val animated by animateFloatAsState(fraction, animationSpec = tween(550), label = "progress")
    val visible = entries.filter {
        (it.hi.contains(query.trim(), true) || it.en.contains(query.trim(), true)) &&
            (filter == 0 || (filter == 1 && !it.packed) || (filter == 2 && it.packed))
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("छुट्टियों की पैकिंग सूची") },
            actions = { TextButton(onClick = { resetOpen = true }) { Text("रीसेट") } }) },
        floatingActionButton = { ExtendedFloatingActionButton(
            onClick = { addOpen = true }) { Text("+ सामान जोड़ें") } }
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("पैकिंग की प्रगति", style = MaterialTheme.typography.titleMedium)
                            Text("$count / ${entries.size} सामान पैक किया गया")
                        }
                        Surface(color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(99.dp)) {
                            Text("${(fraction * 100).toInt()}%",
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    LinearProgressIndicator(progress = { animated },
                        modifier = Modifier.fillMaxWidth().height(10.dp))
                }
            }
            OutlinedTextField(query, { query = it }, singleLine = true,
                label = { Text("सामान खोजें / Search") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("सभी", "बाकी", "पैक किए गए").forEachIndexed { index, title ->
                    FilterChip(selected = filter == index, onClick = { filter = index },
                        label = { Text(title) })
                }
            }
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Category.entries.forEach { category ->
                    val group = visible.filter { it.category == category }
                    if (group.isNotEmpty()) {
                        item(key = "heading-${category.name}") {
                            Text(category.label, fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(top = 12.dp))
                        }
                        items(group, key = { it.id }) { entry ->
                            Card(Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(entry.packed, onCheckedChange = { checked ->
                                        update(entries.map { if (it.id == entry.id) it.copy(packed = checked) else it })
                                    })
                                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                        Text(entry.hi)
                                        if (entry.en.isNotBlank()) Text(entry.en,
                                            style = MaterialTheme.typography.bodySmall)
                                    }
                                    TextButton(onClick = { update(entries.filterNot { it.id == entry.id }) }) {
                                        Text("हटाएँ")
                                    }
                                }
                            }
                        }
                    }
                }
                if (visible.isEmpty()) item { Text("कोई सामान नहीं मिला।") }
            }
        }
    }
    if (addOpen) AddDialog(onDismiss = { addOpen = false }, onAdd = { hi, en, cat ->
        update(entries.toList() + Entry(UUID.randomUUID().toString(), hi, en, cat))
        addOpen = false
    })
    if (resetOpen) AlertDialog(onDismissRequest = { resetOpen = false },
        title = { Text("सूची रीसेट करें") },
        text = { Text("सभी सामान अनचेक करें या मूल 27 सामान वापस लाएँ।") },
        confirmButton = { Button(onClick = { update(defaults()); resetOpen = false }) {
            Text("मूल सूची लौटाएँ")
        } },
        dismissButton = { Column {
            TextButton(onClick = { update(entries.map { it.copy(packed = false) }); resetOpen = false }) {
                Text("सभी अनचेक करें")
            }
            TextButton(onClick = { resetOpen = false }) { Text("रद्द करें") }
        } })
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddDialog(onDismiss: () -> Unit, onAdd: (String, String, Category) -> Unit) {
    var hi by rememberSaveable { mutableStateOf("") }
    var en by rememberSaveable { mutableStateOf("") }
    var cat by rememberSaveable { mutableStateOf(Category.OTHER) }
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("नया सामान जोड़ें") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(hi, { hi = it }, label = { Text("सामान का नाम (हिन्दी) *") },
                singleLine = true)
            OutlinedTextField(en, { en = it }, label = { Text("English name (optional)") },
                singleLine = true)
            ExposedDropdownMenuBox(expanded = expanded,
                onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(value = cat.label, onValueChange = {}, readOnly = true,
                    label = { Text("श्रेणी") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable))
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    Category.entries.forEach { c ->
                        DropdownMenuItem(text = { Text(c.label) },
                            onClick = { cat = c; expanded = false })
                    }
                }
            }
        } },
        confirmButton = { Button(enabled = hi.isNotBlank(),
            onClick = { onAdd(hi.trim(), en.trim(), cat) }) { Text("जोड़ें") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("रद्द करें") } })
}
