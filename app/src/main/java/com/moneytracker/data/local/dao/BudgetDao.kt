/**
 * Room DAO for the singleton [com.moneytracker.data.local.entity.BudgetEntity]
 * row. All methods are `suspend` or `Flow`; the wrapper
 * [com.moneytracker.data.repository.BudgetRepository] handles dispatching.
 *
 * `getBudget()` returns a `Flow<BudgetEntity?>` rather than `Flow<BudgetEntity>`
 * to keep the table tolerant of the empty state before the seed callback
 * has run on first install.
 */
package com.moneytracker.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.moneytracker.data.local.entity.BudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget LIMIT 1")
    fun getBudget(): Flow<BudgetEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(budget: BudgetEntity)

    @Query("DELETE FROM budget")
    suspend fun deleteAll()
}
