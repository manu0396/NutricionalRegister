# Tasks: iOS App Functional Parity with Android

**Input**: Design documents from `/specs/009-ios-parity/`

**Prerequisites**: plan.md, spec.md

## Phase 1: Setup

- [ ] T001 Create feature branch and spec artifacts for `009-ios-parity`.
- [ ] T002 [P] Confirm native iOS toolchain and existing build baseline.

## Phase 2: Foundational

- [ ] T003 Create `shared/src/iosMain/.../IosAppBridge.kt` owning runtime and all view models.
- [ ] T004 [P] Add camera permission to `iosApp/iosApp/Info.plist`.
- [ ] T005 [P] Create `iosApp/iosApp/BarcodeScannerView.swift`.
- [ ] T006 Validate shared iOS compilation and framework linking.

## Phase 3: User Story 1 - Today Diary (P1)

- [ ] T007 [US1] Create `AppRoot.swift` composition root.
- [ ] T008 [US1] Create `NutriLogApp.swift` app entry.
- [ ] T009 [US1] Replace `ContentView.swift` with tab/navigation shell.
- [ ] T010 [US1] Create `TodayView.swift` and `TodayViewModel.swift`.
- [ ] T011 [US1] Add meal sections, entry rows, totals, weekly card, and remove action.
- [ ] T012 [US1] Add add-food FAB and coach FAB.

## Phase 4: User Story 2 - Add Food (P1)

- [ ] T013 [US2] Create `AddFoodFlowViewModel.swift`.
- [ ] T014 [US2] Create `AddFoodSourceView.swift`.
- [ ] T015 [US2] Create `AddFoodQuantityView.swift`.
- [ ] T016 [US2] Create `MealPickerSheet.swift`.
- [ ] T017 [US2] Integrate barcode scan resolution logic.
- [ ] T018 [US2] Wire navigation and session IDs.

## Phase 5: User Story 3 - Library and Product Detail (P2)

- [ ] T019 [US3] Create `LibraryView.swift` and `LibraryViewModel.swift`.
- [ ] T020 [US3] Create `ProductDetailView.swift` and `ProductDetailViewModel.swift`.
- [ ] T021 [US3] Implement save/remove and add-to-diary actions.

## Phase 6: User Story 4 - Weekly, More, Coach, Premium (P2)

- [ ] T022 [US4] Create `WeeklyView.swift` and `WeeklyViewModel.swift`.
- [ ] T023 [US4] Create `MoreView.swift` and `MoreViewModel.swift`.
- [ ] T024 [US4] Create `CoachView.swift` and `CoachViewModel.swift`.
- [ ] T025 [US4] Create `PremiumPurchaseSection.swift`.
- [ ] T026 [US4] Implement goal editing and coach gating.

## Phase 7: Polish and Validation

- [ ] T027 Configure AI gateway URL in build config.
- [ ] T028 Verify StoreKit premium mapping.
- [ ] T029 Run shared tests, Xcode build, Swift format lint.
- [ ] T030 Perform manual parity validation.
- [ ] T031 Update README and AI-LOG.