package com.example.nutricionalregister.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.nutricionalregister.AppShell
import org.junit.Rule
import org.junit.Test

class BottomBarScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun barShowsExactlyTheThreeDestinationsAndIndicatesTheCurrentOne() {
        composeRule.setContent {
            MaterialTheme {
                AppShell(
                    searchContent = { Text("Search", modifier = Modifier.testTag("search-slot")) },
                    savedContent = { _, _ -> Text("Saved products", modifier = Modifier.testTag("saved-slot")) },
                    diaryContent = { Text("Diary", modifier = Modifier.testTag("diary-slot")) },
                )
            }
        }

        // the bar renders exactly the three destinations
        composeRule.onNodeWithTag("bottom-bar-search").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-bar-saved").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-bar-diary").assertIsDisplayed()

        // the start destination is the current one, the others are not
        composeRule.onNodeWithTag("bottom-bar-search").assertIsSelected()
        composeRule.onNodeWithTag("bottom-bar-saved").assertIsNotSelected()
        composeRule.onNodeWithTag("bottom-bar-diary").assertIsNotSelected()

        // selecting a destination switches to it and updates the indicator
        composeRule.onNodeWithTag("bottom-bar-saved").performClick()
        composeRule.onNodeWithTag("saved-slot").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-bar-saved").assertIsSelected()
        composeRule.onNodeWithTag("bottom-bar-search").assertIsNotSelected()

        composeRule.onNodeWithTag("bottom-bar-diary").performClick()
        composeRule.onNodeWithTag("diary-slot").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-bar-diary").assertIsSelected()
    }

    @Test
    fun reSelectingTheCurrentDestinationStaysOnItWithoutDuplicating() {
        composeRule.setContent {
            MaterialTheme {
                AppShell(
                    searchContent = { Text("Search", modifier = Modifier.testTag("search-slot")) },
                    savedContent = { _, _ -> Text("Saved products", modifier = Modifier.testTag("saved-slot")) },
                    diaryContent = { Text("Diary", modifier = Modifier.testTag("diary-slot")) },
                )
            }
        }

        // repeated selection of the current destination stays on it
        repeat(3) { composeRule.onNodeWithTag("bottom-bar-search").performClick() }
        composeRule.onNodeWithTag("search-slot").assertIsDisplayed()
        composeRule.onNodeWithTag("bottom-bar-search").assertIsSelected()
    }
}
