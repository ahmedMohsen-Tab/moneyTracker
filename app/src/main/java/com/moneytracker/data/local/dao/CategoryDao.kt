/**
 * Room DAO for the seeded [CategoryEntity] list.
 *
 * Categories are seeded by [com.moneytracker.data.local.AppDatabase.Callback.seedCategories]
 * on first DB creation. The seed never changes after install unless the
 * version bumps and a migration explicitly inserts the new rows.
 *
 * `getMaxId()` is used when the user creates a brand-new category from the
 * Add Expense screen so we can hand out an id > 10 (the seeded range).
 */
package com.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneytracker.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY id")
    fun getAll(): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): CategoryEntity?

    @Query("SELECT COALESCE(MAX(id), 0) FROM categories")
    suspend fun getMaxId(): Int
}
