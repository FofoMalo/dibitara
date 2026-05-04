package com.dibitara.app.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.dibitara.app.presentation.navigation.Screen

/**
 * Barre de navigation inférieure — présente sur tous les écrans principaux.
 * Pour ajouter un onglet : ajouter une entrée dans [NavItem] + une destination dans Screen.
 */
@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(
        NavItem(Screen.Dashboard,   "Accueil",         Icons.Filled.Home),
        NavItem(Screen.Budget,      "Budget",          Icons.Filled.AccountBalance),
        NavItem(Screen.Expenses,    "Dépenses",        Icons.Filled.Receipt),
        NavItem(Screen.Investments, "Investissements", Icons.Filled.TrendingUp),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.screen.route,
                onClick = {
                    navController.navigate(item.screen.route) {
                        // Évite d'empiler des copies du même écran dans la pile
                        popUpTo(Screen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

private data class NavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)
