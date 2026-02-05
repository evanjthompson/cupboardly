package com.seniorproject.cupboardly.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorproject.cupboardly.room.entity.IngredientEntity
import com.seniorproject.cupboardly.viewmodel.IngredientViewModel

import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun IngredientScreen(
    viewModel: IngredientViewModel = viewModel(),
    onGoToRecipes: () -> Unit
) {
    var ingredients by remember { mutableStateOf(listOf<IngredientEntity>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        ingredients = viewModel.getAllIngredients()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Button(onClick = {
            coroutineScope.launch {
                viewModel.addIngredient("Milk")
                ingredients = viewModel.getAllIngredients()
            }
        }) {
            Text("Add Milk")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onGoToRecipes) {
            Text("Go to Recipes")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Ingredients:")

        LazyColumn {
            items(ingredients) { ingredient ->
                Text(ingredient.name)
                Button(onClick = {
                    coroutineScope.launch {
                        viewModel.addIngredient("Milk")
                        ingredients = viewModel.getAllIngredients()
                    }
                }
                    ,modifier = Modifier.fillParentMaxWidth()
                ) {
                    Text("Add Milk")
                }
            }
        }
    }
}

