# Implementation Plan: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21
**Spec**: [spec.md](spec.md)
**Research**: [research.md](research.md)
**Data Model**: [data-model.md](data-model.md)
**Contracts**: [contracts/app-restructure-contracts.md](contracts/app-restructure-contracts.md)

## Summary

Turn the app from a catalogue browser with a save button into a nutrition tracker:
a Today home with meal-based diary logging, a daily and weekly summary against
editable goals, a personal food library, a corrected navigation model, and a
restrained commercial design system.

The work spans the shared KMP core (new diary domain, new persistence, new
summary feature modules) and the Android Compose UI (new shell, four
destinations, new screens, new design system). iOS keeps compiling; the SwiftUI
screens are not rebuilt.

## Technical Context

- **Language/Framework**: Kotlin 2.4 (KMP), Jetpack Compose (Android), SwiftUI (iOS)
- **UI**: Compose Multiplatform Material 3 (`compose-material3` 1.12.0-alpha03)
- **Navigation**: `androidx.navigation:navigation-compose` 2.9.2 (existing)
- **Persistence**: SQLDelight 2.4.1 (existing), schema v1 -> v2 migration
- **Async**: kotlinx.coroutines 1.11.0, Flow
- **Images**: Coil 3.6.3 (existing)
- **Icons**: `androidx.compose.material:material-icons-core` (existing, ADR-0011)
- **Testing**: kotlin-test (common), JUnit 4 + Compose UI Test (androidTest),
  Konsist (architecture), Kover (coverage, 80 % gate on `core:domain`/`core:data`)
- **New dependencies**: none. Every dependency used here already exists in the
  version catalog, so no new ADR is required (constitution rule III).

## Project Structure (after the restructure)

```
core/domain/src/commonMain/kotlin/com/example/nutricionalregister/domain/
├── product/                 (unchanged)
├── purchase/                (unchanged)
├── diary/                   NEW
│   ├── MealType.kt
│   ├── DiaryEntry.kt
│   ├── DiaryRepository.kt
│   ├── NutritionTotals.kt
│   ├── NutritionGoal.kt
│   └── NutritionGoalRepository.kt

core/data/src/commonMain/
├── sqldelight/.../DiaryEntry.sq        NEW (DiaryEntry + NutritionGoal tables)
├── sqldelight/.../2.sqm                 NEW (migration from schema v1)
└── kotlin/.../DiaryRepositoryImpl.kt    NEW
└── kotlin/.../NutritionGoalRepositoryImpl.kt  NEW

feature/diary/src/commonMain/.../feature/diary/     NEW
├── DiaryState.kt
├── DiaryViewModel.kt
└── DiaryIntents.kt

feature/summary/src/commonMain/.../feature/summary/  NEW
├── DailySummary.kt
├── WeeklySummary.kt
└── SummaryCalculator.kt

androidApp/src/main/kotlin/com/example/nutricionalregister/
├── App.kt, AppShell.kt            (rewritten navigation)
├── ui/
│   ├── NutriLogDestination.kt     (TODAY, SEARCH, LIBRARY, MORE)
│   ├── NutriLogBottomBar.kt       (four destinations)
│   ├── NutriLogScaffold.kt        (top app bar)
│   ├── theme/                     (new design system)
│   └── components/                NEW shared building blocks
├── today/TodayScreen.kt           NEW
├── addfood/AddFoodScreen.kt       NEW
├── library/LibraryScreen.kt       NEW (replaces savedproducts list screen)
├── more/MoreScreen.kt             NEW
├── weekly/WeeklySummaryScreen.kt  NEW
├── search/                        (kept, detail becomes a route)
└── savedproducts/                 (purchase section kept, list screen replaced)
```

## Approach

### Phase 0 - Foundation (design system + navigation model)

**0.1 Design system** (`ui/theme`)

- Replace the green/amber palette with a neutral base and one deep accent.
  Neutrals carry the layout; the accent is reserved for the primary action and
  the selected tab.
- Rebuild the type scale with stronger weight contrast.
- Reduce the shape radii from 16-24 dp to 8-14 dp.
- Keep `NutriLogSpacing` as the single spacing scale and apply it everywhere.
- Add shared components under `ui/components`: `NutriLogSectionHeader`,
  `NutriLogProgressBar`, `NutriLogMacroRow`, `NutriLogEmptyState`,
  `NutriLogStatTile`, `NutriLogListItem`.

**0.2 Navigation model** (`AppShell`, `NutriLogDestination`)

- `NutriLogDestination` becomes `TODAY`, `SEARCH`, `LIBRARY`, `MORE`, with
  `start = TODAY`.
- The shell renders a `NavHost` whose top-level routes are the four
  destinations, plus child routes `product/{code}`, `add-food`, and `weekly`.
- Tab selection navigates to the destination root with
  `popUpTo(home) { inclusive = false }` and `launchSingleTop = true`, so any
  child screen above the home is removed. Re-selecting the current tab is a
  no-op.
- The bottom bar is hidden on the full-screen child routes (`add-food`,
  `weekly`) and on the product detail, so the focus is on the content.
- The shell's slots change shape: instead of three per-destination slots, the
  shell takes a single `NavHost` builder plus the shared dependencies, so
  navigation state lives in one place.

### Phase 1 - Shared domain (core:domain)

- `MealType` enum with a display label and a display order.
- `DiaryEntry` data class with the nutrient snapshot.
- `NutritionTotals` data class with a `plus` operator and a `scaledBy(factor)`.
- `NutritionGoal` data class with defaults.
- `DiaryRepository` and `NutritionGoalRepository` interfaces.
- `DayKey` helper that converts an epoch-millis timestamp into the local
  midnight epoch-millis of that day.

Unit tests cover the meal ordering, the day bucketing across a midnight
boundary, the totals arithmetic and the goal defaults.

### Phase 2 - Persistence (core:data)

- `DiaryEntry.sq`: the `DiaryEntry` table (id autoincrement, product identity,
  meal, quantity, nutrient snapshot, timestamps, `day_epoch_millis`) and the
  `NutritionGoal` singleton table, with their queries.
- `2.sqm`: the additive migration (`CREATE TABLE` only) from schema v1.
- `DiaryRepositoryImpl` and `NutritionGoalRepositoryImpl` over the new tables.
- The `ProductDatabase` driver is created with the generated `Schema`, so the
  migration is applied automatically by SQLDelight on upgrade.
- `ProductSearchRuntime` gains the two new repositories.

Repository tests cover insert, update, remove, day filtering, ordering within a
day, and goal persistence.

### Phase 3 - Shared presentation logic (feature:diary, feature:summary)

- `feature:diary`: `DiaryState` (entries by meal, totals, goals, loading),
  `DiaryViewModel` with the intents the Today screen needs
  (`selectDay`, `removeEntry`, `addEntry`), and the day-change refresh.
- `feature:summary`: `SummaryCalculator` that produces the daily summary
  (entries grouped by meal, totals, remaining against goals) and the weekly
  summary (seven days, per-day totals, average over days with entries).

Both modules get unit tests for the calculation rules, including the
"empty day is not zero intake" rule.

### Phase 4 - Android UI

**4.1 Today screen**

- Date in the app bar, with the day of the week.
- Summary card: calories consumed against the goal with a progress bar, then
  three macro rows with their own progress bars and remaining amounts.
- Four meal sections, each with an icon, the meal name, the meal's calorie
  subtotal, the list of entries, and an add action. An empty meal shows a
  compact empty state with the add action.
- An entry row shows the product name, the quantity and the entry's calories,
  and exposes a remove action.
- A weekly card at the end: the seven-day calorie bars and the average, with a
  link to the weekly screen.

**4.2 Add-food flow**

- One screen with three explicit steps: meal, food, quantity.
- Step 1: four meal options as selectable rows, with the meal preselected when
  the flow was opened from a meal section.
- Step 2: a search field over the catalogue plus a "My foods" section that
  offers the library immediately, so the flow can be completed without typing.
- Step 3: the product summary, a quantity field in grams, quick multipliers
  (0.5x, 1x, 1.5x, 2x of the product's serving), the resulting calories and
  macros, and a confirm action.

**4.3 Library screen**

- The saved products as a list of rows with the add action and a remove action.
- An empty state that explains the purpose and links to Search.

**4.4 More screen**

- Goals section with editable targets.
- Premium section hosting the existing purchase flow.
- About section with the version and the Open Food Facts attribution.

**4.5 Search and product detail**

- The search screen keeps its behaviour and is restyled to the new system.
- The product detail becomes a navigation route. It gains an "Add to diary"
  action that starts the add-food flow with the product preselected.

### Phase 5 - Tests and validation

- Unit tests for the new domain, data, diary and summary modules.
- Instrumented tests for the new screens and for the corrected navigation
  (tab reset, back behaviour, no stale detail).
- The existing instrumented tests are updated for the new destinations and
  routes.
- The full verification gate is run before the branch is proposed.

## Migration and data compatibility

The schema goes from version 1 to version 2 with an additive migration:

```sql
CREATE TABLE DiaryEntry (...);
CREATE TABLE NutritionGoal (...);
```

No existing table is altered or dropped, so saved products, purchase records and
the catalogue cache survive the upgrade untouched. SQLDelight's
`verifyMigrations` task stays enabled and validates the migration chain against
the generated schema.

## Risk mitigation

| Risk | Mitigation |
| --- | --- |
| The navigation rewrite breaks existing instrumented tests | The navigation tests are rewritten in the same change and cover the new model explicitly |
| The theme change touches every screen | The theme lands first, then the screens are migrated one at a time, compiling after each |
| The new shared code drops below the coverage gate | Tests are written with the implementation, module by module |
| The module dependency policy rejects the new module edges | The allowlist is updated in the same commit that adds the dependency |
| The migration fails verification | `verifyMigrations` is enabled, so the failure surfaces in the build, not on a device |

## File changes (expected)

**New**
- `core/domain/.../domain/diary/{MealType,DiaryEntry,NutritionTotals,NutritionGoal,DiaryRepository,NutritionGoalRepository,DayKey}.kt`
- `core/domain/src/commonTest/.../domain/diary/*`
- `core/data/.../sqldelight/.../DiaryEntry.sq`, `core/data/.../sqldelight/.../2.sqm`
- `core/data/.../DiaryRepositoryImpl.kt`, `core/data/.../NutritionGoalRepositoryImpl.kt`
- `core/data/src/androidHostTest/.../DiaryRepositoryImplTest.kt`, `NutritionGoalRepositoryImplTest.kt`
- `feature/diary/src/commonMain/.../feature/diary/{DiaryState,DiaryViewModel}.kt`
- `feature/diary/src/commonTest/.../feature/diary/*`
- `feature/summary/src/commonMain/.../feature/summary/{DailySummary,WeeklySummary,SummaryCalculator}.kt`
- `feature/summary/src/commonTest/.../feature/summary/*`
- `androidApp/.../ui/theme/*` (rewritten), `androidApp/.../ui/components/*`
- `androidApp/.../today/TodayScreen.kt`, `androidApp/.../addfood/AddFoodScreen.kt`
- `androidApp/.../library/LibraryScreen.kt`, `androidApp/.../more/MoreScreen.kt`
- `androidApp/.../weekly/WeeklySummaryScreen.kt`
- `androidApp/src/androidTest/.../today/*`, `.../navigation/*` (rewritten), `.../addfood/*`

**Modified**
- `settings.gradle.kts` (no change: `feature:diary` and `feature:summary` are
  already declared)
- `core/data/build.gradle.kts` (no new dependency)
- `feature/diary/build.gradle.kts`, `feature/summary/build.gradle.kts`
- `core/data/.../ProductSearchRuntime.kt`, `core/data/.../PlatformDataFactories.kt` (android and ios)
- `androidApp/.../App.kt`, `androidApp/.../AppShell.kt`
- `androidApp/.../ui/NutriLogDestination.kt`, `ui/NutriLogBottomBar.kt`, `ui/NutriLogScaffold.kt`
- `androidApp/.../search/ProductSearchScreen.kt`, `search/ProductDetailScreen.kt`
- `androidApp/.../savedproducts/*` (list screen replaced by the library; purchase section kept)
- `build-logic/.../ModuleDependencyPolicyPlugin.kt` (allow the new module edges)
- `AI-LOG.md`

**Deleted**
- `androidApp/.../diary/DiaryPlaceholderScreen.kt` (replaced by the real diary)
- `androidApp/.../savedproducts/SavedProductsScreen.kt` (replaced by the library)
