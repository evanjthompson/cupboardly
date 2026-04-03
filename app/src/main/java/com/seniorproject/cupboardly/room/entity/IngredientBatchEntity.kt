package com.seniorproject.cupboardly.room.entity

import androidx.room.*

@Entity(
    tableName = "ingredient_batch_table",
    foreignKeys = [
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("ingredientId")]
)
data class IngredientBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ingredientId: Long,
    val quantity: Double,     // grams
    val price: Double,
    val expirationDate: Int?,
    val dateAdded: Int
)