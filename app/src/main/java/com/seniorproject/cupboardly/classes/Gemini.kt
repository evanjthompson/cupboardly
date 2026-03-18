package com.seniorproject.cupboardly.ai

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai

object GeminiService {

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

suspend fun askGeminiForDensity(ingredient: String): Float? {

    val prompt = """
        Return ONLY a single floating point number.
        Do not include units or extra text.

        What is the average density of $ingredient in grams per cup?
    """.trimIndent()

    val response = GeminiService.askGemini(prompt)

    Log.d("Gemini", "Parsed density: $response")

    return response?.toFloatOrNull()
}