package com.example.nutricionalregister.domain.product

public data class Product(
    val code: String,
    val name: String,
    val brands: String? = null,
    val quantity: String? = null,
    val nutritionDataPer: String? = null,
    val nutriScoreGrade: String? = null,
    val imageThumbnailUrl: String? = null,
    val imageUrl: String? = null,
    val nutritionValues: List<NutritionValue> = emptyList(),
) {
    init {
        require(code.isNotBlank()) { "Product code must not be blank" }
        require(name.isNotBlank()) { "Product name must not be blank" }
    }
}
