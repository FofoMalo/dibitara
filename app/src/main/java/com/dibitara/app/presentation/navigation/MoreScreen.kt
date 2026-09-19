package com.dibitara.app.presentation.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Accès secondaires regroupés : les écrans métier conservent leurs propres actions. */
@Composable
fun MoreScreen(
    afficherEpargne: Boolean,
    afficherInvestissements: Boolean,
    onNavigate: (Screen) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Plus", style = MaterialTheme.typography.headlineMedium) }
        item { Text("Gérer", style = MaterialTheme.typography.titleMedium) }
        item { MoreEntry("Comptes bancaires", "Soldes et transactions par compte", Screen.BankAccounts, onNavigate) }
        if (afficherEpargne) {
            item { MoreEntry("Épargne et enfants", "Comptes, versements et objectifs", Screen.Savings, onNavigate) }
        }
        if (afficherInvestissements) {
            item { MoreEntry("Investissements", "Placements et évolution du capital", Screen.Investments, onNavigate) }
        }
        item { MoreEntry("Dettes et crédits", "Échéances et remboursements", Screen.Debts, onNavigate) }
        item { Text("Comprendre le passé", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        item { MoreEntry("Rapport mensuel", "Revenus, dépenses et bilan", Screen.Report, onNavigate) }
        item { MoreEntry("Tendances", "Évolution de votre budget", Screen.Trends, onNavigate) }
        item { Text("Préparer l’avenir", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        item { MoreEntry("Projections", "Trésorerie des 30 prochains jours", Screen.ProjectionDetail, onNavigate) }
        item { MoreEntry("Scénarios", "Préparer vos décisions", Screen.Scenarios, onNavigate) }
        item { Text("Application", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp)) }
        item { MoreEntry("Paramètres et sauvegardes", "Apparence, sécurité et copies de vos données", Screen.Settings, onNavigate) }
    }
}

@Composable
private fun MoreEntry(title: String, subtitle: String, screen: Screen, onNavigate: (Screen) -> Unit) {
    Column(Modifier.fillMaxWidth().clickable { onNavigate(screen) }) {
        Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}
