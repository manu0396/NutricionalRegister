package com.example.nutricionalregister.feature.scan

import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import com.example.nutricionalregister.feature.scan.testing.FakeProductSearchRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BarcodeResolverTest {
    @Test
    fun aCodeTheCatalogueKnowsIsResolved() =
        runTest {
            val resolver = BarcodeResolver(FakeProductSearchRepository(listOf(product())))

            val resolution = resolver.resolve("0001")

            val resolved = assertIs<BarcodeResolution.Resolved>(resolution)
            assertEquals("0001", resolved.product.code)
            assertEquals("Oats", resolved.product.name)
        }

    @Test
    fun aCodeWithSurroundingSpacesIsResolved() =
        runTest {
            val resolver = BarcodeResolver(FakeProductSearchRepository(listOf(product())))

            val resolution = resolver.resolve("  0001  ")

            assertIs<BarcodeResolution.Resolved>(resolution)
        }

    @Test
    fun aCodeTheCatalogueDoesNotKnowIsNotInTheCache() =
        runTest {
            val repository = FakeProductSearchRepository(listOf(product()))
            val resolver = BarcodeResolver(repository)

            val resolution = resolver.resolve("9999")

            val notInCache = assertIs<BarcodeResolution.NotInCache>(resolution)
            assertEquals("9999", notInCache.code)
        }

    @Test
    fun anEmptyCatalogueKnowsNoCode() =
        runTest {
            val repository = FakeProductSearchRepository()
            val resolver = BarcodeResolver(repository)

            val resolution = resolver.resolve("0001")

            val notInCache = assertIs<BarcodeResolution.NotInCache>(resolution)
            assertEquals("0001", notInCache.code)
        }

    @Test
    fun aBlankCodeIsUnknownAndIsNeverLookedUp() =
        runTest {
            val repository = FakeProductSearchRepository(listOf(product()))
            val resolver = BarcodeResolver(repository)

            val resolution = resolver.resolve("   ")

            assertIs<BarcodeResolution.Unknown>(resolution)
            assertTrue(repository.requestedCodes.isEmpty())
        }

    private fun product(): Product =
        Product(
            code = "0001",
            name = "Oats",
            nutritionValues =
                listOf(
                    NutritionValue("energy", 380.0, null, NutritionBasis.PER_100_G),
                ),
        )
}
