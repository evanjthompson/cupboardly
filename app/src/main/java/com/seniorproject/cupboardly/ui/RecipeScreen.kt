package com.seniorproject.cupboardly.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorproject.cupboardly.R
import com.seniorproject.cupboardly.room.entity.RecipeEntity
import com.seniorproject.cupboardly.viewModels.IngredientViewModel
import com.seniorproject.cupboardly.viewModels.RecipeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecipeScreen(
    viewModel: RecipeViewModel = viewModel(),
    ingredientViewModel: IngredientViewModel = viewModel(),
    onGoToIngredients: () -> Unit
) {
    val recipes by viewModel.recipes.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // For formatting the dates
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    // State map to hold recipeId -> ingredient names string
    val recipeIngredientsMap = remember { mutableStateMapOf<Long, String>() }

    // Fetch ingredients for all recipes whenever the recipe list changes
    LaunchedEffect(recipes) {
        recipes.forEach { recipe ->
            if (!recipeIngredientsMap.containsKey(recipe.id)) {
                val links = viewModel.getIngredientsForRecipe(recipe.id) // suspend
                val ingredientsWithQuantities = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId) // suspend
                    ingredient?.let { "${link.quantityUsed} ${it.name}" }
                }
                recipeIngredientsMap[recipe.id] = ingredientsWithQuantities.joinToString(", ")
            }
        }
    }

    // Custom colors
    val gold = Color(red = 197, green = 145, blue = 39)
    val darkBlue = Color(red = 11, green = 186, blue = 224)
    val headerPink2 = Color(red = 210, green = 106, blue = 131)
    val headerBlue1 = Color(red = 140, green = 198, blue = 209)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.striperecipebg),
            contentDescription = "Recipe Background",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {

            // Top navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = onGoToIngredients,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray,
                        contentColor = Color.White
                    )
                ) { Text("Ingredients") }

                Button(
                    onClick = {},
                    modifier = Modifier.weight(2f),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = darkBlue,
                        contentColor = Color.White
                    )
                ) { Text("Recipes",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold) }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(recipes, key = { it.id }) { recipe ->

                    var expanded by remember { mutableStateOf(false) }
                    val ingredientText = recipeIngredientsMap[recipe.id] ?: "Loading..."

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(2.dp, darkBlue),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .animateContentSize()
                            .clickable { expanded = !expanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            // Always visible: recipe name
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(recipe.name)
                            }

                            if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Ingredients: $ingredientText")
                                Text("Instructions: ${recipe.instructions}")
                                Text("Date Created: ${sdf.format(Date(recipe.dateCreated * 1000L))}")
                                Text("Times Followed: ${recipe.numTimesFollowed}")

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {  },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = darkBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Follow") }

                                    Button(
                                        onClick = { viewModel.deleteRecipe(recipe) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = darkBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Edit") }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating + button to add a recipe
        Button(
            onClick = {
                coroutineScope.launch {
                    val currentDate = (System.currentTimeMillis() / 1000).toInt()
                    val recipeId: Long = viewModel.addRecipeAndReturnId(
                        name = "Cupcakes",
                        instructions = "Mix ingredients and bake at 350°F for 20 minutes.",
                        dateCreated = currentDate
                    )
                    viewModel.refresh()
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(35.dp)
                .size(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = gold,
                contentColor = Color.White
            )
        ) { Text("+", fontSize = 32.sp) }
    }
}

