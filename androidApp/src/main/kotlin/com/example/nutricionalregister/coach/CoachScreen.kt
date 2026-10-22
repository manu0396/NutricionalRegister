@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.coach

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.nutricionalregister.domain.assistant.AssistantAction
import com.example.nutricionalregister.domain.assistant.AssistantMessage
import com.example.nutricionalregister.domain.assistant.AssistantReply
import com.example.nutricionalregister.feature.assistant.AssistantAvailability
import com.example.nutricionalregister.feature.assistant.AssistantState
import com.example.nutricionalregister.ui.MinimumTouchTarget
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The Coach: the assistant screen and nothing else (FR-017, FR-020, FR-021).
 *
 * The screen is a stateless composable: it renders the [state] it is given and
 * reports the user's actions through its callbacks. It holds no view model,
 * reads no repository and navigates nowhere.
 *
 * The premium gate is decided here and not in the view model, because the
 * entitlement is a purchase fact and not an assistant fact: a user who has not
 * unlocked the premium product sees the upgrade panel and no message field, so
 * the app never pretends a conversation is possible (FR-017, FR-030).
 *
 * @param state the state of the assistant.
 * @param isUnlocked whether the premium product is unlocked.
 * @param onSend invoked with the message the user wrote.
 * @param onConfirmProposal invoked when the user confirms the pending proposal.
 * @param onDismissProposal invoked when the user dismisses the pending proposal.
 * @param onGrantConsent invoked when the user allows the assistant to be used.
 * @param onUpgrade invoked when the user asks for the premium unlock.
 * @param onBack invoked when the user leaves the screen.
 * @param contentPadding the padding of the enclosing shell.
 * @param modifier the modifier applied to the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun CoachScreen(
    state: AssistantState,
    isUnlocked: Boolean,
    onSend: (String) -> Unit,
    onConfirmProposal: () -> Unit,
    onDismissProposal: () -> Unit,
    onGrantConsent: () -> Unit,
    onUpgrade: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding)
                .imePadding(),
    ) {
        CoachTopBar(onBack = onBack)

        when {
            !isUnlocked ->
                LockedPanel(
                    onUpgrade = onUpgrade,
                    modifier = Modifier.weight(1f),
                )

            else ->
                when (state.availability) {
                    AssistantAvailability.NotConfigured ->
                        NoticePanel(
                            title = "The Coach is not configured",
                            body =
                                "This build has no assistant service to talk to. The publisher " +
                                    "has to deploy the AI gateway and configure its address before " +
                                    "the Coach can answer.",
                            modifier = Modifier.weight(1f),
                        )

                    AssistantAvailability.ConsentRequired ->
                        ConsentPanel(
                            onGrantConsent = onGrantConsent,
                            modifier = Modifier.weight(1f),
                        )

                    AssistantAvailability.Offline ->
                        NoticePanel(
                            title = "No connection",
                            body =
                                "The Coach needs a connection to answer. Try again once the " +
                                    "device is online.",
                            modifier = Modifier.weight(1f),
                        )

                    AssistantAvailability.Ready ->
                        Conversation(
                            state = state,
                            onSend = onSend,
                            onConfirmProposal = onConfirmProposal,
                            onDismissProposal = onDismissProposal,
                            modifier = Modifier.weight(1f),
                        )
                }
        }
    }
}

/** The top bar of the Coach, with the back control the route gives it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoachTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = { Text(text = "Coach", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
            IconButton(
                onClick = onBack,
                modifier =
                    Modifier
                        .sizeIn(minWidth = MinimumTouchTarget, minHeight = MinimumTouchTarget)
                        .testTag("coach-back"),
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        modifier = modifier,
    )
}

/** The panel a user who has not unlocked the premium product sees. */
@Composable
private fun LockedPanel(
    onUpgrade: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier =
            modifier
                .fillMaxSize()
                .padding(NutriLogSpacing.extraLarge)
                .testTag("coach-locked"),
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "The Coach is part of Premium",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = NutriLogSpacing.medium),
        )
        Text(
            text =
                "Ask about your day, log a food by conversation or change an entry, " +
                    "with the premium unlock.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = NutriLogSpacing.small),
        )
        Button(
            onClick = onUpgrade,
            modifier = Modifier.padding(top = NutriLogSpacing.extraLarge).testTag("coach-upgrade"),
        ) {
            Text(text = "See Premium in More")
        }
    }
}

/** A single panel with a title and a body, used for the states that explain themselves. */
@Composable
private fun NoticePanel(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxSize().padding(NutriLogSpacing.extraLarge),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(top = NutriLogSpacing.small),
        )
    }
}

/** The consent panel, shown before the first message is sent (FR-021). */
@Composable
private fun ConsentPanel(
    onGrantConsent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(NutriLogSpacing.extraLarge)
                .testTag("coach-consent"),
    ) {
        Text(
            text = "Before the Coach can answer",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text =
                "Your message and the minimum it needs to answer are sent to the " +
                    "app's own service: the entries of today, your daily goal and the codes " +
                    "of the foods you keep. Nothing else is sent, nothing is stored in the " +
                    "cloud by the app, and your diary never leaves this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onGrantConsent,
            modifier = Modifier.testTag("coach-grant-consent"),
        ) {
            Text(text = "Allow")
        }
    }
}

/** The conversation: the transcript, the pending proposal and the composer. */
@Composable
private fun Conversation(
    state: AssistantState,
    onSend: (String) -> Unit,
    onConfirmProposal: () -> Unit,
    onDismissProposal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.pendingProposal) {
        val last = state.messages.size - 1
        if (last >= 0) listState.animateScrollToItem(last)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier =
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = NutriLogSpacing.large)
                    .testTag("coach-transcript"),
        ) {
            items(state.messages, key = { it.text + it.role.name }) { message ->
                MessageBubble(message = message)
            }

            state.pendingProposal
                ?.takeIf { proposal -> proposal.actions.isNotEmpty() }
                ?.let { proposal ->
                    item(key = "proposal") {
                        ProposalCard(
                            reply = proposal,
                            onConfirm = onConfirmProposal,
                            onDismiss = onDismissProposal,
                        )
                    }
                }

            if (state.isSending) {
                item(key = "sending") {
                    Text(
                        text = "Thinking",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("coach-sending"),
                    )
                }
            }

            state.error?.let { error ->
                item(key = "error") {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.testTag("coach-error"),
                    )
                }
            }
        }

        Composer(
            isSending = state.isSending,
            onSend = onSend,
        )
    }
}

/** One turn of the conversation, aligned by who wrote it. */
@Composable
private fun MessageBubble(
    message: AssistantMessage,
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == com.example.nutricionalregister.domain.assistant.AssistantRole.USER
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        Card(
            shape = RoundedCornerShape(NutriLogSpacing.large),
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        if (isUser) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    contentColor =
                        if (isUser) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                ),
            modifier =
                Modifier
                    .align(if (isUser) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxWidth(0.85f),
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(NutriLogSpacing.medium),
            )
        }
    }
}

/** The change the assistant proposes, applied only when the user confirms it. */
@Composable
private fun ProposalCard(
    reply: AssistantReply,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = modifier.fillMaxWidth().testTag("coach-proposal"),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier = Modifier.padding(NutriLogSpacing.large),
        ) {
            Text(
                text = reply.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = "The Coach proposes",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            reply.actions.forEach { action ->
                Text(
                    text = action.describe(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            ) {
                Button(onClick = onConfirm, modifier = Modifier.testTag("coach-confirm-proposal")) {
                    Icon(imageVector = Icons.Filled.Check, contentDescription = null)
                    Text(text = "Apply", modifier = Modifier.padding(start = NutriLogSpacing.extraSmall))
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.testTag("coach-dismiss-proposal")) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = null)
                    Text(text = "Dismiss", modifier = Modifier.padding(start = NutriLogSpacing.extraSmall))
                }
            }
        }
    }
}

/** The message field and the send control. */
@Composable
private fun Composer(
    isSending: Boolean,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf("") }

    fun submit() {
        val text = draft.trim()
        if (text.isEmpty() || isSending) return
        onSend(text)
        draft = ""
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
        modifier =
            modifier
                .fillMaxWidth()
                .padding(NutriLogSpacing.large),
    ) {
        OutlinedTextField(
            value = draft,
            onValueChange = { value -> draft = value },
            placeholder = { Text(text = "Ask about your day") },
            singleLine = false,
            maxLines = 4,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.weight(1f).testTag("coach-input"),
        )
        IconButton(
            onClick = { submit() },
            enabled = !isSending && draft.isNotBlank(),
            modifier =
                Modifier
                    .sizeIn(minWidth = MinimumTouchTarget, minHeight = MinimumTouchTarget)
                    .testTag("coach-send"),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send message",
            )
        }
    }
}

/** What the proposed change would do, in the words of the diary. */
private fun AssistantAction.describe(): String =
    when (this) {
        is AssistantAction.LogFood ->
            "Log ${quantityGrams.toPlainString()} g into ${mealType.displayName}."

        is AssistantAction.RemoveEntry -> "Remove entry $entryId."

        is AssistantAction.ChangeEntry -> {
            val parts =
                buildList {
                    quantityGrams?.let { add("the amount to ${it.toPlainString()} g") }
                    mealType?.let { add("the meal to ${it.displayName}") }
                }
            "Change entry $entryId: ${parts.joinToString(" and ")}."
        }
    }

/** The amount without floating-point noise, so a proposal reads as a person would write it. */
private fun Double.toPlainString(): String {
    val rounded = kotlin.math.round(this * 10.0) / 10.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
}
