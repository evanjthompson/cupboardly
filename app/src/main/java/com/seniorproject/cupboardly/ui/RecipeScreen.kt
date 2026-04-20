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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorproject.cupboardly.R
import com.seniorproject.cupboardly.classes.askGeminiForDensity
import com.seniorproject.cupboardly.viewModels.IngredientViewModel
import com.seniorproject.cupboardly.viewModels.RecipeViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
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
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FolderCopy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.seniorproject.cupboardly.classes.askGeminiForRecipeParse
import com.seniorproject.cupboardly.room.dao.RecipeDao
import com.seniorproject.cupboardly.room.entity.RecipeIngredientEntity

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

suspend fun processPhotoUri(context: Context, photoUri: Uri?): String? {
    if (photoUri == null) return null

    return try {
        val image = InputImage.fromFilePath(context, photoUri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        val result = kotlinx.coroutines.suspendCancellableCoroutine<String?> { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    Log.d("MLKit", "Detected text: ${visionText.text}")
                    cont.resume(visionText.text) {}
                }
                .addOnFailureListener { e ->
                    Log.e("MLKit", "Text recognition failed", e)
                    cont.resume(null) {}
                }
        }

        result
    } catch (e: Exception) {
        Log.e("MLKit", "Failed to process image", e)
        null
    }
}

/**
 * Calculate the cost per gram for an ingredient based on its batches.
 * Returns: total price of all batches / total quantity in grams
 */
suspend fun calculateCostPerGram(
    ingredientId: Long,
    ingredientViewModel: IngredientViewModel
): Double {
    val batches = ingredientViewModel.getBatchesForIngredient(ingredientId)
    val totalPrice = batches.sumOf { it.price }
    val totalQuantityGrams = batches.sumOf { it.quantity }

    return if (totalQuantityGrams > 0.0) totalPrice / totalQuantityGrams else 0.0
}

/**
 * Deducts [gramsNeeded] from an ingredient's batches using FIFO order (oldest first).
 * Fully consumed batches are deleted; the last partially consumed batch is updated.
 */
suspend fun deductFromBatchesFifo(
    ingredientId: Long,
    gramsNeeded: Double,
    ingredientViewModel: IngredientViewModel
) {
    val batches = ingredientViewModel
        .getBatchesForIngredient(ingredientId)
        .sortedBy { it.dateAdded }

    var remaining = gramsNeeded

    for (batch in batches) {
        if (remaining <= 0.0) break

        if (batch.quantity <= remaining) {
            remaining -= batch.quantity
            ingredientViewModel.deleteBatch(batch)
        } else {
            val newQty = batch.quantity - remaining
            remaining = 0.0
            // Auto-delete if the remaining quantity is effectively zero
            if (newQty <= 0.0) {
                ingredientViewModel.deleteBatch(batch)
            } else {
                ingredientViewModel.updateBatch(batch.copy(quantity = newQty))
            }
        }
    }
}


// ---------------------------------------------------------------------------
// TempIngredient
// ---------------------------------------------------------------------------

class TempIngredient(
    val name: String,
    quantity: String = "1.0",
    unit: String = "g"
) {
    var quantity by mutableStateOf(quantity)
    var unit by mutableStateOf(unit)
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@Composable
fun RecipeScreen(
    currentScreen: String,
    recipeViewModel: RecipeViewModel = viewModel(),
    ingredientViewModel: IngredientViewModel = viewModel(),
    onGoToIngredients: () -> Unit,
    onGoToManageData: () -> Unit
){
    var hasCameraPermission by remember { mutableStateOf(false) }

    val recipes by recipeViewModel.recipes.collectAsState()
    val allIngredients by ingredientViewModel.ingredients.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
    val recipeIngredientsMap = remember { mutableStateMapOf<Long, String>() }

    // store detailed ingredient info and recipe costs
    data class RecipeIngredientDetail(
        val name: String,
        val quantity: Double,
        val unit: String,
        val cost: Double
    )
    val recipeIngredientsDetailMap = remember { mutableStateMapOf<Long, List<RecipeIngredientDetail>>() }
    val recipeTotalCostMap = remember { mutableStateMapOf<Long, Double>() }

    val context = LocalContext.current

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // loading screens for during AI processing to ensure nothing can be pressed while processing
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }

    // prefill values for when adding a recipe via camera scan
    var prefillName by remember { mutableStateOf("") }
    var prefillInstructions by remember { mutableStateOf("") }
    val prefillTempIngredients = remember { mutableStateListOf<TempIngredient>() }
    val prefillSelectedIngredients = remember { mutableStateMapOf<Long, String>() }
    val prefillSelectedUnits = remember { mutableStateMapOf<Long, String>() }

    var showAddDialog by remember { mutableStateOf(false) }

    // Edit dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var editingRecipeId by remember { mutableStateOf<Long?>(null) }

    // Search state
    var searchQuery by remember { mutableStateOf("") }

    // Camera launcher — can be triggered from inside the Add dialog too
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            coroutineScope.launch {
                isLoading = true
                loadingMessage = "Analyzing photo..."

                val extractedText = processPhotoUri(context, photoUri)
                if (!extractedText.isNullOrBlank()) {
                    loadingMessage = "Thinking..."
                    val result = askGeminiForRecipeParse(
                        text = extractedText,
                        ingredientList = allIngredients
                    )
                    if (result != null) {
                        prefillName = result.name
                        prefillInstructions = result.instructions.joinToString("\n")
                        prefillTempIngredients.clear()
                        prefillSelectedIngredients.clear()
                        prefillSelectedUnits.clear()

                        result.ingredients.forEach { aiIngredient ->
                            val match = allIngredients.find {
                                it.ingredient.name.equals(aiIngredient.name, ignoreCase = true)
                            }
                            if (match != null) {
                                prefillSelectedIngredients[match.ingredient.id] =
                                    aiIngredient.quantity?.toString() ?: "1.0"
                                prefillSelectedUnits[match.ingredient.id] =
                                    aiIngredient.unit ?: match.ingredient.unit.ifBlank { "g" }
                            } else {
                                prefillTempIngredients.add(
                                    TempIngredient(
                                        name     = aiIngredient.name,
                                        quantity = aiIngredient.quantity?.toString() ?: "1.0",
                                        unit     = aiIngredient.unit ?: "g"
                                    )
                                )
                            }
                        }
                        // Close and reopen the dialog so it picks up the new prefill values
                        showAddDialog = false
                        showAddDialog = true
                    }
                }

                isLoading = false
                loadingMessage = ""
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Helper: launch camera (requests permission first if needed)
    fun launchCamera() {
        if (hasCameraPermission) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val photoFile = File(
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                "JPEG_${timestamp}_.jpg"
            )
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            photoUri = uri
            cameraLauncher.launch(uri)
        } else {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    var showStartDialog by remember { mutableStateOf(false) }
    var activeRecipe by remember { mutableStateOf<Long?>(null) }
    var startError by remember { mutableStateOf<String?>(null) }
    var startDialogIngredientInfo by remember { mutableStateOf<List<String>>(emptyList()) }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var recipeToDelete by remember { mutableStateOf<Long?>(null) }
    
    // Store the multiplier for each recipe when Start is clicked
    val recipeMultipliers = remember { mutableStateMapOf<Long, Double>() }

    LaunchedEffect(recipes) {
        recipes.forEach { recipe ->
            if (!recipeIngredientsDetailMap.containsKey(recipe.id)) {
                val links = recipeViewModel.getIngredientsForRecipe(recipe.id)
                val ingredientDetails = mutableListOf<RecipeIngredientDetail>()
                var totalRecipeCost = 0.0

                links.forEach { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                    ingredient?.let {
                        val displayQty = ingredientViewModel.convertFromGrams(
                            link.quantityUsed,
                            link.unitUsed,
                            it.density
                        )

                        // Calculate cost for this ingredient in the recipe
                        val costPerGram = calculateCostPerGram(link.ingredientId, ingredientViewModel)
                        val ingredientCost = link.quantityUsed * costPerGram

                        ingredientDetails.add(
                            RecipeIngredientDetail(
                                name = it.name,
                                quantity = displayQty,
                                unit = link.unitUsed,
                                cost = ingredientCost
                            )
                        )
                        totalRecipeCost += ingredientCost
                    }
                }

                recipeIngredientsDetailMap[recipe.id] = ingredientDetails
                recipeTotalCostMap[recipe.id] = totalRecipeCost

                // Also keep the old map for backward compatibility if needed
                val ingredientsWithQuantities = ingredientDetails.map {
                    "${formatDouble(it.quantity)} ${it.unit} ${it.name}"
                }
                recipeIngredientsMap[recipe.id] = ingredientsWithQuantities.joinToString(", ")
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

    val ingredientGold = Color(162, 119, 0)
    val recipeBlue = Color(91, 177, 184)

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
            // Ingredients + Recipes nav buttons
            TopNavTabs(
                currentScreen = currentScreen,
                onGoToIngredients = onGoToIngredients,
                onGoToRecipes = {}, // already here
                ingredientGold = ingredientGold,
                recipeBlue = recipeBlue
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color(0x22000000), // shared translucent background
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Search field
                OutlinedTextField(
                    colors = darkTextFieldColors(),
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search recipes") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 56.dp)
                )

                // Manage data button
                Button(
                    onClick = onGoToManageData,
                    modifier = Modifier.size(50.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.DarkGray,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderCopy,
                        contentDescription = "Manage Data"
                    )
                }
            }


            Spacer(modifier = Modifier.height(12.dp))

            // Filter recipes by search query
            val filteredRecipes = remember(recipes, searchQuery) {
                if (searchQuery.isBlank()) recipes
                else recipes.filter {
                    it.name.contains(searchQuery.trim(), ignoreCase = true)
                }
            }

            if (filteredRecipes.isEmpty()) {
                Text(
                    if (searchQuery.isBlank()) "No recipes yet"
                    else "No recipes match \"$searchQuery\""
                )
            }

            LazyColumn {
                items(filteredRecipes, key = { it.id }) { recipe ->

                    var expanded by remember(recipe.id) { mutableStateOf(false) }
                    val ingredientText = recipeIngredientsMap[recipe.id] ?: "Loading..."

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(2.dp, recipeBlue),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .animateContentSize()
                            .clickable { expanded = !expanded }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {

                            Text(recipe.name, fontWeight = FontWeight.SemiBold)

                            if (expanded) {
                                Spacer(modifier = Modifier.height(8.dp))

                                // Recipe multiplier controls
                                var multiplier by remember { mutableStateOf(1.0) }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Scale:", fontWeight = FontWeight.SemiBold, modifier = Modifier.width(50.dp))
                                    listOf(0.5, 1.0, 2.0).forEach { scale ->
                                        Button(
                                            onClick = { multiplier = scale },
                                            enabled = !multiplier.equals(scale),
                                            colors = if (multiplier.equals(scale))
                                                ButtonDefaults.buttonColors(containerColor = recipeBlue)
                                            else
                                                ButtonDefaults.buttonColors(containerColor = Color.LightGray),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(4.dp)
                                        ) {
                                            Text(
                                                if (scale == 0.5) "0.5x" else if (scale == 1.0) "1x" else "2x",
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                // Display ingredients with costs
                                val ingredientDetails = recipeIngredientsDetailMap[recipe.id] ?: emptyList()
                                if (ingredientDetails.isNotEmpty()) {
                                    Text("Ingredients:", fontWeight = FontWeight.Bold)
                                    ingredientDetails.forEach { detail ->
                                        val scaledQuantity = detail.quantity * multiplier
                                        val scaledCost = detail.cost * multiplier
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("${formatDouble(scaledQuantity)} ${detail.unit} ${detail.name}")
                                            Text("$${formatDouble(scaledCost)}")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider()
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total Recipe Cost:", fontWeight = FontWeight.SemiBold)
                                        val scaledTotalCost = (recipeTotalCostMap[recipe.id] ?: 0.0) * multiplier
                                        Text("$${formatDouble(scaledTotalCost)}", fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                } else {
                                    Text("Ingredients: $ingredientText")
                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                Text("Instructions: ${recipe.instructions}")
                                Text("Date Created: ${sdf.format(Date(recipe.dateCreated * 1000L))}")
                                Text("Times Followed: ${recipe.numTimesFollowed}")

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            activeRecipe = recipe.id
                                            recipeMultipliers[recipe.id] = multiplier
                                            startError = null
                                            startDialogIngredientInfo = emptyList()
                                            showStartDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = recipeBlue),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Start")
                                    }

                                    Button(
                                        onClick = {
                                            editingRecipeId = recipe.id
                                            showEditDialog = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = recipeBlue),
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
            onClick = {
                prefillName = ""
                prefillInstructions = ""
                prefillTempIngredients.clear()
                prefillSelectedIngredients.clear()
                prefillSelectedUnits.clear()
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(35.dp)
                .size(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = recipeBlue),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Recipe"
            )
        }

        // ---------------- ADD RECIPE DIALOG ----------------

        if (showAddDialog) {

            val tempIngredients = remember {
                mutableStateListOf<TempIngredient>().also { list ->
                    list.addAll(prefillTempIngredients)
                }
            }

            var name by remember { mutableStateOf(prefillName) }
            var instructions by remember { mutableStateOf(prefillInstructions) }
            var newIngredientName by remember { mutableStateOf("") }
            var nameError by remember { mutableStateOf(false) }
            var ingredientError by remember { mutableStateOf<String?>(null) }
            val selectedIngredients = remember {
                mutableStateMapOf<Long, String>().also { it.putAll(prefillSelectedIngredients) }
            }
            val selectedUnits = remember {
                mutableStateMapOf<Long, String>().also { it.putAll(prefillSelectedUnits) }
            }

            val unitOptions = listOf(
                "g", "kg", "oz", "lb",
                "ml", "gal", "cup", "tbsp", "tsp", "floz"
            )

            AlertDialog(
                containerColor = Color.White,
                titleContentColor = Color.Black,
                textContentColor = Color.Black,
                onDismissRequest = {
                    showAddDialog = false
                    prefillName = ""
                    prefillInstructions = ""
                    prefillTempIngredients.clear()
                    prefillSelectedUnits.clear()
                    prefillSelectedIngredients.clear()
                },
                title = { Text("Add New Recipe") },
                text = {
                    Box {
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
                                onValueChange = { name = it; nameError = false },
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
                                    onValueChange = { newIngredientName = it; ingredientError = null },
                                    label = { Text("Ingredient Name") },
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(onClick = {
                                    val trimmedName = newIngredientName.trim()
                                    if (trimmedName.isBlank()) return@Button

                                    coroutineScope.launch {
                                        val existsInDb = ingredientViewModel.getIngredientByName(trimmedName)
                                        val existsInTemp = tempIngredients.any {
                                            it.name.equals(trimmedName, ignoreCase = true)
                                        }

                                        if (existsInDb != null || existsInTemp) {
                                            ingredientError = "Ingredient already exists"
                                            return@launch
                                        }

                                        ingredientError = null
                                        tempIngredients.add(TempIngredient(trimmedName))
                                        newIngredientName = ""
                                    }
                                },
                                    colors = ButtonDefaults.buttonColors(containerColor = recipeBlue) ) {
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
                                items(allIngredients, key = { it.ingredient.id }) { ingredientWithQty ->
                                    val ingredient = ingredientWithQty.ingredient
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
                                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
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

                                items(tempIngredients, key = { "temp_${it.name}" }) { temp ->
                                    var unitDropdownExpanded by remember { mutableStateOf(false) }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(modifier = Modifier.width(48.dp))
                                        Text("[NEW] " + temp.name, modifier = Modifier.weight(1f))
                                        OutlinedTextField(
                                            value = temp.quantity,
                                            onValueChange = { temp.quantity = it },
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
                                                value = temp.unit,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Unit") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitDropdownExpanded) },
                                                modifier = Modifier
                                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
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
                                                        onClick = { temp.unit = option; unitDropdownExpanded = false }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(onClick = { tempIngredients.remove(temp) }) {
                                            Text("X", color = Color.Red, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable(enabled = false) {}
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = recipeBlue)
                                    Text(
                                        loadingMessage,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    // Bottom row: Camera | Cancel | Save
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Camera button (moved here from the FAB dropdown)
                        Button(
                            onClick = { launchCamera() },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = recipeBlue),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scan Recipe Photo"
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = { showAddDialog = false },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = recipeBlue)
                        ) { Text("Cancel") }

                        Button(
                            onClick = {
                                if (name.isBlank()) { nameError = true; return@Button }

                                coroutineScope.launch {
                                    isLoading = true
                                    loadingMessage = "Adding recipe..."
                                    val currentDate = (System.currentTimeMillis() / 1000).toInt()

                                    val recipeId = recipeViewModel.addRecipeAndReturnId(
                                        name.trim(), instructions.trim(), currentDate
                                    )

                                    tempIngredients.forEach { temp ->
                                        val existing = ingredientViewModel.getIngredientByName(temp.name)

                                        val targetId: Long
                                        val targetDensity: Double?

                                        if (existing != null) {
                                            targetId = existing.id
                                            targetDensity = existing.density
                                        } else {
                                            val densityValue = askGeminiForDensity(temp.name)
                                            targetId = ingredientViewModel.addIngredientAndReturnId(
                                                name = temp.name,
                                                unit = temp.unit,
                                                density = densityValue
                                            )
                                            targetDensity = densityValue
                                        }

                                        val qty = temp.quantity.toDoubleOrNull() ?: 0.0
                                        val grams = ingredientViewModel.convertToGrams(qty, temp.unit, targetDensity)

                                        recipeViewModel.addIngredientToRecipe(
                                            recipeId,
                                            targetId,
                                            grams,
                                            temp.unit
                                        )
                                    }

                                    selectedIngredients.forEach { (id, qtyString) ->
                                        val qty = qtyString.toDoubleOrNull() ?: 0.0
                                        if (qty > 0) {
                                            val ingredientEntity = allIngredients
                                                .find { it.ingredient.id == id }?.ingredient

                                            val selectedUnit = selectedUnits[id]
                                                ?: ingredientEntity?.unit ?: "g"

                                            val gramsToStore = ingredientViewModel.convertToGrams(
                                                qty, selectedUnit, ingredientEntity?.density
                                            )

                                            recipeViewModel.addIngredientToRecipe(
                                                recipeId, id, gramsToStore, selectedUnit
                                            )
                                        }
                                    }

                                    recipeViewModel.refresh()
                                    showAddDialog = false
                                    isLoading = false
                                    loadingMessage = ""
                                }
                            },
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = recipeBlue)
                        ){
                            Text(
                                "Save",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                // dismissButton is intentionally empty — buttons are all in confirmButton row
                dismissButton = {}
            )
        }

        // ---------------- EDIT RECIPE DIALOG ----------------

        if (showEditDialog && editingRecipeId != null) {
            val recipe = recipes.find { it.id == editingRecipeId }

            if (recipe != null) {
                var editName by remember { mutableStateOf(recipe.name) }
                var editInstructions by remember { mutableStateOf(recipe.instructions) }
                var editNameError by remember { mutableStateOf(false) }
                var editIngredientError by remember { mutableStateOf<String?>(null) }
                var newIngredientName by remember { mutableStateOf("") }
                var isSaving by remember { mutableStateOf(false) }

                val editSelectedIngredients = remember { mutableStateMapOf<Long, String>() }
                val editSelectedUnits = remember { mutableStateMapOf<Long, String>() }
                val editTempIngredients = remember { mutableStateListOf<TempIngredient>() }

                LaunchedEffect(editingRecipeId) {
                    val links = recipeViewModel.getIngredientsForRecipe(recipe.id)
                    links.forEach { link ->
                        val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                        if (ingredient != null) {
                            val displayQty = ingredientViewModel.convertFromGrams(
                                link.quantityUsed, link.unitUsed, ingredient.density
                            )
                            editSelectedIngredients[link.ingredientId] = formatDouble(displayQty)
                            editSelectedUnits[link.ingredientId] = link.unitUsed
                        }
                    }
                }

                val unitOptions = listOf(
                    "g", "kg", "oz", "lb",
                    "ml", "gal", "cup", "tbsp", "tsp", "floz"
                )

                AlertDialog(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    textContentColor = Color.Black,
                    onDismissRequest = {
                        if (!isSaving) {
                            showEditDialog = false
                            editingRecipeId = null
                        }
                    },
                    title = { Text("Edit Recipe") },
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
                                value = editName,
                                onValueChange = { editName = it; editNameError = false },
                                label = { Text("Recipe Name") },
                                isError = editNameError,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = editInstructions,
                                onValueChange = { editInstructions = it },
                                label = { Text("Instructions") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3
                            )

                            Divider()

                            Text("Add New Ingredient", fontWeight = FontWeight.Bold)

                            Row {
                                OutlinedTextField(
                                    value = newIngredientName,
                                    onValueChange = { newIngredientName = it; editIngredientError = null },
                                    label = { Text("Ingredient Name") },
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(onClick = {
                                    val trimmedName = newIngredientName.trim()
                                    if (trimmedName.isBlank()) return@Button

                                    coroutineScope.launch {
                                        val existsInDb = ingredientViewModel.getIngredientByName(trimmedName)
                                        val existsInTemp = editTempIngredients.any {
                                            it.name.equals(trimmedName, ignoreCase = true)
                                        }
                                        if (existsInDb != null || existsInTemp) {
                                            editIngredientError = "Ingredient already exists"
                                            return@launch
                                        }
                                        editIngredientError = null
                                        editTempIngredients.add(TempIngredient(trimmedName))
                                        newIngredientName = ""
                                    }
                                },
                                    colors = ButtonDefaults.buttonColors(containerColor = recipeBlue)) {
                                    Text("Add")
                                }
                            }

                            editIngredientError?.let {
                                Text(it, color = Color.Red, fontSize = 14.sp)
                            }

                            Divider()

                            Text("Select Ingredients", fontWeight = FontWeight.Bold)

                            LazyColumn(
                                modifier = Modifier
                                    .height(250.dp)
                                    .fillMaxWidth()
                            ) {
                                items(allIngredients, key = { it.ingredient.id }) { ingredientWithQty ->
                                    val ingredient = ingredientWithQty.ingredient
                                    val isSelected = editSelectedIngredients.containsKey(ingredient.id)
                                    var unitDropdownExpanded by remember { mutableStateOf(false) }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    editSelectedIngredients[ingredient.id] = "1.0"
                                                    editSelectedUnits[ingredient.id] = ingredient.unit.ifBlank { "g" }
                                                } else {
                                                    editSelectedIngredients.remove(ingredient.id)
                                                    editSelectedUnits.remove(ingredient.id)
                                                }
                                            }
                                        )
                                        Text(ingredient.name, modifier = Modifier.weight(1f))

                                        if (isSelected) {
                                            OutlinedTextField(
                                                value = editSelectedIngredients[ingredient.id] ?: "",
                                                onValueChange = { editSelectedIngredients[ingredient.id] = it },
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
                                                    value = editSelectedUnits[ingredient.id] ?: ingredient.unit.ifBlank { "g" },
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    label = { Text("Unit") },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitDropdownExpanded) },
                                                    modifier = Modifier
                                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
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
                                                                editSelectedUnits[ingredient.id] = option
                                                                unitDropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                items(editTempIngredients, key = { "edit_temp_${it.name}" }) { temp ->
                                    var unitDropdownExpanded by remember { mutableStateOf(false) }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Spacer(modifier = Modifier.width(48.dp))
                                        Text("[NEW] " + temp.name, modifier = Modifier.weight(1f))
                                        OutlinedTextField(
                                            value = temp.quantity,
                                            onValueChange = { temp.quantity = it },
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
                                                value = temp.unit,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Unit") },
                                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(unitDropdownExpanded) },
                                                modifier = Modifier
                                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
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
                                                        onClick = { temp.unit = option; unitDropdownExpanded = false }
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(onClick = { editTempIngredients.remove(temp) }) {
                                            Text("X", color = Color.Red, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        var showMoreMenu by remember { mutableStateOf(false) }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Menu button
                            Box(modifier = Modifier.weight(0.4f)) {
                                Button(
                                    onClick = { showMoreMenu = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("⌄")
                                }
                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    containerColor = Color.White
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = Color.Red) },
                                        onClick = {
                                            showMoreMenu = false
                                            recipeToDelete = recipe.id
                                            showDeleteConfirmDialog = true
                                        }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    showEditDialog = false
                                    editingRecipeId = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = recipeBlue),
                                enabled = !isSaving,
                                modifier = Modifier.weight(1f)
                            ){
                                Text(
                                    "Cancel",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = {
                                    if (editName.isBlank()) { editNameError = true; return@Button }

                                    coroutineScope.launch {
                                        isSaving = true

                                        recipeViewModel.updateRecipe(
                                            recipe.copy(
                                                name = editName.trim(),
                                                instructions = editInstructions.trim()
                                            )
                                        )

                                        val newLinks = mutableListOf<RecipeIngredientEntity>()

                                        editSelectedIngredients.forEach { (id, qtyString) ->
                                            val qty = qtyString.toDoubleOrNull() ?: 0.0
                                            if (qty > 0) {
                                                val ingredientEntity = allIngredients
                                                    .find { it.ingredient.id == id }?.ingredient
                                                val selectedUnit = editSelectedUnits[id]
                                                    ?: ingredientEntity?.unit ?: "g"
                                                val gramsToStore = ingredientViewModel.convertToGrams(
                                                    qty, selectedUnit, ingredientEntity?.density
                                                )
                                                newLinks.add(
                                                    RecipeIngredientEntity(
                                                        recipeId = recipe.id,
                                                        ingredientId = id,
                                                        quantityUsed = gramsToStore,
                                                        unitUsed = selectedUnit
                                                    )
                                                )
                                            }
                                        }

                                        editTempIngredients.forEach { temp ->
                                            val existing = ingredientViewModel.getIngredientByName(temp.name)
                                            val targetId: Long
                                            val targetDensity: Double?

                                            if (existing != null) {
                                                targetId = existing.id
                                                targetDensity = existing.density
                                            } else {
                                                val densityValue = askGeminiForDensity(temp.name)
                                                targetId = ingredientViewModel.addIngredientAndReturnId(
                                                    name = temp.name,
                                                    unit = temp.unit,
                                                    density = densityValue
                                                )
                                                targetDensity = densityValue
                                            }

                                            val qty = temp.quantity.toDoubleOrNull() ?: 0.0
                                            val grams = ingredientViewModel.convertToGrams(qty, temp.unit, targetDensity)
                                            newLinks.add(
                                                RecipeIngredientEntity(
                                                    recipeId = recipe.id,
                                                    ingredientId = targetId,
                                                    quantityUsed = grams,
                                                    unitUsed = temp.unit
                                                )
                                            )
                                        }

                                        recipeViewModel.replaceIngredientsForRecipe(recipe.id, newLinks)
                                        recipeIngredientsMap.remove(recipe.id)
                                        recipeIngredientsDetailMap.remove(recipe.id)
                                        recipeTotalCostMap.remove(recipe.id)

                                        recipeViewModel.refresh()
                                        isSaving = false
                                        showEditDialog = false
                                        editingRecipeId = null
                                    }
                                },
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(containerColor = recipeBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                } else {
                                    Text(
                                        "Save",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        // ---------------- DELETE RECIPE CONFIRMATION DIALOG ----------------

        if (showDeleteConfirmDialog && recipeToDelete != null) {
            val recipeName = recipes.find { it.id == recipeToDelete }?.name ?: ""

            AlertDialog(
                containerColor = Color.White,
                titleContentColor = Color.Black,
                textContentColor = Color.Black,
                onDismissRequest = { showDeleteConfirmDialog = false },
                title = { Text("Delete Recipe") },
                text = { Text("Delete \"$recipeName\"? This cannot be undone.") },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                recipeViewModel.deleteRecipeWithIngredients(recipeToDelete!!)
                                recipeIngredientsMap.remove(recipeToDelete)
                                recipeIngredientsDetailMap.remove(recipeToDelete)
                                recipeTotalCostMap.remove(recipeToDelete)
                                showDeleteConfirmDialog = false
                                showEditDialog = false
                                editingRecipeId = null
                                recipeToDelete = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    Button(onClick = { showDeleteConfirmDialog = false; recipeToDelete = null },
                        colors = ButtonDefaults.buttonColors(containerColor = recipeBlue)) {
                        Text("Cancel")
                    }
                }
            )
        }

        // ---------------- START RECIPE DIALOG ----------------

        if (showStartDialog && activeRecipe != null) {

            val recipe = recipes.find { it.id == activeRecipe }

            LaunchedEffect(activeRecipe) {
                val links = recipeViewModel.getIngredientsForRecipe(activeRecipe!!)
                val multiplier = recipeMultipliers[activeRecipe] ?: 1.0
                startDialogIngredientInfo = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                    ingredient?.let {
                        val totalGrams = ingredientViewModel.getTotalQuantity(it.id)
                        val scaledQuantityUsed = link.quantityUsed * multiplier

                        val usedDisplay = ingredientViewModel.convertFromGrams(
                            scaledQuantityUsed, link.unitUsed, it.density
                        )
                        val remainingGrams = (totalGrams - scaledQuantityUsed).coerceAtLeast(0.0)
                        val afterDisplay = ingredientViewModel.convertFromGrams(
                            remainingGrams, link.unitUsed, it.density
                        )

                        "${it.name}: Use ${formatDouble(usedDisplay)} ${link.unitUsed}, " +
                                "Remaining ${formatDouble(afterDisplay)} ${link.unitUsed}"
                    }
                }
            }

            AlertDialog(
                containerColor = Color.White,
                titleContentColor = Color.Black,
                textContentColor = Color.Black,
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
                            startDialogIngredientInfo.forEach { info -> Text(info) }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        startError?.let { Text(it, color = Color.Red) }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        coroutineScope.launch {
                            val links = recipeViewModel.getIngredientsForRecipe(activeRecipe!!)
                            val multiplier = recipeMultipliers[activeRecipe] ?: 1.0
                            
                            if (startError.isNullOrBlank()) {
                                // Normal flow: check for sufficient ingredients
                                val insufficient = mutableListOf<String>()

                                links.forEach { link ->
                                    val ingredient =
                                        ingredientViewModel.getIngredientById(link.ingredientId)
                                    if (ingredient != null) {
                                        val totalGrams =
                                            ingredientViewModel.getTotalQuantity(ingredient.id)
                                        val scaledQuantityNeeded = link.quantityUsed * multiplier
                                        if (totalGrams < scaledQuantityNeeded) {
                                            insufficient.add(ingredient.name)
                                        }
                                    }
                                }

                                if (insufficient.isNotEmpty()) {
                                    startError = "Not enough: ${insufficient.joinToString(", ")}"
                                    return@launch
                                }
                            }
                            
                            // Deduct ingredients (either full in normal flow, or partial in "Make Anyway" flow)
                            links.forEach { link ->
                                val ingredient =
                                    ingredientViewModel.getIngredientById(link.ingredientId)
                                if (ingredient != null) {
                                    val scaledQuantityNeeded = link.quantityUsed * multiplier
                                    
                                    if (startError.isNullOrBlank()) {
                                        // Normal flow: deduct full amount
                                        deductFromBatchesFifo(
                                            ingredientId = ingredient.id,
                                            gramsNeeded = scaledQuantityNeeded,
                                            ingredientViewModel = ingredientViewModel
                                        )
                                    } else {
                                        // "Make Anyway" flow: deduct whatever is available
                                        val totalGrams = ingredientViewModel.getTotalQuantity(ingredient.id)
                                        val gramsToDeduct = minOf(scaledQuantityNeeded, totalGrams)
                                        if (gramsToDeduct > 0.0) {
                                            deductFromBatchesFifo(
                                                ingredientId = ingredient.id,
                                                gramsNeeded = gramsToDeduct,
                                                ingredientViewModel = ingredientViewModel
                                            )
                                        }
                                    }
                                }
                            }

                            recipeViewModel.incrementNumTimesFollowed(activeRecipe!!)
                            showStartDialog = false
                            startError = null
                        }
                    },colors = ButtonDefaults.buttonColors(containerColor = recipeBlue) ) {
                        if (startError.isNullOrBlank()) {
                            Text("Confirm & Start")
                        } else {
                            Text("Make Anyway")
                        }
                    }
                },
                dismissButton = {
                    Button(onClick = { showStartDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = recipeBlue)
                    ) { Text("Cancel") }
                }
            )
        }

        // ---------------- LOADING OVERLAY ----------------
        if (isLoading && !showAddDialog) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = false) {}
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(color = recipeBlue)
                        Text(loadingMessage, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}