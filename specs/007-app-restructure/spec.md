# Feature Specification: App Restructure into a Sellable Nutrition Tracker

**Feature Branch**: `007-app-restructure`
**Created**: 2026-10-21
**Status**: Draft
**Input**: User description: "Completely restructure the app. It must not be just a search box with a saved products section. It must be a sellable application, ready to offer to clients. Restructure the whole app and the functionalities it has, and fix the navigation flows that are failing (for example, navigating from saved products to the search screen shows the detail screen instead of the search screen). It must look like a production app ready to sell, not a child game."

## Problem statement

The repository is a well-engineered **product catalogue application with a purchase
scaffold**, not a nutrition tracker. It opens on a text field, holds a list of saved
products, and shows a third tab that says "Coming soon". A client evaluating it sees
a prototype. Four of the five features the brief describes do not exist.

Four concrete defects make it read as broken rather than unfinished:

| # | Defect | Evidence |
|---|---|---|
| D1 | The Saved to Search shortcut exists only in the empty state, so with any saved product there is no in-screen route to Search | `androidApp/.../savedproducts/SavedProductsScreen.kt` branches on `state.isEmpty` |
| D2 | The product detail back control lies about its origin, hardcoding "Back to search" even when opened from Saved | `androidApp/.../search/ProductDetailScreen.kt` |
| D3 | `openProductCode` is exposed by two screens and wired by neither, so a saved product can never be opened | `androidApp/.../App.kt` supplies no handler |
| D4 | Product detail is view-model state held outside the `NavHost` for the application lifetime, so a detail left open resurrects itself on tab return and the system back gesture closes the tab instead of the detail | `App.kt` holds the ViewModels; `AppShell` uses `popUpTo(start){saveState=true}` |
| D5 | The architecture test matches package roots that no source file uses, so the inward-dependency rule can never fail | `architecture-tests/.../ModuleArchitectureTest.kt` matches `com.nutrition.*`; the real roots are `com.example.nutricionalregister.*` |

This specification restructures the whole application around a **diary-first tracker
shell**, and fixes the navigation model by construction rather than by patch.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The day is the home (Priority: P1)

As a user, I want the app to open on my food log for today, so that I can see at a
glance what I have eaten and how it compares to my goals, and log the next thing in
one tap.

**Why this priority**: A nutrition tracker is a daily logging product. If the app
opens on a search box, the user has to remember to search before they can do the
one thing they came to do. The home screen is the product.

**Independent Test**: Launch the app and confirm the Today screen is shown with the
current date, the day's calorie and macro totals against goals, and the four meal
sections. Log an entry and confirm it appears in the right meal and the totals
update.

**Acceptance Scenarios**:

1. **Given** the app is launched, **When** the shell starts, **Then** the Today
   screen is the visible destination and the Today tab is the selected one.
2. **Given** the Today screen, **When** the user looks at the summary, **Then** it
   shows calories consumed against the calorie goal and the three macros against
   their goals, with a visual indication of progress.
3. **Given** a nutrient that only some of the day's foods report, **When** the
   user looks at the summary, **Then** that nutrient is never shown as zero: it
   is shown as a lower bound, the summary says which nutrients are incomplete,
   and a nutrient that no food reports at all is shown as unknown.
4. **Given** a meal with no entries, **When** the user views that meal section,
   **Then** the section shows an empty state with an add action for that meal.
5. **Given** entries exist for the day, **When** the user views the meal sections,
   **Then** each entry shows the product name, the logged quantity and that entry's
   calories, and the meal's section header shows the meal's calorie subtotal.
6. **Given** the user adds an entry, **When** the entry is saved, **Then** the
   Today screen shows it immediately and the daily totals reflect it.

---

### User Story 2 - Logging food is short and unambiguous (Priority: P1)

As a user, I want to log a food in a short, guided flow that always tells me which
meal I am logging into, so that logging takes seconds and never ends up in the
wrong place.

**Why this priority**: The add flow is the single most repeated interaction in the
app. Everything else in the product depends on it being fast and correct.

**Independent Test**: From the Today screen, use the add action on the Lunch
section, pick a product, set a quantity, confirm, and verify the entry lands in
Lunch with the right quantity and nutrients.

**Acceptance Scenarios**:

1. **Given** the user is on the Today screen, **When** they use the add action on
   a meal section, **Then** the add flow opens with that meal already selected.
2. **Given** the add flow is open, **When** the user is on the food-selection
   step, **Then** they can find a food by searching the catalogue or by picking one
   from their library.
3. **Given** a food is selected, **When** the user sets a quantity, **Then** the
   screen shows the resulting calories and macros for that quantity before the user
   confirms.
4. **Given** a quantity is set, **When** the user confirms, **Then** the entry is
   saved to the selected meal and the flow closes back to the screen that opened it.
5. **Given** the user is on the food-selection step with no query, **When** the
   step is shown, **Then** the library foods are offered immediately so the flow
   can be completed without typing.
6. **Given** the flow is open, **When** the user changes the meal, **Then** the
   meal is chosen in a modal sheet and changing it does not push a navigation
   entry onto the back stack.

---

### User Story 3 - My foods is a shortcut, not a bookmark list (Priority: P1)

As a user, I want the products I keep to be one tap away from being logged, so
that repeating the same meal takes seconds.

**Why this priority**: Frequent foods are the difference between a tracker people
abandon and one they keep. It also gives the existing saved-products feature a
real purpose in the product.

**Independent Test**: Save a product from Search, open the Library tab, and confirm
the product is listed with an add action that starts the add flow with that product
already selected.

**Acceptance Scenarios**:

1. **Given** the user has saved foods, **When** they open the Library tab,
   **Then** the foods are listed with their name, brand and image.
2. **Given** a food is listed, **When** the user uses its add action, **Then** the
   add flow opens with that food selected.
3. **Given** the library is empty, **When** the user opens the Library tab, **Then**
   a polished empty state explains the purpose and offers a way to find foods.
4. **Given** a food is listed, **When** the user removes it, **Then** it leaves the
   library and the change survives a restart.

---

### User Story 4 - Every navigation flow is correct and predictable (Priority: P1)

As a user, I want every screen to take me where I expect, and never to show me a
screen I did not ask for, so that the app is trustworthy.

**Why this priority**: The reported defect (tapping Search after leaving the
library shows a product detail) makes the app feel broken. Navigation is the
product's skeleton.

**Independent Test**: Open a product detail, switch tabs, switch back, and confirm
the tab's root screen is shown; press back on a child screen and confirm it returns
to the tab that opened it.

**Acceptance Scenarios**:

1. **Given** a product detail is open, **When** the user switches to another tab
   and back, **Then** the tab's root screen is shown, not the detail.
2. **Given** the user is on any tab, **When** they tap the tab that is already
   selected, **Then** nothing changes and no duplicate screen is added.
3. **Given** a child screen is open, **When** the user presses back, **Then** the
   screen that opened it is shown.
4. **Given** the user is on a tab other than Today, **When** they press back,
   **Then** the Today screen is shown.
5. **Given** the user wants to find a food from the Library tab, **When** they use
   the library's "find foods" action, **Then** the add flow opens on its search
   step.
6. **Given** the user is on a child screen, **When** the screen is shown, **Then**
   the bottom navigation bar is not shown, because a child screen owns the whole
   viewport.

---

### User Story 5 - The product reads as commercial software (Priority: P1)

As a client evaluating this app, I want every screen to look like a finished
product, so that the app is credible enough to sell.

**Why this priority**: The visual system is what a client judges in the first
thirty seconds. It is also the difference between a portfolio project and a
deliverable.

**Independent Test**: Review every screen in light and dark mode against the
design rules in this spec and confirm no screen looks like a prototype.

**Acceptance Scenarios**:

1. **Given** any screen, **When** it is viewed, **Then** it uses the shared colour,
   type, shape and spacing system with a clear visual hierarchy.
2. **Given** light and dark themes, **When** the app is used, **Then** both are
   legible, consistent and professional.
3. **Given** a list, card or empty state, **When** it is rendered, **Then** it
   follows the same spacing and surface rules as every other list, card and empty
   state in the app.
4. **Given** a screen with data, **When** it is viewed, **Then** the data is the
   most prominent element and the chrome recedes.
5. **Given** the app in its entirety, **When** every screen is reviewed, **Then**
   no screen is left on the pre-restructure visuals; the re-skin covers the whole
   application, not only the new screens.

---

### User Story 6 - A barcode is a shortcut to a logged food (Priority: P1)

As a user, I want to point my camera at a product and log it, so that logging
something I am holding takes one action instead of a search.

**Why this priority**: Logging in a supermarket is the moment the tracker is most
valuable and least convenient. A scan is the natural entry point for packaged
food, and the brief lists it as a core feature.

**Independent Test**: Grant the camera permission, scan a barcode whose product is
in the catalogue, and confirm the product is offered with its nutrition and can be
logged in one confirmation.

**Acceptance Scenarios**:

1. **Given** the user opens the scanner for the first time, **When** the
   permission is not yet granted, **Then** the app asks for the camera permission
   with a clear explanation of why it is needed.
2. **Given** the camera permission is denied, **When** the scanner is shown,
   **Then** it displays an explicit, actionable state that offers manual barcode
   entry instead of failing or crashing.
3. **Given** the scanner is running, **When** a barcode is decoded, **Then** the
   product is resolved from the catalogue and offered with its name and nutrition.
4. **Given** the scanned barcode is not in the offline catalogue cache, **When**
   the lookup completes, **Then** the app says so honestly and offers to search by
   name or to retry.
5. **Given** a product has been resolved, **When** the user confirms, **Then** the
   entry is logged into the selected meal.

---

### User Story 7 - I can talk to the coach and it logs for me (Priority: P2)

As a user, I want to tell the assistant what I ate in my own words and have it
log the food, so that I do not have to navigate a form every time.

**Why this priority**: Conversational logging is the feature that makes the
tracker feel intelligent, and it is the paid differentiator. It sits above the
daily loop, which must be solid first, but it is what the product is sold on.

**Independent Test**: Open the Coach, ask it to log a food that exists in the
catalogue, and confirm the entry appears in the diary with the right quantity and
meal.

**Acceptance Scenarios**:

1. **Given** the user opens the Coach for the first time, **When** the screen is
   shown, **Then** the app discloses that messages are sent to a third-party AI
   service and asks for consent before the first message is sent.
2. **Given** consent was given, **When** the user asks the assistant to log a
   food, **Then** the assistant proposes the food, the quantity and the meal, and
   the proposal is applied to the diary only after the user confirms it.
3. **Given** the assistant proposes an action, **When** the action is invalid, for
   example a food that does not exist or a negative quantity, **Then** the action
   is rejected with an explanation and nothing is written to the diary.
4. **Given** the assistant proposes removing or changing a logged entry, **When**
   the user confirms, **Then** the diary entry is removed or changed accordingly.
5. **Given** the device has no connectivity, **When** the user sends a message,
   **Then** the assistant says it needs a connection and does not pretend to work.
6. **Given** no AI service is configured for the build, **When** the user opens
   the Coach, **Then** the screen states plainly that the assistant is not
   configured and does not offer a dead input field.

---

### User Story 8 - Progress over time (Priority: P2)

As a user, I want to see how my week went, so that the daily logging adds up to
something I can act on.

**Why this priority**: The brief lists a weekly summary as a product feature. It
is valuable but it sits above the daily loop, which must be solid first.

**Independent Test**: Log entries across several days and confirm the weekly
screen shows per-day calories, the daily average, and that days with no entries
are shown as empty rather than as zero intake.

**Acceptance Scenarios**:

1. **Given** entries on some days of the last seven days, **When** the user opens
   the weekly summary, **Then** each of the seven days is shown with its calorie
   total, and days without entries are shown as empty.
2. **Given** the weekly summary, **When** the user views it, **Then** it shows the
   average daily calories over the days that have entries.
3. **Given** the user is on the Today screen, **When** the user opens the weekly
   summary, **Then** they can return to Today with back.

---

### User Story 9 - Goals, premium and settings (Priority: P2)

As a user, I want to set my own goals and manage my account, so that the app fits
me and is ready for daily use.

**Why this priority**: Goals make the daily summary meaningful and the premium
upsell has to live somewhere real.

**Independent Test**: Change the calorie goal in More, return to Today, and confirm
the summary reflects the new goal; confirm the premium purchase section is
reachable from More.

**Acceptance Scenarios**:

1. **Given** the user opens More, **When** they change a goal, **Then** the new
   goal is stored and the Today summary uses it.
2. **Given** the user opens More, **When** they look at the premium section,
   **Then** the purchase flow is reachable and behaves as it did before the
   restructure.
3. **Given** the user opens More, **When** they look at the about section, **Then**
   the app version and the Open Food Facts attribution are shown.
4. **Given** the user opens More, **When** they look at the privacy section,
   **Then** it states that assistant messages are sent to a third-party AI service.

---

### Edge Cases

- **A nutrient the catalogue does not report**: the entry is logged with that
  nutrient unknown. The day total for it is unknown, and the UI says "unknown"
  rather than "0 g". Zero and unknown are different claims.
- **One entry with a value and one without**: the total is the sum of the known
  values, reported as a lower bound, and the summary lists that nutrient as
  incomplete.
- **Day boundary crossed while the app is open**: the Today screen re-reads the
  current day; entries are bucketed by the local day they were logged in, and a
  new day starts with an empty diary.
- **Device time zone changed after logging**: entries keep the day they were
  logged in, so a past day never silently reflows into another one.
- **Daylight-saving transition**: days are stepped as calendar days through the
  platform's calendar API, never by adding 86 400 000 milliseconds, so a 23- or
  25-hour day is handled exactly.
- **Per-serving versus per-100 g basis**: quantity is always expressed in grams
  and nutrients are scaled from the basis the catalogue reported; a product whose
  basis cannot be resolved contributes no number rather than a wrong one.
- **Very large or fractional quantities**: quantities below zero are rejected,
  fractions are supported.
- **A product removed from the catalogue after being logged**: the logged entry
  keeps its snapshot of name and nutrients.
- **Offline**: logging, editing and removing entries, and the whole Today and
  Library experience work with no connection, because everything is on-device.
  The scanner still decodes offline; only the catalogue lookup for the decoded
  barcode may need a connection.
- **The camera is unavailable or the permission is permanently denied**: the
  scanner offers manual barcode entry and never crashes.
- **The assistant returns an action the app cannot honour**: the action is
  rejected with an explanation and the diary is untouched.
- **The assistant returns free text that looks like an instruction**: it is
  displayed as text and never executed.
- **Accessibility**: every icon-only control has a content description, every
  touch target is at least 48 dp, and the app supports large font scales.
- **Dark mode**: every surface, chart and progress indicator is legible in dark
  mode.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The app MUST open on a **Today** screen that shows the current date,
  the day's calorie and macro totals against goals, and the four meal sections
  (Breakfast, Lunch, Dinner, Snacks).
- **FR-002**: The system MUST let the user log a product with a quantity in
  grams, a meal, and a timestamp; an entry MUST record the product identity, the
  quantity, the meal and a snapshot of the nutrient values at log time.
- **FR-003**: The system MUST compute calories, protein, carbohydrates and fat
  per entry, per meal and for the day, in shared code, and MUST treat a nutrient
  that is not reported as unknown rather than as zero.
- **FR-004**: The system MUST let the user add an entry to any meal from the Today
  screen, from a meal section, from a product detail screen, from the Library, from
  a barcode scan, and from the Coach.
- **FR-005**: The system MUST offer an add-food flow in which the user selects the
  food (catalogue search, personal library, or barcode), sets a quantity, chooses
  the meal and confirms.
- **FR-006**: The add-food flow MUST show the calories and macros that the chosen
  quantity produces before the user confirms.
- **FR-007**: The system MUST present a **Library** of the user's saved foods, each
  with an action that starts the add-food flow with that food pre-selected.
- **FR-008**: The system MUST let the user remove a food from the library, and the
  removal MUST survive a restart.
- **FR-009**: The bottom navigation MUST expose exactly three destinations: Today,
  Library and More, each with an icon and a visible label, plus a floating action
  button as the global primary action.
- **FR-010**: Selecting a bottom-navigation destination MUST show that
  destination's **root** screen. A product detail or any other child screen MUST
  NOT be presented as the result of a tab selection.
- **FR-011**: The product detail MUST be a navigation route scoped to the screen
  that opened it, so that leaving the tab and returning shows the tab root, and
  back returns to the screen that opened the detail.
- **FR-012**: The system MUST preserve the existing catalogue search behaviour,
  including recent searches, the offline cache and the Open Food Facts
  attribution, and MUST reach it through the add-food flow and from the Library
  empty state.
- **FR-013**: A product detail screen MUST offer an **Add to diary** action that
  starts the add-food flow with that product pre-selected.
- **FR-014**: The system MUST offer barcode scanning within the add-food flow:
  it MUST request the camera permission at runtime with an explanation, MUST
  offer manual barcode entry when the permission is refused, MUST decode the
  barcode on device, MUST resolve the product through the existing catalogue
  repository, and MUST state honestly when the barcode is not in the offline
  cache.
- **FR-015**: The system MUST show a weekly summary of the last seven days with
  per-day totals, the average over days that have entries, and empty days shown
  as empty.
- **FR-016**: The system MUST let the user edit the daily calorie, protein,
  carbohydrate and fat goals from the More destination and MUST use them in the
  daily summary.
- **FR-017**: The system MUST keep the in-app purchase flow reachable from the More
  destination with its existing states (available, pending, owned, failed,
  restore). The purchase MUST be a single premium unlock and MUST NOT be offered
  on individual catalogue products.
- **FR-018**: The system MUST show the app version and the Open Food Facts
  attribution in the More destination.
- **FR-019**: All logged diary data MUST be stored on the device and MUST survive
  an app restart.
- **FR-020**: The system MUST support a schema migration from the existing
  database version without losing existing saved products or purchase records.
- **FR-021**: The visual system MUST use one shared colour, typography, shape and
  spacing definition across every screen, and EVERY screen in the application MUST
  be rebuilt on that system.
- **FR-022**: Every interactive control MUST have a content description when it
  conveys meaning through an icon alone, and MUST have a touch target of at least
  48 dp.
- **FR-023**: The app MUST be fully usable and legible in light and dark themes.
- **FR-024**: The system MUST provide a **Coach** screen in which the user can
  add, remove and change logged foods conversationally. The assistant MUST be
  reached through the application's own configured AI service, and every action
  it proposes MUST be validated against shared domain rules and applied only
  after the user confirms it.
- **FR-025**: The system MUST disable the assistant explicitly, with a clear
  "not configured" state, when no AI service address is configured for the build.
  It MUST NOT present a dead input field.
- **FR-026**: The system MUST disclose, before the first assistant message is sent
  and in the More destination, that assistant messages are sent to a third-party
  AI service.
- **FR-027**: The assistant MUST require connectivity and MUST say so plainly when
  it has none.
- **FR-028**: The system MUST NOT send the product catalogue, the recent search
  terms or the purchase records to the AI service. Only the user's own message and
  a minimal diary context MAY be sent, and only after consent.
- **FR-029**: The premium unlock MUST grant the assistant. A release build MUST
  require a verified entitlement to use it.
- **FR-030**: The meal picker MUST be a modal sheet and MUST NOT create a
  navigation entry.
- **FR-031**: The bottom navigation bar MUST be hidden on every child route, and
  its visibility MUST be derived from the route hierarchy rather than from a
  lookup that can fail silently.
- **FR-032**: The system MUST NOT contain any remaining reference to a per-food
  purchase, and MUST NOT offer a purchase action on a catalogue product.

### Non-Functional Requirements

- **NFR-001**: The shared `core:domain` and `core:data` modules MUST keep at least
  80 % line coverage, enforced by the existing Kover gate.
- **NFR-002**: The restructure MUST NOT introduce a new third-party dependency
  without a new ADR, per constitution rule III.
- **NFR-003**: The restructure MUST NOT weaken the module dependency policy or the
  architecture tests.
- **NFR-004**: The Today screen MUST render from on-device data without a network
  call.
- **NFR-005**: All business logic MUST stay in the shared KMP modules; the Android
  UI MUST NOT compute nutrition values.
- **NFR-006**: The restructure MUST NOT regress search, save, purchase or recent
  search behaviour.
- **NFR-007**: The iOS target MUST keep compiling; the shared modules MUST stay
  cross-platform and MUST NOT contain an Android-only API.
- **NFR-008**: The architecture test MUST match the real package roots so the
  inward-dependency rule is enforced rather than vacuously satisfied.
- **NFR-009**: The AI service address MUST be build configuration. No secret, key
  or credential MUST be committed to the repository.
- **NFR-010**: The application MUST NOT emit telemetry. The only network egress
  beyond the catalogue search MUST be the consented assistant path, and it MUST
  be documented as such.
- **NFR-011**: The scanner dependencies MUST stay in the Android application
  module, so the iOS framework is unaffected by construction.
- **NFR-012**: The release APK and the iOS framework sizes MUST be measured after
  the restructure and compared against the sizes recorded in ADR-0014 and
  ADR-0015.

### Key Entities

- **Diary Entry**: one logged food. Product code, product name, brand, image URL,
  meal, quantity in grams, unit, calories, protein, carbohydrates, fat, log
  timestamp, and the local day it belongs to. The nutrient values are a frozen
  snapshot and are individually nullable, where null means unknown.
- **Meal**: one of Breakfast, Lunch, Dinner, Snacks. A grouping of entries with a
  display label and a display order.
- **Nutrition Totals**: the sum of calories, protein, carbohydrates and fat over a
  set of entries, where a component is null when at least one contributing entry
  does not report it.
- **Nutrition Goals**: the daily targets for calories, protein, carbohydrates and
  fat. A single user-editable record.
- **Daily Summary**: the day's entries grouped by meal, with the day's totals and
  the remaining amounts against the goals.
- **Weekly Summary**: the last seven days, each with its calorie total, plus the
  average over the days that have entries.
- **Library Food**: a product the user keeps for quick logging. Reuses the
  existing saved-product record.
- **Barcode**: a scanned product code, and the outcome of resolving it against the
  catalogue: resolved, not in the offline cache, or not a known product.
- **Assistant Message**: one turn of the conversation, with the author (user or
  assistant) and the text.
- **Assistant Action**: a structured change the assistant proposes - log a food,
  remove an entry, change an entry's quantity or meal. Every action is validated
  before it can be applied.
- **Premium Entitlement**: whether the single premium product has been verified as
  purchased, which currently grants access to the assistant.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The app opens on the Today screen, and a first-time user sees the
  day's meals and totals without performing a search.
- **SC-002**: A user can log a food from the Today screen in three or fewer
  interactions (add, pick food, confirm), with the quantity defaulted from the
  product's serving.
- **SC-003**: Selecting a bottom-navigation destination never presents a child
  screen the user did not open; the reported "search shows a product detail"
  defect cannot be reproduced.
- **SC-004**: Every screen in the app uses the shared design system; a review of
  the app in light and dark mode finds no screen that reads as a prototype and no
  screen left on the pre-restructure visuals.
- **SC-005**: All unit tests, architecture tests, coverage gates, lint, ktlint and
  Android instrumented tests pass.
- **SC-006**: Diary data survives an app restart, and the daily totals recompute
  from the stored entries.
- **SC-007**: The shared `core:domain` and `core:data` modules keep at least 80 %
  line coverage.
- **SC-008**: A scanned barcode that is in the catalogue leads to a confirmed,
  logged entry in a single scan plus one confirmation.
- **SC-009**: The assistant can log a food the user names, and the diary entry it
  produces is identical to the one the manual add flow produces for the same food,
  quantity and meal.
- **SC-010**: The application contains no purchase affordance on any individual
  catalogue product, and exactly one premium unlock exists, reachable from More.
- **SC-011**: The iOS target still compiles and the shared framework still builds,
  and the scanner dependencies do not appear in it.

## Out of Scope

- **A first-run onboarding flow.** The app starts on Today with sensible default
  goals, and the goals are editable from More.
- **A deterministic, offline coaching engine.** The Coach surface is the
  assistant; rule-based insight cards are a follow-on feature.
- **Streaming assistant responses.** The assistant answers in one message.
- **A subscription plan.** The premium product is a one-time unlock. A
  subscription needs its own ADR and its own purchase-model migration.
- **Data export and platform health write-back.**
- **User accounts, cloud sync, social features, meal recommendations and manual
  product creation**, per the brief.
- **Weight tracking, body measurements and water intake.**
- **Weekly and monthly charts beyond the seven-day summary.**
- **A full SwiftUI rebuild of iOS.** The shared modules stay cross-platform and the
  iOS target keeps compiling.
- **A commercial store listing, marketing material or store-side configuration.**
  The purchase path remains as built in feature 004; publishing the paid product
  is still a store-side prerequisite, and the AI service is a deployment
  prerequisite.
- **Changing the `applicationId`**, the signing configuration or the store
  listing.

## Assumptions

- The user is a single person using the app on one device, with no account.
- A meal is one of the four fixed types; the set is not user-editable in this
  feature.
- Nutrition values come from the Open Food Facts catalogue already integrated in
  feature 002; this feature does not add a second data source.
- The default goals are sensible adult reference values and are editable from the
  More destination, so the summary is meaningful before the user configures
  anything.
- Entries are bucketed into the local day in which they were logged.
- The publisher will deploy the AI service that the assistant talks to, and will
  configure its address in the build. Until then the assistant is explicitly
  unavailable rather than silently broken.
- The store-side purchase configuration remains absent during development, so the
  premium flow is verified against fakes, exactly as it was in feature 004.

## Clarifications

### Session 2026-10-21 - round one

- **Q1: What does "restructure" mean here - a restyle or a new product model?**
  → **A) A new product model.** The user rejected the restyle of feature 005
  explicitly ("still look like a child game"). The restructure introduces the
  diary domain, meals, goals and summaries, not just new colours.
- **Q2: Which screen is the home?** → **A) Today**, the diary. The brief's
  product is a daily nutrition diary and the app must open on the user's day.
- **Q3: How many bottom-navigation destinations?** → **A) Four - Today, Search,
  Library, More.** A fifth (Scan) is deferred to the scanner feature so that no
  destination is a placeholder. **[Superseded in round two: three destinations
  plus a floating action button.]**
- **Q4: Where does search live?** → **A) Both.** Search keeps a top-level
  destination because it is a complete, tested feature, and it is also the search
  step of the add-food flow. **[Superseded in round two: search is no longer a
  top-level destination.]**
- **Q5: What happens to the existing "Saved products" tab?**
  → **A) It becomes the Library.** Same data, new purpose: one-tap logging of
  frequent foods.
- **Q6: Where does the in-app purchase live?** → **A) In More**, as a premium
  section, not inside the food list.
- **Q7: How is the broken navigation fixed?**
  → **A) Product detail becomes a scoped navigation route and tab selection
  always resets to the tab root.** Fixing only the tab switch would leave the
  shared ViewModel state as the underlying defect.
- **Q8: Are the entries editable and deletable?** → **A) Yes.** An entry can be
  removed from its meal section; quantity editing is available when the entry is
  opened.
- **Q9: Visual direction.** → **A) Calm, neutral, data-first.** A single
  restrained accent, strong type hierarchy, restrained radii, one spacing scale,
  minimal motion, dark mode included.
- **Q10: iOS scope.** → **A) The shared modules stay cross-platform and the iOS
  target keeps compiling; the SwiftUI screens are not rebuilt in this feature.**
  A full SwiftUI rebuild is its own feature.

### Session 2026-10-21 - round two

- **Q11: How should the AI chat assistant work?**
  → **A) A remote LLM reached over HTTP.** An on-device deterministic engine was
  rejected because it cannot handle free-form natural language. The assistant is
  reached through the application's own configured AI service, never by embedding
  a provider key in the app. See ADR-0016.
- **Q12: What does the Coach layer do?**
  → **A) The Coach is the assistant screen.** No separate rule-based insight
  engine is built in this feature.
- **Q13: Confirm the bottom navigation shape.**
  → **A) Three destinations - Today, Library, More - plus a floating action
  button.** Search is no longer a top-level destination; it is a step of the
  add-food flow. See ADR-0019.
- **Q14: Which extra surfaces belong in this feature?**
  → **A) Editable goals in More.** A first-run onboarding flow is not included.
- **Q15: Is barcode scanning in scope?**
  → **A) Yes.** `feature:scan` is implemented with on-device decoding and the
  camera permission is handled at runtime. See ADR-0017.
