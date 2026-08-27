/**
 * Room DAO for [com.moneytracker.data.local.entity.IncomeEntity].
 *
 * No `@Transaction` joins here — incomes don't have a category, so the
 * simpler bare-entity reads are sufficient.
 *
 * Like [ExpenseDao], the aggregate queries return scalar projections to
 * keep dashboard cold-start cheap.
 */
package com.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moneytracker.data.local.entity.IncomeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomeDao {

    @Query("SELECT * FROM incomes ORDER BY timestamp DESC")
    fun getAll(): Flow<List<IncomeEntity>>

    /**
     * Returns only the most recent N incomes. Used by the dashboard's
     * "recent transactions" list so we don't materialise the whole history
     * when we only render a handful of rows.
     */
    @Query("SELECT * FROM incomes ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE date = :date ORDER BY timestamp DESC")
    fun getByDate(date: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE strftime('%Y-%m', date) = :month ORDER BY timestamp DESC")
    fun getByMonth(month: String): Flow<List<IncomeEntity>>

    @Query("SELECT * FROM incomes WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): IncomeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(income: IncomeEntity): Long

    @Update
    suspend fun update(income: IncomeEntity)

    @Delete
    suspend fun delete(income: IncomeEntity)

    @Query("DELETE FROM incomes")
    suspend fun deleteAll()

    @Query("SELECT SUM(amount) FROM incomes WHERE strftime('%Y-%m', date) = :month")
    fun getTotalByMonth(month: String): Flow<Double?>

    @Query("SELECT * FROM incomes WHERE recurrenceRule IS NOT NULL ORDER BY timestamp DESC")
    suspend fun getRecurring(): List<IncomeEntity>

    @Query("SELECT * FROM incomes WHERE recurrenceGroupId = :groupId ORDER BY date DESC LIMIT 1")
    suspend fun getLatestInSeries(groupId: String): IncomeEntity?
}
