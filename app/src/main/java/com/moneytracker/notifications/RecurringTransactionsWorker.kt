package com.moneytracker.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.moneytracker.domain.usecase.MaterializeRecurringTransactionsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Once a day, scans every recurring transaction and inserts any occurrences
 * that have come due. Failure is logged and retried with backoff.
 */
@HiltWorker
class RecurringTransactionsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val materialize: MaterializeRecurringTransactionsUseCase
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = try {
        materialize(today = LocalDate.now())
        Result.success()
    } catch (e: Exception) {
        Log.e(TAG, "Recurring transactions materialization failed", e)
        Result.retry()
    }

    companion object {
        private const val TAG = "RecurringTxWorker"
    }
}
