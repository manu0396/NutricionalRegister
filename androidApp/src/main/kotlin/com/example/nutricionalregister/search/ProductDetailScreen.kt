@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.ui.InfoChip
import com.example.nutricionalregister.ui.SectionHeader
import com.example.nutricionalregister.ui.StatRow
import com.example.nutricionalregister.ui.StatRowDivider
import com.example.nutricionalregister.ui.theme.NutriLogSpacing
import kotlin.math.roundToLong

private const val UNAVAILABLE = "Unavailable"
private const val BACK_DESCRIPTION = "Back to search"

/**
 * Full product detail view.
 *
 * Owns its own back control and its own system-bar insets so it is self-contained:
 * it renders identically whether hosted inside the app shell or, as in the
 * instrumented tests, directly under a bare `MaterialTheme {}`. The top bar
 * consumes [WindowInsets.statusBars] so the "Product details" title clears the
 * status bar in the running app; in the test host those insets are zero and the
 * layout is unaffected.
 *
 * Structure: a back bar, an image hero, a title block (name, brand/quantity, a
 * labelled Nutri-Score chip), a "Product info" card and a "Nutrition information"
 * card. Dividers appear only *between* rows so no rule ever sits between a heading
 * and its first row.
 */
@Composable
public fun ProductDetailScreen(
    product: Product,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        DetailBackBar(
            onBack = onBack,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = NutriLogSpacing.large,
                        end = NutriLogSpacing.large,
                        bottom = NutriLogSpacing.extraLarge,
                    ),
        ) {
            ProductImage(
                url = product.imageUrl ?: product.imageThumbnailUrl,
                contentDescription = "Image of ${product.name}",
                initial = initialFor(product.name),
                seed = product.code,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraLarge),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DetailTitleBlock(product = product)

                ProductInfoCard(product = product)

                NutritionCard(product = product)

                OpenFoodFactsAttribution()
            }
        }
    }
}

/**
 * Title block: product name, its brand/quantity line, and a labelled Nutri-Score
 * chip placed with the title so the badge has context instead of floating on its
 * own. The chip is omitted when the catalogue supplies no recognised grade.
 */
@Composable
private fun DetailTitleBlock(
    product: Product,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.small),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = product.name,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        val secondary = listOfNotNull(product.brands, product.quantity).joinToString(" · ")
        if (secondary.isNotBlank()) {
            Text(
                text = secondary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (hasNutriScore(product.nutriScoreGrade)) {
            InfoChip(label = "Nutri-Score") {
                NutriScoreBadge(grade = product.nutriScoreGrade)
            }
        }
    }
}

/**
 * Compact back affordance that mirrors the app shell's bar.
 *
 * A plain [Row] rather than a Material3 TopAppBar keeps the control self-contained
 * so the screen renders correctly with or without an enclosing Scaffold. It matches
 * the search screen's bar in title type scale, minimum height and horizontal
 * padding so the two top bars read as one system across screens. The back button
 * keeps a 48 dp minimum touch target and the "Back to search" description.
 */
@Composable
private fun DetailBackBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraSmall),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .heightIn(min = 64.dp)
                .padding(horizontal = NutriLogSpacing.small),
    ) {
        IconButton(
            onClick = onBack,
            modifier =
                Modifier
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .semantics { contentDescription = BACK_DESCRIPTION },
        ) {
            Text(
                text = "←",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "Product details",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * "Product info" card: code and the declared nutrition basis.
 *
 * Brand and quantity are intentionally omitted here because they already render in
 * the title block directly above; repeating them ~40 dp apart read as duplication.
 * Values are laid out as labelled [StatRow]s separated by dividers *between* rows.
 * The first row sits directly under the heading with no divider, and the card's
 * rounded surface closes the list, so the pattern is symmetric.
 */
@Composable
private fun ProductInfoCard(
    product: Product,
    modifier: Modifier = Modifier,
) {
    val rows =
        buildList {
            add("Product code" to product.code)
            product.nutritionDataPer?.let { basis -> add("Nutrition basis" to basis) }
        }

    DetailCard(title = "Product info", modifier = modifier) {
        rows.forEachIndexed { index, (label, value) ->
            if (index > 0) {
                StatRowDivider()
            }
            StatRow(label = label, value = value)
        }
    }
}

/**
 * "Nutrition information" card: a per-basis header, then one row per known
 * nutrient with a leading colour dot; nutrients the catalogue omits read
 * [UNAVAILABLE]. When no nutrient is known at all the list is replaced by a single
 * clear empty state instead of eight empty rows.
 */
@Composable
private fun NutritionCard(
    product: Product,
    modifier: Modifier = Modifier,
) {
    DetailCard(title = "Nutrition information", modifier = modifier) {
        if (product.nutritionValues.isEmpty()) {
            NutritionEmptyState()
        } else {
            val basisLabel = product.nutritionValues.basisLabel()
            if (basisLabel != null) {
                Text(
                    text = basisLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(bottom = NutriLogSpacing.small),
                )
            }
            val rows =
                NUTRIENT_LABELS.mapNotNull { (key, label) ->
                    val value = product.nutritionValues.firstOrNull { it.nutrientKey == key }
                    label to value
                }
            rows.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    StatRowDivider()
                }
                StatRow(
                    label = label,
                    value = value?.toDisplayString() ?: UNAVAILABLE,
                    leadingDot = nutritionDotColor(value),
                    valueColor =
                        if (value == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
            }
        }
    }
}

/**
 * Shared card chrome for the detail sections: a tonal surface, consistent inner
 * padding, and a [SectionHeader] title so both cards read as one system.
 */
@Composable
private fun DetailCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier = Modifier.padding(NutriLogSpacing.large),
        ) {
            SectionHeader(title = title)
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                content()
            }
        }
    }
}

/**
 * Shown when a product supplies no nutrition values at all.
 *
 * Individual missing nutrients keep rendering as [UNAVAILABLE] rows when the
 * product has at least one value; this replaces the whole list only for the
 * all-empty case, so the user sees one clear message instead of eight.
 *
 * A short leading accent rule ties the message to the card without the lone
 * glyph tile that previously read as a stray typo.
 */
@Composable
private fun NutritionEmptyState(modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = NutriLogSpacing.medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = 3.dp, height = 32.dp)
                    .background(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = MaterialTheme.shapes.small,
                    ),
        )
        Text(
            text = "Nutrition information not available for this product",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Renders a nutrient value without floating-point noise or a repeated basis.
 *
 * The basis is shown once as the card's [basisLabel] header, so each row carries
 * only the amount and unit; a missing basis falls back to "basis unavailable" on
 * the row because it cannot be factored out.
 */
private fun NutritionValue.toDisplayString(): String =
    buildString {
        append(formatAmount(amount))
        unit?.let { unit ->
            append(' ')
            append(unit)
        }
        if (basis == NutritionBasis.UNKNOWN) {
            append(" — basis unavailable")
        }
    }

/**
 * The single basis shared by every nutrient value, or null when mixed/unavailable.
 *
 * The nutrition card prints this once instead of repeating "— per 100 g" on each
 * row. Mixed bases (or an unknown one) return null so the caller shows no header
 * and rows carry their own note when needed.
 */
private fun List<NutritionValue>.basisLabel(): String? {
    val bases = map(NutritionValue::basis).distinct()
    return when (bases.singleOrNull()) {
        NutritionBasis.PER_100_G -> "Values per 100 g"
        NutritionBasis.PER_SERVING -> "Values per serving"
        else -> null
    }
}

/** Leading-dot colour for a nutrient row: muted grey when the value is missing. */
@Composable
private fun nutritionDotColor(value: NutritionValue?): Color =
    if (value == null) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.secondary
    }

/**
 * Formats a nutrient amount without floating-point noise.
 *
 * Catalogue values are doubles such as `7.6899999999999995`; they are rounded to
 * at most two decimals, whole numbers lose their decimal point, and a trailing
 * `.0`/`.x0` is dropped so values read as a person would write them.
 */
internal fun formatAmount(amount: Double): String {
    val rounded = (amount * 100).roundToLong() / 100.0
    return if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        rounded.toString()
    }
}

private val NUTRIENT_LABELS =
    listOf(
        "energy" to "Energy",
        "protein" to "Protein",
        "carbohydrate" to "Carbohydrate",
        "fat" to "Fat",
        "fiber" to "Fiber",
        "sugars" to "Sugars",
        "salt" to "Salt",
        "sodium" to "Sodium",
    )
