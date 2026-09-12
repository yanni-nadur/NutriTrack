package com.yanni.nutritrack.nutrition

import java.text.Normalizer
import java.util.Locale

internal fun normalizeFoodText(text: String): String = Normalizer
    .normalize(text.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .replace(Regex("\\s+"), " ")

data class ReferenceFood(
    val name: String,
    val aliases: List<String>,
    val unit: String,
    val referenceQuantity: Double,
    val nutrition: NutritionValues
)

// TEMPORARY MVP REFERENCE DATA supplied for architecture validation.
// These are not final production nutrition data. Replace this isolated catalog
// with an imported, verified food database later; do not expand it via guesses.
object LocalNutritionCatalog {
    private val foods = listOf(
        ReferenceFood(
            "Arroz branco cozido", listOf("arroz", "arroz branco", "arroz branco cozido"),
            "g", 100.0, NutritionValues(130.0, 2.7, 28.2, 0.3)
        ),
        ReferenceFood(
            "Feijão carioca cozido", listOf("feijão", "feijao", "feijão carioca", "feijao carioca"),
            "g", 100.0, NutritionValues(76.0, 4.8, 13.6, 0.5)
        ),
        ReferenceFood(
            "Peito de frango grelhado",
            listOf("frango", "peito de frango", "peito de frango grelhado", "frango grelhado"),
            "g", 100.0, NutritionValues(165.0, 31.0, 0.0, 3.6)
        ),
        ReferenceFood("Banana", listOf("banana"), "g", 100.0, NutritionValues(89.0, 1.1, 22.8, 0.3)),
        ReferenceFood("Ovo", listOf("ovo", "ovo inteiro"), "unidade", 1.0, NutritionValues(72.0, 6.3, 0.4, 4.8))
    )
    private val byAlias = foods.flatMap { food ->
        (food.aliases + food.name).map { normalizeFoodText(it) to food }
    }.toMap()

    fun find(name: String): ReferenceFood? = byAlias[normalizeFoodText(name)]
}
