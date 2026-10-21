# Implementation Plan: Modern UI Redesign

**Feature**: `005-ui-redesign`  
**Branch**: `005-ui-redesign`  
**Date**: 2026-10-21  
**Spec**: [spec.md](spec.md)  
**Research**: [research.md](research.md)

## Summary
Redesign the app UI to look modern and professional using icons where appropriate, improved spacing/typography/hierarchy, cleaner cards/lists, and polished empty states — while keeping all existing functionality identical. Primarily Compose UI changes in `androidApp` with possible small tweaks to shared UI and review of iOS for consistency.

## Technical Context
- **Language/Framework**: Kotlin (KMP), Jetpack Compose (Android), SwiftUI (iOS)
- **UI**: Material 3
- **Navigation**: Navigation 3/Compose Navigation (existing)
- **Images**: Coil
- **Icons**: Material Icons Extended/Core (already present)
- **Testing**: JUnit, Compose UI tests, Android instrumented tests

## Project Structure (relevant)
```
androidApp/src/main/kotlin/com/example/nutricionalregister/
├── App.kt, AppShell.kt
├── ui/ (NutriLogBottomBar.kt, NutriLogDestination.kt)
├── search/ (ProductSearchScreen.kt, ProductDetailScreen.kt, ...)
├── savedproducts/ (SavedProductsScreen.kt, PurchaseSection.kt, ...)
└── diary/ (DiaryPlaceholderScreen.kt)
```

## Approach

### Phase 0: Research (done)
See [research.md](research.md). Leverage Material 3, existing theme, Coil images, Material icons.

### Phase 1: Design System Alignment
- **Define/confirm tokens**: Use `MaterialTheme.colorScheme`, `typography`, `shapes`. Optionally add a small `ui/theme/Dimens.kt` for consistent spacing if helpful.
- **Icon strategy**: Prefer filled/outlined variants, use `AutoMirrored` icons, ensure `contentDescription` on icon-only buttons.
- **Card treatment**: Standardize on `ElevatedCard` or `Card` with consistent padding (e.g., 16dp), shape, elevation.
- **Button treatment**: Use `IconButton`, `FilledTonalButton`, `TextButton` appropriately.

### Phase 2: Screen Redesigns

*Applying clarifications: refined Material 3, icons+labels always visible in bottom nav, balanced icon/text buttons, subtle elevated cards, minimal motion, icon+heading+body+CTA empty states.*

#### Bottom Navigation
- **File**: `androidApp/src/main/kotlin/com/example/nutricionalregister/ui/NutriLogBottomBar.kt`, `NutriLogDestination.kt`
- **Changes**: Refine icon choices/state indication, keep labels always shown, accessibility preserved. Use `NavigationBarItem` with clearer selected/unselected treatment.
- **Goal**: Clean, modern, professional look.

#### Search Screen
- **File**: `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductSearchScreen.kt`
- **Changes**: Polished `OutlinedTextField`/search bar, result cards as `ElevatedCard` with consistent spacing (16dp padding), icon-based save/unsave action via `IconButton` where appropriate, better empty/loading states.
- **Preserve**: Search logic, debounce, results.

#### Product Detail Screen
- **File**: `androidApp/src/main/kotlin/com/example/nutricionalregister/search/ProductDetailScreen.kt`
- **Changes**: Better hierarchy (header, image, key facts), grouped nutrition with clearer spacing, refined purchase section styling, balanced icon/text actions.
- **Preserve**: All data display, purchase flow.

#### Saved Products Screen
- **File**: `androidApp/src/main/kotlin/com/example/nutricionalregister/savedproducts/SavedProductsScreen.kt`
- **Changes**: Card-based list using `ElevatedCard`, polished empty state with icon + heading + body + CTA to Search, consistent styling.
- **Preserve**: Remove/save/purchase functionality.

#### Purchase Section
- **File**: `androidApp/src/main/kotlin/com/example/nutricionalregister/savedproducts/PurchaseSection.kt`
- **Changes**: Modern button styling with clear states (enabled/disabled/pending), icon only if meaningfully helpful, keep labels on primary actions.
- **Preserve**: All purchase logic/states.

#### Diary Screen
- **File**: `androidApp/src/main/kotlin/com/example/nutricionalregister/diary/DiaryPlaceholderScreen.kt`
- **Changes**: Polished empty/placeholder state with icon + heading + body (or CTA if appropriate). Intentional, professional.

### Phase 3: Testing & Validation
- **Unit tests**: Run `./gradlew test` (relevant modules) — should pass with no logic changes.
- **Android instrumented**: Run `./gradlew :androidApp:connectedDebugAndroidTest` — navigation/saved/purchase tests must pass.
- **UI checks**: Verify light/dark mode, accessibility (content descriptions), touch targets.
- **Manual review**: Compare before/after feel.

## File Changes (expected)
| File | Type | Notes |
|---|---|---|
| `androidApp/src/main/kotlin/.../ui/NutriLogBottomBar.kt` | Modify | Icon/state styling |
| `androidApp/src/main/kotlin/.../ui/NutriLogDestination.kt` | Maybe modify | Icon mapping if changed |
| `androidApp/src/main/kotlin/.../search/ProductSearchScreen.kt` | Modify | Cards, search bar, icons |
| `androidApp/src/main/kotlin/.../search/ProductDetailScreen.kt` | Modify | Layout/hierarchy/icons |
| `androidApp/src/main/kotlin/.../savedproducts/SavedProductsScreen.kt` | Modify | Cards, empty state |
| `androidApp/src/main/kotlin/.../savedproducts/PurchaseSection.kt` | Modify | Button styling/states |
| `androidApp/src/main/kotlin/.../diary/DiaryPlaceholderScreen.kt` | Modify | Polished placeholder |
| `androidApp/src/main/kotlin/.../ui/theme/Dimens.kt` (new?) | Maybe create | Optional spacing tokens |
| `iosApp/iosApp/ContentView.swift` | Maybe review | Consistency check only if easy |

## Risks & Mitigations
| Risk | Mitigation |
|---|---|
| Breaking existing tests | Make minimal behavioral changes; only styling/layout. Run tests frequently. |
| Accessibility regression | Keep labels, add content descriptions, maintain touch targets. |
| Inconsistent design | Use MaterialTheme tokens consistently across all screens. |
| iOS drift | Focus on Android first; review SwiftUI surfaces for obvious inconsistencies. |

## Definition of Done
- [ ] All screens redesigned with modern, professional look using icons appropriately
- [ ] Bottom navigation polished with clear icon states
- [ ] Search, detail, saved, diary all consistent
- [ ] All existing tests pass
- [ ] Light and dark mode look good
- [ ] Accessibility maintained/improved
- [ ] No functionality regressions
- [ ] Spec/plan/tasks complete per spec-kit flow
