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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import androidx.compose.runtime.Composable
import com.seniorproject.cupboardly.classes.AiRecipe
import com.seniorproject.cupboardly.classes.askGeminiForRecipeParse
import com.seniorproject.cupboardly.room.entity.IngredientBatchEntity

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
            ingredientViewModel.updateBatch(batch.copy(quantity = batch.quantity - remaining))
            remaining = 0.0
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
    recipeViewModel: RecipeViewModel = viewModel(),
    ingredientViewModel: IngredientViewModel = viewModel(),
    onGoToIngredients: () -> Unit
) {
    var hasCameraPermission by remember { mutableStateOf(false) }

    val recipes by recipeViewModel.recipes.collectAsState()
    val allIngredients by ingredientViewModel.ingredients.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val recipeIngredientsMap = remember { mutableStateMapOf<Long, String>() }
    val context = LocalContext.current

    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // loading screens for during AI processing to ensure nothing can be pressed while processing
    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("") }

    // prefill values for when adding a recipe via camera scan
    var prefillName       by remember { mutableStateOf("") }
    var prefillInstructions by remember { mutableStateOf("") }
    val prefillTempIngredients = remember { mutableStateListOf<TempIngredient>() }
    val prefillSelectedIngredients = remember { mutableStateMapOf<Long, String>() }
    val prefillSelectedUnits       = remember { mutableStateMapOf<Long, String>() }


    var showAddDialog by remember { mutableStateOf(false) }

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
                                // Already exists = pre-check the checkbox
                                prefillSelectedIngredients[match.ingredient.id] =
                                    aiIngredient.quantity?.toString() ?: "1.0"
                                prefillSelectedUnits[match.ingredient.id] =
                                    aiIngredient.unit ?: match.ingredient.unit.ifBlank { "g" }
                            } else {
                                // Doesn't exist = goes into New Ingredients
                                prefillTempIngredients.add(
                                    TempIngredient(
                                        name     = aiIngredient.name,
                                        quantity = aiIngredient.quantity?.toString() ?: "1.0",
                                        unit     = aiIngredient.unit ?: "g"
                                    )
                                )
                            }
                        }
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

    var showMenu by remember { mutableStateOf(false) }
    var showStartDialog by remember { mutableStateOf(false) }
    var activeRecipe by remember { mutableStateOf<Long?>(null) }
    var startError by remember { mutableStateOf<String?>(null) }
    var startDialogIngredientInfo by remember { mutableStateOf<List<String>>(emptyList()) }

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

        // dropdown
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
                DropdownMenuItem(
                    text = { Text("Add Recipe") },
                    onClick = {
                        showMenu = false
                        prefillName = ""
                        prefillInstructions = ""
                        prefillTempIngredients.clear()
                        showAddDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text("Take Photo") },
                    onClick = {
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
                )
            }
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
                "unit", "g", "kg", "oz", "lb",
                "ml", "cup", "tbsp", "tsp", "floz"
            )

            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    // Clear prefill so a manually-opened dialog starts blank
                    prefillName = ""
                    prefillInstructions = ""
                    prefillTempIngredients.clear()
                    prefillSelectedUnits.clear()
                    prefillSelectedIngredients.clear()
                },
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

                        // ---------------- NEW INGREDIENTS SECTION ----------------

                        if (tempIngredients.isNotEmpty()) {
                            Divider()
                            Text("New Ingredients", fontWeight = FontWeight.Bold)

                            tempIngredients.forEach { temp ->
                                var unitDropdownExpanded by remember { mutableStateOf(false) }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(temp.name, modifier = Modifier.weight(1f))

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
                                                        temp.unit = option
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
                },
                confirmButton = {
                    Button(onClick = {
                        if (name.isBlank()) { nameError = true; return@Button }

                        coroutineScope.launch {
                            // set the loading message
                            isLoading = true
                            loadingMessage = "Adding recipe..."
                            val currentDate = (System.currentTimeMillis() / 1000).toInt()

                            val recipeId = recipeViewModel.addRecipeAndReturnId(
                                name.trim(), instructions.trim(), currentDate
                            )

                            // Create new ingredients and link them using the returned ID
                            tempIngredients.forEach { temp ->
                                val existing = ingredientViewModel.getIngredientByName(temp.name)

                                val targetId: Long
                                val targetDensity: Double?

                                if (existing != null) {
                                    // Already in DB — just link it, don't create a duplicate
                                    targetId = existing.id
                                    targetDensity = existing.density
                                } else {
                                    // actually new — create it
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

                            // Link existing selected ingredients
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
                            // no longer loading
                            isLoading = false
                            loadingMessage = ""
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    Button(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }

        // ---------------- DELETE RECIPE DIALOG ----------------

        if (showDeleteDialog && recipeToDelete != null) {
            val recipeName = recipes.find { it.id == recipeToDelete }?.name ?: ""

            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Recipe") },
                text = { Text("Delete \"$recipeName\"? This cannot be undone.") },
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
                    ) { Text("Delete") }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false; recipeToDelete = null }) {
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
                startDialogIngredientInfo = links.mapNotNull { link ->
                    val ingredient = ingredientViewModel.getIngredientById(link.ingredientId)
                    ingredient?.let {
                        val totalGrams = ingredientViewModel.getTotalQuantity(it.id)

                        val usedDisplay = ingredientViewModel.convertFromGrams(
                            link.quantityUsed, link.unitUsed, it.density
                        )
                        val remainingGrams = (totalGrams - link.quantityUsed).coerceAtLeast(0.0)
                        val afterDisplay = ingredientViewModel.convertFromGrams(
                            remainingGrams, link.unitUsed, it.density
                        )

                        "${it.name}: Use ${formatDouble(usedDisplay)} ${link.unitUsed}, " +
                                "Remaining ${formatDouble(afterDisplay)} ${link.unitUsed}"
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
                    Column {
                        Button(onClick = {
                            coroutineScope.launch {
                                val links = recipeViewModel.getIngredientsForRecipe(activeRecipe!!)
                                val insufficient = mutableListOf<String>()

                                links.forEach { link ->
                                    val ingredient =
                                        ingredientViewModel.getIngredientById(link.ingredientId)
                                    if (ingredient != null) {
                                        val totalGrams =
                                            ingredientViewModel.getTotalQuantity(ingredient.id)
                                        if (totalGrams < link.quantityUsed) {
                                            insufficient.add(ingredient.name)
                                        }
                                    }
                                }

                                if (insufficient.isNotEmpty()) {
                                    startError = "Not enough: ${insufficient.joinToString(", ")}"
                                    return@launch
                                }

                                links.forEach { link ->
                                    val ingredient =
                                        ingredientViewModel.getIngredientById(link.ingredientId)
                                    if (ingredient != null) {
                                        deductFromBatchesFifo(
                                            ingredientId = ingredient.id,
                                            gramsNeeded = link.quantityUsed,
                                            ingredientViewModel = ingredientViewModel
                                        )
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
                    Button(onClick = { showStartDialog = false }) { Text("Cancel") }
                }
            )
        }

        // ---------------- OVERRIDE CONFIRM DIALOG ----------------

        if (showOverrideConfirmDialog && activeRecipe != null) {

            AlertDialog(
                onDismissRequest = { showOverrideConfirmDialog = false },
                title = { Text("Are you sure?") },
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
                                if (ingredient != null) {
                                    val totalGrams =
                                        ingredientViewModel.getTotalQuantity(ingredient.id)
                                    val gramsToDeduct = minOf(link.quantityUsed, totalGrams)
                                    if (gramsToDeduct > 0.0) {
                                        deductFromBatchesFifo(
                                            ingredientId = ingredient.id,
                                            gramsNeeded = gramsToDeduct,
                                            ingredientViewModel = ingredientViewModel
                                        )
                                    }
                                }
                            }

                            showOverrideConfirmDialog = false
                            showStartDialog = false
                            startError = null
                        }
                    }) { Text("Yes, Continue") }
                },
                dismissButton = {
                    Button(onClick = { showOverrideConfirmDialog = false }) { Text("Cancel") }
                }
            )
        }

        // ---------------- LOADING OVERLAY ----------------
        if (isLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(enabled = false) {}  // dont allow any taps during loading
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
                        CircularProgressIndicator(color = darkBlue)
                        Text(loadingMessage, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
