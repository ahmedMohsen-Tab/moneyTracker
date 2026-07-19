package com.moneytracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.repository.BudgetRepository
import com.moneytracker.data.repository.ExpenseRepository
import com.moneytracker.data.repository.IncomeRepository
import com.moneytracker.data.repository.SettingsRepository
import com.moneytracker.domain.model.Budget
import com.moneytracker.domain.model.Transaction
import com.moneytracker.domain.model.toTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    expenseRepository: ExpenseRepository,
    incomeRepository: IncomeRepository,
    budgetRepository: BudgetRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val today = LocalDate.now()
    
    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth = _selectedMonth.asStateFlow()

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun resetToCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
    }

    val currency = settingsRepository.currency.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "USD"
    )

    val uiState: StateFlow<DashboardUiState> = combine(
        expenseRepository.getAllExpenses(),
        incomeRepository.getAllIncome(),
        budgetRepository.getBudget(),
        currency,
        _selectedMonth
    ) { expenses, incomes, budget, currency, month ->
        val monthString = month.toString() // "YYYY-MM"
        
        val monthExpenses = expenses.filter { it.date.toString().startsWith(monthString) }
        val monthIncomes = incomes.filter { it.date.toString().startsWith(monthString) }
        
        val spentThisMonth = monthExpenses.sumOf { it.amount }
        val totalIncome = monthIncomes.sumOf { it.amount }
        
        val spentToday = expenses.filter { it.date == today }.sumOf { it.amount }
        
        val allTransactions = (expenses.map { it.toTransaction() } + incomes.map { it.toTransaction() })
        
        var cashBalance = 0.0
        var bankBalance = 0.0
        var ccBalance = 0.0
        
        allTransactions.forEach { tx ->
            val amount = if (tx is Transaction.ExpenseTransaction) -tx.amount else tx.amount
            val wallet = if (tx is Transaction.ExpenseTransaction) tx.wallet else (tx as Transaction.IncomeTransaction).wallet
            when (wallet) {
                "Cash" -> cashBalance += amount
                "Bank" -> bankBalance += amount
                "Credit Card" -> ccBalance += amount
            }
        }
        
        val totalBalance = cashBalance + bankBalance + ccBalance
        
        val recentTransactions = allTransactions
            .filter { it.date.toString().startsWith(monthString) }
            .sortedByDescending { it.timestamp }
            .take(10)
            
        DashboardUiState(
            recentTransactions = recentTransactions,
            spentToday = spentToday,
            spentThisMonth = spentThisMonth,
            totalIncome = totalIncome,
            remaining = budget.monthlyBudget - spentThisMonth,
            balance = totalBalance,
            cashBalance = cashBalance,
            bankBalance = bankBalance,
            creditCardBalance = ccBalance,
            budget = budget,
            currency = currency,
            budgetUsage = if (budget.monthlyBudget > 0) spentThisMonth / budget.monthlyBudget else 0.0
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState()
    )
}

data class DashboardUiState(
    val recentTransactions: List<Transaction> = emptyList(),
    val spentToday: Double = 0.0,
    val spentThisMonth: Double = 0.0,
    val totalIncome: Double = 0.0,
    val remaining: Double = 0.0,
    val balance: Double = 0.0,
    val cashBalance: Double = 0.0,
    val bankBalance: Double = 0.0,
    val creditCardBalance: Double = 0.0,
    val budget: Budget = Budget(),
    val currency: String = "USD",
    val budgetUsage: Double = 0.0
)
