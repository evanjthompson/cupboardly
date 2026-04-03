package com.seniorproject.cupboardly.classes

import android.content.Context
import com.seniorproject.cupboardly.room.database.AppDatabase
import com.seniorproject.cupboardly.room.entity.IngredientBatchEntity
import com.seniorproject.cupboardly.room.entity.IngredientEntity

class Ingredient(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val ingredientDao = db.ingredientDao()
    private val batchDao = db.ingredientBatchDao()

    // -------------------------------
    // Ingredient Definition
    // -------------------------------

    suspend fun addIngredientDefinition(
        name: String,
        density: Double,
        unit: String
    ): Long {
        val existing = ingredientDao.getIngredientByName(name)
        if (existing != null) return existing.id

        val ingredient = IngredientEntity(
            name = name,
            density = density,
            unit = unit
        )

        val id = ingredientDao.insert(ingredient)
        return id
    }

    suspend fun updateIngredientDefinition(updatedIngredient: IngredientEntity) {
        val existing = ingredientDao.getIngredientById(updatedIngredient.id) ?: return

        // Only update name, density, and unit
        val finalIngredient = existing.copy(
            name = updatedIngredient.name,
            density = updatedIngredient.density,
            unit = updatedIngredient.unit
        )
        ingredientDao.update(finalIngredient)
    }

    suspend fun deleteIngredientDefinition(ingredient: IngredientEntity) {
        ingredientDao.delete(ingredient)
    }

    suspend fun getIngredientByName(name: String): IngredientEntity? {
        return ingredientDao.getIngredientByName(name)
    }

    suspend fun getIngredientById(id: Long): IngredientEntity? {
        return ingredientDao.getIngredientById(id)
    }

    suspend fun getAllIngredients(): List<IngredientEntity> {
        return ingredientDao.getAllIngredients()
    }

    // -------------------------------
    // Ingredient Batches
    // -------------------------------

    suspend fun addBatch(
        ingredientId: Long,
        quantity: Double,       // grams
        price: Double,
        expirationDate: Int?,   // optional
        dateAdded: Int
    ) {
        val batch = IngredientBatchEntity(
            ingredientId = ingredientId,
            quantity = quantity,
            price = price,
            expirationDate = expirationDate,
            dateAdded = dateAdded
        )
        batchDao.insert(batch)
    }

    suspend fun updateBatch(batch: IngredientBatchEntity) {
        batchDao.update(batch)
    }

    suspend fun deleteBatch(batch: IngredientBatchEntity) {
        batchDao.delete(batch)
    }

    suspend fun getBatchesForIngredient(ingredientId: Long): List<IngredientBatchEntity> {
        return batchDao.getBatchesForIngredient(ingredientId)
    }

    suspend fun getTotalQuantity(ingredientId: Long): Double {
        return batchDao.getTotalQuantity(ingredientId) ?: 0.0
    }

    // -------------------------------
    // Unit conversion helpers
    // -------------------------------

    fun convertFromGrams(grams: Double, unit: String, density: Double?): Double {
        return com.seniorproject.cupboardly.classes.convertFromGrams(grams, unit, density)
    }

    fun convertToGrams(amount: Double, unit: String, density: Double?): Double {
        return com.seniorproject.cupboardly.classes.convertToGrams(amount, unit, density)
    }
}