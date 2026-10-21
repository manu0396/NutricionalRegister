# Contracts: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21

Public contracts introduced or changed by this feature. All of them live in the
shared KMP modules; the Android UI consumes them and never reimplements them.

## 1. Domain - `core:domain`

### 1.1 `MealType`

```kotlin
public enum class MealType(public val displayName: String, public val order: Int) {
    BREAKFAST("Breakfast", 0),
    LUNCH("Lunch", 1),
    DINNER("Dinner", 2),
    SNACKS("Snacks", 3),
    ;

    public companion object {
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
    val energyKcal: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbohydrateGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
    val loggedAtEpochMillis: Long = 0,
    val dayEpochMillis: Long = 0,
)
```

Validation: `productCode` and `productName` are not blank; `quantityGrams` is
finite and greater than zero; every nutrient value is finite and not negative.

### 1.3 `NutritionTotals`

```kotlin
public data class NutritionTotals(
    val energyKcal: Double = 0.0,
    val proteinGrams: Double = 0.0,
    val carbohydrateGrams: Double = 0.0,
    val fatGrams: Double = 0.0,
) {
    public operator fun plus(other: NutritionTotals): NutritionTotals
    public fun scaledBy(factor: Double): NutritionTotals
}
```

Contract: `plus` is component-wise and commutative; `scaledBy` multiplies every
component by `factor`.

### 1.4 `NutritionGoal`

```kotlin
public data class NutritionGoal(
    val energyKcal: Int = 2000,
    val proteinGrams: Int = 120,
    val carbohydrateGrams: Int = 220,
    val fatGrams: Int = 70,
)
```

Contract: every target is greater than zero.

### 1.5 `DiaryRepository`

```kotlin
public interface DiaryRepository {
    public fun observeEntriesForDay(dayEpochMillis: Long): Flow<List<DiaryEntry>>
    public suspend fun addEntry(entry: DiaryEntry): Long
    public suspend fun updateEntry(entry: DiaryEntry)
    public suspend fun removeEntry(id: Long)
}
```

Contract: `observeEntriesForDay` emits the entries of exactly that local day,
ordered by log time, and re-emits on every change. `addEntry` returns the new
row id. `removeEntry` on an unknown id is a no-op.

### 1.6 `NutritionGoalRepository`

```kotlin
public interface NutritionGoalRepository {
    public fun observeGoal(): Flow<NutritionGoal>
    public suspend fun saveGoal(goal: NutritionGoal)
}
```

Contract: `observeGoal` emits the stored goal, or the default goal when nothing
is stored yet.

### 1.7 `DayKey`

```kotlin
public object DayKey {
    public fun startOfDay(epochMillis: Long): Long
    public fun daysBetween(startEpochMillis: Long, endEpochMillis: Long): Int
}
```

Contract: `startOfDay` returns the local midnight of the day containing the
timestamp. `daysBetween` returns the number of local-day boundaries between two
timestamps.

## 2. Feature - `feature:diary`

```kotlin
public data class DiaryState(
    val dayEpochMillis: Long = 0,
    val entriesByMeal: Map<MealType, List<DiaryEntry>> = emptyMap(),
    val totals: NutritionTotals = NutritionTotals(),
    val mealTotals: Map<MealType, NutritionTotals> = emptyMap(),
    val goal: NutritionGoal = NutritionGoal(),
    val isLoading: Boolean = true,
)

public class DiaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val goalRepository: NutritionGoalRepository,
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

## 3. Feature - `feature:summary`

```kotlin
public data class DaySummary(
    val dayEpochMillis: Long,
    val entryCount: Int,
    val totals: NutritionTotals,
    val isEmpty: Boolean,
)

public data class WeeklySummary(
    val days: List<DaySummary>,
    val averageTotals: NutritionTotals,
    val daysWithEntries: Int,
)

public object SummaryCalculator {
    public fun daily(
        dayEpochMillis: Long,
        entries: List<DiaryEntry>,
        goal: NutritionGoal,
    ): DailySummary

    public fun weekly(
        todayEpochMillis: Long,
        entries: List<DiaryEntry>,
    ): WeeklySummary
}
```

Contract: `weekly` always returns exactly seven days, oldest first, ending with
today. The average is over the days that have entries only; a day with no entries
is `isEmpty = true` and is never counted as a zero-intake day in the average.

## 4. Navigation - `androidApp`

### 4.1 `NutriLogDestination`

```kotlin
public enum class NutriLogDestination(public val route: String, public val label: String) {
    TODAY("today", "Today"),
    SEARCH("search", "Search"),
    LIBRARY("library", "Library"),
    MORE("more", "More"),
    ;

    public companion object {
        public val start: NutriLogDestination = TODAY
        public fun fromRoute(route: String?): NutriLogDestination?
    }
}
```

### 4.2 Route constants

| Route | Kind | Bottom bar | Purpose |
| --- | --- | --- | --- |
| `today` | destination | shown | The diary home |
| `search` | destination | shown | Catalogue search |
| `library` | destination | shown | The personal food library |
| `more` | destination | shown | Goals, premium, about |
| `product/{code}` | child | hidden | The product detail |
| `add-food` | child | hidden | The add-food flow |
| `weekly` | child | hidden | The weekly summary |

### 4.3 Navigation rules

1. The app starts on `today`.
2. Selecting a destination navigates to its root with
   `popUpTo(startDestination) { inclusive = false }` and `launchSingleTop = true`,
   so every child screen above the home is removed. A tab is therefore always
   presented as its own root.
3. Re-selecting the current destination is a no-op; no duplicate entry is pushed.
4. A child screen is pushed on the current destination's stack. Back returns to
   the screen that opened it.
5. From a destination other than `today`, back returns to `today`.
6. The product detail is a route, not shared view-model state, so leaving a tab
   discards it.

## 5. UI contracts

### 5.1 `TodayScreen`

```kotlin
@Composable
public fun TodayScreen(
    state: DiaryState,
    weeklySummary: WeeklySummary?,
    onAddFood: (MealType) -> Unit,
    onOpenProduct: (String) -> Unit,
    onRemoveEntry: (Long) -> Unit,
    onOpenWeekly: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
)
```

### 5.2 `AddFoodScreen`

```kotlin
@Composable
public fun AddFoodScreen(
    initialMeal: MealType,
    selectedProduct: Product?,
    searchState: ProductSearchState,
    library: List<SavedProduct>,
    onMealSelected: (MealType) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onProductSelected: (String) -> Unit,
    onQuantityChanged: (Double) -> Unit,
    onConfirm: (MealType, String, Double) -> Unit,
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
)
```

Contract: the quantity step shows the calories and macros the chosen quantity
produces, derived from the product's nutrition values and their basis, before the
user confirms.

### 5.3 `LibraryScreen`

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

### 5.4 `MoreScreen`

```kotlin
@Composable
public fun MoreScreen(
    goal: NutritionGoal,
    onGoalChanged: (NutritionGoal) -> Unit,
    purchaseContent: (@Composable () -> Unit)?,
    onOpenPrivacyPolicy: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
)
```

## 6. Unchanged contracts

- `ProductSearchRepository`, `RecentSearchRepository`, `SavedProductRepository`
  and `PurchaseRepository` keep their existing signatures and behaviour.
- `PurchaseClient` and the purchase state machine are unchanged; only the
  placement of the purchase section changes.
