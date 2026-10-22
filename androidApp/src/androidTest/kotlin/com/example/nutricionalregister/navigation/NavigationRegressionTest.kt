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
    fun `appOpensOnTheDiaryWithTheThreeDestinations`() {
        rule.onNodeWithText("Today").assertIsDisplayed()
        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()
    }

    @Test
    fun `switchingDestinationsAlwaysLandsOnThatDestinationsOwnScreen`() {
        rule.onNodeWithTag("bottom-bar-library").performClick()
        rule.onNodeWithText("The foods you keep, ready to log in one tap.").assertIsDisplayed()
        rule.onNodeWithTag("bottom-bar-more").performClick()
        rule.onNodeWithText("Your goals and the rest of the app.").assertIsDisplayed()
        rule.onNodeWithTag("bottom-bar-today").performClick()
        rule.onNodeWithText("The foods you keep, ready to log in one tap.").assertDoesNotExist()
    }

    @Test
    fun `theBottomBarIsVisibleOnTheThreeRootsAndHiddenInsideAChildRoute`() {
        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()
        rule.onNodeWithTag("bottom-bar-library").performClick()
        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()

        rule.onNodeWithTag("nutrilog-add-food-fab").performClick()
        rule.onNodeWithText("Add food").assertIsDisplayed()
        rule.onNodeWithTag("nutrilog-bottom-bar").assertDoesNotExist()
    }

    @Test
    fun `leavingAChildRouteReturnsToTheDestinationRootAndTheBarComesBack`() {
        rule.onNodeWithTag("nutrilog-add-food-fab").performClick()
        rule.onNodeWithText("Add food").assertIsDisplayed()
        rule.onNodeWithTag("add-food-back").performClick()

        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()
    }

    @Test
    fun `theDestinationThatIsAlreadyShowingDoesNotPushASecondEntry`() {
        repeat(2) { rule.onNodeWithTag("bottom-bar-library").performClick() }
        rule.onNodeWithText("The foods you keep, ready to log in one tap.").assertIsDisplayed()
        rule.onNodeWithTag("nutrilog-add-food-fab").assertIsDisplayed()
    }
}
