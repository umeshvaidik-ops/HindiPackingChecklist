package com.example.packingchecklisthindi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

internal enum class AppLanguage(val tag: String, val nativeName: String) {
    HINDI("hi", "हिन्दी"), ENGLISH("en", "English"), SANSKRIT("sa", "संस्कृतम्")
}
internal enum class ThemeMode { SYSTEM, LIGHT, DARK }
internal data class Names(val hi: String = "", val en: String = "", val sa: String = "") {
    fun display(language: AppLanguage): String = when (language) {
        AppLanguage.HINDI -> hi
        AppLanguage.ENGLISH -> en
        AppLanguage.SANSKRIT -> sa
    }.ifBlank { listOf(hi, en, sa).firstOrNull { it.isNotBlank() }.orEmpty() }
    fun trimmed() = Names(hi.trim(), en.trim(), sa.trim())
    fun isNotBlank() = listOf(hi, en, sa).any { it.isNotBlank() }
}
internal data class PackingCategory(val id: String, val names: Names)
internal data class Entry(
    val id: String,
    val names: Names,
    val categoryId: String,
    val packed: Boolean = false,
    val note: String = ""
)
internal data class PackingData(
    val categories: List<PackingCategory> = defaultCategories(),
    val items: List<Entry> = defaultItems(),
    val language: AppLanguage = AppLanguage.HINDI,
    val theme: ThemeMode = ThemeMode.SYSTEM
) {
    fun saveItem(entry: Entry) = copy(items = if (items.any { it.id == entry.id })
        items.map { if (it.id == entry.id) entry else it } else items + entry)
    fun saveCategory(category: PackingCategory) = copy(categories =
        if (categories.any { it.id == category.id }) categories.map { if (it.id == category.id) category else it }
        else categories + category)
    fun deleteCategory(id: String): PackingData {
        if (id == "OTHER") return this
        return copy(categories = categories.filterNot { it.id == id },
            items = items.map { if (it.categoryId == id) it.copy(categoryId = "OTHER") else it })
    }
    fun uncheckAll() = copy(items = items.map { it.copy(packed = false) })
    fun restoreDefaults() = copy(categories = defaultCategories(), items = defaultItems())
}

internal fun defaultCategories() = listOf(
    PackingCategory("DOCUMENTS", Names("दस्तावेज़", "Documents", "प्रमाणपत्राणि")),
    PackingCategory("CLOTHES", Names("कपड़े", "Clothing", "वस्त्राणि")),
    PackingCategory("TOILETRIES", Names("प्रसाधन सामग्री", "Toiletries", "प्रसाधनसामग्री")),
    PackingCategory("ELECTRONICS", Names("इलेक्ट्रॉनिक्स", "Electronics", "विद्युदुपकरणानि")),
    PackingCategory("MEDICINES", Names("दवाइयाँ", "Medicines", "औषधानि")),
    PackingCategory("OTHER", Names("अन्य", "Other", "अन्यानि"))
)

internal fun defaultItems(): List<Entry> {
    val specs = listOf(
        "DOCUMENTS" to Names("आधार कार्ड", "Aadhaar card", "आधारपत्रम्"),
        "DOCUMENTS" to Names("टिकट", "Tickets", "यात्राचिटिकाः"),
        "DOCUMENTS" to Names("पहचान पत्र", "Photo ID", "परिचयपत्रम्"),
        "DOCUMENTS" to Names("पासपोर्ट", "Passport", "पारपत्रम्"),
        "DOCUMENTS" to Names("होटल बुकिंग", "Hotel reservation", "निवासारक्षणपत्रम्"),
        "CLOTHES" to Names("टी-शर्ट", "T-shirts", "लघुकञ्चुकाः"),
        "CLOTHES" to Names("पैंट", "Trousers", "दीर्घजङ्घिकाः"),
        "CLOTHES" to Names("अंतर्वस्त्र", "Underwear", "अन्तर्वस्त्राणि"),
        "CLOTHES" to Names("मोज़े", "Socks", "पादावरणानि"),
        "CLOTHES" to Names("जूते", "Shoes", "पादत्राणानि"),
        "CLOTHES" to Names("रात के कपड़े", "Sleepwear", "शयनवस्त्राणि"),
        "TOILETRIES" to Names("टूथब्रश", "Toothbrush", "दन्तकूर्चिका"),
        "TOILETRIES" to Names("टूथपेस्ट", "Toothpaste", "दन्तलेपः"),
        "TOILETRIES" to Names("साबुन", "Soap", "मार्जकः"),
        "TOILETRIES" to Names("शैम्पू", "Shampoo", "केशमार्जकः"),
        "TOILETRIES" to Names("तौलिया", "Towel", "मार्जनवस्त्रम्"),
        "ELECTRONICS" to Names("मोबाइल फ़ोन", "Mobile phone", "चलदूरभाषः"),
        "ELECTRONICS" to Names("चार्जर", "Charger", "आवेशकम्"),
        "ELECTRONICS" to Names("पावर बैंक", "Power bank", "विद्युत्सञ्चायकः"),
        "ELECTRONICS" to Names("ईयरफ़ोन", "Earphones", "कर्णश्रवणयन्त्रम्"),
        "MEDICINES" to Names("नियमित दवाइयाँ", "Daily medicines", "नित्यसेवनीयानि औषधानि"),
        "MEDICINES" to Names("प्राथमिक उपचार किट", "First aid kit", "प्राथमिकोपचारपेटिका"),
        "MEDICINES" to Names("दर्द निवारक", "Pain reliever", "वेदनाशामकम्"),
        "MEDICINES" to Names("बैंडेज", "Bandages", "व्रणपट्टिकाः"),
        "OTHER" to Names("पानी की बोतल", "Water bottle", "जलपात्रम्"),
        "OTHER" to Names("छाता", "Umbrella", "छत्रम्"),
        "OTHER" to Names("धूप का चश्मा", "Sunglasses", "आतपरक्षकचष्मा")
    )
    return specs.mapIndexed { index, (category, names) -> Entry("default-$index", names, category) }
}

internal fun categoryNameExists(categories: List<PackingCategory>, candidate: PackingCategory): Boolean {
    fun normalized(n: Names) = listOf(n.hi, n.en, n.sa).filter { it.isNotBlank() }
        .map { it.trim().lowercase(Locale.ROOT) }.toSet()
    return categories.any { it.id != candidate.id && normalized(it.names).intersect(normalized(candidate.names)).isNotEmpty() }
}
internal fun Entry.matches(query: String, category: PackingCategory?): Boolean {
    val needle = query.trim()
    return listOf(names.hi, names.en, names.sa, note, category?.names?.hi.orEmpty(),
        category?.names?.en.orEmpty(), category?.names?.sa.orEmpty()).any { it.contains(needle, true) }
}

internal object PackingJson {
    fun encode(data: PackingData): String = JSONObject().apply {
        put("format", "packing-checklist"); put("version", 2)
        put("language", data.language.name); put("theme", data.theme.name)
        put("categories", JSONArray().apply { data.categories.forEach { c ->
            put(JSONObject().apply { put("id", c.id); putNames(c.names) })
        } })
        put("items", JSONArray().apply { data.items.forEach { e ->
            put(JSONObject().apply {
                put("id", e.id); putNames(e.names); put("category", e.categoryId)
                put("packed", e.packed); put("note", e.note)
            })
        } })
    }.toString(2)

    fun decode(raw: String): PackingData {
        val obj = JSONObject(raw)
        require(obj.getString("format") == "packing-checklist" && obj.getInt("version") == 2)
        val cats = obj.getJSONArray("categories")
        val entries = obj.getJSONArray("items")
        require(cats.length() <= 500 && entries.length() <= 10000)
        val categories = (0 until cats.length()).map { i ->
            val c = cats.getJSONObject(i)
            PackingCategory(c.getString("id"), c.names())
        }.toMutableList()
        require(categories.all { it.id.isNotBlank() && it.names.isNotBlank() })
        require(categories.map { it.id }.distinct().size == categories.size)
        if (categories.none { it.id == "OTHER" }) categories.add(defaultCategories().last())
        val ids = categories.map { it.id }.toSet()
        val items = (0 until entries.length()).map { i ->
            val e = entries.getJSONObject(i)
            Entry(e.getString("id"), e.names(), e.optString("category").takeIf { it in ids } ?: "OTHER",
                e.optBoolean("packed"), e.optString("note"))
        }
        require(items.all { it.id.isNotBlank() && it.names.isNotBlank() })
        require(items.map { it.id }.distinct().size == items.size)
        return PackingData(categories, items,
            AppLanguage.entries.find { it.name == obj.optString("language") } ?: AppLanguage.HINDI,
            ThemeMode.entries.find { it.name == obj.optString("theme") } ?: ThemeMode.SYSTEM)
    }
    private fun JSONObject.putNames(n: Names) { put("hi", n.hi); put("en", n.en); put("sa", n.sa) }
    private fun JSONObject.names() = Names(optString("hi"), optString("en"), optString("sa")).trimmed()
}

internal class Store(context: Context) {
    private val pref = context.getSharedPreferences("packing_hindi", Context.MODE_PRIVATE)
    fun load(): PackingData {
        val current = pref.getString("data_v2", null)
        if (current != null) return try { PackingJson.decode(current) } catch (_: Exception) { PackingData() }
        // Version 1 used the same preferences file and enum names as category IDs.
        val legacy = pref.getString("items", null) ?: return PackingData()
        return try {
            val arr = JSONArray(legacy)
            val originals = defaultItems().associateBy { it.id }
            val ids = defaultCategories().map { it.id }.toSet()
            val items = (0 until arr.length()).map { i ->
                val e = arr.getJSONObject(i)
                val id = e.getString("id")
                val original = originals[id]
                val hi = e.optString("hi")
                val en = e.optString("en")
                // Supply Sanskrit only for unchanged original entries, never for custom text.
                val sa = if (original != null && original.names.hi == hi && original.names.en == en) original.names.sa else ""
                Entry(id, Names(hi, en, sa), e.optString("category").takeIf { it in ids } ?: "OTHER",
                    e.optBoolean("packed"))
            }
            require(items.map { it.id }.distinct().size == items.size)
            PackingData(items = items)
        } catch (_: Exception) { PackingData() }
    }
    fun save(data: PackingData) {
        pref.edit().putString("data_v2", PackingJson.encode(data)).apply()
    }
}
