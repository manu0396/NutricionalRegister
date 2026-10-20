@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.example.nutricionalregister.search.ProductSearchDependencies
import com.example.nutricionalregister.search.ProductSearchScreen
import com.example.nutricionalregister.ui.theme.NutriLogTheme

@Composable
@Suppress("ktlint:standard:function-naming")
public fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dependencies = remember(context, scope) { ProductSearchDependencies(context, scope) }
    DisposableEffect(dependencies) {
        onDispose(dependencies::close)
    }

    NutriLogTheme {
        ProductSearchScreen(dependencies.viewModel)
    }
}
