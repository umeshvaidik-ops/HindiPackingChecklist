package com.example.packingchecklisthindi

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "w412dp-h915dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class ChecklistUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Before fun resetData() {
        compose.activity.getSharedPreferences("packing_hindi", Context.MODE_PRIVATE).edit().clear().commit()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
    }

    @Test fun languagesAndDayNightModesUpdateTheWholeUiAndPersistAfterRecreation() {
        compose.onNodeWithText("पैकिंग सूची").assertIsDisplayed()
        snapshot("hindi-day")
        compose.onNodeWithTag("settings").performClick()
        compose.onNodeWithTag("language_en").performScrollTo().performClick()
        compose.onNodeWithTag("theme_dark").performScrollTo().performClick()
        compose.onNodeWithTag("confirm").performClick()
        compose.onNodeWithText("Packing Checklist").assertIsDisplayed()
        assertEquals(ThemeMode.DARK, Store(compose.activity).load().theme)
        snapshot("english-night")
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        compose.onNodeWithText("Packing Checklist").assertIsDisplayed()
        compose.onNodeWithTag("settings").performClick()
        compose.onNodeWithTag("theme_dark").performScrollTo().assertIsSelected()
        compose.onNodeWithTag("language_sa").performScrollTo().performClick()
        compose.onNodeWithTag("theme_light").performScrollTo().performClick()
        compose.onNodeWithTag("confirm").performClick()
        compose.onNodeWithText("यात्रासामग्रीसूची").assertIsDisplayed()
        assertEquals(AppLanguage.SANSKRIT, Store(compose.activity).load().language)
        snapshot("sanskrit-day")
    }

    @Test fun newCategoryAndItemNoteCanBeCreatedEditedSearchedAndRestored() {
        compose.onNodeWithTag("add_item").performClick()
        compose.onNodeWithTag("item_name_hi").performTextInput("पूजा की पुस्तक")
        compose.onNodeWithTag("add_category_inline").performScrollTo().performClick()
        compose.onNodeWithTag("category_name_hi").performTextInput("पूजा सामग्री")
        compose.onNodeWithTag("save_category").performClick()
        compose.onNodeWithTag("item_note").performScrollTo().performTextInput("Keep in the blue bag")
        compose.onNodeWithTag("save_item").performClick()
        var data = Store(compose.activity).load()
        val custom = data.items.single { it.names.hi == "पूजा की पुस्तक" }
        assertEquals("पूजा सामग्री", data.categories.single { it.id == custom.categoryId }.names.hi)
        compose.onNodeWithTag("search").performScrollTo().performTextInput("blue bag")
        compose.onNodeWithTag("checklist").performScrollToNode(hasTestTag("packed_${custom.id}"))
        compose.onNodeWithText("पूजा की पुस्तक").assertIsDisplayed()
        compose.onNodeWithTag("packed_${custom.id}").performClick()
        compose.onNodeWithTag("edit_${custom.id}").performClick()
        compose.onNodeWithTag("item_note").performScrollTo().performTextReplacement("Keep in the blue bag — upstairs")
        compose.onNodeWithTag("save_item").performClick()
        compose.activityRule.scenario.recreate()
        compose.waitForIdle()
        data = Store(compose.activity).load()
        assertTrue(data.items.single { it.id == custom.id }.packed)
        assertEquals("Keep in the blue bag — upstairs", data.items.single { it.id == custom.id }.note)
        compose.onNodeWithTag("checklist").performScrollToNode(hasTestTag("packed_${custom.id}"))
        compose.onNodeWithTag("packed_${custom.id}").assertIsOn()
        snapshot("custom-note")
    }

    @Test fun deletionCanBeUndoneWithoutLosingPackedStatusOrPosition() {
        compose.onNodeWithTag("checklist").performScrollToNode(hasTestTag("packed_default-0"))
        compose.onNodeWithTag("packed_default-0").performClick()
        compose.onNodeWithTag("delete_default-0").performClick()
        assertEquals(26, Store(compose.activity).load().items.size)
        compose.onNodeWithText("वापस लाएँ").performClick()
        val data = Store(compose.activity).load()
        assertEquals(27, data.items.size)
        assertEquals("default-0", data.items.first().id)
        assertTrue(data.items.first().packed)
    }

    private fun snapshot(name: String) {
        compose.waitForIdle()
        compose.runOnIdle {
            val view = compose.activity.window.decorView
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            val output = File("build/reports/ui/$name.png")
            output.parentFile?.mkdirs()
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
