# Feature Specification: Saved Products Bottom Navigation and In-App Purchase

**Feature Branch**: `004-saved-products-navbar`

**Created**: 2026-10-20

**Status**: Implemented

**Input**: User description: "Implement a bottom navigation bar that manages saved
products kept in the local database, and start with buying first with Google Pay."

## Clarifications

### Session 2026-10-20

- Q: What does "buying with Google Pay" mean for this nutrition app? → A: A real
  in-app purchase flow with genuine charges to the user, using the platform
  billing system.
- Q: Which destinations should the bottom navigation expose? → A: Search, Saved
  products, and a Diary destination (Diary shows a "not yet available" state
  until that feature exists).
- Q: Which platforms should the payment capability target? → A: Both Android and
  iOS, using each platform's native in-app purchase mechanism.
- Q: When the app restarts with an interrupted but incomplete purchase, what
  should it do? → A: Reconcile against the platform store and trust the store as
  the source of truth: if the store reports the product owned, mark it purchased;
  otherwise leave it unpaid.
- Q: What should the Saved products destination show when nothing is saved? →
  A: An explicit "no saved products yet" message together with an inline shortcut
  that takes the user to the Search destination.
- Q: What should be shown while a purchase is in progress? → A: A clear
  "purchase in progress" state that disables the purchase control until the
  store returns a result, so a second attempt cannot be started.
- Q: What should happen when the user is not signed in to, or the device is not
  configured for, the platform payment system? → A: Attempt the purchase, catch
  the store's unavailable error, and show an explicit "payments unavailable"
  state with guidance to configure the store account and a way to retry.
- Q: What stable identity decides two purchases are the same transaction? → A:
  The product code: a product can only ever be bought once, so the purchased
  state is keyed on the product code and a repeat confirmation is idempotent.
  The store's own transaction identity is still retained as metadata used to
  acknowledge or finish the store transaction, but it is not the local
  uniqueness key.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Save products and revisit them (Priority: P1)

As a person recording what I eat, I want to save a product I found while
searching so that I can find it again quickly without searching for it a second
time.

**Why this priority**: The bottom navigation's central purpose is to manage saved
products. Without saving and listing, the navigation bar has nothing meaningful
to manage, so this is the smallest slice that delivers the feature's core value.

**Independent Test**: Search for a product, save it, then open the Saved products
destination and verify the product appears there and remains after the app is
closed and reopened without a network connection.

**Acceptance Scenarios**:

1. **Given** a product is shown in search results or in the product detail view,
   **When** the user chooses to save it, **Then** the product is stored on the
   device and is marked as saved wherever it is shown afterwards.
2. **Given** at least one product has been saved, **When** the user opens the
   Saved products destination, **Then** all saved products are listed and each
   one can be opened to view its details.
3. **Given** products have been saved, **When** the app is closed and reopened
   without a network connection, **Then** the saved products are still listed.
4. **Given** a product is already saved, **When** the user saves it again,
   **Then** it is not duplicated in the list.
5. **Given** a product is saved, **When** the user removes it from the saved
   list, **Then** it no longer appears there and is not re-added by a later
   search.
6. **Given** no products have been saved, **When** the user opens the Saved
   products destination, **Then** an explicit "no saved products yet" message is
   shown together with a shortcut that takes the user to the Search destination.

---

### User Story 2 - Move between the app's main areas (Priority: P1)

As a person using the app, I want a persistent bar at the bottom of the screen
that lets me move between searching, my saved products, and my diary, so that I
always know where I am and how to get to the other areas.

**Why this priority**: The navigation bar is the user-visible feature requested.
It must exist for Saved products to be reachable, and it defines the app's
overall shape.

**Independent Test**: From the search screen, use the bar to reach Saved products
and Diary and back; verify the current destination is visibly indicated and that
each destination's content is correct.

**Acceptance Scenarios**:

1. **Given** the app is open, **When** the user views any main destination,
   **Then** a bottom navigation bar is visible showing Search, Saved products,
   and Diary, with the current destination visibly distinguished.
2. **Given** the user is on Search, **When** they select Saved products in the
   bar, **Then** the Saved products destination is shown and the bar indicates it
   is current.
3. **Given** the user is on a product detail view opened from a destination,
   **When** they return, **Then** they arrive back at the destination they came
   from rather than an unrelated one.
4. **Given** the Diary feature is not yet available, **When** the user selects
   Diary, **Then** a clear "not yet available" state is shown instead of an empty
   or broken screen.
5. **Given** the user changes the device's text size to the largest supported
   size, **When** they use the bottom bar, **Then** every destination label
   remains readable and every destination remains selectable.

---

### User Story 3 - Buy a product through the platform payment system (Priority: P2)

As a person using the app, I want to buy a product through the platform's own
payment sheet, so that paying is familiar and I do not have to enter card
details into this app.

**Why this priority**: Purchase is explicitly requested but depends on saved
products and the navigation shell being in place, and it carries the most
external and policy risk, so it is sequenced after the P1 slices.

**Independent Test**: With a test payment account configured, start a purchase
for a product that is eligible for purchase, complete the platform payment sheet,
and verify the app shows a confirmed purchase and that the product's purchased
state persists after restarting the app.

**Acceptance Scenarios**:

1. **Given** a product is eligible for purchase, **When** the user starts a
   purchase, **Then** the platform's own payment sheet is presented and the app
   collects no card or bank details itself.
2. **Given** the payment sheet is presented, **When** the user completes payment
   successfully, **Then** the app confirms the purchase and marks the product as
   purchased.
3. **Given** the payment sheet is presented, **When** the user cancels it,
   **Then** no purchase is recorded and the product is unchanged.
4. **Given** a payment attempt fails or is declined, **When** the user returns to
   the app, **Then** a clear failure message is shown and the user can retry or
   leave without being charged.
5. **Given** a successful purchase, **When** the app is restarted or the device
   is offline, **Then** the purchased state is still shown, and a purchase is
   never recorded twice for the same completed transaction.
6. **Given** a user on either Android or iOS, **When** they buy a product,
   **Then** they receive that platform's native payment experience.
7. **Given** the user has confirmed payment, **When** the store has not yet
   returned a result, **Then** a clear "purchase in progress" state is shown and
   the purchase control is disabled so a second attempt cannot be started.
8. **Given** the user is not signed in to, or the device is not configured for,
   the platform payment system, **When** they attempt a purchase, **Then** an
   explicit "payments unavailable" state is shown with guidance to configure the
   store account and a way to retry.

### Edge Cases

- A saved product whose underlying catalogue entry has since disappeared still
  appears in the saved list with its stored details, and does not crash the list.
- A saved product with no image, no brand, or no nutrition information is
  displayed gracefully rather than as a blank or broken entry.
- Saving or removing a product while offline works without a network connection
  and is not lost when connectivity returns.
- A purchase that is interrupted (app closed, connection lost) is reconciled on
  next launch against the platform store, which is the source of truth: a
  product the store reports as owned is shown as purchased, and any product the
  store does not report is left unpaid.
- A product that is not eligible for purchase does not offer a purchase action.
- While a purchase is in progress the purchase control is disabled, so a second
  attempt for the same product cannot be started and the transaction cannot be
  recorded twice.
- The bottom bar must not obscure page content or the system navigation area,
  including devices with gesture navigation.
- Repeatedly selecting the already-current destination does not stack duplicated
  screens or reset that destination's unsaved input unexpectedly.
- A purchase attempt made while signed out of, or not configured for, the
  platform payment system shows an explicit "payments unavailable" state with
  guidance to configure the store account and a way to retry, instead of a
  silent failure.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: A user MUST be able to save a product from wherever it is shown,
  and saved products MUST be stored on the device so they remain available
  without a network connection.
- **FR-002**: A user MUST be able to view all saved products in a dedicated
  destination and open any one of them to inspect its details. When no products
  are saved, the destination MUST show an explicit "no saved products yet"
  message together with a shortcut to the Search destination.
- **FR-003**: A user MUST be able to remove a previously saved product, and a
  removed product MUST NOT reappear from a later search.
- **FR-004**: Saving the same product more than once MUST NOT create duplicate
  entries in the saved list.
- **FR-005**: The app MUST present a persistent bottom navigation bar exposing
  Search, Saved products, and Diary, and MUST visibly indicate the current
  destination.
- **FR-006**: The Diary destination MUST show an explicit "not yet available"
  state while the diary feature does not exist, and MUST NOT present a broken or
  misleadingly empty screen.
- **FR-007**: Returning from a product detail view MUST return the user to the
  destination from which it was opened.
- **FR-008**: The bottom navigation bar MUST NOT obscure page content or the
  system navigation area, and MUST remain usable with gesture navigation and at
  the largest supported system text size.
- **FR-009**: A user MUST be able to start a purchase for a product that is
  eligible for purchase, and the app MUST present the platform's own payment
  sheet rather than collecting card or bank details itself.
- **FR-010**: The app MUST confirm a successful purchase to the user and MUST
  record the product's purchased state on the device so it survives restarting
  the app and going offline.
- **FR-011**: A cancelled, failed, or declined payment MUST NOT record a purchase
  and MUST show the user a clear outcome with a way to retry.
- **FR-012**: A product MUST NOT be recorded as purchased more than once. The
  purchased state is keyed on the product code (a product can only ever be
  bought once), so a repeated confirmation for the same product is idempotent
  and never produces a second purchase record, including across app restarts.
- **FR-013**: Purchases MUST be reconciled on app launch against the platform
  store, which is the source of truth: a product the store reports as owned MUST
  be shown as purchased, and a product the store does not report MUST be left
  unpaid.
- **FR-014**: The payment experience MUST use each platform's native in-app
  purchase mechanism (Android and iOS), and MUST NOT collect or store the user's
  payment credentials.
- **FR-015**: Products that are not eligible for purchase MUST NOT present a
  purchase action to the user.
- **FR-016**: Saved products and purchase records MUST remain on the device and
  MUST NOT be sent to analytics or tracking services.
- **FR-017**: All new interactive controls MUST have touch targets of at least
  48 dp, expose accessible names, and remain usable at the largest supported
  system text size.
- **FR-018**: While a purchase is in progress, the app MUST show a clear
  "purchase in progress" state and MUST disable the purchase control until the
  store returns a result, so a second attempt cannot be started.
- **FR-019**: When the user is not signed in to, or the device is not configured
  for, the platform payment system, the app MUST show an explicit "payments
  unavailable" state with guidance to configure the store account and, like
  FR-011, a way to retry, instead of failing silently.

### Key Entities *(include if data involved)*

- **Saved product**: A product the user has chosen to keep, retained on the
  device with enough identifying detail (at least its product code and name) to
  be listed and opened without a network connection; it has a stable identity so
  the same product cannot be saved twice and its removal is permanent until the
  user saves it again.
- **Purchased product**: A saved-or-eligible product that the user has
  successfully paid for, carrying the purchased state and the time it was
  completed. Its stable identity is the product code: a product can only ever be
  bought once, so a repeat confirmation for the same product does not create a
  second purchased record.
- **Purchase attempt**: A single user-initiated purchase flow that can end as
  completed, cancelled, failed, interrupted, or unavailable; it carries a status
  (including an in-progress state) and links to the product being purchased.
- **Navigation destination**: One of the app's main areas (Search, Saved
  products, Diary) that the bottom bar can select and indicate as current.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Saving a product from a search result or its detail view adds it to
  the saved list in a single user action and with no network request, and the
  saved list reflects that addition without requiring the app to be restarted.
- **SC-002**: Saved products are all present after closing and reopening the app
  with no network connection, for every product the user saved.
- **SC-003**: A user can reach any main destination from any other main
  destination in a single interaction with the bottom bar.
- **SC-004**: Every main destination is identifiable as current when selected,
  for all destinations, and is conveyed by more than colour alone (icon, label,
  and selected state) so it stays understandable with accessibility services.
- **SC-005**: A completed purchase is shown as purchased after restarting the
  app, and no product is ever shown as purchased twice for one payment.
- **SC-006**: A cancelled or failed payment never results in a recorded purchase
  or a charge shown as successful.
- **SC-007**: Saved products and purchase records remain on the device, with no
  saved-product or purchase data sent to analytics or tracking services.
- **SC-008**: Every new control on the bottom bar and the purchase flow is
  reachable and understandable using accessibility services, and remains usable
  at the largest supported system text size.
- **SC-009**: An empty saved-products list always shows an explicit message and a
  working shortcut to Search, never a blank screen.
- **SC-010**: While a purchase is in progress the purchase control cannot be
  activated a second time, so a single payment can never produce two purchase
  records.
- **SC-011**: When the platform payment system is unavailable or not configured,
  the user always sees the "payments unavailable" state rather than a silent
  failure.

## Assumptions

- "Buying with Google Pay" is interpreted as a real in-app purchase with genuine
  charges, using each platform's native in-app purchase mechanism; the app is
  not a general storefront and offers no product catalogue of its own beyond the
  nutrition products the user searches for.
- Each platform requires a configured paid-products account and store listing
  before a purchase can be completed; the feature can be built and verified
  against each platform's test environment without live customer charges.
- The products offered for purchase are the same catalogue products the search
  feature already surfaces; no separate product-management system is introduced.
  A product can only ever be bought once, so the purchased state is keyed on the
  product code and a repeat confirmation is idempotent.
- The platform store is the source of truth for what a device owns; on launch
  the app reconciles the purchased state against the store rather than trusting
  only local records.
- Saved products are personal to the device and are not synchronized between
  devices or accounts in this feature.
- The Diary destination and any real diary functionality remain a later feature;
  this feature only reserves its place in the navigation bar with a clear
  "not yet available" state.
- The app remains Android-first for its screens, but the payment capability is
  required on both platforms because it is a shared user-facing capability; the
  iOS purchase path is reached from the iOS app surface.
- This feature does not add accounts, recommendations, or manual product
  creation.

## Dependencies and Risks

- **Platform policy**: Real payments must satisfy each platform's in-app purchase
  policy. This constrains how the purchase action may be presented and requires
  the plan to record the policy review before implementation.
- **Platform accounts**: Completing a real purchase requires store-side product
  configuration and payment accounts; the plan must define how the feature is
  verified against test environments when those accounts are unavailable.
- **Shared logic boundary**: Purchase state, purchase-attempt status, and
  double-recording protection are common behavior and belong in shared code;
  the payment sheet itself is platform-specific and belongs in each platform's
  source set, consistent with the project's existing platform boundary.
