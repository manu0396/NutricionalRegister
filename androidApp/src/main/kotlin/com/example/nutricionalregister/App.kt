@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package com.example.nutricionalregister

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.nutricionalregister.coach.CoachScreen
import com.example.nutricionalregister.data.AndroidPurchaseClient
import com.example.nutricionalregister.diary.AddFoodScreen
import com.example.nutricionalregister.diary.TodayScreen
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.navigation.NutriLogDestination
import com.example.nutricionalregister.feature.diary.AddFoodSource
import com.example.nutricionalregister.feature.scan.BarcodeResolution
import com.example.nutricionalregister.library.LibraryScreen
import com.example.nutricionalregister.more.MoreScreen
import com.example.nutricionalregister.more.PremiumPurchaseSection
import com.example.nutricionalregister.search.ProductDetailScreen
import com.example.nutricionalregister.summary.WeeklyScreen
import com.example.nutricionalregister.ui.AddFoodDestination
import com.example.nutricionalregister.ui.NutriLogNavigator
import com.example.nutricionalregister.ui.theme.NutriLogTheme

/**
 * The app: the shell, the three destinations and the flow that connects them.
 *
 * The composition root lives in [AppDependencies]; this function only wires the
 * shell's slots to the view models and turns the user's actions into
 * navigation. Every screen is a stateless composable, so this file is the only
 * place where the app's behaviour is assembled (ADR-0015, ADR-0019).
 */
@Composable
public fun App() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val purchaseClient = remember(context) { AndroidPurchaseClient(context, context::findActivity) }
    val aiGatewayBaseUrl = stringResource(R.string.ai_gateway_base_url)
    val dependencies =
        remember(context, scope, purchaseClient, aiGatewayBaseUrl) {
            AppDependencies(
                context = context,
                scope = scope,
                purchaseClient = purchaseClient,
                aiGatewayBaseUrl = aiGatewayBaseUrl,
            )
        }
    DisposableEffect(dependencies) {
        onDispose { dependencies.close() }
    }

    var showPrivacyPolicy by remember { mutableStateOf(false) }

    NutriLogTheme {
        AppShell(
            todayContent = { padding, navigator -> TodayRoute(dependencies, padding, navigator) },
            libraryContent = { padding, navigator -> LibraryRoute(dependencies, padding, navigator) },
            moreContent = { padding, navigator ->
                MoreRoute(
                    dependencies = dependencies,
                    contentPadding = padding,
                    navigator = navigator,
                    onOpenPrivacyPolicy = { showPrivacyPolicy = true },
                )
            },
            addFoodContent = { padding, destination, navigator ->
                AddFoodRoute(dependencies, destination, padding, navigator)
            },
            productContent = { padding, code, navigator -> ProductRoute(dependencies, code, padding, navigator) },
            coachContent = { padding, navigator -> CoachRoute(dependencies, padding, navigator) },
            weeklyContent = { padding, navigator -> WeeklyRoute(dependencies, padding, navigator) },
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            title = { Text(text = "Privacy") },
            text = {
                Text(
                    text =
                        "Your diary, your goals and your library stay on this device. " +
                            "Only the assistant sends anything anywhere, and only the " +
                            "minimum it needs: the entries of the day, your goal and the " +
                            "codes of your library. The assistant runs on a first-party " +
                            "service, never on the device.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) { Text(text = "Close") }
            },
        )
    }
}

/** The diary home: the day, the goal and the meals. */
@Composable
private fun TodayRoute(
    dependencies: AppDependencies,
    contentPadding: PaddingValues,
    navigator: NutriLogNavigator,
) {
    val state by dependencies.diaryViewModel.state.collectAsState()
    val weeklySummary by dependencies.summaryViewModel.weeklySummary.collectAsState()
    TodayScreen(
        state = state,
        weeklySummary = weeklySummary,
        onAddFood = { meal -> navigator.openAddFood(meal = meal.name) },
        onOpenProduct = { code -> navigator.openProduct(code) },
        onRemoveEntry = { id -> dependencies.diaryViewModel.removeEntry(id) },
        onOpenWeekly = navigator::openWeekly,
        onOpenCoach = navigator::openCoach,
        contentPadding = contentPadding,
    )
}

/** The personal food library. */
@Composable
private fun LibraryRoute(
    dependencies: AppDependencies,
    contentPadding: PaddingValues,
    navigator: NutriLogNavigator,
) {
    val state by dependencies.libraryViewModel.state.collectAsState()
    LibraryScreen(
        foods = state.saved,
        onOpenProduct = { code -> navigator.openProduct(code) },
        onAddFood = { food -> navigator.openAddFood(productCode = food.code) },
        onRemove = { code -> dependencies.libraryViewModel.remove(code) },
        onFindFoods = { navigator.openAddFood() },
        contentPadding = contentPadding,
    )
}

/** Goals, the assistant, the premium unlock and the privacy note. */
@Composable
private fun MoreRoute(
    dependencies: AppDependencies,
    contentPadding: PaddingValues,
    navigator: NutriLogNavigator,
    onOpenPrivacyPolicy: () -> Unit,
) {
    val diaryState by dependencies.diaryViewModel.state.collectAsState()
    val purchaseState by dependencies.premiumPurchaseViewModel.state.collectAsState()
    MoreScreen(
        goal = diaryState.goal,
        onGoalChanged = { goal -> dependencies.diaryViewModel.updateGoal(goal) },
        onOpenCoach = navigator::openCoach,
        purchaseContent = {
            PremiumPurchaseSection(
                state = purchaseState,
                onBuy = { dependencies.premiumPurchaseViewModel.startPurchase() },
                onRetry = { dependencies.premiumPurchaseViewModel.refresh() },
                onRestore = { dependencies.premiumPurchaseViewModel.restore() },
            )
        },
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        contentPadding = contentPadding,
    )
}

/** The two steps of the add-food flow. */
@Composable
private fun AddFoodRoute(
    dependencies: AppDependencies,
    destination: AddFoodDestination,
    contentPadding: PaddingValues,
    navigator: NutriLogNavigator,
) {
    val state by dependencies.addFoodViewModel.state.collectAsState()
    val searchState by dependencies.searchViewModel.state.collectAsState()
    val libraryState by dependencies.libraryViewModel.state.collectAsState()

    LaunchedEffect(destination.sessionId) {
        dependencies.searchViewModel.onQueryChanged("")
        dependencies.addFoodViewModel.start(
            sessionId = destination.sessionId,
            meal = destination.meal?.let { value -> runCatching { MealType.fromStorage(value) }.getOrNull() },
            productCode = destination.productCode,
        )
    }

    AddFoodScreen(
        state = state,
        step = destination.step,
        results = searchState.products,
        library = libraryState.saved,
        onSourceSelected = { source ->
            dependencies.addFoodViewModel.selectSource(source)
            if (source == AddFoodSource.SEARCH) {
                dependencies.searchViewModel.submitSearch()
            }
        },
        onQueryChanged = { query ->
            dependencies.addFoodViewModel.setQuery(query)
            dependencies.searchViewModel.onQueryChanged(query)
        },
        onSearch = { dependencies.searchViewModel.submitSearch() },
        onProductSelected = { product ->
            dependencies.addFoodViewModel.selectProduct(product)
            navigator.openAddFoodQuantity()
        },
        onBarcodeScanned = { code ->
            dependencies.resolveBarcode(code) { resolution -> onBarcodeResolved(resolution, dependencies, navigator) }
        },
        onQuantityChanged = { grams -> dependencies.addFoodViewModel.setQuantity(grams) },
        onMealSheetOpened = { dependencies.addFoodViewModel.openMealSheet() },
        onMealSheetDismissed = { dependencies.addFoodViewModel.closeMealSheet() },
        onMealSelected = { meal -> dependencies.addFoodViewModel.selectMeal(meal) },
        onConfirm = {
            dependencies.addFoodViewModel.confirm()
            navigator.back()
        },
        onBack = { navigator.back() },
        contentPadding = contentPadding,
    )
}

/**
 * What the app does with a scanned code.
 *
 * A code the catalogue knows opens the quantity step with that food. A code the
 * offline catalogue does not know falls back to a search by the code, so the
 * user is never left with a dead end and the app never invents a food.
 */
private fun onBarcodeResolved(
    resolution: BarcodeResolution,
    dependencies: AppDependencies,
    navigator: NutriLogNavigator,
) {
    when (resolution) {
        is BarcodeResolution.Resolved -> {
            dependencies.addFoodViewModel.selectProduct(resolution.product)
            navigator.openAddFoodQuantity()
        }
        is BarcodeResolution.NotInCache -> {
            dependencies.addFoodViewModel.selectSource(AddFoodSource.SEARCH)
            dependencies.addFoodViewModel.setQuery(resolution.code)
            dependencies.searchViewModel.onQueryChanged(resolution.code)
            dependencies.searchViewModel.submitSearch()
        }
        is BarcodeResolution.Unknown -> Unit
    }
}

/** The detail of one catalogue product. */
@Composable
private fun ProductRoute(
    dependencies: AppDependencies,
    code: String,
    contentPadding: PaddingValues,
    navigator: NutriLogNavigator,
) {
    val scope = rememberCoroutineScope()
    val viewModel = remember(code, dependencies) { dependencies.productDetailViewModel(code, scope) }
    val state by viewModel.state.collectAsState()
    val libraryState by dependencies.libraryViewModel.state.collectAsState()
    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
    }
    ProductDetailScreen(
        state = state,
        onBack = { navigator.back() },
        onAddToDiary = { product -> navigator.openAddFood(productCode = product.code) },
        onToggleSaved = { product ->
            if (product.code in libraryState.savedCodes) {
                dependencies.libraryViewModel.remove(product.code)
            } else {
                dependencies.libraryViewModel.save(product)
            }
        },
        isSaved = code in libraryState.savedCodes,
        contentPadding = contentPadding,
    )
}

/** The conversational assistant. */
@Composable
private fun CoachRoute(
    dependencies: AppDependencies,
    contentPadding: PaddingValues,
    navigator: NutriLogNavigator,
) {
    val state by dependencies.coachViewModel.state.collectAsState()
    val purchaseState by dependencies.premiumPurchaseViewModel.state.collectAsState()
    val context = LocalContext.current
    val isDebugBuild =
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    CoachScreen(
        state = state,
        isUnlocked = purchaseState.isUnlocked || isDebugBuild,
        onSend = { text -> dependencies.coachViewModel.send(text) },
        onConfirmProposal = { dependencies.coachViewModel.confirmProposal() },
        onDismissProposal = { dependencies.coachViewModel.dismissProposal() },
        onGrantConsent = { dependencies.coachViewModel.grantConsent() },
        onUpgrade = { navigator.switchTo(NutriLogDestination.MORE) },
        onBack = { navigator.back() },
        contentPadding = contentPadding,
    )
}

/** The seven-day summary. */
@Composable
private fun WeeklyRoute(
    dependencies: AppDependencies,
    contentPadding: PaddingValues,
    navigator: NutriLogNavigator,
) {
    val summary by dependencies.summaryViewModel.weeklySummary.collectAsState()
    WeeklyScreen(
        summary = summary,
        onBack = { navigator.back() },
        contentPadding = contentPadding,
    )
}

/** The activity that hosts the app, found from any context. */
private fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
