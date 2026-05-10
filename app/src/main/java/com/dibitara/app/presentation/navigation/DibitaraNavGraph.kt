package com.dibitara.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dibitara.app.presentation.auth.LockScreen
import com.dibitara.app.presentation.auth.SetupAuthScreen
import com.dibitara.app.presentation.budget.BudgetScreen
import com.dibitara.app.presentation.common.BottomNavBar
import com.dibitara.app.presentation.dashboard.DashboardScreen
import com.dibitara.app.presentation.expenses.ExpensesScreen
import com.dibitara.app.presentation.debts.DebtsScreen
import com.dibitara.app.presentation.investments.InvestmentsScreen
import com.dibitara.app.presentation.savings.SavingsScreen
import com.dibitara.app.presentation.report.MonthlyReportScreen
import com.dibitara.app.presentation.settings.SettingsScreen
import com.dibitara.app.presentation.settings.SettingsViewModel

sealed class Screen(val route: String) {
    data object Lock       : Screen("lock")
    data object SetupAuth  : Screen("setup_auth")
    data object Dashboard  : Screen("dashboard")
    data object Budget     : Screen("budget")
    data object Expenses   : Screen("expenses")
    data object Savings    : Screen("savings")
    data object Investments: Screen("investments")
    data object Debts      : Screen("debts")
    data object Settings   : Screen("settings")
    data object Report     : Screen("report")
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

    // Préférences de navigation — lues ici pour filtrer la nav bar en temps réel
    val settingsVm: SettingsViewModel = hiltViewModel()
    val prefs by settingsVm.preferences.collectAsState()

    Scaffold(
        bottomBar = {
            if (showBottomBar) BottomNavBar(
                navController           = navController,
                afficherEpargne         = prefs.afficherEpargne,
                afficherInvestissements = prefs.afficherInvestissements
            )
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
                            // Supprimer LockScreen de la pile — impossible de revenir en arrière
                            popUpTo(Screen.Lock.route) { inclusive = true }
                        }
                    },
                    onNeedsSetup = {
                        navController.navigate(Screen.SetupAuth.route) {
                            popUpTo(Screen.Lock.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.SetupAuth.route) {
                SetupAuthScreen(
                    onSetupComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.SetupAuth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToDebts       = { navController.navigate(Screen.Debts.route) },
                    onNavigateToReport      = { navController.navigate(Screen.Report.route) },
                    onNavigateToBudget      = { navController.navigate(Screen.Budget.route) },
                    onNavigateToSavings     = { navController.navigate(Screen.Savings.route) },
                    onNavigateToInvestments = { navController.navigate(Screen.Investments.route) }
                )
            }
            composable(Screen.Budget.route)      { BudgetScreen() }
            composable(Screen.Expenses.route)    { ExpensesScreen() }
            composable(Screen.Savings.route)     { SavingsScreen() }
            composable(Screen.Investments.route) { InvestmentsScreen() }
            composable(Screen.Debts.route) {
                DebtsScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Report.route) {
                MonthlyReportScreen(onNavigateBack = { navController.navigateUp() })
            }
        }
    }
}
