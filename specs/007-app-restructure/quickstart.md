# Quickstart: App Restructure into a Sellable Nutrition Tracker

**Feature**: `007-app-restructure`
**Branch**: `007-app-restructure`
**Date**: 2026-10-21

## What this feature changes

The app stops being a catalogue browser with a save button and becomes a nutrition
tracker:

- It opens on **Today**, the diary of the current day.
- Meals (Breakfast, Lunch, Dinner, Snacks) are the spine of the app.
- A food can be logged with a quantity, and the day's totals update immediately.
- The saved products become a **Library** of foods that are one tap from being
  logged.
- The bottom bar has three destinations - **Today**, **Library**, **More** - and a
  floating action button for the one thing you do most: logging something.
- **Search** is no longer a destination; it is the food-selection step of the
  add-food flow, alongside your library and the barcode scanner.
- **More** holds the goals, the coach, the single premium unlock, the privacy
  notice and the about section.
- The product detail is a scoped navigation route, which removes the defect where
  tapping Search after leaving another tab showed a product detail.
- The visual system is replaced with a neutral, data-first, commercial design,
  applied to every screen.

## Run it

```bash
# Build and install on a connected device or emulator
./gradlew :androidApp:installDebug

# Unit tests (shared modules)
./gradlew :core:domain:allTests :core:data:allTests \
          :feature:search:allTests :feature:savedproducts:allTests \
          :feature:diary:allTests :feature:summary:allTests \
          :feature:assistant:allTests :feature:scan:allTests

# Instrumented tests
./gradlew :androidApp:connectedDebugAndroidTest

# Quality gates
./gradlew :architectureTest :coverageVerify :kotlinLint :plainTextCheck :androidApp:lint

# The migration chain verifies from schema version 1
./gradlew :core:data:verifySqlDelightMigration

# iOS still compiles
./gradlew :shared:assembleSharedXCFramework
```

## Walk through the new flow

1. Launch the app. The **Today** screen opens with today's date, the calorie and
   macro summary against the default goals, and the four meal sections.
2. Tap the add action on **Lunch**, or the floating action button. The add-food
   flow opens with Lunch already selected.
3. On the food step, choose a source: **Search** the catalogue, pick one from
   **My foods**, or **Scan** a barcode. The library is offered without typing, so
   a saved food logs in one tap.
4. On the quantity step, check the calories and macros the quantity produces, pick
   a multiplier or type a gram value, choose the meal in the sheet, and confirm.
5. The entry appears in the Lunch section and the daily totals update.
6. Tap the entry's remove action; the entry leaves the diary and the totals
   update again.
7. Open **Library**. The food you saved is listed with an add action, and the
   empty state opens the add-food flow on its search step.
8. Open **More**. Change the calorie goal, then return to **Today** and confirm
   the summary uses the new goal. The premium unlock, the privacy notice and the
   Open Food Facts attribution are here.

## Scan a barcode

1. In the add-food flow, choose **Scan**.
2. The app asks for the camera permission and says why it needs it.
3. Point the camera at the barcode. The product is resolved from the catalogue
   and offered with its nutrition.
4. If the barcode is not in the offline cache, the app says so and offers to
   search by name instead. If you refuse the permission, the scanner offers
   manual barcode entry.

## Use the coach

The coach talks to an AI service that the publisher deploys. Until that service
is configured for the build, the coach screen says plainly that it is not
configured.

1. Open **Coach** from the Today app bar or from More.
2. On first use, read the disclosure that messages are sent to a third-party AI
   service, and accept it.
3. Ask for something to be logged, for example "log two hundred grams of oats for
   breakfast".
4. The assistant proposes the food, the quantity and the meal. Nothing is written
   until you confirm the proposal.
5. The entry appears in the diary, identical to the one the manual flow produces.

The assistant needs a connection. Offline, it says so rather than pretending to
work.

## Verify the navigation fix

1. Open a product detail from any entry point.
2. Tap the **Library** tab.
3. Tap **Today**.
4. The **tab root** is shown, not the product detail. This is the reported defect
   and it is now covered by an instrumented regression test.

Also check:

- Tapping the tab that is already selected does nothing.
- Back on the product detail returns to the screen that opened it.
- Back on a tab other than Today returns to Today.
- The bottom bar is hidden on every child screen.

## Data and migration

The database moves from schema version 2 to version 3 with an additive migration.
Existing saved products, purchase records and the catalogue cache survive the
upgrade untouched. The schema snapshots for versions 1, 2 and 3 are committed, so
the whole chain verifies from v1:

```bash
./gradlew :core:data:verifySqlDelightMigration
```

To check the migration on a device with data, install the version built from
`main` first, save a product, then install the debug build of this branch and
confirm the saved product is still there.

## Configure the AI service

The assistant address is build configuration and defaults to empty, which
disables the assistant with an explicit message rather than breaking it. To point
the app at a gateway, set the property in `local.properties`:

```properties
NUTRILOG_AI_PROXY_URL=https://your-gateway.example.com
```

The provider key is never part of the app and never part of this repository. The
gateway contract is specified in ADR-0016.

## Running the app

The Coach is the assistant screen and it is reachable from the whole app: the
shell keeps a small assistant action next to the add-food action on every
destination and inside its child routes, and it is hidden only while the Coach
itself is on the screen.

A build without the gateway address has no Coach, and the screen says so. To
turn the assistant on, the publisher deploys the gateway and passes its
address as a Gradle property, which becomes a generated string resource the
app reads at start-up:

```
./gradlew :androidApp:assembleDebug -PNUTRILOG_AI_PROXY_URL=https://gateway.example.com
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

A debuggable build treats the premium as unlocked, so the Coach can be
exercised before a purchase exists; a release build ignores it and gates the
Coach on the store entitlement.

## Where the code is

| Area | Location |
| --- | --- |
| Domain | `core/domain/.../domain/diary/`, `domain/navigation/`, `domain/assistant/` |
| Persistence | `core/data/.../sqldelight/.../DiaryEntry.sq`, `2.sqm`, `databases/`, `DiaryRepositoryImpl.kt` |
| Assistant transport | `core/data/.../assistant/AiGatewayClient.kt` |
| Diary state | `feature/diary/.../feature/diary/` |
| Summaries | `feature/summary/.../feature/summary/` |
| Coach state | `feature/assistant/.../feature/assistant/` |
| Barcode resolution | `feature/scan/.../feature/scan/` |
| Shell and navigation | `androidApp/.../AppShell.kt`, `core/domain/.../domain/navigation/` |
| Design system | `androidApp/.../ui/theme/`, `androidApp/.../ui/components/` |
| Screens | `androidApp/.../today/`, `addfood/`, `library/`, `more/`, `coach/`, `weekly/`, `scan/` |
