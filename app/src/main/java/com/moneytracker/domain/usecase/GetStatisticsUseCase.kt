/**
 * Aggregations for the Statistics screen: per-category totals, daily
 * spending for the chosen month, and totals by wallet.
 *
 * Uses month-scoped queries so the screen stays cheap regardless of how
 * much history the user has.
 */
package com.moneytracker.domain.usecase

import com.moneytracker.data.repository.ExpenseRepository
import com.moneytracker.data.repository.IncomeRepository
import com.moneytracker.domain.model.Expense
import com.moneytracker.domain.model.Income
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetStatisticsUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository
) {
    operator fun invoke(month: String): Flow<Statistics> = combine(
        combine(
            expenseRepository.getExpensesByMonth(month),
            incomeRepository.getIncomeByMonth(month),
            expenseRepository.getTotalByMonth(month)
        ) { a, b, c -> Triple(a, b, c) },
        incomeRepository.getTotalIncomeByMonth(month),
        expenseRepository.getHighestSpendingDay(month),
        expenseRepository.getHighestSpendingCategory(month)
    ) { (expenses, incomes, totalExpense), totalIncome, highestDay, highestCategory ->
        Statistics(
            expenses = expenses,
            incomes = incomes,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            remaining = totalIncome - totalExpense,
            averageDailySpending = calculateAverageDaily(expenses, month),
            highestSpendingDay = highestDay,
            highestSpendingCategory = highestCategory,
            transactionCount = expenses.size + incomes.size
        )
    }

    private fun calculateAverageDaily(expenses: List<Expense>, month: String): Double {
        if (expenses.isEmpty()) return 0.0
        val daysWithSpending = expenses.map { it.date.dayOfMonth }.distinct().size.coerceAtLeast(1)
        return expenses.sumOf { it.amount } / daysWithSpending
    }
}

data class Statistics(
    val expenses: List<Expense>,
    val incomes: List<Income>,
    val totalExpense: Double,
    val totalIncome: Double,
    val remaining: Double,
    val averageDailySpending: Double,
    val highestSpendingDay: Pair<String, Double>?,
    val highestSpendingCategory: Pair<Int, Double>?,
    val transactionCount: Int
)
