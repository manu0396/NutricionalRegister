package com.example.nutricionalregister.ui

import com.example.nutricionalregister.domain.navigation.AddFoodStep

/**
 * What the add-food flow is showing right now.
 *
 * The step comes from the route, so the screen always renders the step the
 * navigation state says it is on, and the two can never disagree.
 *
 * @property sessionId identifies the opening of the flow. The value changes
 *   every time the flow is opened, so the flow's state can be reset when it is
 *   opened again and kept when the user moves between its steps.
 * @property step the step the flow is on.
 * @property meal the meal the flow was opened with, if any.
 * @property productCode the food the flow was opened with, if any.
 */
public data class AddFoodDestination(
    public val sessionId: String,
    public val step: AddFoodStep,
    public val meal: String?,
    public val productCode: String?,
)
