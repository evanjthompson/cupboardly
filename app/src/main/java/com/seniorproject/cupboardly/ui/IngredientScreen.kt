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
// Screen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientScreen(
    viewModel: IngredientViewModel = viewModel(),
    onGoToRecipes: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val ingredients by viewModel.ingredients.collectAsState()

    // Add-new dialog state
    var showAddNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newQuantity by remember { mutableStateOf("") }
    var newUnit by remember { mutableStateOf("g") }
    var newPrice by remember { mutableStateOf("") }
    var newExpirationStr by remember { mutableStateOf("") }

    var newNameError by remember { mutableStateOf<String?>(null) }
    var newQuantityError by remember { mutableStateOf<String?>(null) }
    var newPriceError by remember { mutableStateOf<String?>(null) }

    val gold = Color(197, 145, 39)
    val darkBlue = Color(11, 186, 224)

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
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {},
                        modifier = Modifier.weight(2f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gold,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Ingredients", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onGoToRecipes,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = darkBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Recipes")
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

                        // Which batch is currently being edited in edit mode (null = none)
                        var editingBatchId by remember { mutableStateOf<Long?>(null) }

                        // Per-batch edit state — keyed by batch id, populated when a batch row is tapped
                        var batchEditQuantity by remember { mutableStateOf("") }
                        var batchEditUnit by remember { mutableStateOf(ingredient.unit) }
                        var batchEditPrice by remember { mutableStateOf("") }
                        var batchEditExpirationStr by remember { mutableStateOf("") }
                        var batchEditQuantityError by remember { mutableStateOf<String?>(null) }
                        var batchEditPriceError by remember { mutableStateOf<String?>(null) }

                        // Add-more batch state
                        var addMoreQuantity by remember { mutableStateOf("") }
                        var addMoreUnit by remember { mutableStateOf(ingredient.unit) }
                        var addMorePrice by remember { mutableStateOf("") }
                        var addMoreExpirationStr by remember { mutableStateOf("") }
                        var addMoreQuantityError by remember { mutableStateOf<String?>(null) }
                        var addMorePriceError by remember { mutableStateOf<String?>(null) }

                        // Delete guard message
                        var deleteBlockedMessage by remember { mutableStateOf<String?>(null) }

                        // Derived display quantity (total in current display unit)
                        val displayQty =
                            remember(totalQuantityGrams, ingredient.density, displayUnit) {
                                viewModel.convertFromGrams(
                                    totalQuantityGrams,
                                    displayUnit,
                                    ingredient.density
                                )
                            }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(2.dp, gold),
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

                                // -----------------------------------------------------------
                                // Header row: name + quantity + live unit switcher
                                // -----------------------------------------------------------
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

                                // -----------------------------------------------------------
                                // Expanded content
                                // -----------------------------------------------------------
                                if (expanded) {
                                    when {

                                        // -------------------------------------------------------
                                        // ADD MORE: create a new batch
                                        // -------------------------------------------------------
                                        isAddingMore -> {
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Quantity + unit row
                                            val addMoreUnitOptions = if (ingredient.unit == "unit") {
                                                listOf("unit")
                                            } else {
                                                listOf("g", "kg", "oz", "lb", "ml", "gal", "cup", "tbsp", "tsp", "floz")
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
                                                                        // Keep display unit in sync
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

                                            OutlinedTextField(
                                                value = addMoreExpirationStr,
                                                onValueChange = { addMoreExpirationStr = it },
                                                label = { Text("Expiration Date (YYYY-MM-DD, optional)") },
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
                                                        addMoreExpirationStr = ""
                                                        addMoreQuantityError = null
                                                        addMorePriceError = null
                                                        isAddingMore = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
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
                                                            // Convert from whatever unit the user picked into grams
                                                            val gramsToAdd = viewModel.convertToGrams(
                                                                quantityValue!!,
                                                                addMoreUnit,
                                                                ingredient.density
                                                            )
                                                            val currentDate = (System.currentTimeMillis() / 1000).toInt()
                                                            val expirationDate = parseDateToUnix(addMoreExpirationStr)

                                                            viewModel.addBatch(
                                                                ingredientId = ingredient.id,
                                                                quantity = gramsToAdd,
                                                                price = priceValue!!,
                                                                expirationDate = expirationDate,
                                                                dateAdded = currentDate
                                                            )

                                                            addMoreQuantity = ""
                                                            addMoreUnit = ingredient.unit
                                                            addMorePrice = ""
                                                            addMoreExpirationStr = ""
                                                            isAddingMore = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Confirm") }
                                            }
                                        }

                                        // -------------------------------------------------------
                                        // EDIT: modify ingredient name; tap a batch to edit it
                                        // -------------------------------------------------------
                                        isEditing -> {
                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Name field only — no unit dropdown
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

                                            // Batch list — tappable rows
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
                                                        // Inline batch edit form
                                                        val batchEditUnitOptions = if (ingredient.unit == "unit") {
                                                            listOf("unit")
                                                        } else {
                                                            listOf("g", "kg", "oz", "lb", "ml", "gal", "cup", "tbsp", "tsp", "floz")
                                                        }
                                                        var batchEditUnitDropdownExpanded by remember { mutableStateOf(false) }

                                                        Surface(
                                                            shape = RoundedCornerShape(6.dp),
                                                            color = Color.White,
                                                            border = BorderStroke(1.dp, gold),
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

                                                                OutlinedTextField(
                                                                    value = batchEditExpirationStr,
                                                                    onValueChange = { batchEditExpirationStr = it },
                                                                    label = { Text("Expiration Date (YYYY-MM-DD, optional)") },
                                                                    modifier = Modifier.fillMaxWidth()
                                                                )

                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    // Cancel batch edit
                                                                    Button(
                                                                        onClick = {
                                                                            editingBatchId = null
                                                                            batchEditQuantityError = null
                                                                            batchEditPriceError = null
                                                                        },
                                                                        colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                                        modifier = Modifier.weight(1f)
                                                                    ) { Text("Cancel") }

                                                                    // Save batch changes
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
                                                                                val newExpiration = parseDateToUnix(batchEditExpirationStr)
                                                                                viewModel.updateBatch(
                                                                                    batch.copy(
                                                                                        quantity = newGrams,
                                                                                        price = pVal!!,
                                                                                        expirationDate = newExpiration
                                                                                    )
                                                                                )
                                                                                editingBatchId = null
                                                                            }
                                                                        },
                                                                        colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                                        modifier = Modifier.weight(1f)
                                                                    ) { Text("Save") }

                                                                    // Delete this batch
                                                                    Button(
                                                                        onClick = {
                                                                            viewModel.deleteBatch(batch)
                                                                            editingBatchId = null
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
                                                        // Tappable batch summary row
                                                        val addedText = "Added ${sdf.format(Date(batch.dateAdded * 1000L))}"
                                                        val expirationText = batch.expirationDate?.let {
                                                            "  ·  Expires ${sdf.format(Date(it * 1000L))}"
                                                        } ?: ""

                                                        Surface(
                                                            shape = RoundedCornerShape(4.dp),
                                                            color = Color(0xFFF5F5F5),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 3.dp)
                                                                .clickable {
                                                                    // Populate edit fields from this batch
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
                                                                    batchEditExpirationStr = unixToDateStr(batch.expirationDate)
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
                                                                    "$addedText$expirationText",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = Color.Gray
                                                                )
                                                            }
                                                        }
                                                        HorizontalDivider(color = Color(0xFFEEEEEE))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Confirm name / Cancel / Delete ingredient
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        isEditing = false
                                                        editingBatchId = null
                                                        deleteBlockedMessage = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
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
                                                            editingBatchId = null
                                                            deleteBlockedMessage = null
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Confirm") }

                                                // Delete — blocked if ingredient is used by a recipe
                                                Button(
                                                    onClick = {
                                                        scope.launch {
                                                            val inUse = viewModel.isUsedByRecipe(ingredient.id)
                                                            if (inUse) {
                                                                deleteBlockedMessage = "In use by a recipe"
                                                            } else {
                                                                viewModel.deleteIngredient(ingredient)
                                                                isEditing = false
                                                                editingBatchId = null
                                                                deleteBlockedMessage = null
                                                            }
                                                        }
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

                                            // Show blocked message below the button row if needed
                                            deleteBlockedMessage?.let { msg ->
                                                Text(
                                                    text = msg,
                                                    color = Color.Red,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(top = 4.dp)
                                                )
                                            }
                                        }

                                        // -------------------------------------------------------
                                        // DEFAULT: show batch list (read-only) + action buttons
                                        // -------------------------------------------------------
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
                                                    val expirationText = batch.expirationDate?.let {
                                                        "  ·  Expires ${sdf.format(Date(it * 1000L))}"
                                                    } ?: ""

                                                    // Read-only row — no delete button
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
                                                                "$addedText$expirationText",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color.Gray
                                                            )
                                                        }
                                                    }
                                                    HorizontalDivider(color = Color(0xFFEEEEEE))
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(12.dp))

                                            // Action buttons: Add More | Edit | Reset
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        isAddingMore = true
                                                        isEditing = false
                                                        addMoreQuantity = ""
                                                        addMoreUnit = ingredient.unit
                                                        addMorePrice = ""
                                                        addMoreExpirationStr = ""
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
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
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Edit") }

                                                Button(
                                                    onClick = {
                                                        viewModel.resetIngredient(ingredient.id)
                                                    },
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

            // -----------------------------------------------------------------------
            // open Add New dialog
            // -----------------------------------------------------------------------
            Button(
                onClick = { showAddNew = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(35.dp)
                    .size(64.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = gold,
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text("+", fontSize = 32.sp)
                }
            }

            // -----------------------------------------------------------------------
            // Add New dialog — creates ingredient definition + first batch
            // -----------------------------------------------------------------------
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
                        newExpirationStr = ""
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

                                val expirationDate = parseDateToUnix(newExpirationStr)

                                viewModel.addIngredient(
                                    name = trimmedName,
                                    quantity = quantityValue!!,
                                    density = densityValue,
                                    unit = newUnit,
                                    price = priceValue!!,
                                    dateEntered = currentDate,
                                    expirationDate = expirationDate
                                )

                                newName = ""; newQuantity = ""; newUnit = "g"; newPrice = ""
                                newExpirationStr = ""
                                showAddNew = false
                            }
                        }) { Text("Add") }
                    },
                    dismissButton = {
                        Button(onClick = {
                            showAddNew = false
                            newName = ""; newQuantity = ""; newUnit = "g"; newPrice = ""
                            newExpirationStr = ""
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

                            OutlinedTextField(
                                colors = darkTextFieldColors(),
                                value = newExpirationStr,
                                onValueChange = { newExpirationStr = it },
                                label = { Text("Expiration Date (YYYY-MM-DD, optional)") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                )
            }
        }
    }
}