package com.moneytracker.data.repository

import com.moneytracker.data.local.dao.ExpenseDao
import com.moneytracker.data.mapper.toEntity
import com.moneytracker.data.mapper.toExpense
import com.moneytracker.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val expenseDao: ExpenseDao
) {

    fun getAllExpenses(): Flow<List<Expense>> =
        expenseDao.getAll().map { list -> list.map { it.toExpense() } }

    fun getExpensesByDate(date: LocalDate): Flow<List<Expense>> =
        expenseDao.getByDate(date.toString()).map { list -> list.map { it.toExpense() } }

    fun getExpensesByMonth(month: String): Flow<List<Expense>> =
        expenseDao.getByMonth(month).map { list -> list.map { it.toExpense() } }

    fun getExpensesByCategory(categoryId: Int): Flow<List<Expense>> =
        expenseDao.getByCategory(categoryId).map { list -> list.map { it.toExpense() } }

    fun searchExpenses(query: String): Flow<List<Expense>> =
        expenseDao.search(query).map { list -> list.map { it.toExpense() } }

    fun getTotalByDate(date: LocalDate): Flow<Double> =
        expenseDao.getTotalByDate(date.toString()).map { it ?: 0.0 }

    fun getTotalByMonth(month: String): Flow<Double> =
        expenseDao.getTotalByMonth(month).map { it ?: 0.0 }

    fun getHighestSpendingDay(month: String): Flow<Pair<String, Double>?> =
        expenseDao.getHighestSpendingDay(month).map { it?.let { day -> day.date to day.total } }

    fun getHighestSpendingCategory(month: String): Flow<Pair<Int, Double>?> =
        expenseDao.getHighestSpendingCategory(month).map { it?.let { cat -> cat.categoryId to cat.total } }

    suspend fun getExpenseById(id: Long): Expense? =
        expenseDao.getExpenseById(id).firstOrNull()?.toExpense()

    suspend fun insertExpense(expense: Expense): Long =
        expenseDao.insert(expense.toEntity())

    suspend fun updateExpense(expense: Expense) {
        expenseDao.update(expense.toEntity())
    }

    suspend fun deleteExpense(expense: Expense) {
        expenseDao.delete(expense.toEntity())
    }
}
