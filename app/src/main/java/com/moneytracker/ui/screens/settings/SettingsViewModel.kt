/**
 * State holder for the Settings screen.
 *
 * Exposes every preference as a `StateFlow` (via
 * `settingsRepository.<key>.stateIn(...)`) and groups the destructive /
 * IO-bound actions (export, import, reset) behind `runOrError` which
 * catches exceptions and surfaces them as toast events instead of
 * crashing.
 */
package com.moneytracker.ui.screens.settings

import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.R
import com.moneytracker.data.repository.CategoryBudgetRepository
import com.moneytracker.data.repository.CategoryRepository
import com.moneytracker.data.repository.SettingsRepository
import com.moneytracker.domain.model.Category
import com.moneytracker.domain.model.CategoryBudget
import com.moneytracker.notifications.DailySummaryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val categoryBudgetRepository: CategoryBudgetRepository,
    private val dailySummaryScheduler: DailySummaryScheduler,
    categoryRepository: CategoryRepository
) : ViewModel() {

    val currency: StateFlow<String> = settingsRepository.currency.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = "USD"
    )
    val theme: StateFlow<String> = settingsRepository.theme.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = "System"
    )
    val locale: StateFlow<String> = settingsRepository.language.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = "system"
    )
    val dailySummaryEnabled: StateFlow<Boolean> = settingsRepository.dailySummaryEnabled.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = true
    )

    val categoryBudgets: StateFlow<List<CategoryBudget>> = categoryBudgetRepository.getAll().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    val categories: StateFlow<List<Category>> = categoryRepository.getAllCategories().stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = emptyList()
    )

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun setCurrency(currency: String) {
        viewModelScope.launch { settingsRepository.setCurrency(currency) }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setLocale(tag: String) {
        viewModelScope.launch { settingsRepository.setLanguage(tag) }
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDailySummaryEnabled(enabled)
            if (enabled) dailySummaryScheduler.enable() else dailySummaryScheduler.disable()
        }
    }

    fun setCategoryBudget(categoryId: Int, limit: Double, currency: String) {
        viewModelScope.launch {
            categoryBudgetRepository.upsert(CategoryBudget(categoryId, limit, currency))
        }
    }

    fun clearCategoryBudget(categoryId: Int) {
        viewModelScope.launch { categoryBudgetRepository.delete(categoryId) }
    }

    fun exportToCsv(uri: Uri) {
        viewModelScope.launch {
            runOrError(onSuccess = R.string.settings_export_success, onFail = R.string.settings_export_failed) {
                settingsRepository.exportToCsv(uri)
            }
        }
    }

    fun importFromCsv(uri: Uri) {
        viewModelScope.launch {
            runOrError(onSuccess = R.string.settings_import_success, onFail = R.string.settings_import_failed) {
                settingsRepository.importFromCsv(uri)
            }
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            runOrError(onSuccess = R.string.settings_reset_success, onFail = R.string.settings_reset_failed) {
                settingsRepository.resetAll()
            }
        }
    }

    private suspend inline fun runOrError(
        @StringRes onSuccess: Int,
        @StringRes onFail: Int,
        block: () -> Unit
    ) {
        try {
            block()
            _events.emit(SettingsEvent.ShowMessage(onSuccess))
        } catch (e: Exception) {
            Log.w(TAG, "Settings operation failed", e)
            _events.emit(SettingsEvent.ShowError(onFail))
        }
    }

    sealed class SettingsEvent {
        data class ShowMessage(@StringRes val messageRes: Int) : SettingsEvent()
        data class ShowError(@StringRes val messageRes: Int) : SettingsEvent()
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}
