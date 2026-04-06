package com.seniorproject.cupboardly.room.dao

import androidx.room.*
import com.seniorproject.cupboardly.room.entity.IngredientBatchEntity

@Dao
interface IngredientBatchDao {

    @Insert
    suspend fun insert(batch: IngredientBatchEntity)

    @Update
    suspend fun update(batch: IngredientBatchEntity)

    @Delete
    suspend fun delete(batch: IngredientBatchEntity)

    @Query("SELECT * FROM ingredient_batch_table WHERE ingredientId = :id")
    suspend fun getBatchesForIngredient(id: Long): List<IngredientBatchEntity>

    @Query("""
        SELECT SUM(quantity) 
        FROM ingredient_batch_table 
        WHERE ingredientId = :id
    """)
    suspend fun getTotalQuantity(id: Long): Double?
}