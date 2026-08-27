package com.moneytracker.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Daily scheduler for the recurring-transactions materializer.
 * Uses [ExistingPeriodicWorkPolicy.KEEP] so re-installing on every app
 * start is cheap and doesn't reset the existing cadence.
 */
@Singleton
class RecurringTransactionsScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun enable() {
        val request = PeriodicWorkRequestBuilder<RecurringTransactionsWorker>(1, TimeUnit.DAYS)
            // Run once a day; the platform picks an arbitrary hour. The materializer
            // is idempotent, so a delayed run is fine.
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** One-shot run for "Materialize now" used by the worker self-test path. */
    fun runOnce() {
        val request = androidx.work.OneTimeWorkRequestBuilder<RecurringTransactionsWorker>()
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "recurring_transactions_worker"
    }
}
