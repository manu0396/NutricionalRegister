#!/usr/bin/env node
/**
 * The NutriLog AI gateway (ADR-0020).
 *
 * The app's Coach speaks exactly one endpoint of this service, and this service
 * is the only thing allowed to hold a provider key: the key lives in the process
 * environment, is never logged, never echoed and never sent to a client
 * (ADR-0016, FR-028).
 *
 * The service is stateless, so any number of replicas can serve the endpoint
 * behind a load balancer; the container speaks plain HTTP and the platform
 * ingress terminates TLS in front of it. HTTPS on the socket exists only for a
 * development harness on a machine's own network.
 *
 * See gateway/README.md for the configuration and the deployment.
 */

import https from "node:https";
import http from "node:http";
import fs from "node:fs";

import {readBody} from "./body.mjs";
import {rateLimitedClient} from "./rate-limit.mjs";
import {generate, isConfigured, model, providerKind} from "./provider.mjs";
import {fallbackAnswer} from "./fallback.mjs";
import {actionsFrom, contextFor, conversationFor, systemPrompt} from "./coach.mjs";

const PORT = process.env.PORT ? Number(process.env.PORT) : 8080;
const BODY_LIMIT_BYTES = 64 * 1024;
const CHAT_PATH = "/v1/chat";
const HEALTH_PATH = "/v1/health";

const MAX_MESSAGES = 20;
const MAX_MESSAGE_CHARS = 4_000;
const MAX_ENTRIES = 30;
const MAX_LIBRARY_CODES = 50;

const MEALS = new Set(["BREAKFAST", "LUNCH", "DINNER", "SNACKS"]);
const ROLES = new Set(["USER", "ASSISTANT"]);

const isPlainObject = (value) => value !== null && typeof value === "object" && !Array.isArray(value);

/** One line per request, and never a body, a header or an address. */
function log(message) {
  process.stdout.write(`${new Date().toISOString()} ${message}\n`);
}

/** The turn the gateway keeps, or null when the app cannot have meant it. */
function validMessage(turn) {
  if (!isPlainObject(turn)) return null;
  const role = String(turn.role ?? "").toUpperCase();
  const text = String(turn.text ?? "").trim();
  if (!ROLES.has(role) || !text) return null;
  return {
    role: role === "ASSISTANT" ? "assistant" : "user",
    text: text.slice(0, MAX_MESSAGE_CHARS),
  };
}

/** One entry of the day, clamped to the fields the Coach is allowed to see. */
function validEntry(entry, index) {
  if (!isPlainObject(entry)) return null;
  const meal = String(entry.meal ?? "").toUpperCase();
  const productCode = String(entry.productCode ?? "").trim();
  const quantityGrams = Number(entry.quantityGrams);
  if (!MEALS.has(meal) || !productCode || !Number.isFinite(quantityGrams) || quantityGrams <= 0) {
    return null;
  }
  const entryId = Number(entry.entryId);
  const clean = {
    entryId: Number.isInteger(entryId) && entryId > 0 ? entryId : index + 1,
    productCode: productCode.slice(0, 64),
    productName: String(entry.productName ?? "").trim().slice(0, 120),
    meal,
    quantityGrams,
  };
  for (const nutrient of ["energyKcal", "proteinGrams", "carbohydrateGrams", "fatGrams"]) {
    const value = entry[nutrient];
    if (typeof value === "number" && Number.isFinite(value)) clean[nutrient] = value;
  }
  return clean;
}

/** The daily targets, with the app's defaults for the values the app did not send. */
function validGoal(goal) {
  const rounded = (value, fallback) => {
    const number = Math.round(Number(value));
    return Number.isFinite(number) && number > 0 ? number : fallback;
  };
  if (!isPlainObject(goal)) {
    return {energyKcal: 2000, proteinGrams: 120, carbohydrateGrams: 220, fatGrams: 70};
  }
  return {
    energyKcal: rounded(goal.energyKcal, 2000),
    proteinGrams: rounded(goal.proteinGrams, 120),
    carbohydrateGrams: rounded(goal.carbohydrateGrams, 220),
    fatGrams: rounded(goal.fatGrams, 70),
  };
}

/**
 * The envelope, cleaned and capped.
 *
 * Everything a client sends here is untrusted: only the fields the gateway
 * understands survive, only as many of them as the prompt can sensibly carry,
 * and only as text. Nothing is expanded, nothing is stored.
 */
function lintEnvelope(envelope) {
  const context = isPlainObject(envelope?.context) ? envelope.context : {};
  return {
    conversation: (Array.isArray(envelope?.messages) ? envelope.messages : [])
      .slice(0, MAX_MESSAGES)
      .map(validMessage)
      .filter(Boolean),
    entries: (Array.isArray(context.entries) ? context.entries : [])
      .slice(0, MAX_ENTRIES)
      .map(validEntry)
      .filter(Boolean),
    goal: validGoal(context.goal),
    libraryCodes: (Array.isArray(context.libraryCodes) ? context.libraryCodes : [])
      .filter((code) => typeof code === "string" && code.trim())
      .slice(0, MAX_LIBRARY_CODES)
      .map((code) => code.trim().slice(0, 64)),
  };
}

function respond(response, status, body) {
  response.statusCode = status;
  response.setHeader("Content-Type", "application/json; charset=utf-8");
  response.end(JSON.stringify(body));
}

/** Routes one request; the service holds no session, so nothing outlives it. */
async function route(request, response) {
  const requestId = `req-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
  response.setHeader("X-Request-Id", requestId);
  response.setHeader("X-Content-Type-Options", "nosniff");

  if (request.method === "GET" && request.url === HEALTH_PATH) {
    respond(response, 200, {status: "ok", model: model(), provider: isConfigured(), kind: providerKind()});
    return;
  }

  if (request.method !== "POST" || request.url !== CHAT_PATH) {
    respond(response, 405, {error: "Method not allowed"});
    return;
  }

  const client = rateLimitedClient(request.socket.remoteAddress);
  if (!client.hasQuota()) {
    respond(response, 429, {error: "Too many requests"});
    log(`429 request ${requestId}`);
    return;
  }

  let envelope;
  try {
    envelope = await readBody(request, BODY_LIMIT_BYTES);
  } catch {
    respond(response, 400, {error: "Bad request"});
    return;
  }

  const context = lintEnvelope(envelope);
  const lastTurn = context.conversation.at(-1);
  if (!lastTurn || lastTurn.role !== "user") {
    respond(response, 400, {error: "The conversation carries no user message"});
    return;
  }

  try {
    let body;
    try {
      const completion = await generate({
        model: model(),
        maxTokens: 1200,
        system: `${systemPrompt(MEALS)}\n\nDATA:\n${contextFor(context)}`,
        messages: conversationFor(context),
      });
      const reply = String(completion?.text ?? "").trim();
      if (!reply) throw new Error("no-content");
      body = {reply, actions: actionsFrom(reply, context.entries)};
      log(`200 request ${requestId} turns ${context.conversation.length} entries ${context.entries.length} model ${model()}`);
    } catch (failure) {
      // The provider is the production answer, not the only one: when it is
      // not configured or it fails, the gateway answers with the day computed
      // from the same minimal context, so the Coach never shows a dead end
      // (ADR-0020). An upstream outage is never reported as a client's error.
      body = fallbackAnswer(context);
      log(
        `200 request ${requestId} fallback (${failure.name ?? "error"}) entries ${context.entries.length}` +
          ` library ${context.libraryCodes.length}`,
      );
    }
    client.spend();
    respond(response, 200, body);
  } catch (failure) {
    log(`502 request ${requestId} upstream ${failure.name ?? "error"}`);
    respond(response, 502, {error: "The gateway could not answer"});
  }
}

/** The server, plain HTTP in a container or HTTPS on a development harness. */
function createServerOptions() {
  const cert = process.env.AI_GATEWAY_TLS_CERT;
  const key = process.env.AI_GATEWAY_TLS_KEY;
  if (!cert || !key) return {};
  return {cert: fs.readFileSync(cert), key: fs.readFileSync(key)};
}

const options = createServerOptions();
const factory = options.cert ? https.createServer.bind(null, options) : http.createServer.bind(null);
const server = factory((request, response) => {
  route(request, response).catch((failure) => {
    log("Unhandled route failure: internal server error");
    respond(response, 500, {error: "Internal error"});
  });
});

server.headersTimeout = 35_000;
server.requestTimeout = 40_000;
server.listen(PORT, () => {
  log(`the gateway listens on ${server.address().port}, model ${model()}, provider ${providerKind()}`);
});

for (const signal of ["SIGINT", "SIGTERM"]) {
  process.on(signal, () => {
    log(`received ${signal}, closing`);
    server.close(() => process.exit(0));
    setTimeout(() => process.exit(0), 10_000).unref();
  });
}
