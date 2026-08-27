/**
 * Domain model for a spending category (Food, Coffee, Shopping, ...).
 *
 * `id` is the Room primary key. The seeded categories use ids 1..10;
 * user-created categories get ids > 10 (see
 * [com.moneytracker.data.repository.CategoryRepository.insertCategory]).
 *
 * `iconName` is the name of a Material icon. The UI layer looks the icon
 * up by name at render time so we can ship new icons without a code
 * change.
 *
 * [Category.default] is the safe placeholder used by Add Expense when
 * the categories flow hasn't emitted yet.
 */
package com.moneytracker.domain.model

data class Category(
    val id: Int,
    val name: String,
    val iconName: String,
    val color: Int
) {
    companion object {
        val default = Category(10, "Other", "MoreVert", 0xFF607D8B.toInt())
    }
}
