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

@OptIn(ExperimentalMaterial3Api::class)
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

    var showStartDialog by remember { mutableStateOf(false) }
    var activeRecipe by remember { mutableStateOf<Long?>(null) }
    var startError by remember { mutableStateOf<String?>(null) }

    var showOverrideConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recipeToDelete by remember { mutableStateOf<Long?>(null) }

    // ✅ FIX: state to hold ingredient info for the start dialog
    var startDialogIngredientInfo by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(recipes) {
        recipes.forEach { recipe ->
            if (!recipeIngredientsMap.containsKey(recipe.id)) {
                val links = viewModel.getIngredientsForRecipe(recipe.id)
                val ingredientsWithQuantities = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                    ingredient?.let {
                        val displayQty = ingredientViewModel.convertFromGrams(
                            link.quantityUsed,
                            link.unitUsed,
                            it.density
                        )
                        "${"%.2f".format(displayQty)} ${link.unitUsed} ${it.name}"
                    }
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

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(onClick = {
                                    activeRecipe = recipe.id
                                    startError = null
                                    startDialogIngredientInfo = emptyList()
                                    showStartDialog = true
                                }) {
                                    Text("Start")
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = {
                                        recipeToDelete = recipe.id
                                        showDeleteDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = { },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(35.dp)
                .size(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = gold)
        ) { Text("+", fontSize = 32.sp) }


        // START RECIPE DIALOG
        if (showStartDialog && activeRecipe != null) {

            val recipe = recipes.find { it.id == activeRecipe }

            // ✅ FIX: load ingredient info in a coroutine, not directly in composable
            LaunchedEffect(activeRecipe) {
                val links = viewModel.getIngredientsForRecipe(activeRecipe!!)
                startDialogIngredientInfo = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                    ingredient?.let {
                        val usedDisplay = ingredientViewModel.convertFromGrams(
                            link.quantityUsed, link.unitUsed, it.density
                        )
                        val afterDisplay = ingredientViewModel.convertFromGrams(
                            it.quantity - link.quantityUsed, link.unitUsed, it.density
                        )
                        "${it.name}: Use ${"%.2f".format(usedDisplay)} ${link.unitUsed}, " +
                                "Remaining ${"%.2f".format(afterDisplay)} ${link.unitUsed}"
                    }
                }
            }

            AlertDialog(
                onDismissRequest = { showStartDialog = false },
                title = { Text("Start Recipe") },

                text = {
                    Column {

                        Text("Instructions:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(recipe?.instructions ?: "")

                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Ingredients to be used:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))

                        // ✅ FIX: use state list instead of direct suspend calls
                        if (startDialogIngredientInfo.isEmpty()) {
                            Text("Loading...")
                        } else {
                            startDialogIngredientInfo.forEach { info ->
                                Text(info)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        startError?.let {
                            Text(it, color = Color.Red)
                        }
                    }
                },

                confirmButton = {
                    Column {

                        Button(onClick = {
                            coroutineScope.launch {
                                val links = viewModel.getIngredientsForRecipe(activeRecipe!!)

                                val insufficient = mutableListOf<String>()

                                links.forEach { link ->
                                    val ingredient =
                                        ingredientViewModel.getIngredientById(link.ingredientId)

                                    if (ingredient != null && ingredient.quantity < link.quantityUsed) {
                                        val neededDisplay = ingredientViewModel.convertFromGrams(
                                            link.quantityUsed, link.unitUsed, ingredient.density
                                        )
                                        val haveDisplay = ingredientViewModel.convertFromGrams(
                                            ingredient.quantity, link.unitUsed, ingredient.density
                                        )
                                        insufficient.add(
                                            "${ingredient.name} (need ${"%.2f".format(neededDisplay)} ${link.unitUsed}, " +
                                                    "have ${"%.2f".format(haveDisplay)} ${link.unitUsed})"
                                        )
                                    }
                                }

                                if (insufficient.isNotEmpty()) {
                                    startError = "Not enough: ${insufficient.joinToString(", ")}"
                                    return@launch
                                }

                                links.forEach { link ->
                                    val ingredient =
                                        ingredientViewModel.getIngredientById(link.ingredientId)
                                    ingredient?.let {
                                        val updated = it.copy(
                                            quantity = it.quantity - link.quantityUsed,
                                            dateLastUpdated = (System.currentTimeMillis() / 1000).toInt()
                                        )
                                        ingredientViewModel.updateIngredient(updated)
                                    }
                                }

                                showStartDialog = false
                            }
                        }) {
                            Text("Confirm & Start")
                        }

                        if (!startError.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { showOverrideConfirmDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                            ) {
                                Text("Make Anyway")
                            }
                        }
                    }
                },

                dismissButton = {
                    Button(onClick = { showStartDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }


        if (showOverrideConfirmDialog && activeRecipe != null) {

            AlertDialog(
                onDismissRequest = { showOverrideConfirmDialog = false },

                title = { Text("Proceed Anyway?") },

                text = {
                    Text(
                        "Some ingredients are insufficient.\n\n" +
                                "Only ingredients with enough stock will be deducted.\n" +
                                "Others will be skipped.\n\nContinue?"
                    )
                },

                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {

                            val links = viewModel.getIngredientsForRecipe(activeRecipe!!)

                            links.forEach { link ->
                                val ingredient =
                                    ingredientViewModel.getIngredientById(link.ingredientId)

                                ingredient?.let {
                                    if (it.quantity >= link.quantityUsed) {
                                        val updated = it.copy(
                                            quantity = it.quantity - link.quantityUsed,
                                            dateLastUpdated = (System.currentTimeMillis() / 1000).toInt()
                                        )
                                        ingredientViewModel.updateIngredient(updated)
                                    }
                                }
                            }

                            showOverrideConfirmDialog = false
                            showStartDialog = false
                            startError = null
                        }
                    }) {
                        Text("Yes, Continue")
                    }
                },

                dismissButton = {
                    Button(onClick = {
                        showOverrideConfirmDialog = false
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }


        if (showDeleteDialog && recipeToDelete != null) {

            val recipeName = recipes.find { it.id == recipeToDelete }?.name ?: ""

            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },

                title = { Text("Delete Recipe") },

                text = {
                    Text("Delete \"$recipeName\"? This cannot be undone.")
                },

                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.deleteRecipeWithIngredients(recipeToDelete!!)
                                showDeleteDialog = false
                                recipeToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete")
                    }
                },

                dismissButton = {
                    Button(onClick = {
                        showDeleteDialog = false
                        recipeToDelete = null
                    }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}