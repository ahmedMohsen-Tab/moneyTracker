package com.moneytracker.ui.screens.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
                    state.copy(
                        categories = categories,
                        selectedCategory = if (state.selectedCategory == Category.default && categories.isNotEmpty()) {
                            categories.first()
                        } else {
                            state.selectedCategory
                        }
                    )
                }
            }
        }
    }

    fun loadExpense(id: Long) {
        viewModelScope.launch {
            val expense = expenseRepository.getExpenseById(id)
            expense?.let {
                _uiState.value = _uiState.value.copy(
                    amount = it.amount.toString(),
                    selectedCategory = it.category,
                    description = it.description,
                    date = it.date,
                    time = it.time,
                    wallet = it.wallet,
                    isEdit = true,
                    editId = it.id
                )
            }
        }
    }

    fun loadIncome(id: Long) {
        viewModelScope.launch {
            val income = incomeRepository.getIncomeById(id)
            income?.let {
                _uiState.value = _uiState.value.copy(
                    amount = it.amount.toString(),
                    description = it.description,
                    date = it.date,
                    time = it.time,
                    isIncome = true,
                    isEdit = true,
                    editId = it.id,
                    wallet = it.wallet
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

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.amount.toDoubleOrNull() ?: 0.0
            if (amount <= 0) {
                _events.emit(AddExpenseEvent.ShowError("Amount must be greater than zero"))
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
                _events.emit(AddExpenseEvent.ShowError(e.message ?: "Unknown error"))
            }
        }
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
        val categories: List<Category> = emptyList()
    )

    sealed class AddExpenseEvent {
        data object NavigateBack : AddExpenseEvent()
        data class ShowError(val message: String) : AddExpenseEvent()
    }
}
