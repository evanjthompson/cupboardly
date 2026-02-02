package com.example.cupboardly.room.dao

import androidx.room.*
import com.example.cupboardly.room.entity.IngredientEntity

@Dao
interface IngredientDao {

    @Insert
    suspend fun insert(ingredient: IngredientEntity)

    @Update
    suspend fun update(ingredient: IngredientEntity)

    @Delete
    suspend fun delete(ingredient: IngredientEntity)

    @Query("SELECT * FROM ingredient_table ORDER BY name ASC")
    suspend fun getAllIngredients(): List<IngredientEntity>
}