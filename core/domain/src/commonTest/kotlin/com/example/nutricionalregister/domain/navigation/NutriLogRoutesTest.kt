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
    fun `the add food route without arguments opens the source step`() {
        assertEquals("addfood/source?meal=&product=", NutriLogRoutes.addFood())
    }

    @Test
    fun `the add food route carries the meal`() {
        assertEquals("addfood/source?meal=LUNCH&product=", NutriLogRoutes.addFood(meal = "LUNCH"))
    }

    @Test
    fun `the add food route with a food opens the quantity step`() {
        assertEquals("addfood/quantity?meal=&product=123", NutriLogRoutes.addFood(productCode = "123"))
    }

    @Test
    fun `the add food route carries the meal and the food`() {
        assertEquals(
            "addfood/quantity?meal=LUNCH&product=123",
            NutriLogRoutes.addFood(meal = "LUNCH", productCode = "123"),
        )
    }

    @Test
    fun `a blank add food argument is treated as absent`() {
        assertEquals("addfood/source?meal=&product=", NutriLogRoutes.addFood(meal = "  ", productCode = ""))
    }

    @Test
    fun `the quantity step route always names the two arguments`() {
        assertEquals("addfood/quantity?meal=&product=", NutriLogRoutes.addFoodQuantity())
        assertEquals("addfood/quantity?meal=LUNCH&product=123", NutriLogRoutes.addFoodQuantity("LUNCH", "123"))
    }

    @Test
    fun `the source step route always names the two arguments`() {
        assertEquals("addfood/source?meal=&product=", NutriLogRoutes.addFoodSource())
        assertEquals("addfood/source?meal=LUNCH&product=", NutriLogRoutes.addFoodSource("LUNCH"))
    }

    @Test
    fun `the declared step routes carry the optional arguments`() {
        assertEquals(
            "addfood/source?meal={meal}&product={product}",
            NutriLogRoutes.ADD_FOOD_SOURCE_ROUTE,
        )
        assertEquals(
            "addfood/quantity?meal={meal}&product={product}",
            NutriLogRoutes.ADD_FOOD_QUANTITY_ROUTE,
        )
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
