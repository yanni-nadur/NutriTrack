package com.yanni.nutritrack.nutrition

enum class NutritionStatus { NOT_CALCULATED, CALCULATED, ESTIMATED, FAILED }
enum class NutritionSource { LOCAL_DATABASE, EXTERNAL_API, AI_ESTIMATE, USER_CONFIRMED }

data class Nutrition(
    val calories: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    val status: NutritionStatus = NutritionStatus.NOT_CALCULATED,
    val source: NutritionSource? = null
) {
    fun calculatedValues(): NutritionValues? {
        if (status != NutritionStatus.CALCULATED) return null
        val values = NutritionValues(
            calories ?: return null,
            proteinGrams ?: return null,
            carbsGrams ?: return null,
            fatGrams ?: return null
        )
        return values.takeIf { it.isValid() }
    }
}

// Used only when all four values are known; a real zero (e.g. chicken carbs) is valid.
data class NutritionValues(
    val calories: Double,
    val proteinGrams: Double,
    val carbsGrams: Double,
    val fatGrams: Double
) {
    fun isValid() = listOf(calories, proteinGrams, carbsGrams, fatGrams)
        .all { it.isFinite() && it >= 0.0 }

    fun scaledBy(factor: Double) = NutritionValues(
        calories * factor, proteinGrams * factor, carbsGrams * factor, fatGrams * factor
    )

    operator fun plus(other: NutritionValues) = NutritionValues(
        calories + other.calories,
        proteinGrams + other.proteinGrams,
        carbsGrams + other.carbsGrams,
        fatGrams + other.fatGrams
    )
}

data class NutritionTotals(
    val values: NutritionValues?,
    val calculatedCount: Int,
    val ingredientCount: Int
) {
    val isPartial: Boolean get() = values != null && calculatedCount < ingredientCount
}

fun totalNutrition(ingredients: List<Nutrition>): NutritionTotals {
    val known = ingredients.mapNotNull { it.calculatedValues() }
    // No zero starting value: an empty collection remains unknown.
    val total = known.reduceOrNull { sum, values -> sum + values }?.takeIf { it.isValid() }
    return NutritionTotals(total, known.size, ingredients.size)
}
