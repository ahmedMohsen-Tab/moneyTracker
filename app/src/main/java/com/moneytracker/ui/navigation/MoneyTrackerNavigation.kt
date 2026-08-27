/**
 * The single `NavHost` for the whole app.
 *
 * Five destinations:
 *  - Dashboard (start)  — home, summary + recent transactions
 *  - AddExpense         — modal-ish flow for new transactions
 *  - EditExpense        — same screen with an `expenseId` nav arg
 *  - ExpensesList       — full searchable history
 *  - Statistics         — charts
 *  - Settings           — preferences + backup/restore
 *
 * Bottom navigation bar only shows on the three top-level destinations.
 * Uses `popUpTo(startDestination) { saveState = true }` +
 * `restoreState = true` so switching tabs preserves each tab's state and
 * the back button still exits the app cleanly from any tab.
 */
package com.moneytracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.moneytracker.R
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.moneytracker.ui.screens.addexpense.AddExpenseScreen
import com.moneytracker.ui.screens.dashboard.DashboardScreen
import com.moneytracker.ui.screens.expenseslist.ExpensesListScreen
import com.moneytracker.ui.screens.settings.SettingsScreen
import com.moneytracker.ui.screens.statistics.StatisticsScreen

@Composable
fun MoneyTrackerNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(
        BottomNavItem(R.string.nav_dashboard, Icons.Default.Home, Screen.Dashboard.route),
        BottomNavItem(R.string.nav_statistics, Icons.Default.TrendingUp, Screen.Statistics.route),
        BottomNavItem(R.string.nav_settings, Icons.Default.Settings, Screen.Settings.route)
    )

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = { Text(stringResource(item.labelRes)) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddExpense.route) }
                ) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.nav_add))
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onAddExpense = { navController.navigate(Screen.AddExpense.route) },
                    onViewAll = { navController.navigate(Screen.ExpensesList.route) },
                    onEditExpense = { id ->
                        navController.navigate(Screen.EditExpense.createRoute(id))
                    }
                )
            }
            composable(Screen.AddExpense.route) {
                AddExpenseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = Screen.EditExpense.route,
                arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
            ) { backStackEntry ->
                val expenseId = backStackEntry.arguments?.getLong("expenseId") ?: 0L
                AddExpenseScreen(
                    expenseId = expenseId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.ExpensesList.route) {
                ExpensesListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditExpense = { id ->
                        navController.navigate(Screen.EditExpense.createRoute(id))
                    }
                )
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}

private data class BottomNavItem(
    @androidx.annotation.StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: String
)
