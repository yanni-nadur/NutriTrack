package com.yanni.nutritrack.data

import androidx.room.Embedded
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.yanni.nutritrack.nutrition.NutritionSource
import com.yanni.nutritrack.nutrition.NutritionStatus

@Entity(tableName = "meals", indices = [Index("consumedAt")])
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    // Epoch milliseconds: preserves the instant for later date-range queries.
    val consumedAt: Long,
    val timeZoneId: String
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [ForeignKey(
        entity = MealEntity::class,
        parentColumns = ["id"],
        childColumns = ["mealId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("mealId")]
)
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val name: String,
    val quantity: String? = null,
    val unit: String? = null,
    val position: Int,
    val calories: Double? = null,
    val proteinGrams: Double? = null,
    val carbsGrams: Double? = null,
    val fatGrams: Double? = null,
    @ColumnInfo(defaultValue = "'NOT_CALCULATED'")
    val nutritionStatus: NutritionStatus = NutritionStatus.NOT_CALCULATED,
    val nutritionSource: NutritionSource? = null
)

data class MealWithIngredients(
    @Embedded val meal: MealEntity,
    @Relation(parentColumn = "id", entityColumn = "mealId")
    val ingredients: List<IngredientEntity>
)
