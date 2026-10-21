package com.example.nutricionalregister.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.example.nutricionalregister.AppShell
import com.example.nutricionalregister.ui.NutriLogDestination
import org.junit.Rule
import org.junit.Test

/**
 * T023: the bar's destinations are accessible by name and the current destination
 * is not conveyed by colour alone, and the bar stays usable at large text sizes
 * (FR-017, SC-004).
 */
class BottomBarAccessibilityTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eachDestinationExposesItsLabelAsItsAccessibleName() {
        composeRule.setContent {
            MaterialTheme {
                AppShell(
                    searchContent = { Text("Search", modifier = Modifier.testTag("search-slot")) },
                    savedContent = { _, _ -> Text("Saved products", modifier = Modifier.testTag("saved-slot")) },
                    diaryContent = { Text("Diary", modifier = Modifier.testTag("diary-slot")) },
                )
            }
        }

        // each destination item carries its label, so it has an accessible name
        composeRule.onNodeWithTag("bottom-bar-search").assert(hasText(NutriLogDestination.SEARCH.label))
        composeRule.onNodeWithTag("bottom-bar-saved").assert(hasText(NutriLogDestination.SAVED.label))
        composeRule.onNodeWithTag("bottom-bar-diary").assert(hasText(NutriLogDestination.DIARY.label))

        // selection is exposed as a semantic selected state, not colour alone
        composeRule.onNodeWithTag("bottom-bar-search").assertIsSelected()
    }

    @Test
    fun destinationsRemainReachableAtTheLargestSupportedTextScale() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = MAX_TEXT_SCALE)) {
                MaterialTheme {
                    AppShell(
                        searchContent = { Text("Search", modifier = Modifier.testTag("search-slot")) },
                        savedContent = { _, _ -> Text("Saved products", modifier = Modifier.testTag("saved-slot")) },
                        diaryContent = { Text("Diary", modifier = Modifier.testTag("diary-slot")) },
                    )
                }
            }
        }

        // every destination is still displayable and selectable at the largest scale
        composeRule.onNodeWithTag("bottom-bar-saved").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-bar-saved").performClick()
        composeRule.onNodeWithTag("saved-slot").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-bar-saved").assertIsSelected()
    }

    private companion object {
        /** The largest system text scale the app supports (2x). */
        const val MAX_TEXT_SCALE = 2f
    }
}
