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
fun IngredientScreen(viewModel: IngredientViewModel = viewModel()) {
    var ingredients by remember { mutableStateOf(listOf<IngredientEntity>()) }
    val coroutineScope = rememberCoroutineScope() // Composable coroutine scope

    // Load ingredients when Composable first appears
    LaunchedEffect(Unit) {
        ingredients = viewModel.getAllIngredients()
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Button(onClick = {
            coroutineScope.launch {
                viewModel.addIngredient("Milk")
                ingredients = viewModel.getAllIngredients()
            }
        }) {
            Text("Add Milk")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Ingredients:")

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(ingredients) { ingredient ->
                Text(text = ingredient.name)
            }
        }
    }
}
