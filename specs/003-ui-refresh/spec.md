# Feature Specification: Modern UI Refresh with Product Images

**Feature Branch**: `003-ui-refresh`

**Created**: 2026-10-20

**Status**: Implemented

**Input**: User description: "The functionality is set, but the look and feel looks
terrible. Refactor the UI to look like a modern UI, improving the detail of each
product, and connect to an API to set a descriptive image for each product
(select the most capable API for this case)."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Recognise products at a glance (Priority: P1)

As a user searching for a food product, I want to see a representative image for
each result so I can identify the right product quickly instead of reading every
text row.

**Why this priority**: The current text-only result list is the primary reason
the feature feels unfinished. Visual identification is the highest-value
improvement and the specific capability the user asked to add.

**Independent Test**: Submit a query that returns products with images and
verify each result card shows an image (or a neutral placeholder when the
catalogue has no image) without delaying the result list.

**Acceptance Scenarios**:

1. **Given** a completed search whose products have image URLs, **When** the
   results are shown, **Then** each result card displays that product's image in
   a fixed thumbnail area.
2. **Given** a product whose catalogue entry has no image URL, **When** its
   result card is shown, **Then** a neutral placeholder is displayed and no
   error or broken-image state appears.
3. **Given** an image download fails or is slow, **When** the result list is
   shown, **Then** the product name and nutrition summary are still visible and
   the image area degrades to the placeholder.

---

### User Story 2 - Understand a product's nutrition at a glance (Priority: P1)

As a user inspecting a product, I want a well-structured, modern detail view
that highlights the important nutrition information, so I can judge the product
without reading a flat list of rows.

**Why this priority**: The detail view is the destination of every search and is
currently the least polished surface. Improving it is on the critical path for
the "modern UI" requirement.

**Independent Test**: Open a product with full, partial, and missing nutrition
data and verify the detail view presents a header image, key facts, a
Nutri-Score indicator when available, and clearly-labelled nutrition values with
an explicit "unavailable" treatment for missing data.

**Acceptance Scenarios**:

1. **Given** a product with a Nutri-Score grade, **When** its detail view is
   shown, **Then** the grade is displayed as a clearly readable badge.
2. **Given** a product missing some nutrition values, **When** its detail view
   is shown, **Then** each missing value is shown as unavailable and never as
   zero.
3. **Given** a product with an image, **When** its detail view is shown, **Then**
   the image is shown as a header above the product facts.

---

### User Story 3 - Use a consistent, modern look and feel (Priority: P2)

As a user, I want the app to use a coherent modern visual language (colour,
typography, shape, spacing, states) so it feels like a finished product rather
than a prototype.

**Why this priority**: Cohesion and polish are the umbrella requirement, but
they depend on the result and detail surfaces existing first.

**Independent Test**: Walk through initial, loading, results, empty, offline,
error, and detail states and verify a single consistent theme, clear hierarchy,
and readable contrast throughout.

**Acceptance Scenarios**:

1. **Given** the app is opened, **When** the initial screen is shown, **Then** a
   themed search surface is presented with clear affordances and no reliance on
   default component styling.
2. **Given** a search is running, **When** results are being fetched, **Then** a
   loading state is visible and does not shift the layout when results arrive.
3. **Given** accessibility settings such as enlarged text, **When** each screen
   is shown, **Then** content remains readable and controls keep their minimum
   touch targets.

---

### Edge Cases

- Product has an image URL that returns a non-image or 404 response.
- Product name or brand is empty, very long, or contains mixed scripts.
- Nutri-Score grade is absent, unknown, or out of the known a-e range.
- Very large system text scale and high-contrast/dark mode.
- Device is offline: cached results must still render with images that are
  already cached and placeholders otherwise.
- Extremely long product lists must scroll smoothly without loading all images
  eagerly.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Result cards MUST show a fixed-size product image area for each
  product.
- **FR-002**: The detail view MUST show a product image header when an image is
  available.
- **FR-003**: The system MUST show a neutral placeholder when a product has no
  image or the image cannot be loaded, and MUST NOT show a broken-image or error
  state.
- **FR-004**: Image loading MUST NOT block, delay, or replace product and
  nutrition content; text content is shown first and images fill in.
- **FR-005**: The detail view MUST present a Nutri-Score indicator when the
  catalogue provides a supported grade.
- **FR-006**: Missing nutrition values MUST be displayed as unavailable and
  never as zero.
- **FR-007**: The app MUST apply a single consistent theme (colour, typography,
  shape, spacing) to all search and detail surfaces.
- **FR-008**: The app MUST provide distinct, readable states for initial,
  loading, results, no-results, offline, and error conditions.
- **FR-009**: All interactive controls MUST keep a minimum 48 dp touch target
  and accessible labels, preserving the existing accessibility guarantees.
- **FR-010**: Attribution to Open Food Facts MUST remain visible wherever
  product data or images are shown and MUST credit the image source.
- **FR-011**: The app MUST NOT introduce analytics, tracking, or search-term
  logging; image requests carry no user identity beyond the app User-Agent.
- **FR-012**: Existing product-search behavior (offline-first results,
  latest-query-wins, recent-search history) MUST be preserved.

### Key Entities

- **Product**: catalogue item identified by `code`; adds optional image URL
  fields (thumbnail and full/front image) alongside existing name, brand,
  quantity, Nutri-Score, and nutrition values.
- **Nutrition value**: amount, unit, and basis (per 100 g / per serving /
  unknown) for a nutrient.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every result card and detail view renders within the existing
  response-time goals; images never delay text content.
- **SC-002**: 100% of products with an image URL show an image, and 100% of
  products without one show a placeholder and zero broken-image states.
- **SC-003**: All existing automated UI and unit tests continue to pass, with
  new coverage for image-present, image-absent, and image-failure cases.
- **SC-004**: Accessibility checks (labels, 48 dp targets, enlarged text) remain
  green on all search and detail surfaces.
- **SC-005**: No analytics/tracking/logging dependency or call site is added.
- **SC-006**: The constitution's coverage gate (≥80% line) and lint/architecture
  gates remain green.

## Assumptions

- Open Food Facts catalogue image URLs (CC BY-SA) are the selected image source;
  the licence and attribution consequences are recorded in ADR-0009.
- Image loading may reuse an existing/﹘approved Compose image-loading library or
  a minimal custom loader; whichever is chosen must be recorded in an ADR before
  it is added to the version catalog.
- The iOS UI remains out of scope for this feature; the shared domain/data
  changes stay common so a later iOS screen can reuse them.
