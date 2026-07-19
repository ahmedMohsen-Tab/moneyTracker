package com.moneytracker.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.repository.SettingsRepository
import com.moneytracker.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    getStatisticsUseCase: GetStatisticsUseCase,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val currentMonth = LocalDate.now().toString().substring(0, 7)

    val currency = settingsRepository.currency.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "USD"
    )

    val uiState: StateFlow<StatisticsUiState> = combine(
        getStatisticsUseCase(currentMonth),
        currency
    ) { stats, currency ->
        val expenses = stats.expenses
        val daily = expenses
            .groupBy { it.date.toString() }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val category = expenses
            .groupBy { it.category.name }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
        val weekly = (0..6).associate { offset ->
            val day = weekStart.plusDays(offset.toLong())
            day.dayOfWeek.name.take(3) to expenses.filter { it.date == day }.sumOf { it.amount }
        }
        val highestCategory = stats.highestSpendingCategory?.let { pair ->
            val name = expenses.find { it.category.id == pair.first }?.category?.name ?: ""
            name to pair.second
        }
        StatisticsUiState(
            totalIncome = stats.totalIncome,
            totalExpenses = stats.totalExpense,
            remainingBalance = stats.remaining,
            averageDailySpending = stats.averageDailySpending,
            highestSpendingDay = stats.highestSpendingDay,
            highestSpendingCategory = highestCategory,
            transactionCount = stats.transactionCount,
            dailySpending = daily,
            categorySpending = category,
            weeklySpending = weekly,
            currency = currency
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        StatisticsUiState()
    )
}

data class StatisticsUiState(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val remainingBalance: Double = 0.0,
    val averageDailySpending: Double = 0.0,
    val highestSpendingDay: Pair<String, Double>? = null,
    val highestSpendingCategory: Pair<String, Double>? = null,
    val transactionCount: Int = 0,
    val dailySpending: Map<String, Double> = emptyMap(),
    val categorySpending: Map<String, Double> = emptyMap(),
    val weeklySpending: Map<String, Double> = emptyMap(),
    val currency: String = "USD"
)
