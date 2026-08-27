/**
 * `Application` subclass; the root of the Hilt graph.
 *
 * Three things happen in `onCreate`:
 *  1. Locale warm-up: read the saved language from DataStore on the IO
 *     dispatcher and apply it via [com.moneytracker.util.LocaleHelper] so
 *     the very first setContent() already renders in the right language.
 *  2. Daily-summary work: re-enqueue [DailySummaryScheduler] if the user
 *     previously opted in. Uses the latest preference read; idempotent.
 *  3. Recurring-transactions work: always enqueue
 *     [RecurringTransactionsScheduler] (KEEP policy means it's a no-op
 *     if already pending). Guarantees recurring rows are materialised
 *     every day even if the user never opens the app.
 *
 * All three are launched on the application scope (SupervisorJob +
 * Dispatchers.IO) so they never block the main thread and one failure
 * can't cancel the others.
 */
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
