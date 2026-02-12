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
import com.seniorproject.cupboardly.viewModels.IngredientViewModel
import com.seniorproject.cupboardly.viewModels.RecipeViewModel
import kotlinx.coroutines.launch

@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel = viewModel(),
    ingredientViewModel: IngredientViewModel = viewModel(),
    onGoToIngredients: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // State map to hold recipeId -> ingredient names string
    val recipeIngredientsMap = remember { mutableStateMapOf<Long, String>() }

    // Fetch ingredients for all recipes whenever the recipe list changes
    // Fetch ingredients for all recipes whenever the recipe list changes
    LaunchedEffect(recipes) {
        recipes.forEach { recipe ->
            // Skip if already fetched
            if (!recipeIngredientsMap.containsKey(recipe.id)) {
                val links = viewModel.getIngredientsForRecipe(recipe.id) // suspend
                // Get ingredient names with quantities
                val ingredientsWithQuantities = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId) // suspend
                    ingredient?.let {
                        "${link.quantityUsed} ${it.name}"
                    }
                }
                recipeIngredientsMap[recipe.id] = ingredientsWithQuantities.joinToString(", ")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

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
                    onClick = onGoToIngredients,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Blue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Ingredients")
                }

                Button(
                    onClick = {},   // no action
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
                items(recipes) { recipe ->

                    Button(
                        onClick = { viewModel.deleteRecipe(recipe) },
                        modifier = Modifier
                            .fillParentMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green,
                            contentColor = Color.Black
                        )
                    ) {
                        val ingredientText = recipeIngredientsMap[recipe.id] ?: "Loading..."
                        Text("${recipe.name} - $ingredientText")
                    }
                }
            }
        }

        // + Button to add recipe + ingredient
        Button(
            onClick = {
                coroutineScope.launch {
                    val currentDate = (System.currentTimeMillis() / 1000).toInt()

                    // 1. Add the recipe and get its ID
                    val recipeId: Long = viewModel.addRecipeAndReturnId(
                        name = "Cupcakes",
                        instructions = "Mix ingredients and bake at 350°F for 20 minutes.",
                        dateCreated = currentDate
                    )

                    // 2. Look up the ingredient
                    val ingredient = ingredientViewModel.getIngredientByName("Milk")

                    if (ingredient != null) {
                        // 3. Add ingredient to recipe
                        viewModel.addIngredientToRecipe(
                            recipeId = recipeId,
                            ingredientId = ingredient.id,
                            quantityUsed = 2.0
                        )
                    }

                    // 4. Refresh the recipe list
                    viewModel.refresh()
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
            Text("+", fontSize = 32.sp)
        }
    }
}

