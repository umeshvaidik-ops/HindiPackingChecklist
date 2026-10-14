package com.example.packingchecklisthindi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class StoreTest {
    private lateinit var context: Context
    private lateinit var store: Store
    @Before fun setUp() {
        context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("packing_hindi", Context.MODE_PRIVATE).edit().clear().commit()
        store = Store(context)
    }
    @Test fun firstLaunchHasTranslatedDefaultsAndUniqueIds() {
        val data = store.load()
        assertEquals(27, data.items.size)
        assertEquals(6, data.categories.size)
        assertEquals(27, data.items.map { it.id }.distinct().size)
        assertTrue(data.items.none { it.packed })
        assertTrue((data.items.map { it.names } + data.categories.map { it.names }).all {
            it.hi.isNotBlank() && it.en.isNotBlank() && it.sa.isNotBlank()
        })
    }
    @Test fun notesCustomCategoriesTranslationsAndSettingsSurviveReload() {
        val category = PackingCategory("books", Names("पुस्तकें", "Books", "पुस्तकानि"))
        val entry = Entry("custom", Names("पुस्तक", "Book", "पुस्तकम्"), "books", true, "मेरी टिप्पणी\nHandle with care")
        val data = PackingData().saveCategory(category).saveItem(entry)
            .copy(language = AppLanguage.SANSKRIT, theme = ThemeMode.DARK)
        store.save(data)
        assertEquals(data, Store(context).load())
    }
    @Test fun legacyMigrationKeepsCheckedItemsAndCustomTextWithoutRestoringDeletedItems() {
        val old = JSONArray().put(JSONObject().apply {
            put("id", "default-0"); put("hi", "आधार कार्ड"); put("en", "Aadhaar card")
            put("category", "DOCUMENTS"); put("packed", true)
        }).put(JSONObject().apply {
            put("id", "my-item"); put("hi", "मेरी विशेष वस्तु"); put("en", "")
            put("category", "OTHER"); put("packed", false)
        })
        context.getSharedPreferences("packing_hindi", Context.MODE_PRIVATE).edit().putString("items", old.toString()).commit()
        val migrated = store.load()
        assertEquals(2, migrated.items.size)
        assertTrue(migrated.items.first().packed)
        assertEquals("आधारपत्रम्", migrated.items.first().names.sa)
        assertEquals("मेरी विशेष वस्तु", migrated.items.last().names.hi)
        assertEquals("", migrated.items.last().names.sa)
        store.save(migrated)
        assertEquals(migrated, Store(context).load())
    }
    @Test fun emptyLegacyAndCurrentListsStayEmpty() {
        context.getSharedPreferences("packing_hindi", Context.MODE_PRIVATE).edit().putString("items", "[]").commit()
        assertTrue(store.load().items.isEmpty())
        store.save(PackingData(items = emptyList()))
        assertTrue(Store(context).load().items.isEmpty())
    }
    @Test fun editingAnItemPreservesIdentityPositionAndPackedStatus() {
        val data = PackingData()
        val updated = data.items[3].copy(names = Names("नया नाम"), note = "सुरक्षित रखें", packed = true)
        val saved = data.saveItem(updated)
        assertEquals(27, saved.items.size)
        assertEquals(updated, saved.items[3])
        store.save(saved)
        assertEquals(updated, store.load().items[3])
    }
    @Test fun deletingCategoryMovesItemsAndNotesToOtherWithoutDataLoss() {
        val category = PackingCategory("custom", Names(en = "Books"))
        val entry = Entry("book", Names(en = "Book"), "custom", true, "Keep dry")
        val before = PackingData().saveCategory(category).saveItem(entry)
        val after = before.deleteCategory("custom")
        assertEquals(before.items.size, after.items.size)
        assertFalse(after.categories.any { it.id == "custom" })
        assertEquals(entry.copy(categoryId = "OTHER"), after.items.last())
        assertEquals(after, after.deleteCategory("OTHER"))
    }
    @Test fun renamingCategoryKeepsItemAssociations() {
        val before = PackingData()
        val category = before.categories.first().copy(names = Names(en = "Travel papers"))
        val after = before.saveCategory(category)
        assertEquals(before.items, after.items)
        assertEquals(6, after.categories.size)
        assertEquals("Travel papers", after.categories.first().names.en)
    }
    @Test fun uncheckingPreservesNotesCustomItemsAndCategories() {
        val entry = Entry("custom", Names(en = "Book"), "OTHER", true, "Gift")
        val data = PackingData(items = listOf(entry)).uncheckAll()
        assertEquals(listOf(entry.copy(packed = false)), data.items)
    }
    @Test fun restoreDefaultsKeepsLanguageAndTheme() {
        val data = PackingData(items = emptyList(), language = AppLanguage.ENGLISH, theme = ThemeMode.DARK).restoreDefaults()
        assertEquals(27, data.items.size)
        assertEquals(AppLanguage.ENGLISH, data.language)
        assertEquals(ThemeMode.DARK, data.theme)
    }
    @Test fun searchFindsNotesSanskritAndCategoryNames() {
        val entry = Entry("x", Names("पुस्तक", "Book", "पुस्तकम्"), "x", note = "Blue cabin bag")
        val category = PackingCategory("x", Names(en = "Reading"))
        assertTrue(entry.matches("CABIN", category))
        assertTrue(entry.matches("पुस्तकम्", category))
        assertTrue(entry.matches("reading", category))
        assertFalse(entry.matches("laptop", category))
    }
    @Test fun missingNameTranslationFallsBackToAvailableName() {
        assertEquals("Books", Names(en = "Books").display(AppLanguage.SANSKRIT))
        assertEquals("पुस्तकानि", Names("पुस्तकें", "Books", "पुस्तकानि").display(AppLanguage.SANSKRIT))
    }
    @Test fun duplicateCategoryNamesAreRejectedIgnoringWhitespaceAndCase() {
        val categories = listOf(PackingCategory("books", Names(en = "Books")))
        assertTrue(categoryNameExists(categories, PackingCategory("new", Names(en = " books "))))
        assertFalse(categoryNameExists(categories, categories.single()))
    }
    @Test fun backupRoundTripsAllDataIncludingMultilineNotes() {
        val data = PackingData().saveItem(Entry("note", Names(sa = "पुस्तकम्"), "OTHER", note = "One\nTwo \"quoted\""))
        assertEquals(data, PackingJson.decode(PackingJson.encode(data)))
    }
    @Test fun invalidBackupDoesNotReplaceSavedData() {
        val data = PackingData(language = AppLanguage.ENGLISH)
        store.save(data)
        assertTrue(runCatching { PackingJson.decode("{\"format\":\"different\"}") }.isFailure)
        assertEquals(data, store.load())
    }
    @Test fun duplicateIdsInBackupAreRejected() {
        val data = PackingData(items = listOf(defaultItems()[0], defaultItems()[0]))
        assertTrue(runCatching { PackingJson.decode(PackingJson.encode(data)) }.isFailure)
    }
    @Test fun importedUnknownCategoryFallsBackToOther() {
        val data = PackingData(items = listOf(Entry("x", Names(en = "Book"), "missing", note = "Keep")))
        assertEquals("OTHER", PackingJson.decode(PackingJson.encode(data)).items.single().categoryId)
    }
    @Test fun oversizedBackupIsRejectedBeforeParsing() {
        assertTrue(runCatching { readBackup(ByteArrayInputStream(ByteArray(2 * 1024 * 1024 + 1))) }.isFailure)
    }
    @Test fun malformedSavedDataRecoversDefaults() {
        context.getSharedPreferences("packing_hindi", Context.MODE_PRIVATE).edit().putString("data_v2", "broken").commit()
        assertEquals(PackingData(), store.load())
    }
}
