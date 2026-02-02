package com.seniorproject.cupboardly.room.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.seniorproject.cupboardly.room.dao.IngredientDao
import com.seniorproject.cupboardly.room.entity.IngredientEntity

@Database(entities = [IngredientEntity::class], version = 1, exportSchema = false)
abstract class IngredientDatabase : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao

    companion object {
        @Volatile private var INSTANCE: IngredientDatabase? = null

        fun getDatabase(context: Context): IngredientDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IngredientDatabase::class.java,
                    "ingredient_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
