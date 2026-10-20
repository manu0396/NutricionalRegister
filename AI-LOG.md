# AI-Assisted Development Log

Record AI-assisted changes so they can be reviewed and reproduced. Add one
entry per task or coherent task-ID commit. Include actual commands and results;
do not infer successful verification from generated files alone.

## Entries

### Session setup and Spec Kit artifacts

- **Scope**: Initialize the local Git repository and Spec Kit; create the
  platform-skeleton spec, clarifications, plan, research, data model,
  quickstart, and task breakdown.
- **AI assistance**: Copilot-assisted repository setup and Spec Kit
  documentation.
- **Review**: Pending maintainer review.
- **Commands run**: `specify check`,
  `.specify/scripts/bash/resolve-template.sh`,
  `.specify/scripts/bash/check-prerequisites.sh`,
  `.specify/scripts/bash/setup-plan.sh`, `./gradlew tasks --all`, and
  `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`.
- **Results**: Spec artifacts were validated; both existing Android-host and
  iOS simulator test tasks passed. No application implementation or CI
  workflow changes have been made.
- **Failures/corrections**: Planning discovered that the project constitution
  described shared Compose UI while the project brief requires native SwiftUI.
  Clarified this with the maintainer and amended the constitution to v1.1.0.

### Platform skeleton implementation (T008-T035, T038)

- **Scope**: Add the included Gradle convention build and standalone core,
  feature, application, and architecture-test modules; migrate Android to native
  Compose and iOS to native SwiftUI; add module/coverage/lint checks, CI
  workflows, contribution documentation, and initial local artifact baselines.
- **AI assistance**: Copilot-assisted Kotlin/Gradle implementation, native UI
  migration, CI workflow authoring, and documentation.
- **Review**: Pending maintainer review. No commit or push was made. Hosted
  GitHub Actions runs and repository branch-protection settings remain pending.
- **Commands run**:
  `./gradlew -p build-logic test`,
  `./gradlew clean :androidApp:assembleDebug :androidApp:assembleRelease :androidApp:lint :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :shared:assembleSharedReleaseXCFramework :architectureTest :coverageVerify :kotlinLint`,
  the same Gradle verification command without `clean`,
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build`,
  `swift format lint --strict --recursive iosApp/iosApp`,
  `ruby -e 'require "yaml"; Dir.glob(".github/workflows/*.yml").each { |file| YAML.load_file(file, aliases: true) }'`,
  and source/dependency scans for TODOs, tracking, analytics, and networking SDKs.
- **Results**: TestKit allowed/forbidden dependency tests, source architecture
  checks, Android debug/release builds, Android Lint, Android-host and iOS
  simulator tests, release XCFramework assembly, Kover conditional verification,
  Kotlin lint, Swift formatting, workflow YAML parsing, and the native SwiftUI
  app build passed locally. Kover verification skipped both core modules because
  they currently have no JVM-measurable production sources. The full clean
  Gradle validation/build took 53 seconds locally; the repeat took 2 seconds.
  Local artifact observations were 8,861,221 bytes for the unsigned Android
  release APK and 3,100 KiB for the iOS release XCFramework. These are local
  baselines, not GitHub-hosted pull-request timing results.
- **Failures/corrections**: Updated Konsist rules to the APIs available in
  version 0.18.1 and made KMP public visibility explicit. The Gradle dependency
  policy now checks declarable configurations and ignores generated classpath
  edges. KtLint initially flagged the Compose naming convention and generated
  Kotlin DSL accessors; the Compose suppression is localized and the included
  build uses a ktlint CLI task scoped to authored Kotlin and Kotlin DSL files.
  Removing all shared source initially prevented Gradle/Xcode from producing a
  framework, so the iOS app now uses a small tested common greeting API as a
  native interoperability example.
- **External verification remaining**: Configure
  `android-verification`, `ios-verification`, and `review-activity-head` as
  required branch-protection checks. Run the workflows on GitHub to verify the
  review-event Check Runs API permission and record actual hosted PR duration;
  the under-10-minute target is not claimed as met.

### PR CI SDK setup correction (T036 follow-up)

- **Scope**: Correct the Android SDK setup failure in both PR verification
  workflows.
- **AI assistance**: Copilot-assisted CI log diagnosis and workflow correction.
- **Review**: Pending maintainer review. This change is intended for PR #1.
- **Commands run**: `gh run view 39224714726 --log-failed`,
  `gh run view 39224714671 --log-failed`,
  `./gradlew :androidApp:assembleDebug :shared:iosSimulatorArm64Test
  --no-configuration-cache --console=plain`, and GitHub Actions YAML parsing.
- **Results**: Both hosted failures were traced to setup-android v3 requesting
  the retired SDK `tools` package. The workflows now use setup-android v4;
  Android explicitly requests platform 37.0 and Build Tools 36.0.0, while iOS
  sets up command-line tools without extra Android packages. Local Gradle
  targets and workflow YAML validation pass. Hosted reruns are still required.
- **Failures/corrections**: The Android platform package ID is
  `platforms;android-37.0`, not `platforms;android-37`; corrected after checking
  the installed SDK package metadata.

### PR CI Gradle plugin source correction

- **Scope**: Restore the missing module-dependency policy plugin and its
  TestKit coverage to the PR branch, and prevent `.gitignore` from hiding
  Kotlin source packages named `build`.
- **AI assistance**: Copilot-assisted hosted CI log diagnosis and repository
  ignore-rule correction.
- **Review**: Pending maintainer review. This change is intended for PR #1.
- **Commands run**:
  `./gradlew :build-logic:test :androidApp:assembleDebug
  :shared:iosSimulatorArm64Test --no-configuration-cache --console=plain`,
  `git diff --check`, and GitHub Actions workflow YAML parsing.
- **Results**: Both hosted workflows failed because
  `ModuleDependencyPolicyPlugin` was absent from the committed included build.
  The `**/build/` ignore pattern also matched the Kotlin package directory
  `com/nutrition/build`, so the plugin and TestKit test had not been committed.
  Added the source-tree exception and the previously omitted plugin and tests.
  The local Gradle TestKit, Android debug assembly, and iOS simulator test
  targets pass. Hosted Android run 39225953155 and iOS run 39225953154 both
  pass; PR durations were 4m30s and 8m30s respectively.
- **Failures/corrections**: Added an exception for package directories named
  `build` under source trees; the original broad ignore rule had hidden both
  files from the platform-skeleton commit.

### Product Search specification and clarification

- **Scope**: Clarify and plan the F1 product-search feature on
  `002-product-search`, covering name search, product details, empty/error and
  offline states, persistent recent searches, external API, and dependencies.
- **AI assistance**: Copilot-assisted Spec Kit authoring and interactive
  clarification.
- **Review**: Pending maintainer review. Specification and plan artifacts are
  uncommitted on `002-product-search`.
- **Commands run**: `.specify/scripts/bash/check-prerequisites.sh --json
  --paths-only`, `.specify/scripts/bash/setup-plan.sh --json`,
  `git diff --check`, and manual requirements/checklist review. Consulted the
  official Open Food Facts API overview and Search-a-licious OpenAPI.
- **Results**: Four clarification answers were incorporated: show cached
  matches immediately and refresh online in the background; retain the 10 most
  recent distinct terms; only the latest submitted query may update results;
  and allow individual recent-term deletion without deleting cached products.
  The quality checklist passes, with no unresolved clarification markers.
  Official documentation showed the v2 search endpoint is not the required
  free-text search; the maintainer selected Search-a-licious for the plan.
  Added the plan, research, data model, API contract, and validation quickstart.
  Search-a-licious rate limits, User-Agent policy, indexed field mapping, and
  reuse conditions remain explicit pre-implementation checks. Generated a
  56-task breakdown grouped by the three user stories, with ADR/service policy
  approval gates ahead of dependency adoption and network implementation.
- **Analysis remediation**: Specified that every completed non-blank query,
  including no-results and recoverable failures, enters recent history; added a
  persisted monotonic sequence for stable equal-timestamp ordering. Expanded
  the P1 MVP to include both US1 and US2, added an FR-010 privacy audit/test,
  and added hosted PR CI duration measurement against the 10-minute target.
- **Failures/corrections**: Corrected the initial v2 free-text assumption
  during API research. No implementation or new dependency was added.
- **Next**: Rerun `/speckit-analyze` before implementation;
  complete API policy checks and dependency ADR review before dependency
  adoption or live service requests.

### Product Search implementation approval gates

- **Scope**: Prepare dependency ADRs and verify documented compatibility
  evidence before starting product-search implementation.
- **AI assistance**: Copilot-assisted ADR drafting and public documentation
  review.
- **Review**: ADRs 0004–0008 are proposed and pending maintainer review; no
  dependency is approved.
- **Commands run**: `.specify/scripts/bash/check-prerequisites.sh --json
  --require-tasks --include-tasks` and `git diff --check`.
- **Results**: Prepared ADRs for coroutines/serialization, Ktor, SQLDelight,
  Metro, and Compose UI testing. The official Metro compatibility table lists
  Kotlin 2.4.20 as tested but not 2.4.21; forward compatibility is best-effort,
  so Metro compatibility with this repository remains unverified. The official
  Compose Multiplatform UI testing guide documents the Android test artifacts
  `ui-test-junit4-android:1.12.1` and `ui-test-manifest:1.12.1`; Gradle
  resolution still requires verification. Reviewed and recorded official
  Search-a-licious/Open Food Facts sources in the research and service
  contract: production/staging deployment hosts, endpoint, Open Food Facts
  data-reuse requirements, Product Opener-specific rate limits and User-Agent
  guidance, registration guidance, and candidate index fields.
- **Failures/corrections**: Search-a-licious-specific quota, client
  User-Agent/registration policy, and a live response-field guarantee are not
  published in the reviewed sources. The 10/minute/IP limit applies to separate
  Product Opener search endpoints and must not be assumed for Search-a-licious.
  T001 is complete as a documentary evidence review, not service approval;
  production traffic remains blocked pending those unknowns and response
  validation. T002–T006 are complete and T007 remains open. No dependencies,
  version-catalog entries, runtime code, or live network requests were added.
- **Next**: Resume `/speckit-implement` after maintainers review ADRs 0004–0008;
  keep production requests blocked until Search-a-licious-specific policy and
  a representative response contract are confirmed.

### Product Search quickstart validation (T052)

- **Scope**: Run the approved automated scenarios from
  `specs/002-product-search/quickstart.md` and record validation timings and
  performance-target status.
- **AI assistance**: Copilot-assisted validation execution and result
  recording.
- **Review**: Pending maintainer review.
- **Commands run**: the quickstart Gradle command
  (`:core:domain:allTests :core:data:allTests :feature:search:allTests
  :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :architectureTest
  :coverageVerify :kotlinLint`), `./gradlew :androidApp:assembleDebug
  :androidApp:connectedDebugAndroidTest`, plus the `README.md` verification
  tasks including `:androidApp:lint`.
- **Results**: All quickstart scenarios passed with deterministic fixtures
  (fake clocks, controlled dispatchers, SQLDelight test database, Ktor
  MockEngine) and no live-catalogue calls. The full verification run exited
  0; with a warm build cache the clean verification took 6 s and the
  incremental verification took 1 s. Android instrumented UI tests on the
  Medium_Phone emulator (API 16) passed 7/7 with 0 failures. No unmet
  performance target was identified in automated validation. Real-device
  cache/network timings against the live catalogue remain gated by the
  Search-a-licious service-policy check in the contract, so no live-service
  timing is claimed.
- **Failures/corrections**: None in this pass.

### Product Search cross-platform verification (T054)

- **Scope**: Verify Android-host and iOS compilation/tests, Android
  assembly and UI tests, architecture checks, coverage, and Kotlin lint for
  the completed product-search feature and record exact results.
- **AI assistance**: Copilot-assisted validation execution, iOS link-error
  diagnosis, and result recording.
- **Review**: Pending maintainer review.
- **Commands run**: the full module unit-test, `:architectureTest`,
  `:coverageVerify`, `:kotlinLint`, `:androidApp:assembleDebug`, and
  `:androidApp:lint` verification; `./gradlew -p build-logic test`;
  `./gradlew :androidApp:assembleDebug :androidApp:connectedDebugAndroidTest`;
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination
  'generic/platform=iOS Simulator' build`; and
  `swift format lint --strict --recursive iosApp/iosApp`.
- **Results**: Unit tests for all modules (including the shared iOS
  simulator target), architecture tests, `:coverageVerify`, `:kotlinLint`,
  `:androidApp:assembleDebug`, and `:androidApp:lint` all exited 0; the
  warm-build-cache clean verification took 6 s and the incremental
  verification took 1 s. Coverage gate: `core:domain` line coverage 100% and
  `core:data` 81.73%, meeting the constitution's at-least-80% requirement;
  `:coverageVerify` is green. Android instrumented UI tests passed 7/7 with
  0 failures on the Medium_Phone emulator (API 16): search accessibility,
  large text scale, search flow, recent-search ordering and deletion, empty
  response, distinct offline/cache/no-results messages, and error plus
  retry. Artifacts: unsigned release APK 11,035,730 bytes, debug APK
  15,265,427 bytes, androidTest APK 1,127,639 bytes, and
  `Shared.xcframework` approximately 92,000 KB on disk. iOS: `xcodebuild`
  reported BUILD SUCCEEDED with 0 errors and `swift format lint --strict`
  exited 0. `build-logic` TestKit tests, including the
  `ModuleDependencyPolicyPlugin` configuration-cache rewrite tests, are
  green.
- **Failures/corrections**: The iOS app initially failed to link SQLiter
  against `sqlite3`; added `OTHER_LDFLAGS` `-lsqlite3` to both Debug and
  Release configurations in `iosApp/iosApp.xcodeproj/project.pbxproj`, after
  which the build succeeded with 0 errors.

### Product Search privacy audit (T055)

- **Scope**: Verify that search terms and product queries are not sent to
  analytics/tracking and are not written to logs, using the regression tests
  in `feature/search/src/commonTest/kotlin/com/example/nutricionalregister/feature/search/SearchPrivacyTest.kt`
  plus a documented source/dependency audit of `androidApp`, `feature/search`,
  `core/data`, `core/domain`, and `shared`.
- **AI assistance**: Copilot-assisted audit and regression-test review.
- **Review**: Pending maintainer review.
- **Commands run**: `feature:search` unit tests as part of the full module
  verification, and source/dependency scans for analytics, tracking, and
  logging SDKs and call sites across the audited modules.
- **Results**: The `SearchPrivacyTest` regression tests pass: submitted
  terms reach only the approved search contract and the recent-history
  repository, blank queries never leave the ViewModel, and observable state
  emissions carry only search data. Audit finding: search terms flow only to
  `ProductSearchRepository.search(...)` and
  `RecentSearchRepository.recordCompletedSearch(...)`; no analytics,
  tracking, or logging SDK or call site exists in the audited modules.
- **Privacy audit verdict**: PASS. No search-term data leaves the app.

### Modern UI refresh with product images (T057-T073)

- **Scope**: Refresh the Android Compose search and product-detail surfaces into
  a coherent modern UI and add Open Food Facts catalogue product images (list
  thumbnails and a detail-view header), while preserving all existing
  product-search behavior, accessibility semantics, and privacy guarantees.
- **AI assistance**: Architect-assisted image-source and image-loader decision
  records; database-agent model/mapping change; frontend-agent theme and screen
  redesign; orchestrator verification.
- **Review**: Pending maintainer review.
- **Commands run**: `./gradlew :core:domain:testAndroidHostTest`,
  `:core:data:testAndroidHostTest`,
  `:core:domain:ktlintCommonMainSourceSetCheck`,
  `:core:data:ktlintCommonMainSourceSetCheck`, `:androidApp:compileDebugKotlin`,
  `:androidApp:ktlintCheck`, `:androidApp:connectedDebugAndroidTest`,
  `:androidApp:assembleRelease`, `:shared:assembleSharedXCFramework`,
  `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp ...`, and the full
  clean verification (`clean` then module `allTests`, `:architectureTest`,
  `:coverageVerify`, `:kotlinLint`, `:androidApp:assembleDebug`,
  `:androidApp:lint`).
- **Results**: Clean verification exited 0 in 24 s (coverage gate green).
  `assembleRelease` exited 0; `:shared:assembleSharedXCFramework` exited 0;
  `xcodebuild` BUILD SUCCEEDED with 0 errors. Instrumented Compose tests were 14
  of 14 passing (7 pre-existing + 7 new `ProductImageScreenTest` cases covering
  placeholder-on-absent-URL, whitespace-URL fallback, card name discoverability
  and description, Nutri-Score badge present/absent, detail image header +
  badge, and unsupported grade). Mapper tests for image fields (present, absent,
  blank) passed. Artifact sizes: release APK 11,365,002 bytes, debug APK
  15,693,131 bytes, `Shared.xcframework` (release) 92,008 KB.
- **Decisions recorded**: `docs/adr/0009-product-images-open-food-facts.md`
  (image source = Open Food Facts catalogue URLs, CC BY-SA, attribution and
  placeholder-on-failure) and `docs/adr/0010-compose-image-loading.md`
  (loader = Coil 3.6.3, Android-only, Apache-2.0, reuses existing OkHttp).
- **Failures/corrections**: None outstanding. Hosted PR duration remains pending
  task T056 (requires a branch push/PR).

### Professional design-system refactor (2026-10-20, second UI review)

- **Scope**: Address the user's report that the UI was still unprofessional "in
  all screens, overall the detail one".
- **AI assistance**: frontend-agent design-system refactor; orchestrator measured
  on-device diagnosis, source-level critique, and verification.
- **Review**: Pending maintainer review.
- **Root causes found (measured, not guessed)**: (1) `enableEdgeToEdge()` was on
  with no `Scaffold`/`TopAppBar`/`WindowInsets` handling, so titles drew at y=42
  (search) and y=70 (detail) — inside the status-bar band, overlapping the clock;
  (2) `DetailRow`/`NutritionRow` drew a divider *above* every row including the
  first; (3) the Nutri-Score badge was an unlabelled orphan and `StatRow` values
  were not aligned.
- **Commands run**: `adb`/`uiautomator` layout dumps to measure element bounds
  against the 0–63 px status bar; `./gradlew :androidApp:ktlintCheck`,
  `:androidApp:connectedDebugAndroidTest`, `:androidApp:assembleRelease`,
  `:shared:assembleSharedXCFramework`, `xcodebuild`, and the full clean
  verification.
- **Results**: added `ui/NutriLogScaffold.kt` (M3 `Scaffold` + `TopAppBar`),
  `ui/theme/NutriLogSpacing.kt`, `ui/SectionHeader.kt`, `ui/InfoChip.kt`,
  `ui/StatRow.kt`; detail screen restructured (hero 4:3, labelled Nutri-Score chip,
  dividers only between rows, right-aligned value column, accent-rule empty state,
  de-duplicated Brand/Quantity); result cards reserve a fixed badge slot.
  Instrumented tests 20/20 (no assertions relaxed); clean verification exit 0 in
  13 s; release APK 11,381,386 B; `Shared.xcframework` 92,024 KB;
  `xcodebuild` 0 errors. On-device confirmation: titles at y≈92/112 clear the
  status bar; every nutrient value ends at x=996; empty state renders without the
  em-dash tile.

### UI review follow-up: mapping, detail polish, universal images (2026-10-20)

- **Scope**: Address three user-reported issues after the initial refresh: the
  detail-screen back button style, whole-column "Unavailable" nutrition, and
  products with no image.
- **AI assistance**: database-agent mapping fix; frontend-agent detail/placeholder
  polish; orchestrator live-endpoint diagnosis and verification.
- **Review**: Pending maintainer review.
- **Commands run**: `curl` against the live Search-a-licious endpoint to confirm
  field shapes; `./gradlew :core:data:testAndroidHostTest`,
  `:core:data:ktlintCommonMainSourceSetCheck`, `:androidApp:ktlintCheck`,
  `:androidApp:connectedDebugAndroidTest`, `:androidApp:assembleRelease`,
  `:shared:assembleSharedXCFramework`, `xcodebuild`, and the full clean
  verification; plus `adb`/`uiautomator` walkthroughs on emulator-5556.
- **Root causes found**: Search-a-licious returns `brands` (and sometimes
  `quantity`) as JSON arrays, so a string-only mapper dropped them; Open Food
  Facts genuinely has no photo for many products.
- **Results**: mapper now joins array fields and normalizes `"unknown"`
  Nutri-Score to null (8 new mapper tests, all `core:data` green); detail back
  affordance is a compact bar (`contentDescription = "Back to search"`);
  nutrient amounts formatted without float noise (`formatAmount`, 4 tests);
  all-empty nutrition shows one message while partial data keeps per-value
  "Unavailable"; missing images render a deterministic generated gradient tile
  with the product initial. Instrumented tests 20 of 20 pass; clean verification
  exit 0 in 17 s; `xcodebuild` 0 errors. Live walkthrough confirmed brand and
  quantity populated, real nutrition values, the empty-state message, and
  placeholder tiles for image-less products.

### 003 documentation completion and Material icons (2026-10-20)

- **Scope**: Commit and push the 002 product-search feature; complete the missing
  003 UI-refresh documentation (research, data model, quickstart, requirements
  checklist, image contract); and close the one remaining implementation gap —
  the controls that used typographic glyphs instead of designed icons.
- **AI assistance**: orchestrator documentation pass plus frontend-agent icon
  integration; measured on emulator-5556.
- **Review**: Pending maintainer review.
- **Commands run**: `git add`/`git commit`/`git push` for 002;
  `curl` against Maven Central and Google Maven to verify icon-artifact
  coordinates; `./gradlew :androidApp:compileDebugKotlin`,
  `:androidApp:ktlintMainSourceSetCheck`, `:androidApp:connectedDebugAndroidTest`,
  `clean` + full verification gate + `assembleRelease` +
  `assembleSharedXCFramework`, and `xcodebuild`.
- **Results**: 002 committed as `feat(search): add offline-first product search`
  (89 files) and pushed to `origin/002-product-search`. 003 gained
  `research.md`, `data-model.md`, `quickstart.md`,
  `checklists/requirements.md` and `contracts/open-food-facts-images.md`; plan
  and spec cross-linked and the spec marked implemented. The JetBrains
  `material-icons-core` mirror does not resolve at the pinned Compose line
  (last release 1.7.3), so ADR-0011 adopts the AndroidX
  `androidx.compose.material:material-icons-core` (1.7.8); the `←`, `✕`, `⌕`
  glyphs became `ArrowBack`/`Close`/`Search` vector icons. Instrumented tests
  20/20; clean verification exit 0; release APK 11,463,514 B (was 11,381,386);
  `Shared.xcframework` unchanged at 92,024 KB; `xcodebuild` 0 errors.
- **Failures/corrections**: first attempted the JetBrains icon coordinates and
  hit `Could not find org.jetbrains.compose.material:material-icons-core:1.12.2`
  at dependency resolution; corrected to AndroidX coordinates and recorded the
  failed attempt and the reason in ADR-0011's validation record.

### Hosted pull-request CI measurement and convergence (2026-10-20)

- **Scope**: Close task T056 by measuring hosted pull-request CI on the pushed
  branch, and complete the 002 convergence tasks (docs bookkeeping plus the
  review-gate process conflict).
- **AI assistance**: orchestrator CI triage via `gh`, Spec Kit `/speckit-analyze`
  and `/speckit-converge` passes, documentation updates.
- **Review**: Pending maintainer review.
- **Commands run**: `gh pr checks 2`, `gh run list --branch 002-product-search`,
  `gh run view --log-failed`, `gh run watch`, `gh pr review --comment`.
- **Results**: hosted CI on PR #2 — **Android Verification passed in 268 s** and
  **iOS Verification passed in 467 s**, both under the project's 10-minute
  target, so T056 is closed. The **only** failing check is `review-activity`,
  which requires a submitted review from a login different from the PR author;
  the repository has a single collaborator, so the gate is unsatisfiable (a
  submitted `COMMENTED` self-review was attempted and correctly rejected by the
  check, run 39391340341). Convergence appended T057-T061 to the 002 task list.
- **Note on feature ownership**: the product-image surface now visible in the
  search result and detail screens belongs to feature 003 (ADR-0009/0010). The
  002 scope note that excluded product images is therefore superseded by 003, not
  contradicted by it.
- **Failures/corrections**: none in the build; the review gate failure is a
  process constraint, tracked as convergence task T057 (ADR + workflow change).
- **Base-branch finding (2026-10-20)**: the `review-activity` job uses the
  `pull_request_target` trigger, which runs the workflow file from the **base
  branch**, not the PR branch. Pushing the ADR-0012 workflow change to the PR
  branch therefore does not affect the check: the re-run (39392149834) still
  executed the old step and failed. The fix takes effect only once merged to
  `main`. Android Verification (5m17s) and iOS Verification (7m23s) remain green.
