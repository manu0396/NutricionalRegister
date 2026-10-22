# ADR-0016: Remote LLM Assistant Behind a First-Party AI Proxy

- **Status**: Accepted (implemented by specification 007)
- **Date**: 2026-10-21
- **Decision owners**: Project maintainers
- **Related tasks/specifications**: `specs/007-app-restructure/`; [spec.md](../../specs/007-app-restructure/spec.md); ADR-0005

## Context and need

Feature 007 turns the app into a sellable nutrition tracker. Its information
architecture is "Tracker Shell + Log-first + Coach + AI chat assistant": the
Coach destination hosts a chat assistant that answers questions about the
user's own diary in natural language, for example "what should I eat for dinner
to hit my protein goal?".

The user has decided that the assistant is a **remote large language model
reached over HTTP**. An on-device deterministic intent engine was considered
and explicitly rejected in favour of a real model, because free-form natural
language is handled far better by an LLM than by a fixed set of rules.

The decision must respect four standing constraints:

- **No accounts and no cloud sync.** The app has no user identity to
  authenticate against and no server-side copy of the user's data.
- **A proven privacy posture.** `core/data/src/androidHostTest/.../local/PrivacyNoEgressTest.kt`
  and `feature/search/src/commonTest/.../SearchPrivacyTest.kt` assert that the
  diary, save and purchase flows emit no telemetry and no tracking event. The
  assistant *will* egress data, so it must be an explicit, consented and
  documented exception, not a silent hole in that guarantee.
- **An approved HTTP stack.** Ktor Client 3.6.0 is already approved and in use
  (ADR-0005): `ktor-client-core`, `ktor-client-content-negotiation`,
  `ktor-serialization-kotlinx-json`, `ktor-client-okhttp` on Android,
  `ktor-client-darwin` on iOS and `ktor-client-mock` in tests. The assistant
  therefore needs **no new third-party dependency**.
- **No new dependency without an ADR** (spec 007 NFR-002, which cites
  constitution rule III). This ADR is the record that satisfies that rule. It
  introduces no new dependency.

The problem this ADR must settle is **where the LLM provider key lives**. A
provider key embedded in an APK is extractable by anyone who downloads the
app. It becomes an unbounded, unmetered liability billed to the publisher,
and it hard-codes a vendor into the client. The app must never hold that key.

## Decision

Propose that the app talks to a **thin, first-party AI gateway (the proxy)**
that the publisher deploys, and that the provider key never ships inside the
app. This decision is proposed pending maintainer review; nothing described
here is implemented yet.

1. **The provider key never ships inside the app.** The proxy holds the
   provider key, enforces rate limits and quotas, and returns a
   provider-agnostic JSON envelope. The app never names, detects or knows the
   LLM provider, and no provider hostname or credential appears in the
   repository, in the build configuration or in the artifact.

2. **The proxy contract is locked by this ADR.** The client speaks exactly one
   endpoint, and the proxy is provider-agnostic.

   Request:

   ```
   POST {baseUrl}/v1/chat
   Content-Type: application/json

   {
     "messages": [
       { "role": "user",      "text": "..." },
       { "role": "assistant", "text": "..." }
     ],
     "context": { "...": "..." }
   }
   ```

   Response:

   ```
   {
     "reply": "...",
     "actions": [ ... ]
   }
   ```

   - `baseUrl` is the proxy origin; the path is fixed. The app never contacts
     a provider URL.
   - `messages` is the conversation so far, with only the roles `user` and
     `assistant`.
   - `context` carries the minimal diary context described in point 4 and
     nothing else.
   - `reply` is free text for the user to read. It is displayed and never
     executed.
   - `actions` carries shared domain action values. Every returned action is
     validated against the shared domain rules before anything is executed; an
     unknown, malformed or out-of-scope action is rejected and surfaced as not
     understood. **Free text from the model is never executed.**

3. **The proxy base URL is build configuration, not a secret and not
   hardcoded.** It is supplied through a local or build property, for example
   `NUTRILOG_AI_PROXY_URL`, surfaced through the Android `BuildConfig`, and it
   defaults to empty. The base URL is not a secret; it is a public hostname,
   and it is configuration so that the repository stays free of
   environment-specific values and so that the feature can be switched on per
   build channel without a code change. It must be an `https` URL. When the
   value is empty the assistant is **disabled** and the Coach screen shows an
   explicit "not configured" state explaining that the feature is switched off
   in this build and why. The rest of the Coach screen is unaffected.

4. **Privacy.** The app sends only the user's own message plus a minimal diary
   context: today's logged entries, the current goals, and the codes of the
   library foods. It must not send the full catalogue, recent search terms, or
   purchase records. A first-use consent disclosure is shown in the app before
   the first message is sent, and the More destination states that messages are
   sent to a third-party AI service. The existing no-telemetry guarantee
   remains true for everything **except** the assistant, which is an explicit
   and consented exception; the guarantee is scoped, not weakened silently.

5. **Offline behaviour.** The assistant requires connectivity. Offline, it
   reports that clearly and does not pretend to work. The rest of the app
   stays offline-first and unchanged, and the Today screen still renders
   without a network call.

6. **Server-side enforcement.** The proxy is the natural place to enforce the
   paid entitlement later, so the client-side entitlement check is advisory and
   is explicitly **not** the security boundary.

7. **Testing.** The proxy is exercised in shared unit tests through Ktor's
   `MockEngine` (`ktor-client-mock`), which is already approved. No test may
   make a live network call.

Placement follows the existing structure: the assistant client sits in
`core:data` behind the same Ktor boundary as `SearchAliciousClient` (ADR-0005),
the action model and its validation sit in `core:domain` under the 80 % Kover
gate (NFR-001), and the Coach UI is Android UI in this feature, consistent with
the Android-leads-iOS precedent (ADR-0013). The shared client code is the only
iOS impact.

## Alternatives considered

- **Embedding the provider key in the app** and calling the provider directly
  with a bundled credential: rejected. The key is extractable from the APK by
  anyone who installs the app. It cannot be revoked per user, cannot be
  rate-limited per user, and its usage is billed to the publisher without a
  ceiling. Obfuscation is not a protection for a credential that must work.
- **Calling the LLM provider directly from the app** (hosted provider SDK or a
  raw HTTP call with a runtime-fetched key): rejected for the same key problem,
  and it additionally hard-codes a vendor into the client, exposes the
  provider endpoint to abuse, and cannot enforce a paid entitlement later
  without distributing provider credentials to every device.
- **On-device deterministic intent engine**: viable, fully offline, free per
  request, and it was the direction the project originally favoured (the
  deferred explainable coaching engine in the feature 007 brainstorming).
  Rejected for this increment because the user explicitly chose a remote LLM,
  which handles free-form natural language far better than a rules engine with
  a fixed intent set. It remains available as the fallback if the proxy
  cannot be operated, and because the shared action model is provider-agnostic,
  the same Coach UI can be served by either backend without a UI change.
- **A fully local open-weights model on device**: rejected for this increment.
  Model weights are impractical in download size across the `minSdk 24` device
  range the project supports, and on-device inference would introduce a runtime
  dependency, which constitution rule III would treat as a new dependency
  requiring its own ADR.
- **Streaming responses**: deferred, not required for feature 007. The
  assistant returns one complete reply. Streaming changes the proxy contract,
  so it must not be half-built into this increment; it is a follow-on.

## Binary-size impact

No new dependency is added: the assistant reuses the Ktor modules already
resolved by ADR-0005. The delta is therefore limited to the assistant client
code, the request and response DTOs, the action model and its validation, and
the Coach UI.

The measurement procedure is the one already used by ADR-0005, ADR-0010,
ADR-0011 and ADR-0014, so the numbers stay comparable:

- Android: build the unsigned release APK and the debug APK from the same
  configuration and record the byte size of each artifact.
- iOS: build `Shared.xcframework` in the release configuration and record its
  on-disk size in KB.

Baselines to compare against: ADR-0005 recorded an unsigned release APK of
11,035,730 bytes, a debug APK of 15,265,427 bytes and `Shared.xcframework` at
approximately 92,000 KB; ADR-0011 recorded `Shared.xcframework` (release) at
92,024 KB; ADR-0014 and ADR-0015 later recorded a release APK of 12,520,644
bytes.

The iOS framework is unaffected beyond the shared client code, because the
proxy is remote: no model weights, no inference runtime and no native library
enter the artifact. The measured values are recorded in the validation record
when the implementation lands.

## Build-time impact

No new dependency resolution. `ktor-client-core`,
`ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, the
platform engines and `ktor-client-mock` are already on the `core:data`
classpath and in the version catalog (ADR-0005). There is no version-catalog
change, no repository change and no new transitive graph to audit.

The added work is the compilation of the shared client, the DTOs and the
action-validation logic, plus the new shared unit tests that exercise the
proxy contract through `ktor-client-mock`, which is already a `commonTest`
dependency of `core:data`. Clean and incremental Gradle durations are measured
after integration, and hosted pull-request CI must stay within the project's
10-minute target (ADR-0005, task T056).

## Licence

No new dependency is introduced, so **no new third-party licence obligation**
arises. The licence surface of the repository is unchanged: the Ktor 3.6.0
modules and their transitive artifacts remain Apache License 2.0 as recorded in
the ADR-0005 validation record, and no new artifact enters the resolved graph.

Two obligations do exist, and both are deployment-side rather than
repository-side:

- the **deployed proxy's** own licence and the terms under which it is operated
  are the publisher's responsibility and are not recorded in this repository;
- the **chosen LLM provider's** terms, including what they require of a
  public service, are a deployment decision taken by the publisher. The app
  never links against or redistributes the provider, so no provider licence
  reaches the artifact.

## Exit strategy

The assistant is layered so that it can be removed or replaced without
touching the rest of the product:

- **Disable without a code change.** Blanking the base URL, or leaving
  `NUTRILOG_AI_PROXY_URL` unset, disables the assistant and the Coach screen
  falls back to the explicit "not configured" state. This is the cheapest exit
  and needs no release.
- **Remove the feature.** Delete the client and its DTOs from `core:data`, the
  action model and its validation from `core:domain`, the Coach destination
  and its navigation entry from the UI, the consent disclosure and the More
  destination statement, and the `NUTRILOG_AI_PROXY_URL` build property. The
  rest of the app is untouched and no persisted data needs migrating: the only
  local state the assistant introduces is the first-use consent record.
- **Replace the backend.** The action model is provider-agnostic and lives in
  the shared domain, so a different proxy, a different provider behind the
  same proxy, or the deterministic on-device engine can serve the same Coach UI
  behind the same contract. Only the transport implementation behind the
  assistant interface changes.
- **Move to another proxy deployment.** Because the contract is locked and
  provider-agnostic, the publisher can migrate the proxy, the provider or the
  hosting without shipping a new client, as long as the envelope above is
  honoured.

## Consequences

Benefits:

- The provider key never reaches a client, so the publisher's exposure is
  bounded by the proxy's rate limits and quotas rather than by the APK.
- The app is vendor-agnostic: the client contract is the proxy envelope, not a
  provider's API, so changing provider is a proxy-side change.
- No new dependency, so the dependency-review and licence surface of the
  repository does not grow.
- Model output can never mutate the diary arbitrarily: every returned action
  is validated against shared domain rules before execution, and free text is
  only ever displayed.
- The feature can ship switched off and be enabled per build through
  configuration alone.
- The proxy is the natural place to enforce the paid entitlement later, so the
  client needs no entitlement knowledge.

Costs and constraints, stated plainly:

- **A server-side component now exists.** The publisher must deploy, secure,
  monitor and pay for it. This is the first hosted component in the product's
  history and it is an ongoing operational obligation, not a one-off.
- **The app is no longer fully offline.** The assistant requires connectivity.
  Every other part of the app stays offline-first and unchanged; only the
  assistant's own availability depends on the network.
- **There is a per-request cost to the publisher** (hosting plus provider
  tokens). It is not borne by the user and not metered by the client, which is
  precisely why the proxy's quotas matter.
- **The feature cannot work until the publisher deploys the proxy and
  configures the base URL.** Until then the Coach screen shows an honest "not
  configured" state. This is a shipping prerequisite outside the repository and
  a real risk to the feature's perceived completeness.
- **The privacy guarantee is now scoped.** It remains true that the app emits
  no telemetry and no tracking event, but the assistant sends the user's
  message and a minimal diary context to a third-party AI service. That is an
  explicit and consented exception, and the guarantee can no longer be stated
  as "no data ever leaves the device".
- **The client-side entitlement check is advisory.** If the assistant is later
  premium-only, the proxy is the security boundary and the client check is a
  courtesy, so the premium gate cannot be enforced in the app alone.
- **No streaming.** Long answers arrive whole, which is acceptable for 007 but
  is a visible limitation.
- **The privacy and consent copy is a product obligation.** The disclosure text
  is part of the product surface and has to be maintained.

## Review record

- **Review outcome**: Pending
- **Reviewed by**: —
- **Review date**: —
- **Rationale or conditions**: —

## Validation record

This ADR is **proposed and not yet implemented**, so nothing below has been
verified. The entries state what will be checked and against which recorded
baseline; they are completed when the Coach work lands.

- **Contract**: the request and response envelopes are exercised in shared
  common tests through Ktor's `MockEngine` (`ktor-client-mock`). The tests
  cover a successful reply with no actions, a reply with a valid action, a
  reply with an unknown or malformed action, an HTTP error and a timeout. **No
  test may make a live network call.**
- **Action validation**: the shared domain rules are unit-tested in
  `core:domain` under the 80 % Kover gate (NFR-001), including the rejection
  of an action the model returns that the domain does not allow.
- **Payload minimisation**: a new test asserts that the assistant's request
  payload contains only the user's message and the minimal context (today's
  entries, current goals and library food codes), and that it contains no
  catalogue page, no recent search term and no purchase record.
- **Privacy posture**: `PrivacyNoEgressTest` and `SearchPrivacyTest` continue
  to assert zero telemetry egress for the diary, save and purchase flows, and
  the assistant is recorded as the single outbound data path, with its own
  consent test.
- **Size baseline**: ADR-0005 (2026-10-19) recorded an unsigned release APK of
  11,035,730 bytes, a debug APK of 15,265,427 bytes and `Shared.xcframework`
  at approximately 92,000 KB; ADR-0011 recorded `Shared.xcframework` (release)
  at 92,024 KB; ADR-0014 and ADR-0015 (2026-10-20) later recorded a release
  APK of 12,520,644 bytes. The assistant delta is measured against the most
  recent baseline.
- **Not verifiable in this repository**: the deployed proxy, its key handling,
  its rate limits and quotas, and the server-side entitlement enforcement are
  deployment-side obligations. The proxy implementation itself is out of scope
  for this repository, and its contract is what this ADR locks.
