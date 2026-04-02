@file:OptIn(ExperimentalMaterial3Api::class)
package com.seniorproject.cupboardly.ui

import android.content.Context
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
import com.seniorproject.cupboardly.classes.askGeminiForDensity
import com.seniorproject.cupboardly.viewModels.IngredientViewModel
import com.seniorproject.cupboardly.viewModels.RecipeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.seniorproject.cupboardly.classes.askGeminiForDensity
import android.net.Uri
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import android.util.Log
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.runtime.Composable


@OptIn(ExperimentalMaterial3Api::class)

fun processPhotoUri(context: Context, photoUri: Uri?) {
    if (photoUri == null) return

    try {
        // Create an InputImage from the saved file URI
        val image = InputImage.fromFilePath(context, photoUri)

        // Create the text recognizer
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        // Run text recognition
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d("MLKit", "Detected text: ${visionText.text}")
            }
            .addOnFailureListener { e ->
                Log.e("MLKit", "Text recognition failed", e)
            }

    } catch (e: Exception) {
        Log.e("MLKit", "Failed to process image", e)
    }
}

@Composable
fun RecipeScreen(
    recipeViewModel: RecipeViewModel = viewModel(),
    ingredientViewModel: IngredientViewModel = viewModel(),
    onGoToIngredients: () -> Unit
) {
    var hasCameraPermission by remember { mutableStateOf(false) }

    val recipes by recipeViewModel.recipes.collectAsState()
    val allIngredients by ingredientViewModel.ingredients.collectAsState(initial = emptyList())
    val coroutineScope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val recipeIngredientsMap = remember { mutableStateMapOf<Long, String>() }
    val context = LocalContext.current

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Log.d("Camera", "Photo saved: $photoUri")
            // Pass context explicitly since processPhotoUri is no longer Composable
            processPhotoUri(context, photoUri)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var activeRecipe by remember { mutableStateOf<Long?>(null) }
    var startError by remember { mutableStateOf<String?>(null) }
    var startDialogIngredientInfo by remember { mutableStateOf<List<String>>(emptyList()) }

    // Override / delete dialogs
    var showOverrideConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var recipeToDelete by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(recipes) {
        recipes.forEach { recipe ->
            if (!recipeIngredientsMap.containsKey(recipe.id)) {
                val links = recipeViewModel.getIngredientsForRecipe(recipe.id)
                val ingredientsWithQuantities = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                    ingredient?.let {
                        val displayQty = ingredientViewModel.convertFromGrams(
                            link.quantityUsed,
                            link.unitUsed,
                            it.density
                        )
                        "${formatDouble(displayQty)} ${link.unitUsed} ${it.name}"
                    }
                }
                recipeIngredientsMap[recipe.id] =
                    ingredientsWithQuantities.joinToString(", ")
            }
        }
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            hasCameraPermission = true
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    val gold = Color(197, 145, 39)
    val darkBlue = Color(11, 186, 224)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {

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
                ) {
                    AutoSizeText(
                        text = "Ingredients",
                        maxFontSize = 16.sp,
                        minFontSize = 8.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

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

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(35.dp)
        ) {

            Button(
                onClick = { showMenu = true },
                modifier = Modifier.size(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = darkBlue)
            ) {
                Text("+", fontSize = 32.sp)
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {

                // Add Recipe
                DropdownMenuItem(
                    text = { Text("Add Recipe") },
                    onClick = {
                        showMenu = false
                        showAddDialog = true
                    }
                )

                // Take Photo
                DropdownMenuItem(
                    text = { Text("Take Photo") },
                    onClick = {
                        if (hasCameraPermission) {
                            // Create a file for the photo
                            val timestamp =
                                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val photoFile = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "JPEG_${timestamp}_.jpg"
                            )

                            // Get a content URI using FileProvider
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider", // MUST match manifest
                                photoFile
                            )

                            photoUri = uri

                            // Launch the camera safely
                            cameraLauncher.launch(uri)
                        } else {
                            // Request permission if not yet granted
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }
                )
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
                                    val existing =
                                        ingredientViewModel.getIngredientByName(trimmedName)
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
                            val unitOptions = listOf(
                                "unit",
                                "g",
                                "kg",
                                "oz",
                                "lb",
                                "ml",
                                "cup",
                                "tbsp",
                                "tsp",
                                "floz"
                            )

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
                                                selectedUnits[ingredient.id] =
                                                    ingredient.unit.ifBlank { "g" }
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
                                            onValueChange = {
                                                selectedIngredients[ingredient.id] = it
                                            },
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
                                                value = selectedUnits[ingredient.id]
                                                    ?: ingredient.unit.ifBlank { "g" },
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Unit") },
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                                        unitDropdownExpanded
                                                    )
                                                },
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
                            val recipeId = recipeViewModel.addRecipeAndReturnId(
                                name.trim(), instructions.trim(), currentDate
                            )

                            selectedIngredients.forEach { (id, qtyString) ->
                                val qty = qtyString.toDoubleOrNull() ?: 0.0
                                if (qty > 0) {
                                    val ingredient = allIngredients.find { it.id == id }
                                    val selectedUnit = selectedUnits[id] ?: ingredient?.unit ?: "g"
                                    val gramsToStore = ingredientViewModel.convertToGrams(
                                        qty,
                                        selectedUnit,
                                        ingredient?.density
                                    )
                                    recipeViewModel.addIngredientToRecipe(
                                        recipeId,
                                        id,
                                        gramsToStore,
                                        selectedUnit
                                    )
                                }
                            }

                            recipeViewModel.refresh()
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
                                recipeViewModel.deleteRecipeWithIngredients(recipeToDelete!!)
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

        if (showStartDialog && activeRecipe != null) {

            val recipe = recipes.find { it.id == activeRecipe }

            LaunchedEffect(activeRecipe) {
                val links = recipeViewModel.getIngredientsForRecipe(activeRecipe!!)
                startDialogIngredientInfo = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                    ingredient?.let {
                        val usedDisplay = ingredientViewModel.convertFromGrams(
                            link.quantityUsed, link.unitUsed, it.density
                        )

                        val remainingGrams = (it.quantity - link.quantityUsed).coerceAtLeast(0.0)

                        val afterDisplay = ingredientViewModel.convertFromGrams(
                            remainingGrams, link.unitUsed, it.density
                        )

                        "${it.name}: Use ${formatDouble(usedDisplay)} ${link.unitUsed}, " +
                                "Remaining ${formatDouble(afterDisplay)} ${link.unitUsed}"
                    }
                }
            }

            // ---------------- START RECIPE DIALOG ----------------

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
                                val links = recipeViewModel.getIngredientsForRecipe(activeRecipe!!)
                                val insufficient = mutableListOf<String>()

                                links.forEach { link ->
                                    val ingredient =
                                        ingredientViewModel.getIngredientById(link.ingredientId)
                                    if (ingredient != null && ingredient.quantity < link.quantityUsed) {
                                        // unchanged (no "(not enough)")
                                        insufficient.add("${ingredient.name}")
                                    }
                                }

                                if (insufficient.isNotEmpty()) {
                                    startError = "Not enough: ${insufficient.joinToString(", ")}"
                                    return@launch
                                }

                                links.forEach { link ->
                                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                                    ingredient?.let {
                                        if (it.quantity <= 0.0) return@let  // safety

                                        val usedQty = link.quantityUsed
                                        val remainingQty = (it.quantity - usedQty).coerceAtLeast(0.0)
                                        val pricePerUnit = if (it.quantity > 0.0) it.price / it.quantity else 0.0
                                        val remainingPrice = pricePerUnit * remainingQty

                                        val updated = it.copy(
                                            quantity = remainingQty,
                                            price = remainingPrice,
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

// ---------------- OVERRIDE CONFIRM DIALOG ----------------

        if (showOverrideConfirmDialog && activeRecipe != null) {

            AlertDialog(
                onDismissRequest = { showOverrideConfirmDialog = false },

                title = { Text("Are you sure?") },

                // ✅ FIX: show missing ingredients
                text = {
                    Text(
                        "You don't have enough ingredients:\n\n${startError ?: ""}\n\nContinue anyway?"
                    )
                },

                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            val links = recipeViewModel.getIngredientsForRecipe(activeRecipe!!)

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
                    Button(onClick = { showOverrideConfirmDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}