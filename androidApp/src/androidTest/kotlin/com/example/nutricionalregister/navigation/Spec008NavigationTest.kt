package com.example.nutricionalregister.navigation

import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToLog
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nutricionalregister.AppShell
import com.example.nutricionalregister.MainActivity
import com.example.nutricionalregister.coach.CoachDialog
import com.example.nutricionalregister.diary.AddFoodScreen
import com.example.nutricionalregister.domain.diary.MealType
import com.example.nutricionalregister.domain.diary.NutritionCalculator
import com.example.nutricionalregister.domain.diary.NutritionGoal
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.feature.assistant.AssistantState
import com.example.nutricionalregister.feature.diary.AddFoodState
import com.example.nutricionalregister.feature.search.ProductDetailState
import com.example.nutricionalregister.more.MoreScreen
import com.example.nutricionalregister.search.ProductDetailScreen
import com.example.nutricionalregister.ui.theme.NutriLogTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises spec 008's navigation contracts against the production navigation
 * graph and production flow/detail/More composables. The route slots use small
 * in-memory state holders so these tests are deterministic and never depend on
 * the remote catalogue, billing, or the assistant gateway.
 */
@RunWith(AndroidJUnit4::class)
class Spec008NavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun confirmingFromDiaryReturnsToSearchKeepsMealAndLogsExactlyOnce() {
        launchHarness()

        rule.onNodeWithTag("test-open-breakfast-flow").performClick()
        searchForOats()
        rule.onNodeWithText(TEST_PRODUCT.name).performClick()
        rule.onNodeWithTag("product-add-to-diary").performClick()
        rule.onNodeWithText("Breakfast").assertIsDisplayed()

        rule.onNodeWithTag("add-food-confirm").performScrollTo()
        rule.onNodeWithTag("add-food-confirm").assertIsEnabled()
        rule.onNodeWithTag("add-food-confirm").performClick()

        rule.onNodeWithTag("test-logged-count").assertTextEquals("1")
        waitForTag("add-food-query")
        rule.onNodeWithTag("add-food-query").assertIsDisplayed()
        rule.onNodeWithText("Breakfast").assertDoesNotExist()
        rule.onNodeWithTag("product-add-to-diary").assertDoesNotExist()
        rule.onNodeWithTag("test-logged-count").assertTextEquals("1")

        rule.onNodeWithTag("add-food-back").performClick()
        rule.onNodeWithTag("test-diary-root").assertIsDisplayed()
        rule.onNodeWithText("Logged Oats into Breakfast").assertIsDisplayed()
        rule.onNodeWithTag("test-logged-count").assertTextEquals("1")
    }

    @Test
    fun backAtDetailQuantityAndFlowSourceReturnsToThePreviousVisitedScreen() {
        launchHarness()

        rule.onNodeWithTag("test-open-breakfast-flow").performClick()
        searchForOats()
        rule.onNodeWithText(TEST_PRODUCT.name).performClick()
        rule.onNodeWithTag("product-detail-back").performClick()

        rule.onNodeWithTag("add-food-query").assertIsDisplayed()
        rule.onNodeWithTag("add-food-query").assertTextEquals("oats")
        rule.onNodeWithText(TEST_PRODUCT.name).assertIsDisplayed()

        rule.onNodeWithText(TEST_PRODUCT.name).performClick()
        rule.onNodeWithTag("product-add-to-diary").performClick()
        rule.onNodeWithTag("add-food-quantity").assertIsDisplayed()
        rule.onNodeWithTag("add-food-quick-200").performClick()
        rule.onNodeWithTag("add-food-back").performClick()

        rule.onNodeWithTag("add-food-query").assertTextEquals("oats")
        rule.onNodeWithText(TEST_PRODUCT.name).assertIsDisplayed()
        rule.onNodeWithTag("test-flow-quantity").assertTextEquals("200.0")
        rule.onNodeWithTag("add-food-back").performClick()
        rule.onNodeWithTag("test-diary-root").assertIsDisplayed()
    }

    @Test
    fun systemBackAtTheFirstFlowStepReturnsToTheScreenThatOpenedIt() {
        launchHarness()

        rule.onNodeWithTag("test-open-breakfast-flow").performClick()
        rule.onNodeWithTag("add-food-query").assertIsDisplayed()
        rule.activityRule.scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }

        rule.waitForIdle()
        rule.onNodeWithTag("test-diary-root").assertIsDisplayed()
        rule.onNodeWithTag("nutrilog-bottom-bar").assertIsDisplayed()
    }

    @Test
    fun addingFromAFlowDetailCarriesTheSameSessionAndBackReturnsDirectlyToSearch() {
        launchHarness()

        rule.onNodeWithTag("test-open-breakfast-flow").performClick()
        searchForOats()
        rule.onNodeWithTag("test-flow-session-stable").assertTextEquals("true")
        rule.onNodeWithText(TEST_PRODUCT.name).performClick()
        rule.onNodeWithTag("product-add-to-diary").performClick()

        rule.onNodeWithTag("add-food-quantity").assertIsDisplayed()
        rule.onNodeWithTag("test-flow-session-stable").assertTextEquals("true")
        rule.onNodeWithTag("add-food-back").performClick()

        rule.onNodeWithTag("add-food-query").assertIsDisplayed()
        rule.onNodeWithTag("add-food-query").assertTextEquals("oats")
        rule.onNodeWithTag("product-add-to-diary").assertDoesNotExist()
        rule.onNodeWithTag("test-flow-session-stable").assertTextEquals("true")
    }

    @Test
    fun openingFromLibraryAndConfirmingReturnsToSearchThenBackReturnsToLibrary() {
        launchHarness()

        rule.onNodeWithTag("bottom-bar-library").performClick()
        rule.onNodeWithTag("test-open-library-flow").performClick()
        rule.onNodeWithTag("add-food-query").assertIsDisplayed()
        rule.onNodeWithTag("add-food-source-library").performClick()
        rule.onNodeWithTag("add-food-source-search").performClick()
        rule.onNodeWithTag("add-food-query").performTextInput("oats")
        rule.onNodeWithText(TEST_PRODUCT.name).performClick()
        rule.onNodeWithTag("product-add-to-diary").performClick()
        rule.onNodeWithTag("add-food-open-meal-sheet").performScrollTo()
        rule.onNodeWithTag("add-food-open-meal-sheet").performClick()
        waitForTag("add-food-meal-sheet")
        waitForText("Lunch")
        rule.onNodeWithText("Lunch").assertIsDisplayed()
        rule.onNodeWithText("Lunch").performClick()
        rule.onNodeWithTag("add-food-confirm").performScrollTo()
        rule.onNodeWithTag("add-food-confirm").assertIsEnabled()
        rule.onNodeWithTag("add-food-confirm").performClick()

        rule.onNodeWithTag("test-logged-count").assertTextEquals("1")
        waitForTag("add-food-query")
        rule.onNodeWithTag("add-food-query").assertIsDisplayed()
        rule.onNodeWithTag("test-logged-count").assertTextEquals("1")
        rule.onNodeWithTag("add-food-back").performClick()
        rule.onNodeWithText("The foods you keep, ready to log in one tap.").assertIsDisplayed()
        rule.onNodeWithText("Logged Oats into Lunch").assertDoesNotExist()
    }

    @Test
    fun dismissingTheMorePrivacySurfaceReturnsToMoreWithGoalsStillVisible() {
        launchHarness()

        rule.onNodeWithTag("bottom-bar-more").performClick()
        rule.onNodeWithTag("more-goal-section").assertIsDisplayed()
        rule.onNodeWithTag("goal-field-energy").performTextClearance()
        rule.onNodeWithTag("goal-field-energy").performTextInput("2500")
        rule.onNodeWithTag("goal-field-energy").assertTextContains("2500")
        rule.onNodeWithTag("more-privacy-row").performScrollTo()
        rule.onNodeWithTag("more-privacy-row").performClick()
        rule.onNodeWithText("Privacy").assertIsDisplayed()
        rule.onNodeWithTag("test-close-privacy").performClick()

        rule.onNodeWithTag("more-goal-section").assertIsDisplayed()
        rule.onNodeWithTag("goal-field-energy").assertTextContains("2500")
        rule.onNodeWithTag("bottom-bar-more").assertIsDisplayed()
    }

    @Test
    fun tappingTheSelectedBottomTabTwiceDoesNotAddAStackEntry() {
        launchHarness()

        rule.onNodeWithTag("bottom-bar-library").performClick()
        rule.onNodeWithTag("bottom-bar-library").performClick()
        rule.onNodeWithTag("test-library-root").assertIsDisplayed()
        rule.activityRule.scenario.onActivity { activity -> activity.onBackPressedDispatcher.onBackPressed() }

        rule.waitForIdle()
        rule.onNodeWithTag("test-diary-root").assertIsDisplayed()
    }

    @Test
    fun closingCoachOverQuantityKeepsTheCurrentFlowAndItsMeal() {
        launchHarness()

        rule.onNodeWithTag("test-open-breakfast-flow").performClick()
        searchForOats()
        rule.onNodeWithText(TEST_PRODUCT.name).performClick()
        rule.onNodeWithTag("product-add-to-diary").performClick()
        rule.onNodeWithTag("test-flow-session-stable").assertTextEquals("true")
        rule.onNodeWithTag("nutrilog-coach-fab").performClick()
        rule.onNodeWithTag("coach-dialog").assertIsDisplayed()
        rule.onNodeWithTag("coach-back").performClick()

        rule.onNodeWithTag("add-food-quantity").assertIsDisplayed()
        rule.onNodeWithText("Breakfast").assertIsDisplayed()
        rule.onNodeWithTag("test-flow-session-stable").assertTextEquals("true")
    }

    @Test
    fun detailOpenedOutsideAFlowOpensANewFlowWithTheFoodSelected() {
        launchHarness()

        rule.onNodeWithTag("test-open-product-outside-flow").performClick()
        rule.onNodeWithTag("product-add-to-diary").performClick()

        rule.onNodeWithTag("add-food-quantity").assertIsDisplayed()
        rule.onNodeWithText(TEST_PRODUCT.name).assertIsDisplayed()
        rule.onNodeWithTag("test-flow-session-stable").assertIsDisplayed()
    }

    private fun launchHarness() {
        rule.activityRule.scenario.onActivity { activity ->
            val content = activity.findViewById<ViewGroup>(android.R.id.content)
            val composeView = content.getChildAt(0) as ComposeView
            composeView.setContent { spec008TestApp() }
        }
        rule.waitForIdle()
    }

    private fun searchForOats() {
        rule.onNodeWithTag("add-food-query").performTextInput("oats")
        rule.onNodeWithTag("add-food-search-submit").performClick()
        rule.onNodeWithText(TEST_PRODUCT.name).assertIsDisplayed()
    }

    private fun waitForTag(tag: String) {
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        try {
            rule.waitUntil(15_000) {
                rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            rule.onRoot(useUnmergedTree = true).printToLog("navigation-debug-$tag")
            val diagnosticTags =
                listOf(
                    "test-current-flow-step",
                    "test-logged-count",
                    "add-food-confirm",
                    "add-food-query",
                    "product-add-to-diary",
                    "test-diary-root",
                ).associateWith { candidate ->
                    rule.onAllNodesWithTag(candidate).fetchSemanticsNodes().size
                }
            throw AssertionError("Timed out waiting for $tag; current nodes: $diagnosticTags", failure)
        }
    }

    private fun waitForText(text: String) {
        rule.mainClock.advanceTimeBy(1_000)
        rule.waitForIdle()
        try {
            rule.waitUntil(15_000) {
                rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (failure: Throwable) {
            rule.onRoot(useUnmergedTree = true).printToLog("navigation-debug-$text")
            throw failure
        }
    }
}

@Composable
private fun spec008TestApp() {
    var addFoodState by remember { mutableStateOf(AddFoodState()) }
    var activeSessionId by remember { mutableStateOf<String?>(null) }
    var expectedCarriedSessionId by remember { mutableStateOf<String?>(null) }
    var showCoach by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var goal by remember { mutableStateOf(NutritionGoal()) }
    val loggedMeals = remember { mutableStateListOf<String>() }

    NutriLogTheme {
        AppShell(
            todayContent = { _, navigator ->
                Column(Modifier.fillMaxSize()) {
                    Text("Today", modifier = Modifier.testTag("test-diary-root"))
                    Text(loggedMeals.size.toString(), modifier = Modifier.testTag("test-logged-count"))
                    loggedMeals.forEach { entry -> Text("Logged $entry") }
                    Button(
                        onClick = { navigator.openAddFood(meal = MealType.BREAKFAST.name) },
                        modifier = Modifier.testTag("test-open-breakfast-flow"),
                    ) { Text("Add breakfast") }
                    Button(
                        onClick = { navigator.openProduct(TEST_PRODUCT.code) },
                        modifier = Modifier.testTag("test-open-product-outside-flow"),
                    ) { Text("Open product") }
                }
            },
            libraryContent = { _, navigator ->
                Column {
                    Text("The foods you keep, ready to log in one tap.", modifier = Modifier.testTag("test-library-root"))
                    Button(
                        onClick = { navigator.openAddFood() },
                        modifier = Modifier.testTag("test-open-library-flow"),
                    ) { Text("Find foods") }
                }
            },
            moreContent = { padding, _ ->
                MoreScreen(
                    goal = goal,
                    onGoalChanged = { goal = it },
                    purchaseContent = null,
                    onOpenPrivacyPolicy = { showPrivacy = true },
                    contentPadding = padding,
                )
            },
            addFoodContent = { padding, destination, navigator ->
                LaunchedEffect(destination.sessionId) {
                    if (activeSessionId != destination.sessionId) {
                        activeSessionId = destination.sessionId
                        val product = TEST_PRODUCT.takeIf { destination.productCode == it.code }
                        addFoodState =
                            AddFoodState(
                                mealType = destination.meal?.let(MealType::fromStorage),
                                product = product,
                                quantityGrams = if (product == null) 0.0 else 100.0,
                            )
                    }
                }
                AddFoodScreen(
                    state = addFoodState,
                    step = destination.step,
                    results = listOf(TEST_PRODUCT),
                    library = emptyList(),
                    onSourceSelected = { source -> addFoodState = addFoodState.copy(source = source) },
                    onQueryChanged = { query -> addFoodState = addFoodState.copy(query = query) },
                    onSearch = {},
                    onProductSelected = { product -> navigator.openProduct(product.code) },
                    onBarcodeScanned = {},
                    onQuantityChanged = { grams ->
                        addFoodState =
                            addFoodState.copy(
                                quantityGrams = grams,
                                preview = NutritionCalculator.contribution(TEST_PRODUCT, grams),
                            )
                    },
                    onMealSheetOpened = { addFoodState = addFoodState.copy(isMealSheetOpen = true) },
                    onMealSheetDismissed = { addFoodState = addFoodState.copy(isMealSheetOpen = false) },
                    onMealSelected = { meal -> addFoodState = addFoodState.copy(mealType = meal, isMealSheetOpen = false) },
                    onConfirm = {
                        val product = addFoodState.product
                        val meal = addFoodState.mealType
                        if (product != null && meal != null && addFoodState.canConfirm) {
                            loggedMeals.add("${product.name} into ${meal.displayName}")
                            navigator.landAfterAddFood(meal.name)
                        }
                    },
                    onBack = navigator::back,
                    contentPadding = padding,
                )
                Text(
                    text =
                        (
                            expectedCarriedSessionId == null ||
                                destination.sessionId == expectedCarriedSessionId
                        ).toString(),
                    modifier = Modifier.testTag("test-flow-session-stable"),
                )
                Text(destination.step.name, modifier = Modifier.testTag("test-current-flow-step"))
                Text(loggedMeals.size.toString(), modifier = Modifier.testTag("test-logged-count"))
                Text(addFoodState.quantityGrams.toString(), modifier = Modifier.testTag("test-flow-quantity"))
            },
            productContent = { padding, _, navigator ->
                ProductDetailScreen(
                    state = ProductDetailState.Ready(TEST_PRODUCT),
                    onBack = navigator::back,
                    onAddToDiary = { product ->
                        if (navigator.isFlowOpen()) {
                            expectedCarriedSessionId = activeSessionId
                            addFoodState =
                                addFoodState.copy(
                                    product = product,
                                    quantityGrams = 100.0,
                                    preview = NutritionCalculator.contribution(product, 100.0),
                                )
                            navigator.carryProductIntoFlow(addFoodState.mealType?.name)
                        } else {
                            navigator.openAddFood(productCode = product.code)
                        }
                    },
                    onToggleSaved = {},
                    isSaved = false,
                    contentPadding = padding,
                )
            },
            weeklyContent = { _, navigator ->
                Column {
                    Text("Weekly", modifier = Modifier.testTag("test-weekly-root"))
                    Button(onClick = navigator::back) { Text("Back") }
                }
            },
            onOpenCoach = { showCoach = true },
        )
    }

    if (showCoach) {
        CoachDialog(
            state = AssistantState(),
            isUnlocked = true,
            onSend = {},
            onConfirmProposal = {},
            onDismissProposal = {},
            onGrantConsent = {},
            onUpgrade = {},
            onDismiss = { showCoach = false },
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy") },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }, modifier = Modifier.testTag("test-close-privacy")) {
                    Text("Close")
                }
            },
        )
    }
}

private val TEST_PRODUCT = Product(code = "spec-008-oats", name = "Oats")
