package com.moneytracker

import android.app.Application
import com.moneytracker.data.local.preferences.UserPreferences
import com.moneytracker.util.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MoneyTrackerApplication : Application() {

    @Inject
    lateinit var userPreferences: UserPreferences

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val saved = userPreferences.language.first()
            LocaleHelper.apply(saved)
        }
    }
}
