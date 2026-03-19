package com.seniorproject.cupboardly.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val allIngredients by ingredientViewModel.ingredients.collectAsState(initial = emptyList())
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
                recipeIngredientsMap[recipe.id] = ingredientsWithQuantities.joinToString(", ")
            }
        }
    }

    val gold = Color(197, 145, 39)
    val darkBlue = Color(11, 186, 224)

    Box(modifier = Modifier.fillMaxSize()) {

        Image(
            painter = painterResource(id = R.drawable.striperecipebg),
            contentDescription = null,
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
                    modifier = Modifier.weight(1f)
                ) { Text("Ingredients") }

                Button(
                    onClick = {},
                    modifier = Modifier.weight(2f),
                    colors = ButtonDefaults.buttonColors(containerColor = darkBlue)
                ) {
                    Text("Recipes", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn {
                items(recipes, key = { it.id }) { recipe ->

                    var expanded by remember(recipe.id) { mutableStateOf(false) }
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

                            Text(recipe.name)

                            if (expanded) {

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Ingredients: $ingredientText")
                                Text("Instructions: ${recipe.instructions}")
                                Text("Date Created: ${sdf.format(Date(recipe.dateCreated * 1000L))}")
                                Text("Times Followed: ${recipe.numTimesFollowed}")
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
            colors = ButtonDefaults.buttonColors(containerColor = gold)
        ) { Text("+", fontSize = 32.sp) }

        if (showAddDialog) {

            var name by remember { mutableStateOf("") }
            var instructions by remember { mutableStateOf("") }
            var newIngredientName by remember { mutableStateOf("") }
            var nameError by remember { mutableStateOf(false) }
            var ingredientError by remember { mutableStateOf<String?>(null) } // NEW
            val selectedIngredients = remember { mutableStateMapOf<Long, String>() }

            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add New Recipe") },

                text = {
                    val scrollState = rememberScrollState()

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                nameError = false
                            },
                            label = { Text("Recipe Name") },
                            isError = nameError
                        )

                        OutlinedTextField(
                            value = instructions,
                            onValueChange = { instructions = it },
                            label = { Text("Instructions") }
                        )

                        Divider()

                        Text("Add New Ingredient", fontWeight = FontWeight.Bold)

                        Row {
                            OutlinedTextField(
                                value = newIngredientName,
                                onValueChange = {
                                    newIngredientName = it
                                    ingredientError = null
                                },
                                label = { Text("Ingredient Name") },
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(onClick = {
                                val trimmedName = newIngredientName.trim()
                                if (trimmedName.isBlank()) return@Button

                                coroutineScope.launch {
                                    // Check if ingredient already exists
                                    val existing = ingredientViewModel.getIngredientByName(trimmedName)
                                    if (existing != null) {
                                        ingredientError = "Ingredient already exists"
                                        return@launch
                                    }

                                    ingredientError = null
                                    val currentDate = (System.currentTimeMillis() / 1000).toInt()

                                    // Add ingredient with quantity 0
                                    ingredientViewModel.addIngredient(
                                        name = trimmedName,
                                        quantity = 0.0,
                                        unit = "",
                                        price = 0.0,
                                        pricePerUnit = 0.0,
                                        dateEntered = currentDate,
                                        dateLastUpdated = currentDate
                                    )

                                    val created = ingredientViewModel.getIngredientByName(trimmedName)
                                    created?.let { selectedIngredients[it.id] = "1.0" }

                                    newIngredientName = ""
                                }
                            }) {
                                Text("Add")
                            }
                        }

                        ingredientError?.let {
                            Text(it, color = Color.Red, fontSize = 14.sp)
                        }

                        Divider()

                        Text("Select Ingredients", fontWeight = FontWeight.Bold)

                        LazyColumn(
                            modifier = Modifier
                                .height(250.dp)
                                .fillMaxWidth()
                        ) {
                            items(allIngredients, key = { it.id }) { ingredient ->

                                val isSelected = selectedIngredients.containsKey(ingredient.id)

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selectedIngredients[ingredient.id] = "1.0"
                                            else selectedIngredients.remove(ingredient.id)
                                        }
                                    )

                                    Text(ingredient.name, modifier = Modifier.weight(1f))

                                    if (isSelected) {
                                        OutlinedTextField(
                                            value = selectedIngredients[ingredient.id] ?: "",
                                            onValueChange = {
                                                selectedIngredients[ingredient.id] = it
                                            },
                                            label = { Text("Qty") },
                                            modifier = Modifier.width(80.dp),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }
                },

                confirmButton = {
                    Button(onClick = {
                        if (name.isBlank()) {
                            nameError = true
                            return@Button
                        }

                        coroutineScope.launch {
                            val currentDate = (System.currentTimeMillis() / 1000).toInt()

                            val recipeId = viewModel.addRecipeAndReturnId(
                                name.trim(),
                                instructions.trim(),
                                currentDate
                            )

                            // Save ingredients
                            selectedIngredients.forEach { (id, qtyString) ->
                                val qty = qtyString.toDoubleOrNull() ?: 0.0
                                if (qty > 0) {
                                    var ingredient = ingredientViewModel.getIngredientById(id)
                                    if (ingredient == null) {
                                        ingredientViewModel.addIngredient(
                                            name = allIngredients.find { it.id == id }?.name ?: "Unknown",
                                            quantity = 0.0,
                                            unit = "",
                                            price = 0.0,
                                            pricePerUnit = 0.0,
                                            dateEntered = currentDate,
                                            dateLastUpdated = currentDate
                                        )
                                        ingredient = ingredientViewModel.getIngredientById(id)
                                    }
                                    ingredient?.let { viewModel.addIngredientToRecipe(recipeId, it.id, qty) }
                                }
                            }

                            viewModel.refresh()
                            showAddDialog = false
                        }
                    }) { Text("Save") }
                },

                dismissButton = {
                    Button(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}