# Quickstart: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21

## What this feature changes

The app stops being a catalogue browser with a save button and becomes a nutrition
tracker:

- It opens on **Today**, the diary of the current day.
- Meals (Breakfast, Lunch, Dinner, Snacks) are the spine of the app.
- A food can be logged with a quantity, and the day's totals update immediately.
- The saved products become a **Library** of foods that are one tap from being
  logged.
- **Search** and **More** (goals, premium, about) complete the four destinations.
- The product detail is a scoped navigation route, which removes the defect where
  tapping Search after leaving another tab showed a product detail.
- The visual system is replaced with a neutral, data-first, commercial design.

## Run it

```bash
# Build and install on a connected device or emulator
./gradlew :androidApp:installDebug

# Unit tests (shared modules)
./gradlew :core:domain:allTests :core:data:allTests \
          :feature:search:allTests :feature:savedproducts:allTests \
          :feature:diary:allTests :feature:summary:allTests

# Instrumented tests
./gradlew :androidApp:connectedDebugAndroidTest

# Quality gates
./gradlew :architectureTest :coverageVerify :kotlinLint :plainTextCheck :androidApp:lint

# iOS still compiles
./gradlew :shared:assembleSharedXCFramework
```

## Walk through the new flow

1. Launch the app. The **Today** screen opens with today's date, the calorie and
   macro summary against the default goals, and the four meal sections.
2. Tap the add action on **Lunch**. The add-food flow opens with Lunch already
   selected.
3. On the food step, either type a food name and search, or pick one from **My
   foods**. The library is offered without typing, so a saved food logs in one tap.
4. On the quantity step, check the calories and macros the quantity produces, pick
   a multiplier or type a gram value, and confirm.
5. The entry appears in the Lunch section and the daily totals update.
6. Tap the entry's remove action; the entry leaves the diary and the totals
   update again.
7. Open **Library**. The food you saved from Search is listed with an add action.
8. Open **More**. Change the calorie goal, then return to **Today** and confirm
   the summary uses the new goal. The premium purchase section and the Open Food
   Facts attribution are here.

## Verify the navigation fix

1. Open **Search**, tap a result to open the product detail.
2. Tap the **Library** tab.
3. Tap the **Search** tab again.
4. The **search screen** is shown, not the product detail. This is the reported
   defect and it is now covered by an instrumented regression test.

Also check:

- Tapping the tab that is already selected does nothing.
- Back on the product detail returns to the screen that opened it.
- Back on a tab other than Today returns to Today.

## Data and migration

The database moves from schema version 1 to version 2 with an additive migration.
Existing saved products, purchase records and the catalogue cache survive the
upgrade untouched. To check the migration on a device with data, install the
version built from `main` first, save a product, then install the debug build of
this branch and confirm the saved product is still there.

## Where the code is

| Area | Location |
| --- | --- |
| Domain | `core/domain/.../domain/diary/` |
| Persistence | `core/data/.../sqldelight/.../DiaryEntry.sq`, `2.sqm`, `DiaryRepositoryImpl.kt` |
| Diary state | `feature/diary/.../feature/diary/` |
| Summaries | `feature/summary/.../feature/summary/` |
| Shell and navigation | `androidApp/.../AppShell.kt`, `ui/NutriLogDestination.kt` |
| Design system | `androidApp/.../ui/theme/`, `androidApp/.../ui/components/` |
| Screens | `androidApp/.../today/`, `addfood/`, `library/`, `more/`, `weekly/` |
