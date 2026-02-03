package com.seniorproject.cupboardly.room.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ingredient_table")
data class IngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String

)