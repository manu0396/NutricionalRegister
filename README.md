# NutriLog

NutriLog is a Kotlin Multiplatform nutrition-diary project. This repository
currently establishes the platform foundation; it does not yet implement diary
storage, nutrition calculations, scanning, search, accounts, or synchronization.
Diary data is intended to remain on-device, with no analytics or tracking.

## Architecture

- `androidApp` is the native Android application and owns the Jetpack Compose UI.
- `iosApp` is the native SwiftUI application.
- `shared` is the Apple framework facade used by the iOS app, not a UI or
  business-logic layer.
- `core:domain` defines domain behavior and has no project-module dependencies.
- `core:data` implements domain-facing data behavior and depends only on
  `core:domain`.
- `feature:search`, `feature:scan`, `feature:diary`, and `feature:summary` are
  independent presentation modules that depend only on `core:domain`.
- `core:common` is intentionally standalone and has no consumers until a later
  specification permits them.
- `architecture-tests` and the Gradle module dependency policy enforce source
  and project-module boundaries.

The version catalog in `gradle/libs.versions.toml` is the central dependency
inventory. Before adopting a third-party dependency, record its need,
alternatives, binary-size impact, build-time impact, licence, and exit strategy
in a decision record under `docs/adr/`.

## Prerequisites

- JDK 21 (the pinned Gradle daemon toolchain).
- Android SDK for Android build and lint tasks.
- Xcode and iOS Simulator support for iOS builds and tests.

## Build and verify

From the repository root:

```sh
./gradlew :androidApp:assembleDebug
./gradlew :shared:testAndroidHostTest
./gradlew :shared:iosSimulatorArm64Test
./gradlew :androidApp:lint
./gradlew :architectureTest
./gradlew :coverageVerify
./gradlew :kotlinLint
swift format lint --strict --recursive iosApp/iosApp
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' \
  build
```

Kover measures JVM/Android-host coverage only. It enforces at least 80% line
coverage separately for `core:domain` and `core:data` when those modules contain
JVM-measurable production code; it does not claim Kotlin/Native coverage.
Architecture checks include a Gradle TestKit negative case that confirms a
forbidden dependency fails with an actionable edge.

The equivalent pull-request checks run in `.github/workflows/android.yml` and
`.github/workflows/ios.yml`. The review-activity gate is
`.github/workflows/review-gate.yml`; a submitted review from someone other than
the pull-request author is required, but approval is not. Repository
administrators must mark `android-verification`, `ios-verification`, and
`review-activity-head` as required checks in branch protection/rulesets and
require pull requests to merge to `main`. The review workflow writes the
stable `review-activity-head` check directly to the pull-request head commit.
Do not require an approval for this policy.

## Testing and contribution records

Read [the test policy](docs/testing.md), [the platform quickstart](specs/001-platform-skeleton/quickstart.md),
and [the AI-assisted change log](AI-LOG.md). Use one atomic commit per
implementation task and include its task ID in the commit message. All changes
to `main` must arrive through a pull request with recorded review activity.

The `Mainline Build Metrics` workflow measures clean and incremental Android
release/iOS framework build durations and reports the Android APK and iOS
XCFramework sizes. Initial measurements are baselines; no artifact-size budget
is set yet. The local macOS baseline on 2026-10-19 was 53 seconds for the clean
Gradle validation/build and 2 seconds incremental; the unsigned
Android release APK was 8,861,221 bytes and the iOS release XCFramework was
3,100 KiB. These are local measurements, not GitHub-hosted PR workflow timing.
The under-10-minute CI target remains pending its first hosted workflow run.
