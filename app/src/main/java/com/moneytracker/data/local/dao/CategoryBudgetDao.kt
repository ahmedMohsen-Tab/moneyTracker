package com.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneytracker.data.local.entity.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {

    @Query("SELECT * FROM category_budgets")
    fun getAll(): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT * FROM category_budgets WHERE categoryId = :categoryId LIMIT 1")
    suspend fun getByCategory(categoryId: Int): CategoryBudgetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets WHERE categoryId = :categoryId")
    suspend fun deleteByCategory(categoryId: Int)

    @Query("DELETE FROM category_budgets")
    suspend fun deleteAll()
}