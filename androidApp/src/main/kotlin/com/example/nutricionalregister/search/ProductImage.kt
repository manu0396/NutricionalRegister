@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter

/**
 * Renders a product image over a deterministic generated placeholder.
 *
 * The placeholder is always drawn first, so text and layout never wait on the
 * network. When [url] is blank the placeholder is the whole surface; when a URL
 * is supplied it paints over the placeholder unless the download fails
 * ([AsyncImagePainter.State.Error]), in which case the placeholder remains.
 *
 * The placeholder is designed to look intentional rather than broken: a pleasant
 * two-stop gradient tile whose hues are picked deterministically from [seed] (or
 * [initial]), with a large centred initial glyph. The same product therefore
 * always receives the same tile, across the card thumbnail and the detail header.
 *
 * @param url remote image URL for this product, or null/blank when unknown.
 * @param contentDescription accessibility label, or null to treat as decorative
 *   when an enclosing card already describes the product.
 * @param initial the product's leading glyph, shown centred on the placeholder.
 * @param seed stable identity used to derive the placeholder colour; defaults to
 *   [initial] so existing call sites keep a deterministic tile.
 */
@Composable
public fun ProductImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    initial: Char? = null,
    seed: String? = null,
) {
    val shape = MaterialTheme.shapes.medium
    val hasUrl = !url.isNullOrBlank()
    var failed by remember(url) { mutableStateOf(false) }
    val semanticsModifier =
        if (contentDescription == null) {
            Modifier
        } else {
            Modifier.semantics { this.contentDescription = contentDescription }
        }
    val palette = PlaceholderPalette[placeholderIndex(seed ?: initial?.toString().orEmpty())]
    val placeholderBrush = Brush.linearGradient(listOf(palette.start, palette.end))

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .clip(shape)
                .background(placeholderBrush)
                .then(semanticsModifier),
    ) {
        PlaceholderGlyph(initial = initial, onColor = palette.onColor)
        if (hasUrl && !failed) {
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onState = { state ->
                    if (state is AsyncImagePainter.State.Error) {
                        failed = true
                    }
                },
            )
        }
    }
}

/** Generated placeholder shown before an image loads and whenever it cannot be shown. */
@Composable
private fun PlaceholderGlyph(
    initial: Char?,
    onColor: Color,
) {
    val glyph = initial?.takeIf { !it.isWhitespace() }?.uppercaseChar()?.toString() ?: "?"
    Text(
        text = glyph,
        color = onColor,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.displaySmall,
    )
}

/**
 * Stable, non-negative palette index derived from a product identity.
 *
 * Uses a small FNV-1a-style fold so identical seeds always map to the same slot
 * and different products spread across the palette.
 */
internal fun placeholderIndex(seed: String): Int {
    var hash = -0x7EE3623B // 0x811C9DC5 as a signed Int
    seed.forEach { char ->
        hash = hash xor char.code
        hash *= 0x01000193
    }
    return (hash and 0x7FFFFFFF) % PlaceholderPalette.size
}

private data class PlaceholderColors(
    val start: Color,
    val end: Color,
    val onColor: Color,
)

/**
 * Tasteful placeholder gradients in the brand's spirit (green, amber, teal plus
 * complementary tones). Each entry keeps a light-to-deep gradient with dark text
 * so the centred glyph stays legible against every tile.
 */
private val PlaceholderPalette: List<PlaceholderColors> =
    listOf(
        PlaceholderColors(Color(0xFF8AF9B6), Color(0xFF3FA76B), Color(0xFF06301A)),
        PlaceholderColors(Color(0xFFFFDDB3), Color(0xFFE0913A), Color(0xFF3A2500)),
        PlaceholderColors(Color(0xFFA3EEFC), Color(0xFF2E9FB5), Color(0xFF03303A)),
        PlaceholderColors(Color(0xFFFFC9C9), Color(0xFFD76B6B), Color(0xFF3D0F0F)),
        PlaceholderColors(Color(0xFFD7CCFF), Color(0xFF8A72D6), Color(0xFF221541)),
        PlaceholderColors(Color(0xFFC9E7A8), Color(0xFF7EAF4B), Color(0xFF23340E)),
        PlaceholderColors(Color(0xFFFFE0F0), Color(0xFFD973A8), Color(0xFF3F0F27)),
        PlaceholderColors(Color(0xFFB9E3D6), Color(0xFF4F9E86), Color(0xFF073026)),
    )

internal fun initialFor(name: String): Char? = name.firstOrNull { it.isLetterOrDigit() }
