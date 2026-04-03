package com.seniorproject.cupboardly.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.seniorproject.cupboardly.room.dao.IngredientBatchDao
import com.seniorproject.cupboardly.room.dao.IngredientDao
import com.seniorproject.cupboardly.room.dao.RecipeDao
import com.seniorproject.cupboardly.room.dao.RecipeIngredientDao
import com.seniorproject.cupboardly.room.entity.IngredientBatchEntity
import com.seniorproject.cupboardly.room.entity.IngredientEntity
import com.seniorproject.cupboardly.room.entity.RecipeEntity
import com.seniorproject.cupboardly.room.entity.RecipeIngredientEntity

@Database(
    entities = [IngredientEntity::class, RecipeEntity::class, RecipeIngredientEntity::class, IngredientBatchEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun recipeIngredientDao(): RecipeIngredientDao
    abstract fun ingredientBatchDao(): IngredientBatchDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cupboardly_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}