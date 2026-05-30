package com.dibitara.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.dibitara.app.presentation.auth.LockScreen
import com.dibitara.app.presentation.auth.SetupAuthScreen
import com.dibitara.app.presentation.budget.BudgetScreen
import com.dibitara.app.presentation.common.BottomNavBar
import com.dibitara.app.presentation.dashboard.DashboardScreen
import com.dibitara.app.presentation.expenses.ExpensesScreen
import com.dibitara.app.presentation.debts.DebtsScreen
import com.dibitara.app.presentation.investments.InvestmentsScreen
import com.dibitara.app.presentation.savings.SavingsScreen
import com.dibitara.app.presentation.patrimoine.PatrimoineDetailScreen
import com.dibitara.app.presentation.projection.ProjectionDetailScreen
import com.dibitara.app.presentation.report.MonthlyReportScreen
import com.dibitara.app.presentation.importcsv.ImportBredScreen
import com.dibitara.app.presentation.importcsv.ImportBredPdfScreen
import com.dibitara.app.presentation.importcsv.ImportScreen
import com.dibitara.app.presentation.settings.DuplicateCleanupScreen
import com.dibitara.app.presentation.settings.SettingsScreen
import com.dibitara.app.presentation.settings.SettingsViewModel
import com.dibitara.app.presentation.trends.TrendsScreen

sealed class Screen(val route: String) {
    data object Lock       : Screen("lock")
    data object SetupAuth  : Screen("setup_auth")
    data object Dashboard  : Screen("dashboard")
    data object Budget     : Screen("budget")
    // category et type sont des args optionnels pour pré-filtrer depuis BudgetScreen
    data object Expenses   : Screen("expenses?category={category}&type={type}&month={month}&year={year}") {
        fun withFilter(
            category: String? = null,
            type: String? = null,
            month: Int? = null,
            year: Int? = null
        ): String {
            val args = buildString {
                if (category != null) append("category=$category")
                if (type != null) { if (isNotEmpty()) append("&"); append("type=$type") }
                if (month != null) { if (isNotEmpty()) append("&"); append("month=$month") }
                if (year != null)  { if (isNotEmpty()) append("&"); append("year=$year") }
            }
            return if (args.isNotEmpty()) "expenses?$args" else "expenses"
        }
    }
    data object Savings    : Screen("savings")
    data object Investments: Screen("investments")
    data object Debts      : Screen("debts")
    data object Settings          : Screen("settings")
    data object Report            : Screen("report")
    data object PatrimoineDetail  : Screen("patrimoine_detail")
    data object ImportTR          : Screen("import_tr")
    data object ImportBred        : Screen("import_bred")
    data object ImportBredPdf     : Screen("import_bred_pdf")
    data object DuplicateCleanup  : Screen("duplicate_cleanup")
    data object ProjectionDetail  : Screen("projection_detail")
    data object Trends             : Screen("trends")
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
                    onNavigateToDebts        = { navController.navigate(Screen.Debts.route) },
                    onNavigateToReport       = { navController.navigate(Screen.Report.route) },
                    onNavigateToBudget       = { navController.navigate(Screen.Budget.route) },
                    onNavigateToSavings      = { navController.navigate(Screen.Savings.route) },
                    onNavigateToInvestments  = { navController.navigate(Screen.Investments.route) },
                    onNavigateToPatrimoine   = { navController.navigate(Screen.PatrimoineDetail.route) },
                    navController            = navController
                )
            }
            composable(Screen.ProjectionDetail.route) {
                ProjectionDetailScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(Screen.Budget.route) {
                BudgetScreen(
                    onNavigateToExpenses = { category, type, month, year ->
                        navController.navigate(Screen.Expenses.withFilter(category, type, month, year))
                    },
                    onNavigateToTrends = { navController.navigate(Screen.Trends.route) }
                )
            }
            composable(Screen.Trends.route) {
                TrendsScreen(onNavigateBack = { navController.popBackStack() })
            }
            composable(
                route = Screen.Expenses.route,
                arguments = listOf(
                    navArgument("category") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("type")     { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("month")    { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("year")     { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { ExpensesScreen() }
            composable(Screen.Savings.route)     { SavingsScreen() }
            composable(Screen.Investments.route) { InvestmentsScreen() }
            composable(Screen.Debts.route) {
                DebtsScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(
                route = Screen.Settings.route,
                deepLinks = listOf(navDeepLink { uriPattern = "dibitara://settings" })
            ) {
                SettingsScreen(
                    onNavigateToImportTR         = { navController.navigate(Screen.ImportTR.route) },
                    onNavigateToImportBred       = { navController.navigate(Screen.ImportBred.route) },
                    onNavigateToImportBredPdf    = { navController.navigate(Screen.ImportBredPdf.route) },
                    onNavigateToDuplicateCleanup = { navController.navigate(Screen.DuplicateCleanup.route) }
                )
            }
            composable(Screen.ImportTR.route) {
                ImportScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.ImportBred.route) {
                ImportBredScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.ImportBredPdf.route) {
                ImportBredPdfScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.DuplicateCleanup.route) {
                DuplicateCleanupScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
            composable(Screen.Report.route) {
                MonthlyReportScreen(onNavigateBack = { navController.navigateUp() })
            }
            composable(Screen.PatrimoineDetail.route) {
                PatrimoineDetailScreen(
                    onNavigateBack         = { navController.navigateUp() },
                    onNavigateToBudget     = { navController.navigate(Screen.Budget.route) },
                    onNavigateToSavings    = { navController.navigate(Screen.Savings.route) },
                    onNavigateToInvestments = { navController.navigate(Screen.Investments.route) },
                    onNavigateToDebts      = { navController.navigate(Screen.Debts.route) }
                )
            }
        }
    }
}
