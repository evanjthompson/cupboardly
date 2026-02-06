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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorproject.cupboardly.room.entity.RecipeEntity
import com.seniorproject.cupboardly.viewModels.RecipeViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel = viewModel(),
    onGoToIngredients: () -> Unit
) {
    var recipes by remember { mutableStateOf(listOf<RecipeEntity>()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        recipes = viewModel.getAllRecipes()
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Top row: Ingredients button (navigates) + Recipes button (no action)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onGoToIngredients,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Ingredients")
                }

                Button(
                    onClick = {},   // no action
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Gray,
                        contentColor = Color.White
                    )
                ) {
                    Text("Recipes")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable list of recipes
            LazyColumn {
                items(recipes) { recipe ->
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.deleteRecipe(recipe)
                                recipes = viewModel.getAllRecipes()
                            }
                        },
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(recipe.name)
                    }
                }
            }
        }

        // '+' button in bottom corner to add "Cupcakes"
        Button(
            onClick = {
                coroutineScope.launch {
                    viewModel.addRecipe("Cupcakes")
                    recipes = viewModel.getAllRecipes()
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
            Text(
                "+",
                fontSize = 32.sp
            )
        }
    }
}
