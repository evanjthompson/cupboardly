package com.seniorproject.cupboardly.room.dao

import androidx.room.*
import com.seniorproject.cupboardly.room.entity.RecipeIngredientEntity

@Dao
interface RecipeIngredientDao {

    @Insert
    suspend fun insert(recipeIngredient: RecipeIngredientEntity)

    @Update
    suspend fun update(recipeIngredient: RecipeIngredientEntity)

    @Delete
    suspend fun delete(recipeIngredient: RecipeIngredientEntity)

    // ✅ NEW (REQUIRED)
    @Query("DELETE FROM recipe_ingredient_table WHERE recipeId = :recipeId")
    suspend fun deleteByRecipeId(recipeId: Long)

    @Query("SELECT * FROM recipe_ingredient_table WHERE recipeId = :recipeId")
    suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity>

    @Query("SELECT * FROM recipe_ingredient_table WHERE ingredientId = :ingredientId")
    suspend fun getRecipesForIngredient(ingredientId: Long): List<RecipeIngredientEntity>
}
