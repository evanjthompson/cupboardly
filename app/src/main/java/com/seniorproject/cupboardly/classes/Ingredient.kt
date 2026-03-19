package com.seniorproject.cupboardly.classes

import android.content.Context
import com.seniorproject.cupboardly.room.database.AppDatabase
import com.seniorproject.cupboardly.room.entity.IngredientEntity

class Ingredient(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val ingredientDao = db.ingredientDao()

    suspend fun addIngredient(
        name: String,
        quantity: Double,
        unit: String,
        density: Double,
        price: Double,
        dateEntered: Int,
        dateLastUpdated: Int
    ) {
        val gramsToStore = convertToGrams(quantity, unit, density)

        val existing = ingredientDao.getIngredientByName(name)

        if (existing != null) {
            val newAllTimeQuantity = existing.allTimeQuantity + gramsToStore
            val newAllTimePrice = existing.allTimePrice + price
            val newQuantity = existing.quantity + gramsToStore
            val newPrice = existing.price + price
            val displayQty = convertFromGrams(newQuantity, existing.unit, existing.density)

            val updatedIngredient = existing.copy(
                quantity = newQuantity,
                price = newPrice,
                allTimeQuantity = newAllTimeQuantity,
                allTimePrice = newAllTimePrice,
                pricePerUnit = if (displayQty > 0) newPrice / displayQty else 0.0,
                dateLastUpdated = dateLastUpdated
            )
            ingredientDao.update(updatedIngredient)
        } else {
            val ingredient = IngredientEntity(
                name = name,
                quantity = gramsToStore,
                unit = unit,
                density = density,
                price = price,
                pricePerUnit = if (quantity > 0) price / quantity else 0.0,
                allTimeQuantity = gramsToStore,
                allTimePrice = price,
                dateEntered = dateEntered,
                dateLastUpdated = dateLastUpdated
            )
            ingredientDao.insert(ingredient)
        }
    }

    suspend fun getIngredientByName(name: String): IngredientEntity? {
        return ingredientDao.getIngredientByName(name)
    }

    suspend fun updateIngredient(updatedIngredient: IngredientEntity) {
        val existing = ingredientDao.getIngredientById(updatedIngredient.id)
            ?: return

        val deltaQuantity = updatedIngredient.quantity - existing.quantity
        val deltaPrice = updatedIngredient.price - existing.price

        val newAllTimeQuantity = existing.allTimeQuantity + deltaQuantity
        val newAllTimePrice = existing.allTimePrice + deltaPrice

        val finalIngredient = updatedIngredient.copy(
            allTimeQuantity = newAllTimeQuantity,
            allTimePrice = newAllTimePrice,
            pricePerUnit = if (updatedIngredient.quantity > 0) {
                val displayQty = convertFromGrams(
                    updatedIngredient.quantity,
                    updatedIngredient.unit,
                    updatedIngredient.density
                )
                if (displayQty > 0) updatedIngredient.price / displayQty else 0.0
            } else 0.0
        )

        ingredientDao.update(finalIngredient)
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