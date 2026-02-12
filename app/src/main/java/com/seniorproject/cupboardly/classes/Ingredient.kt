package com.seniorproject.cupboardly.classes

import android.content.Context
import com.seniorproject.cupboardly.room.database.AppDatabase
import com.seniorproject.cupboardly.room.entity.IngredientEntity

class Ingredient(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val ingredientDao = db.ingredientDao()

    /**
     * Add an ingredient to inventory.
     * If ingredient with same name exists, increment quantity and update price info.
     */
    suspend fun addIngredient(
        name: String,
        quantity: Double,
        unit: String,
        price: Double,
        pricePerUnit: Double,
        dateEntered: Int,
        dateLastUpdated: Int
    ) {
        // Check if ingredient exists
        val existing = ingredientDao.getAllIngredients().find { it.name.equals(name, ignoreCase = true) }

        if (existing != null) {
            // Update existing ingredient
            val newQuantity = existing.quantity + quantity
            val newPrice = existing.price + price
            val updatedIngredient = existing.copy(
                quantity = newQuantity,
                price = newPrice,
                pricePerUnit = pricePerUnit,
                dateLastUpdated = dateLastUpdated
            )
            ingredientDao.update(updatedIngredient)
        } else {
            // Insert new ingredient
            val ingredient = IngredientEntity(
                name = name,
                quantity = quantity,
                unit = unit,
                price = price,
                pricePerUnit = pricePerUnit,
                dateEntered = dateEntered,
                dateLastUpdated = dateLastUpdated
            )
            ingredientDao.insert(ingredient)
        }
    }

    suspend fun getIngredientByName(name: String): IngredientEntity? {
        return ingredientDao.getIngredientByName(name)
    }

    suspend fun getIngredientById(id: Long): IngredientEntity? {
        return ingredientDao.getAllIngredients().find { it.id == id }
    }

    suspend fun getAll(): List<IngredientEntity> {
        return ingredientDao.getAllIngredients()
    }

    suspend fun delete(ingredient: IngredientEntity) {
        ingredientDao.delete(ingredient)
    }
}
