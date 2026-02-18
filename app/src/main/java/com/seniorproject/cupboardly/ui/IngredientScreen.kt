package com.seniorproject.cupboardly.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorproject.cupboardly.room.entity.IngredientEntity
import com.seniorproject.cupboardly.viewModels.IngredientViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.seniorproject.cupboardly.R

@Composable
fun IngredientScreen(
    viewModel: IngredientViewModel = viewModel(),
    onGoToRecipes: () -> Unit
) {

    val ingredients by viewModel.ingredients.collectAsState(initial = emptyList())

    // Dialog state
    var showDialog by remember { mutableStateOf(false) }

    // Input states
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.stripeingredientBG ),
            contentDescription = "Ingredient Background",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
            )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {},
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

            LazyColumn {
                items(ingredients) { ingredient: IngredientEntity ->
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
                        Text("${ingredient.name} ${ingredient.quantity} ${ingredient.unit}")
                    }
                }
            }
        }

        // + Button
        Button(
            onClick = { showDialog = true },
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

        // Input Dialog
        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    Button(onClick = {
                        val currentDate = (System.currentTimeMillis() / 1000).toInt()

                        viewModel.addIngredient(
                            name = name,
                            quantity = quantity.toDoubleOrNull() ?: 0.0,
                            unit = unit,
                            price = price.toDoubleOrNull() ?: 0.0,
                            pricePerUnit = if (quantity.toDoubleOrNull() != null && quantity.toDouble() != 0.0)
                                price.toDoubleOrNull()!! / quantity.toDouble()
                            else 0.0,
                            dateEntered = currentDate,
                            dateLastUpdated = currentDate
                        )

                        // Clear fields
                        name = ""
                        quantity = ""
                        unit = ""
                        price = ""
                        showDialog = false
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Add Ingredient") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Ingredient Name") }
                        )
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity") }
                        )
                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit of Measurement") }
                        )
                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price") }
                        )
                    }
                }
            )
        }
    }
}

