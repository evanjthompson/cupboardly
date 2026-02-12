package com.seniorproject.cupboardly.room.dao

import androidx.room.*
import com.seniorproject.cupboardly.room.entity.RecipeEntity

@Dao
interface RecipeDao {

    @Insert
    suspend fun insert(recipe: RecipeEntity): Long // returns the rowId of the inserted recipe

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Delete
    suspend fun delete(recipe: RecipeEntity)

    @Query("SELECT * FROM recipe_table ORDER BY name ASC")
    suspend fun getAllRecipes(): List<RecipeEntity>
}
