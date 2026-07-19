package com.moneytracker.data.repository

import com.moneytracker.data.local.dao.IncomeDao
import com.moneytracker.data.mapper.toEntity
import com.moneytracker.data.mapper.toIncome
import com.moneytracker.domain.model.Income
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomeRepository @Inject constructor(
    private val incomeDao: IncomeDao
) {

    fun getAllIncome(): Flow<List<Income>> =
        incomeDao.getAll().map { list -> list.map { it.toIncome() } }

    fun getIncomeByMonth(month: String): Flow<List<Income>> =
        incomeDao.getByMonth(month).map { list -> list.map { it.toIncome() } }

    fun getTotalIncomeByMonth(month: String): Flow<Double> =
        incomeDao.getTotalByMonth(month).map { it ?: 0.0 }

    suspend fun getIncomeById(id: Long): Income? =
        incomeDao.getById(id)?.toIncome()

    suspend fun insertIncome(income: Income): Long =
        incomeDao.insert(income.toEntity())

    suspend fun updateIncome(income: Income) {
        incomeDao.update(income.toEntity())
    }

    suspend fun deleteIncome(income: Income) {
        incomeDao.delete(income.toEntity())
    }
}
