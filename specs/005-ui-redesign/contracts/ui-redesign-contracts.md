# Contracts: Modern UI Redesign

**Feature**: `005-ui-redesign`  
**Branch**: `005-ui-redesign`  
**Date**: 2026-10-21

## Scope
UI-only redesign. No API, GraphQL/gRPC, or platform service contracts change.

## Navigation Contract
Existing navigation contract (from 004) remains unchanged:
- Bottom nav exposes exactly 3 destinations: Search, Saved products, Diary
- Current destination indicated (not by color alone)
- Re-selecting current destination is a no-op
- Each destination maintains its own back stack
- Labels remain accessible

## Component Contracts (behavioral)
| Component | Contract | Notes |
|---|---|---|
| `NutriLogBottomBar` | Shows 3 items, selected state clear, >=48dp targets, labels accessible | Visual treatment may change; behavior must stay identical |
| `AppShell` | Navigation behavior unchanged | No changes to navigation logic |
| `ProductSearchScreen` | Search, results display, save action | Visual restyle only |
| `ProductDetailScreen` | Product info, nutrition, purchase entry, save | Visual restyle only |
| `SavedProductsScreen` | List saved products, empty state, navigate to search/remove | Visual restyle only |
| `DiaryPlaceholderScreen` | Placeholder displayed | Polished visual treatment only |

## Accessibility Contract
- Icon-only buttons MUST have `contentDescription`
- Touch targets >= 48dp
- Sufficient contrast in light/dark
- Screen reader navigation unchanged
- Current destination still identifiable

## Testing Contract
- All existing tests must continue to pass
- No test changes required purely for styling unless tests assert on specific visuals that must be preserved intentionally
- Instrumented tests for navigation must still pass
