# Contracts: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21

Public contracts introduced or changed by this feature. All of them live in the
shared KMP modules; the Android UI consumes them and never reimplements them.

## 1. Domain - `core:domain`, package `domain.diary`

### 1.1 `MealType`

```kotlin
public enum class MealType(
    public val displayName: String,
    public val order: Int,
) {
    BREAKFAST("Breakfast", 0),
    LUNCH("Lunch", 1),
    DINNER("Dinner", 2),
    SNACKS("Snacks", 3),
    ;

    public companion object {
        public val ordered: List<MealType>
        public fun fromStorage(value: String): MealType
    }
}
```

Contract: `fromStorage` maps a stored value back to a constant and throws
`IllegalArgumentException` for an unknown value, so a corrupted row fails loudly
rather than silently dropping an entry.

### 1.2 `DiaryEntry`

```kotlin
public data class DiaryEntry(
    val id: Long = 0,
    val productCode: String,
    val productName: String,
    val brands: String? = null,
    val imageThumbnailUrl: String? = null,
    val mealType: MealType,
    val quantityGrams: Double,
    val unit: String = "g",
    val energyKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
    val loggedAtEpochMillis: Long = 0,
    val dayEpochMillis: Long = 0,
) {
    public val totals: NutritionTotals
}
```

Validation: `productCode` and `productName` are not blank; `quantityGrams` is
finite and greater than zero; every nutrient value is either null or finite and
not negative.

The entry is a **snapshot**. The name, the brand, the image and the nutrients are
copied at log time, so a later catalogue change cannot rewrite what the user ate.

### 1.3 `NutritionTotals`

```kotlin
public data class NutritionTotals(
    val energyKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
) {
    public operator fun plus(other: NutritionTotals): NutritionTotals
    public fun scaledBy(factor: Double): NutritionTotals
    public fun remainingAgainst(goal: NutritionGoal): NutritionTotals
    public val isComplete: Boolean

    public companion object {
        public val ZERO: NutritionTotals
        public fun fromEntries(entries: List<DiaryEntry>): NutritionTotals
    }
}
```

Contract: `null` means **unknown**, not zero. `0.0` means known to be zero.
`plus` is component-wise and conservative: as soon as one side is unknown the
result is unknown. `fromEntries` of an empty list is the known zero; of a
non-empty list, a nutrient that at least one entry does not report is unknown.
`remainingAgainst` is never negative and is unknown when the total is unknown.

### 1.4 `NutritionGoal`

```kotlin
public data class NutritionGoal(
    val energyKcal: Int = DEFAULT_ENERGY_KCAL,
    val proteinGrams: Int = DEFAULT_PROTEIN_GRAMS,
    val carbohydrateGrams: Int = DEFAULT_CARBOHYDRATE_GRAMS,
    val fatGrams: Int = DEFAULT_FAT_GRAMS,
)
```

Contract: every target is greater than zero. The defaults are 2000 kcal,
120 g of protein, 220 g of carbohydrate and 70 g of fat.

### 1.5 `DiaryRepository`

```kotlin
public interface DiaryRepository {
    public fun observeEntriesForDay(dayEpochMillis: Long): Flow<List<DiaryEntry>>
    public fun observeEntriesForWeek(dayEpochMillis: Long): Flow<List<DiaryEntry>>
    public suspend fun addEntry(entry: DiaryEntry): Long
    public suspend fun updateEntry(entry: DiaryEntry)
    public suspend fun removeEntry(id: Long)
}
```

Contract: `observeEntriesForDay` emits the entries of exactly that local day,
oldest first, and re-emits on every change. `observeEntriesForWeek` emits the
seven local days that end with that day. `addEntry` returns the new row id.
`removeEntry` on an unknown id is a no-op. No method performs a network call.

### 1.6 `NutritionGoalRepository`

```kotlin
public interface NutritionGoalRepository {
    public fun observeGoal(): Flow<NutritionGoal>
    public suspend fun saveGoal(goal: NutritionGoal)
}
```

Contract: `observeGoal` emits the stored goal, or the default goal when nothing
is stored yet.

### 1.7 `DayBoundaryProvider` and `DiaryDay`

```kotlin
public interface DayBoundaryProvider {
    public fun startOfDay(epochMillis: Long): Long
    public fun addDays(dayStartEpochMillis: Long, days: Int): Long
}

public object DiaryDay {
    public fun startOfDay(provider: DayBoundaryProvider, epochMillis: Long): Long
    public fun lastDays(provider: DayBoundaryProvider, epochMillis: Long, count: Int): List<Long>
}
```

Contract: the domain owns the rules and the platform supplies the time zone.
`addDays` steps whole calendar days, so a 23- or 25-hour day is handled exactly.
`lastDays` always returns exactly `count` days, oldest first, ending with the day
that contains the timestamp.

### 1.8 `NutritionCalculator`

```kotlin
public object NutritionCalculator {
    public fun contribution(product: Product, quantityGrams: Double): NutritionTotals
    public fun servingGrams(product: Product): Double?
    public fun defaultQuantityGrams(product: Product): Double
    public fun entryFor(
        product: Product,
        mealType: MealType,
        quantityGrams: Double,
        loggedAtEpochMillis: Long,
        dayEpochMillis: Long,
    ): DiaryEntry
}
```

Contract: a value per 100 g is scaled linearly; a value per serving is scaled by
the serving size the product declares; a basis that cannot be resolved yields no
number at all, because a wrong number is worse than an absent one.

## 2. Domain - `core:domain`, package `domain.navigation`

```kotlin
public enum class NutriLogDestination(
    public val route: String,
    public val label: String,
) {
    TODAY("today", "Today"),
    LIBRARY("library", "Library"),
    MORE("more", "More"),
    ;

    public companion object {
        public val start: NutriLogDestination = TODAY
        public fun fromRoute(route: String?): NutriLogDestination?
    }
}

public object NutriLogRoutes {
    public const val TODAY: String = "today"
    public const val LIBRARY: String = "library"
    public const val MORE: String = "more"
    public const val ADD_FOOD_GRAPH: String = "addfood"
    public const val ADD_FOOD_SOURCE: String = "addfood/source"
    public const val ADD_FOOD_QUANTITY: String = "addfood/quantity"
    public const val PRODUCT_DETAIL: String = "product/{code}"
    public const val COACH: String = "coach"
    public const val WEEKLY: String = "weekly"

    public fun productDetail(code: String): String
}
```

Contract: the model is plain data with no Compose types, so a future native
SwiftUI navigation can be built from the same definitions (ADR-0015, ADR-0019).

## 3. Domain - `core:domain`, package `domain.assistant`

```kotlin
public enum class AssistantRole { USER, ASSISTANT }

public data class AssistantMessage(
    public val role: AssistantRole,
    public val text: String,
)

public sealed interface AssistantAction {
    public data class LogFood(
        public val productCode: String,
        public val quantityGrams: Double,
        public val mealType: MealType,
    ) : AssistantAction

    public data class RemoveEntry(public val entryId: Long) : AssistantAction

    public data class ChangeEntry(
        public val entryId: Long,
        public val quantityGrams: Double? = null,
        public val mealType: MealType? = null,
    ) : AssistantAction
}

public data class AssistantContext(
    public val entries: List<DiaryEntry>,
    public val goal: NutritionGoal,
    public val libraryCodes: List<String>,
)

public data class AssistantReply(
    public val text: String,
    public val actions: List<AssistantAction>,
)

public interface AssistantClient {
    public suspend fun complete(
        public val messages: List<AssistantMessage>,
        public val context: AssistantContext,
    ): AssistantReply
}
```

Contract: the client returns **proposals**, never applied changes. Free text is
displayed and never executed.

```kotlin
public class AssistantActionExecutor(
    private val diaryRepository: DiaryRepository,
    private val productRepository: ProductSearchRepository,
) {
    public suspend fun validate(action: AssistantAction): ActionValidation
    public suspend fun apply(action: AssistantAction): ActionOutcome
}
```

Contract: `validate` checks the action against the domain rules and against the
catalogue, and reports why an action was rejected. `apply` writes only when the
action is valid; an invalid action leaves the diary untouched.

## 4. Data - `core:data`, package `data.assistant`

```kotlin
public class AiGatewayClient(
    private val httpClient: HttpClient,
    private val baseUrl: String,
) {
    public suspend fun complete(
        public val messages: List<AssistantMessage>,
        public val context: AssistantContext,
    ): AssistantReply
}
```

Contract: the gateway contract is `POST {baseUrl}/v1/chat` with
`{"messages":[...],"context":{...}}` and the response
`{"reply":"...","actions":[...]}`. The client is provider-agnostic. When
`baseUrl` is blank the assistant is disabled and the caller must report that
state rather than attempt a request.

The gateway is exercised with Ktor's `MockEngine` in tests. No test performs a
live network call.

## 5. Feature - `feature:diary`

```kotlin
public data class DiaryState(
    public val dayEpochMillis: Long = 0,
    public val entriesByMeal: Map<MealType, List<DiaryEntry>> = emptyMap(),
    public val totals: NutritionTotals = NutritionTotals.ZERO,
    public val mealTotals: Map<MealType, NutritionTotals> = emptyMap(),
    public val goal: NutritionGoal = NutritionGoal(),
    public val remaining: NutritionTotals = NutritionTotals.ZERO,
    public val isLoading: Boolean = true,
)

public class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val goalRepository: NutritionGoalRepository,
    private val dayBoundaryProvider: DayBoundaryProvider,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
) {
    public val state: StateFlow<DiaryState>
    public fun refresh()
    public fun removeEntry(id: Long)
    public fun updateGoal(goal: NutritionGoal)
}
```

Contract: the state is a single immutable value. `refresh` re-reads the current
day, so a day boundary crossed while the app is open is picked up. The totals in
the state are always the sum of the entries in the state.

## 6. Feature - `feature:summary`

```kotlin
public data class DaySummary(
    public val dayEpochMillis: Long,
    public val entryCount: Int,
    public val totals: NutritionTotals,
    public val isEmpty: Boolean,
)

public data class WeeklySummary(
    public val days: List<DaySummary>,
    public val averageTotals: NutritionTotals,
    public val daysWithEntries: Int,
)

public object SummaryCalculator {
    public fun daily(
        public val dayEpochMillis: Long,
        public val entries: List<DiaryEntry>,
        public val goal: NutritionGoal,
    ): DailySummary

    public fun weekly(
        public val dayBoundaryProvider: DayBoundaryProvider,
        public val todayEpochMillis: Long,
        public val entries: List<DiaryEntry>,
    ): WeeklySummary
}
```

Contract: `weekly` always returns exactly seven days, oldest first, ending with
today. The average is over the days that have entries only; a day with no
entries is `isEmpty = true` and is never counted as a zero-intake day.

## 7. Feature - `feature:assistant`

```kotlin
public sealed interface AssistantAvailability {
    public data object Ready : AssistantAvailability
    public data object NotConfigured : AssistantAvailability
    public data object Offline : AssistantAvailability
    public data object ConsentRequired : AssistantAvailability
}

public data class AssistantState(
    public val availability: AssistantAvailability = AssistantAvailability.NotConfigured,
    public val messages: List<AssistantMessage> = emptyList(),
    public val pendingProposal: AssistantReply? = null,
    public val isSending: Boolean = false,
    public val error: String? = null,
)

public class CoachViewModel(...) {
    public val state: StateFlow<AssistantState>
    public fun send(text: String)
    public fun confirmProposal()
    public fun dismissProposal()
    public fun grantConsent()
}
```

Contract: nothing is written to the diary until `confirmProposal` is called. The
availability state distinguishes "not configured" from "offline", because the
user's next action differs.

## 8. Feature - `feature:scan`

```kotlin
public sealed interface BarcodeResolution {
    public data class Resolved(public val product: Product) : BarcodeResolution
    public data class NotInCache(public val code: String) : BarcodeResolution
    public data class Unknown(public val code: String) : BarcodeResolution
}

public class BarcodeResolver(
    private val productRepository: ProductSearchRepository,
) {
    public suspend fun resolve(code: String): BarcodeResolution
}
```

Contract: the resolver resolves through the existing
`ProductSearchRepository.getProduct(code)`, so a scanned code uses the same
offline cache as the rest of the catalogue and no second data source is
introduced.

## 9. Navigation - `androidApp`

### 9.1 Route table

| Route | Kind | Bottom bar | Purpose |
| --- | --- | --- | --- |
| `today` | destination | shown | The diary home |
| `library` | destination | shown | The personal food library |
| `more` | destination | shown | Goals, coach, premium, privacy, about |
| `addfood` | nested graph | hidden | The add-food flow |
| `addfood/source` | nested step | hidden | Search, my foods, scan |
| `addfood/quantity` | nested step | hidden | Quantity, meal and confirmation |
| `product/{code}` | child | hidden | The product detail |
| `coach` | child | hidden | The AI assistant |
| `weekly` | child | hidden | The seven-day summary |

### 9.2 Navigation rules

1. The app starts on `today`.
2. Selecting a destination navigates to its root with
   `popUpTo(startDestination) { inclusive = false }` and `launchSingleTop = true`,
   so every child screen above the home is removed. A destination is therefore
   always presented as its own root.
3. Re-selecting the current destination is a no-op; no duplicate entry is pushed.
4. A child screen is pushed on the current stack. Back returns to the screen that
   opened it.
5. From a destination other than `today`, back returns to `today`.
6. The product detail is a route, not shared view-model state, so leaving a
   destination discards it.
7. The bottom bar is visible only on the three top-level routes. The visibility
   is derived from the route hierarchy, never from a lookup that can silently
   fail for a nested route.
8. The floating action button opens `addfood/source` from `today` and from
   `library`.
9. The meal picker is a modal bottom sheet. It creates no navigation entry.

### 9.3 The add-food flow

The source step may pre-select a food, when the flow was opened from a Library
row or from a product detail, and it may pre-select a meal, when the flow was
opened from a meal section. The quantity step confirms the entry.

## 10. UI contracts

### 10.1 `TodayScreen`

```kotlin
@Composable
public fun TodayScreen(
    state: DiaryState,
    weeklySummary: WeeklySummary?,
    onAddFood: (MealType) -> Unit,
    onOpenProduct: (String) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onOpenWeekly: () -> Unit,
    onOpenCoach: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
)
```

### 10.2 `AddFoodScreen`

```kotlin
@Composable
public fun AddFoodScreen(
    state: AddFoodState,
    onSourceSelected: (AddFoodSource) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onProductSelected: (String) -> Unit,
    onBarcodeScanned: (String) -> Unit,
    onQuantityChanged: (Double) -> Unit,
    onMealSelected: (MealType) -> Unit,
    onConfirm: (MealType, String, Double) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
)
```

Contract: the quantity step shows the calories and macros the chosen quantity
produces, derived from the product's nutrition values and their basis, before the
user confirms.

### 10.3 `LibraryScreen`

```kotlin
@Composable
public fun LibraryScreen(
    foods: List<SavedProduct>,
    onOpenProduct: (String) -> Unit,
    onAddFood: (SavedProduct) -> Unit,
    onRemove: (String) -> Unit,
    onFindFoods: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
)
```

### 10.4 `MoreScreen`

```kotlin
@Composable
public fun MoreScreen(
    goal: NutritionGoal,
    onGoalChanged: (NutritionGoal) -> Unit,
    onOpenCoach: () -> Unit,
    purchaseContent: (@Composable () -> Unit)?,
    onOpenPrivacyPolicy: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
)
```

### 10.5 `CoachScreen`

```kotlin
@Composable
public fun CoachScreen(
    state: AssistantState,
    onSend: (String) -> Unit,
    onConfirmProposal: () -> Unit,
    onDismissProposal: () -> Unit,
    onGrantConsent: () -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
)
```

## 11. Unchanged contracts

- `ProductSearchRepository`, `RecentSearchRepository`, `SavedProductRepository`
  and `PurchaseRepository` keep their existing signatures and behaviour.
- `PurchaseClient` and the purchase state machine are unchanged; only the
  placement of the purchase section and the product code change.
- The per-food purchasable codes are replaced by the single premium product code,
  which is one shared constant.
