package com.example.packingchecklisthindi

import android.content.Context
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

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

    @Test fun firstLaunchLoadsAllDefaultItems() {
        val items = store.load()
        assertEquals(27, items.size)
        assertEquals(items.size, items.map { it.id }.distinct().size)
        assertEquals(Category.entries.toSet(), items.map { it.category }.toSet())
        assertTrue(items.none { it.packed })
    }

    @Test fun savedHindiNamesAndPackedStatusSurviveNewStore() {
        val saved = listOf(Entry("custom", "दवाइयाँ \"विशेष\"", "Daily medicines", Category.MEDICINES, true))
        store.save(saved)
        assertEquals(saved, Store(context).load())
    }

    @Test fun deletingAllItemsKeepsAnEmptyChecklist() {
        store.save(emptyList())
        assertTrue(Store(context).load().isEmpty())
    }

    @Test fun uncheckingPreservesCustomItemsAndDeletedDefaults() {
        val items = listOf(Entry("custom", "मेरी पुस्तक", "", Category.OTHER, true))
        store.save(items.map { it.copy(packed = false) })
        assertEquals(items.map { it.copy(packed = false) }, Store(context).load())
    }

    @Test fun restoringDefaultsRemovesCustomItems() {
        store.save(listOf(Entry("custom", "मेरी पुस्तक", "", Category.OTHER, true)))
        store.save(defaults())
        assertEquals(defaults(), Store(context).load())
    }

    @Test fun corruptSavedDataFallsBackToDefaultChecklist() {
        context.getSharedPreferences("packing_hindi", Context.MODE_PRIVATE)
            .edit().putString("items", "invalid-json").commit()
        assertEquals(defaults(), store.load())
    }
}
