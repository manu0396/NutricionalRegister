package com.example.nutricionalregister.domain.navigation

/**
 * The three destinations the bottom navigation exposes (FR-009).
 *
 * The day is the home: [TODAY] is where the app opens, because a nutrition
 * tracker is a daily logging product and the food log is the thing the user
 * came for (FR-001). Search is deliberately **not** a destination: a catalogue
 * is an input to the diary, not a peer of it, so search is reached through the
 * add-food flow and from the Library empty state (FR-012, ADR-0019).
 *
 * Plain data (a route and a label) so the same model can be reused by a future
 * native SwiftUI navigation without dragging Compose into shared code
 * (ADR-0015, ADR-0019).
 *
 * @property route the route of the destination's root screen.
 * @property label the label shown under the destination's icon.
 */
public enum class NutriLogDestination(
    public val route: String,
    public val label: String,
) {
    TODAY("today", "Today"),
    LIBRARY("library", "Library"),
    MORE("more", "More"),
    ;

    public companion object {
        /** The destination the app opens on. */
        public val start: NutriLogDestination = TODAY

        /**
         * Resolves a [route] back to its destination, or null when unknown.
         *
         * The lookup only ever answers for the three top-level routes. It is
         * deliberately NOT the rule that decides whether the bottom bar is
         * visible: a route inside a nested graph resolves to null, and a rule
         * that blanks the bar on a null answer is the exact defect ADR-0019
         * removes (FR-031).
         */
        public fun fromRoute(route: String?): NutriLogDestination? = entries.firstOrNull { it.route == route }
    }
}
