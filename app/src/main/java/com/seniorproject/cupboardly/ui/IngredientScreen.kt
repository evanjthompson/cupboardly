package com.seniorproject.cupboardly.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorproject.cupboardly.room.entity.IngredientEntity
import com.seniorproject.cupboardly.viewModels.IngredientViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

@Composable
fun IngredientScreen(
    viewModel: IngredientViewModel = viewModel(),
    onGoToRecipes: () -> Unit
) {

    // Collect the ingredients StateFlow as Compose state
    val ingredients by viewModel.ingredients.collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {  // box allows floating '+' button

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
                        containerColor = Color.Blue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Ingredients")
                }

                Button(
                    onClick = onGoToRecipes,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Recipes")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable list of ingredients
            LazyColumn {
                items(ingredients) { ingredient: IngredientEntity ->  // explicit type
                    Button(
                        onClick = { viewModel.deleteIngredient(ingredient) },
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Blue,
                            contentColor = Color.White
                        )
                    ) {
                        Text(ingredient.name + " " + ingredient.quantity +  " " + ingredient.unit)
                    }
                }
            }
        }

        // '+' button in bottom corner
        Button(
            onClick = {
                val currentDate = (System.currentTimeMillis() / 1000).toInt()
                viewModel.addIngredient(
                    name = "Milk",
                    quantity = 2.0,
                    unit = "Gallon(s)",
                    price = 10.0,
                    pricePerUnit = 5.0,
                    dateEntered = currentDate,
                    dateLastUpdated = currentDate
                )
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
            Text("+", fontSize = 32.sp)
        }
    }
}
