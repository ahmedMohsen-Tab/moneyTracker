package com.moneytracker

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.moneytracker.data.local.preferences.UserPreferences
import com.moneytracker.notifications.DailySummaryScheduler
import com.moneytracker.notifications.RecurringTransactionsScheduler
import com.moneytracker.util.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MoneyTrackerApplication : Application(), Configuration.Provider {

    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var dailySummaryScheduler: DailySummaryScheduler
    @Inject lateinit var recurringTransactionsScheduler: RecurringTransactionsScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Best-effort locale warm-up. The authoritative locale sync lives in
        // MainActivity (which reads the preference and recreates itself when
        // needed); this only narrows the window during which the very first
        // setContent() may briefly render in the previous locale.
        appScope.launch {
            val saved = userPreferences.language.first()
            LocaleHelper.apply(saved)
        }
        // Re-enqueue the daily-summary work if the user previously opted in.
        // Reads the latest preference, then asks the scheduler to (re)install
        // the periodic work with a fresh initial delay targeting 8 PM tonight.
        appScope.launch {
            val enabled = userPreferences.dailySummaryEnabled.firstOrNull() ?: return@launch
            if (enabled) dailySummaryScheduler.enable()
        }
        // Always run the recurring-transactions materializer once a day. Idempotent
        // — safe to re-enqueue with KEEP on every cold start.
        recurringTransactionsScheduler.enable()
    }
}
