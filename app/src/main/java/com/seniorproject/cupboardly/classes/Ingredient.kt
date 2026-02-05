package com.seniorproject.cupboardly.classes

import android.content.Context
import com.seniorproject.cupboardly.room.database.IngredientDatabase
import com.seniorproject.cupboardly.room.entity.IngredientEntity

class Ingredient(context: Context) {

    private val ingredientDao = IngredientDatabase.getDatabase(context).ingredientDao()

    suspend fun addIngredient(name: String)
    {
        val ingredient = IngredientEntity(name = name)
        ingredientDao.insert(ingredient)
    }

    suspend fun getAll(): List<IngredientEntity>
    {
        return ingredientDao.getAllIngredients()
    }

    suspend fun delete(ingredient: IngredientEntity)
    {
        ingredientDao.delete(ingredient)
    }
}