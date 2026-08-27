package com.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.moneytracker.data.local.entity.ExpenseEntity
import com.moneytracker.data.local.entity.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ExpenseWithCategory>>

    /**
     * Returns only the most recent N expenses (with their categories joined).
     * Used by the dashboard's "recent transactions" list so we don't materialise
     * the whole history when we only render 10 rows.
     */
    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE date = :date ORDER BY timestamp DESC")
    fun getByDate(date: String): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE strftime('%Y-%m', date) = :month ORDER BY timestamp DESC")
    fun getByMonth(month: String): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE categoryId = :categoryId ORDER BY timestamp DESC")
    fun getByCategory(categoryId: Int): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE description LIKE '%' || :query || '%' OR categoryId IN (SELECT id FROM categories WHERE name LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun search(query: String): Flow<List<ExpenseWithCategory>>

    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ExpenseEntity?

    @Transaction
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1")
    fun getExpenseById(id: Long): Flow<ExpenseWithCategory?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()

    @Query("SELECT SUM(amount) FROM expenses WHERE date = :date")
    fun getTotalByDate(date: String): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE strftime('%Y-%m', date) = :month")
    fun getTotalByMonth(month: String): Flow<Double?>

    @Query("SELECT date, SUM(amount) as total FROM expenses WHERE strftime('%Y-%m', date) = :month GROUP BY date ORDER BY total DESC LIMIT 1")
    fun getHighestSpendingDay(month: String): Flow<HighestSpendingDay?>

    @Query("SELECT categoryId, SUM(amount) as total FROM expenses WHERE strftime('%Y-%m', date) = :month GROUP BY categoryId ORDER BY total DESC LIMIT 1")
    fun getHighestSpendingCategory(month: String): Flow<HighestSpendingCategory?>

    @Transaction
    @Query("SELECT * FROM expenses WHERE recurrenceRule IS NOT NULL ORDER BY timestamp DESC")
    suspend fun getRecurring(): List<ExpenseWithCategory>

    @Transaction
    @Query("SELECT * FROM expenses WHERE recurrenceGroupId = :groupId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestInSeries(groupId: String): ExpenseWithCategory?
}

data class HighestSpendingDay(
    val date: String,
    val total: Double
)

data class HighestSpendingCategory(
    val categoryId: Int,
    val total: Double
)
