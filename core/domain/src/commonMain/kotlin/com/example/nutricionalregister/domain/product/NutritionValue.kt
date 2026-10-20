package com.example.nutricionalregister.domain.product

public enum class NutritionBasis {
    PER_100_G,
    PER_SERVING,
    UNKNOWN,
}

public data class NutritionValue(
    val nutrientKey: String,
    val amount: Double,
    val unit: String?,
    val basis: NutritionBasis,
) {
    init {
        require(nutrientKey.isNotBlank()) { "Nutrient key must not be blank" }
        require(amount.isFinite()) { "Nutrition amount must be finite" }
    }
}
