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
  targets pass; clean hosted reruns are pending.
- **Failures/corrections**: Added an exception for package directories named
  `build` under source trees; hosted verification remains pending.
