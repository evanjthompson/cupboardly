package com.seniorproject.cupboardly.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seniorproject.cupboardly.room.entity.IngredientEntity
import com.seniorproject.cupboardly.viewModels.IngredientViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import com.seniorproject.cupboardly.R
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ExposedDropdownMenuAnchorType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientScreen(
    viewModel: IngredientViewModel = viewModel(),
    onGoToRecipes: () -> Unit
) {

    val ingredients by viewModel.ingredients.collectAsState(initial = emptyList())

    var showDialog by remember { mutableStateOf(false) }

    // Input states
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    // Custom color vars for UI theming
    val gold = Color (red = 197, green = 145, blue = 39)
    val darkBlue = Color (red = 11, green = 186, blue =224)
    //val lightOrange = Color (red = 255, green = 233, blue = 206)
    val headerPink1 = Color (red = 255, green = 150, blue = 174)
    //val headerPink2 = Color (red = 210, green = 106, blue = 131)
    //val headerBlue1 = Color (red = 140, green = 198, blue = 209)
    val headerBlue2 = Color (red = 105, green = 150, blue = 156)
    // Validation error states
    var nameError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.stripeingredientbg ),
            contentDescription = "Ingredient Background",
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
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = headerPink1,
                        contentColor = Color.White
                    )
                ) {
                    Text("Ingredients")
                }

                Button(
                    onClick = onGoToRecipes,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = headerBlue2,
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

            LazyColumn {
                items(ingredients) { ingredient: IngredientEntity ->
                    Button(
                        border = BorderStroke(2.dp, gold),
                        onClick = { viewModel.deleteIngredient(ingredient) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("${ingredient.name} ${ingredient.quantity} ${ingredient.unit}")
                    }
                }
            }
        }

        // Floating Add Button
        Button(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(35.dp)
                .size(64.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = darkBlue,
                contentColor = Color.White
            )
        ) {
            Text("+", fontSize = 32.sp)
        }

        if (showDialog) {

            var expanded by remember { mutableStateOf(false) }

            val matchingIngredients = ingredients.filter {
                it.name.contains(name.trim(), ignoreCase = true) &&
                        name.isNotBlank()
            }

            AlertDialog(
                onDismissRequest = { showDialog = false },

                confirmButton = {
                    Button(onClick = {

                        nameError = null
                        quantityError = null
                        priceError = null

                        val trimmedName = name.trim()
                        val quantityValue = quantity.toDoubleOrNull()
                        val priceValue = price.toDoubleOrNull()

                        var isValid = true

                        if (trimmedName.isBlank()) {
                            nameError = "Please enter a Name"
                            isValid = false
                        }

                        if (quantityValue == null || quantityValue <= 0.0) {
                            quantityError = "Quantity cannot be 0"
                            isValid = false
                        }

                        if (priceValue == null || priceValue < 0.0) {
                            priceError = "Price cannot be 0"
                            isValid = false
                        }

                        val exists = ingredients.any {
                            it.name.equals(trimmedName, ignoreCase = true)
                        }

                        if (exists) {
                            nameError = "Ingredient already exists"
                            isValid = false
                        }

                        if (!isValid) return@Button

                        val currentDate =
                            (System.currentTimeMillis() / 1000).toInt()

                        val pricePerUnitValue =
                            priceValue!! / quantityValue!!

                        viewModel.addIngredient(
                            name = trimmedName,
                            quantity = quantityValue,
                            unit = unit,
                            price = priceValue,
                            pricePerUnit = pricePerUnitValue,
                            dateEntered = currentDate,
                            dateLastUpdated = currentDate
                        )

                        name = ""
                        quantity = ""
                        unit = ""
                        price = ""
                        showDialog = false

                    }) {
                        Text("Add")
                    }
                },

                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                },

                title = { Text("Add Ingredient") },

                text = {
                    Column {

                        // NAME FIELD WITH UPDATED MENU ANCHOR
                        ExposedDropdownMenuBox(
                            expanded = expanded && matchingIngredients.isNotEmpty(),
                            onExpandedChange = { expanded = it }
                        ) {

                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    expanded = true
                                },
                                label = { Text("Ingredient Name") },
                                isError = nameError != null,
                                modifier = Modifier
                                    .menuAnchor(
                                        type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                                        enabled = true
                                    )
                                    .fillMaxWidth(),
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                },
                                supportingText = {
                                    nameError?.let {
                                        Text(
                                            it,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            )

                            ExposedDropdownMenu(
                                expanded = expanded && matchingIngredients.isNotEmpty(),
                                onDismissRequest = { expanded = false }
                            ) {
                                matchingIngredients.forEach { ingredient ->
                                    DropdownMenuItem(
                                        text = { Text(ingredient.name) },
                                        onClick = {
                                            name = ingredient.name
                                            unit = ingredient.unit
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { quantity = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            isError = quantityError != null,
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                quantityError?.let {
                                    Text(
                                        it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = unit,
                            onValueChange = { unit = it },
                            label = { Text("Unit of Measurement") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = price,
                            onValueChange = { price = it },
                            label = { Text("Price") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            isError = priceError != null,
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = {
                                priceError?.let {
                                    Text(
                                        it,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        )
                    }
                }
            )
        }
    }
}