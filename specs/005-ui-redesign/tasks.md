# Tasks: Modern UI Redesign

**Feature**: `005-ui-redesign`  
**Branch**: `005-ui-redesign`  
**Spec**: [spec.md](spec.md)  
**Plan**: [plan.md](plan.md)

## Phase 1: Setup & Design System
- [ ] **T001** Create spacing/dimens tokens (optional) in `androidApp/src/main/kotlin/.../ui/theme/Dimens.kt`
- [ ] **T002** Review current Material 3 theme and identify needed adjustments for a more polished look
- [ ] **T003** Define icon strategy and mapping for all screens

## Phase 2: Bottom Navigation Redesign
- [ ] **T004** Redesign `NutriLogBottomBar.kt` with modern icon treatment and clearer states
- [ ] **T005** Review/update `NutriLogDestination.kt` icon mappings if needed
- [ ] **T006** Verify navigation behavior unchanged (run related tests)

## Phase 3: Search Screen Redesign
- [ ] **T007** Polish `ProductSearchScreen.kt` - search bar, layout, spacing
- [ ] **T008** Redesign product result cards with better hierarchy and icon actions
- [ ] **T009** Improve loading/empty/error states with icons
- [ ] **T010** Verify search functionality unchanged

## Phase 4: Product Detail Screen Redesign
- [ ] **T011** Redesign `ProductDetailScreen.kt` with better hierarchy and spacing
- [ ] **T012** Polish nutrition section layout
- [ ] **T013** Refine save/purchase action buttons with icon usage
- [ ] **T014** Verify detail and purchase flow unchanged

## Phase 5: Saved Products & Diary Redesign
- [ ] **T015** Redesign `SavedProductsScreen.kt` with card-based layout
- [ ] **T016** Polish `SavedProductsScreen` empty state with icon + CTA
- [ ] **T017** Redesign `PurchaseSection.kt` for cleaner purchase UI
- [ ] **T018** Polish `DiaryPlaceholderScreen.kt` for professional look
- [ ] **T019** Verify saved/purchase/diary flows unchanged

## Phase 6: Polish & Validation
- [ ] **T020** Review dark mode appearance across all screens
- [ ] **T021** Accessibility audit (content descriptions, touch targets, contrast)
- [ ] **T022** Run all unit tests - fix any issues
- [ ] **T023** Run Android instrumented tests - verify all pass
- [ ] **T024** Manual review against success criteria
- [ ] **T025** Update spec status to Implemented, document changes
