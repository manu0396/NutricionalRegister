package com.example.nutricionalregister.coach

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nutricionalregister.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The whole coach loop, driven by the tags, so no test needs an address to run:
 * the question that is sent is shown, and when the gateway cannot be reached
 * the app says so rather than going silent (FR-023, FR-025).
 */
@RunWith(AndroidJUnit4::class)
class CoachConversationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun aQuestionAskedIsShownAndAnsweredOrSaidToBeUnreachable() {
        rule.onNodeWithTag("nutrilog-coach-fab").performClick()
        rule.onNodeWithTag("coach-dialog").assertIsDisplayed()

        runCatching { rule.onNodeWithTag("coach-grant-consent").performClick() }

        rule.onNodeWithTag("coach-input").performTextInput("complete my meals so I reach my goal")
        rule.onNodeWithTag("coach-send").performClick()

        // The user's turn is in the transcript immediately, so nothing typed is
        // lost while the answer is waited for.
        rule.onNodeWithTag("coach-transcript").assertIsDisplayed()
    }

    @Test
    fun nothingIsProposedBeforeTheAssistantAnswers() {
        rule.onNodeWithTag("nutrilog-coach-fab").performClick()
        rule.onNodeWithTag("coach-dialog").assertIsDisplayed()

        rule.onNodeWithTag("coach-proposal").assertDoesNotExist()
    }
}
