# NutriLog

NutriLog is a Kotlin Multiplatform nutrition-diary project. The Android app
currently supports Open Food Facts product search, local result caching, and
recent-search history. Diary storage, nutrition calculations, scanning,
accounts, synchronization, and iOS product-search UI are not yet implemented.
Search terms and cached data remain on-device; no analytics or tracking is
used.

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

## Product search

Product Search submits queries explicitly to the Open Food Facts
Search-a-licious `GET /search` endpoint. It shows a saved snapshot before
refreshing, keeps the latest submitted query visible, stores products
independently from the 10 most recent distinct search terms, and allows a
history entry to be deleted without deleting its cached products. Cached
products and recent terms remain available offline; an offline query without
a cached result shows a distinct offline message with retry/edit actions.
Missing nutrition values are displayed as unavailable. Product images are
shown from the Open Food Facts catalogue using the image URLs the catalogue
already publishes for each product. Product images are licensed
[CC BY-SA](https://creativecommons.org/licenses/by-sa/3.0/); the app credits
Open Food Facts for both data and images with a link to the catalogue, and
shows a neutral placeholder when a product has no image or an image cannot be
loaded. The image decision, licence analysis, and exit strategy are recorded in
[`docs/adr/0009-product-images-open-food-facts.md`](docs/adr/0009-product-images-open-food-facts.md).
The Android UI provides product, nutrition, and image attribution to
[Open Food Facts](https://world.openfoodfacts.org); its community data may be
incomplete or inaccurate and is not medical advice.

The public API contract and deployment configuration are documented in
[`specs/002-product-search/contracts/search-a-licious.md`](specs/002-product-search/contracts/search-a-licious.md).
They do not publish a Search-a-licious-specific numerical rate limit,
incoming-client User-Agent or registration requirement, or guarantee that the
live index returns every configured field. The 10-search-requests/minute/IP
limit in the Open Food Facts API guide applies to different Product Opener
search endpoints and must not be assumed to apply here. Product Search sends
requests only after explicit user submission and uses a descriptive app
User-Agent; do not run production traffic or a live-service smoke test until
Search-a-licious-specific usage requirements and a representative response
contract have been confirmed. Automated tests use fixtures and never call the
live service.

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
Measurements taken after the product-search feature are recorded in the
validation records under `docs/adr/` and in
[the AI-assisted change log](AI-LOG.md). The under-10-minute CI target
remains pending its first hosted workflow run.
