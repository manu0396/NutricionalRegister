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

/**
 * The system prompt of the Coach, built from the kinds of action the app
 * honours.
 *
 * The order matters: the prose is asked for FIRST and the actions LAST, so a
 * model that is tempted to answer in actions alone still writes the answer to
 * the question it was asked. The marker is what the gateway splits the answer
 * on, which is why the exact line matters as much as the words.
 */
export function systemPrompt(meals) {
  const kinds = [...meals].join(" | ");
  return (
    "You are the Coach, the nutrition assistant of a food-tracking app.\n" +
    "DATA: the app sends the day's entries, the goal they are measured against " +
    "and the codes of the foods the user keeps. It is the only source you have.\n" +
    "\n" +
    "How you answer:\n" +
    "- first, in one to three sentences, answer THE question the user asked, " +
    "using the DATA: what was logged, its totals, what remains against the goal, " +
    "and what is unknown, said as unknown;\n" +
    "- answer in the language the user wrote in;\n" +
    "- never invent a nutrition fact for a food absent from the DATA;\n" +
    "- if the question is not about food, the day or the diary, say what you can " +
    "answer here instead of it.\n" +
    "\n" +
    "You may also propose at most two changes to the diary, and the app applies " +
    `none of them before the user confirms. A change is one of exactly these kinds:\n` +
    `{"type":"log","productCode":"<a libraryCodes code>","quantityGrams":100,"meal":"${kinds}"}\n` +
    `{"type":"remove","entryId":<an entryId from the DATA>}\n` +
    `{"type":"change","entryId":<an entryId>,"quantityGrams":100,"meal":"${kinds}"} where a ` +
    "quantity or a meal may be present and the other left out\n" +
    "Never propose a change when the user did not ask for one.\n" +
    "\n" +
    "Finish your answer with a line that reads exactly:\n" +
    "### ACTIONS\n" +
    "followed by a JSON array of the changes (or [] when you propose none), and " +
    "nothing after it. No other line may start with ###."
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
      totals: summaryOf(context.entries),
      remaining: {
        energyKcal: remainingKcal(context),
      },
      libraryCodes: context.libraryCodes,
    },
    null,
    2,
  );
}

/**
 * The day's numbers, computed by the gateway and never by the model.
 *
 * A small model that does arithmetic is a model that is wrong, so the gateway
 * sums what the app reported and says what is unknown in words; the model's
 * job is the phrasing and the advice, never the sums.
 */
export function summaryOf(entries) {
  const nutrient = (key) => {
    const reported = entries.filter((entry) => typeof entry[key] === "number");
    if (reported.length === 0) return null;
    const value = reported.reduce((total, entry) => total + entry[key], 0);
    return reported.length < entries.length ? {atLeast: Math.round(value)} : {value: Math.round(value)};
  };
  return {
    energyKcal: nutrient("energyKcal"),
    proteinGrams: nutrient("proteinGrams"),
    carbohydrateGrams: nutrient("carbohydrateGrams"),
    fatGrams: nutrient("fatGrams"),
    entries: entries.length,
  };
}

/** The calories the day still has room for, or null when the day is unknown. */
function remainingKcal(context) {
  const totals = summaryOf(context.entries).energyKcal;
  if (!totals) return null;
  const consumed = totals.atLeast ?? totals.value;
  return Math.max(0, Math.round(context.goal.energyKcal - consumed));
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
