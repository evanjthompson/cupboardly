package com.seniorproject.cupboardly.room.dao

import androidx.room.*
import com.seniorproject.cupboardly.room.entity.RecipeEntity

@Dao
interface RecipeDao {

    @Insert
    suspend fun insert(recipe: RecipeEntity): Long

    @Update
    suspend fun update(recipe: RecipeEntity)

    @Delete
    suspend fun delete(recipe: RecipeEntity)

    @Query("DELETE FROM recipe_table WHERE id = :recipeId")
    suspend fun deleteById(recipeId: Long)

    @Query("SELECT * FROM recipe_table ORDER BY name ASC")
    suspend fun getAllRecipes(): List<RecipeEntity>

    @Query("UPDATE recipe_table SET numTimesFollowed = numTimesFollowed + 1 WHERE id = :id")
    suspend fun incrementNumTimesFollowed(id: Long)
}
