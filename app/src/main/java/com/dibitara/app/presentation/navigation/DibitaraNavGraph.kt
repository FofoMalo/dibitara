package com.dibitara.app.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Toutes les destinations de l'application sont définies ici.
 * Ajouter un nouvel écran = ajouter une entrée dans Screen + un composable dans le NavHost.
 */
sealed class Screen(val route: String) {
    data object Dashboard   : Screen("dashboard")
    data object Budget      : Screen("budget")
    data object Expenses    : Screen("expenses")
    data object Investments : Screen("investments")
}

@Composable
fun DibitaraNavGraph(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(Screen.Dashboard.route) {
            // TODO Sprint 1 : DashboardScreen()
        }
        composable(Screen.Budget.route) {
            // TODO Sprint 2 : BudgetScreen()
        }
        composable(Screen.Expenses.route) {
            // TODO Sprint 2 : ExpensesScreen()
        }
        composable(Screen.Investments.route) {
            // TODO Sprint 3 : InvestmentsScreen()
        }
    }
}
