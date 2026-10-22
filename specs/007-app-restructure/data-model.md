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
| `energyKcal` | Double? | Snapshot of calories for the logged quantity |
| `proteinGrams` | Double? | Snapshot of protein for the logged quantity |
| `carbohydrateGrams` | Double? | Snapshot of carbohydrates for the logged quantity |
| `fatGrams` | Double? | Snapshot of fat for the logged quantity |
| `loggedAtEpochMillis` | Long | When the entry was logged |
| `dayEpochMillis` | Long | Local midnight of the day the entry belongs to |

Validation: `productCode` and `productName` are not blank, `quantityGrams` is
finite and greater than zero, and the nutrient values are finite and not
negative.

A nutrient is nullable because the catalogue does not report every nutrient for
every product. `null` means "not reported", which is a different statement from
"zero".

### NutritionTotals (domain value object)

The nutrition of a set of entries.

| Attribute | Type | Notes |
| --- | --- | --- |
| `energyKcal` | Double? | `null` when no entry reports it |
| `proteinGrams` | Double? | `null` when no entry reports it |
| `carbohydrateGrams` | Double? | `null` when no entry reports it |
| `fatGrams` | Double? | `null` when no entry reports it |

Operations: `plus(other)` for addition, `scaledBy(factor)` for scaling,
`remainingAgainst(goal)` for the amount left before the goal is reached, and
`fromEntries(entries)` for the total of a set of entries. `isComplete` reports
whether every component is known.

The aggregation rule is conservative about honesty and generous about
usefulness:

- No entry reports a nutrient: the total is **unknown** (`null`), never zero.
- Some entries report it and others do not: the total is **unknown** for that
  nutrient as well, because a partial sum would claim more than is known. The
  UI reports the nutrient as incomplete rather than as a number.
- Every entry reports it: the total is exact.

The empty total is a known zero: nothing has been eaten yet.

The implementation therefore carries no separate "incomplete" set. The
nullability of each component already encodes the distinction, and `isComplete`
answers "is anything unknown" for the whole total.

### NutritionGoal (domain entity)

The daily targets. A single record.

| Attribute | Type | Default |
| --- | --- | --- |
| `energyKcal` | Int | 2000 |
| `proteinGrams` | Int | 120 |
| `carbohydrateGrams` | Int | 220 |
| `fatGrams` | Int | 70 |

### DailySummary (core:domain)

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

### WeeklySummary (core:domain)

The last seven days.

| Attribute | Type | Notes |
| --- | --- | --- |
| `days` | List<DaySummary> | Always seven, oldest first |
| `averageTotals` | NutritionTotals | Average over the days **that have entries** |
| `daysWithEntries` | Int | Number of days with at least one entry |

Each `DaySummary` carries the day, the entry count, that day's totals, and an
`isEmpty` flag. An empty day reports zero totals **and** `isEmpty = true`, so the
UI can show it as "no entries" rather than as "0 kcal eaten".

### AssistantMessage (domain value object)

One turn of the conversation with the coach.

| Attribute | Type | Notes |
| --- | --- | --- |
| `role` | AssistantRole | `USER` or `ASSISTANT` |
| `text` | String | The message body; never blank |
| `proposal` | AssistantAction? | The action the assistant proposes, when it proposes one |

### AssistantAction (domain value object)

A structured change the assistant proposes. Every action is validated before it
can be applied, and an invalid action is rejected with an explanation while the
diary is left untouched.

| Variant | Fields | Meaning |
| --- | --- | --- |
| `LogFood` | `productCode`, `quantityGrams`, `mealType` | Log a food into a meal |
| `RemoveEntry` | `entryId` | Remove a logged entry |
| `ChangeEntry` | `entryId`, `quantityGrams?`, `mealType?` | Change a logged entry |

Validation: `productCode` is not blank, `quantityGrams` is finite and greater
than zero, `mealType` is one of the four meals, and `entryId` is positive. A
`LogFood` whose code is not in the catalogue is rejected.

### AssistantContext (domain value object)

The minimal context sent with a request.

| Attribute | Type | Notes |
| --- | --- | --- |
| `entries` | List<DiaryEntry> | Today's logged entries only |
| `goal` | NutritionGoal | The current daily goals |
| `libraryCodes` | List<String> | The codes of the user's library foods |

The catalogue, the recent search terms and the purchase records are **never**
part of the context.

### AssistantReply (domain value object)

| Attribute | Type | Notes |
| --- | --- | --- |
| `text` | String | The reply shown to the user |
| `actions` | List<AssistantAction> | The proposed changes, each still unapplied |

### BarcodeResolution (domain value object)

The outcome of resolving a scanned code.

| Variant | Fields | Meaning |
| --- | --- | --- |
| `Resolved` | `product: Product` | The code is in the catalogue |
| `NotInCache` | `code: String` | The code is not in the offline catalogue cache |
| `Unknown` | `code: String` | The code is not a product this app knows |

### PremiumEntitlement (domain value object)

Whether the single premium product has been verified as purchased. The premium
product code is one shared constant, `nutrilog_pro`, and the entitlement
currently grants access to the assistant.

## Persistence

### New tables (SQLDelight, schema version 3)

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
    energy_kcal REAL,
    protein_grams REAL,
    carbohydrate_grams REAL,
    fat_grams REAL,
    logged_at_epoch_millis INTEGER NOT NULL,
    day_epoch_millis INTEGER NOT NULL
);

CREATE INDEX diary_entry_day_index ON DiaryEntry(day_epoch_millis);

CREATE TABLE NutritionGoal (
    singleton INTEGER NOT NULL PRIMARY KEY CHECK (singleton = 1),
    energy_kcal INTEGER NOT NULL,
    protein_grams INTEGER NOT NULL,
    carbohydrate_grams INTEGER NOT NULL,
    fat_grams INTEGER NOT NULL
);
```

The nutrient columns are nullable: `null` means the catalogue did not report the
nutrient for that product.

### Migration

The schema goes from version 2 to version 3. `2.sqm` contains exactly the two
`CREATE TABLE` statements and the index. It is additive: version 2 databases
(catalogue cache, saved products, purchase records, recent searches) migrate
without any data change. `1.sqm` (version 1 to 2) is unchanged, so a database
created by an earlier release still migrates in one step.

### Schema snapshots

SQLDelight verifies the migration chain against the snapshots in
`core/data/src/commonMain/sqldelight/databases/`:

| File | Schema version | How it is produced |
| --- | --- | --- |
| `1.db` | 1 | Committed with the original schema |
| `2.db` | 2 | Reconstructed from `1.db` plus `1.sqm` |
| `3.db` | 3 | Generated by `generateCommonMainProductDatabaseSchema` |

`2.db` was missing before this feature, which meant a fresh checkout could not
verify the chain past version 1 and the v2 state was never checked. Both `2.db`
and `3.db` are committed, so the whole chain verifies from v1 on a clean
checkout.

`verifyMigrations` stays enabled and checks the whole chain, and the verification
task is wired into the CI workflow, which previously never invoked it.

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
