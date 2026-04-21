package com.seniorproject.cupboardly.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TopNavTabs(
    currentScreen: String,
    onGoToIngredients: () -> Unit,
    onGoToRecipes: () -> Unit,
    ingredientGold: Color,
    recipeBlue: Color
) {
    val isIngredients = currentScreen == "ingredients"
    val isRecipes = currentScreen == "recipes"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(
                color = Color(0x22000000),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ---------------- INGREDIENTS ----------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isIngredients) ingredientGold else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onGoToIngredients() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Ingredients",
                    color = if (isIngredients) Color.White else Color.Gray,
                    fontWeight = if (isIngredients) FontWeight.Bold else FontWeight.Normal
                )
            }

            // ---------------- RECIPES ----------------
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (isRecipes) recipeBlue else Color.Transparent,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onGoToRecipes() }
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Recipes",
                    color = if (isRecipes) Color.White else Color.Gray,
                    fontWeight = if (isRecipes) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}