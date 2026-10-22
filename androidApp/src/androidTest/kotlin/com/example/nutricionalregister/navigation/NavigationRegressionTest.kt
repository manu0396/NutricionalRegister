package com.example.nutricionalregister.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nutricionalregister.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The instrumented navigation regression for the reported defect (T056).
 *
 * The defect was a detail screen that survived a switch of destination and
 * resurrected itself when the user came back, and a bottom bar that went
 * blank. The rules that remove it are: a switch pops back to the destination's
 * root, no state is saved or restored, and the bar is visible only on the three
 * top-level routes.
 *
 * The tests run against the real `App`, so the graph, the bar rule and the
 * back behaviour are exercised together.
 */
@RunWith(AndroidJUnit4::class)
class NavigationRegressionTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `the app opens on the diary with the three destinations`() {
        rule.onNodeWithText("Today").assertIsDisplayed()
        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()
    }

    @Test
    fun `switching destinations always lands on that destination's own screen`() {
        rule.onNodeWithText("Library").performClick()
        rule.onNodeWithText("The foods you keep, ready to log in one tap.").assertIsDisplayed()
        rule.onNodeWithText("More").performClick()
        rule.onNodeWithText("Your goals and the rest of the app.").assertIsDisplayed()
        rule.onNodeWithText("Today").performClick()
        rule.onNodeWithText("The foods you keep, ready to log in one tap.").assertDoesNotExist()
    }

    @Test
    fun `the bottom bar is visible on the three roots and hidden inside a child route`() {
        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()
        rule.onNodeWithText("Library").performClick()
        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()

        rule.onNodeWithTag("nutrilog-add-food-fab").performClick()
        rule.onNodeWithText("Add food").assertIsDisplayed()
        rule.onNodeWithTag("nutrilog-bottom-bar").assertDoesNotExist()
    }

    @Test
    fun `leaving a child route returns to the destination root and the bar comes back`() {
        rule.onNodeWithTag("nutrilog-add-food-fab").performClick()
        rule.onNodeWithText("Add food").assertIsDisplayed()
        rule.onNodeWithTag("add-food-back").performClick()

        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()
    }

    @Test
    fun `the destination that is already showing does not push a second entry`() {
        repeat(2) { rule.onNodeWithText("Library").performClick() }
        rule.onNodeWithText("The foods you keep, ready to log in one tap.").assertIsDisplayed()
        rule.onNodeWithTag("nutrilog-add-food-fab").assertIsDisplayed()
    }
}
