package com.seniorproject.cupboardly.classes

val VOLUME_TO_ML = mapOf(
    "ml" to 1.0, "cup" to 236.588, "tbsp" to 14.7868, "tsp" to 4.92892, "floz" to 29.5735, "gal" to 3785.408
)
val MASS_TO_GRAM = mapOf(
    "g" to 1.0, "kg" to 1000.0, "oz" to 28.3495, "lb" to 453.592
)

fun convertToGrams(amount: Double, unit: String, density: Double? = null): Double {
    if (unit == "unit") return amount // no conversion
    return when {
        MASS_TO_GRAM.containsKey(unit) -> amount * MASS_TO_GRAM[unit]!!
        VOLUME_TO_ML.containsKey(unit) -> {
            val ml = amount * VOLUME_TO_ML[unit]!!
            ml * (density ?: 1.0)
        }
        else -> amount
    }
}

fun convertFromGrams(grams: Double, unit: String, density: Double? = null): Double {
    if (unit == "unit") return grams // no conversion
    return when {
        MASS_TO_GRAM.containsKey(unit) -> grams / MASS_TO_GRAM[unit]!!
        VOLUME_TO_ML.containsKey(unit) -> {
            val ml = grams / (density ?: 1.0)
            ml / VOLUME_TO_ML[unit]!!
        }
        else -> grams
    }
}