# Research: Platform Skeleton

**Date**: 2026-10-19
**Scope**: Resolve implementation choices for `specs/001-platform-skeleton/spec.md`.

## Decisions

### Native platform UI with shared KMP logic

- **Decision**: Keep Android UI in Jetpack Compose and iOS UI in SwiftUI. Use KMP
  for shared domain, data, common abstractions, and feature state/reducer logic;
  do not share UI.
- **Rationale**: This is the approved project brief and the clarified product
  boundary. The existing Xcode project already calls
  `:shared:embedAndSignAppleFrameworkForXcode`, so preserve `:shared` as the
  Apple framework facade and export the required core/feature modules through
  that bridge while Gradle modules are reorganized.
- **Alternatives considered**: Keep the generated Compose Multiplatform UI
  shared across Android and iOS. Rejected because it conflicts with the brief's
  native SwiftUI requirement.
- **Compatibility evidence**: Kotlin's multiplatform DSL documents Android and
  Apple targets as supported targets. The Android-KMP plugin is Google's
  supported Android target plugin for KMP library modules.

### Gradle convention plugins

- **Decision**: Put reusable build conventions in an included `build-logic/`
  build and apply convention plugins from app and library modules.
- **Rationale**: Gradle documents included builds as a scalable location for
  convention plugins in multi-project builds and prefers convention plugins
  over cross-project `allprojects`/`subprojects` configuration.
- **Alternatives considered**: Keep all configuration duplicated in module
  build files, or use root cross-project configuration. Both make standards
  harder to audit and maintain.

### Module dependency enforcement

- **Decision**: Enforce declared Gradle project dependency edges in build
  verification, and use source architecture tests for package/layer rules.
  `core:domain` depends on no project modules; `feature-*` depends only on
  `core:domain`; `core:data` implements domain interfaces; `core:common` is
  independent with no consumers in this step. The `:shared` Apple-framework
  facade is an interoperability boundary outside the domain layering rules.
- **Rationale**: The user's clarification makes the boundary deterministic.
  Package-only checks cannot alone prove Gradle project dependencies are
  allowed, so validate both the build graph and source architecture.
- **Alternatives considered**: Check package names only (does not cover module
  edges), or permit domain to consume `core:common` (rejected by the selected
  boundary).
- **Dependency note**: If Konsist is selected for source architecture tests,
  document it in an ADR before adding it. It supports Kotlin architecture
  checks expressed as tests, including layer dependency rules.

### Coverage measurement

- **Decision**: Enforce the 80% line threshold for shared/core production code
  that runs on JVM/Android host tests; run iOS simulator tests independently
  without claiming they contribute to Kover's coverage percentage.
- **Rationale**: Kover supports JVM test coverage for Kotlin Multiplatform and
  Android projects, but does not collect coverage for Kotlin/Native tests.
  Keeping the metric scoped to measured code avoids false coverage claims.
- **Alternatives considered**: Claim a cross-platform coverage percentage from
  JVM-only reports (not supported by the selected tooling), or omit the
  threshold (conflicts with the project brief).
- **Dependency note**: Kover is a new build plugin; review an ADR before adding
  it. Re-check its compatibility with the chosen Kotlin, Gradle, and Android
  Gradle Plugin versions at implementation time.

### Linting

- **Decision**: Use Android Lint for the Android application, ktlint for Kotlin
  source across shared and Android modules, and `swift format lint` from the
  selected Xcode toolchain for Swift sources.
- **Rationale**: These checks cover the native platform code and shared Kotlin
  code without requiring a single tool to understand all three source
  languages. The Swift formatter is included in Xcode 16 and later; pin the
  macOS/Xcode runner image so its availability is reproducible.
- **Alternatives considered**: Rely on Android Lint alone (does not lint Swift
  or all shared Kotlin style), or introduce separate third-party linters for
  each language.
- **Dependency note**: ktlint is a new third-party tool and requires an ADR
  before adoption. Android Lint is supplied by the Android Gradle Plugin;
  `swift format` is supplied by the selected Xcode toolchain.

### Dependency injection

- **Decision**: Reserve Metro as the intended KMP dependency-injection choice
  for a later feature that needs object graph composition; do not install it in
  the platform skeleton without a real graph to wire.
- **Rationale**: The official Metro documentation describes Android and Apple
  targets and multiplatform graph support. The project brief prefers Metro.
  Confirm the selected release's Kotlin/compiler compatibility and record its
  required ADR before adding it.
- **Alternatives considered**: Koin, retained as the brief's fallback only if
  Metro's compatibility or build impact is unacceptable.

### CI topology and measurements

- **Decision**: Run required Linux Android/shared verification and macOS iOS
  build/test verification as parallel GitHub Actions jobs. Collect clean and
  incremental durations and release artifact sizes in the merge-to-main
  reporting path; establish baselines before enforcing size budgets.
- **Rationale**: GitHub-hosted runners offer Ubuntu and macOS environments, and
  Gradle's maintained `gradle/actions/setup-gradle` action configures Gradle
  across supported runner platforms. Parallel jobs make the under-10-minute
  target more achievable without skipping iOS.
- **Alternatives considered**: Run iOS on Linux (not supported), run all
  verification sequentially (increases wall time), or enforce an invented
  artifact budget before measuring a baseline.
- **Workflow requirement**: Pull requests require a submitted review event
  from a person other than the PR author, with approval not required. Required
  checks remain merge-blocking. Implement review activity as an explicit
  workflow/status gate rather than treating a zero-approval repository rule as
  proof that a review occurred.

### Version selection

- **Decision**: Treat the checked-in version catalog and wrapper as the
  migration baseline. Before feature implementation, compare Kotlin, Compose,
  Android Gradle Plugin, Gradle, JDK, and iOS/Xcode compatibility against their
  current stable official release documentation; update the catalog and
  wrapper together only when the matrix and local/CI builds support the
  upgrade.
- **Rationale**: The project already pins versions, and the platform has a
  coordinated Kotlin/Gradle/AGP/Compose toolchain. A piecemeal "latest" upgrade
  can break builds and is not necessary to define the architecture.
- **Alternatives considered**: Update every version to the newest release
  independently, which risks an incompatible toolchain.
- **Current repository evidence**: The version catalog currently pins Kotlin
  2.4.20, Compose Multiplatform 1.12.1, and AGP 9.1.1; the wrapper pins Gradle
  9.5.1 and a Java 21 toolchain. Official Kotlin documentation currently lists
  Kotlin 2.4.21, and Compose documentation currently lists 1.12.2. These are
  candidates for compatibility review, not automatic upgrade instructions.

## Sources

- Gradle, [Implementing Gradle Plugins / Convention Plugins](https://docs.gradle.org/current/userguide/implementing_gradle_plugins_convention.html)
- Gradle, [Gradle Wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
- Kotlin, [Multiplatform Gradle DSL reference](https://kotlinlang.org/docs/multiplatform/multiplatform-dsl-reference.html)
- Kotlin, [Multiplatform compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- Google, [Android-KMP library plugin](https://developer.android.com/kotlin/multiplatform/plugin)
- JetBrains, [Compose Multiplatform compatibility and versioning](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- JetBrains, [Kover Gradle Plugin](https://kotlin.github.io/kotlinx-kover/gradle-plugin/)
- LemonAppDev, [Konsist](https://github.com/LemonAppDev/konsist)
- Zac Sweers, [Metro Multiplatform](https://zacsweers.github.io/metro/latest/multiplatform/)
- GitHub, [GitHub-hosted runners](https://docs.github.com/en/actions/concepts/runners/github-hosted-runners)
- Gradle, [setup-gradle GitHub Action](https://github.com/gradle/actions/tree/main/setup-gradle)
- GitHub, [Workflow syntax](https://docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax)
- ktlint, [Kotlin linter](https://github.com/ktlint/ktlint)
- Swift, [swift-format](https://github.com/swiftlang/swift-format)
- Project, [Platform skeleton feature spec](spec.md)
- Project, [Project brief](../../docs/Project%20brief%20for%20Spec%20Kit%20KMP%20nutrition%20diary.md)
