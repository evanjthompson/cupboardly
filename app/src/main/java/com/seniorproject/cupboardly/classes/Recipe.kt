package com.seniorproject.cupboardly.classes

import android.content.Context
import com.seniorproject.cupboardly.room.database.RecipeDatabase
import com.seniorproject.cupboardly.room.entity.RecipeEntity

class Recipe(context: Context) {

    private val recipeDao = RecipeDatabase.getDatabase(context).recipeDao()

    suspend fun addRecipe(name: String)
    {
        val recipe = RecipeEntity(name = name)
        recipeDao.insert(recipe)
    }

    suspend fun getAll(): List<RecipeEntity>
    {
        return recipeDao.getAllRecipes()
    }

    suspend fun delete(recipe: RecipeEntity)
    {
        recipeDao.delete(recipe)
    }
}