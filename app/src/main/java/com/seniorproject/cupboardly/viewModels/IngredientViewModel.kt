package com.seniorproject.cupboardly.viewModels

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seniorproject.cupboardly.classes.Ingredient
import com.seniorproject.cupboardly.room.entity.IngredientEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class IngredientViewModel(application: Application) : AndroidViewModel(application){

    private val ingredientWrapper = Ingredient(application)

    // StateFlow to hold ingredients
    private val _ingredients = MutableStateFlow<List<IngredientEntity>>(emptyList())
    val ingredients: StateFlow<List<IngredientEntity>> = _ingredients

    init {
        viewModelScope.launch {
            _ingredients.value = ingredientWrapper.getAll()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _ingredients.value = ingredientWrapper.getAll()
        }
    }

    fun addIngredient(
        name: String,
        quantity: Double,
        unit: String,
        density: Double,
        price: Double,
        dateEntered: Int,
        dateLastUpdated: Int
    ) {
        viewModelScope.launch {
            ingredientWrapper.addIngredient(name, quantity, unit, density, price, dateEntered, dateLastUpdated)
            _ingredients.value = ingredientWrapper.getAll()
        }
    }

    suspend fun getIngredientByName(name: String): IngredientEntity? {
        return ingredientWrapper.getIngredientByName(name)
    }

    suspend fun getIngredientById(id: Long): IngredientEntity? {
        return ingredientWrapper.getIngredientById(id)
    }

    fun updateIngredient(updatedIngredient: IngredientEntity) {
        viewModelScope.launch {
            ingredientWrapper.updateIngredient(updatedIngredient)
            _ingredients.value = ingredientWrapper.getAll()
        }
    }

    fun deleteIngredient(ingredient: IngredientEntity) {
        viewModelScope.launch {
            ingredientWrapper.delete(ingredient)
            _ingredients.value = ingredientWrapper.getAll()
        }
    }

    // function to convert from grams to unit
    fun convertFromGrams(grams: Double, unit: String, density: Double?): Double {
        return com.seniorproject.cupboardly.classes.convertFromGrams(grams, unit, density)
    }

    fun convertToGrams(amount: Double, unit: String, density: Double?): Double {
        return com.seniorproject.cupboardly.classes.convertToGrams(amount, unit, density)
    }

}

