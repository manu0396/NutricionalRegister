# Quickstart: Saved Products Bottom Navigation and In-App Purchase

**Feature**: `004-saved-products-navbar` | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

**Date**: 2026-10-20

## Prerequisites

- JDK and the Gradle wrapper (`./gradlew`, Gradle 9.5.1).
- Android SDK for `compileSdk 37`, with `minSdk 24`; an emulator (e.g.
  `emulator-5556`) for instrumented tests.
- Xcode with an iOS simulator and Kotlin/Native toolchain for the iOS checks.
- **Payment test environments** (required to exercise a real purchase flow):
  - Android: an app build uploaded to a Play Console **internal testing** track
    with the tester account added as a **licensing tester**, plus the paid
    product defined in the store.
  - iOS: an Xcode **`.storekit`** configuration file with the product defined, and
    a sandbox Apple ID.
  - Without these, the purchase path is built and unit-tested against fakes but
    **cannot be completed end-to-end**; that limitation is expected and reported.

## 1. Build everything

```bash
./gradlew clean
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`; a debug APK under
`androidApp/build/outputs/apk/debug/`.

## 2. Run the verification gate

Unit/feature tests, architecture tests, coverage, lint, plain-text check, debug
build and Android lint:

```bash
./gradlew \
  :core:domain:allTests :core:data:allTests \
  :feature:search:allTests :feature:savedproducts:allTests \
  :shared:testAndroidHostTest :architectureTest :coverageVerify :kotlinLint \
  :plainTextCheck :androidApp:assembleDebug :androidApp:lint
```

Expected: `BUILD SUCCESSFUL`, exit 0, coverage gate (>=80% line) green, and
`plainTextCheck` reporting no emoji.

> Note: `:core:domain:test` and `:core:domain:ktlintMainSourceSetCheck` are
> ambiguous in this project. Use `:core:domain:testAndroidHostTest` and
> `:core:domain:ktlintCommonMainSourceSetCheck`.

### Measured results (2026-10-21)

- Local clean verification gate: `BUILD SUCCESSFUL` in ~30 s (434 tasks).
- `:core:domain` and `:core:data` coverage gates pass at the 80% line bound.
  The Android Play Billing client (a thin bridge over the live Billing SDK) is
  excluded from the `:core:data` coverage gate because its branches need a real
  store connection; its pure BillingResult -> PurchaseFailure mapping remains
  covered (see ADR-0014).
- `:androidApp:connectedDebugAndroidTest`: 39/39 passed, 0 failed on
  emulator-5556 (~7 min).
- `:shared:iosSimulatorArm64Test` and `:shared:assembleSharedXCFramework`:
  `BUILD SUCCESSFUL`; `Shared.xcframework` 93,836 KB.
- `xcodebuild -scheme iosApp` (iOS Simulator): `BUILD SUCCEEDED`, 0 errors.
- Release APK: 12,520,644 bytes; debug APK: 17,167,130 bytes.

### Hosted CI results (T058, 2026-10-21, PR #3)

The plain-text gate failed on the first push because `ripgrep` is not guaranteed
on the macOS runner and because the `rg` scan of `.` silently skips hidden
directories (`.github`, `.specify`). `scripts/check-no-emoji.sh` now derives its
file set from `git ls-files -z` (skipping binaries by a NUL test), excludes
vendored spec-kit tooling (`.github/skills/**`, `.specify/templates/**`) that the
project does not author, and falls back to a portable `perl -CSD` scan when `rg`
is absent; `ios.yml` also installs `ripgrep`. After the fix, launched from a
clean checkout on GitHub-hosted runners:

- `android-verification`: **pass**, 5m55s.
- `ios-verification`: **pass**, 5m7s.
- `review-activity` / `review-activity-head`: fail by design — the independent
  human review gate is not satisfiable for a solo maintainer (see ADR-0012);
  it is unrelated to the verification result.

Both verification jobs are within the project's under-10-minute target.

## 3. Run the Android instrumented tests

```bash
./gradlew :androidApp:connectedDebugAndroidTest
```

Expected: bottom bar renders three destinations; current destination is
indicated; detail→back returns to origin; Diary shows the placeholder; saved
products render offline; purchase states (Idle/Pending/Purchased/Cancelled/
Failed) each render correctly; accessibility and large-text checks pass.

## 4. Run the iOS checks

```bash
./gradlew :shared:assembleSharedXCFramework
./gradlew :shared:iosSimulatorArm64Test
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' build
```

Expected: XCFramework builds, shared iOS simulator tests pass, and
`xcodebuild` reports `BUILD SUCCEEDED` with 0 errors. The Swift `StoreKit 2`
bridge compiles.

## 5. Verify the feature manually

Install and launch:

```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n com.example.nutricionalregister/.MainActivity
```

Then confirm:

1. **Bottom bar** — Search, Saved products and Diary are visible; the current one
   is clearly indicated; the bar does not overlap content or the system
   navigation area (test with gesture navigation).
2. **Save and revisit** — search a product, save it, open **Saved products** and
   see it; open it to view details; return and confirm you are back on Saved
   products, not Search.
3. **Offline persistence** — enable airplane mode, force-stop and reopen the app;
   saved products are still listed.
4. **No duplicates / removal** — save the same product again and confirm the list
   is unchanged; remove it and confirm it disappears and does not return from a
   new search.
5. **Diary placeholder** — selecting Diary shows a clear "not yet available"
   state, not a blank screen.
6. **Empty saved list** — with nothing saved, open **Saved products** and confirm
   a "no saved products yet" message appears together with a shortcut that takes
   you to Search (never a blank screen).
7. **Purchase (test environment)** — with a licensing tester / `.storekit`
   config, start a purchase and confirm the **platform's own payment sheet**
   appears, the app asks for no card details, a completed purchase is confirmed
   and survives restart, and a cancelled purchase leaves no record.
8. **In-progress purchase** — observe that between confirming payment and the
   store returning a result the purchase control shows a "purchase in progress"
   state and cannot be activated a second time.
9. **Payments unavailable** — with the store account signed out or unconfigured,
   start a purchase and confirm an explicit "payments unavailable" state appears
   with guidance and a retry, not a silent failure.
10. **Reconcile on launch (store is source of truth)** — complete a purchase,
    force-stop the app, and reopen it while offline; the product the store reports
    as owned is shown as purchased, and no product is ever shown as purchased
    twice.
11. **Accessibility** — set the largest system text size and confirm every bar
    label and purchase control remains readable and selectable (48 dp targets).

## 6. Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `:core:domain:test` ambiguous task | Use the explicit task names in §2's note. |
| Purchase action shows "not configured" | Expected without store-side paid products / testers; the UI must show the explicit "payments unavailable" state (§5 step 9), not a silent failure. |
| No payment sheet appears | Confirm the product is defined in the store and the account is a tester; check the store's test configuration. |
| Instrumented tests find no device | Start an emulator; confirm with `adb devices`. |
| iOS simulator tests cannot resolve symbols | Run §4 step 1 first so the XCFramework exists. |
| Saved products empty after upgrade | Confirm the SQLDelight migration ran; do not recreate the database. |
