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

    fun refresh() {
        viewModelScope.launch {
            _recipes.value = recipeWrapper.getAll()
        }
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            recipeWrapper.delete(recipe)
            refresh()
        }
    }

    // ✅ NEW: REQUIRED FOR YOUR UI
    fun deleteRecipeWithIngredients(recipeId: Long) {
        viewModelScope.launch {

            // 🔥 delete join table entries FIRST
            recipeWrapper.deleteIngredientsForRecipe(recipeId)

            // 🔥 then delete the recipe itself
            recipeWrapper.deleteById(recipeId)

            refresh()
        }
    }

    suspend fun getIngredientsForRecipe(recipeId: Long): List<RecipeIngredientEntity> {
        return recipeWrapper.getIngredientsForRecipe(recipeId)
    }

    suspend fun addIngredientToRecipe(
        recipeId: Long,
        ingredientId: Long,
        quantityUsed: Double,
        unitUsed: String
    ) {
        recipeWrapper.addIngredientToRecipe(recipeId, ingredientId, quantityUsed, unitUsed)
    }

    suspend fun addRecipeAndReturnId(
        name: String,
        instructions: String,
        dateCreated: Int
    ): Long {
        return recipeWrapper.addRecipe(name, instructions, dateCreated)
    }
}