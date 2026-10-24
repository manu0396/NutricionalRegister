# Implementation Plan: iOS App Functional Parity with Android

**Branch**: `009-ios-parity` | **Date**: 2026-10-24 | **Spec**: `specs/009-ios-parity/spec.md`

## Summary

Bring the native SwiftUI iOS app to functional parity with the current Compose Android app without duplicating business logic. The shared Kotlin runtime remains authoritative. iOS gets an extended bridge, native SwiftUI screens, AVFoundation barcode scanning, and StoreKit premium mapping.

## Technical Context

- **Language/Version**: Kotlin 2.4.21, Swift 5, iOS 18.2+
- **Primary Dependencies**: KMP shared framework, StoreKit, AVFoundation, SwiftUI
- **Storage**: Existing SQLDelight database through shared runtime
- **Testing**: Gradle shared tests, Xcode build, Swift format lint
- **Target Platform**: iOS simulator/device
- **Project Type**: KMP mobile app with native UI
- **Performance Goals**: Use existing local baselines; no new performance budget
- **Constraints**: Native SwiftUI only; no Compose UI migration; no live network in tests
- **Scale/Scope**: Full app shell parity

## Constitution Check

- Native UI boundary preserved.
- Shared runtime remains authoritative.
- No new third-party dependencies.
- Architecture and module boundaries preserved.

## Project Structure

### Documentation

```text
specs/009-ios-parity/
├── spec.md
├── plan.md
├── tasks.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
```

### Source Code

```text
shared/src/iosMain/kotlin/com/example/nutricionalregister/IosAppBridge.kt
iosApp/iosApp/*.swift
iosApp/iosApp/BarcodeScannerView.swift
```

## Implementation Approach

1. Extend the iOS bridge to own the shared runtime and all feature view models.
2. Add snapshot/state subscription methods and actions mirroring Android.
3. Implement SwiftUI composition root, navigation, and screens.
4. Integrate barcode scanning, premium mapping, and coach gating.
5. Validate with shared tests, Xcode build, Swift format, and manual parity checks.

## Risks

- Kotlin/Native interop complexity.
- Route-scoped product detail lifecycle.
- AddFood session semantics.
- Premium product App Store configuration.
- Camera permission and simulator behavior.