package com.yanni.nutritrack.nutrition

object NutritionCalculator {
    // Accept decimal dots or commas; never interpret thousands separators or free text.
    private val decimalQuantity = Regex("(?:[0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)")

    fun calculate(name: String, quantity: String?, unit: String?): Nutrition {
        val food = LocalNutritionCatalog.find(name) ?: return Nutrition()
        if (normalizeFoodText(unit.orEmpty()) != food.unit) return Nutrition()
        val text = quantity?.trim() ?: return Nutrition()
        if (!decimalQuantity.matches(text)) return Nutrition()
        val amount = text.replace(',', '.').toDoubleOrNull() ?: return Nutrition()
        if (!amount.isFinite() || amount <= 0.0) return Nutrition()
        val values = food.nutrition.scaledBy(amount / food.referenceQuantity)
        if (!values.isValid() || values.calories <= 0.0) return Nutrition()
        return Nutrition(
            values.calories, values.proteinGrams, values.carbsGrams, values.fatGrams,
            NutritionStatus.CALCULATED, NutritionSource.LOCAL_DATABASE
        )
    }
}
