/**
 * State holder for the ExpensesList screen.
 *
 * Subscribes to all expenses + incomes (merged into a single sorted
 * list by timestamp) and exposes a `query: StateFlow<String>` that the
 * search box binds to. Filtering is done client-side on the snapshotted
 * list — the dataset is bounded by what the user has, and a separate
 * server-side query would force a recomposition every keystroke.
 */
package com.moneytracker.ui.screens.expenseslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.repository.ExpenseRepository
import com.moneytracker.data.repository.IncomeRepository
import com.moneytracker.data.repository.SettingsRepository
import com.moneytracker.domain.model.Expense
import com.moneytracker.domain.model.Income
import com.moneytracker.domain.model.Transaction
import com.moneytracker.domain.model.toTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpensesListViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Int?>(null)
    val selectedCategoryId = _selectedCategoryId.asStateFlow()

    private val _selectedMonth = MutableStateFlow<String?>(null)
    val selectedMonth = _selectedMonth.asStateFlow()

    private val _showIncome = MutableStateFlow(true)
    val showIncome = _showIncome.asStateFlow()

    private val _showExpenses = MutableStateFlow(true)
    val showExpenses = _showExpenses.asStateFlow()

    val currency = settingsRepository.currency.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "USD"
    )

    private data class Filters(
        val query: String,
        val categoryId: Int?,
        val month: String?,
        val showIncome: Boolean,
        val showExpenses: Boolean
    )

    val uiState: StateFlow<ExpensesListUiState> = combine(
        expenseRepository.getAllExpenses(),
        incomeRepository.getAllIncome(),
        combine(
            _searchQuery,
            _selectedCategoryId,
            _selectedMonth,
            _showIncome,
            _showExpenses
        ) { q, c, m, i, e -> Filters(q, c, m, i, e) },
        currency
    ) { expenses, incomes, filters, currency ->
        val query = filters.query
        val categoryId = filters.categoryId
        val month = filters.month
        val showIncome = filters.showIncome
        val showExpenses = filters.showExpenses

        val filteredExpenses = expenses.filter {
            (query.isBlank() || it.description.contains(query, true) || it.category.name.contains(query, true)) &&
                (categoryId == null || it.category.id == categoryId) &&
                (month == null || it.date.toString().startsWith(month))
        }
        val filteredIncomes = incomes.filter {
            (query.isBlank() || it.description.contains(query, true)) &&
                (month == null || it.date.toString().startsWith(month))
        }
        val transactions = mutableListOf<Transaction>()
        if (showExpenses) transactions.addAll(filteredExpenses.map { it.toTransaction() })
        if (showIncome) transactions.addAll(filteredIncomes.map { it.toTransaction() })
        transactions.sortByDescending { it.timestamp }
        ExpensesListUiState(
            transactions = transactions,
            currency = currency,
            searchQuery = query,
            selectedCategoryId = categoryId,
            selectedMonth = month,
            showIncome = showIncome,
            showExpenses = showExpenses
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        ExpensesListUiState()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(categoryId: Int?) {
        _selectedCategoryId.value = categoryId
    }

    fun setMonth(month: String?) {
        _selectedMonth.value = month
    }

    fun toggleIncome() {
        _showIncome.value = !_showIncome.value
    }

    fun toggleExpenses() {
        _showExpenses.value = !_showExpenses.value
    }

    private var lastDeleted: Transaction? = null

    fun deleteTransaction(transaction: Transaction) {
        lastDeleted = transaction
        viewModelScope.launch {
            when (transaction) {
                is Transaction.ExpenseTransaction -> expenseRepository.deleteExpense(
                    Expense(
                        id = transaction.id,
                        amount = transaction.amount,
                        category = transaction.category,
                        description = transaction.description,
                        date = transaction.date,
                        time = transaction.time,
                        wallet = transaction.wallet
                    )
                )
                is Transaction.IncomeTransaction -> incomeRepository.deleteIncome(
                    Income(
                        id = transaction.id,
                        amount = transaction.amount,
                        description = transaction.description,
                        date = transaction.date,
                        time = transaction.time,
                        wallet = transaction.wallet
                    )
                )
            }
        }
    }

    fun restoreLastDeleted() {
        val transaction = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch {
            when (transaction) {
                is Transaction.ExpenseTransaction -> expenseRepository.insertExpense(
                    Expense(
                        id = 0,
                        amount = transaction.amount,
                        category = transaction.category,
                        description = transaction.description,
                        date = transaction.date,
                        time = transaction.time,
                        wallet = transaction.wallet
                    )
                )
                is Transaction.IncomeTransaction -> incomeRepository.insertIncome(
                    Income(
                        id = 0,
                        amount = transaction.amount,
                        description = transaction.description,
                        date = transaction.date,
                        time = transaction.time,
                        wallet = transaction.wallet
                    )
                )
            }
        }
    }
}

data class ExpensesListUiState(
    val transactions: List<Transaction> = emptyList(),
    val currency: String = "USD",
    val searchQuery: String = "",
    val selectedCategoryId: Int? = null,
    val selectedMonth: String? = null,
    val showIncome: Boolean = true,
    val showExpenses: Boolean = true
)
