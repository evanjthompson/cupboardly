package com.seniorproject.cupboardly.classes

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.seniorproject.cupboardly.viewModels.IngredientWithQuantity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// set up Gemini service
object GeminiService {

    // using 2.5-flash-lite
    private val generativeModel =
        Firebase.ai.generativeModel(modelName = "gemini-2.5-flash-lite")

    suspend fun askGemini(prompt: String): String? {
        return try {
            Log.d("Gemini", "Prompt: $prompt")

            val response = generativeModel.generateContent(prompt)
            val text = response.text?.trim()

            Log.d("Gemini", "Response: $text")

            text
        } catch (e: Exception) {
            Log.e("Gemini", "Error: ${e.message}", e)
            null
        }
    }
}

// JSON parser function
private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

private fun cleanJson(raw: String): String {
    return raw
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
}

// Temp models for storing parsed data

@Serializable
data class AiRecipe(
    val name: String,
    val ingredients: List<AiIngredient>,
    val instructions: List<String>
)

@Serializable
data class AiIngredient(
    val name: String,
    val quantity: Double? = null,
    val unit: String? = null
)

// prompt Gemini for ingredient density
suspend fun askGeminiForDensity(ingredient: String): Double {

    val prompt = """
        Return ONLY a single floating point number.
        Do not include units or extra text.

        What is the average density of $ingredient in grams per milliliter?
    """.trimIndent()

    val response = GeminiService.askGemini(prompt)

    return response?.toDoubleOrNull() ?: 1.0
}

// prompt Gemini to parse raw recipe text into JSON format
suspend fun askGeminiForRecipeParse(
    text: String,
    ingredientList: List<IngredientWithQuantity>
): AiRecipe? {

    val ingredientsString = ingredientList.joinToString(", ") {
        it.ingredient.name
    }

    val prompt = """
        Parse text to JSON. Output ONLY JSON.

        Format:
        {"name":string,"ingredients":[{"name":string,"quantity":number|null,"unit":string|null}],"instructions":[string]}

        Rules:
        - Ingredient names: match closest from list; else best guess.
        - quantity: number or null.
        - unit must be one of ["unit","g","kg","oz","lb","ml","gal","cup","tbsp","tsp","floz"] or null.
        - If unit not in list, use null.
        - Use singular units only.
        - Instructions: clear steps.
        - name: human readable (choc chips → chocolate chips)
        - be sure to match reasonable ingredient names even if implicit (ex: "shortening" could be referred to as "crisco" if crisco exists in the ingredient list)
        - Include all fields.

        List: $ingredientsString
        Text: $text
    """.trimIndent()

    return try {
        val response = GeminiService.askGemini(prompt)

        Log.d("Gemini", "Raw recipe response: $response")

        if (response.isNullOrBlank()) return null

        json.decodeFromString(
            AiRecipe.serializer(),
            cleanJson(response)
        )

    } catch (e: Exception) {
        Log.e("Gemini", "Recipe parse failed", e)
        null
    }
}