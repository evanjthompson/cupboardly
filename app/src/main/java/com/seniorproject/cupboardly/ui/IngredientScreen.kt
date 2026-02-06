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
import com.seniorproject.cupboardly.viewModels.IngredientViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.unit.sp

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

    Box(modifier = Modifier.fillMaxSize()) {  // box allows floating button (+ button added below)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Top row: Ingredients label + Go to Recipes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {},   // no action
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Ingredients")
                }

                Button(
                    onClick = onGoToRecipes,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Recipes")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable list of ingredients
            LazyColumn {
                items(ingredients) { ingredient ->
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.deleteIngredient(ingredient)
                                ingredients = viewModel.getAllIngredients()
                            }
                        },
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(ingredient.name)
                    }
                }
            }
        }

        // '+' button in bottom corner
        Button(

            onClick = {
                coroutineScope.launch {
                    viewModel.addIngredient("Milk")
                    ingredients = viewModel.getAllIngredients()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(35.dp)
                .size(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Yellow,
                contentColor = Color.Black
            )
        ) {
            Text("+",
                fontSize = 32.sp)
        }
    }
}
