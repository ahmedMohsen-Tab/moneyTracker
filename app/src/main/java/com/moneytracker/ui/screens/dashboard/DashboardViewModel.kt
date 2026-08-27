/**
 * State holder for the Dashboard screen.
 *
 * Composes the dashboard summary use case with the user's selected month
 * (a flatMapLatest over `_selectedMonth`) and the current "today"
 * (re-evaluated on each `ON_RESUME` via [onResume]).
 *
 * `currency` is exposed separately so it reacts to Settings-screen
 * changes immediately, without having to recompute the whole summary.
 *
 * Note: `currency` is passed as a *value* into
 * [com.moneytracker.domain.usecase.GetDashboardSummaryUseCase.invoke]
 * but is currently unused there — the active currency is re-read
 * reactively from `SettingsRepository.currency` in the ViewModel. Kept
 * in the signature for forward compatibility.
 */
package com.moneytracker.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.repository.SettingsRepository
import com.moneytracker.domain.model.Budget
import com.moneytracker.domain.model.CategoryBudgetUsage
import com.moneytracker.domain.model.Transaction
import com.moneytracker.domain.usecase.GetDashboardSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getDashboardSummary: GetDashboardSummaryUseCase,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _selectedMonth = MutableStateFlow(YearMonth.now())
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    // `today` is observed reactively so the dashboard rolls over at midnight even
    // when this ViewModel is kept alive by configuration changes.
    private val _today = MutableStateFlow(LocalDate.now())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    val currency: StateFlow<String> = settingsRepository.currency.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = "USD"
    )

    val uiState: StateFlow<DashboardUiState> = _selectedMonth
        .flatMapLatest { month ->
            // combine with both `_today` and `currency` so the screen reflects
            // either change without requiring a month flip or a process restart.
            combine(
                getDashboardSummary(month, _today.value, "$currency"),
                currency
            ) { summary, ccy -> summary to ccy }
                .mapToUiState()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardUiState()
        )

    private fun kotlinx.coroutines.flow.Flow<Pair<GetDashboardSummaryUseCase.Summary, String>>.mapToUiState() =
        map { (summary, ccy) ->
            DashboardUiState(
                recentTransactions = summary.recentTransactions,
                spentToday = summary.spentToday,
                spentThisMonth = summary.spentThisMonth,
                totalIncome = summary.totalIncome,
                remaining = summary.remaining,
                balance = summary.balance,
                cashBalance = summary.cashBalance,
                bankBalance = summary.bankBalance,
                budget = summary.budget,
                currency = ccy,
                budgetUsage = summary.budgetUsage,
                categoryBudgetUsages = summary.categoryBudgetUsages
            )
        }

    fun nextMonth() {
        _selectedMonth.value = _selectedMonth.value.plusMonths(1)
    }

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun resetToCurrentMonth() {
        _selectedMonth.value = YearMonth.now()
    }

    /** Called by the host (Activity) on `ON_RESUME` so midnight/day rollovers are reflected. */
    fun onResume() {
        _today.value = LocalDate.now()
    }
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
    val budget: Budget = Budget(),
    val currency: String = "USD",
    val budgetUsage: Double = 0.0,
    val categoryBudgetUsages: List<CategoryBudgetUsage> = emptyList()
)
