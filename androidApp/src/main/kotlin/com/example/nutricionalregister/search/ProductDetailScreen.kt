@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.feature.search.ProductDetailState
import com.example.nutricionalregister.ui.InfoChip
import com.example.nutricionalregister.ui.MinimumTouchTarget
import com.example.nutricionalregister.ui.SectionHeader
import com.example.nutricionalregister.ui.StatRow
import com.example.nutricionalregister.ui.StatRowDivider
import com.example.nutricionalregister.ui.theme.NutriLogSpacing
import kotlin.math.roundToLong

private const val BACK_DESCRIPTION = "Back"

/**
 * The detail of one catalogue product (FR-024).
 *
 * The detail is a **route**: the state it renders belongs to one product code,
 * which is what removes the reported defect, because a detail that is no longer
 * on the stack cannot resurrect itself.
 *
 * The screen carries no purchase affordance (FR-032): the app sells the one
 * premium unlock and it is offered in More and nowhere else, so a food screen
 * never asks for money.
 *
 * A nutrient the product does not report is not shown at all, never shown as a
 * zero, so a food without a fat value never looks like a food without fat
 * (FR-006).
 *
 * @param state the state of the detail of one product code.
 * @param onBack invoked when the user leaves the detail.
 * @param onAddToDiary invoked with the product the user wants to log.
 * @param onToggleSaved invoked with the product the user saves or removes.
 * @param isSaved whether the product is in the user's library.
 * @param contentPadding the padding of the enclosing shell.
 * @param modifier the modifier applied to the screen.
 */
@Composable
public fun ProductDetailScreen(
    state: ProductDetailState,
    onBack: () -> Unit,
    onAddToDiary: (Product) -> Unit,
    onToggleSaved: (Product) -> Unit,
    isSaved: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    when (state) {
        is ProductDetailState.Loading ->
            DetailFrame(onBack = onBack, contentPadding = contentPadding, modifier = modifier) {
                Text(
                    text = "Looking up the food",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

        is ProductDetailState.NotFound ->
            DetailFrame(onBack = onBack, contentPadding = contentPadding, modifier = modifier) {
                Text(
                    text =
                        "This food is not in the catalogue on this device and is not in " +
                            "your library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("product-not-found"),
                )
            }

        is ProductDetailState.Ready ->
            Ready(
                product = state.product,
                onBack = onBack,
                onAddToDiary = onAddToDiary,
                onToggleSaved = onToggleSaved,
                isSaved = isSaved,
                contentPadding = contentPadding,
                modifier = modifier,
            )
    }
}

/** The body of the detail of the product that was found. */
@Composable
private fun Ready(
    product: Product,
    onBack: () -> Unit,
    onAddToDiary: (Product) -> Unit,
    onToggleSaved: (Product) -> Unit,
    isSaved: Boolean,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(contentPadding),
    ) {
        DetailBackBar(
            onBack = onBack,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = NutriLogSpacing.small),
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.extraLarge),
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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

            DetailTitleBlock(product = product)

            Button(
                onClick = { onAddToDiary(product) },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinimumTouchTarget)
                        .testTag("product-add-to-diary"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    modifier = Modifier.padding(end = NutriLogSpacing.small),
                )
                Text(text = "Add to diary")
            }

            SaveProductControl(
                productName = product.name,
                isSaved = isSaved,
                onToggleSave = { onToggleSaved(product) },
            )

            ProductInfoCard(product = product)

            NutritionCard(product = product)

            OpenFoodFactsAttribution()
        }
    }
}

/**
 * The body a detail shows while it is being read or when the food is not known.
 *
 * The frame keeps the back bar, so a detail that finds nothing is still a place
 * the user can leave.
 */
@Composable
private fun DetailFrame(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(contentPadding),
    ) {
        DetailBackBar(
            onBack = onBack,
            modifier = Modifier.fillMaxWidth().padding(horizontal = NutriLogSpacing.small),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.large),
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(NutriLogSpacing.large),
        ) {
            content()
        }
    }
}

/**
 * The save/remove control of the detail.
 *
 * The label states the current state in words rather than relying on colour or an
 * icon alone, so the state is announced correctly by accessibility services.
 */
@Composable
private fun SaveProductControl(
    productName: String,
    isSaved: Boolean,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = if (isSaved) "Remove from library" else "Save to library"
    FilledTonalButton(
        onClick = onToggleSave,
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = MinimumTouchTarget)
                .testTag("save-product-control")
                .semantics { contentDescription = "$label: $productName" },
    ) {
        Icon(
            imageVector = if (isSaved) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            modifier = Modifier.padding(end = NutriLogSpacing.small),
        )
        Text(text = label)
    }
}

/**
 * The title block: the name, the brand and pack size, and a labelled Nutri-Score
 * chip. The chip is omitted when the catalogue supplies no recognised grade.
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

/** The back control of the detail, which returns to the screen that opened it. */
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
                    .testTag("product-detail-back")
                    .semantics { contentDescription = BACK_DESCRIPTION },
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = BACK_DESCRIPTION,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = "Food",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * The product code and the declared nutrition basis, laid out as labelled rows
 * separated by dividers between the rows and never before the first one.
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
 * The nutrition the product reports: one row per nutrient the catalogue gives,
 * and nothing for the nutrients it does not. A nutrient that is not reported is
 * never shown as a zero (FR-006).
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
            val reported =
                NUTRIENT_LABELS.mapNotNull { (key, label) ->
                    product.nutritionValues.firstOrNull { it.nutrientKey == key }?.let { label to it }
                }
            reported.forEachIndexed { index, (label, value) ->
                if (index > 0) {
                    StatRowDivider()
                }
                StatRow(
                    label = label,
                    value = value.toDisplayString(),
                    leadingDot = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

/** The shared card chrome of the detail sections. */
@Composable
private fun DetailCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.large,
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
 * Shown when a product supplies no nutrition values at all, so the card says so
 * instead of rendering an empty list.
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
            text = "The catalogue does not report any nutrition for this food",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Renders a nutrient value without floating-point noise or a repeated basis.
 *
 * The basis is shown once as the card's header, so each row carries only the
 * amount and the unit; a missing basis falls back to a note on the row because
 * it cannot be factored out.
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
 * The single basis shared by every nutrient value, or null when it is mixed or
 * unavailable.
 */
private fun List<NutritionValue>.basisLabel(): String? {
    val bases = map(NutritionValue::basis).distinct()
    return when (bases.singleOrNull()) {
        NutritionBasis.PER_100_G -> "Values per 100 g"
        NutritionBasis.PER_SERVING -> "Values per serving"
        else -> null
    }
}

/**
 * Formats a nutrient amount without floating-point noise.
 *
 * Catalogue values are doubles such as `7.6899999999999995`; they are rounded to
 * at most two decimals, whole numbers lose their decimal point, and a trailing
 * `.0` is dropped so values read as a person would write them.
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
