package com.seniorproject.cupboardly.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredient_table")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val pricePerUnit: Double,
    val dateEntered: Int,
    val dateLastUpdated: Int,
    val amountUsedRecently: Double
)