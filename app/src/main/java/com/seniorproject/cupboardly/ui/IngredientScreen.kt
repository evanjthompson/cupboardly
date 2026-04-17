package com.seniorproject.cupboardly.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FolderCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorproject.cupboardly.viewModels.IngredientViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.seniorproject.cupboardly.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.seniorproject.cupboardly.classes.askGeminiForDensity
import com.seniorproject.cupboardly.room.entity.IngredientBatchEntity
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

// ---------------------------------------------------------------------------
// Utilities
// ---------------------------------------------------------------------------

fun formatDouble(value: Double): String {
    val rounded = Math.round(value * 100) / 100.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

/** Parse a "YYYY-MM-DD" string into a Unix timestamp (seconds), or null if blank/invalid. */
fun parseDateToUnix(dateStr: String): Int? {
    if (dateStr.isBlank()) return null
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val date = sdf.parse(dateStr.trim()) ?: return null
        (date.time / 1000).toInt()
    } catch (e: Exception) {
        null
    }
}

fun unixToDateStr(unix: Int?): String {
    if (unix == null) return ""
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        sdf.format(Date(unix * 1000L))
    } catch (e: Exception) {
        ""
    }
}

fun getExpirationColor(expirationDate: Int): Color {
    val todayUnix = parseDateToUnix(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    ) ?: return Color.Gray

    val diffDays = (expirationDate - todayUnix) / (60 * 60 * 24)

    return when {
        diffDays < 0 -> Color(0xFFC62828)      // expired
        diffDays <= 3 -> Color(0xFFFF8C00)     // close to expiring
        diffDays <= 7 -> Color(0xFFD4A017)     // getting close
        else -> Color(0xFF2E7D32)              // still good
    }
}

@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    minFontSize: TextUnit = 8.sp,
    maxFontSize: TextUnit = 16.sp,
    style: TextStyle = LocalTextStyle.current
) {
    var fontSize by remember { mutableStateOf(maxFontSize) }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontSize = fontSize),
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && fontSize > minFontSize) {
                fontSize = (fontSize.value * 0.9f).sp
            }
        }
    )
}

// ---------------------------------------------------------------------------
// Date Picker Field
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpirationDateField(
    unixSeconds: Int?,
    onDateSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Expiration Date (optional)"
) {
    val displaySdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val displayText = if (unixSeconds != null) {
        displaySdf.format(Date(unixSeconds * 1000L))
    } else {
        ""
    }

    var showPicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = unixSeconds?.let { it * 1000L }
            ?: System.currentTimeMillis()
    )

    OutlinedTextField(
        colors = darkTextFieldColors(),
        value = displayText,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        placeholder = { Text("None") },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Pick date",
                    tint = Color.Black
                )
            }
        },
        modifier = modifier
            .clickable { showPicker = true }
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        onDateSelected(null)
                        showPicker = false
                    }) { Text("Clear") }

                    TextButton(onClick = {
                        val millis = datePickerState.selectedDateMillis
                        onDateSelected(millis?.let { (it / 1000).toInt() })
                        showPicker = false
                    }) { Text("OK") }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ---------------------------------------------------------------------------
// Shared text field colors
// ---------------------------------------------------------------------------

@Composable
fun darkTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    disabledTextColor = Color.Black,
    focusedLabelColor = Color.Black,
    unfocusedLabelColor = Color(0xFF444444),
    disabledLabelColor = Color(0xFF444444),
    focusedBorderColor = Color.Black,
    unfocusedBorderColor = Color(0xFF888888),
    focusedTrailingIconColor = Color.Black,
    unfocusedTrailingIconColor = Color(0xFF444444),
    focusedSupportingTextColor = Color.Black,
    unfocusedSupportingTextColor = Color(0xFF444444),
    cursorColor = Color.Black
)

// ---------------------------------------------------------------------------
// Helper: expiration string
// ---------------------------------------------------------------------------

@Composable
fun expirationAnnotatedString(
    addedText: String,
    expirationDate: Int?,
    sdf: SimpleDateFormat,
    baseColor: Color = Color.Gray
) = buildAnnotatedString {
    withStyle(SpanStyle(color = baseColor)) { append(addedText) }
    expirationDate?.let { expDate ->
        withStyle(SpanStyle(color = baseColor)) { append("  ·  ") }
        withStyle(SpanStyle(color = getExpirationColor(expDate))) {
            append("Expires ${sdf.format(Date(expDate * 1000L))}")
        }
    }
}

// ---------------------------------------------------------------------------
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientScreen(
    viewModel: IngredientViewModel = viewModel(),
    onGoToRecipes: () -> Unit,
    onGoToSettings: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
    val scope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val ingredients by viewModel.ingredients.collectAsState()

    // Add-new dialog state
    var showAddNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newQuantity by remember { mutableStateOf("") }
    var newUnit by remember { mutableStateOf("g") }
    var newPrice by remember { mutableStateOf("") }
    var newExpirationUnix by remember { mutableStateOf<Int?>(null) }

    var newNameError by remember { mutableStateOf<String?>(null) }
    var newQuantityError by remember { mutableStateOf<String?>(null) }
    var newPriceError by remember { mutableStateOf<String?>(null) }

    val ingredientGold = Color(197, 145, 39)
    val recipeBlue = Color(11, 186, 224)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Background
            Image(
                painter = painterResource(id = R.drawable.stripeingredientbg),
                contentDescription = "Ingredient Background",
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.matchParentSize()
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Tab row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ingredientGold,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Ingredients", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onGoToRecipes,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = recipeBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Recipes")
                    }
                    Button(
                        onClick = onGoToSettings,
                        modifier = Modifier.weight(.75f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.DarkGray,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderCopy,
                            contentDescription = "Settings"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (ingredients.isEmpty()) {
                    Text("No ingredients yet")
                }

                // ---------------------------------------------------------------------------
                // Ingredient list
                // ---------------------------------------------------------------------------
                LazyColumn {
                    items(
                        items = ingredients,
                        key = { it.ingredient.id }
                    ) { ingredientWithQty ->

                        val ingredient = ingredientWithQty.ingredient
                        val totalQuantityGrams = ingredientWithQty.totalQuantity

                        // Card state
                        var expanded by remember { mutableStateOf(false) }
                        var isEditing by remember { mutableStateOf(false) }
                        var isAddingMore by remember { mutableStateOf(false) }

                        var batches by remember {
                            mutableStateOf<List<IngredientBatchEntity>>(emptyList())
                        }

                        LaunchedEffect(expanded, totalQuantityGrams) {
                            if (expanded) {
                                batches = viewModel.getBatchesForIngredient(ingredient.id)
                            }
                        }

                        var displayUnit by remember { mutableStateOf(ingredient.unit) }
                        LaunchedEffect(ingredient.unit) { displayUnit = ingredient.unit }

                        // Edit form state — name only
                        var editName by remember { mutableStateOf(ingredient.name) }
                        var editNameError by remember { mutableStateOf<String?>(null) }

                        // Which batch is currently being edited in edit mode
                        var editingBatchId by remember { mutableStateOf<Long?>(null) }

                        // Per-batch edit state
                        var batchEditQuantity by remember { mutableStateOf("") }
                        var batchEditUnit by remember { mutableStateOf(ingredient.unit) }
                        var batchEditPrice by remember { mutableStateOf("") }
                        var batchEditExpirationUnix by remember { mutableStateOf<Int?>(null) }
                        var batchEditQuantityError by remember { mutableStateOf<String?>(null) }
                        var batchEditPriceError by remember { mutableStateOf<String?>(null) }

                        // Add-more batch state
                        var addMoreQuantity by remember { mutableStateOf("") }
                        var addMoreUnit by remember { mutableStateOf(ingredient.unit) }
                        var addMorePrice by remember { mutableStateOf("") }
                        var addMoreExpirationUnix by remember { mutableStateOf<Int?>(null) }
                        var addMoreQuantityError by remember { mutableStateOf<String?>(null) }
                        var addMorePriceError by remember { mutableStateOf<String?>(null) }

                        // Delete guard message
                        var deleteBlockedMessage by remember { mutableStateOf<String?>(null) }

                        // Confirmation dialog state
                        var showDeleteIngredientConfirm by remember { mutableStateOf(false) }
                        var showResetConfirm by remember { mutableStateOf(false) }
                        var batchPendingDelete by remember { mutableStateOf<IngredientBatchEntity?>(null) }

                        // Derived display quantity
                        val displayQty =
                            remember(totalQuantityGrams, ingredient.density, displayUnit) {
                                viewModel.convertFromGrams(
                                    totalQuantityGrams,
                                    displayUnit,
                                    ingredient.density
                                )
                            }

                        // -----------------------------------------------------------------------
                        // Confirmation: delete a single batch
                        // -----------------------------------------------------------------------
                        if (batchPendingDelete != null) {
                            val batchQtyDisplay = viewModel.convertFromGrams(
                                batchPendingDelete!!.quantity, displayUnit, ingredient.density
                            )
                            AlertDialog(
                                containerColor = Color.White,
                                titleContentColor = Color.Black,
                                textContentColor = Color.Black,
                                onDismissRequest = { batchPendingDelete = null },
                                title = { Text("Delete Batch") },
                                text = {
                                    Text(
                                        "Delete the batch of ${formatDouble(batchQtyDisplay)} $displayUnit " +
                                                "(added ${sdf.format(Date(batchPendingDelete!!.dateAdded * 1000L))})? " +
                                                "This cannot be undone."
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.deleteBatch(batchPendingDelete!!)
                                            editingBatchId = null
                                            batchPendingDelete = null
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) { Text("Delete") }
                                },
                                dismissButton = {
                                    Button(onClick = { batchPendingDelete = null }) { Text("Cancel") }
                                }
                            )
                        }

                        // -----------------------------------------------------------------------
                        // Confirmation: delete entire ingredient
                        // -----------------------------------------------------------------------
                        if (showDeleteIngredientConfirm) {
                            AlertDialog(
                                containerColor = Color.White,
                                titleContentColor = Color.Black,
                                textContentColor = Color.Black,
                                onDismissRequest = { showDeleteIngredientConfirm = false },
                                title = { Text("Delete Ingredient") },
                                text = {
                                    Text(
                                        "Delete \"${ingredient.name}\" and all its batches? This cannot be undone."
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                val inUse = viewModel.isUsedByRecipe(ingredient.id)
                                                if (inUse) {
                                                    deleteBlockedMessage = "In use by a recipe"
                                                } else {
                                                    viewModel.deleteIngredient(ingredient)
                                                    isEditing = false
                                                    viewModel.refresh()
                                                    editingBatchId = null
                                                    deleteBlockedMessage = null
                                                }
                                                showDeleteIngredientConfirm = false
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                    ) { Text("Delete") }
                                },
                                dismissButton = {
                                    Button(onClick = { showDeleteIngredientConfirm = false }) {
                                        Text("Cancel")
                                    }
                                }
                            )
                        }

                        // -----------------------------------------------------------------------
                        // Confirmation: reset ingredient (clear all batches)
                        // -----------------------------------------------------------------------
                        if (showResetConfirm) {
                            AlertDialog(
                                containerColor = Color.White,
                                titleContentColor = Color.Black,
                                textContentColor = Color.Black,
                                onDismissRequest = { showResetConfirm = false },
                                title = { Text("Reset Ingredient") },
                                text = {
                                    Text(
                                        "Reset \"${ingredient.name}\"? This will clear all batches and set the quantity to 0. This cannot be undone."
                                    )
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            viewModel.resetIngredient(ingredient.id)
                                            showResetConfirm = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF888888))
                                    ) { Text("Reset") }
                                },
                                dismissButton = {
                                    Button(onClick = { showResetConfirm = false }) { Text("Cancel") }
                                }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(2.dp, ingredientGold),
                            color = Color.White,
                            contentColor = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .animateContentSize()
                                .clickable {
                                    expanded = !expanded
                                    if (!expanded) {
                                        isEditing = false
                                        isAddingMore = false
                                        editingBatchId = null
                                        deleteBlockedMessage = null
                                    }
                                }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {

                                // Header row
                                when {
                                    isEditing -> {
                                        Text(
                                            text = editName.ifBlank { ingredient.name },
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }

                                    ingredient.unit == "unit" -> {
                                        Text("${ingredient.name}  ${formatDouble(totalQuantityGrams)} ${ingredient.unit}")
                                    }

                                    else -> {
                                        val unitOptions = listOf(
                                            "g", "kg", "oz", "lb",
                                            "ml", "gal", "cup", "tbsp", "tsp", "floz"
                                        )
                                        var unitDropdownExpanded by remember { mutableStateOf(false) }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "${ingredient.name}  ${formatDouble(displayQty)}",
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            ExposedDropdownMenuBox(
                                                expanded = unitDropdownExpanded,
                                                onExpandedChange = { unitDropdownExpanded = it },
                                                modifier = Modifier.width(110.dp)
                                            ) {
                                                OutlinedTextField(
                                                    colors = darkTextFieldColors(),
                                                    value = displayUnit,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    trailingIcon = {
                                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                            unitDropdownExpanded
                                                        )
                                                    },
                                                    modifier = Modifier
                                                        .menuAnchor(
                                                            ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                                            true
                                                        )
                                                        .fillMaxWidth(),
                                                    singleLine = true
                                                )
                                                ExposedDropdownMenu(
                                                    containerColor = Color.White,
                                                    expanded = unitDropdownExpanded,
                                                    onDismissRequest = { unitDropdownExpanded = false }
                                                ) {
                                                    unitOptions.forEach { option ->
                                                        DropdownMenuItem(
                                                            text = { Text(option, color = Color.Black) },
                                                            onClick = {
                                                                displayUnit = option
                                                                unitDropdownExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Expanded content
                                if (expanded) {
                                    when {

                                        // ADD MORE
                                        isAddingMore -> {
                                            Spacer(modifier = Modifier.height(8.dp))

                                            val addMoreUnitOptions = if (ingredient.unit == "unit") {
                                                listOf("unit")
                                            } else {
                                                listOf("g", "kg", "oz", "lb",
                                                    "ml", "gal", "cup", "tbsp", "tsp", "floz")
                                            }
                                            var addMoreUnitDropdownExpanded by remember { mutableStateOf(false) }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    colors = darkTextFieldColors(),
                                                    value = addMoreQuantity,
                                                    onValueChange = {
                                                        addMoreQuantity = it
                                                        addMoreQuantityError = null
                                                    },
                                                    label = { Text("Quantity") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    isError = addMoreQuantityError != null,
                                                    supportingText = {
                                                        addMoreQuantityError?.let {
                                                            Text(it, color = MaterialTheme.colorScheme.error)
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f)
                                                )

                                                if (ingredient.unit != "unit") {
                                                    ExposedDropdownMenuBox(
                                                        expanded = addMoreUnitDropdownExpanded,
                                                        onExpandedChange = { addMoreUnitDropdownExpanded = it },
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        OutlinedTextField(
                                                            colors = darkTextFieldColors(),
                                                            value = addMoreUnit,
                                                            onValueChange = {},
                                                            readOnly = true,
                                                            label = { Text("Unit") },
                                                            trailingIcon = {
                                                                ExposedDropdownMenuDefaults.TrailingIcon(
                                                                    addMoreUnitDropdownExpanded
                                                                )
                                                            },
                                                            modifier = Modifier
                                                                .menuAnchor(
                                                                    ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                                                    true
                                                                )
                                                                .fillMaxWidth()
                                                        )
                                                        ExposedDropdownMenu(
                                                            containerColor = Color.White,
                                                            expanded = addMoreUnitDropdownExpanded,
                                                            onDismissRequest = { addMoreUnitDropdownExpanded = false }
                                                        ) {
                                                            addMoreUnitOptions.forEach { option ->
                                                                DropdownMenuItem(
                                                                    text = { Text(option, color = Color.Black) },
                                                                    onClick = {
                                                                        addMoreUnit = option
                                                                        displayUnit = option
                                                                        addMoreUnitDropdownExpanded = false
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            OutlinedTextField(
                                                colors = darkTextFieldColors(),
                                                value = addMorePrice,
                                                onValueChange = {
                                                    addMorePrice = it
                                                    addMorePriceError = null
                                                },
                                                label = { Text("Price of this batch ($)") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                isError = addMorePriceError != null,
                                                supportingText = {
                                                    addMorePriceError?.let {
                                                        Text(it, color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            ExpirationDateField(
                                                unixSeconds = addMoreExpirationUnix,
                                                onDateSelected = { addMoreExpirationUnix = it },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        addMoreQuantity = ""
                                                        addMoreUnit = ingredient.unit
                                                        addMorePrice = ""
                                                        addMoreExpirationUnix = null
                                                        addMoreQuantityError = null
                                                        addMorePriceError = null
                                                        isAddingMore = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ingredientGold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Cancel") }

                                                Button(
                                                    onClick = {
                                                        val quantityValue = addMoreQuantity.toDoubleOrNull()
                                                        val priceValue = addMorePrice.toDoubleOrNull()
                                                        var valid = true

                                                        if (quantityValue == null || quantityValue <= 0) {
                                                            addMoreQuantityError = "Quantity must be > 0"
                                                            valid = false
                                                        }
                                                        if (priceValue == null || priceValue < 0) {
                                                            addMorePriceError = "Enter a valid price"
                                                            valid = false
                                                        }

                                                        if (valid) {
                                                            val gramsToAdd = viewModel.convertToGrams(
                                                                quantityValue!!,
                                                                addMoreUnit,
                                                                ingredient.density
                                                            )
                                                            val currentDate = (System.currentTimeMillis() / 1000).toInt()

                                                            viewModel.addBatch(
                                                                ingredientId = ingredient.id,
                                                                quantity = gramsToAdd,
                                                                price = priceValue!!,
                                                                expirationDate = addMoreExpirationUnix,
                                                                dateAdded = currentDate
                                                            )

                                                            addMoreQuantity = ""
                                                            addMoreUnit = ingredient.unit
                                                            addMorePrice = ""
                                                            addMoreExpirationUnix = null
                                                            isAddingMore = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ingredientGold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Confirm") }
                                            }
                                        }

                                        // EDIT
                                        isEditing -> {
                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedTextField(
                                                colors = darkTextFieldColors(),
                                                value = editName,
                                                onValueChange = { editName = it; editNameError = null },
                                                label = { Text("Name (currently ${ingredient.name})") },
                                                isError = editNameError != null,
                                                supportingText = {
                                                    editNameError?.let {
                                                        Text(it, color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (batches.isEmpty()) {
                                                Text(
                                                    "No batches recorded",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            } else {
                                                Text(
                                                    "Tap a batch to edit or delete it:",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))

                                                batches.forEach { batch ->
                                                    val isThisBatchEditing = editingBatchId == batch.id
                                                    val batchDisplayQty = viewModel.convertFromGrams(
                                                        batch.quantity, displayUnit, ingredient.density
                                                    )

                                                    if (isThisBatchEditing) {
                                                        val batchEditUnitOptions = if (ingredient.unit == "unit") {
                                                            listOf("unit")
                                                        } else {
                                                            listOf("g", "kg", "oz", "lb", "ml", "gal", "cup", "tbsp", "tsp", "floz")
                                                        }
                                                        var batchEditUnitDropdownExpanded by remember { mutableStateOf(false) }

                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color.White,
                                                            border = BorderStroke(1.dp, ingredientGold),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp)
                                                        ) {
                                                            Column(modifier = Modifier.padding(8.dp)) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    OutlinedTextField(
                                                                        colors = darkTextFieldColors(),
                                                                        value = batchEditQuantity,
                                                                        onValueChange = {
                                                                            batchEditQuantity = it
                                                                            batchEditQuantityError = null
                                                                        },
                                                                        label = { Text("Quantity") },
                                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                        isError = batchEditQuantityError != null,
                                                                        supportingText = {
                                                                            batchEditQuantityError?.let {
                                                                                Text(it, color = MaterialTheme.colorScheme.error)
                                                                            }
                                                                        },
                                                                        modifier = Modifier.weight(1f)
                                                                    )

                                                                    if (ingredient.unit != "unit") {
                                                                        ExposedDropdownMenuBox(
                                                                            expanded = batchEditUnitDropdownExpanded,
                                                                            onExpandedChange = { batchEditUnitDropdownExpanded = it },
                                                                            modifier = Modifier.weight(1f)
                                                                        ) {
                                                                            OutlinedTextField(
                                                                                colors = darkTextFieldColors(),
                                                                                value = batchEditUnit,
                                                                                onValueChange = {},
                                                                                readOnly = true,
                                                                                label = { Text("Unit") },
                                                                                trailingIcon = {
                                                                                    ExposedDropdownMenuDefaults.TrailingIcon(
                                                                                        batchEditUnitDropdownExpanded
                                                                                    )
                                                                                },
                                                                                modifier = Modifier
                                                                                    .menuAnchor(
                                                                                        ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                                                                                        true
                                                                                    )
                                                                                    .fillMaxWidth()
                                                                            )
                                                                            ExposedDropdownMenu(
                                                                                containerColor = Color.White,
                                                                                expanded = batchEditUnitDropdownExpanded,
                                                                                onDismissRequest = { batchEditUnitDropdownExpanded = false }
                                                                            ) {
                                                                                batchEditUnitOptions.forEach { option ->
                                                                                    DropdownMenuItem(
                                                                                        text = { Text(option, color = Color.Black) },
                                                                                        onClick = {
                                                                                            batchEditUnit = option
                                                                                            displayUnit = option
                                                                                            batchEditUnitDropdownExpanded = false
                                                                                        }
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }

                                                                OutlinedTextField(
                                                                    colors = darkTextFieldColors(),
                                                                    value = batchEditPrice,
                                                                    onValueChange = {
                                                                        batchEditPrice = it
                                                                        batchEditPriceError = null
                                                                    },
                                                                    label = { Text("Price ($)") },
                                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                                    isError = batchEditPriceError != null,
                                                                    supportingText = {
                                                                        batchEditPriceError?.let {
                                                                            Text(it, color = MaterialTheme.colorScheme.error)
                                                                        }
                                                                    },
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )

                                                                ExpirationDateField(
                                                                    unixSeconds = batchEditExpirationUnix,
                                                                    onDateSelected = { batchEditExpirationUnix = it },
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )

                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    Button(
                                                                        onClick = {
                                                                            editingBatchId = null
                                                                            batchEditQuantityError = null
                                                                            batchEditPriceError = null
                                                                        },
                                                                        colors = ButtonDefaults.buttonColors(containerColor = ingredientGold),
                                                                        modifier = Modifier.weight(1f)
                                                                    ) { Text("Cancel") }

                                                                    Button(
                                                                        onClick = {
                                                                            val qVal = batchEditQuantity.toDoubleOrNull()
                                                                            val pVal = batchEditPrice.toDoubleOrNull()
                                                                            var valid = true

                                                                            if (qVal == null || qVal <= 0) {
                                                                                batchEditQuantityError = "Quantity must be > 0"
                                                                                valid = false
                                                                            }
                                                                            if (pVal == null || pVal < 0) {
                                                                                batchEditPriceError = "Enter a valid price"
                                                                                valid = false
                                                                            }

                                                                            if (valid) {
                                                                                val newGrams = viewModel.convertToGrams(
                                                                                    qVal!!,
                                                                                    batchEditUnit,
                                                                                    ingredient.density
                                                                                )
                                                                                viewModel.updateBatch(
                                                                                    batch.copy(
                                                                                        quantity = newGrams,
                                                                                        price = pVal!!,
                                                                                        expirationDate = batchEditExpirationUnix
                                                                                    )
                                                                                )
                                                                                editingBatchId = null
                                                                            }
                                                                        },
                                                                        colors = ButtonDefaults.buttonColors(containerColor = ingredientGold),
                                                                        modifier = Modifier.weight(1f)
                                                                    ) { Text("Save") }

                                                                    // Delete batch — now shows confirmation dialog
                                                                    Button(
                                                                        onClick = {
                                                                            batchPendingDelete = batch
                                                                        },
                                                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                                        modifier = Modifier.weight(0.75f)
                                                                    ) {
                                                                        AutoSizeText(
                                                                            text = "Delete",
                                                                            maxFontSize = 16.sp,
                                                                            minFontSize = 8.sp,
                                                                            modifier = Modifier.fillMaxWidth()
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        val addedText = "Added ${sdf.format(Date(batch.dateAdded * 1000L))}"

                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFFF5F5F5),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 3.dp)
                                                                .clickable {
                                                                    editingBatchId = batch.id
                                                                    batchEditUnit = displayUnit
                                                                    batchEditQuantity = formatDouble(
                                                                        viewModel.convertFromGrams(
                                                                            batch.quantity,
                                                                            displayUnit,
                                                                            ingredient.density
                                                                        )
                                                                    )
                                                                    batchEditPrice = formatDouble(batch.price)
                                                                    batchEditExpirationUnix = batch.expirationDate
                                                                    batchEditQuantityError = null
                                                                    batchEditPriceError = null
                                                                }
                                                        ) {
                                                            Column(modifier = Modifier.padding(8.dp)) {
                                                                Text(
                                                                    "${formatDouble(batchDisplayQty)} $displayUnit  —  $${formatDouble(batch.price)}",
                                                                    style = MaterialTheme.typography.bodyMedium
                                                                )
                                                                Text(
                                                                    text = expirationAnnotatedString(
                                                                        addedText = addedText,
                                                                        expirationDate = batch.expirationDate,
                                                                        sdf = sdf
                                                                    ),
                                                                    style = MaterialTheme.typography.bodySmall
                                                                )
                                                            }
                                                        }
                                                        HorizontalDivider(color = Color(0xFFEEEEEE))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        isEditing = false
                                                        viewModel.refresh()
                                                        editingBatchId = null
                                                        deleteBlockedMessage = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ingredientGold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Cancel") }

                                                Button(
                                                    onClick = {
                                                        val trimmedName = editName.trim()
                                                        var valid = true

                                                        if (trimmedName.isBlank()) {
                                                            editNameError = "Enter a name"
                                                            valid = false
                                                        }
                                                        if (ingredients.any {
                                                                it.ingredient.id != ingredient.id &&
                                                                        it.ingredient.name.equals(trimmedName, ignoreCase = true)
                                                            }
                                                        ) {
                                                            editNameError = "Name already exists"
                                                            valid = false
                                                        }

                                                        if (valid) {
                                                            viewModel.updateIngredient(
                                                                ingredient.copy(name = trimmedName)
                                                            )
                                                            isEditing = false
                                                            viewModel.refresh()
                                                            editingBatchId = null
                                                            deleteBlockedMessage = null
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ingredientGold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Confirm") }

                                                // Delete ingredient — now shows confirmation dialog
                                                Button(
                                                    onClick = { showDeleteIngredientConfirm = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                    modifier = Modifier.weight(0.75f)
                                                ) {
                                                    AutoSizeText(
                                                        text = "Delete",
                                                        maxFontSize = 16.sp,
                                                        minFontSize = 8.sp,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }

                                            deleteBlockedMessage?.let { msg ->
                                                Text(
                                                    text = msg,
                                                    color = Color.Red,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }

                                        // DEFAULT VIEW
                                        else -> {
                                            Spacer(modifier = Modifier.height(8.dp))

                                            val totalPrice = batches.sumOf { it.price }
                                            val totalCostPerUnit = if (displayQty > 0) totalPrice / displayQty else 0.0
                                            Text(
                                                "Total value: $${formatDouble(totalPrice)}  ·  Cost/unit: $${formatDouble(totalCostPerUnit)} /$displayUnit",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            if (batches.isEmpty()) {
                                                Text(
                                                    "No batches recorded",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.Gray
                                                )
                                            } else {
                                                batches.forEach { batch ->
                                                    val batchDisplayQty = viewModel.convertFromGrams(
                                                        batch.quantity, displayUnit, ingredient.density
                                                    )
                                                    val batchCostPerUnit =
                                                        if (batchDisplayQty > 0) batch.price / batchDisplayQty else 0.0
                                                    val addedText = "Added ${sdf.format(Date(batch.dateAdded * 1000L))}"

                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 3.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                "${formatDouble(batchDisplayQty)} $displayUnit  —  $${formatDouble(batch.price)}  ·  $${formatDouble(batchCostPerUnit)}/$displayUnit",
                                                                style = MaterialTheme.typography.bodyMedium
                                                            )
                                                            Text(
                                                                text = expirationAnnotatedString(
                                                                    addedText = addedText,
                                                                    expirationDate = batch.expirationDate,
                                                                    sdf = sdf
                                                                ),
                                                                style = MaterialTheme.typography.bodySmall
                                                            )
                                                        }
                                                    }
                                                    HorizontalDivider(color = Color(0xFFEEEEEE))
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        isAddingMore = true
                                                        isEditing = false
                                                        viewModel.refresh()
                                                        addMoreQuantity = ""
                                                        addMoreUnit = ingredient.unit
                                                        addMorePrice = ""
                                                        addMoreExpirationUnix = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ingredientGold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Add More") }

                                                Button(
                                                    onClick = {
                                                        isEditing = true
                                                        isAddingMore = false
                                                        editName = ingredient.name
                                                        editNameError = null
                                                        editingBatchId = null
                                                        deleteBlockedMessage = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = ingredientGold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Edit") }

                                                // Reset — now shows confirmation dialog
                                                Button(
                                                    onClick = { showResetConfirm = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF888888)),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Reset") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Add New button
            Button(
                onClick = { showAddNew = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(35.dp)
                    .size(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ingredientGold,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Ingredient"
                )
            }

            // Add New dialog
            if (showAddNew) {
                var dialogDropdownExpanded by remember { mutableStateOf(false) }

                val matchingIngredients = ingredients.filter {
                    it.ingredient.name.contains(newName.trim(), ignoreCase = true) &&
                            newName.isNotBlank()
                }

                AlertDialog(
                    containerColor = Color.White,
                    titleContentColor = Color.Black,
                    textContentColor = Color.Black,
                    onDismissRequest = {
                        showAddNew = false
                        newName = ""; newQuantity = ""; newUnit = "g"; newPrice = ""
                        newExpirationUnix = null
                        newNameError = null; newQuantityError = null; newPriceError = null
                    },
                    confirmButton = {
                        Button(onClick = {
                            newNameError = null
                            newQuantityError = null
                            newPriceError = null

                            val trimmedName = newName.trim()
                            val quantityValue = newQuantity.toDoubleOrNull()
                            val priceValue = newPrice.toDoubleOrNull()
                            var isValid = true

                            if (trimmedName.isBlank()) {
                                newNameError = "Please enter a name"
                                isValid = false
                            }
                            if (quantityValue == null || quantityValue <= 0.0) {
                                newQuantityError = "Quantity must be > 0"
                                isValid = false
                            }
                            if (priceValue == null || priceValue < 0.0) {
                                newPriceError = "Enter the purchase price"
                                isValid = false
                            }

                            if (!isValid) return@Button

                            scope.launch {
                                val currentDate = (System.currentTimeMillis() / 1000).toInt()

                                val existingIngredient = ingredients
                                    .find { it.ingredient.name.equals(trimmedName, ignoreCase = true) }
                                    ?.ingredient

                                val densityValue = when {
                                    existingIngredient != null -> existingIngredient.density
                                    newUnit == "unit" -> 1.0
                                    else -> askGeminiForDensity(trimmedName)
                                }

                                viewModel.addIngredient(
                                    name = trimmedName,
                                    quantity = quantityValue!!,
                                    density = densityValue,
                                    unit = newUnit,
                                    price = priceValue!!,
                                    dateEntered = currentDate,
                                    expirationDate = newExpirationUnix
                                )

                                newName = ""; newQuantity = ""; newUnit = "g"; newPrice = ""
                                newExpirationUnix = null
                                showAddNew = false
                            }
                        }) { Text("Add") }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showAddNew = false
                            newName = ""; newQuantity = ""; newUnit = "g"; newPrice = ""
                            newExpirationUnix = null
                            newNameError = null; newQuantityError = null; newPriceError = null
                        }) { Text("Cancel") }
                    },
                    title = { Text("Add Ingredient") },
                    text = {
                        Column {
                            ExposedDropdownMenuBox(
                                expanded = dialogDropdownExpanded && matchingIngredients.isNotEmpty(),
                                onExpandedChange = { dialogDropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    colors = darkTextFieldColors(),
                                    value = newName,
                                    onValueChange = { newName = it; dialogDropdownExpanded = true },
                                    label = { Text("Ingredient Name") },
                                    isError = newNameError != null,
                                    modifier = Modifier
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable, true)
                                        .fillMaxWidth(),
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(dialogDropdownExpanded)
                                    },
                                    supportingText = {
                                        newNameError?.let {
                                            Text(it, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                )
                                ExposedDropdownMenu(
                                    containerColor = Color.White,
                                    expanded = dialogDropdownExpanded && matchingIngredients.isNotEmpty(),
                                    onDismissRequest = { dialogDropdownExpanded = false }
                                ) {
                                    matchingIngredients.forEach { ingredientWithQty ->
                                        DropdownMenuItem(
                                            text = { Text(ingredientWithQty.ingredient.name, color = Color.Black) },
                                            onClick = {
                                                newName = ingredientWithQty.ingredient.name
                                                newUnit = ingredientWithQty.ingredient.unit
                                                newPrice = ""
                                                dialogDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            val unitOptions = listOf(
                                "unit", "g", "kg", "oz", "lb",
                                "ml", "gal", "cup", "tbsp", "tsp", "floz"
                            )
                            var unitDropdownExpanded by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    colors = darkTextFieldColors(),
                                    value = newQuantity,
                                    onValueChange = { newQuantity = it; newQuantityError = null },
                                    label = { Text("Quantity") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    isError = newQuantityError != null,
                                    modifier = Modifier.weight(1f),
                                    supportingText = {
                                        newQuantityError?.let {
                                            Text(it, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                )

                                ExposedDropdownMenuBox(
                                    expanded = unitDropdownExpanded,
                                    onExpandedChange = { unitDropdownExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        colors = darkTextFieldColors(),
                                        value = newUnit,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Unit") },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(unitDropdownExpanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        containerColor = Color.White,
                                        expanded = unitDropdownExpanded,
                                        onDismissRequest = { unitDropdownExpanded = false }
                                    ) {
                                        unitOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option, color = Color.Black) },
                                                onClick = { newUnit = option; unitDropdownExpanded = false }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                colors = darkTextFieldColors(),
                                value = newPrice,
                                onValueChange = { newPrice = it; newPriceError = null },
                                label = { Text("Price ($)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = newPriceError != null,
                                modifier = Modifier.fillMaxWidth(),
                                supportingText = {
                                    newPriceError?.let {
                                        Text(it, color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            ExpirationDateField(
                                unixSeconds = newExpirationUnix,
                                onDateSelected = { newExpirationUnix = it },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }
        }
    }
}