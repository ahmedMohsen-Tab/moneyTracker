package com.moneytracker.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object AddExpense : Screen("add_expense")
    data object EditExpense : Screen("edit_expense/{expenseId}") {
        fun createRoute(expenseId: Long) = "edit_expense/$expenseId"
    }
    data object ExpensesList : Screen("expenses_list")
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")
}
