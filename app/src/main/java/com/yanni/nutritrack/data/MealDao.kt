package com.yanni.nutritrack.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class MealDao {
    @Insert
    protected abstract suspend fun insertMeal(meal: MealEntity): Long

    @Insert
    protected abstract suspend fun insertIngredients(ingredients: List<IngredientEntity>)

    @Transaction
    open suspend fun insertMealWithIngredients(
        meal: MealEntity,
        ingredients: List<IngredientEntity>
    ): Long {
        require(meal.name.isNotBlank())
        require(ingredients.isNotEmpty() && ingredients.all { it.name.isNotBlank() })
        val mealId = insertMeal(meal)
        insertIngredients(ingredients.map { it.copy(id = 0, mealId = mealId) })
        return mealId
    }

    @Transaction
    @Query("SELECT * FROM meals WHERE consumedAt >= :start AND consumedAt < :end ORDER BY consumedAt DESC, id DESC")
    abstract fun observeMealsBetween(start: Long, end: Long): Flow<List<MealWithIngredients>>

    // Foreign-key cascading removes the meal's ingredients as well.
    @Query("DELETE FROM meals WHERE id = :mealId")
    abstract suspend fun deleteMeal(mealId: Long)
}
