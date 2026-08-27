/**
 * Schedules / cancels the [DailySummaryWorker] using WorkManager.
 *
 * `enable()` enqueues a unique periodic worker with a fresh initial delay
 * targeting 8 PM tonight. `disable()` cancels it. The unique-work name
 * ensures re-enabling never creates duplicates.
 */
package com.moneytracker.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the periodic work that posts the daily summary.
 * Called from SettingsScreen whenever the user toggles dailySummaryEnabled.
 *
 * We aim for 8 PM local time; the first run is scheduled at the next 20:00,
 * then once every 24 hours.
 */
@Singleton
class DailySummaryScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun enable() {
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNext8Pm(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun disable() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    private fun delayUntilNext8Pm(): Long {
        val now = LocalDateTime.now()
        val target = now.toLocalDate().atTime(LocalTime.of(20, 0))
        val next = if (now.isBefore(target)) target else target.plusDays(1)
        return Duration.between(now, next).toMillis()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "daily_summary_worker"
    }
}
