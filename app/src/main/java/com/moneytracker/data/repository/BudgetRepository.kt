/**
 * Repository wrapping [BudgetDao]. Exposes the singleton budget as a
 * `Flow<Budget>` so the dashboard reacts to changes (Settings screen
 * saving a new monthly budget updates the dashboard immediately).
 */
package com.moneytracker.data.repository

import com.moneytracker.data.local.dao.BudgetDao
import com.moneytracker.data.mapper.toBudget
import com.moneytracker.data.mapper.toEntity
import com.moneytracker.domain.model.Budget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BudgetRepository @Inject constructor(
    private val budgetDao: BudgetDao
) {

    fun getBudget(): Flow<Budget> =
        budgetDao.getBudget().map { it?.toBudget() ?: Budget() }

    suspend fun saveBudget(budget: Budget) {
        budgetDao.insert(budget.toEntity())
    }

    suspend fun reset() {
        budgetDao.deleteAll()
    }
}
