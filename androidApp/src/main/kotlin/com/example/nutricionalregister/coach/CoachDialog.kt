@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.coach

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nutricionalregister.feature.assistant.AssistantState
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The Coach, shown over the screen it was asked for (FR-017, FR-020, FR-021).
 *
 * The assistant is not a destination of its own: it opens as a dialog over
 * whatever screen the user is on, so a question about today's diary is asked
 * from today's diary and a question over the flow is asked over the flow, and
 * the screen underneath is exactly where the user returns to when it closes.
 *
 * The content is the same [CoachScreen] as before, and its back control closes
 * the dialog instead of leaving a route.
 *
 * @param state the state of the assistant.
 * @param isUnlocked whether the premium product is unlocked.
 * @param onSend invoked with the message the user wrote.
 * @param onConfirmProposal invoked when the user confirms the pending proposal.
 * @param onDismissProposal invoked when the user dismisses the pending proposal.
 * @param onGrantConsent invoked when the user allows the assistant to be used.
 * @param onUpgrade invoked when the user asks for the premium unlock.
 * @param onDismiss invoked when the Coach is closed.
 */
@Composable
public fun CoachDialog(
    state: AssistantState,
    isUnlocked: Boolean,
    onSend: (String) -> Unit,
    onConfirmProposal: () -> Unit,
    onDismissProposal: () -> Unit,
    onGrantConsent: () -> Unit,
    onUpgrade: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false,
            ),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            shape = MaterialTheme.shapes.extraLarge,
            modifier =
                Modifier
                    .padding(NutriLogSpacing.medium)
                    .fillMaxWidth()
                    .fillMaxHeight(0.94f)
                    .testTag("coach-dialog"),
        ) {
            CoachScreen(
                state = state,
                isUnlocked = isUnlocked,
                onSend = onSend,
                onConfirmProposal = onConfirmProposal,
                onDismissProposal = onDismissProposal,
                onGrantConsent = onGrantConsent,
                onUpgrade = onUpgrade,
                onBack = onDismiss,
                contentPadding =
                    androidx.compose.foundation.layout
                        .PaddingValues(0.dp),
            )
        }
    }
}
