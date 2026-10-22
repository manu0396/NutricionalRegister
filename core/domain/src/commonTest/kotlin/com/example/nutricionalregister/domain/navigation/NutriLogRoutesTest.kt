package com.example.nutricionalregister.domain.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NutriLogRoutesTest {
    @Test
    fun `a product detail route carries the code`() {
        assertEquals("product/123", NutriLogRoutes.productDetail("123"))
    }

    @Test
    fun `a product detail route trims the code`() {
        assertEquals("product/123", NutriLogRoutes.productDetail("  123  "))
    }

    @Test
    fun `a product detail route without a code is rejected`() {
        assertFailsWith<IllegalArgumentException> { NutriLogRoutes.productDetail("") }
        assertFailsWith<IllegalArgumentException> { NutriLogRoutes.productDetail("   ") }
    }

    @Test
    fun `the add food route without arguments opens the graph with empty arguments`() {
        assertEquals("addfood?meal=&product=", NutriLogRoutes.addFood())
    }

    @Test
    fun `the add food route carries the meal and the product`() {
        assertEquals("addfood?meal=LUNCH&product=123", NutriLogRoutes.addFood(meal = "LUNCH", productCode = "123"))
    }

    @Test
    fun `a blank add food argument is treated as absent`() {
        assertEquals("addfood?meal=&product=", NutriLogRoutes.addFood(meal = "  ", productCode = ""))
    }

    @Test
    fun `the destination routes and the route constants agree`() {
        assertEquals(NutriLogRoutes.TODAY, NutriLogDestination.TODAY.route)
        assertEquals(NutriLogRoutes.LIBRARY, NutriLogDestination.LIBRARY.route)
        assertEquals(NutriLogRoutes.MORE, NutriLogDestination.MORE.route)
    }

    @Test
    fun `the child routes are not destination routes`() {
        val childRoutes =
            listOf(
                NutriLogRoutes.ADD_FOOD_GRAPH,
                NutriLogRoutes.ADD_FOOD_SOURCE,
                NutriLogRoutes.ADD_FOOD_QUANTITY,
                NutriLogRoutes.PRODUCT_DETAIL,
                NutriLogRoutes.COACH,
                NutriLogRoutes.WEEKLY,
            )
        childRoutes.forEach { route ->
            assertEquals(null, NutriLogDestination.fromRoute(route))
        }
    }
}
