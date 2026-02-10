package com.seniorproject.cupboardly.classes

import android.content.Context
import com.seniorproject.cupboardly.room.database.IngredientDatabase
import com.seniorproject.cupboardly.room.entity.IngredientEntity

class Ingredient(context: Context) {

    private val ingredientDao = IngredientDatabase.getDatabase(context).ingredientDao()

    suspend fun addIngredient(name: String, quantity: Double, unit: String, price: Double, pricePerUnit: Double, dateEntered: Int, dateLastUpdated: Int, amountUsedRecently: Double)
    {
        val ingredient = IngredientEntity(name = name, quantity = quantity, unit = unit, price = price, pricePerUnit = pricePerUnit, dateEntered = dateEntered, dateLastUpdated = dateLastUpdated, amountUsedRecently = amountUsedRecently)
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