# Quickstart: Modern UI Refresh with Product Images

**Feature**: `003-ui-refresh` | **Plan**: [plan.md](plan.md) | **Spec**: [spec.md](spec.md)

**Date**: 2026-10-20

## Prerequisites

- JDK and Gradle wrapper available (Gradle 9.5.1 via `./gradlew`).
- Android SDK with the platform required by `compileSdk 37`, and `minSdk 24`
  for devices/emulators.
- Xcode with an iOS simulator for the iOS build check (macOS only).
- Optional: a running Android emulator for the instrumented tests, e.g.
  `emulator-5556` (any API-appropriate AVD works).

## 1. Build everything

```bash
./gradlew clean
./gradlew :androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, a debug APK under
`androidApp/build/outputs/apk/debug/`.

## 2. Run the full verification gate

The same set the feature is validated against (unit tests, architecture tests,
coverage gate, lint, debug build and Android lint):

```bash
./gradlew \
  :core:domain:allTests :core:data:allTests :feature:search:allTests \
  :shared:testAndroidHostTest :architectureTest :coverageVerify :kotlinLint \
  :androidApp:assembleDebug :androidApp:lint
```

Expected: `BUILD SUCCESSFUL`, exit code 0, coverage gate (≥80% line) green.

> Note: `:core:domain:test` and `:core:domain:ktlintMainSourceSetCheck` are
> ambiguous in this project. Use `:core:domain:testAndroidHostTest` and
> `:core:domain:ktlintCommonMainSourceSetCheck` when targeting those modules
> directly.

## 3. Run the Android instrumented UI tests

With an emulator running:

```bash
./gradlew :androidApp:connectedDebugAndroidTest
```

Expected: **20 tests, 0 failed**, covering query entry/submission, result and
detail navigation, recent-search ordering and deletion, outcome states,
accessibility (labels and 48 dp targets), large text, image present/absent,
Nutri-Score badge, and the all-empty vs partial nutrition cases.

Tests are deterministic and **offline**: they use a fake image loader and never
call the Open Food Facts image CDN.

## 4. Run the iOS build check

```bash
./gradlew :shared:assembleSharedXCFramework
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' build
```

Expected: `BUILD SUCCEEDED` with 0 errors. The iOS UI is out of scope for this
feature; this verifies that the shared model change (two optional fields) does
not break the iOS framework.

## 5. Verify the feature manually

Install and launch:

```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n com.example.nutricionalregister/.MainActivity
```

Then confirm:

1. **Search screen** — the app title sits *below* the status bar (not underneath
   the clock); the search field and a full-width Search button are clear and
   thumb-sized; recent searches appear as intentional rows with a delete control;
   the Open Food Facts attribution is visible.
2. **Results** — each card shows a thumbnail (real photo where the catalogue has
   one, a deterministic generated tile otherwise), the product name, its
   brand/quantity, its code, and a Nutri-Score badge when a grade exists. Cards
   share aligned edges and are all the same width.
3. **Product detail** — a back control (`Back to search`) and the title clear the
   status bar; a hero image; the product name and brand/quantity; a labelled
   Nutri-Score chip; a "Product info" card; a "Nutrition information" card whose
   values are right-aligned in a clean column and formatted without floating-point
   noise (e.g. `7.69`, not `7.6899999999999995`).
4. **Missing data is graceful** — a product with no nutrition at all shows one
   message ("Nutrition information not available for this product") rather than a
   column of "Unavailable"; a product with partial nutrition shows real values and
   flags only the genuinely missing nutrients.
5. **Every product has an image area** — search for a product with no catalogue
   photo (e.g. `water`) and confirm a styled generated tile, never a blank or
   broken image.
6. **Offline behaviour is unchanged** — enabling airplane mode and re-running a
   recent search still shows cached results with the offline message.

## 6. Useful diagnostic commands

Dump the real layout bounds of the running app (used during this feature to prove
that content clears the system bars and that the value column aligns):

```bash
adb shell uiautomator dump /sdcard/ui.xml
adb pull /sdcard/ui.xml -
```

Compare top-of-screen `bounds` against the status-bar inset reported by:

```bash
adb shell dumpsys window displays | grep -iE "statusBar|inset"
```

## Troubleshooting

| Symptom | Cause / fix |
|---|---|
| `:core:domain:test` ambiguous task error | Use the explicit task names in §2's note. |
| Instrumented tests fail to find a device | Start an emulator or attach a device; confirm with `adb devices`. |
| Images never appear but the app works | Expected offline or when the catalogue has no photo; the generated placeholder is the designed fallback, not an error. |
| Coverage gate fails | Run `./gradlew :coverageVerify` and inspect the Kover report before assuming a regression in this feature. |
| `xcodebuild` fails on a fresh machine | Ensure the XCFramework was assembled first (§4, step 1) and that the simulator destination is available. |
