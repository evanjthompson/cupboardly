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
import androidx.compose.material3.ButtonDefaults
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
import com.seniorproject.cupboardly.classes.askGeminiForDensity

@OptIn(ExperimentalMaterial3Api::class)
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

    // NEW
    var showStartDialog by remember { mutableStateOf(false) }
    var activeRecipe by remember { mutableStateOf<Long?>(null) }
    var startError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(recipes) {
        recipes.forEach { recipe ->
            if (!recipeIngredientsMap.containsKey(recipe.id)) {
                val links = viewModel.getIngredientsForRecipe(recipe.id)
                val ingredientsWithQuantities = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                    ingredient?.let {
                        // convert from grams back to the unit it was entered in
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

    Box(modifier = Modifier
        .fillMaxSize()
        .safeDrawingPadding()) {

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
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gold,
                        contentColor = Color.White
                    )
                ) { AutoSizeText(
                    text = "Ingredients",
                    maxFontSize = 20.sp,
                    minFontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                ) }

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
                                    showStartDialog = true
                                }) {
                                    Text("Start")
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
            colors = ButtonDefaults.buttonColors(containerColor = darkBlue),
            contentPadding = PaddingValues(0.dp) // remove default padding
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("+", fontSize = 32.sp, color = Color.White)
            }
        }

        // ---------------- ADD RECIPE DIALOG ----------------

        if (showAddDialog) {

            var name by remember { mutableStateOf("") }
            var instructions by remember { mutableStateOf("") }
            var newIngredientName by remember { mutableStateOf("") }
            var nameError by remember { mutableStateOf(false) }
            var ingredientError by remember { mutableStateOf<String?>(null) }
            val selectedIngredients = remember { mutableStateMapOf<Long, String>() }
            val selectedUnits = remember { mutableStateMapOf<Long, String>() }

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
                                    val existing = ingredientViewModel.getIngredientByName(trimmedName)
                                    if (existing != null) {
                                        ingredientError = "Ingredient already exists"
                                        return@launch
                                    }

                                    ingredientError = null
                                    val currentDate = (System.currentTimeMillis() / 1000).toInt()
                                    val densityValue = askGeminiForDensity(trimmedName)

                                    ingredientViewModel.addIngredient(
                                        name = trimmedName,
                                        quantity = 0.0,
                                        unit = "",
                                        density = densityValue,
                                        price = 0.0,
                                        dateEntered = currentDate,
                                        dateLastUpdated = currentDate
                                    )

                                    val created =
                                        ingredientViewModel.getIngredientByName(trimmedName)

                                    created?.let {
                                        selectedIngredients[it.id] = "1.0"
                                    }

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

                            val unitOptions = listOf("unit", "g", "kg", "oz", "lb", "ml", "cup", "tbsp", "tsp", "floz")

                            items(allIngredients, key = { it.id }) { ingredient ->

                                val isSelected = selectedIngredients.containsKey(ingredient.id)
                                var unitDropdownExpanded by remember { mutableStateOf(false) }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                selectedIngredients[ingredient.id] = "1.0"
                                                selectedUnits[ingredient.id] = ingredient.unit.ifBlank { "g" }
                                            } else {
                                                selectedIngredients.remove(ingredient.id)
                                                selectedUnits.remove(ingredient.id)
                                            }
                                        }
                                    )

                                    Text(ingredient.name, modifier = Modifier.weight(1f))

                                    if (isSelected) {
                                        OutlinedTextField(
                                            value = selectedIngredients[ingredient.id] ?: "",
                                            onValueChange = { selectedIngredients[ingredient.id] = it },
                                            label = { Text("Qty") },
                                            modifier = Modifier.width(70.dp),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.width(4.dp))

                                        ExposedDropdownMenuBox(
                                            expanded = unitDropdownExpanded,
                                            onExpandedChange = { unitDropdownExpanded = it },
                                            modifier = Modifier.width(100.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = selectedUnits[ingredient.id] ?: ingredient.unit.ifBlank { "g" },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Unit") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitDropdownExpanded) },
                                                modifier = Modifier
                                                    .menuAnchor(
                                                        type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                                        enabled = true
                                                    )
                                                    .fillMaxWidth(),
                                                singleLine = true
                                            )

                                            ExposedDropdownMenu(
                                                expanded = unitDropdownExpanded,
                                                onDismissRequest = { unitDropdownExpanded = false }
                                            ) {
                                                unitOptions.forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option) },
                                                        onClick = {
                                                            selectedUnits[ingredient.id] = option
                                                            unitDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
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
                            val recipeId = viewModel.addRecipeAndReturnId(name.trim(), instructions.trim(), currentDate)

                            selectedIngredients.forEach { (id, qtyString) ->
                                val qty = qtyString.toDoubleOrNull() ?: 0.0

                                if (qty > 0) {
                                    val ingredient = allIngredients.find { it.id == id }
                                    val selectedUnit = selectedUnits[id] ?: ingredient?.unit ?: "g"
                                    val gramsToStore = ingredientViewModel.convertToGrams(qty, selectedUnit, ingredient?.density)
                                    viewModel.addIngredientToRecipe(recipeId, id, gramsToStore, selectedUnit)
                                }
                            }

                            viewModel.refresh()
                            showAddDialog = false
                        }
                    }) { Text("Save") }
                },

                dismissButton = {
                    Button(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ---------------- START DIALOG ----------------

        if (showStartDialog && activeRecipe != null) {

            val recipe = recipes.find { it.id == activeRecipe }

            AlertDialog(
                onDismissRequest = { showStartDialog = false },
                title = { Text("Start Recipe") },

                text = {
                    Column {

                        Text("Instructions:", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(recipe?.instructions ?: "")

                        Spacer(modifier = Modifier.height(12.dp))

                        startError?.let {
                            Text(it, color = Color.Red)
                        }
                    }
                },

                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {

                            val links =
                                viewModel.getIngredientsForRecipe(activeRecipe!!)

                            val insufficient = mutableListOf<String>()

                            links.forEach { link ->
                                val ingredient =
                                    ingredientViewModel.getIngredientById(link.ingredientId)

                                if (ingredient != null && ingredient.quantity < link.quantityUsed) {
                                    insufficient.add("${ingredient.name} (need ${link.quantityUsed}, have ${ingredient.quantity})")
                                }
                            }

                            if (insufficient.isNotEmpty()) {
                                startError =
                                    "Not enough: ${insufficient.joinToString(", ")}"
                                return@launch
                            }

                            links.forEach { link ->
                                val ingredient =
                                    ingredientViewModel.getIngredientById(link.ingredientId)

                                ingredient?.let {
                                    val updated = it.copy(
                                        quantity = it.quantity - link.quantityUsed,
                                        dateLastUpdated =
                                            (System.currentTimeMillis() / 1000).toInt()
                                    )
                                    ingredientViewModel.updateIngredient(updated)
                                }
                            }

                            showStartDialog = false
                        }
                    }) {
                        Text("Confirm & Start")
                    }
                },

                dismissButton = {
                    Button(onClick = { showStartDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}