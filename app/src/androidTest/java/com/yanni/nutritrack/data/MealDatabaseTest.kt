package com.yanni.nutritrack.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MealDatabaseTest {
    private lateinit var context: Context
    private lateinit var database: NutriTrackDatabase
    private val databaseName = "meal-persistence-test.db"

    private fun openDatabase() = Room.databaseBuilder(
        context, NutriTrackDatabase::class.java, databaseName
    ).build()

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(databaseName)
        database = openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    private suspend fun insert(at: Long, name: String = "Almoço"): Long =
        database.mealDao().insertMealWithIngredients(
            MealEntity(name = name, consumedAt = at, timeZoneId = "America/Sao_Paulo"),
            listOf(
                IngredientEntity(mealId = 0, name = "Arroz", quantity = "150", unit = "g", position = 0),
                IngredientEntity(mealId = 0, name = "Feijão", position = 1)
            )
        )

    @Test
    fun mealAndOptionalIngredientsSurviveReopeningDatabase() = runBlocking {
        val id = insert(1500)
        database.close()
        database = openDatabase()
        val row = database.mealDao().observeMealsBetween(1000, 2000).first().single()
        assertEquals(id, row.meal.id)
        assertEquals(1500L, row.meal.consumedAt)
        assertEquals("America/Sao_Paulo", row.meal.timeZoneId)
        assertEquals(2, row.ingredients.size)
        assertTrue(row.ingredients.all { it.mealId == id && it.id > 0 })
        val optional = row.ingredients.single { it.name == "Feijão" }
        assertNull(optional.quantity)
        assertNull(optional.unit)
        val rice = row.ingredients.single { it.name == "Arroz" }
        assertEquals("150", rice.quantity)
        assertEquals("g", rice.unit)
    }

    @Test
    fun dateRangeIncludesStartAndExcludesNextDay() = runBlocking {
        insert(999, "Antes")
        insert(1000, "Início")
        insert(1999, "Fim")
        insert(2000, "Depois")
        assertEquals(
            listOf("Fim", "Início"),
            database.mealDao().observeMealsBetween(1000, 2000).first().map { it.meal.name }
        )
    }

    @Test
    fun deletingMealAlsoDeletesItsIngredients() = runBlocking {
        val id = insert(1500)
        database.mealDao().deleteMeal(id)
        assertTrue(database.mealDao().observeMealsBetween(1000, 2000).first().isEmpty())
        database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM ingredients").use {
            assertTrue(it.moveToFirst())
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun editingReplacesIngredientsAndPreservesMealDateAfterReopening() = runBlocking {
        val id = insert(1500)
        val untouchedId = insert(1600, "Outra refeição")
        val repository = MealRepository(database.mealDao())
        repository.saveMeal(
            com.yanni.nutritrack.model.Meal(
                id = id,
                name = "Almoço atualizado",
                ingredients = listOf(
                    com.yanni.nutritrack.model.Ingredient(1, "Arroz integral", "200", "g"),
                    com.yanni.nutritrack.model.Ingredient(3, "Salada", "", "")
                )
            )
        )
        database.close()
        database = openDatabase()
        val rows = database.mealDao().observeMealsBetween(1000, 2000).first()
        assertEquals(2, rows.size)
        val updated = rows.single { it.meal.id == id }
        assertEquals("Almoço atualizado", updated.meal.name)
        assertEquals(1500L, updated.meal.consumedAt)
        assertEquals("America/Sao_Paulo", updated.meal.timeZoneId)
        val ingredients = updated.ingredients.sortedBy { it.position }
        assertEquals(listOf("Arroz integral", "Salada"), ingredients.map { it.name })
        assertEquals("200", ingredients[0].quantity)
        assertEquals("g", ingredients[0].unit)
        assertNull(ingredients[1].quantity)
        assertNull(ingredients[1].unit)
        assertEquals(2, rows.single { it.meal.id == untouchedId }.ingredients.size)
    }

    @Test
    fun failedUpdateRestoresOriginalNameAndIngredients() = runBlocking {
        val id = insert(1500)
        val original = database.mealDao().observeMealsBetween(1000, 2000).first().single()
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_update BEFORE INSERT ON ingredients " +
                "WHEN NEW.name = 'Rejeitado' BEGIN SELECT RAISE(ABORT, 'Test failure'); END"
        )
        try {
            database.mealDao().updateMealWithIngredients(
                id, "Novo nome",
                listOf(IngredientEntity(mealId = id, name = "Rejeitado", position = 0))
            )
            fail("Expected the test trigger to reject the update")
        } catch (_: android.database.sqlite.SQLiteException) {
            val restored = database.mealDao().observeMealsBetween(1000, 2000).first().single()
            assertEquals(original.meal, restored.meal)
            assertEquals(original.ingredients.sortedBy { it.id }, restored.ingredients.sortedBy { it.id })
        }
    }

    @Test
    fun failedIngredientInsertRollsBackEntireMeal() = runBlocking {
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_ingredient BEFORE INSERT ON ingredients " +
                "WHEN NEW.name = 'Feijão' BEGIN SELECT RAISE(ABORT, 'Test failure'); END"
        )
        try {
            insert(1500)
            fail("Expected the test trigger to reject the second ingredient")
        } catch (_: android.database.sqlite.SQLiteException) {
            assertTrue(database.mealDao().observeMealsBetween(1000, 2000).first().isEmpty())
            database.openHelper.readableDatabase.query("SELECT COUNT(*) FROM ingredients").use {
                assertTrue(it.moveToFirst())
                assertEquals(0, it.getInt(0))
            }
        }
    }
}
