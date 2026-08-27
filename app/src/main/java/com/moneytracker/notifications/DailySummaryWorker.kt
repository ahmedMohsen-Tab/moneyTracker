/**
 * CoroutineWorker wrapper around [DailySummaryNotifier]. Scheduled by
 * [DailySummaryScheduler].
 */
package com.moneytracker.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneytracker.data.repository.BudgetRepository
import com.moneytracker.data.repository.ExpenseRepository
import com.moneytracker.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Runs once a day. Reads today's spend and the configured budget, then posts
 * the daily-summary notification.
 *
 * Respect for the user's preference (dailySummaryEnabled) is enforced at the
 * scheduling layer so a disabled worker is not enqueued in the first place.
 */
@HiltWorker
class DailySummaryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val settingsRepository: SettingsRepository,
    private val notifier: DailySummaryNotifier
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val currency = settingsRepository.currency.first()
            val budget = budgetRepository.getBudget().first()
            val spentToday = expenseRepository.getTotalByDate(LocalDate.now()).first()
            notifier.showDailySummary(spentToday = spentToday, budget = budget, currency = currency)
            Result.success()
        } catch (e: Exception) {
            // Surface to WorkManager; the platform will retry with backoff.
            Result.retry()
        }
    }
}
