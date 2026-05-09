package com.dibitara.app.presentation.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Currency

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val prefs by viewModel.preferences.collectAsState()

    // État local du champ seuil — synchronisé depuis prefs au premier rendu
    var seuilEuros by remember(prefs.seuilFondsCents) {
        mutableStateOf((prefs.seuilFondsCents / 100).toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Paramètres", style = MaterialTheme.typography.headlineMedium)

        // ─── Section notifications ────────────────────────────────────────────
        SectionCard(titre = "Notifications") {
            Text(
                text = "Seuil d'alerte — liquidités insuffisantes",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = "Une alerte est envoyée si le solde du mois passe sous ce montant.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = seuilEuros,
                    onValueChange = { seuilEuros = it },
                    label = { Text("Seuil (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = { viewModel.mettreAJourSeuil(seuilEuros) },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text("Appliquer")
                }
            }
        }

        // ─── Section tableau de bord ──────────────────────────────────────────
        SectionCard(titre = "Tableau de bord") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Rapport mensuel",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Remplace le graphique des 6 derniers mois.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = prefs.afficherRapportMensuel,
                    onCheckedChange = { viewModel.mettreAJourAfficherRapport(it) }
                )
            }
        }

        // ─── Section devise ───────────────────────────────────────────────────
        SectionCard(titre = "Devise par défaut") {
            Text(
                text = "Devise utilisée à la saisie des nouvelles transactions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            // Seules les 3 devises supportées par l'app
            val devises = listOf(Currency.EUR, Currency.USD, Currency.XOF)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                devises.forEach { devise ->
                    FilterChip(
                        selected = prefs.deviseParDefaut == devise,
                        onClick  = { viewModel.mettreAJourDevise(devise) },
                        label    = { Text("${devise.symbol} ${devise.isoCode}") }
                    )
                }
            }
        }
    }
}

// ─── Composant utilitaire ─────────────────────────────────────────────────────

@Composable
private fun SectionCard(titre: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(titre, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
