package com.seniorproject.cupboardly.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seniorproject.cupboardly.classes.Recipe
import com.seniorproject.cupboardly.room.entity.RecipeEntity
import kotlinx.coroutines.launch

class RecipeViewModel(application: Application) :
    AndroidViewModel(application) {

    private val repository = Recipe(application)

    fun addRecipe(name: String) {
        viewModelScope.launch {
            repository.addRecipe(name)
        }
    }

    suspend fun getAllRecipes(): List<RecipeEntity> {
        return repository.getAll()
    }

    fun deleteRecipe(recipe: RecipeEntity) {
        viewModelScope.launch {
            repository.delete(recipe)
        }
    }
}
