# Quickstart: Platform Skeleton Validation

Use this guide after implementation. Commands run from the repository root.

## Prerequisites

- JDK 21 (the repository's Gradle daemon toolchain).
- Android SDK matching the version catalog and an Android-capable environment.
- Xcode and iOS simulator support for native iOS builds and tests.
- GitHub Actions access to Ubuntu and macOS hosted runners.

## Local Android and Shared Verification

```sh
./gradlew :androidApp:assembleDebug
./gradlew :shared:testAndroidHostTest
./gradlew :architectureTest
./gradlew :coverageVerify
./gradlew :androidApp:lint
./gradlew :kotlinLint
```

Expected result: Android app assembly, shared Android-host tests, Gradle module
and source architecture checks, the conditional JVM coverage rules for
`core:domain` and `core:data`, Android Lint, and Kotlin lint all pass. The
coverage tasks skip modules without JVM-measurable production sources.

## Local iOS Verification (macOS)

```sh
./gradlew :shared:iosSimulatorArm64Test
swift format lint --strict --recursive iosApp/iosApp
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' \
  build
```

Expected result: iOS simulator tests and Swift formatting checks pass, and the
native SwiftUI application builds with the KMP framework facade through the
existing Xcode build phase.

## Pull Request CI Acceptance Scenarios

1. Open a pull request and verify that Android, iOS, and review-activity checks
   start. Configure `android-verification`, `ios-verification`, and
   `review-activity-head` as required checks in repository branch
   protection/rulesets.
2. Confirm that Android build, shared tests, Android Lint, Kotlin lint,
   architecture, and coverage checks and the iOS app build, simulator tests,
   and Swift format lint report independently; any required job failure blocks
   a passing result.
3. Submit a review from a contributor other than the PR author. Confirm the
   review-activity gate passes without requiring an approval.
4. Temporarily introduce a forbidden Gradle project dependency in a local test
   change. Confirm architecture verification fails with the offending edge.
5. Merge a successful change to `main`. Confirm the build-duration and artifact
   size report is published and initial observations are identified as
   baselines, without claiming an unconfigured size budget.
6. Confirm the full required pull-request workflow completes within 10 minutes.

## Reports

- Kotlin/Android host coverage: `./gradlew :coverageVerify` checks each populated
  core module. Generate HTML reports with
  `./gradlew :core:domain:koverHtmlReport :core:data:koverHtmlReport` when the
  modules contain JVM-measurable production source.
- Build timing and artifact sizes: CI summary or uploaded workflow artifact.
- iOS-native tests are reported separately; they are not included in the Kover
  percentage.
