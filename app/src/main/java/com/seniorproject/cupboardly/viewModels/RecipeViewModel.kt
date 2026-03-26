package com.seniorproject.cupboardly.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seniorproject.cupboardly.classes.Recipe
import com.seniorproject.cupboardly.room.entity.RecipeEntity
import com.seniorproject.cupboardly.room.entity.RecipeIngredientEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val recipeWrapper = Recipe(application)

    private val _recipes = MutableStateFlow<List<RecipeEntity>>(emptyList())
    val recipes: StateFlow<List<RecipeEntity>> = _recipes.asStateFlow()

    init {
        refresh()
    }

    // Refresh the recipe list from the database
    fun refresh() {
        viewModelScope.launch {
            _recipes.value = recipeWrapper.getAll()
        }
    }

    // Delete a recipe (single)
    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            recipeWrapper.delete(recipe)
            refresh()
        }
    }

    // Delete a recipe AND all its ingredients in the join table
    fun deleteRecipeWithIngredients(recipeId: Long) {
        viewModelScope.launch {
            // Delete all recipe_ingredient_table entries first
            recipeWrapper.deleteIngredientsForRecipe(recipeId)
            // Then delete the recipe itself
            recipeWrapper.deleteById(recipeId)
            // Refresh the recipes list
            refresh()
        }
    }

    // Get all ingredients for a specific recipe
    suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity> {
        return recipeWrapper.getIngredientsForRecipe(recipeId)
    }

    // Add ingredient to recipe
    suspend fun addIngredientToRecipe(
        recipeId: Long,
        ingredientId: Long,
        quantityUsed: Double,
        unitUsed: String
    ) {
        recipeWrapper.addIngredientToRecipe(recipeId, ingredientId, quantityUsed, unitUsed)
    }

    // Add recipe and return its ID
    suspend fun addRecipeAndReturnId(
        name: String,
        instructions: String,
        dateCreated: Int
    ): Long {
        return recipeWrapper.addRecipe(name, instructions, dateCreated)
    }
}