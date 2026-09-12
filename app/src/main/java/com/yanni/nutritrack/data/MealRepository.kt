package com.yanni.nutritrack.data

import com.yanni.nutritrack.model.Ingredient
import com.yanni.nutritrack.model.Meal
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

data class DayRange(val start: Long, val end: Long)

internal fun dayRange(
    now: Long = System.currentTimeMillis(),
    timeZone: TimeZone = TimeZone.getDefault()
): DayRange {
    val calendar = Calendar.getInstance(timeZone).apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = calendar.timeInMillis
    calendar.add(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    return DayRange(start, calendar.timeInMillis)
}

class MealRepository(private val dao: MealDao) {
    // Re-evaluate the local date while observed, including midnight/time-zone changes.
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeTodayMeals() = flow {
        while (true) {
            emit(dayRange())
            delay(30_000)
        }
    }.distinctUntilChanged().flatMapLatest { range ->
        dao.observeMealsBetween(range.start, range.end)
    }.map { rows ->
        rows.map { row ->
            Meal(
                id = row.meal.id,
                name = row.meal.name,
                consumedAt = row.meal.consumedAt,
                ingredients = row.ingredients.sortedBy { it.position }.map {
                    Ingredient(it.id, it.name, it.quantity.orEmpty(), it.unit.orEmpty())
                }
            )
        }
    }

    suspend fun saveMeal(meal: Meal) {
        dao.insertMealWithIngredients(
            MealEntity(
                name = meal.name.trim(),
                consumedAt = meal.consumedAt,
                timeZoneId = TimeZone.getDefault().id
            ),
            meal.ingredients.mapIndexed { index, ingredient ->
                IngredientEntity(
                    mealId = 0, // Replaced with the generated meal ID inside the transaction.
                    name = ingredient.name.trim(),
                    quantity = ingredient.quantity.trim().ifEmpty { null },
                    unit = ingredient.unit.trim().ifEmpty { null },
                    position = index
                )
            }
        )
    }

    suspend fun deleteMeal(mealId: Long) = dao.deleteMeal(mealId)
}
