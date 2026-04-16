package com.seniorproject.cupboardly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.seniorproject.cupboardly.ui.IngredientScreen
import com.seniorproject.cupboardly.ui.RecipeScreen
import com.seniorproject.cupboardly.ui.theme.CupboardlyTheme
import com.google.firebase.FirebaseApp
import com.seniorproject.cupboardly.ui.SettingsScreen


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        setContent {
            CupboardlyTheme {
                var currentScreen by remember { mutableStateOf("ingredients") }

                when (currentScreen) {
                    "ingredients" -> IngredientScreen(
                        onGoToRecipes = { currentScreen = "recipes" },
                        onGoToSettings = { currentScreen = "settings" }
                    )

                    "recipes" -> RecipeScreen(
                        onGoToIngredients = { currentScreen = "ingredients" },
                        onGoToSettings = { currentScreen = "settings" }
                    )

                    "settings" -> SettingsScreen(
                        onGoToIngredients = { currentScreen = "ingredients" },
                        onGoToRecipes =  { currentScreen = "recipes" },
                    )
                }
            }
        }
    }
}


