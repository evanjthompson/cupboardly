package com.seniorproject.cupboardly.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seniorproject.cupboardly.classes.Ingredient
import com.seniorproject.cupboardly.room.entity.IngredientBatchEntity
import com.seniorproject.cupboardly.room.entity.IngredientEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ----------------------------
// UI model for ingredient + total quantity
// ----------------------------
data class IngredientWithQuantity(
    val ingredient: IngredientEntity,
    val totalQuantity: Double
)

class IngredientViewModel(application: Application) : AndroidViewModel(application) {

    private val ingredientWrapper = Ingredient(application)

    // ----------------------------
    // StateFlow to hold ingredients with total quantity
    // ----------------------------
    private val _ingredients = MutableStateFlow<List<IngredientWithQuantity>>(emptyList())
    val ingredients: StateFlow<List<IngredientWithQuantity>> = _ingredients

    // ----------------------------
    // Initialization
    // ----------------------------
    init {
        viewModelScope.launch { loadIngredients() }
    }

    fun refresh() {
        viewModelScope.launch { loadIngredients() }
    }

    private suspend fun loadIngredients() {
        val ingredientList = ingredientWrapper.getAllIngredients()
        val result = ingredientList.map { ingredient ->
            val total = ingredientWrapper.getTotalQuantity(ingredient.id)
            IngredientWithQuantity(ingredient, total)
        }
        _ingredients.value = result
    }

    // ----------------------------
    // Ingredient Definition CRUD
    // ----------------------------
    fun addIngredient(
        name: String,
        quantity: Double,
        unit: String,
        density: Double,
        price: Double,
        dateEntered: Int,
        expirationDate: Int? = null
    ) {
        viewModelScope.launch {
            // 1. Add or get ingredient definition
            val ingredientId = ingredientWrapper.addIngredientDefinition(name, density, unit)

            // 2. Add batch for inventory
            ingredientWrapper.addBatch(
                ingredientId,
                ingredientWrapper.convertToGrams(quantity, unit, density),
                price,
                expirationDate,
                dateEntered
            )

            // 3. Refresh StateFlow
            loadIngredients()
        }
    }

    fun updateIngredient(updatedIngredient: IngredientEntity) {
        viewModelScope.launch {
            ingredientWrapper.updateIngredientDefinition(updatedIngredient)
            loadIngredients()
        }
    }

    fun deleteIngredient(ingredient: IngredientEntity) {
        viewModelScope.launch {
            ingredientWrapper.deleteIngredientDefinition(ingredient)
            loadIngredients()
        }
    }

    suspend fun getIngredientByName(name: String): IngredientEntity? {
        return ingredientWrapper.getIngredientByName(name)
    }

    suspend fun getIngredientById(id: Long): IngredientEntity? {
        return ingredientWrapper.getIngredientById(id)
    }

    // ----------------------------
    // Batch CRUD
    // ----------------------------
    fun addBatch(
        ingredientId: Long,
        quantity: Double,
        price: Double,
        expirationDate: Int?,
        dateAdded: Int
    ) {
        viewModelScope.launch {
            ingredientWrapper.addBatch(
                ingredientId,
                quantity,
                price,
                expirationDate,
                dateAdded
            )
            loadIngredients()
        }
    }

    fun updateBatch(batch: IngredientBatchEntity) {
        viewModelScope.launch {
            ingredientWrapper.updateBatch(batch)
            loadIngredients()
        }
    }

    fun deleteBatch(batch: IngredientBatchEntity) {
        viewModelScope.launch {
            ingredientWrapper.deleteBatch(batch)
            loadIngredients()
        }
    }

    suspend fun getBatchesForIngredient(ingredientId: Long): List<IngredientBatchEntity> {
        return ingredientWrapper.getBatchesForIngredient(ingredientId)
    }

    suspend fun getTotalQuantity(ingredientId: Long): Double {
        return ingredientWrapper.getTotalQuantity(ingredientId)
    }

    // ----------------------------
    // Unit conversion helpers
    // ----------------------------
    fun convertFromGrams(grams: Double, unit: String, density: Double?): Double {
        return com.seniorproject.cupboardly.classes.convertFromGrams(grams, unit, density)
    }

    fun convertToGrams(amount: Double, unit: String, density: Double?): Double {
        return com.seniorproject.cupboardly.classes.convertToGrams(amount, unit, density)
    }
}