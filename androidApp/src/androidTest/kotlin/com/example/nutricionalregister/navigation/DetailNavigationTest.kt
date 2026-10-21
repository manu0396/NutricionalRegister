package com.example.nutricionalregister.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.nutricionalregister.AppShell
import org.junit.Rule
import org.junit.Test

/**
 * T021: opening a product detail from a destination and returning lands on that
 * destination, not on Search (FR-007, navigation contract rule 4).
 */
class DetailNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun returningFromADetailOpenedInSavedLandsOnSavedNotSearch() {
        composeRule.setContent {
            MaterialTheme {
                AppShell(
                    searchContent = { Text("Search", modifier = Modifier.testTag("search-slot")) },
                    savedContent = { _, _ -> savedSlotWithDetail() },
                    diaryContent = { Text("Diary", modifier = Modifier.testTag("diary-slot")) },
                )
            }
        }

        // switch to Saved products, open a detail, then return
        composeRule.onNodeWithTag("bottom-bar-saved").performClick()
        composeRule.onNodeWithTag("saved-open-detail").performClick()
        composeRule.onNodeWithTag("saved-detail").assertIsDisplayed()

        composeRule.onNodeWithTag("saved-detail-back").performClick()

        // we are back on the Saved products destination, not Search
        composeRule.onNodeWithTag("saved-slot").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-bar-saved").assertIsSelected()
    }

    @Composable
    private fun savedSlotWithDetail() {
        var detailOpen by remember { mutableStateOf(false) }
        Column {
            if (detailOpen) {
                Text("Detail", modifier = Modifier.testTag("saved-detail"))
                Text(
                    "Back",
                    modifier = Modifier.testTag("saved-detail-back").clickable { detailOpen = false },
                )
            } else {
                Text(
                    "Open detail",
                    modifier = Modifier.testTag("saved-open-detail").clickable { detailOpen = true },
                )
            }
            Text("Saved products", modifier = Modifier.testTag("saved-slot"))
        }
    }
}
