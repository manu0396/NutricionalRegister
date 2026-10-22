package com.example.nutricionalregister.coach

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
 * The assistant opens over the screen it was asked from, and only from the
 * shell's shortcut (FR-017).
 *
 * The Coach is not a destination: it is shown over the screen the user is on
 * and the screen underneath is where the user returns to when it closes. The
 * Today screen and the More screen offer no coach of their own, because two
 * entry points for one thing read as two different things.
 */
@RunWith(AndroidJUnit4::class)
class CoachDialogTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun theShellShortcutOpensTheCoachOverTheScreenItIsOn() {
        rule.onNodeWithTag("nutrilog-coach-fab").performClick()

        rule.onNodeWithTag("coach-dialog").assertIsDisplayed()
    }

    @Test
    fun closingTheCoachReturnsToTheScreenItWasOpenedOver() {
        rule.onNodeWithTag("nutrilog-coach-fab").performClick()
        rule.onNodeWithTag("coach-dialog").assertIsDisplayed()

        rule.onNodeWithText("Allow").performClick()
        rule.onNodeWithTag("coach-back").performClick()

        // Today's own content is underneath again, with the week shortcut.
        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()
    }

    @Test
    fun neitherTheTodayScreenNorTheMoreScreenOffersACoachOfItsOwn() {
        rule.onNodeWithText("Log what you ate by telling the coach about it").assertDoesNotExist()
        rule.onNodeWithText("more-coach-row").assertDoesNotExist()
    }
}
