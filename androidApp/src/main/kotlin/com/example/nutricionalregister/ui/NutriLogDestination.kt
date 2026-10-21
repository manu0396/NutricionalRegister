package com.example.nutricionalregister.ui

/**
 * The main areas the app's bottom navigation bar exposes (FR-005).
 *
 * Plain data (route + label + icon) so the same model can be reused by a future
 * native SwiftUI navigation without dragging Compose into shared code (ADR-0015).
 * The icon is an Android resource-free Material icon reference resolved by
 * `NutriLogBottomBar`, keeping this list free of Compose types.
 */
public enum class NutriLogDestination(
    public val route: String,
    public val label: String,
) {
    SEARCH(route = "search", label = "Search"),
    SAVED(route = "saved", label = "Saved products"),
    DIARY(route = "diary", label = "Diary"),
    ;

    public companion object {
        /** The destination the app opens on. */
        public val start: NutriLogDestination = SEARCH

        /** Resolves a [route] back to its destination, or null when unknown. */
        public fun fromRoute(route: String?): NutriLogDestination? = entries.firstOrNull { it.route == route }
    }
}
