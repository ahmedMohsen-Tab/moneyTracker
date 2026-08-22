package com.moneytracker.ui.screens.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.repository.SettingsRepository
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
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val currency: StateFlow<String> = settingsRepository.currency.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "USD"
    )

    val theme: StateFlow<String> = settingsRepository.theme.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "System"
    )

    val locale: StateFlow<String> = settingsRepository.language.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        "system"
    )

    private val _events = MutableSharedFlow<SettingsEvent>()
    val events = _events.asSharedFlow()

    fun setCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.setCurrency(currency)
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setLocale(tag: String) {
        viewModelScope.launch {
            settingsRepository.setLanguage(tag)
        }
    }

    fun exportToCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                settingsRepository.exportToCsv(uri)
                _events.emit(SettingsEvent.ShowMessage("Exported successfully"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowError(e.message ?: "Export failed"))
            }
        }
    }

    fun importFromCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                settingsRepository.importFromCsv(uri)
                _events.emit(SettingsEvent.ShowMessage("Imported successfully"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowError(e.message ?: "Import failed"))
            }
        }
    }

    fun resetAll() {
        viewModelScope.launch {
            try {
                settingsRepository.resetAll()
                _events.emit(SettingsEvent.ShowMessage("All data reset"))
            } catch (e: Exception) {
                _events.emit(SettingsEvent.ShowError(e.message ?: "Reset failed"))
            }
        }
    }

    sealed class SettingsEvent {
        data class ShowMessage(val message: String) : SettingsEvent()
        data class ShowError(val message: String) : SettingsEvent()
    }
}
