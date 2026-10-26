# Feature Specification: Meal Detail as the Meal Workspace with Library-First Food Selection

**Feature Branch**: `010-library-meal-details`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Simplify the interaction so that users do not have to search every time they add a food. Foods saved in the Library must appear first, with an option to search for a new food when it is not in the Library. Every meal must open a detail screen showing its added foods and allowing them to be edited there. A meal's Add button must no longer redirect to food search; it opens the meal detail, where users can add saved Library foods and explicitly open search when needed."

## Problem Statement

Today, adding a food from a meal sends the user straight into catalogue search, even when the food is already saved in the Library. Repeating a familiar meal therefore starts with typing or searching every time, and the meal itself has no single place where its foods can be reviewed, corrected, moved or replaced.

This feature makes each meal a workspace. A meal or its Add action opens that meal's detail. Adding starts from saved Library foods, and search is an explicit choice rather than the default. The same detail is where an entry is opened, corrected, moved to another meal or replaced, with the meal's and the day's nutrition staying truthful after every change.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The meal detail is the meal workspace (Priority: P1)

As a user, I want to open a meal and see everything logged in it, so that I can understand the meal, add to it and manage its foods without leaving that context.

**Why this priority**: The meal is the unit the user thinks in when logging. Without a meal workspace, every correction or addition starts from a global search screen that forgets which meal the user was working on.

**Independent Test**: Open each of the day's four meals from Today, including an empty meal, and verify the detail shows that meal's entries, quantities, nutrition, subtotal and empty state, and that back returns to Today at the meal that was opened.

**Acceptance Scenarios**:

1. **Given** the Today screen with entries in Breakfast, **When** the user activates the Breakfast section header, **Then** the Breakfast detail for the current day opens and shows every Breakfast entry with its quantity and nutrition.
2. **Given** the Today screen, **When** the user activates the Add action on Lunch, **Then** the Lunch detail opens and does not open catalogue search.
3. **Given** a meal detail with entries, **When** the user views it, **Then** it shows the meal name, the meal's calorie subtotal and reported macro subtotals, and the nutrition contributed by each entry.
4. **Given** a meal with no entries, **When** its detail opens, **Then** it shows a clear empty state that explains the meal has no foods yet and offers an add action, not a search field.
5. **Given** a meal detail opened from Today, **When** the user goes back, **Then** Today is shown at the meal section that was opened, with its summaries and entries as they were.
6. **Given** an entry was added or edited in a meal detail, **When** the user goes back from the resulting meal detail, **Then** Today is shown with the updated summaries and the meal containing the entry visible.
7. **Given** the Today screen, **When** the user views the meal sections, **Then** the existing section summaries and inline entries remain visible and keep their current behavior, apart from the section and Add actions now opening the meal detail.

---

### User Story 2 - Adding starts with saved foods and search is explicit (Priority: P1)

As a user, I want my saved foods offered immediately when I add to a meal, so that logging a familiar food takes a few actions and no search, while a new food is still one deliberate search away.

**Why this priority**: The repeated, daily action is logging a food the user already knows. Making search the mandatory first step is the friction this feature exists to remove, while an explicit search path keeps new foods reachable.

**Independent Test**: From Today, open an empty meal, choose a saved Library food, confirm the proposed quantity, and verify the entry appears in that meal without typing. Repeat with an empty Library and verify the explicit search action is present and opens the existing catalogue experience.

**Acceptance Scenarios**:

1. **Given** the user opens a meal detail through a meal Add action, **When** the Library-first selector is shown, **Then** saved Library foods are listed immediately without text input and catalogue search is not the default view.
2. **Given** the Library-first selector, **When** the user selects a saved food, **Then** the quantity confirmation step opens with that food and the existing proposed quantity already chosen, and shows the nutrition that quantity produces.
3. **Given** the user confirms the proposed quantity for a saved food, **When** the confirmation completes, **Then** exactly one entry is saved to that meal and the meal detail is shown with the entry and the updated totals.
4. **Given** the user starts from Today and logs a saved food without changing the proposal, **When** they count their actions, **Then** the food is logged in exactly three actions: activate the meal Add, choose the saved food, confirm.
5. **Given** the Library is empty, **When** the selector opens, **Then** it says that no saved foods exist, keeps the explicit search action available, and does not start a catalogue search or network request on its own.
6. **Given** the user chooses the explicit search action, **When** search opens, **Then** the existing catalogue search behavior is preserved, including product inspection for a new catalogue food, barcode capability, the offline cache and the catalogue attribution.
7. **Given** search returns no results or fails, **When** the user is shown the outcome, **Then** the app explains it without losing the meal context, offers a way to retry, and the Library-first selector remains reachable.
8. **Given** the add flow is open at any step, **When** the user exits or cancels the overall add flow without confirming, **Then** no entry is saved and the meal detail is shown unchanged.
9. **Given** the quantity confirmation step is open, **When** the user goes back, **Then** the saved-food selection for the same meal is shown with the entry unsaved; a further back press shows the meal detail unchanged.
10. **Given** the user changes the meal during an add flow, **When** they confirm, **Then** the entry is saved to the chosen meal and the detail for that meal is shown.

---

### User Story 3 - An entry can be corrected completely in one edit flow (Priority: P1)

As a user, I want to open a logged food and fix its quantity, move it to another meal or replace the food itself, so that a mistake is corrected in one place instead of being removed and recreated.

**Why this priority**: A meal workspace is only useful if the entries in it can be corrected. Partial editing forces users to delete and re-enter foods, which risks losing the entry's day and history and multiplies the chance of duplicates.

**Independent Test**: Open an existing entry from a meal detail, change its quantity, confirm, and verify the nutrition rescales in the same entry. Then move the entry to another meal and verify the subtotals of both meals. Then replace its food and verify the replacement's nutrition.

**Acceptance Scenarios**:

1. **Given** a meal detail with an entry, **When** the user opens that entry, **Then** an edit flow opens containing the entry's current food, quantity and meal.
2. **Given** the edit flow, **When** the user changes the quantity and saves, **Then** the same entry remains in the meal with the new quantity and nutrition scaled from its frozen values.
3. **Given** the edit flow, **When** the user moves the entry to another meal and saves, **Then** the entry no longer appears in the origin meal, appears exactly once in the destination meal, and both meal subtotals and the day's totals are updated.
4. **Given** the edit flow, **When** the user replaces the food using the Library-first selector or the explicit search action, **Then** the entry keeps its identity but uses the replacement food's nutrition scaled to the chosen quantity.
5. **Given** the user changes quantity, meal and food in the same edit flow, **When** they save once, **Then** all three changes are applied once and the app shows the detail of the meal that now contains the entry.
6. **Given** a saved edit, **When** the user inspects the entry, **Then** it is the same logical entry with its original day and original logged time, not a newly added entry.
7. **Given** the edit flow, **When** the user cancels or goes back without saving, **Then** the entry is unchanged and remains where it was.
8. **Given** a saved edit, **When** the user opens that entry again, **Then** the updated quantity, meal and food are shown.
9. **Given** another occurrence of the same product in the diary, **When** one occurrence is edited, **Then** the other occurrence is unchanged.

---

### Edge Cases

- **Empty Library**: the selector explains that nothing is saved yet and keeps the explicit search action available. It does not run a search by itself and does not imply that no food can be added.
- **Saved food absent from the catalogue**: a Library food that no longer exists in the catalogue remains selectable and loggable from the saved data. Its missing nutrition is shown as unknown, and the entry is still usable.
- **Unknown nutrient values**: a nutrient the food does not report stays unknown after an add or an edit. It is never displayed or totalled as zero, and an edit or replacement does not invent a value.
- **Missing image or brand**: the entry and selector show a neutral placeholder and a stable layout without a broken image or an empty label that looks like a defect.
- **Same product logged more than once in one meal**: each occurrence appears as its own entry and can be opened, edited or removed independently. Editing or removing one does not change the others.
- **Repeated confirmation or save**: a second activation caused by a double tap or an impatient repeat does not create a second entry and does not apply an edit twice. One completed confirmation produces one result.
- **Write failure**: if an add, edit or removal cannot be stored, the user is told it was not saved, the app does not present updated totals as if it had succeeded, and the user can retry without losing the choices already made.
- **Move to the same meal**: saving an edit that keeps the entry in its current meal changes nothing about the entry's meal membership and updates only what the user actually changed.
- **Zero, negative, fractional or very large quantity**: existing quantity rules still apply. Zero and negative quantities are invalid and cannot be confirmed. Positive fractional quantities are supported. The confirmation does not save an invalid amount.
- **No proposed quantity**: if a saved food has no usable proposed quantity, the user must choose a valid quantity before confirming instead of the app guessing one.
- **Search failure or no results**: the failure is stated honestly, the retry path is visible, and the user can return to the Library-first selector with the meal still chosen.
- **Offline**: viewing a meal detail and adding, editing, moving, replacing or removing an entry all work with no connection when the selected foods are saved Library foods or their food data is already available locally. A saved food does not require the catalogue to be reachable. New catalogue discovery, including searching for and replacing with a food that is not locally available, retains the existing cache and connectivity behavior and may be unavailable offline.
- **Restart during or after a change**: a completed add, edit, move, replacement or removal survives a restart. An unconfirmed change is not saved.
- **Day boundary or time zone change**: a meal detail is always the current day's meal. An edit preserves the entry's original day and time, so an entry never silently moves to another day.
- **Entry removed from the origin meal while another edit is in progress**: the user is told the entry no longer exists or is shown the refreshed meal, no duplicate is created, and no unrelated entry is changed.
- **Large text and assistive technology**: meal details, selectors, quantity confirmation and edit controls remain usable at large text sizes, and icon-only controls keep meaningful descriptions.
- **Library size**: saved foods are offered as a scrollable list. Because Library text filtering is out of scope, a very large Library may require more scrolling; this is documented as an accepted limitation of this feature rather than a reason to fall back to catalogue search.
- **Direct entry points outside meal detail**: opening the global add action, a Library row or a Coach action keeps its existing behavior and is not redirected into a meal detail by this feature.

## Requirements *(mandatory)*

### Functional Requirements

#### Meal Detail and Navigation

- **FR-001**: Activating a meal section on Today, or that section's Add action, MUST open the current day's detail for that meal. A meal Add action MUST NOT open catalogue search.
- **FR-002**: A meal detail MUST show the meal name, every entry in that meal with its food name, quantity and nutrition, the meal's calorie and reported macro subtotals, a meal-level add action, and per-entry edit and remove actions.
- **FR-003**: A meal detail with no entries MUST show an empty state that explains the meal has no foods yet and offers an add action that leads to saved Library foods. It MUST NOT show an empty search field as the primary empty state.
- **FR-004**: Going back from a meal detail MUST return to the screen that opened it. When the detail was opened from Today, back MUST return to Today at the same meal section. When the detail is the result of a completed add or edit, back MUST return to Today with the meal now containing the entry visible.
- **FR-005**: Today MUST retain its existing calorie and macro summaries, meal section summaries and inline entries. This feature changes only where the meal section and Add actions lead and the detail behavior described here.
- **FR-006**: The existing back rule MUST remain in force everywhere outside the meal detail flows: one back press moves to the previous screen the user genuinely came from, keeps the choices already made, and never shows a screen the user did not visit.

#### Library-First Adding

- **FR-007**: The add action in a meal detail MUST present the user's saved Library foods immediately, without requiring text input, and MUST NOT present catalogue search as the default view.
- **FR-008**: The food selector MUST always include an explicit action to search for a new food, including when the Library is empty or when saved foods fail to load.
- **FR-009**: When the Library is empty, the selector MUST state that no saved foods exist and MUST keep the explicit search action available. It MUST NOT start a catalogue search automatically.
- **FR-010**: Selecting a saved food MUST continue to quantity confirmation with that food selected and the existing proposed quantity already applied. The confirmation MUST show the nutrition that quantity produces before the user confirms.
- **FR-011**: When a meal detail is opened through a Today meal Add action, the Library-first selector MUST be presented as part of that arrival, so that logging a saved food with its proposed quantity takes exactly three user actions: activate the meal Add, choose the saved food, and confirm.
- **FR-012**: The explicit search action MUST open the existing catalogue search experience and MUST preserve its existing behavior: result selection, product inspection for a new catalogue food, barcode capability, recent searches, the offline cache and the catalogue attribution.
- **FR-013**: Confirming an add MUST save exactly one entry to the selected meal, using the chosen food and quantity. If the user changes the meal before confirming, the entry MUST be saved to the chosen meal.
- **FR-014**: After an add is confirmed, the app MUST show the detail of the meal that now contains the entry, with the new entry and the updated meal and daily totals visible without a manual refresh.
- **FR-015**: One confirmed add MUST produce one entry. Repeated activation of the confirm action, including a double tap, MUST NOT create a duplicate entry or save the same food twice.
- **FR-016**: Going back or cancelling before confirmation MUST save nothing. Going back from quantity confirmation MUST return to the saved-food selection for the same meal, and going back from the selection MUST return to the meal detail unchanged.

#### Editing an Entry

- **FR-017**: Every entry shown in a meal detail MUST be openable for editing.
- **FR-018**: In a single edit flow the user MUST be able to change the quantity, move the entry to another meal, replace its food, or combine those changes.
- **FR-019**: Replacing a food in the edit flow MUST use the same Library-first selector and the same explicit search action as adding, including the empty-Library state and the existing catalogue search behavior.
- **FR-020**: A quantity change MUST rescale the entry's existing frozen nutrition values. Replacing the food MUST use the replacement food's nutrition values, scaled to the chosen quantity.
- **FR-021**: Saving an edit MUST update the same logical entry. It MUST NOT add a new entry or duplicate the original, and it MUST preserve the entry's original day and original logged time. Date and time editing are out of scope.
- **FR-022**: Moving an entry MUST remove it from the origin meal for that day, place it exactly once in the destination meal, and update the subtotals of both meals and the day's totals.
- **FR-023**: After an edit is saved, the app MUST show the detail of the meal that now contains the entry, with the updated entry and totals visible without a manual refresh.
- **FR-024**: Cancelling or going back from an edit without saving MUST leave the entry's quantity, meal and food unchanged.
- **FR-025**: Repeated save activation MUST apply one edit once and MUST NOT create a duplicate or apply the changes twice.
- **FR-026**: Editing or removing one occurrence of a food MUST NOT change any other occurrence of the same food in the diary.

#### Removal

- **FR-027**: The existing entry removal capability MUST remain available from the meal detail and MUST update the meal subtotal and the day's totals. Its existing interaction behavior is retained; this feature introduces no new confirmation or undo requirement.

#### Nutrition Truthfulness

- **FR-028**: Nutrition values MUST follow the existing rule that a nutrient not reported by a food is unknown. Unknown values MUST never be displayed or totalled as zero in a meal detail, an entry, a selector preview or a quantity confirmation.
- **FR-029**: A saved food that is not present in the catalogue MUST remain usable. Any nutrition it does not provide MUST be presented as unknown rather than being omitted in a way that implies zero.

#### Persistence, Offline and Failure Behavior

- **FR-030**: Viewing a meal detail and adding, editing, moving, replacing and removing entries MUST work with no connection when the operation uses saved Library foods or food data already available locally. Selecting a saved food MUST NOT require a network request. Discovering a new catalogue food retains the existing offline cache and connectivity behavior and is not guaranteed while offline.
- **FR-031**: Every completed add, edit, move, replacement and removal MUST persist and MUST survive an app restart. An unconfirmed change MUST NOT persist.
- **FR-032**: If a change cannot be stored, the app MUST state that it was not saved, MUST NOT present the change as completed, and MUST offer a way to retry without silently discarding the user's choices.
- **FR-033**: Search failure, an empty search result and a missing product MUST be stated honestly, MUST offer a usable next step, and MUST NOT lose the meal context or make the Library-first selector unreachable.
- **FR-034**: A missing product image or brand MUST be shown with a neutral placeholder and MUST NOT break the layout or produce a broken image.

#### Platform and Compatibility

- **FR-035**: Every user-visible behavior in this specification MUST be equivalent on Android and iOS, including the meal detail, Library-first selection, explicit search fallback, add, edit, move, replace, remove, empty and failure states.
- **FR-036**: This specification supersedes only the meal Add destination described in the earlier restructure specification and the post-add landing described in the earlier navigation specification, and only for flows that begin at a Today meal Add or at that meal's detail. The earlier navigation specification's predictable back behavior remains in force.
- **FR-037**: This specification's behavior is the parity target where the earlier iOS parity specification conflicts. The earlier rule that a completed add returns to Today is superseded for flows that begin at a Today meal Add or at that meal's detail.
- **FR-038**: The global add action, direct Library actions, Coach actions and every other entry point not named in this specification MUST keep their existing behavior.

### Key Entities

- **Meal Detail**: the current day's view of one meal. It contains the meal name, its entries, the meal's nutrition subtotal, an empty state, and the add, edit and remove actions.
- **Diary Entry**: one logged occurrence of a food in a meal on a day. It has an identity independent of the food, a quantity, a meal, an original logging time, an original day, and a frozen set of nutrition values where each value may be unknown.
- **Library Food**: a food the user has saved for quick selection. It may exist in the catalogue, may be absent from it, and may have incomplete nutrition. It is selectable without typing.
- **Food Selector**: the choice surface used for adding and replacing. It presents saved Library foods first and always offers an explicit search action, including when the Library is empty.
- **Edit Draft**: the pending changes to one entry: quantity, meal and replacement food. It exists only while the user is editing and is discarded without effect when the user cancels.
- **Nutrition Values**: calories, protein, carbohydrates and fat contributed by a food, a quantity, an entry or a meal. Each value is either known or unknown, never an invented zero.
- **Meal**: a fixed meal type, one of Breakfast, Lunch, Dinner or Snacks. A meal's detail groups the entries logged into that meal type for one day.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 percent of meal section and meal Add activations open the corresponding current-day meal detail; none open catalogue search.
- **SC-002**: A saved Library food with a proposed quantity is logged from Today in exactly three user actions and with zero characters typed.
- **SC-003**: 100 percent of confirmed adds and saved edits return the user to the meal detail containing the entry, with the entry and the meal and daily totals updated on that visit.
- **SC-004**: Repeating confirm or save five times in succession for one intended change produces exactly one entry and applies the change once, with no duplicate.
- **SC-005**: After moving an entry, the entry appears in exactly one meal, the origin meal no longer contains it, and both meal subtotals and the day's totals are correct in 100 percent of checks.
- **SC-006**: The meal detail, add, edit, move, replace and remove experience succeeds with no connection in 100 percent of covered scenarios that use saved Library foods or food data already available locally. New catalogue discovery retains its existing offline cache and connectivity behavior.
- **SC-007**: Every completed change is present after a restart, and every cancelled or unconfirmed change is absent in 100 percent of checks.
- **SC-008**: Cancelling an add or edit leaves the diary and all totals unchanged in 100 percent of checks.
- **SC-009**: No unknown nutrient is displayed or totalled as zero in any meal detail, entry, selector preview or confirmation state.
- **SC-010**: Every acceptance scenario in this specification has equivalent observable behavior on Android and iOS, with no platform-specific flow required to complete the task.
- **SC-011**: The explicit search action is reachable in every selector state, including an empty Library, empty search result and search failure, in 100 percent of checks.
- **SC-012**: Today's existing summaries and inline entries, the global add action, direct Library actions, Coach actions and the existing predictable back behavior show no regression in the feature's covered checks.

## Out of Scope

- **Library text filtering or searching.** The Library-first selector lists saved foods without a new text filter. Catalogue search remains the explicit way to find a food that is not saved.
- **Automatic catalogue fallback.** The app does not search the catalogue on the user's behalf when the Library is empty or when a saved food is missing.
- **A new add destination for global entry points.** The global add action, direct Library rows, product details, barcode entry and Coach keep their current behavior and landing.
- **Barcode capability inside the meal detail or selector.** Barcode scanning remains available through the existing catalogue search experience and is not relocated by this feature.
- **Date and time editing.** Entries keep their original day and logging time. Calendar navigation, backdating and changing an entry's time are not part of this feature.
- **Entry reordering.** The order of entries within a meal is not user-configurable in this feature.
- **New removal confirmation or undo.** Existing removal behavior is retained; this feature does not add a new confirmation or undo requirement.
- **Accounts, cloud sync, social features and manual food creation.** The feature uses the user's existing on-device Library and diary.
- **A redesign of the Today screen, catalogue search, Library destination, Coach or premium surfaces** beyond what is explicitly stated here.
- **Changes to nutrition calculation rules or to the set of tracked nutrients.**
- **Migration or repair of previously inconsistent diary data.**

## Assumptions

- The user is a single person using one device, with no account and no sync.
- A meal is one of the four fixed meals. The set is not user-editable in this feature.
- The app already stores the Library, the diary entries, each entry's original day and time, and frozen nutrition values. This feature reuses those existing records.
- Proposed quantities are produced by existing quantity-proposal rules rather than stored per food. The quantity confirmation reuses those rules. If no usable proposal results, the user chooses a valid quantity before confirming.
- The existing removal interaction is acceptable as-is and does not need a new confirmation or undo step.
- Existing catalogue search, product inspection, recent searches, barcode capability, the offline cache and the attribution remain available unchanged and remain the only way to find an unsaved food.
- The current day is the day shown by Today. Opening a meal detail from Today always addresses that current day's meal.
- The meal detail is reached from the Today meal sections and their Add actions. Other entry points named in the out-of-scope section are not redirected.
- Nutrition rules, including the distinction between unknown and zero, remain as already defined. This feature does not add a second data source or a new nutrient.
- Large Libraries are acceptable without text filtering for this release. The selector remains scrolled and ordered by the existing Library order, and filtering can be considered separately if usage shows it is needed.
- Both Android and iOS expose the Today, Library and More destinations and the existing add, search and entry-removal capabilities, which are the baseline for this feature. Any current gap between the platforms must be closed so that the equivalence requirements in this specification are met.

## Dependencies and Compatibility

### Dependencies

- The existing Today diary, meal sections, daily summaries, Library and entry-removal behavior.
- The existing add flow's food selection, quantity confirmation, product inspection and meal choice behavior.
- The existing catalogue search, offline cache, attribution and barcode capability, which this feature reaches only through the explicit search action.
- The existing on-device persistence of Library foods and diary entries, including frozen nutrition and original day and time.
- The existing navigation behavior that makes back predictable and keeps child screens out of tab selection.

### Compatibility and Supersession

- Spec 007 (app restructure) meal Add destination is superseded: a meal Add now leads to that meal's detail, not directly to food selection. Its other behavior, including Today summaries, inline entries, Library, search, scan, Coach and the global add action, remains in force.
- Spec 008 (intuitive navigation) post-add landing on the search step is superseded only for flows that begin at a Today meal Add or at that meal's detail. Those flows now land on the meal detail containing the entry. Spec 008's predictable back behavior, its rule against detours and its tab behavior remain in force.
- The Spec 009 (iOS parity) rule that a completed add returns to Today is superseded for flows that begin at a Today meal Add or at that meal's detail. The new meal detail and Library-first behavior becomes the parity target for both platforms. All other parity requirements remain in force.
- The prior specification files are not modified by this feature. Their superseded clauses are recorded here so that planning and review do not enforce two conflicting landing rules.

### Risks

- **Long Library lists without filtering**: offering every saved food without text filtering may make selection slow as the Library grows. This is an accepted scope limit; the selector must remain scrollable and stable, and filtering is a separate future decision.
- **Duplicate writes**: repeated confirmation or save is the most likely source of duplicate entries. The feature treats one completed activation as one result and must be verified under double taps and slow storage.
- **Rescaling versus replacing nutrition**: quantity changes must rescale the entry's frozen values, while food replacement must use the replacement food's values. Conflating the two would silently rewrite history or keep the wrong food's nutrition.
- **Moving entries between meals**: a move touches two meal groups, both subtotals and the day's totals. An incomplete move could show the entry twice or make it disappear.
- **Unknown versus zero**: a saved-only or incomplete food is the most likely place for an unknown nutrient to be rendered as zero, which would make the diary claim something it does not know.
- **Platform divergence**: the same flow must be delivered on both platforms. A native implementation that behaves differently would break the parity target.
- **Superseded navigation rules**: the earlier specifications still describe return-to-search and return-to-Today behavior. Reviewers must apply this feature's landing rule only to the flows named here and keep the earlier back behavior everywhere else.
- **Storage failure during a flow**: an offline device can fail to persist a change. The app must not show a successful update that did not survive.

## Clarifications

### Session 2026-10-26

- **Q1: What must editing an existing diary food support?**
  **Answer**: In one edit flow the user can change the quantity, change the meal, and replace the food with another Library or catalogue food. All three changes may be combined and saved once.
- **Q2: Where does the user land after adding a food or saving an edit?**
  **Answer**: On the meal detail containing that entry, with the updated entries and totals. If an edit moves the entry to another meal, the user lands on the destination meal's detail.
