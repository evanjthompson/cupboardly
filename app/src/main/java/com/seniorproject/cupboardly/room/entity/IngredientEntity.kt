package com.seniorproject.cupboardly.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredient_table")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val density: Double,           // density in grams per ml
    val unit: String
)
