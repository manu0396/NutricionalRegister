# The NutriLog AI gateway

The one service the Coach talks to, and the only thing allowed to hold a
provider key (ADR-0016, ADR-0020). Plain Node with **zero third-party
packages**, so the surface an auditor reads is the code in this directory and
nothing else.

## The contract

One endpoint, `POST /v1/chat`, with the envelope the app already implements and
its client tests lock:

```json
POST /v1/chat
content-type: application/json

{"messages":[{"role":"USER","text":"..."}],
 "context":{"entries":[{"entryId":7,"productCode":"00042","productName":"Oat drink",
            "meal":"BREAKFAST","quantityGrams":250,"energyKcal":105}],
            "goal":{"energyKcal":2000,"proteinGrams":120,"carbohydrateGrams":220,"fatGrams":70},
            "libraryCodes":["00042"]}}
```

```json
{"reply":"...", "actions":[{"type":"log","productCode":"00042","quantityGrams":100,"meal":"SNACKS"}]}
```

An action is always a **proposal**. The app validates every one against the
diary before it is applied, and none is applied before the user confirms it, so
the gateway can propose only, never write. A nutrient an entry does not report
is absent from the request, so the model is never shown a zero for an unknown
amount.

`GET /v1/health` answers `{"status":"ok","model":...,"provider":true|false}` for
a load balancer.

## Configuration

| Variable | Meaning |
| --- | --- |
| `PORT` | the port the service listens on (default 8080). |
| `ANTHROPIC_API_KEY` | the provider key. Configure it to answer with the model; leave it out and the service answers with its deterministic fallback. |
| `AI_GATEWAY_MODEL` | the model to generate with (default `claude-sonnet-4-5`). |
| `AI_GATEWAY_QUOTA_PER_MINUTE` | requests a single client address may make per minute (default 20). |
| `AI_GATEWAY_TIMEOUT_MS` | the upstream call's timeout in milliseconds (default 90000). |
| `AI_GATEWAY_TLS_CERT`, `AI_GATEWAY_TLS_KEY` | for a local harness only: the service listens with HTTPS. In a deployed container the platform ingress terminates TLS in front of it. |

The client in the app requires an `https://` address, which is what a real
deployment has. To point the app at a gateway:

```bash
./gradlew :androidApp:assembleDebug -PNUTRILOG_AI_PROXY_URL=https://your-gateway.example.com
```

## Run the service locally

```bash
node gateway/server.mjs            # listens on 8080, talks to the model when a key is set
curl -s localhost:8080/v1/health
```

## Run the service as a container

```bash
docker build -t nutrilog-gateway gateway/
docker run --rm -p 8080:8080 -e ANTHROPIC_API_KEY="$ANTHROPIC_API_KEY" nutrilog-gateway
```

## What production means here (ADR-0020)

- **Stateless.** No session, no storage, no affinity: replicas can be added,
  removed or replaced at any time behind an ordinary load balancer, and the
  only per-replica cost is the process itself.
- **The key is the environment's**, so a replica is a container plus one
  secret; the key is never baked into an image, committed or logged.
- **The model is a configuration value**, so a provider migration is a deploy
  with a different `AI_GATEWAY_MODEL` and no app release.
- **Fallen back on, honestly.** Without a provider key, or when the provider
  fails, the gateway answers with a deterministic coach computed from the day's
  own numbers, in the same envelope. It reads as a measurement, not as a
  conversation, and it keeps the whole path from the consent screen to the
  confirmation of a proposal testable without a provider.
- **Quota rules are set here.** Each client address gets a bounded request
  rate, and the gateway absorbs a malformed or oversized body without
  forwarding it.
- **Logging is minimal by design.** One line per request, with the status, the
  timing and counts: never a body, never a header, never an address, because
  the body is the user's diary.

### What is deliberately pending

- **Device attestation.** A production gateway wants a per-device entitlement
  check (Play Integrity on Android, App Attest on iOS) so an authenticated
  app, not an anonymous client, spends the quota. The app does not carry a
  device credential yet; the quota in this service is per address as a first
  line, and the entitlement hook is recorded rather than faked with a header
  the client does not send.
- **Server-side entitlement enforcement.** The premium gate is advisory in the
  app, and the gateway is the authoritative one (FR-025); enforcing it here is
  the same device-attestation work above, which is why the quota and the rate
  limit are the first controls this service ships with.

### Files

| File | Role |
| --- | --- |
| `server.mjs` | the endpoint, the envelope lint, the health check and the shutdown. |
| `coach.mjs` | the prompt, the conversation the app sent, and the cleaning of the proposed actions. |
| `provider.mjs` | the transport to the model; the only place that reads the key. |
| `fallback.mjs` | the deterministic coach, used when the provider is absent or fails. |
| `body.mjs` | the capped body reader. |
| `rate-limit.mjs` | the per-address request quota. |
| `dev-certs/` | a development certificate the app's debug build trusts; never committed. |
