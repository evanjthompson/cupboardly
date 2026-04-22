package com.seniorproject.cupboardly.repos

import android.content.Context
import com.seniorproject.cupboardly.room.database.AppDatabase
import com.seniorproject.cupboardly.room.entity.IngredientBatchEntity
import com.seniorproject.cupboardly.room.entity.IngredientEntity

class IngredientRepo(context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val ingredientDao = db.ingredientDao()
    private val batchDao = db.ingredientBatchDao()
    private val recipeIngredientDao = db.recipeIngredientDao()

    // Format ingredient name
    private fun formatIngredientName(name: String): String {
        return name
            .trim()
            .lowercase()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    // -------------------------------
    // Ingredient Definition
    // -------------------------------

    suspend fun addIngredientDefinition(
        name: String,
        density: Double,
        unit: String
    ): Long {

        val formattedName = formatIngredientName(name)

        val existing = ingredientDao.getIngredientByName(formattedName)
        if (existing != null) return existing.id

        val ingredient = IngredientEntity(
            name = formattedName,
            density = density,
            unit = unit
        )

        return ingredientDao.insert(ingredient)
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
    // Recipe relationship checks
    // -------------------------------

    suspend fun isUsedByRecipe(ingredientId: Long): Boolean {
        return recipeIngredientDao.getRecipesForIngredient(ingredientId).isNotEmpty()
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

    suspend fun resetBatches(ingredientId: Long) {
        val batches = batchDao.getBatchesForIngredient(ingredientId)
        batches.forEach { batch ->
            batchDao.delete(batch)
        }
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