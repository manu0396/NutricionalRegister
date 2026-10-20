package com.example.nutricionalregister.data.remote

import com.example.nutricionalregister.domain.product.NutritionBasis
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProductResponseMapperTest {
    @Test
    fun ignoresHitsWithoutIdentityOrDisplayName() {
        val hit = Json.parseToJsonElement("""{"code":"x","product_name":" "}""").jsonObject
        val noCode = Json.parseToJsonElement("""{"product_name":"Beans"}""").jsonObject

        assertNull(ProductResponseMapper.mapHit(hit))
        assertNull(ProductResponseMapper.mapHit(noCode))
    }

    @Test
    fun keepsValidHitsWhenOptionalNutritionIsMalformed() {
        val hit =
            Json
                .parseToJsonElement(
                    """{"code":"00042","product_name":"Beans","nutriments":{"proteins_100g":"bad","fat_100g":0}}""",
                ).jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertEquals("00042", product.code)
        assertEquals(1, product.nutritionValues.size)
        assertEquals(0.0, product.nutritionValues.single().amount)
        assertEquals(NutritionBasis.PER_100_G, product.nutritionValues.single().basis)
    }

    @Test
    fun rejectsNonFiniteNutritionButRetainsProduct() {
        val hit =
            Json
                .parseToJsonElement(
                    """{"code":"00042","product_name":"Beans","nutriments":{"fat_100g":1e999}}""",
                ).jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertTrue(product.nutritionValues.isEmpty())
    }

    @Test
    fun mapsPresentImageUrls() {
        val hit =
            Json
                .parseToJsonElement(
                    """
                    {
                      "code":"00042",
                      "product_name":"Beans",
                      "image_front_small_url":"https://images.example/beans.small.jpg",
                      "image_front_url":"https://images.example/beans.jpg"
                    }
                    """.trimIndent(),
                ).jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertEquals("https://images.example/beans.small.jpg", product.imageThumbnailUrl)
        assertEquals("https://images.example/beans.jpg", product.imageUrl)
    }

    @Test
    fun absentImageUrlsBecomeNull() {
        val hit = Json.parseToJsonElement("""{"code":"00042","product_name":"Beans"}""").jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertNull(product.imageThumbnailUrl)
        assertNull(product.imageUrl)
    }

    @Test
    fun blankImageUrlsBecomeNull() {
        val hit =
            Json
                .parseToJsonElement(
                    """{"code":"00042","product_name":"Beans","image_front_small_url":"  ","image_front_url":"\t"}""",
                ).jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertNull(product.imageThumbnailUrl)
        assertNull(product.imageUrl)
    }

    @Test
    fun keepsValidProductsAmongMalformedHits() {
        val products = ProductResponseMapper.mapResponse(SearchAliciousResponseFixtures.mixedProducts)

        assertEquals(listOf("00042"), products.map { it.code })
        assertEquals("Beans", products.single().name)
        assertEquals(
            0.0,
            products
                .single()
                .nutritionValues
                .single()
                .amount,
        )
    }

    @Test
    fun mapsBrandsFromJsonArrayJoinedWithComma() {
        val hit =
            Json
                .parseToJsonElement("""{"code":"00042","product_name":"Beans","brands":["Lima","Oatly"]}""")
                .jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertEquals("Lima, Oatly", product.brands)
    }

    @Test
    fun mapsBrandsFromJsonString() {
        val hit =
            Json
                .parseToJsonElement("""{"code":"00042","product_name":"Beans","brands":"Lima"}""")
                .jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertEquals("Lima", product.brands)
    }

    @Test
    fun mapsEmptyBrandsArrayToNull() {
        val hit =
            Json
                .parseToJsonElement("""{"code":"00042","product_name":"Beans","brands":[]}""")
                .jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertNull(product.brands)
    }

    @Test
    fun mapsQuantityFromJsonArray() {
        val hit =
            Json
                .parseToJsonElement("""{"code":"00042","product_name":"Beans","quantity":["1 L","33 cl"]}""")
                .jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertEquals("1 L, 33 cl", product.quantity)
    }

    @Test
    fun mapsUnknownNutriScoreGradeToNull() {
        val hit =
            Json
                .parseToJsonElement("""{"code":"00042","product_name":"Beans","nutriscore_grade":"unknown"}""")
                .jsonObject

        val product = requireNotNull(ProductResponseMapper.mapHit(hit))

        assertNull(product.nutriScoreGrade)
    }

    @Test
    fun keepsNutriScoreGradeLowercased() {
        val lower = Json.parseToJsonElement("""{"code":"00042","product_name":"Beans","nutriscore_grade":"e"}""")
        val upper = Json.parseToJsonElement("""{"code":"00042","product_name":"Beans","nutriscore_grade":"E"}""")

        assertEquals("e", requireNotNull(ProductResponseMapper.mapHit(lower.jsonObject)).nutriScoreGrade)
        assertEquals("e", requireNotNull(ProductResponseMapper.mapHit(upper.jsonObject)).nutriScoreGrade)
    }

    @Test
    fun mapsLiveStyleArrayFieldsFromFixture() {
        val products = ProductResponseMapper.mapResponse(SearchAliciousResponseFixtures.arrayBrandsProduct)

        val product = products.single()
        assertEquals("Lima, Oatly", product.brands)
        assertEquals("1 L, 33 cl", product.quantity)
        assertNull(product.nutriScoreGrade)
    }

    @Test
    fun mapsEmptyArrayFieldsFromFixtureToNull() {
        val products = ProductResponseMapper.mapResponse(SearchAliciousResponseFixtures.emptyArrayBrandsProduct)

        val product = products.single()
        assertNull(product.brands)
        assertNull(product.quantity)
    }
}
