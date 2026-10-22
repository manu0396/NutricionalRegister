/**
 * The provider behind the gateway (ADR-0020).
 *
 * The transport to the model is one function that returns the generated text.
 * The provider is a configuration choice of this service and not a fact the
 * Coach's contract depends on, which is what ADR-0016's replacement story
 * promises: the same envelope is served by whatever the environment points the
 * gateway at.
 *
 * Two protocols are spoken:
 * - Anthropic's own Messages API (`ANTHROPIC_API_KEY`);
 * - the OpenAI-compatible chat format, which is what a hosted provider, a
 *   self-hosted Ollama or a local LM Studio speak (`AI_GATEWAY_OPENAI_BASE_URL`
 *   plus an optional key). Self-hosted local engines are the development
 *   harness of a real deployment; the production answer is the model a funded
 *   key reaches.
 */

const ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
const ANTHROPIC_API_VERSION = "2023-06-01";
const DEFAULT_MODEL = "claude-sonnet-4-5";

/** The OpenAI-compatible path every chat-shaped endpoint shares. */
const OPENAI_CHAT_PATH = "/v1/chat/completions";
const DEFAULT_OPENAI_BASE_URL = "http://127.0.0.1:11434";

const UPSTREAM_TIMEOUT_MS = process.env.AI_GATEWAY_TIMEOUT_MS
  ? Number(process.env.AI_GATEWAY_TIMEOUT_MS)
  : 90_000;

/** The model the conversations are generated with. */
export function model() {
  return process.env.AI_GATEWAY_MODEL || process.env.OPENAI_MODEL || DEFAULT_MODEL;
}

/** Whether a provider is configured, so the health check can say so. */
export function isConfigured() {
  return Boolean(process.env.ANTHROPIC_API_KEY || process.env.AI_GATEWAY_OPENAI_BASE_URL || process.env.OPENAI_API_KEY);
}

/** The way the upstream is called: Anthropic's API when a key for it is set. */
function speakAnthropic() {
  return Boolean(process.env.ANTHROPIC_API_KEY) && !process.env.AI_GATEWAY_OPENAI_BASE_URL;
}

/** Answers one completion and reports the text it generated. */
export async function generate(request) {
  const completion = speakAnthropic() ? await anthropic(request) : await openAiCompatible(request);
  const text = (completion || "").trim();
  if (!text) throw new Error("no-content");
  return {text};
}

/** One completion over Anthropic's own API. */
async function anthropic(request) {
  const apiKey = process.env.ANTHROPIC_API_KEY;
  if (!apiKey) throw new Error("provider-not-configured");

  const upstream = new AbortController();
  const timer = setTimeout(() => upstream.abort(), UPSTREAM_TIMEOUT_MS);
  try {
    const response = await fetch(ANTHROPIC_API_URL, {
      method: "POST",
      signal: upstream.signal,
      headers: {
        "content-type": "application/json",
        "x-api-key": apiKey,
        "anthropic-version": ANTHROPIC_API_VERSION,
      },
      body: JSON.stringify({
        model: request.model,
        max_tokens: request.maxTokens,
        system: request.system,
        messages: request.messages,
      }),
    });
    if (!response.ok) throw new Error(`upstream-status-${response.status}`);

    const payload = await response.json();
    return (payload?.content || [])
      .filter((block) => block && block.type === "text")
      .map((block) => block.text)
      .filter(Boolean)
      .join("\n");
  } finally {
    clearTimeout(timer);
  }
}

/**
 * One completion over the OpenAI-compatible chat format.
 *
 * The base URL decides whether the model is hosted or on this machine, and the
 * conversation is sent in the shape that format defines: a system turn first,
 * then the app's turns.
 */
async function openAiCompatible(request) {
  const baseUrl = process.env.AI_GATEWAY_OPENAI_BASE_URL || process.env.OPENAI_API_BASE_URL || DEFAULT_OPENAI_BASE_URL;
  const apiKey = process.env.OPENAI_API_KEY || process.env.OLLAMA_API_KEY;

  const upstream = new AbortController();
  const timer = setTimeout(() => upstream.abort(), UPSTREAM_TIMEOUT_MS);
  try {
    const response = await fetch(`${baseUrl.replace(/\/+$/, "")}${OPENAI_CHAT_PATH}`, {
      method: "POST",
      signal: upstream.signal,
      headers: {
        "content-type": "application/json",
        ...(apiKey ? {authorization: `Bearer ${apiKey}`} : {}),
      },
      body: JSON.stringify({
        model: request.model,
        max_tokens: request.maxTokens,
        temperature: 0.4,
        messages: [
          {role: "system", content: request.system},
          ...request.messages,
        ],
      }),
    });
    if (!response.ok) throw new Error(`upstream-status-${response.status}`);

    const payload = await response.json();
    return payload?.choices
      ?.map((choice) => choice?.message?.content)
      .filter(Boolean)
      .join("\n");
  } finally {
    clearTimeout(timer);
  }
}

/** The name the health check mentions, so the operator can see what it answers with. */
export function providerKind() {
  if (speakAnthropic()) return "anthropic";
  if (process.env.AI_GATEWAY_OPENAI_BASE_URL || process.env.OPENAI_API_BASE_URL || process.env.OPENAI_API_KEY) {
    return "openai-compatible";
  }
  return "none";
}
