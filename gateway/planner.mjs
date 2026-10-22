/**
 * The plan of a day that is not finished (ADR-0020).
 *
 * "Complete my meals so I reach my goal" is not a request the model can be
 * trusted with: it has to spread an exact number of calories over the meals
 * that are still empty, in grams, from the foods the user actually keeps. That
 * is arithmetic, and a model that does arithmetic is a model that is wrong.
 *
 * So the plan is computed here, in grams that reach the day's goal with the
 * foods of the user's library: one portion per meal that has no entries, split
 * evenly, and then topped up in a second pass when a portion hit the largest
 * size the app sensible serves. The model is asked to write the words for the
 * plan the gateway computed; the plan's actions are the ones the gateway
 * proposes, never the model's own.
 *
 * The plan proposes only, exactly like every other action: the app validates
 * each one and applies it when the user confirms.
 */

import {summaryOf} from "./coach.mjs";

/** The meals of the day, in the order the app shows them. */
const MEAL_ORDER = ["BREAKFAST", "LUNCH", "DINNER", "SNACKS"];

/** The grams are rounded to, so a portion reads as a weighable amount. */
const GRAM_STEP = 5;
const MIN_PORTION_GRAMS = 10;
const MAX_PORTION_GRAMS = 600;

/**
 * The plan for the rest of the day, or null when it is not this day.
 *
 * The plan exists when the user asks for the day to be completed: at least one
 * meal is still empty, something remains of the goal, and the library has a
 * food with calories to fill it with.
 */
export function plan(context) {
  const remaining = remainingToGoal(context);
  if (!(remaining > 0)) return null;

  const logged = new Set((context.entries ?? []).map((entry) => String(entry.meal ?? "").toUpperCase()));
  const emptyMeals = MEAL_ORDER.filter((meal) => !logged.has(meal));
  if (emptyMeals.length === 0) return null;

  const foods = (context.library ?? []).filter((food) => typeof food?.energyKcal === "number" && food.energyKcal > 0);
  if (foods.length === 0) return null;

  const share = remaining / emptyMeals.length;
  const portions = emptyMeals.map((meal, index) => portion(foods[index % foods.length], meal, share));

  // A portion that hit its largest size leaves the day short, so the calories
  // its cap could not carry are given to the portions that can take them, in
  // the order of the day.
  let assigned = 0;
  portions.forEach((p) => {
    assigned += p.energyKcal;
  });
  let uncovered = Math.round(remaining - assigned);
  for (const portionPlan of portions) {
    if (uncovered <= 0) break;
    const room = (MAX_PORTION_GRAMS - portionPlan.grams) / 100 * portionPlan.food.energyKcal;
    if (!(room > 0)) continue;
    const extra = topUp(portionPlan, uncovered);
    uncovered -= extra;
  }

  const total = portions.reduce((sum, p) => sum + p.energyKcal, 0);
  const consumed = Math.round(context.goal.energyKcal - remaining);
  return {
    meals: emptyMeals,
    portions,
    actions: portions.map((p) => p.action),
    consumedKcal: consumed,
    goalEnergyKcal: context.goal.energyKcal,
    totalKcal: Math.round(total),
    /** Whether the plan's calories reach the goal, which the answer must not overstate. */
    reachesGoal: uncovered <= 0,
    uncoveredKcal: Math.max(0, uncovered),
  };
}

/** One portion of [food] for the meal [meal]: the grams that carry [targetKcal]. */
function portion(food, meal, targetKcal) {
  const gramsFloat = (targetKcal / food.energyKcal) * 100;
  const grams =
    Math.max(MIN_PORTION_GRAMS, Math.min(MAX_PORTION_GRAMS, Math.round(gramsFloat / GRAM_STEP) * GRAM_STEP));
  const energyKcal = roundOf((grams / 100) * food.energyKcal);
  return {
    meal,
    food,
    grams,
    energyKcal,
    action: {type: "log", productCode: food.code, quantityGrams: grams, meal},
  };
}

/** Grows [portionPlan] to carry up to [kcal] more, and reports what it carried. */
function topUp(portionPlan, kcal) {
  const gramsFloat = portionPlan.grams + (kcal / portionPlan.food.energyKcal) * 100;
  const grams = Math.min(MAX_PORTION_GRAMS, Math.round(gramsFloat / GRAM_STEP) * GRAM_STEP);
  const added = roundOf((grams / 100) * portionPlan.food.energyKcal) - portionPlan.energyKcal;
  if (!(added > 0)) return 0;
  portionPlan.grams = grams;
  portionPlan.energyKcal = roundOf((grams / 100) * portionPlan.food.energyKcal);
  portionPlan.action.quantityGrams = grams;
  return added;
}

/** The kcal of a portion, as a number the model can speak of. */
function roundOf(value) {
  return Math.round(value * 10) / 10;
}

/** The calories the day still has room for, or null when the day is unknown. */
function remainingToGoal(context) {
  const totals = summaryOf(context.entries ?? []).energyKcal;
  const consumed = totals?.atLeast ?? totals?.value;
  if (typeof consumed !== "number") return null;
  return Math.max(0, Math.round(context.goal.energyKcal - consumed));
}

/** The words the model is asked to write, for a plan the gateway computed. */
export function planPrompt(plan) {
  const lines =
    plan.portions
      .map(
        (portionPlan) =>
          `${portionPlan.meal}: ${portionPlan.grams} g of ${portionPlan.food.name} = ${portionPlan.energyKcal} kcal`,
      )
      .join("; ");
  return (
    "Complete my meals so I reach my daily goal.\n\n" +
    `A plan was computed for me, exactly: ${lines}. It adds ${plan.totalKcal} kcal, so the day ` +
    (plan.reachesGoal
      ? `reaches the goal: the day ends at about ${plan.consumedKcal + plan.totalKcal} of ${plan.goalEnergyKcal} kcal.`
      : `gets me to about ${plan.consumedKcal + plan.totalKcal} of ${plan.goalEnergyKcal} kcal, which is as far ` +
        "as my library goes, and the answer must say plainly what is still missing.") +
    "\nWrite the answer in your own words: what I have eaten today, what remains, and the food I " +
    "would eat for each of those meals with its quantity, so my calories reach my goal when I " +
    "confirm. Do not propose anything different from the plan."
  );
}

/**
 * The words the gateway says about its own plan, when the model has none.
 *
 * The plan is the gateway's, so words about it are the gateway's to keep: a
 * plan is answered with what it does, and a model that answers only in actions
 * does not stop the user from reading what is proposed to them.
 */
export function planReplyText(plan) {
  const introduced =
    `You have logged about ${plan.consumedKcal} kcal, and ${plan.goalEnergyKcal - plan.consumedKcal} are left ` +
    `toward the goal of ${plan.goalEnergyKcal}. Here is how the day gets there:`;
  const lines = plan.portions.map((portionPlan) => `- ${portionPlan.meal}: ${portionPlan.grams} g of ` +
    `${portionPlan.food.name} = ${portionPlan.energyKcal} kcal`);
  const closing =
    plan.reachesGoal
      ? `Confirm them and the day ends at about ${plan.consumedKcal + plan.totalKcal} kcal.`
      : `Confirm them and the day ends at about ${plan.consumedKcal + plan.totalKcal} kcal, as far as ` +
        "the foods you keep go. A food with more calories fills what is left, so add one to the library.";
  return [introduced, ...lines, closing].join("\n");
}
