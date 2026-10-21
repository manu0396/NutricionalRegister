package com.example.nutricionalregister.domain.purchase

import com.example.nutricionalregister.domain.product.Product
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SavedProductTest {
    @Test
    fun savedProductRetainsIdentityAndDisplayFields() {
        // GIVEN a saved product carrying a display snapshot
        val saved =
            SavedProduct(
                code = "0001",
                name = "Oats",
                brands = "Acme",
                quantity = "500 g",
                nutriScoreGrade = "a",
                imageThumbnailUrl = "https://example.test/1.jpg",
                savedAtEpochMillis = 1_700_000_000_000L,
            )

        // THEN every value is retained on read
        assertEquals("0001", saved.code)
        assertEquals("Oats", saved.name)
        assertEquals("Acme", saved.brands)
        assertEquals("500 g", saved.quantity)
        assertEquals("a", saved.nutriScoreGrade)
        assertEquals("https://example.test/1.jpg", saved.imageThumbnailUrl)
        assertEquals(1_700_000_000_000L, saved.savedAtEpochMillis)

        // AND optional fields default to null so an offline entry is always valid
        val minimal = SavedProduct(code = "0002", name = "Rice", savedAtEpochMillis = 1L)
        assertNull(minimal.brands)
        assertNull(minimal.quantity)
        assertNull(minimal.nutriScoreGrade)
        assertNull(minimal.imageThumbnailUrl)
    }

    @Test
    fun savedProductRejectsBlankIdentity() {
        // WHEN a saved product is constructed with a blank code or name
        // THEN construction fails fast for each invalid value
        assertFailsWith<IllegalArgumentException>("blank code") {
            SavedProduct(code = " ", name = "Oats", savedAtEpochMillis = 1L)
        }
        assertFailsWith<IllegalArgumentException>("blank name") {
            SavedProduct(code = "0001", name = "", savedAtEpochMillis = 1L)
        }
    }

    @Test
    fun fromProductCopiesOnlyTheDisplayFieldsAndTheGivenTimestamp() {
        // GIVEN a catalogue product with fields a saved entry does not keep
        val product =
            Product(
                code = "0003",
                name = "Milk",
                brands = "Dairy Co",
                quantity = "1 L",
                nutritionDataPer = "100 ml",
                nutriScoreGrade = "b",
                imageThumbnailUrl = "https://example.test/3.jpg",
                imageUrl = "https://example.test/3-full.jpg",
            )

        // WHEN it is saved with a fixed timestamp
        val saved = SavedProduct.fromProduct(product, savedAtEpochMillis = 42L)

        // THEN the identity and display fields are copied
        assertEquals("0003", saved.code)
        assertEquals("Milk", saved.name)
        assertEquals("Dairy Co", saved.brands)
        assertEquals("1 L", saved.quantity)
        assertEquals("b", saved.nutriScoreGrade)
        assertEquals("https://example.test/3.jpg", saved.imageThumbnailUrl)
        assertEquals(42L, saved.savedAtEpochMillis)

        // AND a display field default flows through as null
        val plain = SavedProduct.fromProduct(Product(code = "0004", name = "Salt"), 1L)
        assertNull(plain.brands)
        assertTrue(plain.code == "0004")
    }
}
