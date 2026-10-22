# Feature Specification: Intuitive navigation in the app's main work

**Feature Branch**: `008-intuitive-navigation`

**Created**: 2026-10-22

**Status**: Draft

**Input**: Fix the flows in the app that make the navigation hard to follow:
when the user adds a food to a meal and goes back, the app must return to the
search (the step where the food is looked for), not to a product detail they
already left. Fix every other navigation that behaves the same way, so the
main work of the app - looking for a food, logging it, and seeing the day it
was logged into - is one continuous, predictable flow.

## Clarifications

### Session 2026-10-22

- Q: After a food is logged, where does the user land? A: On the flow's search
  step, with the meal still chosen, so another food for the same meal is one
  search away. Confirmed.
- Q: What does the back control mean inside the flow? A: The previous step of
  the flow, keeping what was chosen; from the flow's first step, the screen
  the flow came from. Confirmed.
- Q: When the quantity step goes back, or a detail row's back control is
  pressed, where does the app return? A: The flow's search step, with the
  query and the results kept; a detour never appears, whatever pushed on the
  flow. Confirmed.
- Q: When the flow was opened from a Library row (a food already chosen) and
  the user confirms, where does the app land? A: The flow's search step as
  well, so the work of adding food is one continuous flow from any opening.
  Confirmed.
- Q: What does a result's tap inside the flow's search do? A: It opens the
  food's detail, so the nutrition is read before the amount is chosen; the
  detail's "Add to diary" carries the food into the flow's quantity step.
  Confirmed.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A food is logged and the user goes on (Priority: P1)

The user adds a food to a meal and confirms it: the app returns to the step
where the food is looked for, with the meal still chosen, so the same meal's
next food is one search away. The screen the flow was opened from stays
exactly where it was underneath, and the food the entry was logged from is
never presented again on the way out.

**Why this priority**: it is the app's main work, the path every user takes
many times a day, and the defect the user reported.

**Independent Test**: "Can be fully tested by opening the flow, logging a food
into a meal and confirming: the search is shown again with the meal still
chosen, the totals of that meal's section go up, and no product detail is
shown."

**Acceptance Scenarios**:

1. **Given** the user opens the flow from a meal of the diary, **When** they
   log one food with the confirm control, **Then** the source step (search) is
   shown with that meal still chosen, the day's totals include the food they
   logged and the product detail they came from is not shown.
2. **Given** the user opened the flow from the Library, **When** they confirm
   a food, **Then** the search step is shown again and the Library is one back
   press away.
3. **Given** the user logged a food and is on the search step, **When** they
   press back, **Then** they return to the screen the flow was opened from,
   which shows the state it had, with the new entry among them.

### User Story 2 - Back means the previous step, never a detour (Priority: P1)

Every back press inside the flow means "one step", and the screen it returns to
is the one the user genuinely came from: the quantity step returns to the step
the food was chosen on, the detail a flow row opens returns to that same step,
and the flow's first step returns to the screen it came from. A pal that was
opened from nowhere (a detail the user never saw) never appears on the way
back, and the meal already chosen never silently disappears.

**Why this priority**: predictability of the back is what "intuitive"
navigation is, and it is what the reported defect was.

**Independent Test**: "Can be fully tested by walking the flow forward and
pressing back at every step: each press lands on the step actually visited
before, with every choice kept."

**Acceptance Scenarios**:

1. **Given** a food was chosen and the quantity step is shown, **When** the
   user presses back, **Then** the step where the food was looked for is shown
   again with the choice still in it, and the amount chosen so far is kept.
2. **Given** the flow's row opened a product detail, **When** the user presses
   the detail's back control, **Then** the flow's step they came from is shown,
   with the search results and the meal as they were.
3. **Given** the flow's first step was reached from the diary, **When** the
   user presses back, **Then** the diary is shown as it was, and the meal
   section they opened the flow from is visible.

### User Story 3 - The screens the app opens are the ones the user asked for (Priority: P2)

The app never shows a screen the user did not navigate to: no detail screen
resurfaces after a destination switch, no destination is duplicated on the
stack by a second tap of the same tab, and no screen of the app hides its back
control behind a state that has no meaning. A screen that opens from a
destination's menu (goals, premium, privacy) returns to that destination's
state, and the same tap of the same tab is a no-op.

**Why this priority**: it is the class of defect 007 removed from the resolved
paths, and it must not come back through the rest of the work.

**Independent Test**: "Can be fully tested by walking every reachable screen
and pressing back and repeating a tap of the bottom bar at each of them: no
screen appears twice, no unknown screen appears."

**Acceptance Scenarios**:

1. **Given** the user is on a destination's root, **When** the bottom bar's
   current tab is tapped, **Then** nothing changes and no new entry is pushed.
2. **Given** the user opened the goals editor from More, **When** they come
   back from it, **Then** the state the editor left is not lost and no screen
   from another destination is shown.
3. **Given** the user opened the Coach over the flow and closes it, **When**
   they go back, **Then** the flow is exactly as they left it, step and choice
   included.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001** Logging a food with the flow's confirm control returns the user
  to the step where the food is looked for, with the meal still chosen; the
  screen the flow came from stays behind and no product detail is shown on
  the way out.
- **FR-002** A back press inside the flow means the previous step of the flow;
  the flow keeps every choice the user made across those steps.
- **FR-003** A product detail opened from the flow's rows returns, on its back
  control, to the flow's step it came from, with the flow's state as it was.
- **FR-004** A back press on the flow's first step returns to the screen the
  flow came from.
- **FR-005** A logged entry is counted by the screen the flow came from when
  it is returned to, without the user having to reopen the app's state.
- **FR-006** A second tap of the bottom bar's current tab changes nothing.
- **FR-007** The bottom bar is visible on the three destinations' roots and
  hidden inside a child screen; a child screen's back control always returns
  to the screen it came from.
- **FR-008** The Coach opens over the screen it is asked from and closes back
  onto it, with that screen's state untouched.
- **FR-009** The goals editor, the premium surface and the privacy note live
  in More and return from their back controls to More's state.
- **FR-010** Logging one food produces exactly one entry, once.

### Key Entities *(include if feature involves data)*

- **DiaryEntry**: the food logged with the day and the meal it was logged into;
  changes nothing in this feature.
- **The flow's state**: the meal, the chosen food, the amount, the source and
  the query of the flow; it survives the screen changes the flow's own steps
  cause, and is reset when the flow is opened again.
- **The screens of the app**: the three destinations' roots, the flow's two
  steps, the product detail, the assistant's dialog and the goals editor.

## Review & Acceptance Checklist

### Content Quality

- [x] Navigation decisions are stated as the user's experience, not as a
  route table (FR-001 to FR-004, FR-007)
- [x] The app's cold-start behaviour is unaffected (FR-005 uses the diary
  state the app already owns)

### Required Sections

- [x] User Scenarios present with priorities and acceptance scenarios
- [x] Requirements are testable and traceable to the scenarios they serve

### Spec Readiness

- [x] All decisions recorded in Clarifications have prerequisites wired in the
  tasks that follow
- [x] The system up to ADR-0020 is not contradicted by this specification
