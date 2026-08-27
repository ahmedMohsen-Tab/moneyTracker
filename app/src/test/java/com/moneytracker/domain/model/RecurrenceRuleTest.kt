package com.moneytracker.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class RecurrenceRuleTest {

    private val anchor = LocalDate.of(2026, 8, 26) // Wednesday

    @Test
    fun `daily next occurrence is the day after the anchor`() {
        assertEquals(LocalDate.of(2026, 8, 27), RecurrenceRule.Daily.nextOccurrence(anchor))
    }

    @Test
    fun `weekly next occurrence rolls forward to the named weekday`() {
        val rule = RecurrenceRule.Weekly(DayOfWeek.FRIDAY)
        // Anchor is Wednesday; next Friday is two days later.
        assertEquals(LocalDate.of(2026, 8, 28), rule.nextOccurrence(anchor))
    }

    @Test
    fun `weekly on anchor's weekday skips to next week`() {
        val rule = RecurrenceRule.Weekly(DayOfWeek.WEDNESDAY)
        assertEquals(LocalDate.of(2026, 9, 2), rule.nextOccurrence(anchor))
    }

    @Test
    fun `monthly clamps day of month when target month is shorter`() {
        val rule = RecurrenceRule.Monthly(dayOfMonth = 31)
        // August has 31 days, so September clamps to 30.
        assertEquals(LocalDate.of(2026, 9, 30), rule.nextOccurrence(anchor))
    }

    @Test
    fun `monthly in a long target month keeps the original day`() {
        val rule = RecurrenceRule.Monthly(dayOfMonth = 15)
        assertEquals(LocalDate.of(2026, 9, 15), rule.nextOccurrence(anchor))
    }

    @Test
    fun `yearly moves to the configured month and day`() {
        val rule = RecurrenceRule.Yearly(month = 6, dayOfMonth = 15)
        assertEquals(LocalDate.of(2027, 6, 15), rule.nextOccurrence(anchor))
    }

    @Test
    fun `yearly clamps Feb 29 in a non-leap year`() {
        val rule = RecurrenceRule.Yearly(month = 2, dayOfMonth = 29)
        val from = LocalDate.of(2024, 2, 29)
        // 2025 is not a leap year → February 28.
        assertEquals(LocalDate.of(2025, 2, 28), rule.nextOccurrence(from))
    }

    @Test
    fun `encode then decode round trips every rule`() {
        val rules: List<RecurrenceRule> = listOf(
            RecurrenceRule.Daily,
            RecurrenceRule.Weekly(DayOfWeek.MONDAY),
            RecurrenceRule.Monthly(dayOfMonth = 5),
            RecurrenceRule.Yearly(month = 12, dayOfMonth = 25)
        )
        rules.forEach { original ->
            val decoded = RecurrenceRule.decode(original.encode())
            assertEquals(original, decoded)
        }
    }

    @Test
    fun `decode returns null for blank, null, or unknown inputs`() {
        assertNull(RecurrenceRule.decode(null))
        assertNull(RecurrenceRule.decode(""))
        assertNull(RecurrenceRule.decode("   "))
        assertNull(RecurrenceRule.decode("BIWEEKLY"))
    }
}
