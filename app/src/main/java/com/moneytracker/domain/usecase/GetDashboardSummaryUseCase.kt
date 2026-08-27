package com.moneytracker.domain.usecase

import com.moneytracker.data.repository.BudgetRepository
import com.moneytracker.data.repository.ExpenseRepository
import com.moneytracker.data.repository.IncomeRepository
import com.moneytracker.domain.model.Budget
import com.moneytracker.domain.model.CategoryBudgetUsage
import com.moneytracker.domain.model.Transaction
import com.moneytracker.domain.model.toTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

/**
 * Single source of truth for the dashboard.
 * Replaces the inline 80-line aggregation block that previously lived in
 * `DashboardViewModel.uiState`.
 */
class GetDashboardSummaryUseCase @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val budgetRepository: BudgetRepository,
    private val getCategoryBudgetUsage: GetCategoryBudgetUsageUseCase,
    private val calculateWalletBalances: CalculateWalletBalancesUseCase
) {

    data class Summary(
        val recentTransactions: List<Transaction>,
        val spentToday: Double,
        val spentThisMonth: Double,
        val totalIncome: Double,
        val remaining: Double,
        val balance: Double,
        val cashBalance: Double,
        val bankBalance: Double,
        val budget: Budget,
        val budgetUsage: Double,
        val categoryBudgetUsages: List<CategoryBudgetUsage>
    )

    operator fun invoke(
        month: YearMonth,
        today: LocalDate,
        currency: String
    ): Flow<Summary> {
        val monthString = month.toString()
        // Cold-start optimised. Previous implementation loaded the FULL expense
        // history (with a per-row @Transaction join against `categories`) just to
        // compute per-month totals in Kotlin. For an install with a year of data
        // that was thousands of rows of unnecessary work on every first frame.
        //
        // The dashboard actually only needs:
        //   • aggregate totals for the month and for today (single SUM queries),
        //   • this month's expenses + incomes to compute wallet balances, and
        //   • the most recent N transactions (regardless of month) for the list.
        //
        // The category-budget usage flow is still chained in here, but it
        // subscribes to month-filtered expenses internally — see
        // [GetCategoryBudgetUsageUseCase] — so it stays cheap.
        val recentLimit = RECENT_TRANSACTION_LIMIT
        return combine(
            // Aggregate totals — single SUM queries, no row materialisation.
            expenseRepository.getTotalByMonth(monthString),
            expenseRepository.getTotalByDate(today),
            incomeRepository.getTotalIncomeByMonth(monthString),
            // Month-scoped rows for the wallet-balance aggregation.
            expenseRepository.getExpensesByMonthString(monthString),
            incomeRepository.getIncomeByMonth(monthString),
            // Top-N rows for the recent transactions list (with category join).
            expenseRepository.getRecentExpenses(recentLimit),
            incomeRepository.getRecentIncome(recentLimit),
            // Budget + category-budget usage.
            budgetRepository.getBudget(),
            getCategoryBudgetUsage(monthString)
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val spentThisMonth = values[0] as Double
            @Suppress("UNCHECKED_CAST")
            val spentToday = values[1] as Double
            @Suppress("UNCHECKED_CAST")
            val totalIncome = values[2] as Double
            @Suppress("UNCHECKED_CAST")
            val monthExpenses = values[3] as List<com.moneytracker.domain.model.Expense>
            @Suppress("UNCHECKED_CAST")
            val monthIncomes = values[4] as List<com.moneytracker.domain.model.Income>
            @Suppress("UNCHECKED_CAST")
            val recentExpenses = values[5] as List<com.moneytracker.domain.model.Expense>
            @Suppress("UNCHECKED_CAST")
            val recentIncomes = values[6] as List<com.moneytracker.domain.model.Income>
            val budget = values[7] as Budget
            @Suppress("UNCHECKED_CAST")
            val categoryUsages = values[8] as List<CategoryBudgetUsage>

            // Wallet balances use only month-scoped transactions.
            val monthTransactions = buildList {
                addAll(monthExpenses.map { it.toTransaction() })
                addAll(monthIncomes.map { it.toTransaction() })
            }
            val balances = calculateWalletBalances(monthTransactions)

            // Recent transactions list — top N by timestamp across the full history.
            // Uses the lightweight getRecent() queries instead of full-table loads.
            val recent = buildList<Transaction> {
                addAll(recentExpenses.map { it.toTransaction() })
                addAll(recentIncomes.map { it.toTransaction() })
            }
                .sortedByDescending { it.timestamp }
                .take(recentLimit)

            Summary(
                recentTransactions = recent,
                spentToday = spentToday,
                spentThisMonth = spentThisMonth,
                totalIncome = totalIncome,
                remaining = budget.monthlyBudget - spentThisMonth,
                balance = balances.total,
                cashBalance = balances.cash,
                bankBalance = balances.bank,
                budget = budget,
                budgetUsage = if (budget.monthlyBudget > 0) spentThisMonth / budget.monthlyBudget else 0.0,
                categoryBudgetUsages = categoryUsages
            )
        }
    }

    companion object {
        const val RECENT_TRANSACTION_LIMIT = 10
    }
}
