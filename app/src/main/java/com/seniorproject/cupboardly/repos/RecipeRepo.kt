package com.seniorproject.cupboardly.repos

import android.content.Context
import com.seniorproject.cupboardly.room.database.AppDatabase
import com.seniorproject.cupboardly.room.entity.RecipeEntity
import com.seniorproject.cupboardly.room.entity.RecipeIngredientEntity

class RecipeRepo(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val recipeDao = db.recipeDao()
    private val recipeIngredientDao = db.recipeIngredientDao()
    private val ingredientDao = db.ingredientDao()

    suspend fun addRecipe(name: String, instructions: String, dateCreated: Int): Long {
        val recipe = RecipeEntity(name = name, instructions = instructions, dateCreated = dateCreated)
        return recipeDao.insert(recipe)
    }

    suspend fun updateRecipe(recipe: RecipeEntity) {
        recipeDao.update(recipe)
    }

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

    suspend fun deleteIngredientsForRecipe(recipeId: Long) {
        recipeIngredientDao.deleteByRecipeId(recipeId)
    }

    suspend fun deleteById(recipeId: Long) {
        recipeDao.deleteById(recipeId)
    }

    suspend fun getAll(): List<RecipeEntity> {
        return recipeDao.getAllRecipes()
    }

    suspend fun delete(recipe: RecipeEntity) {
        recipeDao.delete(recipe)
    }

    suspend fun incrementNumTimesFollowed(recipeId: Long) {
        recipeDao.incrementNumTimesFollowed(recipeId)
    }
}