package com.example.cupboardly.room.dao

import androidx.room.*
import com.example.cupboardly.room.entity.RecipeEntity

@Dao
interface RecipeDao {

    @Insert
    suspend fun insert(recipe: RecipeEntity)

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Delete
    suspend fun delete(recipe: RecipeEntity)

    @Query("SELECT * FROM recipe_table ORDER BY name ASC")
    suspend fun getAllRecipes(): List<RecipeEntity>
}