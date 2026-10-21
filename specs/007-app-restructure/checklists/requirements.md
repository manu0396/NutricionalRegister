# Requirements Checklist: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21

## Functional Requirements

- [ ] FR-001: The app opens on a Today screen with the date, the daily totals against goals and the four meal sections
- [ ] FR-002: A product can be logged with a quantity in grams, a meal and a timestamp, snapshotting the nutrient values
- [ ] FR-003: Calories, protein, carbohydrates and fat are computed per entry, per meal and per day
- [ ] FR-004: An entry can be added from the Today screen, from a meal section and from a product detail
- [ ] FR-005: The add-food flow selects the meal, the food and the quantity, and confirms
- [ ] FR-006: The add-food flow shows the calories and macros the quantity produces before confirming
- [ ] FR-007: The Library lists the saved foods, each with an action that starts the add-food flow pre-selected
- [ ] FR-008: A food can be removed from the library and the removal survives a restart
- [ ] FR-009: The bottom navigation exposes exactly four destinations, each with an icon and a visible label
- [ ] FR-010: Selecting a destination always shows that destination's root screen
- [ ] FR-011: The product detail is a scoped navigation step, discarded when the tab is left
- [ ] FR-012: The Search destination keeps the existing search behaviour, recent searches, offline cache and attribution
- [ ] FR-013: The product detail offers an "Add to diary" action
- [ ] FR-014: The weekly summary shows the last seven days, the per-day totals and the average over days with entries
- [ ] FR-015: The daily goals are editable and are used in the daily summary
- [ ] FR-016: The in-app purchase flow is reachable from More with its existing states
- [ ] FR-017: The app version and the Open Food Facts attribution are shown in More
- [ ] FR-018: The diary data is stored on the device and survives a restart
- [ ] FR-019: The schema migration from version 1 keeps the saved products and the purchase records
- [ ] FR-020: Every screen uses one shared colour, typography, shape and spacing system
- [ ] FR-021: Icon-only controls have content descriptions and a 48 dp touch target
- [ ] FR-022: The app is fully usable and legible in light and dark themes

## Non-Functional Requirements

- [ ] NFR-001: `core:domain` and `core:data` keep at least 80 % line coverage
- [ ] NFR-002: No new third-party dependency without a new ADR
- [ ] NFR-003: The module dependency policy and the architecture tests are not weakened
- [ ] NFR-004: The Today screen renders from on-device data with no network call
- [ ] NFR-005: All business logic stays in the shared modules; the UI computes no nutrition values
- [ ] NFR-006: Search, save, purchase and recent-search behaviour do not regress
- [ ] NFR-007: The iOS target keeps compiling and the shared modules stay cross-platform

## Success Criteria

- [ ] SC-001: The app opens on the Today screen
- [ ] SC-002: A food can be logged from Today in three or fewer interactions
- [ ] SC-003: The reported "search shows a product detail" defect cannot be reproduced
- [ ] SC-004: Every screen uses the shared design system and reads as a product
- [ ] SC-005: All unit tests, architecture tests, coverage, lint, ktlint and the instrumented suite pass
- [ ] SC-006: The diary data survives a restart and the totals recompute
- [ ] SC-007: The coverage gate stays green

## Testing

- [ ] Domain unit tests cover the new entities
- [ ] Data-layer unit tests cover the new repositories and the migration
- [ ] Feature unit tests cover the diary view model and the summary calculator
- [ ] Instrumented tests cover the new screens
- [ ] Instrumented tests cover the corrected navigation, including the regression test
- [ ] Light and dark mode reviewed
- [ ] Accessibility reviewed
