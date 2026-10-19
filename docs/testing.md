# Test and Verification Policy

## Acceptance criteria

Each acceptance criterion in a feature specification maps to at least one
automated test or an explicitly documented manual/CI verification step. Tests
name the behavior they verify and run as part of the corresponding required
Gradle or GitHub Actions check.

## Determinism and network isolation

- Unit and architecture tests MUST NOT contact live network services. Use fake
  implementations or mock HTTP engines for service boundaries.
- Code that reads the current time MUST receive a clock/time source as an
  injected dependency; tests supply a fixed source.
- Code that creates identifiers MUST receive an ID generator; tests provide
  deterministic values.
- Any random source MUST be injected and seeded in tests.
- Repeat the relevant test task to confirm that identical inputs produce
  identical outcomes.

## Test layers

- Shared domain/data behavior: common tests where portable, with Android-host
  tests for JVM-only tooling and coverage.
- Platform behavior: Android host/device or iOS simulator tests in the
  corresponding source set.
- Module architecture: `architecture-tests` and the Gradle
  `moduleDependencyCheck` task.
- Coverage: Kover enforces the 80% JVM line threshold for populated
  `core:domain` and `core:data` modules. It does not report Kotlin/Native test
  coverage.
