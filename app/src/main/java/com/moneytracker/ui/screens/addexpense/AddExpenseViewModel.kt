package com.moneytracker.ui.screens.addexpense

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.R
import com.moneytracker.data.repository.CategoryRepository
import com.moneytracker.data.repository.ExpenseRepository
import com.moneytracker.data.repository.IncomeRepository
import com.moneytracker.data.repository.SettingsRepository
import com.moneytracker.domain.model.Category
import com.moneytracker.domain.model.Expense
import com.moneytracker.domain.model.Income
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val incomeRepository: IncomeRepository,
    private val categoryRepository: CategoryRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddExpenseEvent>()
    val events: SharedFlow<AddExpenseEvent> = _events.asSharedFlow()

    val currency = settingsRepository.currency

    init {
        viewModelScope.launch {
            categoryRepository.getAllCategories().collect { categories ->
                _uiState.update { state ->
                    val pending = state.pendingNewCategoryName
                    val matching = pending?.let { name ->
                        categories.firstOrNull { it.name.equals(name, ignoreCase = true) }
                    }
                    state.copy(
                        categories = categories,
                        selectedCategory = when {
                            matching != null -> matching
                            // First run with no selection yet — fall back to the first
                            // available category rather than the "Other" placeholder.
                            state.selectedCategory == Category.default && categories.isNotEmpty() -> {
                                categories.first()
                            }
                            else -> state.selectedCategory
                        },
                        // Drop the marker once we've either picked the new row or
                        // decided it isn't there — either way, the user's intent
                        // has been honoured and we shouldn't keep overriding the
                        // selection on subsequent emissions.
                        pendingNewCategoryName = if (matching != null || pending == null) null else pending
                    )
                }
            }
        }
    }

    fun loadExpense(id: Long) {
        viewModelScope.launch {
            expenseRepository.getExpenseById(id)?.let { expense ->
                _uiState.value = _uiState.value.copy(
                    amount = expense.amount.toString(),
                    selectedCategory = expense.category,
                    description = expense.description,
                    date = expense.date,
                    time = expense.time,
                    wallet = expense.wallet,
                    isEdit = true,
                    editId = expense.id
                )
            }
        }
    }

    fun loadIncome(id: Long) {
        viewModelScope.launch {
            incomeRepository.getIncomeById(id)?.let { income ->
                _uiState.value = _uiState.value.copy(
                    amount = income.amount.toString(),
                    description = income.description,
                    date = income.date,
                    time = income.time,
                    isIncome = true,
                    isEdit = true,
                    editId = income.id,
                    wallet = income.wallet
                )
            }
        }
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun updateCategory(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(description = description)
    }

    fun updateDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateTime(time: LocalTime) {
        _uiState.value = _uiState.value.copy(time = time)
    }

    fun updateWallet(wallet: String) {
        _uiState.value = _uiState.value.copy(wallet = wallet)
    }

    fun toggleIncome(isIncome: Boolean) {
        _uiState.value = _uiState.value.copy(isIncome = isIncome)
    }

    fun onOtherCategorySelected() {
        _uiState.update { state ->
            state.copy(
                showNewCategoryDialog = true,
                newCategoryName = if (state.selectedCategory == Category.default) "" else state.newCategoryName
            )
        }
    }

    fun updateNewCategoryName(name: String) {
        _uiState.update { it.copy(newCategoryName = name) }
    }

    fun dismissNewCategoryDialog() {
        _uiState.update { it.copy(showNewCategoryDialog = false, newCategoryName = "") }
    }

    fun confirmNewCategory() {
        val trimmed = _uiState.value.newCategoryName.trim()
        if (trimmed.isEmpty()) {
            viewModelScope.launch { emitError(R.string.add_expense_error_category_empty) }
            return
        }
        val existing = _uiState.value.categories
        if (existing.any { it.name.equals(trimmed, ignoreCase = true) }) {
            viewModelScope.launch { emitError(R.string.add_expense_error_category_exists) }
            return
        }
        viewModelScope.launch {
            try {
                val newCategory = Category(
                    // id = 0 lets Room auto-generate, avoiding the race that came from
                    // reading `getMaxCategoryId() + 1` (two near-simultaneous inserts
                    // could collide on the same id).
                    id = 0,
                    name = trimmed,
                    iconName = Category.default.iconName,
                    color = Category.default.color
                )
                categoryRepository.insertCategory(newCategory)
                _uiState.update {
                    it.copy(
                        showNewCategoryDialog = false,
                        newCategoryName = "",
                        // Stash the requested name so the init block's `collect`
                        // can promote the matching freshly-inserted row to the
                        // selected category once Room emits the new list.
                        pendingNewCategoryName = trimmed
                    )
                }
            } catch (e: Exception) {
                _events.emit(AddExpenseEvent.ShowError(R.string.add_expense_error_category_failed))
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.amount.toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                emitError(R.string.add_expense_error_amount_zero)
                return@launch
            }
            try {
                if (state.isIncome) {
                    val income = Income(
                        id = state.editId,
                        amount = amount,
                        description = state.description,
                        date = state.date,
                        time = state.time,
                        wallet = state.wallet
                    )
                    if (state.isEdit) incomeRepository.updateIncome(income)
                    else incomeRepository.insertIncome(income)
                } else {
                    val expense = Expense(
                        id = state.editId,
                        amount = amount,
                        category = state.selectedCategory,
                        description = state.description,
                        date = state.date,
                        time = state.time,
                        wallet = state.wallet
                    )
                    if (state.isEdit) expenseRepository.updateExpense(expense)
                    else expenseRepository.insertExpense(expense)
                }
                _events.emit(AddExpenseEvent.NavigateBack)
            } catch (e: Exception) {
                _events.emit(AddExpenseEvent.ShowError(R.string.error_unknown))
            }
        }
    }

    private suspend fun emitError(@StringRes messageRes: Int) {
        _events.emit(AddExpenseEvent.ShowError(messageRes))
    }

    data class AddExpenseUiState(
        val amount: String = "",
        val selectedCategory: Category = Category.default,
        val description: String = "",
        val date: LocalDate = LocalDate.now(),
        val time: LocalTime = LocalTime.now(),
        val wallet: String = "Cash",
        val isIncome: Boolean = false,
        val isEdit: Boolean = false,
        val editId: Long = 0,
        val categories: List<Category> = emptyList(),
        val showNewCategoryDialog: Boolean = false,
        val newCategoryName: String = "",
        /**
         * Marker set after the user confirms a new category. The init block's
         * `collect` on the categories flow uses it to promote the freshly
         * inserted row to `selectedCategory` once Room emits the new list.
         * Cleared as soon as the selection has been updated.
         */
        val pendingNewCategoryName: String? = null
    )

    sealed class AddExpenseEvent {
        data object NavigateBack : AddExpenseEvent()
        data class ShowError(@StringRes val messageRes: Int) : AddExpenseEvent()
    }
}
