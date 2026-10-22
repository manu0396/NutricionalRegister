/**
 * The conversation the Coach turns into a model request, and the actions that
 * come back (ADR-0016, FR-023 to FR-025).
 *
 * The actions the gateway sends are a proposal, never a change: the app
 * validates each one against the diary before it is applied, and only an
 * action the user confirms is written. A malformed action is dropped instead
 * of failing the whole reply, because the text of the answer is still useful.
 */

const MARKER = "### ACTIONS";

/** The system prompt of the Coach, built from the kinds of action the app honours. */
export function systemPrompt(meals) {
  const kinds = [...meals].join(" | ");
  return (
    "You are the Coach, the nutrition assistant of a food-tracking app. " +
    "You answer with the food the user logged today, the targets they set and the " +
    "foods they keep in their library.\n" +
    "Never invent a nutrition fact for a food you were not given; when a nutrient " +
    "is missing from the data, say plainly that it is unknown instead of guessing. " +
    "Answer in the language the user writes in, be concrete and keep the answer short.\n" +
    "You can propose changes to the user's diary, and the app applies one only after " +
    `the user confirms it. A change is exactly one of these kinds: a log, with a {"type":"log",` +
    `"productCode":"<a code from the libraryCodes you were given>","quantityGrams":100,` +
    `"meal":"${kinds}"}; a remove, with {"type":"remove","entryId":<an entryId of the entries ` +
    'you were given"}; a change, with {"type":"change","entryId":<an entryId>,"quantityGrams":100,' +
    '"meal":"..."}, where the quantity or the meal may be present and the other left out.\n' +
    "When you propose nothing, end your answer with an empty array.\n" +
    "\nEnd your answer with a line that is exactly `### ACTIONS` followed by a JSON array " +
    "of the changes, and nothing after it."
  );
}

/** The conversation, in the provider's shape: only the turns the app sent. */
export function conversationFor(context) {
  return context.conversation.map((turn) => ({role: turn.role, content: turn.text}));
}

/** The diary context, written for the model so it can answer about the day. */
export function contextFor(context) {
  return JSON.stringify(
    {
      entries: context.entries,
      goal: context.goal,
      libraryCodes: context.libraryCodes,
    },
    null,
    2,
  );
}

/**
 * The actions proposed in the reply, already cleaned.
 *
 * A reply the model wrote without the marker is answered with no actions, so
 * the app still shows the text and nothing is applied.
 */
export function actionsFrom(reply, entries) {
  const marker = reply.lastIndexOf(MARKER);
  if (marker < 0) return [];
  const payload = reply.slice(marker + MARKER.length).trim();
  const fenced = payload.match(/```(?:json)?\s*([\[\{].*?[\]\}])\s*```/s);
  const candidate = fenced ? fenced[1] : payload;
  let parsed;
  try {
    parsed = JSON.parse(candidate);
  } catch {
    return [];
  }
  if (!Array.isArray(parsed)) return [];
  return parsed.map((action) => cleaned(action, entries)).filter(Boolean);
}

const MEALS_PERMITTED = new Set(["BREAKFAST", "LUNCH", "DINNER", "SNACKS"]);

/** One action's proposed change, or null when the gateway must not forward it. */
function cleaned(action) {
  if (!action || typeof action !== "object" || Array.isArray(action)) return null;
  const kind = String(action.type ?? action.kind ?? "").toLowerCase();

  if (kind === "log") {
    const productCode = typeof action.productCode === "string" ? action.productCode.trim() : "";
    const quantityGrams = Number(action.quantityGrams);
    const meal = String(action.meal ?? "").toUpperCase();
    if (!productCode || !MEALS_PERMITTED.has(meal)) return null;
    const grams = Number.isFinite(quantityGrams) && quantityGrams > 0 ? quantityGrams : null;
    if (!grams) return null;
    return {type: "log", productCode: productCode.slice(0, 64), quantityGrams: grams, meal};
  }

  if (kind === "remove") {
    const entryId = Number(action.entryId);
    if (!Number.isInteger(entryId) || entryId <= 0) return null;
    return {type: "remove", entryId};
  }

  if (kind === "change") {
    const entryId = Number(action.entryId);
    if (!Number.isInteger(entryId) || entryId <= 0) return null;
    const change = {type: "change", entryId};
    const quantityGrams = Number(action.quantityGrams);
    if (typeof action.quantityGrams === "number" && Number.isFinite(quantityGrams) && quantityGrams > 0) {
      change.quantityGrams = quantityGrams;
    }
    const meal = String(action.meal ?? "").toUpperCase();
    if (MEALS_PERMITTED.has(meal)) change.meal = meal;
    if (change.quantityGrams === undefined && change.meal === undefined) return null;
    return change;
  }

  return null;
}
