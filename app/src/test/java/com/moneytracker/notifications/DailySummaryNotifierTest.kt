package com.moneytracker.notifications

import com.moneytracker.domain.model.Budget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySummaryNotifierTest {

    @Test
    fun `no daily budget produces NoBudget body`() {
        val body = DailySummaryNotifier.renderBody(
            spentToday = 25.0,
            budget = Budget(monthlyBudget = 600.0, dailyBudget = 0.0),
            currency = "USD"
        )
        assertTrue(body is DailySummaryNotifier.Companion.Body.NoBudget)
        assertEquals(25.0, (body as DailySummaryNotifier.Companion.Body.NoBudget).spent, 0.0)
    }

    @Test
    fun `with daily budget computes percent and clamps negative to zero`() {
        // 50 of 200 = 25%.
        val body = DailySummaryNotifier.renderBody(
            spentToday = 50.0,
            budget = Budget(monthlyBudget = 6_000.0, dailyBudget = 200.0),
            currency = "USD"
        )
        body as DailySummaryNotifier.Companion.Body.WithBudget
        assertEquals(50.0, body.spent, 0.0)
        assertEquals(200.0, body.budget, 0.0)
        assertEquals(25, body.percent)

        // Refund-style negative spend must clamp to 0%, not -25%.
        val negative = DailySummaryNotifier.renderBody(
            spentToday = -50.0,
            budget = Budget(monthlyBudget = 6_000.0, dailyBudget = 200.0),
            currency = "USD"
        )
        assertEquals(0, (negative as DailySummaryNotifier.Companion.Body.WithBudget).percent)
    }

    @Test
    fun `spending over budget reports over-100 percent`() {
        // 250 of 100 = 250% — clamp is only applied below zero, not above.
        val body = DailySummaryNotifier.renderBody(
            spentToday = 250.0,
            budget = Budget(monthlyBudget = 3_000.0, dailyBudget = 100.0),
            currency = "USD"
        )
        assertEquals(250, (body as DailySummaryNotifier.Companion.Body.WithBudget).percent)
    }
}
