---

description: "User-visible requirements for searching the food catalogue by name"
---

# Feature Specification: Product Search

**Feature Branch**: `002-product-search`

**Created**: 2026-10-19

**Status**: Implemented

**Input**: User request to begin F1 Product search from the project brief after
the platform foundation; users need an interactive app entry point instead of
the current placeholder text.

## Clarifications

### Session 2026-10-19

- Q: When a search already has matching products saved on the device and the phone is online, should the app also refresh those results from the catalogue? → A: Show saved matches immediately and refresh them from the catalogue in the background when online.
- Q: How many recent search terms should remain available on the device? → A: Keep the 10 most recent distinct terms.
- Q: If a user submits a new search before the previous search has finished, which results should be shown? → A: Only the latest submitted search may update the screen.
- Q: Should users be able to delete their saved recent-search history? → A: Let the user remove individual recent search terms.
- Q: Which searches are added to recent history? → A: Record every completed non-blank submitted search, including no-results and recoverable failures; do not record blank submissions.

## User Scenarios & Testing

### User Story 1 - Search the food catalogue by name (Priority: P1)

As a person recording what I eat, I want to search for a food by name and
identify matching products so I can choose the right food for a later diary
entry.

**Why this priority**: Product search is the first product feature in the
project delivery order and provides the first useful interaction beyond the
platform placeholder.

**Independent Test**: Enter a known food name while connected and verify that
matching products appear, can be distinguished using the information available
for each product, and can be opened to inspect their available details.

**Acceptance Scenarios**:

1. **Given** a non-empty search term and a product already available on the
   device for that term, **When** the user submits the search, **Then** matching
   products appear within one second and an online catalogue refresh begins in
   the background without delaying those results.
2. **Given** a non-empty search term with no usable on-device result and a
   normal working internet connection, **When** the user submits the search,
   **Then** matching catalogue products appear within three seconds.
3. **Given** matching products are found, **When** the user opens one result,
   **Then** available identifying and nutrition information for that product
   is shown without creating a diary entry.
4. **Given** the submitted term contains only whitespace, **When** the user
   submits it, **Then** no catalogue request is made and the search screen
   remains in its initial guidance state.

### User Story 2 - Understand empty, no-result, and failure states (Priority: P1)

As a person searching for food, I want clear feedback when there are no matches
or a search cannot complete so I know what happened and what to try next.

**Why this priority**: A search screen is not dependable if an empty catalogue,
an unsuccessful query, and a failed search look identical.

**Independent Test**: Exercise a blank query, a valid query with no matches,
and a failed catalogue lookup; verify each outcome has distinct, actionable
feedback.

**Acceptance Scenarios**:

1. **Given** a submitted term with no matching products in the available
   results, **When** search completes, **Then** a no-results message is shown
   with an option to change the term.
2. **Given** the catalogue cannot be reached and no matching on-device results
   exist, **When** search completes, **Then** a recoverable error is shown and
   the user can retry or edit the term.
3. **Given** the catalogue cannot be reached but matching on-device results
   exist, **When** search completes, **Then** those results remain available
   and the user is informed that they may not be current.
4. **Given** a catalogue product has missing or incomplete nutrition
   information, **When** it matches the search, **Then** it remains findable
   and unavailable nutrition values are clearly identified rather than
   presented as zero.

### User Story 3 - Return to recent searches offline (Priority: P2)

As a person who may lose connectivity, I want to revisit recent searches and
any results already saved on my device so I can continue looking for food
without a connection.

**Why this priority**: Offline access is a core project goal; recent search
history makes the first search feature useful when the network is unavailable.

**Independent Test**: Perform multiple searches, close and reopen the app
without connectivity, and verify that recent terms remain available and can
show any matching products already on the device.

**Acceptance Scenarios**:

1. **Given** searches were completed earlier, **When** the user opens search
   without connectivity, **Then** recent search terms are shown in most-recent
   order.
2. **Given** the user selects a recent term while offline, **When** matching
   product information is available on the device, **Then** those results are
   shown without requiring a connection.
3. **Given** a recent term has no matching product information on the device,
   **When** it is selected offline, **Then** the user sees an offline-specific
   message and can retry after connectivity returns.
4. **Given** a search term is repeated, **When** the new search completes,
   **Then** the term appears once in recent history and moves to the most-recent
   position.
5. **Given** a recent term is no longer wanted, **When** the user removes that
   term from history, **Then** it is removed from recent searches without
   deleting cached product information.

### Scope boundaries

- Product Search provides discovery and product-detail inspection only. Adding,
  editing, or deleting diary entries and calculating daily totals belong to
  F3 Daily diary.
- The initial search and product-detail screens are available on Android. The
  project brief defers iOS UI to the later iOS screens step; shared product
  behavior must remain usable by future native iOS screens.
- The feature does not include barcode scanning, recommendations, manual
  product creation, accounts, or synchronization.

### Edge Cases

- Leading/trailing whitespace and an empty search term do not trigger an
  unnecessary lookup.
- A search with no matches is distinct from an unavailable catalogue and from
  a blank query.
- On-device results remain usable during a network failure and are identified
  as potentially stale.
- Malformed or incomplete product information does not crash search or hide a
  matching product; absent nutrition values are not shown as zero.
- A repeated recent query appears only once and is ordered by its latest use.
- A completed non-blank search is added to recent history even when it ends in no results or a recoverable failure; a blank submission is never recorded.
- A slower response for an earlier submitted term does not replace the results
  for the latest submitted term.
- A result whose product name is missing cannot be presented as a misleading
  valid match; other valid results remain visible.

## Requirements

### Functional Requirements

- **FR-001**: The search experience MUST let a user enter and submit a food
  name.
- **FR-002**: Search results MUST let a user distinguish matching products
  using the identifying information available for each product.
- **FR-003**: A user MUST be able to inspect the available details and nutrition
  information for a selected product without creating a diary entry.
- **FR-004**: Results already available on the device MUST be shown within one
  second of search submission. When online, catalogue refresh MUST occur in the
  background and MUST NOT delay showing the on-device results.
- **FR-005**: When no usable on-device result is available and the internet
  connection is working normally, matching catalogue results MUST appear
  within three seconds.
- **FR-006**: The experience MUST distinguish the initial/empty-query,
  no-results, offline, and recoverable search-failure states and provide an
  appropriate next action.
- **FR-007**: A catalogue failure MUST NOT prevent matching product information
  already available on the device from being displayed; those results MUST
  remain visible and be identified as potentially stale.
- **FR-008**: Recent search terms MUST persist on the device across app
  restarts, remain available offline, be ordered by most recent use, contain no
  duplicates, be limited to the 10 most recent distinct terms, and allow a
  user to remove individual terms without deleting cached products. Order ties
  MUST be deterministic when searches complete in the same clock millisecond.
- **FR-008a**: Every completed non-blank submitted search MUST update recent
  history whether it succeeds, returns no results, or ends in a recoverable
  failure. Blank submissions MUST NOT update history.
- **FR-009**: A product with missing nutrition information MUST remain
  searchable; unavailable values MUST be identified as unavailable, not
  interpreted as zero.
- **FR-010**: Search history MUST remain on-device. Search terms sent to the
  product catalogue MUST NOT be sent to analytics or tracking services.
- **FR-011**: Malformed product data MUST NOT crash the search experience or
  prevent other valid matching products from appearing.
- **FR-012**: Search MUST NOT create, modify, or delete food diary entries.
- **FR-013**: When multiple submitted searches are in progress, only the
  response for the most recently submitted term MUST update the visible results.
- **FR-014**: Search controls MUST have touch targets of at least 48 dp, expose
  accessible names, and remain usable with enlarged system text.

### Key Entities

- **Search term**: The food name entered by the user, after surrounding
  whitespace is disregarded.
- **Product search result**: A catalogue product that matches a search term,
  with the identifying and nutrition information available for display.
- **Recent search**: A previously submitted search term stored on the device
  and ordered by its latest completed search; at most 10 distinct recent
  searches are retained, and each term can be removed independently.
- **Search outcome**: The user-visible result of a search: matching products,
  no matches, unavailable network with usable local results, or a recoverable
  failure.

## Success Criteria

### Measurable Outcomes

- **SC-001**: Matching products already available on the device are visible
  within one second of submitting a search.
- **SC-002**: Searches requiring a catalogue lookup on a normal connection
  show matching products within three seconds.
- **SC-003**: Blank query, no matches, offline-with-results, offline-without-
  results, and recoverable failure each produce distinguishable user feedback.
- **SC-004**: Recent search terms remain visible after closing and reopening
  the app without internet access, with at most 10 distinct terms retained and
  individual deletion available.
- **SC-005**: Missing nutrition data is never displayed as a zero value, and
  malformed product data does not prevent valid results from being used.
- **SC-006**: Searching and inspecting results never creates or changes a
  diary entry.
- **SC-007**: Every search control is reachable and understandable using
  accessibility services and remains usable at the largest supported system
  text size.

## Assumptions

- The product catalogue is the Open Food Facts catalogue described in the
  project brief.
- A normal working internet connection means the device can reach the
  catalogue service without an outage; timing is measured from user submission
  until matching products are visible.
- Search terms are compared without regard to surrounding whitespace, and
  repeating the same normalized term moves its single history entry to the
  most-recent position.
- If a product does not provide a nutrition value, the user sees an explicit
  unavailable indication rather than a fabricated value.
- The first release of this feature provides Android screens only; later iOS
  UI scope is governed by the planned iOS screens step.
