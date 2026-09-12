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
