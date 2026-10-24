# Feature Specification: iOS App Functional Parity with Android

**Feature Branch**: `009-ios-parity`

**Created**: 2026-10-24

**Status**: Draft

**Input**: User description: "Bring the native SwiftUI iOS app to functional parity with the current Android app"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Track today's diary on iOS (Priority: P1)

As an iOS user, I can open the app and see today's diary with calorie and macro totals, meal sections, entries, and the weekly summary card, matching Android behavior.

**Why this priority**: The diary is the primary daily workflow and the core value of the app.

**Independent Test**: Launch the iOS app and verify the Today screen renders the same diary state, meal sections, totals, empty states, and add-food entry points as Android.

**Acceptance Scenarios**:

1. **Given** the app has diary entries, **When** I open Today, **Then** I see the date, calorie/macro totals, meal sections, entry rows, and weekly card.
2. **Given** a day with no entries, **When** I open Today, **Then** I see empty meal states and the add-food affordance.
3. **Given** an entry in a meal, **When** I remove it, **Then** totals and the meal section update.

### User Story 2 - Add food through search, library, and barcode (Priority: P1)

As an iOS user, I can add food to a meal through the same two-step flow as Android, including catalogue search, recent searches, library selection, barcode scanning, quantity, meal selection, and confirmation.

**Why this priority**: Logging food is the main user action and must work identically across platforms.

**Independent Test**: Open add food from Today or Library and complete search/library/scan flows through confirmation.

**Acceptance Scenarios**:

1. **Given** the add-food flow, **When** I choose Search and submit a query, **Then** results and recent searches behave as on Android.
2. **Given** a saved food, **When** I choose Library, **Then** I can select it and continue to quantity.
3. **Given** a scanned code, **When** resolution succeeds, **Then** the quantity step opens with that product.
4. **Given** an unknown barcode, **When** resolution fails, **Then** the flow falls back to search by code.
5. **Given** product, meal, and quantity, **When** I confirm, **Then** the entry is logged and I return to Today.

### User Story 3 - Browse library and product details (Priority: P2)

As an iOS user, I can browse saved foods, open product details, save/remove products, and add a saved food to the diary.

**Why this priority**: The library is a core navigation destination and supports reuse of foods.

**Independent Test**: Save a product, open Library and Product Detail, remove/save, and add to diary.

**Acceptance Scenarios**:

1. **Given** saved products, **When** I open Library, **Then** they appear most recently saved first.
2. **Given** a product detail route, **When** I open it, **Then** its nutrition and saved state render and the view model is route-scoped.
3. **Given** a product, **When** I toggle saved, **Then** Library reflects the change.

### User Story 4 - Weekly summary, goals, premium, and coach (Priority: P2)

As an iOS user, I can view the weekly summary, edit goals, inspect the premium unlock, restore purchases, and use the coach when entitled or in debug builds.

**Why this priority**: These features complete the app shell and premium/assistant behavior.

**Independent Test**: Navigate More and open Weekly/Coach; verify goal updates, premium state, restore, and coach availability match Android.

**Acceptance Scenarios**:

1. **Given** seven days of entries, **When** I open Weekly, **Then** averages and empty-day distinctions match Android.
2. **Given** a goal change, **When** I save it, **Then** Today reflects the new goal.
3. **Given** the premium product is unavailable, **When** I open More, **Then** restore and unavailable messaging match Android.
4. **Given** a debug or unlocked build, **When** I open Coach, **Then** the assistant flows and gating match Android.

### Edge Cases

- What happens when camera permission is denied or unavailable on simulator?
- How does the app handle an AI gateway that is not configured?
- What happens when the premium product is not offered by the store?
- How does navigation behave for nested add-food and product routes?
- What happens when a product is only in the saved library and not in the catalogue cache?

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: iOS MUST use the shared Kotlin runtime as the single source of truth for diary, search, saved products, purchases, goals, summary, assistant, and barcode resolution.
- **FR-002**: iOS MUST expose all required shared state through closure-based subscriptions compatible with Kotlin/Native.
- **FR-003**: iOS MUST provide Today, Library, and More tabs with the same top-level destinations as Android.
- **FR-004**: iOS MUST implement the add-food flow as two route steps with session reset semantics matching Android.
- **FR-005**: iOS MUST implement barcode scanning with AVFoundation and the shared BarcodeResolver, including resolved, not-in-cache, and unknown outcomes.
- **FR-006**: iOS MUST implement route-scoped product detail view models.
- **FR-007**: iOS MUST implement weekly summary, goal editing, premium purchase/restore, and coach modal gating.
- **FR-008**: iOS MUST pass the AI gateway base URL from build configuration to the shared runtime.
- **FR-009**: iOS MUST keep StoreKit in Swift and inject it as the shared PurchaseClient.
- **FR-010**: iOS MUST update StoreKit mapping for the single premium product code `nutrilog_pro`.
- **FR-011**: iOS MUST not duplicate business logic in Swift.
- **FR-012**: iOS MUST preserve existing local data by using the same runtime and database.

### Key Entities

- **IosAppBridge**: Owns the shared runtime and all feature view models, exposing state and actions to Swift.
- **ProductDetailHandle**: Route-scoped handle for one product detail.
- **NutriLogTab/Route**: Native navigation model mirroring shared route constants.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: iOS builds successfully with `xcodebuild` and shared framework linking.
- **SC-002**: Swift source passes `swift format lint --strict`.
- **SC-003**: iOS screens cover the same functional flows as Android's Today, Library, More, Add Food, Product Detail, Weekly, Coach, and Premium surfaces.
- **SC-004**: No business logic is duplicated in Swift beyond presentation and navigation.
- **SC-005**: Shared Kotlin tests and architecture checks remain green.

## Assumptions

- Native SwiftUI remains the iOS UI technology; no Compose migration is performed.
- The existing Android behavior is the parity oracle.
- Premium product IDs may require App Store configuration outside the repository.
- AI gateway configuration may be empty in local builds, disabling the coach.