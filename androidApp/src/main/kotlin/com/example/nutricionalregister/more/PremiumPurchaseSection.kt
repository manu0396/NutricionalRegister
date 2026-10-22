@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.more

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.nutricionalregister.domain.purchase.PurchaseState
import com.example.nutricionalregister.feature.savedproducts.PremiumPurchaseState
import com.example.nutricionalregister.ui.MinimumTouchTarget
import com.example.nutricionalregister.ui.SectionHeader
import com.example.nutricionalregister.ui.components.NutriLogSurfaceCard
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The premium unlock, sold in the More destination and nowhere else
 * (ADR-0018, FR-017, FR-030).
 *
 * The app sells exactly one unlock, and it unlocks the Coach assistant. There is
 * no per-food purchase anywhere, so this surface never mentions a food product
 * (FR-032).
 *
 * The price shown is the store's own label, read verbatim and never parsed into a
 * number or into a currency. While an attempt is pending the purchase control is
 * disabled, so a second attempt cannot start (FR-018). When the store cannot be
 * reached the surface says so explicitly and offers a retry, instead of failing
 * silently (FR-019). The app never collects card details: the action only hands
 * off to the platform's own payment sheet (FR-014).
 *
 * @param state the entitlement, the store's offering and the attempt's state.
 * @param onBuy starts the purchase flow.
 * @param onRetry reloads the store's offering after a failure, so the user can try again.
 * @param onRestore reconciles outstanding purchases from the store (FR-013).
 * @param modifier the modifier applied to the section.
 */
@Composable
public fun PremiumPurchaseSection(
    state: PremiumPurchaseState,
    onBuy: () -> Unit,
    onRetry: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
        modifier = modifier.fillMaxWidth().testTag("premium-purchase-section"),
    ) {
        SectionHeader(title = "Premium")

        NutriLogSurfaceCard(modifier = Modifier.testTag("premium-purchase-card")) {
            Column(verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium)) {
                Text(
                    text = "Unlock the Coach assistant with a one-time purchase.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                PremiumAction(state = state, onBuy = onBuy)

                PremiumStatusMessage(state = state, onRetry = onRetry)

                TextButton(
                    onClick = onRestore,
                    modifier =
                        Modifier
                            .heightIn(min = MinimumTouchTarget)
                            .testTag("premium-purchase-restore"),
                ) {
                    Text(text = "Restore purchases")
                }
            }
        }
    }
}

/**
 * The single action of the section: the unlock, the purchase, or the reason
 * neither is possible.
 */
@Composable
private fun PremiumAction(
    state: PremiumPurchaseState,
    onBuy: () -> Unit,
) {
    when {
        state.isUnlocked ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Premium unlocked. The Coach assistant is available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("premium-purchase-unlocked"),
                )
            }
        state.isEligibleForPurchase ->
            UnlockButton(state = state, onBuy = onBuy)
        else ->
            Text(
                text = "The premium unlock is not available in the store right now.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("premium-purchase-not-available"),
            )
    }
}

/**
 * The purchase control.
 *
 * The label carries the store's own price label verbatim, and it stays visible
 * while an attempt is pending so the price never disappears from the surface; the
 * control itself is disabled then, so a second attempt cannot start (FR-018).
 */
@Composable
private fun UnlockButton(
    state: PremiumPurchaseState,
    onBuy: () -> Unit,
) {
    val label = "Unlock for ${state.amountLabel ?: "the store price"}"
    Button(
        onClick = onBuy,
        enabled = !state.isPending,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinimumTouchTarget)
                .testTag("premium-purchase-action")
                .semantics { contentDescription = label },
    ) {
        Icon(
            imageVector = Icons.Filled.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.padding(end = NutriLogSpacing.small),
        )
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * What happened to the attempt, told plainly.
 *
 * A failure the store cannot explain is rendered as an explicit message with a
 * retry control rather than as a silent no-op (FR-019).
 */
@Composable
private fun PremiumStatusMessage(
    state: PremiumPurchaseState,
    onRetry: () -> Unit,
) {
    when (state.state) {
        is PurchaseState.Pending ->
            Text(
                text = "Waiting for the payment sheet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("premium-purchase-pending"),
            )
        is PurchaseState.Cancelled ->
            Text(
                text = "The purchase was cancelled. No charge was made.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("premium-purchase-cancelled"),
            )
        is PurchaseState.Failed ->
            RetryMessage(
                message =
                    if (state.requiresGuidance) {
                        "Payments are unavailable on this device. Check that the store is set up, then retry."
                    } else {
                        "The purchase could not be completed. Please try again."
                    },
                onRetry = onRetry,
            )
        is PurchaseState.Unavailable ->
            RetryMessage(
                message = "Payments are unavailable right now. Check your store account, then retry.",
                onRetry = onRetry,
            )
        is PurchaseState.Purchased, PurchaseState.Idle -> Unit
    }
}

/**
 * An explicit failure message with the control that recovers from it.
 */
@Composable
private fun RetryMessage(
    message: String,
    onRetry: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.small)) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.testTag("premium-purchase-guidance"),
        )
        OutlinedButton(
            onClick = onRetry,
            modifier =
                Modifier
                    .heightIn(min = MinimumTouchTarget)
                    .testTag("premium-purchase-retry"),
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                modifier = Modifier.padding(end = NutriLogSpacing.extraSmall),
            )
            Text(text = "Retry")
        }
    }
}
