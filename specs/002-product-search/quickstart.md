# Product Search Validation Quickstart

**Feature**: [Product Search](spec.md)
**Plan**: [plan.md](plan.md)

This guide validates the F1 search behavior without making real catalogue
requests in automated tests. Do not enable a live Search-a-licious request
until the service policy gate in [the contract](contracts/search-a-licious.md)
has been completed.

## Prerequisites

- JDK 21 and the repository's Gradle wrapper.
- Android SDK and an Android emulator/device for the native UI checks.
- Xcode and an iOS simulator for shared KMP simulator validation.
- Reviewed ADRs for every new runtime and test dependency before they are
  introduced.
- For an optional manual service smoke test: confirmed Open Food Facts
  Search-a-licious usage terms, rate limits, User-Agent policy, attribution,
  and permitted environment.

## Automated Validation

From the repository root, run shared tests and module/coverage/lint checks:

```sh
./gradlew \
  :core:domain:allTests \
  :core:data:allTests \
  :feature:search:allTests \
  :shared:testAndroidHostTest \
  :shared:iosSimulatorArm64Test \
  :architectureTest \
  :coverageVerify \
  :kotlinLint
```

Build the Android app and run any configured on-device Compose UI tests:

```sh
./gradlew :androidApp:assembleDebug :androidApp:connectedDebugAndroidTest
```

If a test task is not available in the final Gradle configuration, update this
guide with the exact supported task rather than silently omitting that test.
Automated tests use fake clocks, controlled coroutine dispatchers, a local
SQLDelight test database, and Ktor MockEngine fixtures; they do not call the
live service.

## Required Behavior Scenarios

1. **Cached-first refresh**: Seed a matching local snapshot, submit the term,
   and assert that results are rendered before the fake network response is
   released. Measure the one-second visible-cache target separately on a
   device; verify a successful refresh replaces the query snapshot.
2. **Remote lookup**: Submit a term with no local snapshot and return a
   successful fixture from MockEngine. Verify the network state transitions
   to results or no-results and that the UI is not held in a final empty state
   while the request is pending.
3. **Latest query wins**: Start request A, submit request B, return B first and
   A last. Verify only B is visible; verify a late response cannot overwrite
   B.
4. **Failure and offline**: Simulate network failure with and without cached
   products. Verify cached products remain visible as potentially stale and
   that an uncached offline query has an offline-specific retry/edit action.
   Simulate a service error separately and verify recoverable feedback.
5. **Malformed records**: Include a hit with missing identity, a hit with a
   blank name, malformed optional nutrition, and a valid hit. Verify the
   valid result remains visible, invalid hits are ignored, and absent
   nutrition is labelled unavailable rather than zero.
6. **Persistent history**: Complete more than 10 distinct searches, recreate
   the repository/database connection, and verify the 10 most recent distinct
   terms remain ordered and usable offline. Verify successful matches,
   no-results, and recoverable failures all update history; blank submissions
   do not. Repeat a term and verify it moves to the front without duplication.
   Complete two searches at the same fake-clock millisecond and verify their
   persisted sequence determines stable ordering after reopening the database.
7. **History deletion**: Remove one recent term and verify its history entry
   is absent while the associated product remains in the local product cache.
8. **No diary side effects**: Search and inspect a product, then verify no
   diary entry or nutrition total was created or modified.
9. **Privacy**: Verify the submitted term is sent only to the approved product
   catalogue request, is absent from analytics/tracking integrations and
   application logs, and is not uploaded as recent-history data.
10. **Accessibility**: With Compose semantics tests, verify search controls
   expose accessible names, interactive targets are at least 48 dp, result
   selection is labelled, and large system text does not hide essential
   actions.

Use deterministic fixtures for all automated cases. Record real-device
network timing separately and only after the service policy gate is approved;
automated timing tests must not depend on internet conditions.

After changes are pushed to a pull request, record the end-to-end hosted CI
duration and verify that it remains below the project's 10-minute target.
Report any miss and the mitigation or outstanding gap.

## Optional Manual Smoke Test

After the contract requirements are verified and the app is configured for an
approved environment:

1. Install and open the Android debug build.
2. Submit a common food name and verify result order, product identity, and
   available nutrition values.
3. Open a result and confirm the detail view does not create a diary entry.
4. Search again, disable connectivity, reopen a recent term, and verify the
   cached result and stale/offline message.
5. Re-enable connectivity and retry; verify the latest query remains the
   visible query.
6. Confirm the displayed Open Food Facts attribution and record elapsed time
   without including search terms or personal data in diagnostic logs.
