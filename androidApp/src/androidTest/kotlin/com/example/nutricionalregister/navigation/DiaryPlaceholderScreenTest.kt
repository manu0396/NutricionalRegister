package com.example.nutricionalregister.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.nutricionalregister.AppShell
import com.example.nutricionalregister.diary.DiaryPlaceholderScreen
import com.example.nutricionalregister.ui.NutriLogDestination
import org.junit.Rule
import org.junit.Test

/**
 * T022: the Diary destination shows an explicit "not yet available" message rather
 * than a blank screen (FR-006).
 */
class DiaryPlaceholderScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun diaryDestinationShowsAnExplicitNotYetAvailableMessage() {
        composeRule.setContent {
            MaterialTheme {
                DiaryPlaceholderScreen()
            }
        }

        composeRule.onNodeWithText("Not yet available").assertIsDisplayed()
        composeRule.onNodeWithTag("diary-placeholder-message").assertIsDisplayed()
    }

    @Test
    fun diaryIsReachableFromTheBottomBar() {
        composeRule.setContent {
            MaterialTheme {
                AppShell(
                    searchContent = { Text("Search") },
                    savedContent = { _, _ -> Text("Saved") },
                    diaryContent = { DiaryPlaceholderScreen() },
                )
            }
        }

        composeRule.onNodeWithTag("bottom-bar-${NutriLogDestination.DIARY.route}").performClick()
        composeRule.onNodeWithText("Not yet available").assertIsDisplayed()
    }
}
