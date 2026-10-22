package com.example.nutricionalregister.domain.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NutriLogDestinationTest {
    @Test
    fun `the app opens on the day`() {
        assertEquals(NutriLogDestination.TODAY, NutriLogDestination.start)
    }

    @Test
    fun `the shell exposes exactly three destinations`() {
        assertEquals(3, NutriLogDestination.entries.size)
    }

    @Test
    fun `every destination resolves from its own route`() {
        NutriLogDestination.entries.forEach { destination ->
            assertEquals(destination, NutriLogDestination.fromRoute(destination.route))
        }
    }

    @Test
    fun `each route resolves to its own destination`() {
        assertEquals(NutriLogDestination.TODAY, NutriLogDestination.fromRoute("today"))
        assertEquals(NutriLogDestination.LIBRARY, NutriLogDestination.fromRoute("library"))
        assertEquals(NutriLogDestination.MORE, NutriLogDestination.fromRoute("more"))
    }

    @Test
    fun `an unknown route resolves to no destination`() {
        assertNull(NutriLogDestination.fromRoute("search"))
        assertNull(NutriLogDestination.fromRoute("addfood/source"))
        assertNull(NutriLogDestination.fromRoute(""))
    }

    @Test
    fun `a null route resolves to no destination`() {
        assertNull(NutriLogDestination.fromRoute(null))
    }

    @Test
    fun `every destination carries a label and a route`() {
        NutriLogDestination.entries.forEach { destination ->
            assertEquals(true, destination.route.isNotBlank())
            assertEquals(true, destination.label.isNotBlank())
        }
    }
}
