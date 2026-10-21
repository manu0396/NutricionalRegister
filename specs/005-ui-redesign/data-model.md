# Data Model: Modern UI Redesign

**Feature**: `005-ui-redesign`  
**Branch**: `005-ui-redesign`  
**Date**: 2026-10-21

## Overview
This feature is **UI/UX only**. No changes to domain models, repositories, database schema, or API contracts are expected.

## Entities
None new. Existing entities remain unchanged:
- `Product` (domain)
- `SavedProduct`
- `PurchaseState` / `PurchaseRecord`
- Navigation destinations (`NutriLogDestination`)

## UI State
No new persistent state models required. UI state remains in existing ViewModels/composables.

## Styling Tokens (conceptual)
If introduced, these would be Compose-only helpers (no serialization/migration):
- `Dimens` / spacing scale (e.g., xs/sm/md/lg/xl) — optional, can use `16.dp`, `8.dp` etc directly
- `Elevation` scale — prefer `CardDefaults.elevatedCardElevation()`

## Validation
- No schema migrations
- No data contracts changed
- All existing data flows unchanged
