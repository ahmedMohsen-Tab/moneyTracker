/**
 * State holder for the Statistics screen. Delegates the aggregations to
 * [com.moneytracker.domain.usecase.GetStatisticsUseCase] and exposes
 * them as `StateFlow`s for the screen.
 */
package com.moneytracker.ui.screens.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.repository.SettingsRepository
import com.moneytracker.domain.usecase.GetStatisticsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatisticsViewModel @Inject constructor(
    getStatisticsUseCase: GetStatisticsUseCase,
    settingsRepository: SettingsRepository
) : ViewModel() {

    // Reactive so the screen updates when the day rolls over even if this VM is
    // kept alive across midnight.
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    val currency: StateFlow<String> = settingsRepository.currency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "USD"
    )

    val uiState: StateFlow<StatisticsUiState> = _today
        .flatMapLatest { today ->
            val monthString = today.toString().substring(0, 7)
            combine(getStatisticsUseCase(monthString), currency) { stats, ccy ->
                StatisticsUiState(
                    totalIncome = stats.totalIncome,
                    totalExpenses = stats.totalExpense,
                    remainingBalance = stats.remaining,
                    averageDailySpending = stats.averageDailySpending,
                    highestSpendingDay = stats.highestSpendingDay,
                    highestSpendingCategory = stats.highestSpendingCategory?.let { pair ->
                        (stats.expenses.find { it.category.id == pair.first }?.category?.name ?: "") to pair.second
                    },
                    transactionCount = stats.transactionCount,
                    dailySpending = stats.expenses
                        .groupBy { it.date.toString() }
                        .mapValues { entry -> entry.value.sumOf { it.amount } },
                    categorySpending = stats.expenses
                        .groupBy { it.category.name }
                        .mapValues { entry -> entry.value.sumOf { it.amount } },
                    weeklySpending = (0..6).associate { offset ->
                        val day = today.with(DayOfWeek.MONDAY).plusDays(offset.toLong())
                        day.dayOfWeek.name.take(3) to
                            stats.expenses.filter { it.date == day }.sumOf { it.amount }
                    },
                    currency = ccy
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatisticsUiState()
        )

    /**
     * Re-anchor `_today` only when the actual day has changed. Without this
     * guard, attaching the lifecycle observer (which fires ON_RESUME
     * immediately on add, because the lifecycle is already at RESUMED when
     * the screen mounts) would trigger a `_today` update on every tab
     * switch, which restarts the upstream `combine`, which makes MPAndroidChart
     * re-paint all three charts on top of their first-paint — a major
     * source of jank on tab navigation.
     */
    fun onResume() {
        val now = LocalDate.now()
        if (_today.value != now) _today.value = now
    }
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
