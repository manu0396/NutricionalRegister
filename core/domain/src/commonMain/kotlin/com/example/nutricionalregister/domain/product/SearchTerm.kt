package com.example.nutricionalregister.domain.product

public data class SearchTerm(
    val normalizedTerm: String,
    val displayTerm: String,
) {
    init {
        require(normalizedTerm.isNotBlank()) { "Normalized term must not be blank" }
        require(displayTerm.isNotBlank()) { "Display term must not be blank" }
    }

    public companion object {
        public fun fromInput(input: String): SearchTerm? {
            val display = input.trim()
            if (display.isEmpty()) return null
            return SearchTerm(
                normalizedTerm = display.lowercase(),
                displayTerm = display,
            )
        }
    }
}
