@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.savedproducts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.example.nutricionalregister.domain.purchase.PurchaseFailure
import com.example.nutricionalregister.domain.purchase.PurchaseState
import com.example.nutricionalregister.feature.savedproducts.PurchaseSheetState
import com.example.nutricionalregister.ui.MinimumTouchTarget
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The purchase surface for a product (FR-009 to FR-019).
 *
 * The purchase action is shown only for products the store offers (FR-015), and
 * displays the store's own price label verbatim. While an attempt is `Pending` the
 * control is disabled so no second purchase can start (FR-018). The unavailable /
 * not-configured states render an explicit message with a retry control instead of
 * failing silently (FR-019). The app never collects card details: the action only
 * hands off to the platform's own payment sheet (FR-014).
 *
 * @param onBuy starts the purchase flow.
 * @param onRetry resets after a transient failure so the user can try again.
 * @param onRestore reconciles outstanding purchases from the store (FR-013, T052).
 */
@Composable
public fun PurchaseSection(
    state: PurchaseSheetState,
    onBuy: () -> Unit,
    onRetry: () -> Unit,
    onRestore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
        modifier = modifier.fillMaxWidth().testTag("purchase-section"),
    ) {
        Text(
            text = "Purchase",
            style = MaterialTheme.typography.titleMedium,
        )

        if (state.isPurchased) {
            Text(
                text = "Purchased",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("purchase-purchased"),
            )
        } else if (state.isEligibleForPurchase) {
            PurchaseAction(state = state, onBuy = onBuy)
        } else {
            Text(
                text = "This product is not available for purchase.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("purchase-not-eligible"),
            )
        }

        PurchaseStatusMessage(state = state, onRetry = onRetry)

        TextButton(
            onClick = onRestore,
            modifier =
                Modifier
                    .heightIn(min = MinimumTouchTarget)
                    .testTag("restore-purchases"),
        ) {
            Text(text = "Restore purchases")
        }
    }
}

@Composable
private fun PurchaseAction(
    state: PurchaseSheetState,
    onBuy: () -> Unit,
) {
    val label = "Buy for ${state.amountLabel ?: "the store price"}"
    Button(
        onClick = onBuy,
        enabled = !state.isPending,
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = MinimumTouchTarget)
                .testTag("purchase-action")
                .semantics { contentDescription = label },
    ) {
        Text(
            text = if (state.isPending) "Processing" else label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun PurchaseStatusMessage(
    state: PurchaseSheetState,
    onRetry: () -> Unit,
) {
    when (val current = state.state) {
        is PurchaseState.Pending ->
            Text(
                text = "Waiting for the payment sheet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("purchase-pending"),
            )
        is PurchaseState.Cancelled ->
            Text(
                text = "Purchase cancelled. No charge was made.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("purchase-cancelled"),
            )
        is PurchaseState.Failed ->
            RetryMessage(
                message =
                    if (current.reason == PurchaseFailure.NOT_CONFIGURED) {
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
            modifier = Modifier.testTag("purchase-guidance"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.small)) {
            OutlinedButton(
                onClick = onRetry,
                modifier =
                    Modifier
                        .heightIn(min = MinimumTouchTarget)
                        .testTag("purchase-retry"),
            ) {
                Text(text = "Retry")
            }
        }
    }
}
