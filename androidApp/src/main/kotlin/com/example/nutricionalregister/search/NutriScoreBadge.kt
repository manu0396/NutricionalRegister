@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Nutri-Score grade badge.
 *
 * Renders a single-letter, colour-coded badge for grades a-e (case-insensitive).
 * Nothing is rendered for null, blank, or unsupported grades, so callers can pass
 * the raw catalogue value without pre-validating it.
 */
@Composable
public fun NutriScoreBadge(
    grade: String?,
    modifier: Modifier = Modifier,
) {
    val normalized = grade?.trim()?.lowercase()
    val entry = normalized?.let(NutriScorePalette::get) ?: return

    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .sizeIn(minWidth = 32.dp, minHeight = 32.dp)
                .background(entry.color, RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clearAndSetSemantics { contentDescription = "Nutri-Score ${entry.letter}" },
    ) {
        Text(
            text = entry.letter,
            color = entry.onColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
    }
}

private data class NutriScoreEntry(
    val letter: String,
    val color: Color,
    val onColor: Color,
)

private val NutriScorePalette: Map<String, NutriScoreEntry> =
    mapOf(
        "a" to NutriScoreEntry("A", Color(0xFF1E8E3E), Color.White),
        "b" to NutriScoreEntry("B", Color(0xFF6DB33F), Color.White),
        "c" to NutriScoreEntry("C", Color(0xFFF3C218), Color(0xFF1A1A00)),
        "d" to NutriScoreEntry("D", Color(0xFFEE8100), Color.White),
        "e" to NutriScoreEntry("E", Color(0xFFD93025), Color.White),
    )

/**
 * Whether [grade] maps to a badge the Nutri-Score palette can render.
 *
 * Callers use this to omit surrounding chrome (for example a labelled chip) when
 * the catalogue supplies a null, blank, or unsupported grade, so no empty label
 * is ever shown next to a missing badge.
 */
internal fun hasNutriScore(grade: String?): Boolean = NutriScorePalette.containsKey(grade?.trim()?.lowercase())
