package com.moneytracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moneytracker.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // Synchronous first read. The previous implementation used a synthetic
    // placeholder ("system" / "System") as the initial value of stateIn, which
    // made Compose's first frame render with the wrong locale / theme until the
    // real DataStore value arrived. MainActivity then saw locale change and
    // called recreate() — looping until the placeholder and the persisted value
    // happened to align. That loop showed up to the user as the screen going
    // black and coming back twice on cold start, and silently corrupted the
    // saved locale preference on every launch.
    //
    // Blocking the constructor here is intentional: DataStore's first read
    // completes in single-digit milliseconds and the ViewModel is created on
    // the main thread before the first Compose frame, so the user never
    // perceives the wait. We use Dispatchers.IO to keep the main thread free
    // in case the disk is slow.
    private val initialLocale: String = runBlocking(Dispatchers.IO) {
        settingsRepository.language.first()
    }
    private val initialTheme: String = runBlocking(Dispatchers.IO) {
        settingsRepository.theme.first()
    }

    val theme: StateFlow<String> = settingsRepository.theme.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        initialTheme
    )

    val locale: StateFlow<String> = settingsRepository.language.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        initialLocale
    )
}
