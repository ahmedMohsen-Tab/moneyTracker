/**
 * Builds the local notification shown at 8 PM with today's spending.
 *
 * Two flavours:
 *  - With budget: "You spent X of Y (Z% of your daily budget)".
 *  - Without budget: "You spent X today".
 *
 * Reads today's total from [com.moneytracker.data.repository.ExpenseRepository]
 * (a `Flow<Double>` reduced with `first()` so the notification worker
 * doesn't have to subscribe).
 */
package com.moneytracker.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.moneytracker.MainActivity
import com.moneytracker.R
import com.moneytracker.domain.format.MoneyFormatter
import com.moneytracker.domain.model.Budget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Owns the "Daily summary" notification: builds the channel, posts the body,
 * and exposes a pure function that renders the message text so it can be
 * exercised from a unit test.
 */
@Singleton
class DailySummaryNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun showDailySummary(spentToday: Double, budget: Budget, currency: String) {
        ensureChannel()

        // POST_NOTIFICATIONS is runtime-only on API 33+.
        val canPost = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!canPost) return

        val title = context.getString(R.string.notification_title_daily)
        val body = renderLocalizedText(
            body = renderBody(spentToday, budget, currency),
            currency = currency
        )

        val openApp = PendingIntent.getActivity(
            context,
            REQUEST_OPEN_APP,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_daily_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_daily_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun renderLocalizedText(body: Body, currency: String): CharSequence = when (body) {
        is Body.WithBudget -> context.getString(
            R.string.notification_body_daily_with_budget,
            MoneyFormatter.format(body.spent, currency),
            MoneyFormatter.format(body.budget, currency),
            body.percent
        )
        is Body.NoBudget -> context.getString(
            R.string.notification_body_daily_no_budget,
            MoneyFormatter.format(body.spent, currency)
        )
    }

    companion object {
        const val CHANNEL_ID = "daily_summary"
        const val NOTIFICATION_ID = 1001
        const val REQUEST_OPEN_APP = 2001

        /**
         * Pure formatter — testable without an Android Context.
         * If no daily budget is set, only the spend is reported.
         */
        fun renderBody(spentToday: Double, budget: Budget, currency: String): Body {
            return if (budget.dailyBudget > 0) {
                val percent = ((spentToday / budget.dailyBudget) * 100)
                    .roundToInt()
                    .coerceAtLeast(0)
                Body.WithBudget(
                    spent = spentToday,
                    budget = budget.dailyBudget,
                    percent = percent
                )
            } else {
                Body.NoBudget(spent = spentToday)
            }
        }

        sealed class Body {
            data class WithBudget(val spent: Double, val budget: Double, val percent: Int) : Body()
            data class NoBudget(val spent: Double) : Body()
        }
    }
}
