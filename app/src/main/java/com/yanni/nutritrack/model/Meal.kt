package com.yanni.nutritrack.model

import com.yanni.nutritrack.nutrition.Nutrition

// Text supports an empty quantity while the form is being edited.
data class Ingredient(
    val id: Long,
    val name: String = "",
    val quantity: String = "",
    val unit: String = "",
    val nutrition: Nutrition = Nutrition()
)

data class Meal(
    val name: String,
    val ingredients: List<Ingredient>,
    val id: Long = 0,
    val consumedAt: Long = System.currentTimeMillis()
)
