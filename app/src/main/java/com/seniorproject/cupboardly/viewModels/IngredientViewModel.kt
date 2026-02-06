package com.seniorproject.cupboardly.viewModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.seniorproject.cupboardly.classes.Ingredient
import com.seniorproject.cupboardly.room.entity.IngredientEntity
import kotlinx.coroutines.launch

class IngredientViewModel(application: Application) :
    AndroidViewModel(application) {

    private val repository = Ingredient(application)

    fun addIngredient(name: String) {
        viewModelScope.launch {
            repository.addIngredient(name)
        }
    }

    suspend fun getAllIngredients(): List<IngredientEntity> {
        return repository.getAll()
    }

    fun deleteIngredient(ingredient: IngredientEntity) {
        viewModelScope.launch {
            repository.delete(ingredient)
        }
    }
}
