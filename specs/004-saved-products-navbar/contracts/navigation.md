# Navigation Contract

**Status**: Design contract. Versions verified on 2026-10-20.
**Plan**: [../plan.md](../plan.md) | **ADR**: [ADR-0015](../../../docs/adr/0015-compose-multiplatform-navigation.md)

## Purpose

Define the destinations the bottom navigation bar exposes and the routing rules
for the app shell, so the bar's behaviour (FR-005, FR-007, FR-008) is fixed
before implementation.

## Destinations

| Destination | Route | Content | Notes |
|---|---|---|---|
| Search | `search` | existing product-search screen | default/start destination |
| Saved products | `saved` | saved-products list | new |
| Diary | `diary` | "not yet available" placeholder | FR-006; real diary is a later feature |

## Routing rules

1. The bar shows exactly these three destinations and always indicates the
   current one (FR-005).
2. Selecting a destination switches to it and updates the indicator; repeated
   selection of the current destination does not push a duplicate entry onto the
   stack.
3. A product-detail view is opened **within** the destination that opened it, so
   returning from it lands back on that destination (FR-007).
4. The bar is part of the app shell (`Scaffold`), which consumes system-bar and
   navigation insets so the bar never obscures content or the system navigation
   area, including gesture navigation (FR-008).
5. Each destination keeps its own back stack; switching destinations does not
   destroy unsaved input in a way that is not explicitly modelled.
6. The Saved products destination's empty-state shortcut navigates to the Search
   destination through the same shell routing the bar uses (FR-002), so the bar
   indicator and back stack stay consistent with a normal destination switch.

## Accessibility

- Each destination has an accessible name and a **48 dp minimum** touch target
  (FR-017).
- The current destination is conveyed by more than colour alone (icon + label +
  selected state) so it remains understandable with accessibility services and at
  the largest supported system text size.

## Dependency

Backed by JetBrains `navigation-compose` **2.9.2** (stable, Compose Multiplatform
line). The bar composable is project-owned (`androidApp/ui/NutriLogBottomBar.kt`)
so the dependency is confined to the UI layer and can be swapped without touching
shared code. AndroidX `navigation-compose` 2.10.2 was rejected because it would
introduce a second Compose version line (see ADR-0015).
