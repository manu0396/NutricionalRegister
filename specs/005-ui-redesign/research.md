# Research: Modern UI Redesign

**Feature**: `005-ui-redesign`  
**Branch**: `005-ui-redesign`  
**Date**: 2026-10-21

## Goals
Make the app look distinctly more professional and modern using icons where appropriate, better spacing/typography/hierarchy, and a cohesive design language across all screens without regressing existing functionality.

## Current State
- **UI stack**: Jetpack Compose + Material 3 on Android, SwiftUI on iOS (shared KMP/domain/data + feature modules)
- **Navigation**: `AppShell` + `NutriLogBottomBar` using `NavigationBar`/`NavigationBarItem` with Material icons and labels
- **Screens**: `ProductSearchScreen`, `ProductDetailScreen`, `SavedProductsScreen`, `PurchaseSection`, `DiaryPlaceholderScreen`
- **Theming**: Material 3 theming present (`MaterialTheme.colorScheme`, `typography`, `shapes`). Dark/light both supported by the system.
- **Images**: Coil used for product images (added in 003). Placeholders exist.
- **Icons**: `androidx.compose.material:material-icons-core` in use

## Modern UI Direction (Material 3)
| Area | Current | Target | Notes |
|---|---|---|---|
| **Bottom Navigation** | `NavigationBar` with `Filled/Search`, `List`, `DateRange` | Cleaner icon set, clearer active/inactive treatment, keep labels accessible, ensure >=48dp targets | Don't remove labels; indicate state via selected icon/weight/alpha, not color alone. |
| **Cards** | Varies by screen | Unified `ElevatedCard`/`Card` with consistent `shape`, `padding`, `elevation`, content hierarchy | Use `MaterialTheme.shapes` and `spacing` tokens. |
| **Search Bar** | Functional | Polished `SearchBar`/`OutlinedTextField` with leading icon, clearer affordance | Keep behavior identical. |
| **Product Cards (results)** | Image + text rows | Card with image thumbnail, strong typographic hierarchy, icon-based actions where sensible | Preserve Coil loading/placeholder behavior from 003. |
| **Detail Screen** | Informational | Clear sections, better grouping, icon actions, refined nutrition presentation | Don't change data model. |
| **Saved Products** | List-based | Card list + polished empty state with icon + CTA to Search | Consistent with search cards. |
| **Diary** | Placeholder | Intentional, polished empty/placeholder state | Avoid "game-y" look. |
| **Actions** | Mix of text/icon | Prefer icon buttons for secondary actions, filled/tonal for primary CTAs with clear states | Must keep content descriptions. |

## Iconography Strategy
- **Source**: Material Icons (already in dependencies). Prefer `Icons.AutoMirrored.*` where directionally appropriate.
- **Usage**: Navigation, secondary actions (save/unsave, back/share-like actions), empty states.
- **States**: selected/outlined/filled variants where meaningful.
- **Accessibility**: `contentDescription` for icon-only buttons; decorative icons get `null` with labeled context.

## Theming & Visual Tokens
- Leverage existing `MaterialTheme` (colorScheme, typography, shapes) before introducing custom tokens.
- Ensure sufficient contrast in light and dark.
- Use consistent spacing (4dp grid) already implied by Material.
- Avoid heavy drop shadows/glow; prefer subtle elevation and shape.

## Constraints
- **No functionality regression**: navigation, save/purchase flows, tests must keep passing.
- **KMP boundaries**: UI changes are primarily in `androidApp` + shared UI where applicable; don't move business logic.
- **iOS**: SwiftUI will need review for consistency but this spec focuses on a unified, professional look; changes should be reasonable or tracked.
- **Tests**: Existing Android instrumented/unit tests must still pass.
- **Constitution**: Plain-text, no emoji (Principle V).

## Open Questions (to resolve in plan)
- Should we introduce a small `ui/theme/` extension for spacing/elevation tokens or just use `MaterialTheme` + `dp`?
- Which icon set/treatments best read as "professional" vs the current look?
- Keep bottom nav labels always visible or use a more modern treatment while keeping accessibility intact?

## References
- Material 3 Guidelines: Navigation bar, Cards, Buttons, Icon buttons
- Existing: `AppShell.kt`, `NutriLogBottomBar.kt`, `NutriLogDestination.kt`, 003 UI refresh artifacts
