package com.example.nutricionalregister.data.remote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class SearchAliciousExceptionTest {
    @Test
    fun serviceFailureCarriesMessageStatusAndCause() {
        // GIVEN a catalogue response that failed with a status code
        val cause = IllegalStateException("Catalogue returned HTTP 503")

        // WHEN the service exception is constructed
        val exception =
            SearchAliciousException(
                message = "Catalogue returned HTTP 503",
                statusCode = 503,
                cause = cause,
            )

        // THEN it exposes the message, the status code, and the cause
        assertEquals("Catalogue returned HTTP 503", exception.message)
        assertEquals(503, exception.statusCode)
        assertSame(cause, exception.cause)
    }

    @Test
    fun serviceFailureDefaultsToUnknownStatusCodeAndCause() {
        // WHEN the service exception is constructed without optional details
        val exception = SearchAliciousException("Catalogue request timed out")

        // THEN status code and cause default to null
        assertNull(exception.statusCode)
        assertNull(exception.cause)
    }

    @Test
    fun malformedResponseFailureCarriesMessageAndCause() {
        // GIVEN a payload that could not be parsed
        val cause = IllegalStateException("Response is not a JSON object")

        // WHEN the malformed-response exception is constructed with and without a cause
        val withCause = MalformedCatalogueResponseException("Response is not a JSON object", cause)
        val withoutCause = MalformedCatalogueResponseException("Response is missing the hits array")

        // THEN it exposes the message and the optional cause
        assertEquals("Response is not a JSON object", withCause.message)
        assertSame(cause, withCause.cause)
        assertEquals("Response is missing the hits array", withoutCause.message)
        assertNull(withoutCause.cause)
    }
}
