package com.dibitara.app.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.dibitara.app.domain.model.Currency

/**
 * Écran principal — tableau de bord financier.
 * Sprint 1 : affichage du budget du mois courant.
 * Sprint 3 : on ajoutera les graphiques de projection ici.
 */
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (val state = uiState) {
            is DashboardUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is DashboardUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }

            is DashboardUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Tableau de bord",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    state.budget?.let { budget ->
                        BudgetCard(
                            allocatedCents = budget.allocatedCents,
                            spentCents = budget.spentCents,
                            currency = budget.currency
                        )
                    } ?: Text(
                        text = "Aucun budget défini pour ce mois.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetCard(
    allocatedCents: Long,
    spentCents: Long,
    currency: Currency
) {
    val remaining = allocatedCents - spentCents
    val progress = if (allocatedCents > 0) spentCents.toFloat() / allocatedCents else 0f

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Budget du mois", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = if (progress > 0.9f) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Dépensé : ${spentCents.toEuroDisplay(currency)}")
                Text("Restant : ${remaining.toEuroDisplay(currency)}")
            }
        }
    }
}

// Convertit des centimes en chaîne lisible. Ex: 1250L → "12,50 €"
private fun Long.toEuroDisplay(currency: Currency): String {
    val amount = this / 100.0
    return "%.2f %s".format(amount, currency.symbol)
}
