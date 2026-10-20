package com.example.nutricionalregister.domain.product

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RecentSearchTest {
    @Test
    fun recentSearchRetainsTermsTimestampAndSequence() {
        // GIVEN a completed search entry
        val search =
            RecentSearch(
                normalizedTerm = "oat drink",
                displayTerm = "Oat Drink",
                lastCompletedAtEpochMillis = 1_700_000_000_000L,
                completionSequence = 5L,
            )

        // THEN every recorded value is retained on read
        assertEquals("oat drink", search.normalizedTerm)
        assertEquals("Oat Drink", search.displayTerm)
        assertEquals(1_700_000_000_000L, search.lastCompletedAtEpochMillis)
        assertEquals(5L, search.completionSequence)

        // AND copying preserves equality unless the sequence changes
        assertEquals(search, search.copy(completionSequence = 6L).copy(completionSequence = 5L))
        assertNotEquals(search, search.copy(completionSequence = 6L))
        assertTrue(search.toString().contains("Oat Drink"))
    }

    @Test
    fun recentSearchRejectsBlankTermsAndNonPositiveSequence() {
        // GIVEN invalid normalized terms, display terms, and sequences
        // WHEN recent searches are constructed with them
        // THEN construction fails fast for each invalid value
        assertFailsWith<IllegalArgumentException>("blank normalized term") {
            RecentSearch(
                normalizedTerm = " ",
                displayTerm = "Oat Drink",
                lastCompletedAtEpochMillis = 1L,
                completionSequence = 1L,
            )
        }
        assertFailsWith<IllegalArgumentException>("blank display term") {
            RecentSearch(
                normalizedTerm = "oat drink",
                displayTerm = "",
                lastCompletedAtEpochMillis = 1L,
                completionSequence = 1L,
            )
        }
        assertFailsWith<IllegalArgumentException>("zero sequence") {
            RecentSearch(
                normalizedTerm = "oat drink",
                displayTerm = "Oat Drink",
                lastCompletedAtEpochMillis = 1L,
                completionSequence = 0L,
            )
        }
        assertFailsWith<IllegalArgumentException>("negative sequence") {
            RecentSearch(
                normalizedTerm = "oat drink",
                displayTerm = "Oat Drink",
                lastCompletedAtEpochMillis = 1L,
                completionSequence = -1L,
            )
        }
    }
}
