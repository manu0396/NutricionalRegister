@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.nutricionalregister.ui.NutriLogScaffold
import com.example.nutricionalregister.ui.SectionHeader
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The Diary destination's placeholder (FR-006).
 *
 * The real diary is a later feature; this destination must never render a blank
 * screen, so it shows an explicit "not yet available" message instead.
 */
@Composable
public fun DiaryPlaceholderScreen(modifier: Modifier = Modifier) {
    NutriLogScaffold(
        title = "Diary",
        subtitle = "Your daily food log",
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        DiaryPlaceholderContent(contentPadding = innerPadding, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun DiaryPlaceholderContent(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
        modifier =
            modifier
                .padding(contentPadding)
                .padding(
                    horizontal = NutriLogSpacing.large,
                    vertical = NutriLogSpacing.large,
                ),
    ) {
        SectionHeader(title = "Not yet available")
        Text(
            text = "The diary is not available yet. It will let you record what you eat each day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .testTag("diary-placeholder-message"),
        )
    }
}
