package com.example.nutricionalregister.domain.navigation

/**
 * The steps of the add-food flow.
 *
 * The flow has two steps and no more: the food is chosen, and then the amount
 * and the meal are confirmed. The meal is not a step of its own; it is picked
 * from a modal sheet on the quantity step, because choosing a meal is not a
 * navigation step and giving it one would make the system back button dismiss
 * something that is not a destination (FR-030, ADR-0019).
 *
 * The step is part of the route, so the flow's position is a property of the
 * navigation state and not of a view model that could disagree with it.
 */
public enum class AddFoodStep {
    /** The step where the food is chosen: search, the personal library or a barcode. */
    SOURCE,

    /** The step where the quantity and the meal are confirmed. */
    QUANTITY,
}
