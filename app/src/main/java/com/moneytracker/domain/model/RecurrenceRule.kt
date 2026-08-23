package com.moneytracker.domain.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/**
 * Represents how often a transaction repeats. Persisted as a string in Room.
 * Format: TYPE[:arg1[:arg2]]
 *
 *   - DAILY
 *   - WEEKLY:MONDAY
 *   - MONTHLY:15        (day of month, 1-28/29/30/31 as supported by java.time)
 *   - YEARLY:6:15       (month, dayOfMonth)
 */
sealed class RecurrenceRule {
    abstract fun nextOccurrence(after: LocalDate): LocalDate

    fun encode(): String = when (this) {
        is Daily -> "DAILY"
        is Weekly -> "WEEKLY:${dayOfWeek.name}"
        is Monthly -> "MONTHLY:${dayOfMonth}"
        is Yearly -> "YEARLY:${month}:${dayOfMonth}"
    }

    object Daily : RecurrenceRule() {
        override fun nextOccurrence(after: LocalDate): LocalDate = after.plusDays(1)
    }

    data class Weekly(val dayOfWeek: DayOfWeek) : RecurrenceRule() {
        override fun nextOccurrence(after: LocalDate): LocalDate {
            var d = after.plusDays(1)
            while (d.dayOfWeek != dayOfWeek) d = d.plusDays(1)
            return d
        }
    }

    data class Monthly(val dayOfMonth: Int) : RecurrenceRule() {
        override fun nextOccurrence(after: LocalDate): LocalDate {
            val nextMonth = YearMonth.from(after).plusMonths(1)
            val dom = dayOfMonth.coerceAtMost(nextMonth.lengthOfMonth())
            return nextMonth.atDay(dom)
        }
    }

    data class Yearly(val month: Int, val dayOfMonth: Int) : RecurrenceRule() {
        override fun nextOccurrence(after: LocalDate): LocalDate {
            val ym = YearMonth.of(after.year + 1, month)
            val dom = dayOfMonth.coerceAtMost(ym.lengthOfMonth())
            return ym.atDay(dom)
        }
    }

    companion object {
        fun decode(value: String?): RecurrenceRule? {
            if (value.isNullOrBlank()) return null
            val parts = value.split(":")
            return when (parts[0]) {
                "DAILY" -> Daily
                "WEEKLY" -> Weekly(DayOfWeek.valueOf(parts[1]))
                "MONTHLY" -> Monthly(parts[1].toInt())
                "YEARLY" -> Yearly(parts[1].toInt(), parts[2].toInt())
                else -> null
            }
        }
    }
}