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
import androidx.compose.ui.text.font.FontWeight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientScreen(
    viewModel: IngredientViewModel = viewModel(),
    onGoToRecipes: () -> Unit
) {

    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val ingredients by viewModel.ingredients.collectAsState(initial = emptyList())

    var showAddNew by remember { mutableStateOf(false) }

    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    val gold = Color(197,145,39)
    val darkBlue = Color(11,186,224)
    val headerPink1 = Color(255,150,174)
    val headerBlue2 = Color(105,150,156)

    var nameError by remember { mutableStateOf<String?>(null) }
    var quantityError by remember { mutableStateOf<String?>(null) }
    var priceError by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {

        Box(modifier = Modifier.fillMaxSize()) {
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
                            containerColor = Color.DarkGray,
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
                    items(
                        items = ingredients,
                        key = { it.id }
                    ) { ingredient ->

                        var expanded by remember { mutableStateOf(false) }
                        var isEditing by remember { mutableStateOf(false) }
                        var isAddingMore by remember { mutableStateOf(false) }

                        var editNameError by remember { mutableStateOf<String?>(null) }
                        var editQuantityError by remember { mutableStateOf<String?>(null) }
                        var editPriceError by remember { mutableStateOf<String?>(null) }

                        var addMoreQuantityError by remember { mutableStateOf<String?>(null) }
                        var addMorePriceError by remember { mutableStateOf<String?>(null) }

                        var editName by remember { mutableStateOf(ingredient.name) }
                        var editQuantity by remember { mutableStateOf(ingredient.quantity.toString()) }
                        var editUnit by remember { mutableStateOf(ingredient.unit) }
                        var editPrice by remember { mutableStateOf(ingredient.price.toString()) }

                        var addMoreQuantity by remember { mutableStateOf("") }
                        var addMorePrice by remember { mutableStateOf("") }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(2.dp, gold),
                            color = Color.White,
                            contentColor = Color.Black,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .animateContentSize()
                                .clickable { expanded = !expanded }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {

                                if (!isEditing && !isAddingMore) {
                                    Text("${ingredient.name} ${ingredient.quantity} ${ingredient.unit}")
                                } else {
                                    Text(ingredient.name)
                                }

                                if (expanded) {
                                    when {
                                        isAddingMore -> {

                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedTextField(
                                                value = addMoreQuantity,
                                                onValueChange = { addMoreQuantity = it; addMoreQuantityError = null },
                                                label = { Text("Add Quantity") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                isError = addMoreQuantityError != null,
                                                supportingText = {
                                                    addMoreQuantityError?.let {
                                                        Text(it, color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            OutlinedTextField(
                                                value = addMorePrice,
                                                onValueChange = { addMorePrice = it; addMorePriceError = null },
                                                label = { Text("Price of Additional Quantity") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                isError = addMorePriceError != null,
                                                supportingText = {
                                                    addMorePriceError?.let {
                                                        Text(it, color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        addMoreQuantity = ""
                                                        addMorePrice = ""
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
                                                        if (priceValue == null || priceValue <= 0) {
                                                            addMorePriceError = "Enter new price"
                                                            valid = false
                                                        }

                                                        if (valid) {
                                                            val currentDate = (System.currentTimeMillis() / 1000).toInt()
                                                            val newQuantity = ingredient.quantity + quantityValue!!
                                                            val newPrice = ingredient.price + priceValue!!
                                                            val newPricePerUnit = newPrice / newQuantity

                                                            val updatedIngredient = ingredient.copy(
                                                                quantity = newQuantity,
                                                                price = newPrice,
                                                                pricePerUnit = newPricePerUnit,
                                                                allTimeQuantity = ingredient.allTimeQuantity + quantityValue,
                                                                allTimePrice = ingredient.allTimePrice + priceValue,
                                                                dateLastUpdated = currentDate
                                                            )

                                                            viewModel.updateIngredient(updatedIngredient)

                                                            addMoreQuantity = ""
                                                            addMorePrice = ""
                                                            isAddingMore = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Confirm") }
                                            }
                                        }

                                        isEditing -> {

                                            Spacer(modifier = Modifier.height(8.dp))

                                            OutlinedTextField(
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

                                            OutlinedTextField(
                                                value = editQuantity,
                                                onValueChange = { editQuantity = it; editQuantityError = null },
                                                label = { Text("Quantity (currently ${ingredient.quantity})") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                isError = editQuantityError != null,
                                                supportingText = {
                                                    editQuantityError?.let {
                                                        Text(it, color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            OutlinedTextField(
                                                value = editUnit,
                                                onValueChange = { editUnit = it },
                                                label = { Text("Unit (currently ${ingredient.unit})") },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            OutlinedTextField(
                                                value = editPrice,
                                                onValueChange = { editPrice = it; editPriceError = null },
                                                label = { Text("Price (currently ${ingredient.price})") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                isError = editPriceError != null,
                                                supportingText = {
                                                    editPriceError?.let {
                                                        Text(it, color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = { isEditing = false },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Cancel") }

                                                Button(
                                                    onClick = {
                                                        val trimmedName = editName.trim()
                                                        val quantityValue = editQuantity.toDoubleOrNull()
                                                        val priceValue = editPrice.toDoubleOrNull()
                                                        var valid = true

                                                        if (trimmedName.isBlank()) {
                                                            editNameError = "Enter a name"
                                                            valid = false
                                                        }
                                                        if (quantityValue == null || quantityValue <= 0) {
                                                            editQuantityError = "Quantity > 0"
                                                            valid = false
                                                        }
                                                        if (priceValue == null || priceValue < 0) {
                                                            editPriceError = "Price >= 0"
                                                            valid = false
                                                        }

                                                        if (ingredients.any {
                                                                it.id != ingredient.id &&
                                                                        it.name.equals(trimmedName, ignoreCase = true)
                                                            }
                                                        ) {
                                                            editNameError = "Name already exists"
                                                            valid = false
                                                        }

                                                        if (valid) {
                                                            val currentDate = (System.currentTimeMillis() / 1000).toInt()
                                                            val oldQuantity = ingredient.quantity
                                                            val oldPrice = ingredient.price

                                                            val deltaQuantity = quantityValue!! - oldQuantity
                                                            val deltaPrice = priceValue!! - oldPrice

                                                            val updatedIngredient = ingredient.copy(
                                                                name = trimmedName,
                                                                quantity = quantityValue,
                                                                unit = editUnit,
                                                                price = priceValue,
                                                                pricePerUnit = priceValue / quantityValue,
                                                                allTimeQuantity = ingredient.allTimeQuantity + deltaQuantity,
                                                                allTimePrice = ingredient.allTimePrice + deltaPrice,
                                                                dateLastUpdated = currentDate
                                                            )

                                                            viewModel.updateIngredient(updatedIngredient)
                                                            isEditing = false
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Confirm") }

                                                Button(
                                                    onClick = {
                                                        viewModel.deleteIngredient(ingredient)
                                                        isEditing = false
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                    modifier = Modifier.weight(0.75f)
                                                ) { Text("Delete") }
                                            }
                                        }

                                        else -> {

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Total Price: $${ingredient.price}")
                                            Text("Price per Unit: $${"%.2f".format(ingredient.pricePerUnit)} per ${ingredient.unit}")
                                            Text("Date Entered: ${sdf.format(Date(ingredient.dateEntered * 1000L))}")
                                            Text("Date Last Updated: ${sdf.format(Date(ingredient.dateLastUpdated * 1000L))}")
                                            Spacer(modifier = Modifier.height(5.dp))
                                            Text("Total Tracked All Time: ${ingredient.allTimeQuantity}")
                                            Text("Total Cost All Time: $${ingredient.allTimePrice}")

                                            Spacer(modifier = Modifier.height(12.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        isAddingMore = true
                                                        isEditing = false
                                                        addMoreQuantity = ""
                                                        addMorePrice = ""
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Add More") }

                                                Button(
                                                    onClick = {
                                                        isEditing = true
                                                        isAddingMore = false
                                                        editName = ingredient.name
                                                        editQuantity = ingredient.quantity.toString()
                                                        editUnit = ingredient.unit
                                                        editPrice = ingredient.price.toString()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = gold),
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Edit") }

                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { showAddNew = true },
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

            if (showAddNew) {

                var expanded by remember { mutableStateOf(false) }

                val matchingIngredients = ingredients.filter {
                    it.name.contains(name.trim(), ignoreCase = true) && name.isNotBlank()
                }

                AlertDialog(
                    onDismissRequest = { showAddNew = false },

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

                            if (priceValue == null || priceValue <= 0.0) {
                                priceError = "Enter the new purchase price"
                                isValid = false
                            }

                            if (!isValid) return@Button

                            val currentDate =
                                (System.currentTimeMillis() / 1000).toInt()

                            val existingIngredient = ingredients.find {
                                it.name.equals(trimmedName, ignoreCase = true)
                            }

                            if (existingIngredient != null) {

                                val newQuantity = existingIngredient.quantity + quantityValue!!
                                val newTotalPrice = existingIngredient.price + priceValue!!
                                val newPricePerUnit = newTotalPrice / newQuantity

                                val updatedIngredient = existingIngredient.copy(
                                    quantity = newQuantity,
                                    price = newTotalPrice,
                                    pricePerUnit = newPricePerUnit,
                                    allTimeQuantity = existingIngredient.allTimeQuantity + quantityValue,
                                    allTimePrice = existingIngredient.allTimePrice + priceValue,
                                    dateLastUpdated = currentDate
                                )

                                viewModel.updateIngredient(updatedIngredient)

                            } else {

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
                            }

                            name = ""
                            quantity = ""
                            unit = ""
                            price = ""
                            showAddNew = false

                        }) {
                            Text("Add")
                        }
                    },

                    dismissButton = {
                        Button(onClick = { showAddNew = false }) {
                            Text("Cancel")
                        }
                    },

                    title = { Text("Add Ingredient") },

                    text = {
                        Column {

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
                                                price = ""
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
}
}
