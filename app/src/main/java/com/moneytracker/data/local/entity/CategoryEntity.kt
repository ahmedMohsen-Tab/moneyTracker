/**
 * Room entity for the seeded category list (Food, Coffee, Shopping, ...).
 *
 * The id is hard-coded to small stable integers (1..10) so that
 * [ExpenseEntity.categoryId] references survive database migrations and
 * cross-device CSV imports. Never auto-generate new ids here.
 *
 * `iconName` is the name of a Material icon (looked up by string at render
 * time) so adding a new category requires no Kotlin change — just a new
 * row with a valid icon name.
 */
package com.moneytracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val id: Int,
    val name: String,
    val iconName: String,
    val color: Int
)
