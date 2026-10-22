/**
 * The provider behind the gateway (ADR-0020).
 *
 * The transport to the model is one function that returns the generated text;
 * the provider is a configuration choice of this service and not a fact the
 * Coach's contract depends on, which is what ADR-0016's replacement story
 * promises.
 */

const API_VERSION = "2023-06-01";
const DEFAULT_MODEL = "claude-sonnet-4-5";
const API_URL = "https://api.anthropic.com/v1/messages";
const UPSTREAM_TIMEOUT_MS = process.env.AI_GATEWAY_TIMEOUT_MS
  ? Number(process.env.AI_GATEWAY_TIMEOUT_MS)
  : 90_000;

/** The model the conversations are generated with. */
export function model() {
  return process.env.AI_GATEWAY_MODEL || DEFAULT_MODEL;
}

/** Whether a provider key is configured, so the health check can say so. */
export function isConfigured() {
  return Boolean(process.env.ANTHROPIC_API_KEY);
}

/**
 * Generates one completion.
 *
 * The provider's credentials are read from the environment and are never
 * logged. A failure of the upstream is reported as an opaque gateway error:
 * the caller answers 502, and the reply never leaks the upstream's internals.
 */
export async function generate(request) {
  const apiKey = process.env.ANTHROPIC_API_KEY;
  if (!apiKey) throw new Error("provider-not-configured");

  const upstream = new AbortController();
  const timer = setTimeout(() => upstream.abort(), UPSTREAM_TIMEOUT_MS);
  try {
    const response = await fetch(API_URL, {
      method: "POST",
      signal: upstream.signal,
      headers: {
        "content-type": "application/json",
        "x-api-key": apiKey,
        "anthropic-version": API_VERSION,
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
    const text = (payload?.content || [])
      .filter((block) => block && block.type === "text")
      .map((block) => block.text)
      .filter(Boolean)
      .join("\n");
    return {text};
  } finally {
    clearTimeout(timer);
  }
}
