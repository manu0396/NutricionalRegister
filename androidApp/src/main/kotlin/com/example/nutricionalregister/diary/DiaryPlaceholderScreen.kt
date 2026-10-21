@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.nutricionalregister.ui.NutriLogScaffold
import com.example.nutricionalregister.ui.theme.NutriLogSpacing

/**
 * The Diary destination's placeholder (FR-006).
 *
 * The real diary is a later feature; this destination must never render a blank
 * screen, so it shows a polished, intentional placeholder with an icon and
 * helpful guidance.
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
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier
                .padding(contentPadding)
                .padding(NutriLogSpacing.large),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(NutriLogSpacing.medium),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Outlined.DateRange,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp),
            )
            Spacer(modifier = Modifier.height(NutriLogSpacing.small))
            Text(
                text = "Coming soon",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "The diary is not available yet. It will let you record what you eat each day and track your nutrition goals.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("diary-placeholder-message"),
            )
        }
    }
}
