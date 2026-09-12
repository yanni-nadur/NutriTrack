package com.yanni.nutritrack.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [MealEntity::class, IngredientEntity::class], version = 2, exportSchema = true)
abstract class NutriTrackDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE ingredients ADD COLUMN calories REAL")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN proteinGrams REAL")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN carbsGrams REAL")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN fatGrams REAL")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN nutritionStatus TEXT NOT NULL DEFAULT 'NOT_CALCULATED'")
                db.execSQL("ALTER TABLE ingredients ADD COLUMN nutritionSource TEXT")
            }
        }

        @Volatile private var instance: NutriTrackDatabase? = null

        fun getInstance(context: Context): NutriTrackDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NutriTrackDatabase::class.java,
                "nutritrack.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
