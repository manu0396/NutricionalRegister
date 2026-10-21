# Data Model: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21

## Overview

The restructure introduces a diary domain: logged entries, meals, daily totals and
daily goals. The catalogue (`Product`), the saved foods (`SavedProduct`) and the
purchase records (`PurchaseRecord`) are unchanged.

All new data is on-device. The nutrition values of an entry are **snapshotted at
log time** so that a later catalogue change cannot rewrite what the user ate.

## Entities

### MealType (domain enum)

The four meals of the day.

| Attribute | Type | Notes |
| --- | --- | --- |
| `name` | enum constant | `BREAKFAST`, `LUNCH`, `DINNER`, `SNACKS` |
| `displayName` | String | "Breakfast", "Lunch", "Dinner", "Snacks" |
| `order` | Int | 0, 1, 2, 3 - the display order of the day |

### DiaryEntry (domain entity)

One logged food.

| Attribute | Type | Notes |
| --- | --- | --- |
| `id` | Long | Row id; 0 for a not-yet-persisted entry |
| `productCode` | String | Identity of the logged product |
| `productName` | String | Snapshot of the name at log time |
| `brands` | String? | Snapshot of the brand |
| `imageThumbnailUrl` | String? | Snapshot of the thumbnail |
| `mealType` | MealType | The meal the entry belongs to |
| `quantityGrams` | Double | Logged quantity in grams; must be > 0 |
| `unit` | String | "g" by default |
| `energyKcal` | Double | Snapshot of calories for the logged quantity |
| `proteinGrams` | Double | Snapshot of protein for the logged quantity |
| `carbohydrateGrams` | Double | Snapshot of carbohydrates for the logged quantity |
| `fatGrams` | Double | Snapshot of fat for the logged quantity |
| `loggedAtEpochMillis` | Long | When the entry was logged |
| `dayEpochMillis` | Long | Local midnight of the day the entry belongs to |

Validation: `productCode` and `productName` are not blank, `quantityGrams` is
finite and greater than zero, and the nutrient values are finite and not
negative.

### NutritionTotals (domain value object)

The sum of the nutrition of a set of entries.

| Attribute | Type |
| --- | --- |
| `energyKcal` | Double |
| `proteinGrams` | Double |
| `carbohydrateGrams` | Double |
| `fatGrams` | Double |

Operations: `plus(other)` for addition and `scaledBy(factor)` for scaling a
per-100 g basis to a logged quantity. The empty total is all zeroes.

### NutritionGoal (domain entity)

The daily targets. A single record.

| Attribute | Type | Default |
| --- | --- | --- |
| `energyKcal` | Int | 2000 |
| `proteinGrams` | Int | 120 |
| `carbohydrateGrams` | Int | 220 |
| `fatGrams` | Int | 70 |

### DailySummary (feature:summary)

The Today screen's data.

| Attribute | Type |
| --- | --- |
| `dayEpochMillis` | Long |
| `entries` | List<DiaryEntry> |
| `entriesByMeal` | Map<MealType, List<DiaryEntry>> |
| `totals` | NutritionTotals |
| `mealTotals` | Map<MealType, NutritionTotals> |
| `goal` | NutritionGoal |
| `remaining` | NutritionTotals (goal minus totals, floored at zero) |

### WeeklySummary (feature:summary)

The last seven days.

| Attribute | Type | Notes |
| --- | --- | --- |
| `days` | List<DaySummary> | Always seven, oldest first |
| `averageTotals` | NutritionTotals | Average over the days **that have entries** |
| `daysWithEntries` | Int | Number of days with at least one entry |

Each `DaySummary` carries the day, the entry count, that day's totals, and an
`isEmpty` flag. An empty day reports zero totals **and** `isEmpty = true`, so the
UI can show it as "no entries" rather than as "0 kcal eaten".

## Persistence

### New tables (SQLDelight, schema version 2)

```sql
CREATE TABLE DiaryEntry (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    product_code TEXT NOT NULL,
    product_name TEXT NOT NULL,
    brands TEXT,
    image_thumbnail_url TEXT,
    meal_type TEXT NOT NULL,
    quantity_grams REAL NOT NULL,
    unit TEXT NOT NULL DEFAULT 'g',
    energy_kcal REAL NOT NULL,
    protein_grams REAL NOT NULL,
    carbohydrate_grams REAL NOT NULL,
    fat_grams REAL NOT NULL,
    logged_at_epoch_millis INTEGER NOT NULL,
    day_epoch_millis INTEGER NOT NULL
);

CREATE INDEX diary_entry_day ON DiaryEntry(day_epoch_millis);

CREATE TABLE NutritionGoal (
    singleton INTEGER NOT NULL PRIMARY KEY CHECK (singleton = 1),
    energy_kcal INTEGER NOT NULL,
    protein_grams INTEGER NOT NULL,
    carbohydrate_grams INTEGER NOT NULL,
    fat_grams INTEGER NOT NULL
);
```

### Migration

`2.sqm` contains exactly the two `CREATE TABLE` statements and the index. It is
additive: version 1 databases (catalogue cache, saved products, purchase records,
recent searches) migrate without any data change.

### Unchanged

`Product`, `ProductNutrition`, `SearchSnapshot`, `SearchSnapshotProduct`,
`RecentSearch`, `RecentSearchSequence`, `SavedProduct`, `PurchaseRecord`.

## Derived data

The daily and weekly summaries are **computed, never stored**. A stored copy would
have to be invalidated on every entry change, every goal change and every
time-zone change. The query cost is bounded by the number of entries in the
window, which is small.

## Day bucketing

An entry stores both `loggedAtEpochMillis` and `dayEpochMillis`. The day is the
local midnight of the day the entry was logged, computed once at write time with
the device's time zone. Reads group by `dayEpochMillis`, so a later time-zone
change cannot move a past entry into a different day.
