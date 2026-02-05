package com.seniorproject.cupboardly

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.seniorproject.cupboardly.ui.IngredientScreen
import com.seniorproject.cupboardly.ui.theme.CupboardlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CupboardlyTheme {
                IngredientScreen()
            }
        }
    }
}
