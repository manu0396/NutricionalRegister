# Requirements Checklist: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21

Each item mirrors a requirement in [spec.md](../spec.md). A requirement is only
complete when the behaviour it describes is implemented and covered by a test.

## Functional Requirements

- [ ] FR-001: The app opens on a Today screen with the date, the daily totals against goals and the four meal sections
- [ ] FR-002: A product can be logged with a quantity in grams, a meal and a timestamp, snapshotting the nutrient values
- [ ] FR-003: Calories, protein, carbohydrates and fat are computed per entry, per meal and per day, and an unreported nutrient stays unknown rather than becoming zero
- [ ] FR-004: An entry can be added from the Today screen, from a meal section, from a product detail, from the Library, from a barcode scan and from the Coach
- [ ] FR-005: The add-food flow selects the food, sets a quantity, chooses the meal and confirms
- [ ] FR-006: The add-food flow shows the calories and macros the quantity produces before confirming
- [ ] FR-007: The Library lists the saved foods, each with an action that starts the add-food flow pre-selected
- [ ] FR-008: A food can be removed from the library and the removal survives a restart
- [ ] FR-009: The bottom navigation exposes exactly three destinations - Today, Library and More - each with an icon and a visible label, plus a floating action button
- [ ] FR-010: Selecting a destination always shows that destination's root screen
- [ ] FR-011: The product detail is a navigation route scoped to the screen that opened it, discarded when the tab is left, and back returns to the opener
- [ ] FR-012: The catalogue search keeps its existing behaviour, recent searches, offline cache and attribution, and is reached through the add-food flow and from the Library empty state
- [ ] FR-013: The product detail offers an "Add to diary" action
- [ ] FR-014: Barcode scanning requests the camera permission at runtime, offers manual entry when refused, decodes on device, resolves the product through the catalogue repository and states honestly when the barcode is not cached
- [ ] FR-015: The weekly summary shows the last seven days, the per-day totals and the average over days with entries
- [ ] FR-016: The daily goals are editable from More and are used in the daily summary
- [ ] FR-017: The in-app purchase flow is reachable from More with its existing states, is a single premium unlock and is not offered on individual catalogue products
- [ ] FR-018: The app version and the Open Food Facts attribution are shown in More
- [ ] FR-019: The diary data is stored on the device and survives a restart
- [ ] FR-020: The schema migration from version 2 keeps the saved products and the purchase records
- [ ] FR-021: Every screen uses one shared colour, typography, shape and spacing system, and every screen is rebuilt on it
- [ ] FR-022: Icon-only controls have content descriptions and a 48 dp touch target
- [ ] FR-023: The app is fully usable and legible in light and dark themes
- [ ] FR-024: The Coach screen lets the user add, remove and change logged foods conversationally, and every proposed action is validated in shared code and applied only after the user confirms it
- [ ] FR-025: With no AI service configured, the assistant is explicitly disabled with a clear "not configured" state and no dead input field
- [ ] FR-026: The app discloses, before the first assistant message and in More, that assistant messages are sent to a third-party AI service
- [ ] FR-027: The assistant states plainly when it has no connectivity
- [ ] FR-028: The catalogue, the recent search terms and the purchase records are never sent to the AI service
- [ ] FR-029: The premium unlock grants the assistant, and a release build requires a verified entitlement
- [ ] FR-030: The meal picker is a modal sheet and creates no navigation entry
- [ ] FR-031: The bottom navigation bar is hidden on every child route, and its visibility is derived from the route hierarchy
- [ ] FR-032: No reference to a per-food purchase survives, and no purchase action is offered on a catalogue product

## Non-Functional Requirements

- [ ] NFR-001: `core:domain` and `core:data` keep at least 80 % line coverage
- [ ] NFR-002: No new third-party dependency without a new ADR
- [ ] NFR-003: The module dependency policy and the architecture tests are not weakened
- [ ] NFR-004: The Today screen renders from on-device data with no network call
- [ ] NFR-005: All business logic stays in the shared modules; the UI computes no nutrition values
- [ ] NFR-006: Search, save, purchase and recent-search behaviour do not regress
- [ ] NFR-007: The iOS target keeps compiling and the shared modules stay cross-platform with no Android-only API
- [ ] NFR-008: The architecture test matches the real package roots, so the inward-dependency rule is enforced rather than vacuously satisfied
- [ ] NFR-009: The AI service address is build configuration, and no secret is committed to the repository
- [ ] NFR-010: No telemetry is emitted, and the only egress beyond the catalogue search is the consented assistant path
- [ ] NFR-011: The scanner dependencies stay in `androidApp`, so the iOS framework is unaffected
- [ ] NFR-012: The release APK and the iOS framework sizes are measured after the restructure and compared against ADR-0014 and ADR-0015

## Success Criteria

- [ ] SC-001: The app opens on the Today screen
- [ ] SC-002: A food can be logged from Today in three or fewer interactions
- [ ] SC-003: The reported "search shows a product detail" defect cannot be reproduced
- [ ] SC-004: Every screen uses the shared design system and reads as a product, with no screen left on the pre-restructure visuals
- [ ] SC-005: All unit tests, architecture tests, coverage, lint, ktlint and the instrumented suite pass
- [ ] SC-006: The diary data survives a restart and the totals recompute
- [ ] SC-007: The coverage gate stays green
- [ ] SC-008: A scanned barcode leads to a confirmed, logged entry in one scan plus one confirmation
- [ ] SC-009: An assistant-logged entry is identical to the one the manual add flow produces for the same food, quantity and meal
- [ ] SC-010: There is exactly one premium unlock, reachable from More, and no purchase affordance on any catalogue product
- [ ] SC-011: The iOS target still compiles and the scanner dependencies do not appear in the shared framework

## Testing

- [ ] Domain unit tests cover the diary, the navigation model and the assistant domain
- [ ] Data-layer unit tests cover the new repositories, the gateway client and the migration chain
- [ ] Feature unit tests cover the diary view model, the summary calculator, the assistant view model and the barcode resolver
- [ ] Instrumented tests cover the new screens
- [ ] Instrumented tests cover the corrected navigation, including the regression test, the bar visibility and the back behaviour
- [ ] The scanner is exercised against a fake decoder, so no test needs a camera
- [ ] The assistant payload is asserted to carry no catalogue, search or purchase data
- [ ] Light and dark mode reviewed
- [ ] Accessibility reviewed
