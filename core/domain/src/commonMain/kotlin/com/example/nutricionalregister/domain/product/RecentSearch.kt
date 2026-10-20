package com.example.nutricionalregister.domain.product

public data class RecentSearch(
    val normalizedTerm: String,
    val displayTerm: String,
    val lastCompletedAtEpochMillis: Long,
    val completionSequence: Long,
) {
    init {
        require(normalizedTerm.isNotBlank()) { "Normalized term must not be blank" }
        require(displayTerm.isNotBlank()) { "Display term must not be blank" }
        require(completionSequence > 0L) { "Completion sequence must be positive" }
    }
}
