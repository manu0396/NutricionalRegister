# ADR-0020: The AI gateway is a first-party service in this repository

**Status**: Accepted (implemented)
**Date**: 2026-10-22
**Related**: ADR-0016 (the assistant behind a first-party gateway), FR-025 to
FR-028, ADR-0018 (the premium unlock).

## Context

ADR-0016 decided that the Coach reaches a model through a first-party gateway
and fixed the app's half of the contract: the envelope, the minimal context,
the honest "not configured" state. It did not decide what the gateway itself is
made of, where it lives, or what happens when no provider key is configured.
The result was that the app was complete and correct, and the assistant still
answered "not configured", because there was no service to talk to.

The work has to answer three questions at once:

1. What is the gateway made of?
2. How does it scale and how is it hardened?
3. What does the user see when the publisher has not yet configured a provider
   key?

## Decision

The gateway is a **first-party service that lives in this repository**, in
`gateway/`, written in plain Node.js with **zero third-party packages**: the
whole surface an auditor reads is the six files in that directory. It
implements exactly the endpoint ADR-0016 fixed, holds the provider key in its
own process environment, and is stateless so an ordinary load balancer can run
any number of replicas.

The model is a **configuration value** of the gateway, not a fact of the app:
`AI_GATEWAY_MODEL` names it, the provider's key is read from the environment at
run time, and a provider migration is a deploy, never an app release. The
gateway speaks **two protocols**, Anthropic's Messages API (a funded key) and
the OpenAI-compatible chat format that a hosted provider, a self-hosted Ollama
or a local LM Studio speak, so the development harness of a real deployment
answers with a real model without carrying a key.

When no provider key is configured, or the provider fails, the gateway answers
with a **deterministic coach** computed from the same minimal context the app
sent. It is not a stand-in model: it reports the day the user actually logged
(the calories, the reported macros, what is left) and proposes at most a food
from the user's own library. It reads as a measurement rather than as a
conversation, and it exists so that the path from the consent screen to the
confirmation of a proposal is testable and honest before a provider is paid
for. The production answer is the model; the fallback is the service not
answering nothing.

Hardening is the service's own, not an app concern:

- a per-client-address request quota, because the gateway owns the cost of the
  model it calls;
- a capped request body, so a client cannot grow the gateway's memory;
- an upstream timeout, so a hung provider cannot hold a request forever;
- graceful shutdown, so a replica drains in flight before it goes away;
- minimal logging: one line per request with the status, the timing and
  counts; never a body, never a header, never an address, because the body is
  the user's diary.

## Consequences

- The assistant becomes a product, not a UI over a promise: the publisher
  deploys one container and the app's `-PNUTRILOG_AI_PROXY_URL` and the Coach
  works, with everything between the consent screen and a diary write covered
  by tests and covered live.
- The repository carries the service, so the contract, the client and the
  gateway are reviewed together and cannot drift: the app's client tests and
  the gateway's envelope lint enforce the same shapes from both ends.
- The zero-dependency choice is deliberate: the fewer packages a service adds,
  the smaller the security and licence surface it has to justify, at the cost
  of hand-written code that a framework would have supplied. The code is small
  enough for that trade to be safe here.
- A production deployment still wants device attestation and enforced
  entitlement, which the app cannot carry yet. Those are recorded as pending
  work (ADR-0018's server-side authoritative gate on the assistant) rather
  than faked with a header the client does not send.
