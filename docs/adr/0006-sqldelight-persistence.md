# ADR-0006: SQLDelight for Product and Recent-Search Persistence

- **Status**: Accepted
- **Date**: 2026-10-19
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: T004, T010, T053; `specs/002-product-search/`

## Context and need

Product Search requires durable local product snapshots and recent terms that
remain available offline and survive restarts. History deletion must not delete
cached products. Queries need deterministic ordering, a 10-term cap, and
transactional updates for product snapshots.

## Decision

Propose SQLDelight 2.4.1 for typed SQL, generated Kotlin APIs, and Android and
Native/iOS SQLite drivers. Use its test driver or another approved in-memory
SQLite driver for deterministic repository tests. Keep schema and migrations
owned by `core:data`; store history separately from product and search-snapshot
records.

No dependency or version-catalog entry is approved by this proposal. Verify
the Gradle plugin, Kotlin 2.4.21, AGP 9.1.1, Gradle 9.5.1, Android, and Native
driver compatibility before adoption.

## Alternatives considered

- Platform-specific persistence implementations: rejected because they
  duplicate schema and query semantics across Android/iOS.
- In-memory-only cache: rejected because it cannot survive app restarts or
  provide offline history.
- Handwritten SQLite access: rejected because it gives up generated typed
  queries and increases query/mapping maintenance.
- Another KMP database: reconsider if SQLDelight's compiler/plugin compatibility
  or Native driver requirements cannot be met.

## Binary-size impact

SQLDelight runtime and platform drivers are production dependencies; generated
query code and SQLite support can affect the APK and iOS framework. No
project-specific delta is measured. Measure release artifact size before and
after adding the selected runtime/drivers; keep test drivers out of production
source sets.

## Build-time impact

The Gradle plugin validates SQL and generates Kotlin code, increasing
configuration/compilation work. Repository tests add test-only driver work.
Measure clean/incremental builds and full hosted PR duration after integration;
report any gap against the 10-minute target.

## Licence

The SQLDelight repository is Apache License 2.0:
<https://github.com/sqldelight/sqldelight/blob/2.4.1/LICENSE>.
Confirm the selected artifact metadata and test-driver/transitive licences
before adoption.

## Exit strategy

Replace SQLDelight repository adapters and schema definitions with an
approved KMP persistence implementation. Export/migrate the local product,
snapshot, history, and ordering data before removing the old database. Preserve
offline behavior and independent history/product deletion tests.

## Consequences

- Shared database schema and generated Kotlin query APIs support both targets.
- Schema generation and driver dependencies add build and artifact cost.
- Deterministic migration and equal-timestamp ordering tests are required.

## Review record

- **Review outcome**: Accepted by maintainer
- **Reviewed by**: Repository maintainer
- **Review date**: 2026-10-19
- **Rationale or conditions**: Approved SQLDelight for persistence. Verify
  plugin/runtime/driver compatibility and selected/transitive licences during
  integration; measure size/build impacts as planned.

## Validation record

- **Final licences (verified 2026-10-19)**: the published POM metadata for
  the resolved `app.cash.sqldelight` 2.4.1 artifacts (`runtime`,
  `coroutines-extensions`, `android-driver`, `native-driver`, `sqlite-driver`,
  and the Gradle plugin) declares `Apache-2.0`.
- **Compatibility**: the plugin, runtime, and platform drivers compile and
  pass the module tests under the repository toolchain (Kotlin 2.4.21,
  AGP 9.1.1, Gradle 9.5.1).
- **Measured sizes (2026-10-19, with all approved dependencies and feature
  code integrated)**: unsigned Android release APK 11,035,730 bytes; Android
  debug APK 15,265,427 bytes; `Shared.xcframework` approximately 92,000 KB on
  disk. These are feature-level totals, not per-dependency deltas; test
  drivers stayed out of production source sets.
- **Build-time deltas (2026-10-19)**: the full verification run (module unit
  tests, architecture tests, `coverageVerify`, `kotlinLint`, `assembleDebug`,
  and Android lint) exited 0; the warm-build-cache clean verification took
  6 s and the incremental verification took 1 s. Hosted pull-request duration
  remains pending task T056.
