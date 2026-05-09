package com.dibitara.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dibitara.app.presentation.auth.LockScreen
import com.dibitara.app.presentation.budget.BudgetScreen
import com.dibitara.app.presentation.common.BottomNavBar
import com.dibitara.app.presentation.dashboard.DashboardScreen
import com.dibitara.app.presentation.expenses.ExpensesScreen
import com.dibitara.app.presentation.debts.DebtsScreen
import com.dibitara.app.presentation.investments.InvestmentsScreen
import com.dibitara.app.presentation.savings.SavingsScreen
import com.dibitara.app.presentation.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Lock        : Screen("lock")
    data object Dashboard   : Screen("dashboard")
    data object Budget      : Screen("budget")
    data object Expenses    : Screen("expenses")
    data object Savings     : Screen("savings")
    data object Investments : Screen("investments")
    data object Debts       : Screen("debts")
    data object Settings    : Screen("settings")
}

// Écrans qui affichent la barre de navigation inférieure
private val bottomNavScreens = setOf(
    Screen.Dashboard.route,
    Screen.Budget.route,
    Screen.Expenses.route,
    Screen.Savings.route,
    Screen.Investments.route,
    Screen.Settings.route
)

@Composable
fun DibitaraNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavScreens

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomNavBar(navController)
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Lock.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Lock.route) {
                LockScreen(
                    onAuthenticated = {
                        navController.navigate(Screen.Dashboard.route) {
                            // Supprimer LockScreen de la pile — l'utilisateur ne peut pas revenir en arrière
                            popUpTo(Screen.Lock.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Dashboard.route) {
                DashboardScreen(onNavigateToDebts = { navController.navigate(Screen.Debts.route) })
            }
            composable(Screen.Budget.route)      { BudgetScreen() }
            composable(Screen.Expenses.route)    { ExpensesScreen() }
            composable(Screen.Savings.route)     { SavingsScreen() }
            composable(Screen.Investments.route) { InvestmentsScreen() }
            composable(Screen.Debts.route) {
                DebtsScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
