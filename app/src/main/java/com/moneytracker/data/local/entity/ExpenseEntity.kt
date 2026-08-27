/**
 * Room entity for a single expense row.
 *
 * `categoryId` is a foreign key into [CategoryEntity.id] — we do NOT use a
 * Room @Relation here because most queries need only the id, and joining
 * per-row is expensive (see [ExpenseWithCategory] for the join variant used
 * by the dashboard list).
 *
 * `timestamp` is the **epoch second at the user-entered local date/time**
 * (NOT UTC). See the comment in [com.moneytracker.data.mapper.toEntity]
 * for the full rationale.
 *
 * `recurrenceRule` and `recurrenceGroupId` are nullable so non-recurring
 * expenses stay compact. `recurrenceGroupId` ties together all concrete
 * rows that were materialised from the same template.
 */
package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,
    val categoryId: Int,
    val description: String,
    val date: String,
    val time: String,
    val timestamp: Long,
    val wallet: String = "Cash",
    val recurrenceRule: String? = null,
    val recurrenceGroupId: String? = null
)
