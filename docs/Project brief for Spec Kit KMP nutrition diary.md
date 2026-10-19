# Project brief for Spec Kit: KMP nutrition diary

Oct 19, 2026 · @Manuel Lucas Sánchez

## Overview

This project is an offline-first nutrition diary for Android and iOS, built with Kotlin Multiplatform on top of the public Open Food Facts API. Working title: **NutriLog** (replace freely). It is a public portfolio project: its purpose is to show senior-level mobile architecture and a disciplined AI-assisted workflow with GitHub Spec Kit.

**Target user:** a person who logs what they eat, mostly by scanning barcodes, and wants daily macros and a weekly summary without needing a connection.

**Goals of the repository (in priority order):**

1. Demonstrate a clean KMP architecture: shared logic outside the UI, fully native UI per platform.
2. Demonstrate platform engineering: Gradle convention plugins, version catalog, CI, architecture tests, published build metrics.
3. Leave a verifiable trail of AI-assisted development: constitution, specs, plans, tasks and an honest `AI-LOG.md`.

The app is deliberately small. The quality of the structure, specs and tests matters more than feature count.

## Constitution inputs

Use these as the basis for `/speckit.constitution`. Each rule must be testable or checkable in CI.

1. **Shared core, native UI.** Business logic, data and networking live in shared KMP modules. Android UI is Jetpack Compose, iOS UI is SwiftUI. No business logic in any UI layer.
2. **Strict module boundaries.** Dependencies point inward: `feature-*` → `domain` → nothing. `data` implements `domain` interfaces. Violations fail an architecture test.
3. **No new dependency without an ADR.** Each ADR states the need, alternatives considered, binary-size and build-time impact, licence, and the exit strategy. Third-party libraries are wrapped behind an interface in the core.
4. **Test-first on the core.** The `domain` and `data` modules require at least 80% line coverage, enforced in CI. Every acceptance criterion in a spec maps to at least one test.
5. **Deterministic tests.** No real network, clock or randomness in tests. Time and IDs are injected; any random source is seeded.
6. **Structured concurrency.** Coroutines and Flow only; no `GlobalScope`; every suspend API is cancellable and main-safe.
7. **Fully typed code.** Explicit types on all public APIs; no placeholders, TODOs or dead code on `main`.
8. **Atomic commits.** One commit per task, referencing the task ID. `main` receives changes only through reviewed pull requests.
9. **Privacy by default.** No analytics or tracking SDKs. Food logs never leave the device.
10. **AI accountability.** Every AI-generated change is reviewed and run before merge. Failures or corrections are recorded in `AI-LOG.md`, and recurring ones become a new constitution rule.

## Product scope

This section feeds `/speckit.specify`, one feature at a time. It describes the what and the why, not the technology.

| ID | Feature | User story | Acceptance criteria |
| --- | --- | --- | --- |
| F1 | Product search | As a user, I search foods by name to log them. | Results appear within 1 s from cache and within 3 s from network on a normal connection; empty, error and no-results states are shown; recent searches persist offline. |
| F2 | Barcode scan | As a user, I scan a barcode to find a product quickly. | A valid EAN-13 resolves to a product or to a clear "not found" state; unsupported or damaged codes give an actionable message; previously fetched products resolve offline. |
| F3 | Daily diary | As a user, I log a product with a quantity and meal to track my day. | Calories, protein, carbohydrates and fat are computed per entry and per day; entries can be edited and deleted; totals update immediately; data survives app restarts. |
| F4 | Weekly summary | As a user, I review my week to spot trends. | Shows daily and average macros for the last 7 days; days with no entries are shown as empty, not as zero intake; totals match the sum of the daily diary. |
| F5 | Offline-first sync | As a user, I use the app without a connection. | All logged data and cached products work in airplane mode; cached products refresh when online; stale entries are marked with their age; no data loss on connectivity changes. |

**Edge cases every spec must address:** products with missing nutrition values, per-100 g versus per-serving units, very large quantities, changing the device time zone, and a day boundary crossed while the app is open.

## Technical context

Use this for `/speckit.plan`. Versions are not pinned here: resolve them against current stable releases and record them in the version catalog.

| Concern | Decision |
| --- | --- |
| Language and sharing | Kotlin Multiplatform; shared logic outside the UI |
| Android UI | Jetpack Compose |
| iOS UI | SwiftUI, limited to the scanner and diary screens to prove the model; the rest of iOS is out of scope |
| Async | kotlinx.coroutines and Flow |
| Serialization | kotlinx.serialization |
| Networking | Ktor client, with a mock engine in tests |
| Persistence | SQLDelight |
| Dependency injection | Metro (aligned with the target employer's stack); if its KMP support is not viable, fall back to Koin and record an ADR |
| Build | Gradle with convention plugins and a version catalog |
| CI | GitHub Actions: build, unit tests, lint, architecture tests, binary-size and build-time report |
| Architecture tests | Konsist, or an equivalent that fails on module-boundary violations |
| Backend | None. The app talks only to Open Food Facts |

**Module layout (inward dependencies only):**

- `core:domain`: entities, use cases, repository interfaces. Pure Kotlin.
- `core:data`: Open Food Facts client, SQLDelight database, repository implementations, sync.
- `core:common`: shared utilities, time and dispatcher abstractions.
- `feature:search`, `feature:scan`, `feature:diary`, `feature:summary`: presentation logic (state, intents, reducers) shared in KMP.
- `androidApp`: Compose UI and navigation. `iosApp`: SwiftUI screens.
- `build-logic`: convention plugins.

**Presentation pattern:** MVI with a single immutable state and a one-way event flow per screen.

**Data source:** the Open Food Facts public API. Data is under the ODbL licence, so the app and README must carry the attribution, and requests must send a descriptive `User-Agent` and respect the API's rate limits. Confirm the current endpoints and fields against the official documentation before planning; do not assume them.

## Non-functional requirements and out of scope

**Non-functional requirements (each is measured in CI or in the README):**

- **Performance:** cold start to first interactive screen under 2 s on a mid-range device; diary operations complete without dropped frames.
- **Footprint:** Android release APK size and iOS framework size reported on every merge to `main`, with a budget set after the first baseline.
- **Build health:** clean and incremental build times reported; the CI pipeline stays under 10 minutes.
- **Reliability:** no crash on malformed API responses; every network failure surfaces a recoverable error state.
- **Accessibility:** touch targets of at least 48 dp, content descriptions on interactive elements, dynamic text size supported.
- **Privacy and licensing:** no tracking SDKs; all dependencies have licences compatible with a public repository; Open Food Facts attribution present.

**Out of scope:** user accounts, cloud sync, a custom backend, social features, meal recommendations, manual product creation, and a full iOS app beyond the screens listed in the technical context.

## Delivery order

Run one full Spec Kit cycle (specify, clarify, plan, tasks, analyze, implement) per step, in this order.

1. **Step 0: Platform skeleton.** Modules, convention plugins, version catalog, CI, architecture tests, empty `AI-LOG.md` and ADR template. Proves the platform engineering story before any feature exists.
2. **Step 1: F1 Product search.** Proves the data layer: Ktor client, SQLDelight cache, repository pattern, error handling.
3. **Step 2: F3 Daily diary.** Proves the domain model and macro calculations with strong unit tests.
4. **Step 3: F2 Barcode scan.** Proves platform integration (camera) behind an interface in the shared core.
5. **Step 4: F5 Offline-first sync.** Proves the caching and staleness strategy across the existing features.
6. **Step 5: F4 Weekly summary.** Proves aggregation correctness and the consistency between summary and diary.
7. **Step 6: iOS screens.** SwiftUI scanner and diary on top of the shared core, to prove the multiplatform model.
8. **Step 7: Polish.** README with module diagram, ADR index, published metrics, and a 60-second demo of the spec-to-code flow.

## How to run it

Command names below match the Spec Kit flow I know \[INFERIDO: not verified against the current release\]. Check the official repository for the exact commands and project initialisation before starting.

1. **Initialise** the repository with Spec Kit, choose your AI agent, and place this brief at `docs/project-brief.md` so every step can reference it.
2. **`/speckit.constitution`**: paste the section *Constitution inputs* and ask the agent to turn it into a constitution with testable rules.
3. **`/speckit.specify`**: paste the Step 0 or feature row from *Product scope* plus its edge cases. Describe the what and why only; leave the stack to the plan.
4. **`/speckit.clarify`**: answer every question in writing and keep the answers in the spec; they are part of the portfolio trail.
5. **`/speckit.plan`**: paste *Technical context* and *Non-functional requirements*. Ask the agent to verify library APIs against official documentation instead of assuming them.
6. **`/speckit.tasks`**: review the task list yourself; remove tasks that exceed the spec.
7. **`/speckit.analyze`**: run it before implementing and record any inconsistency it finds in `AI-LOG.md`.
8. **`/speckit.implement`**: work task by task, build and test after each one, and commit atomically with the task ID.

**Starter prompt for the first run:**

```markdown
Read docs/project-brief.md. Start with Step 0 (Platform skeleton) from the Delivery order section.
Follow the Constitution inputs strictly. Do not add dependencies that are not listed in the
Technical context. If something is ambiguous, ask instead of assuming.
```
