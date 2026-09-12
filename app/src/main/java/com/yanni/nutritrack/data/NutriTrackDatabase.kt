package com.yanni.nutritrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MealEntity::class, IngredientEntity::class], version = 1, exportSchema = true)
abstract class NutriTrackDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        @Volatile private var instance: NutriTrackDatabase? = null

        fun getInstance(context: Context): NutriTrackDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NutriTrackDatabase::class.java,
                "nutritrack.db"
            ).build().also { instance = it }
        }
    }
}
