package com.example.nutricionalregister.navigation

import android.content.ContentValues
import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nutricionalregister.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Verifies spec 008's primary acceptance path against the real application composition root. */
@RunWith(AndroidJUnit4::class)
class Spec008AppIntegrationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loggingFromTheDiaryReturnsToSearchThenBackShowsOneEntryInTheSelectedMeal() {
        seedSearchCache()

        rule.onNodeWithContentDescription("Add food to Breakfast").performClick()
        rule.onNodeWithTag("add-food-query").performTextInput(SEARCH_TERM)
        rule.onNodeWithTag("add-food-search-submit").performClick()
        rule.onNodeWithText(PRODUCT_NAME).assertIsDisplayed()
        rule.onNodeWithText(PRODUCT_NAME).performClick()
        rule.onNodeWithTag("product-add-to-diary").performClick()
        rule.onNodeWithText("Breakfast").assertIsDisplayed()

        rule.onNodeWithTag("nutrilog-coach-fab").performClick()
        rule.onNodeWithTag("coach-dialog").assertIsDisplayed()
        rule.onNodeWithTag("coach-back").performClick()
        rule.onNodeWithTag("add-food-quantity").assertIsDisplayed()
        rule.onNodeWithText("Breakfast").assertIsDisplayed()

        rule.onNodeWithTag("add-food-confirm").performScrollTo()
        rule.onNodeWithTag("add-food-confirm").assertIsEnabled()
        rule.onNodeWithTag("add-food-confirm").performClick()

        waitForDiaryEntry()
        waitForTag("add-food-query")
        rule.onNodeWithTag("add-food-query").assertIsDisplayed()
        rule.onNodeWithTag("product-add-to-diary").assertDoesNotExist()
        rule.onNodeWithTag("add-food-back").performClick()

        rule.onNodeWithText("Breakfast").assertIsDisplayed()
        rule.onAllNodesWithText(PRODUCT_NAME).assertCountEquals(1)
    }

    @Test
    fun loggingAProductOpenedFromTheLibraryReturnsToTheLibraryAndUpdatesTheDiary() {
        seedSearchCache()

        rule.onNodeWithTag("bottom-bar-library").performClick()
        rule.onNodeWithTag("library-add-$PRODUCT_CODE").performClick()
        rule.onNodeWithTag("add-food-chosen-product").assertIsDisplayed()
        rule.onNodeWithTag("add-food-open-meal-sheet").performScrollTo()
        rule.onNodeWithTag("add-food-open-meal-sheet").performClick()
        waitForTag("add-food-meal-sheet")
        waitForText("Lunch")
        rule.onNodeWithText("Lunch").assertIsDisplayed()
        rule.onNodeWithText("Lunch").performClick()
        rule.onNodeWithTag("add-food-confirm").performScrollTo()
        rule.onNodeWithTag("add-food-confirm").assertIsEnabled()
        rule.onNodeWithTag("add-food-confirm").performClick()

        waitForDiaryEntry()
        waitForTag("add-food-query")
        rule.onNodeWithTag("add-food-query").assertIsDisplayed()
        rule.onNodeWithTag("add-food-back").performClick()
        rule.onNodeWithTag("library-add-$PRODUCT_CODE").assertIsDisplayed()
        rule.onNodeWithTag("bottom-bar-today").performClick()
        rule.onNodeWithText("Lunch").assertIsDisplayed()
        rule.onAllNodesWithText(PRODUCT_NAME).assertCountEquals(1)
    }

    @Test
    fun editingGoalsAndClosingPrivacyKeepsTheAppInMoreWithTheGoalChange() {
        seedSearchCache()

        rule.onNodeWithTag("bottom-bar-more").performClick()
        rule.onNodeWithTag("goal-field-energy").performTextClearance()
        rule.onNodeWithTag("goal-field-energy").performTextInput("2500")
        hideKeyboard()
        rule.onNodeWithTag("more-privacy-row").performScrollTo()
        rule.onNodeWithTag("more-privacy-row").performClick()
        waitForText("Privacy")
        rule.onNodeWithText("Privacy").assertIsDisplayed()
        rule.onNodeWithText("Close").performClick()

        rule.onNodeWithTag("more-goal-section").assertIsDisplayed()
        rule.onNodeWithTag("goal-field-energy").assertTextContains("2500")
        rule.onNodeWithTag("bottom-bar-more").assertIsDisplayed()
    }

    @Test
    fun systemBackAtTheRealFlowsSourceReturnsToTheDiary() {
        seedSearchCache()

        rule.onNodeWithContentDescription("Add food to Breakfast").performClick()
        rule.onNodeWithTag("add-food-query").assertIsDisplayed()
        rule.activityRule.scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }

        rule.waitForIdle()
        rule.onNodeWithText("Today").assertIsDisplayed()
        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()
    }

    private fun waitForTag(tag: String) {
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        try {
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            rule.onRoot(useUnmergedTree = true).printToLog("navigation-debug-$tag")
            val diagnosticTags =
                listOf(
                    "add-food-query",
                    "add-food-confirm",
                    "add-food-meal-sheet",
                    "product-add-to-diary",
                    "more-privacy-row",
                    "nutrilog-bottom-bar",
                ).associateWith { candidate ->
                    rule.onAllNodesWithTag(candidate).fetchSemanticsNodes().size
                }
            throw AssertionError("Timed out waiting for $tag; current nodes: $diagnosticTags", failure)
        }
    }

    private fun waitForDiaryEntry() {
        rule.waitUntil(15_000) {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { database ->
                database.rawQuery("SELECT COUNT(*) FROM DiaryEntry", null).use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) == 1
                }
            }
        }
    }

    private fun waitForText(text: String) {
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        try {
            rule.waitUntil(15_000) {
                rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            rule.onRoot(useUnmergedTree = true).printToLog("navigation-debug-$text")
            val diagnosticTags =
                listOf(
                    "add-food-meal-sheet",
                    "add-food-query",
                    "add-food-confirm",
                    "more-privacy-row",
                    "nutrilog-bottom-bar",
                ).associateWith { candidate ->
                    rule.onAllNodesWithTag(candidate).fetchSemanticsNodes().size
                }
            throw AssertionError("Timed out waiting for text $text; current nodes: $diagnosticTags", failure)
        }
    }

    private fun hideKeyboard() {
        rule.activityRule.scenario.onActivity { activity ->
            val inputMethodManager =
                activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
        }
        rule.waitForIdle()
    }

    private fun seedSearchCache() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { database ->
            database.beginTransaction()
            try {
                database.delete("DiaryEntry", null, null)
                database.delete("NutritionGoal", null, null)
                database.delete("SearchSnapshotProduct", "normalized_term = ?", arrayOf(SEARCH_TERM))
                database.delete("SearchSnapshot", "normalized_term = ?", arrayOf(SEARCH_TERM))
                database.delete("ProductNutrition", "product_code = ?", arrayOf(PRODUCT_CODE))
                database.delete("Product", "code = ?", arrayOf(PRODUCT_CODE))
                database.delete("SavedProduct", "code = ?", arrayOf(PRODUCT_CODE))

                database.insertOrThrow(
                    "NutritionGoal",
                    null,
                    ContentValues().apply {
                        put("singleton", 1)
                        put("energy_kcal", 2000)
                        put("protein_grams", 120)
                        put("carbohydrate_grams", 220)
                        put("fat_grams", 70)
                    },
                )

                database.insertOrThrow(
                    "Product",
                    null,
                    ContentValues().apply {
                        put("code", PRODUCT_CODE)
                        put("name", PRODUCT_NAME)
                        put("brands", "Spec fixture")
                        put("quantity", "500 g")
                        put("nutrition_data_per", "100g")
                        put("nutri_score_grade", "a")
                        put("cached_at_epoch_millis", System.currentTimeMillis())
                    },
                )
                database.insertOrThrow(
                    "ProductNutrition",
                    null,
                    ContentValues().apply {
                        put("product_code", PRODUCT_CODE)
                        put("nutrient_key", "energy-kcal_100g")
                        put("amount", 380.0)
                        put("unit", "kcal")
                        put("basis", "PER_100_G")
                    },
                )
                database.insertOrThrow(
                    "SavedProduct",
                    null,
                    ContentValues().apply {
                        put("code", PRODUCT_CODE)
                        put("name", PRODUCT_NAME)
                        put("brands", "Spec fixture")
                        put("quantity", "500 g")
                        put("nutri_score_grade", "a")
                        putNull("image_thumbnail_url")
                        put("saved_at_epoch_millis", System.currentTimeMillis())
                    },
                )
                database.insertOrThrow(
                    "SearchSnapshot",
                    null,
                    ContentValues().apply {
                        put("normalized_term", SEARCH_TERM)
                        put("display_term", SEARCH_TERM)
                        put("fetched_at_epoch_millis", System.currentTimeMillis())
                    },
                )
                database.insertOrThrow(
                    "SearchSnapshotProduct",
                    null,
                    ContentValues().apply {
                        put("normalized_term", SEARCH_TERM)
                        put("product_code", PRODUCT_CODE)
                        put("result_position", 0)
                    },
                )
                database.setTransactionSuccessful()
            } finally {
                database.endTransaction()
            }
        }
        rule.activityRule.scenario.recreate()
        rule.waitForIdle()
    }

    private companion object {
        const val DATABASE_NAME = "product-search.db"
        const val SEARCH_TERM = "spec oats"
        const val PRODUCT_CODE = "spec-008-integration-oats"
        const val PRODUCT_NAME = "Spec Integration Oats"
    }
}
