package com.yanni.nutritrack.model

// Text supports an empty quantity while the form is being edited.
data class Ingredient(
    val id: Long,
    val name: String = "",
    val quantity: String = "",
    val unit: String = ""
)

data class Meal(
    val name: String,
    val ingredients: List<Ingredient>,
    val id: Long = 0,
    val consumedAt: Long = System.currentTimeMillis()
)
