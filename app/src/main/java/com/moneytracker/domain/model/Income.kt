package com.moneytracker.domain.model

import java.time.LocalDate
import java.time.LocalTime

data class Income(
    val id: Long = 0,
    val amount: Double,
    val description: String,
    val date: LocalDate,
    val time: LocalTime,
    val wallet: String = "Cash",
    val recurrenceRule: RecurrenceRule? = null,
    val recurrenceGroupId: String? = null
)
