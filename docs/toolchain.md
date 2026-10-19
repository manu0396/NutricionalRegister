# Build Toolchain

Validated on 2026-10-19 using the project's wrapper and the installed macOS
toolchain.

| Component | Pinned version | Evidence |
|---|---:|---|
| Kotlin Gradle Plugin / Kotlin | 2.4.21 | Stable bug-fix release listed by the Kotlin release documentation. |
| Compose Multiplatform | 1.12.2 | Stable release listed by Compose Multiplatform compatibility documentation; compatible with the current Kotlin line. |
| Android Gradle Plugin | 9.1.1 | Existing stable project pin; Android target uses Google's `com.android.kotlin.multiplatform.library` plugin for KMP libraries. |
| Gradle wrapper | 9.5.1 | Existing pinned wrapper; verified to launch on the configured Java 21 runtime. |
| Gradle daemon JDK | 21 | Existing daemon toolchain; local Java 21 and Xcode 26.2 were available during validation. |
| iOS toolchain | Xcode 26.2 (17C52) | Shared iOS simulator tests and the native SwiftUI app build passed after the Kotlin/Compose and KMP module updates. |

## Compatibility and verification

The repository's `./gradlew` wrapper pins Gradle 9.5.1 and uses a Java 21
daemon. Kotlin 2.4.21 is the stable bug-fix update to the existing 2.4.20
release; Compose Multiplatform 1.12.2 is the stable patch update to the
existing 1.12.1 release. The project retains its existing Android Gradle Plugin
9.1.1 pin rather than introducing an unverified major/minor update. The KMP
Android library uses Google's Android-KMP plugin, consistent with the current
Kotlin Multiplatform migration guidance. The updated setup passed clean Android
debug/release builds, Android Lint, shared Android-host/iOS simulator tests,
release XCFramework assembly, module architecture checks, and the SwiftUI
application build locally on the pinned toolchain.

Validation commands:

```sh
./gradlew --version
./gradlew :androidApp:assembleDebug :androidApp:assembleRelease :androidApp:lint
./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test
./gradlew :architectureTest :coverageVerify :kotlinLint
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'generic/platform=iOS Simulator' build
```

The Gradle wrapper reported Gradle 9.5.1 and the local JVM reported Java 21.
The current configuration passed the listed checks on the local macOS
toolchain. The required Android and iOS GitHub Actions jobs must repeat them
before their hosted-runner results and elapsed time can be recorded.

## Official references

- [Kotlin releases](https://kotlinlang.org/docs/releases.html)
- [Kotlin Multiplatform compatibility guide](https://kotlinlang.org/docs/multiplatform/multiplatform-compatibility-guide.html)
- [Compose Multiplatform compatibility and versioning](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- [Google Android-KMP library plugin](https://developer.android.com/kotlin/multiplatform/plugin)
- [Gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
