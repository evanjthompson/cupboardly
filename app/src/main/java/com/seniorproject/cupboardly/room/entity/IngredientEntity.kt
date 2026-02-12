package com.seniorproject.cupboardly.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredient_table")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val quantity: Double,          // current inventory
    val unit: String,
    val price: Double,             // total price of current quantity
    val pricePerUnit: Double,      // price per single unit
    val dateEntered: Int,
    val dateLastUpdated: Int
)
