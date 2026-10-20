@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * Attribution for catalogue data and images.
 *
 * ADR-0009 requires the Open Food Facts credit to cover both the product data
 * (ODbL/DbCL) and the product images (CC BY-SA) and to remain a 48 dp minimum
 * touch target that opens the catalogue. The label is kept stable for tests.
 *
 * Presented as a muted footer with an explicit "Open catalogue" link line so the
 * credit reads as an intentional part of the screen rather than stray body text.
 */
@Composable
public fun OpenFoodFactsAttribution(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Product data and images from Open Food Facts",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = NutriLogSpacing.small),
        )
        Text(
            text = "Open catalogue →",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .padding(vertical = NutriLogSpacing.medium)
                    .clickable { uriHandler.openUri(OPEN_FOOD_FACTS_URL) }
                    .semantics {
                        contentDescription = "Open Food Facts attribution and catalogue link"
                    },
        )
    }
}

private const val OPEN_FOOD_FACTS_URL = "https://world.openfoodfacts.org"
