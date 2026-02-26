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
        val existing = ingredientDao.getIngredientByName(name)

        if (existing != null) {

            val newQuantity = existing.quantity + quantity
            val newPrice = existing.price + price

            val newAllTimeQuantity = existing.allTimeQuantity + quantity
            val newAllTimePrice = existing.allTimePrice + price

            val updatedIngredient = existing.copy(
                quantity = newQuantity,
                price = newPrice,
                allTimeQuantity = newAllTimeQuantity,
                allTimePrice = newAllTimePrice,
                pricePerUnit = newPrice / newQuantity,
                dateLastUpdated = dateLastUpdated
            )

            ingredientDao.update(updatedIngredient)

        } else {

            val ingredient = IngredientEntity(
                name = name,
                quantity = quantity,
                unit = unit,
                price = price,
                pricePerUnit = pricePerUnit,
                allTimeQuantity = quantity,
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
            pricePerUnit = if (updatedIngredient.quantity > 0)
                updatedIngredient.price / updatedIngredient.quantity
            else 0.0
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
