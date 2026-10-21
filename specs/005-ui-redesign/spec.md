# Feature Specification: Modern UI Redesign

**Feature Branch**: `005-ui-redesign`
**Created**: 2026-10-21
**Status**: Implemented
**Input**: User description: "Redesign the UI to look like a modern app using icons instead of buttons where appropriate and giving a proper look and feel to the app to look professional, not just a child game app"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Modern, icon-driven navigation (Priority: P1)

As a user, I want a clean, modern bottom navigation that uses clear icons with proper visual hierarchy so the app feels professional and easy to navigate at a glance.

**Why this priority**: Navigation is always visible; a polished bar sets the tone for the whole app.

**Independent Test**: Open the app and verify the bottom bar uses modern iconography, active/inactive states are clearly distinguishable without relying solely on color, and labels remain accessible.

**Acceptance Scenarios**:

1. **Given** the app is open, **When** I look at the bottom navigation, **Then** each destination shows an icon with a proper label and the active destination is clearly indicated.
2. **Given** I switch tabs, **When** I tap a different destination, **Then** the transition feels smooth and the selected state updates immediately.
3. **Given** I use accessibility services, **When** navigating, **Then** destinations remain identifiable by icon + label and touch targets meet minimum size.

---

### User Story 2 - Professional search experience (Priority: P1)

As a user, I want the product search screen to feel polished — clean search bar, modern result cards with proper imagery, spacing, and icon-based actions — so I can scan results quickly and confidently.

**Why this priority**: Search is the primary entry point for discovering products.

**Independent Test**: Search for a product and verify cards use a modern layout with product images, clear hierarchy, subtle elevation/shadows, and icon actions where appropriate.

**Acceptance Scenarios**:

1. **Given** I search for a product, **When** results load, **Then** each result card shows the product image, name, brand, and key info in a clean card layout.
2. **Given** a result card, **When** I view it, **Then** actions (e.g. save) use icon buttons with proper states rather than bulky filled buttons where it improves UX.
3. **Given** no results or loading state, **When** displayed, **Then** empty/loading states look polished and professional.

---

### User Story 3 - Clean product detail & purchase UI (Priority: P1)

As a user, I want the product detail screen and purchase flow to look refined — proper typography, spacing, icon usage, and a professional treatment of nutrition info and purchase actions.

**Why this priority**: The detail view and purchase flow are key conversion points.

**Independent Test**: Open a product detail, verify the layout is clean with icons for actions, nutrition is well-organized, and the purchase section looks professional.

**Acceptance Scenarios**:

1. **Given** I open product detail, **When** viewing it, **Then** the header, image, and key info are clearly organized with good spacing and hierarchy.
2. **Given** the purchase section is visible, **When** I interact with it, **Then** the buy/action uses a modern, icon-appropriate treatment with clear states (enabled/disabled/pending).
3. **Given** I save/unsave a product, **When** I tap the action, **Then** the icon state updates smoothly with proper affordance.

---

### User Story 4 - Polished saved products & diary screens (Priority: P2)

As a user, I want the Saved products and Diary screens to look consistent with the modern design language — clean lists, empty states with icons/illustrations, and a professional overall feel.

**Why this priority**: Consistency across all tabs is essential for a professional app.

**Independent Test**: Navigate to Saved products and Diary, verify consistent card/list styling, polished empty states, and icon usage throughout.

**Acceptance Scenarios**:

1. **Given** I have saved products, **When** I view the Saved tab, **Then** products appear in clean, scannable cards consistent with the design system.
2. **Given** no saved products, **When** I view the Saved tab, **Then** a polished empty state with icon/text/CTA guides me to search.
3. **Given** I view the Diary tab, **When** no entries exist, **Then** it looks intentional and professional (not placeholder-like).

---

### Edge Cases

- **Dark mode**: All redesigned surfaces must look polished in dark mode with proper contrast.
- **Small screens**: Layout must adapt gracefully without breaking or feeling cramped.
- **Accessibility**: Icon buttons need proper content descriptions; touch targets >= 48dp; sufficient contrast.
- **Existing functionality**: All current features (save/purchase/navigation) must continue to work exactly as before.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Bottom navigation MUST use modern iconography with clear active/inactive states.
- **FR-002**: Replace bulky text buttons with icon buttons where appropriate while maintaining accessibility.
- **FR-003**: Search results MUST be displayed in clean, modern cards with consistent spacing/elevation.
- **FR-004**: Product detail MUST have improved typography hierarchy, spacing, and visual organization.
- **FR-005**: Saved products and Diary screens MUST be restyled for consistency and professionalism.
- **FR-006**: All interactive elements MUST have proper states (default/pressed/disabled/selected).
- **FR-007**: Empty states MUST be polished with appropriate icons and helpful guidance.
- **FR-008**: Design MUST work well in both light and dark modes.

### Non-Functional Requirements

- **NFR-001**: No regression in existing functionality or navigation behavior.
- **NFR-002**: Maintain or improve accessibility (content descriptions, touch targets, contrast).
- **NFR-003**: Changes should feel cohesive as a unified design system.
- **NFR-004**: Performance must not be negatively impacted by styling changes.

## Success Criteria *(mandatory)*

- **SC-001**: App looks distinctly more professional and modern compared to current appearance.
- **SC-002**: Icons are used effectively throughout; buttons feel intentional and polished.
- **SC-003**: All existing tests pass (unit, instrumented) with no functionality regressions.
- **SC-004**: UI is consistent across all screens (navigation, search, detail, saved, diary).
- **SC-005**: Dark mode looks polished with good contrast ratios.

## Out of Scope

- Complete visual overhaul of every pixel or rebrand with new logo/name.
- Major architectural changes.
- New features beyond UI/UX improvements.
- Changes to backend/data layer logic (only UI/styling where necessary).


## Clarifications

### Session 2026-10-21

- **Q1: Overall Design Direction** → **A) Refined Material 3** – Clean, polished, platform-consistent. Looks professional without chasing trends.
- **Q2: Bottom Navigation Treatment** → **A) Icons + labels always visible** – Keep current pattern but refresh icons and states. Best for accessibility and clarity.
- **Q3: Buttons vs Icons** → **A) Balanced** – Secondary actions (save/unsave, remove) use `IconButton`. Primary CTAs (Buy/Purchase) keep text with an icon only when it adds meaning. Keep labels on important actions.
- **Q4: Cards, Elevation & Spacing** → **A) Subtle elevated cards** – `ElevatedCard` with soft corners, gentle elevation, consistent padding. Clean and modern.
- **Q5: Micro-interactions & Animations** → **A) Minimal** – Stick to Material 3 default press/selection states. No custom animations. Safe and performant.
- **Q6: iOS (SwiftUI) Scope** → **A) Android-first** – Focus all redesign work on Android/Compose. Only fix obvious SwiftUI inconsistencies if trivial.
- **Q7: Empty States** → **A) Icon + heading + body + CTA** – Polished and guides the user (especially "Saved products" -> "Search products").
