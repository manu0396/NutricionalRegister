package com.example.nutricionalregister.data.remote

import com.example.nutricionalregister.domain.product.NutritionBasis
import com.example.nutricionalregister.domain.product.NutritionValue
import com.example.nutricionalregister.domain.product.Product
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject

internal object ProductResponseMapper {
    private data class NutrientField(
        val sourceKey: String,
        val displayKey: String,
        val unitKey: String,
    )

    private val nutrientFields =
        listOf(
            NutrientField("energy-kcal_100g", "energy", "energy-kcal_unit"),
            NutrientField("proteins_100g", "protein", "proteins_unit"),
            NutrientField("carbohydrates_100g", "carbohydrate", "carbohydrates_unit"),
            NutrientField("fat_100g", "fat", "fat_unit"),
            NutrientField("fiber_100g", "fiber", "fiber_unit"),
            NutrientField("sugars_100g", "sugars", "sugars_unit"),
            NutrientField("salt_100g", "salt", "salt_unit"),
            NutrientField("sodium_100g", "sodium", "sodium_unit"),
        )

    fun mapResponse(body: String): List<Product> {
        val root =
            try {
                Json.parseToJsonElement(body).jsonObject
            } catch (exception: SerializationException) {
                throw MalformedCatalogueResponseException("Response is not a JSON object", exception)
            } catch (exception: IllegalArgumentException) {
                throw MalformedCatalogueResponseException("Response is not a JSON object", exception)
            }
        val hits =
            root["hits"] as? JsonArray
                ?: throw MalformedCatalogueResponseException("Response is missing the hits array")
        return hits.mapNotNull { element ->
            val hit = element as? JsonObject ?: return@mapNotNull null
            mapHit(hit)
        }
    }

    fun mapHit(hit: JsonObject): Product? {
        val code = hit.string("code")?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val name = hit.string("product_name")?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val nutritionObject = hit["nutriments"] as? JsonObject
        val dataBasis = basisFor(hit.string("nutrition_data_per"))
        val values =
            nutrientFields.mapNotNull { field ->
                val amount = nutritionObject?.finiteDouble(field.sourceKey) ?: return@mapNotNull null
                NutritionValue(
                    nutrientKey = field.displayKey,
                    amount = amount,
                    unit = nutritionObject.string(field.unitKey)?.takeIf(String::isNotBlank),
                    basis = basisForField(field.sourceKey, dataBasis),
                )
            }

        return Product(
            code = code,
            name = name,
            brands = hit.stringOrArray("brands").cleanOptional(),
            quantity = hit.stringOrArray("quantity").cleanOptional(),
            nutritionDataPer = hit.string("nutrition_data_per").cleanOptional(),
            nutriScoreGrade = hit.nutriScoreGrade(),
            imageThumbnailUrl = hit.string("image_front_small_url").cleanOptional(),
            imageUrl = hit.string("image_front_url").cleanOptional(),
            nutritionValues = values,
        )
    }

    private fun basisFor(value: String?): NutritionBasis =
        when {
            value.isNullOrBlank() -> NutritionBasis.UNKNOWN
            value.contains("100g", ignoreCase = true) || value.contains("100 g", ignoreCase = true) ->
                NutritionBasis.PER_100_G
            value.contains("serving", ignoreCase = true) -> NutritionBasis.PER_SERVING
            else -> NutritionBasis.UNKNOWN
        }

    private fun basisForField(
        sourceKey: String,
        fallback: NutritionBasis,
    ): NutritionBasis =
        when {
            sourceKey.endsWith("_100g") -> NutritionBasis.PER_100_G
            sourceKey.endsWith("_serving") -> NutritionBasis.PER_SERVING
            else -> fallback
        }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)
            ?.takeIf(JsonPrimitive::isString)
            ?.content

    private fun JsonObject.stringOrArray(key: String): String? =
        when (val element = this[key]) {
            is JsonArray ->
                element
                    .mapNotNull { child ->
                        (child as? JsonPrimitive)?.takeIf(JsonPrimitive::isString)?.content?.trim()
                    }.filter(String::isNotEmpty)
                    .joinToString(", ")
                    .takeIf(String::isNotEmpty)
            is JsonPrimitive -> element.takeIf(JsonPrimitive::isString)?.content
            else -> null
        }

    private fun JsonObject.nutriScoreGrade(): String? {
        val raw =
            (string("nutriscore_grade") ?: stringOrArray("nutriscore_grade"))
                ?.trim()
                ?.lowercase()
        return raw?.takeIf { it.isNotEmpty() && it != "unknown" }
    }

    private fun JsonObject.finiteDouble(key: String): Double? {
        val element: JsonElement = this[key] ?: return null
        val primitive = element as? JsonPrimitive ?: return null
        val number = primitive.doubleOrNull ?: return null
        return number.takeIf(Double::isFinite)
    }

    private fun String?.cleanOptional(): String? = this?.trim()?.takeIf(String::isNotEmpty)
}

internal class MalformedCatalogueResponseException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
