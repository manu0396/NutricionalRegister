/**
 * The deterministic coach the gateway falls back on (ADR-0020).
 *
 * The gateway's production answer is the model a configured provider key
 * reaches. When the key is absent, unconfigured or fails, the gateway does not
 * answer nothing: it answers with the day computed from the same minimal
 * context the app sent, in the same envelope, in the words of a measurement
 * rather than in the words of a conversation.
 *
 * It is a lower bound on the Coach's capability, not a stand-in model: it will
 * never use language a model would, and the app's UI treats it exactly like
 * the model's answer, so the whole path from the consent to the confirmation
 * of a proposal stays testable without a provider.
 */

/** The sum of a nutrient the entries report; null when no entry reported it. */
function sumOf(entries, nutrient) {
  const reported = entries.filter((entry) => typeof entry[nutrient] === "number");
  if (reported.length === 0) return null;
  return reported.reduce((total, entry) => total + entry[nutrient], 0);
}

/** Whether at least one nutrient of the total is a lower bound only. */
function isPartial(entries, nutrient) {
  return entries.filter((entry) => typeof entry[nutrient] === "number").length < entries.length;
}

/** The human line of a nutrient that is not fully known. */
function amountLine(entries, nutrient, unit) {
  const value = sumOf(entries, nutrient);
  if (value === null) return `unknown ${unit}`;
  const prefix = isPartial(entries, nutrient) && entries.length > 0 ? "at least " : "";
  return `${prefix}${Math.round(value)} ${unit}`;
}

/** The deterministic answer, written from the day's own numbers. */
export function fallbackAnswer(context) {
  const {entries, goal, libraryCodes} = context;
  const energy = sumOf(entries, "energyKcal");
  const known = energy !== null;

  const lines = ["Here is where your day stands:"];
  lines.push(
    known
      ? `You logged about ${Math.round(energy)} kcal ${(isPartial(entries, "energyKcal") ? "or more" : "")} of ${goal.energyKcal} kcal.`
      : `Your ${entries.length} logged entries carry no calories the catalogue reported, so today's calories are unknown.`,
  );
  lines.push(
    `Protein ${amountLine(entries, "proteinGrams", "g")}, carbohydrate ${amountLine(entries, "carbohydrateGrams", "g")}, fat ${amountLine(entries, "fatGrams", "g")}.`,
  );

  if (known && energy < goal.energyKcal) {
    lines.push(`You still have about ${Math.round(goal.energyKcal - energy)} kcal left today.`);
  }

  const leastLogged = entries.length === 0 ? "BREAKFAST" : "SNACKS";
  if (libraryCodes.length > 0) {
    const code = libraryCodes[0];
    lines.push(
      `If you want to log your first food here, I can propose ${code} into ${leastLogged}; confirm it and the entry is yours.`,
    );
    return {
      reply: lines.join(" "),
      actions: [{type: "log", productCode: code, quantityGrams: 100, meal: leastLogged}],
    };
  }

  lines.push("Your library is empty, so I cannot propose a food yet; add one in the Library and I will propose it here.");
  return {reply: lines.join(" "), actions: []};
}
