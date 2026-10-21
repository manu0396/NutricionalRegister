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
    val energyKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
    val loggedAtEpochMillis: Long = 0,
    val dayEpochMillis: Long = 0,
)
```

Validation: `productCode` and `productName` are not blank; `quantityGrams` is
finite and greater than zero; every nutrient value is finite and not negative
when it is present.

A nutrient is `null` when the catalogue does not report it. That is a different
statement from zero, and the UI never turns one into the other.

### 1.3 `Nutrient`

```kotlin
public enum class Nutrient {
    ENERGY,
    PROTEIN,
    CARBOHYDRATE,
    FAT,
}
```

### 1.4 `NutritionTotals`

```kotlin
public data class NutritionTotals(
    val energyKcal: Double? = null,
    val proteinGrams: Double? = null,
    val carbohydrateGrams: Double? = null,
    val fatGrams: Double? = null,
    val incomplete: Set<Nutrient> = emptySet(),
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

Contract: `plus` is component-wise and commutative. A component is `null` when
nothing reports it, and is listed in `incomplete` when only some of the
contributions report it, so the UI can show a lower bound instead of a
fabricated number. `scaledBy` multiplies every known component by `factor` and
keeps the incomplete markers. `remainingAgainst` never returns a negative
amount and keeps the incomplete markers. `ZERO` is the total of nothing: every
component is a known zero.

### 1.5 `NutritionGoal`

```kotlin
public data class NutritionGoal(
    val energyKcal: Int = 2000,
    val proteinGrams: Int = 120,
    val carbohydrateGrams: Int = 220,
    val fatGrams: Int = 70,
)
```

Contract: every target is greater than zero.

### 1.6 `DiaryRepository`

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
ordered by log time, and re-emits on every change. `observeEntriesForWeek`
emits the entries of the seven local days that end with the given day.
`addEntry` returns the new row id. `removeEntry` on an unknown id is a no-op.

### 1.7 `NutritionGoalRepository`

```kotlin
public interface NutritionGoalRepository {
    public fun observeGoal(): Flow<NutritionGoal>
    public suspend fun saveGoal(goal: NutritionGoal)
}
```

Contract: `observeGoal` emits the stored goal, or the default goal when nothing
is stored yet.

### 1.8 `DayBoundaryProvider` and `DiaryDay`

```kotlin
public interface DayBoundaryProvider {
    public fun startOfDay(epochMillis: Long): Long
    public fun addDays(dayStartEpochMillis: Long, days: Int): Long
}

public object DiaryDay {
    public fun lastDays(
        dayBoundaryProvider: DayBoundaryProvider,
        todayEpochMillis: Long,
        count: Int,
    ): List<Long>
}
```

Contract: `startOfDay` returns the local midnight of the day containing the
timestamp. `addDays` steps calendar days, so a 23- or 25-hour daylight-saving
day is handled exactly. The provider is implemented per platform: `java.util.Calendar`
on Android and `NSCalendar` on iOS. `lastDays` returns `count` local days,
oldest first, ending with the day containing `todayEpochMillis`.

The provider is an interface rather than a static helper because the shared
modules have no time-zone library, and because it keeps the day arithmetic
testable with a fixed day length.

### 1.9 `NutritionCalculator`

```kotlin
public object NutritionCalculator {
    public fun contribution(
        nutritionValues: List<NutritionValue>,
        quantityGrams: Double,
    ): NutritionTotals

    public fun entryFor(
        product: Product,
        mealType: MealType,
        quantityGrams: Double,
        loggedAtEpochMillis: Long,
        dayEpochMillis: Long,
    ): DiaryEntry
}
```

Contract: the quantity is always expressed in grams. A value reported per
100 g is scaled by `quantityGrams / 100`; a value reported per serving is
scaled by `quantityGrams / servingGrams` when the serving size can be read
from the product's serving description, and stays unknown when it cannot. A
product with no nutrition values contributes nothing but is still logged.

### 1.10 Summaries - `core:domain`

```kotlin
public data class DailySummary(
    val dayEpochMillis: Long,
    val entries: List<DiaryEntry>,
    val entriesByMeal: Map<MealType, List<DiaryEntry>>,
    val totals: NutritionTotals,
    val mealTotals: Map<MealType, NutritionTotals>,
    val goal: NutritionGoal,
)

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
    public const val DAYS_IN_WEEK: Int = 7

    public fun daily(
        dayEpochMillis: Long,
        entries: List<DiaryEntry>,
        goal: NutritionGoal,
    ): DailySummary

    public fun weekly(
        dayBoundaryProvider: DayBoundaryProvider,
        todayEpochMillis: Long,
        entries: List<DiaryEntry>,
    ): WeeklySummary
}
```

Contract: `daily` always reports every meal of the day, including the meals
with no entries. `weekly` always returns exactly seven days, oldest first,
ending with today. The average is over the days that have entries only; a day
with no entries is `isEmpty = true` and is never counted as a zero-intake day
in the average.

The summaries live in `core:domain` and not in `feature:summary`: the daily
summary is needed by the diary feature, and the module policy forbids a
feature module from depending on another feature module.

## 2. Feature - `feature:diary`

```kotlin
public data class DiaryState(
    val dayEpochMillis: Long = 0,
    val entriesByMeal: Map<MealType, List<DiaryEntry>> = emptyMap(),
    val totals: NutritionTotals = NutritionTotals.ZERO,
    val mealTotals: Map<MealType, NutritionTotals> = emptyMap(),
    val goal: NutritionGoal = NutritionGoal(),
    val isLoading: Boolean = true,
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
    public fun log(product: Product, mealType: MealType, quantityGrams: Double)
    public fun removeEntry(id: Long)
    public fun updateGoal(goal: NutritionGoal)
    public fun clear()
}
```

Contract: the state is a single immutable value. `refresh` re-reads the current
day, so a day boundary crossed while the app is open is picked up. The totals in
the state are always the sum of the entries in the state. `log` snapshots the
product name and its nutrition values, so a later catalogue change cannot
rewrite what the user ate.

## 3. Feature - `feature:summary`

```kotlin
public class SummaryViewModel(
    private val diaryRepository: DiaryRepository,
    private val dayBoundaryProvider: DayBoundaryProvider,
    private val clock: EpochClock,
    private val scope: CoroutineScope,
) {
    public val weeklySummary: StateFlow<WeeklySummary?>
    public fun refresh()
    public fun clear()
}
```

Contract: the summary is `null` until the first week has been read, so the
screen can show a progress indicator instead of an empty week that is not
empty yet. The summary is computed from the stored entries on every change and
is never cached in the database.

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
