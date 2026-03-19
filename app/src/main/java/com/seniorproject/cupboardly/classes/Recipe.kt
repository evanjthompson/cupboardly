package com.seniorproject.cupboardly.classes

import android.content.Context
import com.seniorproject.cupboardly.room.database.AppDatabase
import com.seniorproject.cupboardly.room.entity.RecipeEntity
import com.seniorproject.cupboardly.room.entity.RecipeIngredientEntity

class Recipe(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val recipeDao = db.recipeDao()
    private val recipeIngredientDao = db.recipeIngredientDao()
    private val ingredientDao = db.ingredientDao()

    // Add recipe and return the generated ID
    suspend fun addRecipe(name: String, instructions: String, dateCreated: Int): Long {
        val recipe = RecipeEntity(name = name, instructions = instructions, dateCreated = dateCreated)
        return recipeDao.insert(recipe)
    }

    // Link ingredient to recipe with specified quantity
    suspend fun addIngredientToRecipe(recipeId: Long, ingredientId: Long, quantityUsed: Double, unitUsed: String) {
        recipeIngredientDao.insert(
            RecipeIngredientEntity(
                recipeId = recipeId,
                ingredientId = ingredientId,
                quantityUsed = quantityUsed,
                unitUsed = unitUsed
            )
        )
    }

    suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity> {
        return recipeIngredientDao.getIngredientsForRecipe(recipeId)
    }

    suspend fun getAll(): List<RecipeEntity> {
        return recipeDao.getAllRecipes()
    }

    suspend fun delete(recipe: RecipeEntity) {
        recipeDao.delete(recipe)
    }
}
