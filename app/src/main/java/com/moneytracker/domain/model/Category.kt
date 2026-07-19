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
