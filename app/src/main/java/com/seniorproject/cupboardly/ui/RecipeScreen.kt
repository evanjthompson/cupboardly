package com.seniorproject.cupboardly.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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

    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    val recipeIngredientsMap = remember { mutableStateMapOf<Long, String>() }

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(recipes) {
        recipes.forEach { recipe ->
            if (!recipeIngredientsMap.containsKey(recipe.id)) {
                val links = viewModel.getIngredientsForRecipe(recipe.id)
                val ingredientsWithQuantities = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                    ingredient?.let { "${link.quantityUsed} ${it.name}" }
                }
                recipeIngredientsMap[recipe.id] =
                    ingredientsWithQuantities.joinToString(", ")
            }
        }
    }

    val gold = Color(197, 145, 39)
    val darkBlue = Color(11, 186, 224)

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
                ) {
                    Text("Ingredients")
                }

                Button(
                    onClick = {},
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = darkBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        "Recipes",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(recipes, key = { it.id }) { recipe ->

                    var expanded by remember { mutableStateOf(false) }
                    val ingredientText =
                        recipeIngredientsMap[recipe.id] ?: "Loading..."

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
                                Text("Date Created: ${
                                    sdf.format(
                                        Date(recipe.dateCreated * 1000L)
                                    )
                                }")
                                Text("Times Followed: ${recipe.numTimesFollowed}")

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {

                                    Button(
                                        onClick = { },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = darkBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Follow")
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.deleteRecipe(recipe)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = darkBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Edit")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(35.dp)
                .size(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = gold,
                contentColor = Color.White
            )
        ) {
            Text("+", fontSize = 32.sp)
        }

        if (showAddDialog) {

            var name by remember { mutableStateOf("") }
            var instructions by remember { mutableStateOf("") }
            var nameError by remember { mutableStateOf(false) }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },

                title = { Text("Add New Recipe") },

                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = false
                            },
                            label = { Text("Recipe Name") },
                            isError = nameError,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (nameError) {
                            Text(
                                "Recipe name is required",
                                color = Color.Red,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("Instructions") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 5
                        )
                    }
                },

                confirmButton = {
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = true
                                return@Button
                            }

                            coroutineScope.launch {
                                val currentDate =
                                    (System.currentTimeMillis() / 1000).toInt()

                                viewModel.addRecipeAndReturnId(
                                    name = name.trim(),
                                    instructions = instructions.trim(),
                                    dateCreated = currentDate
                                )

                                viewModel.refresh()
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Save")
                    }
                },

                dismissButton = {
                    Button(
                        onClick = { showAddDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}