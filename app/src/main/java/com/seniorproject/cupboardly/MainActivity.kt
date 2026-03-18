package com.seniorproject.cupboardly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.seniorproject.cupboardly.ui.IngredientScreen
import com.seniorproject.cupboardly.ui.RecipeScreen
import com.seniorproject.cupboardly.ui.theme.CupboardlyTheme
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.ai
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            CupboardlyTheme {
                var currentScreen by remember { mutableStateOf("ingredients") }

                when (currentScreen) {
                    "ingredients" -> IngredientScreen(
                        onGoToRecipes = { currentScreen = "recipes" }
                    )
                    "recipes" -> RecipeScreen(
                        onGoToIngredients = { currentScreen = "ingredients" }
                    )
                }
            }
        }
    }
}


